## 1. Utilitário de normalização

- [x] 1.1 Adicionar uma função de normalização para casamento insensível a caixa/acento (`Normalizer.normalize(NFD)` → remover `\p{Mn}+` → `lowercase(Locale.ROOT)`), em local reutilizável (ex.: `Theme.kt` ou um pequeno arquivo util na `ui`). Valida o cenário "Ignora caixa" e "Ignora acento" da spec.
- [x] 1.2 Cobrir a normalização com teste unitário (`kotlin.test`): `gmail`↔`Gmail`, `joao`↔`João`, substring no meio (`bank` em `My Bank App`). Valida os cenários de casamento da spec.

## 2. UI de busca na lista

- [x] 2.1 Criar helper `Theme.searchField(context, hint)` (EditText de linha única, `inputType=text`, estilo do tema) se não houver um adequado. Valida o requisito "Busca de entradas por rótulo" (entrada da UI).
- [x] 2.2 Em `ListActivity`, inserir o campo de busca no card (entre header e/ou botão "+ Adicionar") e manter referência.

## 3. Filtragem em memória

- [x] 3.1 Em `ListActivity`, separar `allEntries` (resultado de `vault.list()`) de `filtered` (lista exibida); `refresh()` recarrega `allEntries` e reaplica o filtro com o termo atual. Valida "Filtrar enquanto digita" e "Busca vazia mostra tudo".
- [x] 3.2 Conectar um `TextWatcher.afterTextChanged` que recomputa `filtered` (substring normalizada) e atualiza o adapter do `ListView`.
- [x] 3.3 Ajustar o clique para mapear `filtered[pos].id` (não a lista original). Valida o cenário de abrir o detalhe correto após filtrar.
- [x] 3.4 Diferenciar o estado vazio: "Nenhuma entrada ainda." (cofre vazio) vs "Nenhum resultado." (filtro sem casamento); visibilidade de `listView`/`empty` baseada em `filtered`. Valida "Estado vazio de resultados".

## 4. Garantias de segurança

- [x] 4.1 Confirmar que a busca só opera com sessão desbloqueada (reusa o guard de `onResume`) e que o termo NÃO é persistido (sem `onSaveInstanceState`; `allowBackup=false`). Valida "Some ao bloquear" e "Busca não enfraquece o modelo de segurança".
- [x] 4.2 Confirmar (revisão/manual) que o filtro usa apenas `EntrySummary.label` — `username`/`password` nunca são tocados. Valida "Busca não alcança senha nem usuário".

## 5. Verificação

- [x] 5.1 `./gradlew :app:assembleDebug` compila sem warnings novos; rodar os testes unitários.
- [x] 5.2 Sideload do APK e verificação manual: digitar filtra ao vivo, ignora caixa/acento, estado vazio aparece, limpar campo restaura tudo, e abrir um item filtrado leva ao detalhe certo.
