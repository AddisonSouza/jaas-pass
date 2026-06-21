## Context

O cofre usa envelope encryption: a DEK (cifra as entradas) fica em repouso cifrada pela KEK, que é
derivada da senha mestra via PBKDF2 (`CryptoManager.deriveKEK`). `VaultSession` mantém a DEK em
memória só enquanto DESBLOQUEADO; `unlock` re-deriva a KEK e desenvelopa a DEK
(`CryptoManager.unwrapDEK`). Os metadados (`VaultMeta`: salt, iterations, schemeVersion,
wrappedDek) moram numa linha única da tabela `vault_meta` (SQLite, `VaultRepository`, implementando
a porta `VaultMetaStore`). O módulo `vault-crypto` é **JVM puro** (zero Android) e testável.

Hoje o desbloqueio é exclusivamente por senha mestra (`UnlockActivity`). Não há tela de
Configurações. `SecureActivity` aplica `FLAG_SECURE` e renova o auto-lock.

Esta change adiciona biometria como **atalho** para obter a DEK, sem tocar no esquema PBKDF2/KEK
nem na cifragem das entradas.

## Goals / Non-Goals

**Goals:**
- Desbloquear o cofre com biometria, derivando a DEK por uma chave do Android Keystore protegida por
  autenticação do usuário — pulando PBKDF2/senha mestra.
- Manter a senha mestra como raiz de confiança e fallback obrigatório.
- Amarrar a biometria à decifragem real via `BiometricPrompt` + `CryptoObject` (nada de gate
  decorativo).
- Preservar as constraints do projeto: **zero dependências de terceiros** (sem `androidx.biometric`)
  e offline absoluto.
- Manter o `vault-crypto` puro: toda a parte Android (Keystore + BiometricPrompt) fica no módulo
  `app`.

**Non-Goals:**
- Tela de Configurações dedicada (a ativação é oferecida após unlock; desativação na tela de unlock).
- Suporte a Android 8.0/8.1 (API 26/27) — `minSdk` sobe para 28.
- PIN/padrão do dispositivo como credencial (apenas biometria; senha mestra é o fallback do app).
- Mudanças no esquema de cifragem das entradas, no PBKDF2 ou na rotação de salt.

## Decisions

### 1. BiometricPrompt nativo do SDK + minSdk 28 (zero-deps)
Usar `android.hardware.biometrics.BiometricPrompt` (API 28+) em vez de `androidx.biometric`. Isso
mantém a regra de zero dependências, ao custo de subir `minSdk` 26→28. Detecção de
disponibilidade via `BiometricManager` não existe na API 28 sem AndroidX; usar
`FingerprintManager`/`PackageManager.FEATURE_FINGERPRINT` **não** é desejável (deprecado/limitado).
Alternativa adotada: tentar gerar/usar a chave do Keystore e tratar `KeyPermanentlyInvalidatedException`
/ ausência de hardware via exceções, e checar `PackageManager.hasSystemFeature(FEATURE_FINGERPRINT)`
(API 23+) para decidir se oferece o opt-in.

### 2. Envelope direto da DEK pela chave do Keystore (não da KEK)
A chave do Keystore cifra **a DEK**, não a KEK nem a senha. Consequências:
- O desbloqueio biométrico não precisa da senha mestra nem de PBKDF2.
- A troca de senha mestra (rotaciona salt + re-envelopa a DEK com nova KEK) **não invalida** o
  envelope biométrico, pois a DEK permanece a mesma.
- Persistimos `biometric_wrapped_dek` (ciphertext) + `biometric_iv` (o IV do GCM do Keystore).
  Diferente do formato `nonce‖ct‖tag` do `CryptoManager`: aqui o IV é gerado/possuído pela chave do
  Keystore e recuperado de `cipher.iv`, então guardamos IV e ciphertext separadamente.

### 3. Parâmetros da chave do Keystore
`KeyGenParameterSpec` (alias fixo, ex. `"jaaspass.biometric.dek"`):
- `AES` / `GCM` / `NoPadding`, 256 bits.
- `setUserAuthenticationRequired(true)` — uso só após autenticação biométrica.
- `setInvalidatedByBiometricEnrollment(true)` — nova digital invalida a chave.
- (StrongBox quando disponível é opcional; não bloquear se ausente.)
A chave nunca é exportada; só um `Cipher` inicializado por ela viaja no `CryptoObject`.

