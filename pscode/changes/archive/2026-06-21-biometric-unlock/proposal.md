## Why

Hoje o único jeito de abrir o cofre é digitar a senha mestra — que, por segurança, deve ser
longa e forte. Em um app de uso frequente, redigitar uma senha forte a cada desbloqueio (inclusive
após cada auto-lock por inatividade ou ida para segundo plano) gera fricção e empurra o usuário a
escolher senhas fracas. Oferecer **biometria como atalho de desbloqueio** reduz essa fricção sem
enfraquecer o modelo criptográfico: a senha mestra continua sendo a raiz de confiança e o fallback
obrigatório.

## What Changes

- Nova capacidade de **desbloqueio por biometria** como atalho para a senha mestra. A digital
  libera uma chave do Android Keystore que decifra a DEK direto para a sessão, pulando o
  PBKDF2/senha mestra.
- **Opt-in explícito**: após um `unlock`/`setup` bem-sucedido com a senha mestra, se o aparelho
  tiver biometria cadastrada e ela ainda não estiver ativa, o app oferece ativá-la. A ativação só
  ocorre com a sessão já desbloqueada (a DEK precisa estar em memória para ser re-envelopada).
- **Tela de unlock** passa a disparar o `BiometricPrompt` automaticamente quando a biometria está
  ativa e o cofre está bloqueado; o campo de senha mestra permanece visível como fallback.
- **Desativar biometria** pela própria tela de unlock (descarta o blob da DEK envelopada pelo
  Keystore e a chave do Keystore).
- A chave do Keystore usa `setUserAuthenticationRequired(true)` e
  `setInvalidatedByBiometricEnrollment(true)`: cadastrar uma nova digital **invalida** a chave e
  força o uso da senha mestra (re-ativação posterior, se desejada).
- **BREAKING (constraints do projeto)**: `minSdk` sobe de **26 → 28** para usar o
  `android.hardware.biometrics.BiometricPrompt` nativo do SDK, mantendo a regra de **zero
  dependências de terceiros** (sem `androidx.biometric`). Aparelhos em Android 8.0/8.1 deixam de
  ser suportados.
- **BREAKING (schema do banco)**: a tabela `vault_meta` ganha colunas para o blob da DEK envelopada
  pela chave do Keystore (`biometric_wrapped_dek`) e o IV correspondente; exige bump de
  `DB_VERSION` com migração aditiva.

## Capabilities

### New Capabilities
- `biometric-unlock`: ativação/desativação da biometria, envelopamento da DEK por chave do Android
  Keystore protegida por autenticação do usuário, desbloqueio via `BiometricPrompt` + `CryptoObject`,
  invalidação por novo cadastro biométrico e fallback obrigatório para a senha mestra.

### Modified Capabilities
- `master-authentication`: o desbloqueio passa a ter **dois caminhos** (senha mestra ou biometria),
  com a senha mestra permanecendo como raiz de confiança e fallback. O ciclo de sessão/auto-lock é
  reaproveitado sem mudança de comportamento; acrescenta-se que, após auto-lock, a biometria também
  pode reabrir a sessão.

## Impact

- **Build**: `app/build.gradle.kts` (`minSdk` 26→28). Nenhuma nova dependência Gradle.
- **Manifesto**: `app/src/main/AndroidManifest.xml` — declarar `USE_BIOMETRIC`
  (permissão "normal", concedida em tempo de instalação; não viola o offline absoluto).
- **Cripto (módulo app, Android-específico)**: nova peça para gerar/usar a chave do Keystore e
  envelopar/desenvelopar a DEK. **Não** entra no módulo JVM puro `vault-crypto` (que permanece sem
  dependências de Android).
- **Sessão**: `VaultSession` ganha um caminho para injetar a DEK já decifrada pela biometria
  (sem PBKDF2) e um sinal de "expor a DEK atual" no momento da ativação.
- **Persistência**: `VaultRepository`/`vault_meta` — novas colunas + migração; `VaultMeta` ganha
  os campos opcionais do envelope biométrico.
- **UI**: `UnlockActivity` (prompt automático + fallback + desativar) e o ponto de oferta após
  unlock/setup. `SecureActivity` (`FLAG_SECURE`) já cobre o endurecimento de tela.
- **Não afeta**: o modelo de cifragem das entradas, o offline absoluto, nem a regra de não
  persistir a senha mestra.
