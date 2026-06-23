## 1. Persistência: coluna e migração (entry-categorization, vault-encryption)

- [x] 1.1 Em `VaultDbHelper`, adicionar a coluna `category BLOB` (nullable) ao `CREATE TABLE entries` (`onCreate`) e bumpar `DB_VERSION` 2→3.
- [x] 1.2 Em `onUpgrade`, tratar `oldVersion < 3` com `ALTER TABLE entries ADD COLUMN category BLOB`, mantendo o ramo v1→v2 (envelope biométrico) intacto e encadeável.
- [ ] 1.3 Validar a migração: cofre v2 existente abre após atualização com a coluna NULL e entradas inalteradas (cenário "Cofre antigo continua válido após atualização").

## 2. Camada de dados (entry-categorization, entry-management)

- [x] 2.1 Adicionar `category: ByteArray?` em `EncryptedEntry` e mapear no `Cursor.toEntry()`.
- [x] 2.2 Em `VaultRepository.insertEntry`/`updateEntry`, gravar `category` (ou `putNull`), e garantir que `getEntry`/`listEntries` selecionem a coluna.
- [ ] 2.3 Verificar (inspeção do .db / teste) que a categoria é persistida apenas como ciphertext+nonce, nunca em texto claro (cenário "Inspeção do banco não revela categorias").

## 3. Fachada Vault (entry-management, entry-categorization)

- [x] 3.1 Adicionar `category: String?` a `Vault.EntrySummary` e `Vault.PlainEntry`; em `add`/`update` cifrar a categoria com a DEK (nova nonce por gravação), tratando vazio/só-espaços como `null`.
- [x] 3.2 Em `list()` e `detail()`, decifrar a categoria em memória (sessão desbloqueada).
- [x] 3.3 Implementar `Vault.categories(): List<String>` — distinct das categorias decifradas, deduplicadas/normalizadas via `LabelSearch.normalize`, preservando a 1ª grafia (cenários "Sugerir categoria existente" / "Sugestão ignora caixa e acento").

## 4. UI de edição: campo de categoria com autocomplete (entry-categorization, entry-management)

- [x] 4.1 Adicionar helper em `Theme.kt` para `AutoCompleteTextView` estilizado (SDK puro, coerente com `input`).
- [x] 4.2 Em `AddEditActivity`, adicionar o campo "Categoria (opcional)" alimentado por `ArrayAdapter` com `vault.categories()`; carregar a categoria atual na edição.
- [x] 4.3 No `save()`, passar a categoria (normalizada para `null` quando vazia) para `vault.add`/`vault.update` (cenários "Adicionar com categoria", "Alterar categoria na edição", "Categoria é opcional").

## 5. UI de listagem: visualização agrupada (entry-categorization, entry-search)

- [x] 5.1 Criar um `BaseAdapter` com dois view types — cabeçalho de categoria (não clicável, `isEnabled=false`) e item de entrada — sobre uma lista achatada `Header + Item`.
- [x] 5.2 Em `ListActivity`, construir os grupos a partir do resultado filtrado: ordenar categorias por `LabelSearch.normalize`, "Sem categoria" sempre por último, entradas mantendo `updated_at DESC` (cenários "Entradas agrupadas sob cabeçalhos", "Grupo sem categoria só quando existir").
- [x] 5.3 Ajustar o clique para mapear posição → `EntrySummary` pela lista achatada (substituindo `filtered[pos]`), abrindo o detalhe correto.
- [x] 5.4 Integrar a busca existente: o termo filtra as entradas e os grupos são reexibidos, ocultando grupos sem resultado (cenários "Resultados permanecem agrupados", "Busca vazia mostra tudo"); preservar estados vazio/sem-resultado.

## 6. Verificação

- [x] 6.1 `./gradlew :app:assembleDebug` compila sem novas dependências; `./gradlew :vault-crypto:test` (self-test de `LabelSearch`) verde.
- [ ] 6.2 Sideload do APK e smoke test: criar/editar com categoria, ver agrupamento com "Sem categoria" ao final, autocomplete sugerindo categorias, busca compondo com os grupos, e bloquear/desbloquear sem vazar estado.
