## Why

À medida que o cofre cresce, uma lista linear de entradas mistura senhas de contextos diferentes
(banco, trabalho, pessoal) e dificulta a navegação. O usuário quer **agrupar** as senhas por uma
categoria própria (ex.: "Banco") e vê-las **separadas na visualização**, escolhendo a categoria de
forma reutilizável a partir das que já cadastrou. É a evolução natural da listagem agora que
criação/edição, busca por rótulo e desbloqueio já estão estáveis.

> **Nota de terminologia:** neste projeto "rótulo" (`label`) já é o **nome da própria entrada**
> (ex.: "Gmail") e a busca por rótulo já existe (`entry-search`). O conceito novo aqui é a
> **categoria** — um agrupador opcional escolhido pelo usuário. Para evitar colisão, usamos
> "categoria" em código e UI; "rótulo" continua significando o nome da entrada.

## What Changes

- Uma entrada passa a ter uma **categoria opcional** (0 ou 1) — escolhida na tela de adicionar/editar
  por um campo com **autocomplete** das categorias já usadas (digitar pela 1ª vez "cadastra" a
  categoria; não há tela nem tabela de gerenciamento).
- A tela do cofre (`ListActivity`) passa a exibir as entradas **agrupadas por cabeçalhos de
  categoria**, com um grupo **"Sem categoria"** sempre ao final. Categorias ordenadas de forma
  insensível a caixa/acentos; entradas dentro do grupo mantêm a ordem atual (`updated_at DESC`).
- A **busca por rótulo já existente continua funcionando** e compõe com o agrupamento: o termo
  filtra as entradas e os grupos resultantes são reexibidos (grupos sem resultado somem).
- A categoria é **cifrada em repouso** com a DEK, igual a rótulo/usuário/senha — uma categoria como
  "Banco" é potencialmente sensível; em ambiguidade de segurança, a opção mais conservadora.
- O conjunto de categorias para sugestão é **derivado em memória** das entradas já decifradas por
  `Vault.list()` (sessão desbloqueada), normalizado para deduplicar (sem acento/caixa). Nada novo é
  persistido além do blob cifrado da categoria.
- **Migração de schema** SQLite v2→v3: nova coluna `category` (BLOB, nullable) aditiva; cofres
  existentes seguem funcionando com a coluna NULL = "Sem categoria".
- Sem novas permissões, sem rede, sem dependências de terceiros.

## Capabilities

### New Capabilities
- `entry-categorization`: atribuição de uma categoria opcional por entrada (cifrada em repouso),
  sugestão das categorias já usadas com casamento insensível a caixa/acentos, e visualização da
  lista agrupada por categoria com um grupo "Sem categoria" ao final.

### Modified Capabilities
- `entry-management`: a criação e a edição de entrada passam a aceitar uma **categoria opcional**;
  a persistência cifra também esse campo com a DEK e a listagem o decifra em memória.
- `entry-search`: a busca por rótulo passa a **compor com o agrupamento** — os resultados filtrados
  são apresentados dentro dos cabeçalhos de categoria, em vez de uma lista plana.

## Impact

- **UI**: `app/src/main/java/com/jaaspass/ui/AddEditActivity.kt` — novo campo de categoria com
  sugestões (`android.widget.AutoCompleteTextView`, SDK puro). `ListActivity.kt` — adapter com
  cabeçalhos de seção (dois tipos de view: cabeçalho + entrada) sobre o resultado filtrado.
  `Theme.kt` — possível helper de campo com autocomplete estilizado.
- **Dados/cripto**: `EncryptedEntry` ganha `category: ByteArray?`; `Vault.add/update/list/detail` e
  `Vault.EntrySummary`/`PlainEntry` ganham `category`; novo `Vault.categories()` (distinct em
  memória). `VaultRepository` + `VaultDbHelper`: coluna `category` e migração `onUpgrade` v2→v3
  (DB_VERSION 2→3). Reuso de `com.jaaspass.search.LabelSearch.normalize` (`:vault-crypto`) para
  normalizar/agrupar/deduplicar categorias.
- **Dependências**: nenhuma nova (apenas SDK Android + Kotlin stdlib).
- **Segurança/permissões**: sem alteração de modelo — categoria cifrada com AES-256-GCM/DEK, só em
  memória com sessão desbloqueada, falha segura; migração apenas aditiva e nullable.
