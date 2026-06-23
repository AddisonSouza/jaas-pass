## Context

A `ListActivity` exibe o cofre carregando `Vault.list()` — que decifra **apenas os rótulos** em
memória (sessão desbloqueada) e devolve `List<EntrySummary>(id, label)`. A lista é renderizada com
um `ArrayAdapter<String>` sobre `android.R.layout.simple_list_item_1`. O clique mapeia a posição
para `entries[pos].id` e abre o `DetailActivity`.

Como os rótulos estão **cifrados em disco** (AES-256-GCM/DEK), não existe — e não deve existir —
índice ou query textual no banco. Qualquer busca tem de acontecer sobre a coleção já decifrada em
memória. O projeto também proíbe dependências de terceiros e exige modo offline absoluto.

## Goals / Non-Goals

**Goals:**
- Filtrar a lista de entradas pelo rótulo, em tempo real, conforme o usuário digita.
- Casamento por substring, insensível a caixa e a acentos (pt-BR friendly).
- Manter o modelo de segurança intacto: só em sessão desbloqueada, nada persistido, escopo só rótulo.
- Zero dependências novas; apenas SDK Android + Kotlin stdlib.

**Non-Goals:**
- Buscar em `username`/`password` (decifrados apenas no detalhe).
- Busca fuzzy/ranqueamento/realce de trechos — apenas "contém".
- Persistir histórico ou último termo de busca.
- Paginação/índice — o volume de entradas pessoais é pequeno; filtragem linear é suficiente.

## Decisions

**D1 — Filtragem 100% em memória sobre `EntrySummary`.**
Manter a lista completa decifrada em um campo (`allEntries`) e derivar `filtered` a partir do termo.
O `ListView` passa a refletir `filtered`, e o clique mapeia `filtered[pos].id` (não mais
`entries[pos].id`). _Alternativa descartada:_ consultar o banco — impossível/insegura, pois os
rótulos são ciphertext.

**D2 — Normalização para casamento insensível a caixa/acento.**
Função utilitária: `Normalizer.normalize(s, NFD)` → remover marcas diacríticas via regex
`\p{Mn}+` → `lowercase()` (com `Locale.ROOT`). Aplicada ao rótulo e ao termo antes do
`contains`. Usa só `java.text.Normalizer` (SDK). _Alternativa descartada:_ `Collator` — mais
pesado e voltado a ordenação/comparação, menos direto para "contém".

**D3 — UI: `EditText` no topo do card, com `TextWatcher`.**
Inserir um `EditText` de linha única entre o header e o botão "+ Adicionar" (ou logo abaixo dele),
com `inputType=text`, hint "Buscar por rótulo". Um `TextWatcher.afterTextChanged` recomputa
`filtered` e troca o adapter (ou usa `notifyDataSetChanged` sobre uma lista mutável). Reutiliza o
estilo via helper em `Theme.kt` (criar `Theme.searchField(...)` se não houver um `EditText`
adequado), seguindo o padrão dos demais helpers de tema.

**D4 — Reaproveitar `refresh()` e o ciclo de vida.**
`refresh()` recarrega `allEntries = vault.list()` e reaplica o filtro com o termo atual. Em
`onResume`, se a sessão estiver bloqueada, o fluxo existente já redireciona ao desbloqueio — o
termo em memória é descartado naturalmente com a Activity. Nada novo a persistir.

**D5 — Estado vazio diferenciado.**
Reusar o `TextView empty`, ajustando a mensagem conforme o caso: "Nenhuma entrada ainda." quando o
cofre está vazio; "Nenhum resultado." quando há entradas mas o filtro não casa nada. A visibilidade
de `listView`/`empty` passa a depender de `filtered`, não de `allEntries`.

## Risks / Trade-offs

- [Filtragem linear a cada tecla pode pesar com muitas entradas] → Para um cofre pessoal o N é
  pequeno; a normalização é O(tamanho do rótulo). Se algum dia crescer, pré-computar o rótulo
  normalizado uma vez por carga (cache em paralelo a `allEntries`). Mitigação suficiente: normalizar
  o termo uma vez por digitação e os rótulos sob demanda.
- [Mapeamento de índice de clique quebra se continuar usando a lista original] → Garantir que o
  clique use **sempre** a lista filtrada atualmente exibida (`filtered[pos].id`).
- [Acentos compostos/diacríticos raros] → NFD + remoção de `\p{Mn}` cobre o caso pt-BR usual; casos
  exóticos degradam para "não casa", nunca para erro.
- [Vazamento do termo via estado da Activity] → `EditText` não persiste fora do processo; sem
  `android:allowBackup` (já `false`) e sem salvar em `onSaveInstanceState` o termo não vaza.

## Migration Plan

Mudança puramente aditiva na UI; sem migração de dados nem de schema. Rollback = reverter o commit
da `ListActivity`/`Theme`. Sem flags.

## Open Questions

Nenhuma — decisões de produto (escopo só rótulo; casamento sem caixa e sem acento) já confirmadas
no refinamento.
