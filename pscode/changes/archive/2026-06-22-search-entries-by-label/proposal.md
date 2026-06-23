## Why

À medida que o cofre cresce, encontrar uma entrada rolando a `ListView` fica lento e propenso a
erro. O usuário precisa localizar rapidamente uma senha pelo seu rótulo (nome do serviço) sem
precisar varrer a lista inteira. É a próxima melhoria natural de usabilidade da tela de listagem,
agora que criação/edição/desbloqueio já estão estáveis.

## What Changes

- Adicionar um campo de **busca** no topo da tela de lista (`ListActivity`) que filtra as entradas
  pelo **rótulo** conforme o usuário digita.
- A filtragem é **em memória**, sobre os rótulos já decifrados por `Vault.list()` — não há query no
  banco nem decriptação adicional. Isso preserva o modelo de segurança (rótulos cifrados em disco) e
  a constraint offline/zero-dependências.
- Casamento **sem distinção de caixa e sem distinção de acento** (ex.: `joao` encontra `João`),
  via normalização com `java.text.Normalizer` (apenas SDK).
- A busca cobre **somente o rótulo** — `username` e `password` continuam decifrados apenas no
  detalhe e ficam fora do escopo da busca.
- Estado da busca é **efêmero**: limpa naturalmente ao sair/bloquear (auto-lock) junto com a lista;
  nada novo é persistido.
- Sem novas permissões, sem mudança de schema do banco, sem dependências.

## Capabilities

### New Capabilities
- `entry-search`: busca/filtragem em memória das entradas do cofre pelo rótulo, em sessão
  desbloqueada, com casamento insensível a caixa e a acentos.

### Modified Capabilities
<!-- A listagem em si (entry-management) não muda seus requisitos: a busca apenas filtra o que já
     é listado. Sem alteração de requisito em specs existentes. -->

## Impact

- **UI**: `app/src/main/java/com/jaaspass/ui/ListActivity.kt` — adicionar campo de busca ao card e
  filtrar o adapter; `Theme.kt` pode receber um helper de `EditText` se ainda não houver um adequado.
- **Dados/cripto**: nenhum impacto — reusa `Vault.list()`; nada é decifrado a mais nem persistido.
- **Dependências**: nenhuma nova (usa `android.widget.EditText`, `android.text.TextWatcher`,
  `java.text.Normalizer`).
- **Segurança/permissões**: sem alteração — continua offline, sem rede, sem novas permissões.
