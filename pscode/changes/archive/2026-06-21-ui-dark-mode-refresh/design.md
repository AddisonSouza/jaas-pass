## Context

O jaas-pass é um gerenciador de senhas 100% offline para Android, construído com **UI inteiramente programática em Kotlin** (não há `res/layout`, `res/values` nem temas custom em XML) sob uma constraint inegociável: **zero dependências de terceiros** — apenas `android.*` / `java.*` / `javax.crypto.*` + Kotlin stdlib, sem AndroidX/Jetpack/Material.

Estado atual:
- Tema definido no manifesto: `@android:style/Theme.Material.Light.NoActionBar` (claro).
- Todas as Activities estendem `SecureActivity` (aplica `FLAG_SECURE`, auto-lock via `onUserInteraction`, helper `dp()`), e montam `LinearLayout`/`EditText`/`Button`/`TextView` nativos sem estilização, alinhados à esquerda.
- Toggle mostrar/ocultar existe apenas em `DetailActivity`, sobre um `TextView` (não em campo editável).
- `AddEditActivity` usa `InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD` no campo Senha → a senha aparece em texto claro sempre, sem máscara.

Telas afetadas: `UnlockActivity` (Desbloquear + Criar cofre), `ListActivity`, `DetailActivity`, `AddEditActivity`.

## Goals / Non-Goals

**Goals:**
- Modo escuro por padrão, sem alternância de tema claro.
- Visual moderno e centralizado, consistente entre as 4 telas.
- Botão de mostrar/ocultar (olho) em **todos** os campos de senha, com estado inicial mascarado e preservação do cursor.
- Centralizar a aparência em um único `Theme.kt` (paleta, espaçamento, raio, fábricas de widgets), eliminando estilo duplicado.
- Preservar 100% da segurança e dos fluxos existentes (FLAG_SECURE, auto-lock, não persistência, falha segura, offline).

**Non-Goals:**
- Não adicionar dependências (sem AndroidX/Material).
- Não criar recursos XML (`res/values/colors|themes`) — manter UI programática para coerência com a base atual. (Decisão D1 explica.)
- Não alterar cripto, modelo de dados, sessão ou permissões.
- Não implementar alternância de tema, seguir o tema do sistema, nem personalização de cor pelo usuário.

## Decisions

### D1 — Estilização programática centralizada em `Theme.kt`, não em XML
Criar `com.jaaspass.ui.Theme` como `object` Kotlin contendo:
- **Paleta escura** (`Int` ARGB): `background`, `surface` (cartão), `primary`/accent, `onBackground`, `onSurface`, `hint`, `danger`.
- **Tokens**: espaçamentos (`space(n)` ou constantes em dp) e raio de canto.
- **Fábricas de widgets** que recebem um `Context` e devolvem views estilizadas:
  - `card(context)` → `LinearjLayout` vertical com fundo `surface` arredondado (`GradientDrawable`), centralizado com largura máxima.
  - `primaryButton(context, text)` / `secondaryButton(...)` → `Button` com fundo arredondado de acento e texto contrastante.
  - `input(context, hint, inputType)` → `EditText` estilizado (fundo arredondado, cor de texto/hint).
  - `passwordField(context, hint)` → row horizontal com `input` (mascarado) + botão de olho acoplado (ver D3).
  - `titleText` / `bodyText` helpers.

**Por que `object` programático e não XML?** A base inteira já é programática; introduzir `res/values` + temas XML criaria dois paradigmas de estilo concorrentes e divergiria com o tempo. Um único `object` Kotlin é o ponto de verdade mais simples e coerente, e contorna a ausência de Material3 (que exigiria AndroidX). **Alternativa considerada:** `res/values/colors.xml` + `themes.xml` com `@android:style` — rejeitada por fragmentar o estilo e não cobrir cantos arredondados/fábricas de widget sem mais boilerplate.

### D2 — Tema escuro nativo no manifesto
Trocar `android:theme` de `Theme.Material.Light.NoActionBar` para **`@android:style/Theme.Material.NoActionBar`** (variante escura nativa). Isso garante chrome do sistema (diálogos como o `AlertDialog` de exclusão, seleção de texto, cursor) coerente com o fundo escuro sem precisar estilizar cada diálogo. As cores finas de conteúdo vêm de `Theme.kt`.

