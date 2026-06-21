## Why

Hoje a troca da senha mestra (`VaultSession.changeMasterPassword`) reutiliza o **mesmo salt** do PBKDF2 ao derivar a KEK da nova senha — apenas re-cifra a DEK. Manter o salt fixo por toda a vida do cofre enfraquece a higiene criptográfica: reduz o benefício de trocar a senha após uma possível exposição, mantém uma KEK derivável do mesmo material caso a senha antiga tenha vazado, e não acompanha a boa prática de renovar o salt a cada mudança de credencial. Como a constraint do projeto manda escolher sempre a opção mais conservadora em segurança, a troca de senha deve gerar um salt novo.

## What Changes

- `VaultSession.changeMasterPassword` passa a **gerar um salt aleatório novo** a cada troca e derivar a nova KEK com esse salt (em vez de reutilizar `meta.salt`).
- A persistência grava o **novo salt junto com a DEK re-cifrada de forma atômica** (mesma transação SQLite), evitando qualquer estado intermediário que torne o cofre inacessível.
- **BREAKING (API interna)**: a porta `VaultMetaStore.updateWrappedDek(wrappedDek)` é substituída/estendida para também receber o salt novo (ex.: `updateAuthMaterial(salt, wrappedDek)`), atualizando `VaultRepository` e a sessão. Não há mudança no formato em disco — `salt` já é uma coluna existente em `vault_meta`; apenas passa a ser reescrita.
- O `CryptoSelfTest` ganha um caso que prova que **o salt muda após a troca de senha** e que os dados permanecem íntegros e acessíveis com a nova senha.
- Sem migração de cofres existentes: o esquema cripto (`SCHEME_VERSION`) permanece em 1, pois o layout persistido não muda; o salt antigo continua válido até a próxima troca.

## Capabilities

### New Capabilities
<!-- Nenhuma capability nova. -->

### Modified Capabilities
- `master-authentication`: o requisito de troca da senha mestra passa a exigir geração de um novo salt a cada troca, persistido atomicamente com a DEK re-cifrada.

## Impact

- **Código**: `vault-crypto/src/main/kotlin/com/jaaspass/crypto/VaultSession.kt`, `VaultMetaStore.kt`; `app/.../data/VaultRepository.kt`; `vault-crypto/src/test/kotlin/com/jaaspass/crypto/CryptoSelfTest.kt`.
- **APIs**: assinatura da porta `VaultMetaStore` (interna ao app; sem superfície pública externa).
- **Dependências**: nenhuma nova (mantém zero-deps; só `javax.crypto` / `android.database.sqlite`).
- **Dados**: nenhum schema novo nem migração; `vault_meta.salt` passa a ser atualizado na troca de senha. Cofres existentes seguem funcionando.
- **Segurança**: melhora a postura — renova material derivado a cada troca; atomicidade da gravação evita brick do cofre.
