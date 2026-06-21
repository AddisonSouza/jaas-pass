## Context

O núcleo cripto isolado (`:vault-crypto`) usa envelope encryption: KEK = PBKDF2-HMAC-SHA256(senha, salt, iter) protege uma DEK aleatória que cifra as entradas. O salt é gerado uma vez no `setup` e persistido em `vault_meta.salt`.

Hoje `VaultSession.changeMasterPassword` (vault-crypto/.../VaultSession.kt) re-deriva a KEK da nova senha **com o salt antigo** (`meta.salt`) e chama `VaultMetaStore.updateWrappedDek(rewrapped)`, que escreve só a coluna `wrapped_dek`. O salt nunca muda após o `setup`.

Constraints do projeto: zero deps de terceiros, AEAD sempre, falha segura, e em qualquer ambiguidade de segurança escolher a opção mais conservadora.

## Goals / Non-Goals

**Goals:**
- Gerar um salt novo a cada troca de senha e derivar a nova KEK com ele.
- Persistir salt novo + DEK re-cifrada de forma atômica (transação SQLite).
- Manter todas as entradas decriptáveis com a nova senha; nenhuma re-cifragem de entradas (a DEK não muda).
- Cobrir o comportamento com o harness de self-test existente (sem deps).

**Non-Goals:**
- Migração de cofres existentes (o layout em disco não muda; salt antigo segue válido até a próxima troca).
- Bump de `SCHEME_VERSION` (formato persistido inalterado).
- Rotação automática de salt fora da troca de senha (ex.: agendada).
- Mudança nas iterações do PBKDF2 (já são as do `DEFAULT_ITERATIONS` da sessão na troca).

## Decisions

**1. Gerar salt novo via `CryptoManager.generateSalt()` dentro de `changeMasterPassword`.**
Reusa o gerador existente (`SecureRandom`, `SALT_LENGTH`). A nova KEK é derivada com `crypto.deriveKEK(new, novoSalt, iterations)`; a DEK desenvelopada com a senha atual é re-envelopada com essa KEK nova.
- Alternativa descartada: rotacionar salt num método separado — acopla mal e abre janela de inconsistência entre dois writes.

**2. Substituir `VaultMetaStore.updateWrappedDek(wrappedDek)` por `updateAuthMaterial(salt, wrappedDek)`.**
A porta passa a gravar salt + DEK juntos. Renomear (em vez de adicionar) evita deixar um método legado que reintroduza o bug de salt fixo. A superfície é interna ao app — sem impacto externo.
- Alternativa descartada: manter `updateWrappedDek` e adicionar `updateSalt` — dois writes, sem atomicidade trivial e fácil de chamar pela metade.

**3. Atomicidade via transação SQLite no `VaultRepository`.**
`updateAuthMaterial` executa um único `UPDATE vault_meta SET salt=?, wrapped_dek=? WHERE id=1` envolto em `beginTransaction()/setTransactionSuccessful()/endTransaction()`. Embora um UPDATE de linha única já seja atômico, a transação explícita documenta a invariante "tudo-ou-nada" e protege contra evolução futura do método. (Decisão confirmada com o usuário.)

**4. Ordem de operações segura na sessão.** Primeiro validar a senha atual desenvelopando a DEK (retorna `false` em `AEADBadTagException` sem efeitos colaterais); só então gerar salt novo, re-cifrar e persistir. Garante que uma senha atual errada não toca o material persistido.

**5. Teste no `CryptoSelfTest`.** Estender o caso de troca de senha para capturar `meta.salt` antes/depois e asserir que mudou, e que `unlock` com a nova senha funciona e com a antiga falha.

## Risks / Trade-offs

- **[Brick do cofre se salt e DEK divergirem]** → gravação atômica numa única transação/UPDATE; senha atual validada antes de qualquer write.
- **[Quebra de chamadores de `updateWrappedDek`]** → única chamada conhecida é a própria sessão; renomeação é localizada e coberta pela compilação + self-test.
- **[Salt antigo permanece em cofres não migrados]** → aceitável e documentado; o objetivo é renovar na próxima troca, não migrar retroativamente.
- **[Trade-off: transação explícita para um único UPDATE]** → custo desprezível; ganho de clareza e robustez a mudanças futuras.

## Migration Plan

Sem migração de dados. `SCHEME_VERSION` permanece 1; coluna `salt` já existe. Rollback = reverter o código; cofres criados/alterados continuam válidos pois o formato persistido não mudou.

## Open Questions

Nenhuma — escopo e atomicidade já decididos.