**Alternativa:** manter o tema claro e pintar tudo manualmente — rejeitada porque `AlertDialog`/menus do sistema continuariam claros, quebrando a consistência.

### D3 — Toggle de visibilidade preservando cursor
O botão de olho alterna o `inputType` do `EditText` entre:
- Oculto: `TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_PASSWORD`
- Visível: `TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_VISIBLE_PASSWORD`

Trocar `inputType` reseta o cursor para 0; portanto, após a troca, **reaplicar `setSelection(selectionEnd)`** salvo antes da alteração. Também reaplicar a `typeface` (a troca pode forçar fonte monoespaçada). Estado inicial **mascarado** em todos os campos — inclusive em `AddEditActivity`, que hoje mostra a senha em texto claro (mudança de comportamento desejada).

O olho é um `Button`/`TextView` clicável compacto (texto "Mostrar"/"Ocultar" ou um glifo `👁`/`🙈`) dentro de uma row horizontal; sem ícones de drawable para não criar assets. A lógica de toggle vive em `Theme.passwordField(...)` para não duplicar em 4 lugares.

**Segurança:** revelar não persiste nada; `FLAG_SECURE` (em `SecureActivity`) cobre captura de tela. Na autenticação mestra, a extração da senha continua via `EditText.extractChars()` (`CharArray` + zeragem) — o toggle não interfere nisso.

### D4 — Escopo de refatoração por tela
Cada Activity passa a montar seu conteúdo via fábricas do `Theme`: container `card()` central, títulos/campos/botões estilizados. `SecureActivity.dp()` permanece; tokens de espaçamento podem reutilizá-lo. `DetailActivity` mantém seu toggle de revelar (sobre `TextView`) — opcionalmente alinhado ao novo estilo, mas o requisito de olho-em-campo se aplica a campos **editáveis**.

## Risks / Trade-offs

- **[Troca de `inputType` reposiciona cursor / muda fonte]** → salvar `selectionStart/End` e `typeface` antes, reaplicar depois; cobrir com cenário de teste (cursor preservado).
- **[`Theme.Material` (dark) pode ter contraste insuficiente em alguns elementos nativos]** → definir cores de conteúdo explicitamente no `Theme.kt` e validar contraste no APK de debug; ajustar paleta no refinamento.
- **[Mudança de comportamento: senha em Add/Edit agora nasce mascarada]** → intencional e documentado na spec; comunicar no PR.
- **[`AlertDialog` nativo segue o tema do app]** → ao usar `Theme.Material.NoActionBar`, o diálogo de exclusão fica escuro automaticamente; validar visualmente.
- **[Regressão visual sem testes de UI automatizados]** → validação manual via `./gradlew :app:assembleDebug` + sideload nas 4 telas; lógica pura testável (ex.: cálculo de toggle/seleção) coberta por `kotlin.test` onde fizer sentido.

## Migration Plan

1. Adicionar `Theme.kt` (sem mudar telas) — compila isolado.
2. Trocar `android:theme` no manifesto para o tema escuro.
3. Migrar tela a tela para as fábricas do `Theme` (Unlock → List → Detail → AddEdit), compilando a cada passo.
4. Introduzir `passwordField` com olho nos campos de senha.
5. `./gradlew :app:assembleDebug`, sideload e validação visual das 4 telas.

**Rollback:** reverter o atributo `android:theme` e os arquivos de tela; `Theme.kt` é aditivo e pode ser removido sem afetar dados/cripto. Nenhuma migração de dados envolvida.

## Open Questions

- Glifo do olho (emoji `👁/🙈` vs. texto "Mostrar/Ocultar"): emoji é mais compacto, texto é mais explícito. Decidir no refinamento/implementação; default proposto: texto curto para garantir legibilidade sem assets.
- Cor de acento exata da paleta (ex.: teal/indigo) — ajustável no `Theme.kt` durante a validação visual.
