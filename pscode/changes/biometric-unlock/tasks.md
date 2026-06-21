## 1. Build e manifesto

- [x] 1.1 Subir `minSdk` 26→28 em `app/build.gradle.kts`; confirmar `./gradlew :app:assembleDebug` ok (BREAKING documentado no proposal).
- [x] 1.2 Declarar `<uses-permission android:name="android.permission.USE_BIOMETRIC"/>` em `AndroidManifest.xml`; verificar que nenhuma permissão de rede foi introduzida (offline absoluto).

## 2. Persistência (metadados + migração)

- [x] 2.1 Adicionar `biometricWrappedDek: ByteArray?` e `biometricIv: ByteArray?` (nullable) a `VaultMeta`, sem quebrar o `equals`/uso atual.
- [x] 2.2 Adicionar colunas nullable `biometric_wrapped_dek` e `biometric_iv` em `vault_meta`; bump `DB_VERSION` 1→2 com `onUpgrade` aditivo (`ALTER TABLE ADD COLUMN`). Valida *Inspeção do armazenamento não revela segredos* e que cofres existentes (sem biometria) seguem abrindo.
- [x] 2.3 Estender `VaultMetaStore` com operações para gravar (`saveBiometricMaterial`) e limpar (`clearBiometricMaterial`) o envelope biométrico isoladamente; implementar em `VaultRepository` sem tocar em salt/wrappedDek. `loadMeta` passa a carregar os campos biométricos. Valida *Desativar biometria descarta o atalho* (lado da persistência).

## 3. Sessão (vault-crypto puro)

- [x] 3.1 Adicionar `VaultSession.exportDekForBiometric(): SecretKey` exigindo estado DESBLOQUEADO (usado na ativação). Teste: lança/recusa quando BLOQUEADO.
- [x] 3.2 Adicionar `VaultSession.unlockWithDek(dek: SecretKey)` que injeta a DEK e marca DESBLOQUEADO sem PBKDF2. Valida *Ambos os caminhos abrem o mesmo cofre* (a DEK injetada decripta as entradas). Manter `vault-crypto` sem imports Android.

## 4. Keystore + envelope (módulo app)

- [x] 4.1 Criar `app/.../crypto/BiometricKeyStore.kt`: gerar/abrir chave AES no Android Keystore com `setUserAuthenticationRequired(true)` e `setInvalidatedByBiometricEnrollment(true)` (GCM/NoPadding, 256 bits, alias fixo). Valida *Proteção da DEK por chave do Android Keystore*.
- [x] 4.2 Expor `cipherForEncrypt()` / `cipherForDecrypt(iv)` (para o `CryptoObject`), `wrap(dek, cipher) -> (ct, iv)`, `unwrap(ct, cipher) -> dek` e `deleteKey()`. Tratar `KeyPermanentlyInvalidatedException`/`UserNotAuthenticatedException`. Valida *Nova digital invalida o atalho biométrico* e *Biometria cancelada ou inválida não concede acesso* (lado cripto).
- [x] 4.3 Helper de disponibilidade: `PackageManager.hasSystemFeature(FEATURE_FINGERPRINT)` + ausência de chave/hardware → biometria indisponível. Valida *Sem hardware/cadastro biométrico não há oferta*.

## 5. UI — ativação

- [x] 5.1 Após `setup`/`unlock` por senha em `UnlockActivity`, se há hardware e biometria não ativa, oferecer ativar (`AlertDialog` do SDK). Valida *Oferta após desbloqueio com biometria disponível*.
- [x] 5.2 Fluxo de ativação: `exportDekForBiometric` → `BiometricPrompt` com `CryptoObject(cipherForEncrypt())` → no sucesso, `wrap(dek)` e `saveBiometricMaterial(ct, iv)`. Valida *Ativação não persiste a senha mestra* (somente ct+iv gravados).

## 6. UI — desbloqueio e desativação

- [x] 6.1 Em `UnlockActivity`/`onResume`, com cofre BLOQUEADO e biometria ativa, disparar `BiometricPrompt` automaticamente, mantendo o campo de senha visível. Valida *Prompt automático com cofre bloqueado e biometria ativa* e *Disparo automático do prompt*.
- [x] 6.2 No sucesso da biometria: `cipherForDecrypt(iv)` via `CryptoObject` → `unwrap(ct)` → `unlockWithDek(dek)` → ir para a lista. Valida *Biometria válida desbloqueia o cofre*.
- [x] 6.3 Em cancelamento/erro do prompt: permanecer BLOQUEADO, mensagem genérica, manter fallback de senha. Valida *Biometria cancelada ou inválida não concede acesso* e *Fallback para senha mestra sempre disponível*.
- [x] 6.4 Detecção de chave invalidada (nova digital): ao falhar com `KeyPermanentlyInvalidatedException`, limpar material biométrico (`clearBiometricMaterial` + `deleteKey`) e exigir senha mestra. Valida *Nova digital invalida o atalho biométrico*.
- [x] 6.5 Ação "Desativar biometria" na tela de unlock: `clearBiometricMaterial` + `deleteKey`, voltando ao desbloqueio só por senha. Valida *Desativar biometria descarta o atalho*.

## 7. Verificação de integração

- [x] 7.1 Conferir que trocar a senha mestra com biometria ativa não quebra o atalho (envelope cifra a DEK, não a KEK). Valida *Troca de senha não quebra o atalho biométrico*.
- [ ] 7.2 Smoke manual no aparelho (sideload APK debug): setup → ativar biometria → auto-lock → desbloquear por biometria → desbloquear por senha (fallback) → desativar. Confere o fluxo fim a fim.
