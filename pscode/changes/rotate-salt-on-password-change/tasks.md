## 1. Porta de persistência (VaultMetaStore)

- [ ] 1.1 Substituir `updateWrappedDek(wrappedDek: ByteArray)` por `updateAuthMaterial(salt: ByteArray, wrappedDek: ByteArray)` na interface `VaultMetaStore`.

## 2. Persistência atômica (VaultRepository)

- [ ] 2.1 Implementar `updateAuthMaterial` no `VaultRepository`: `UPDATE vault_meta SET salt=?, wrapped_dek=? WHERE id=1` envolto em transação (`beginTransaction`/`setTransactionSuccessful`/`endTransaction`).
- [ ] 2.2 Remover a implementação antiga `updateWrappedDek` e garantir que não há outros chamadores.

## 3. Rotação do salt na sessão (VaultSession)

- [ ] 3.1 Em `changeMasterPassword`, validar a senha atual desenvelopando a DEK antes de qualquer escrita (retornar `false` em `AEADBadTagException`, sem efeitos colaterais).
- [ ] 3.2 Gerar um salt novo via `crypto.generateSalt()` e derivar a nova KEK com `crypto.deriveKEK(new, novoSalt, iterations)`.
- [ ] 3.3 Re-cifrar a DEK com a nova KEK e persistir via `store.updateAuthMaterial(novoSalt, rewrapped)`; manter a sessão desbloqueada com a DEK em memória.

## 4. Testes (zero-deps)

- [ ] 4.1 Estender o caso de troca de senha no `CryptoSelfTest` para capturar o salt antes/depois e asserir que mudou (cobre "Salt é renovado a cada troca").
- [ ] 4.2 Asserir que após a troca o `unlock` funciona com a nova senha e falha com a antiga, e que as entradas permanecem decriptáveis (cobre "Troca de senha preserva os dados").
- [ ] 4.3 Adicionar caso de senha atual incorreta: a troca retorna negativa e salt + wrappedDek persistidos permanecem inalterados (cobre "Senha atual incorreta não altera o material").

## 5. Validação

- [ ] 5.1 Rodar `./gradlew :vault-crypto:cryptoSelfTest` (todos verdes).
- [ ] 5.2 Rodar `./gradlew :app:assembleDebug` (compila sem chamadores quebrados da porta renomeada).
