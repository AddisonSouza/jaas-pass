## Context

O cofre (`ListActivity`) hoje mostra uma lista plana de rótulos decifrados em memória
(`Vault.list()`), com busca por substring sobre o rótulo (`LabelSearch`, no módulo JVM puro
`:vault-crypto`). As entradas são persistidas como blobs cifrados AES-256-GCM (`nonce ‖ ct ‖ tag`)
em SQLite (`entries`: `label`, `username?`, `password`), DB na versão 2. Não há nenhum agrupador.

Esta mudança introduz uma **categoria opcional por entrada** e troca a lista plana por uma
**lista agrupada por categoria**. Restrições do projeto valem integralmente: zero dependências de
terceiros (apenas SDK Android + Kotlin stdlib), offline absoluto, AEAD sempre, falha segura, e em
ambiguidade de segurança escolher o mais conservador.

> Terminologia: `label`/"rótulo" = **nome da entrada** (já existente). "categoria" = o **agrupador
> novo**. Mantemos os dois nomes distintos em código e UI para não colidir com `entry-search`.

## Goals / Non-Goals

**Goals:**
- Uma categoria opcional (0..1) por entrada, cifrada em repouso com a DEK.
- Escolha da categoria por texto com autocomplete das categorias já usadas (derivadas em memória).
- Listagem agrupada por cabeçalhos de categoria, com grupo "Sem categoria" ao final.
- Busca por rótulo existente preservada, compondo com o agrupamento.
- Migração SQLite v2→v3 aditiva e nullable, sem re-cifrar entradas existentes.

**Non-Goals:**
- Múltiplas categorias/tags por entrada (many-to-many).
- Tela ou tabela dedicada de gerenciamento de categorias (renomear/excluir em massa).
- Filtro por chips/seletor de categoria (a separação é feita pelos cabeçalhos de seção).
- Busca pelo texto da categoria (o escopo da busca continua sendo o rótulo).

## Decisions

### D1 — Uma coluna cifrada `category` (nullable) na tabela `entries`
Adicionar `category BLOB` (nullable) em `entries`, cifrada com a DEK como os demais campos. `NULL`
≡ "Sem categoria".
- *Por quê:* cardinalidade 0..1 mapeia direto para uma coluna; sem join, sem tabela extra.
- *Alternativa:* tabela `entry_tags`/`tags` many-to-many — descartada (escopo, complexidade de UI e
  de cifragem por tag) conforme decisão de produto (uma categoria por senha).

### D2 — Categoria é dado sensível ⇒ cifrada com a DEK
A categoria entra no mesmo caminho de `encryptField`/`decryptField` do `CryptoManager`, com novo
nonce por gravação (regravação na edição regenera o nonce, como já ocorre com os outros campos).
- *Por quê:* "Banco", "Trabalho" etc. revelam contexto; constraint de segurança manda conservar.
- *Alternativa:* gravar em texto claro (mais simples para ordenar/agrupar via SQL) — descartada por
  enfraquecer o modelo "armazenamento somente de ciphertext" (`vault-encryption`).

### D3 — Conjunto de categorias derivado em memória (sem registro persistido)
As sugestões vêm de `Vault.categories()`: distinct das categorias já decifradas por `Vault.list()`,
deduplicadas/normalizadas via `LabelSearch.normalize` (preservando a 1ª grafia exibida).
- *Por quê:* atende "labels cadastrados" sem nova tabela nem tela; "cadastrar" = digitar a 1ª vez.
- *Alternativa:* tabela/tela de categorias — descartada (Non-Goal).

### D4 — Migração SQLite v2→v3 aditiva
`DB_VERSION` 2→3; em `onUpgrade`, para `oldVersion < 3`, `ALTER TABLE entries ADD COLUMN category
BLOB` (nullable). Mantém o `onUpgrade` v1→v2 (envelope biométrico) intacto e encadeável.
- *Por quê:* coluna aditiva nullable não exige reescrever linhas nem re-cifrar; cofres antigos
  abrem normalmente com categoria NULL.

### D5 — Agrupamento e ordenação em memória (na UI), não no SQL
Como a categoria está cifrada, agrupar/ordenar por ela em SQL é inviável. `ListActivity` recebe a
lista filtrada (mesmo caminho da busca atual) e a transforma em linhas tipadas
`Header(categoria) + Item(entrada)`, ordenando categorias por `LabelSearch.normalize` e colocando
"Sem categoria" por último. Um `BaseAdapter` com **dois view types** (cabeçalho não clicável /
entrada clicável) substitui o `ArrayAdapter` atual; o clique mapeia a posição → `EntrySummary` via
a lista achatada (substitui o índice direto `filtered[pos]`).
- *Por quê:* reaproveita `Vault.list()` + `LabelSearch` (em memória, sessão desbloqueada) sem tocar
  no modelo de segurança; `ListView` com `getViewTypeCount()` é SDK puro.
- *Alternativa:* `ExpandableListView` — mais cerimônia para um agrupamento simples; descartada.

### D6 — Autocomplete com `AutoCompleteTextView` (SDK)
Campo "Categoria (opcional)" em `AddEditActivity` usando `android.widget.AutoCompleteTextView` com
`ArrayAdapter` alimentado por `Vault.categories()`, estilizado por um helper em `Theme.kt`.
- *Por quê:* autocomplete nativo do SDK, zero dependências; coerente com os campos existentes.

## Risks / Trade-offs

- [Agrupar em memória custa O(n) por refresh/keystroke] → n é pequeno (cofre pessoal) e já é o custo
  atual da busca; reutiliza a mesma normalização. Sem regressão prática.
- [Duplicatas por grafia: "Banco" vs "banco"] → deduplicação/agrupamento por `normalize`; exibe a
  primeira grafia encontrada. Aceitável; não há renomear em massa (Non-Goal).
- [Clique por posição com lista heterogênea (headers+items)] → adapter expõe `getItem`/tipo por
  posição e marca cabeçalhos como não selecionáveis (`isEnabled=false`), evitando clique em header.
- [Migração executada parcial] → `ALTER TABLE ADD COLUMN` é atômico no SQLite; nullable garante
  compatibilidade mesmo se nenhuma entrada tiver categoria.
- [Vazamento de categoria em logs] → categoria tratada como sensível; não logar; `PlainEntry.toString`
  continua mascarando a senha (categoria não é mais sensível que o rótulo já exibido).

## Migration Plan

1. Bump `DB_VERSION` 2→3 e adicionar a coluna em `onCreate` (novos cofres) e `onUpgrade` (existentes).
2. Estender camada de dados (`EncryptedEntry`, `VaultRepository` CRUD) para ler/gravar `category`.
3. Estender `Vault` (`add/update/list/detail`, `EntrySummary`, `PlainEntry`, novo `categories()`).
4. UI: campo de categoria com autocomplete em `AddEditActivity`; adapter agrupado em `ListActivity`.
5. **Rollback:** a coluna é aditiva/nullable; reverter o app para a versão anterior continua lendo o
   banco (a coluna extra é ignorada). Não há downgrade destrutivo de schema.

## Open Questions

- Nenhuma pendente — cardinalidade, origem das categorias e modelo de visualização foram decididos
  no refinamento (uma categoria por senha; sugestões derivadas; lista agrupada com cabeçalhos).