### 4. Dois fluxos com CryptoObject
- **Ativar** (sessão DESBLOQUEADA): inicializar `Cipher` em `ENCRYPT_MODE` com a chave do Keystore →
  `BiometricPrompt` com `CryptoObject(cipher)` → ao autenticar, cifrar a DEK atual → persistir
  `biometric_wrapped_dek` + `biometric_iv`.
- **Desbloquear** (cofre BLOQUEADO): inicializar `Cipher` em `DECRYPT_MODE` com
  `GCMParameterSpec(128, biometric_iv)` → `BiometricPrompt` com `CryptoObject(cipher)` → ao
  autenticar, decifrar `biometric_wrapped_dek` → injetar a DEK na sessão.

Para obter a DEK em claro na ativação e injetá-la no desbloqueio, `VaultSession` ganha:
- `exportDekForBiometric(): SecretKey` — só com sessão DESBLOQUEADA (usado na ativação).
- `unlockWithDek(dek: SecretKey)` — injeta a DEK decifrada pela biometria, marca DESBLOQUEADO.
Esses métodos vivem no `vault-crypto` (puros), enquanto o uso de Keystore/BiometricPrompt fica no app.

### 5. Onde mora o código Android-específico
Nova classe no módulo `app` (ex. `app/.../crypto/BiometricKeyStore.kt`) encapsula: gerar/abrir a
chave, montar os `Cipher`, expor `wrap(dek): (ct, iv)` e `unwrap(ct, iv): dek`, e apagar a chave.
`UnlockActivity` orquestra o `BiometricPrompt`. O `vault-crypto` permanece sem imports de Android.

### 6. Persistência e migração
- `VaultMeta` ganha campos opcionais `biometricWrappedDek: ByteArray?` e `biometricIv: ByteArray?`.
- `vault_meta` ganha colunas `biometric_wrapped_dek BLOB` e `biometric_iv BLOB` (nullable).
- `DB_VERSION` 1→2 com `onUpgrade` aditivo (`ALTER TABLE ... ADD COLUMN`), preservando cofres
  existentes.
- `VaultMetaStore` ganha operações para gravar e limpar o material biométrico de forma isolada
  (ativar/desativar), sem tocar em salt/wrappedDek.

### 7. Manifesto
Declarar `<uses-permission android:name="android.permission.USE_BIOMETRIC"/>` (permissão "normal",
concedida na instalação; não envolve rede, preserva o offline absoluto).

### 8. UX
- Após `setup`/`unlock` por senha: se há hardware e biometria não ativa, oferecer ativar (diálogo
  `android.app.AlertDialog` com tema do SDK).
- Tela de unlock com biometria ativa e cofre BLOQUEADO: disparar `BiometricPrompt` automaticamente
  no `onResume`; manter campo de senha como fallback; incluir ação "Desativar biometria".
- Falha/cancelamento do prompt: permanecer BLOQUEADO, mensagem genérica, fallback por senha.

## Risks / Trade-offs

- **minSdk 26→28 (BREAKING)**: derruba Android 8.0/8.1. Trade-off aceito para manter zero-deps;
  decidido explicitamente com o usuário.
- **Detecção de disponibilidade sem AndroidX**: a API nativa 28 é mais crua que `BiometricManager`.
  Mitigação: checagem por `PackageManager` + tratamento de exceções do Keystore
  (`KeyPermanentlyInvalidatedException`, `UserNotAuthenticatedException`).
- **DEK exposta brevemente na ativação**: `exportDekForBiometric` materializa a DEK para envelopá-la.
  Já existe exposição equivalente em memória durante o uso normal; manter o tempo de vida curto.
- **Superfície de chave adicional**: além do `wrappedDek` (senha), há agora `biometric_wrapped_dek`.
  Ambos cedem a **mesma** DEK; quem confia na biometria do aparelho aceita esse caminho. A
  invalidação por novo cadastro biométrico limita abuso por troca de digital.
- **`SecretKeySpec`/DEK em memória**: limitação já documentada no projeto (não trivialmente apagável);
  mitigada por sessão curta e auto-lock — sem regressão.
- **Migração de DB**: `ALTER TABLE` aditivo é de baixo risco; colunas nullable garantem cofres
  antigos (sem biometria) funcionando sem mudança.
