## 1. Tema central (Theme.kt)

- [ ] 1.1 Criar `app/src/main/java/com/jaaspass/ui/Theme.kt` como `object` com a paleta escura (ARGB `Int`): `background`, `surface`, `primary`/accent, `onBackground`, `onSurface`, `hint`, `danger`.
- [ ] 1.2 Adicionar tokens de espaçamento e raio de canto (em dp), reutilizando `dp()` onde fizer sentido; expor helper de raio para `GradientDrawable`.
- [ ] 1.3 Implementar fábrica `card(context)`: `LinearLayout` vertical, fundo `surface` arredondado via `GradientDrawable`, centralizado horizontalmente com largura máxima e padding interno consistente.
- [ ] 1.4 Implementar `primaryButton(context, text)` e `secondaryButton(context, text)`: `Button` com fundo arredondado (acento/superfície) e cor de texto contrastante.
- [ ] 1.5 Implementar `input(context, hint, inputType)`: `EditText` estilizado (fundo arredondado, cores de texto/hint da paleta).
- [ ] 1.6 Implementar helpers de texto `titleText(context, text)` e `bodyText(context, text)` com cores/tamanhos da paleta.
- [ ] 1.7 Compilar isolado (`./gradlew :app:assembleDebug`) garantindo que `Theme.kt` não quebra o build (ainda sem uso nas telas).

## 2. Modo escuro por padrão

- [ ] 2.1 Trocar `android:theme` em `AndroidManifest.xml` de `@android:style/Theme.Material.Light.NoActionBar` para `@android:style/Theme.Material.NoActionBar`.
- [ ] 2.2 Build + sideload e verificar visualmente que diálogos do sistema (ex.: `AlertDialog` de exclusão) e seleção de texto ficam escuros e legíveis.

## 3. Campo de senha com olho (mostrar/ocultar)

- [ ] 3.1 Implementar `passwordField(context, hint)` em `Theme.kt`: row horizontal com `input` mascarado (`TYPE_TEXT_VARIATION_PASSWORD`) + botão de olho acoplado; estado inicial mascarado.
- [ ] 3.2 Implementar a lógica de toggle alternando `inputType` entre `TYPE_TEXT_VARIATION_PASSWORD` e `TYPE_TEXT_VARIATION_VISIBLE_PASSWORD`, salvando e reaplicando `selectionStart/End` e `typeface` para preservar cursor e fonte.
- [ ] 3.3 Expor a partir do `passwordField` o `EditText` interno (para `extractChars()`/leitura) sem vazar o controle de toggle; rótulo do botão alterna "Mostrar"/"Ocultar".
- [ ] 3.4 (Validação spec `password-visibility`) Teste `kotlin.test` para a função de toggle: dado texto e posição de cursor, após alternar a seleção é preservada e o `inputType` corresponde ao estado esperado.

## 4. Restyling das telas

- [ ] 4.1 `UnlockActivity` — Desbloquear: montar com `card()` central, `titleText`, `passwordField("Senha mestra")` (olho) e `primaryButton("Desbloquear")`; preservar `extractChars()` + zeragem do `CharArray`.
- [ ] 4.2 `UnlockActivity` — Criar cofre: `card()` central com aviso, `passwordField("Defina a senha mestra")` e `passwordField("Confirme a senha mestra")` (ambos com olho) e `primaryButton("Criar cofre")`; manter validações (MIN_LEN, coincidência) e zeragem.
- [ ] 4.3 `ListActivity`: header e ações com widgets do `Theme` (título, `primaryButton("+ Adicionar")`, `secondaryButton("Bloquear")`), conteúdo centralizado; manter `ListView`/empty-state e fluxo de auto-lock.
- [ ] 4.4 `AddEditActivity`: `card()` central, `input` para rótulo/usuário e `passwordField("Senha")` (olho, **inicia mascarado** — substitui o `VISIBLE_PASSWORD` atual); manter validação e `vault.add/update`.
- [ ] 4.5 `DetailActivity`: aplicar paleta/widgets do `Theme` aos botões e textos; manter o toggle de revelar sobre o `TextView` e o `AlertDialog` de exclusão coerentes com o tema escuro.

## 5. Validação e fechamento

- [ ] 5.1 `./gradlew :app:assembleDebug` sem erros; confirmar que nenhuma dependência nova foi adicionada e que o manifesto não declara novas permissões (constraints zero-deps/offline).
- [ ] 5.2 Sideload e validação visual das 4 telas: fundo escuro, conteúdo centralizado, contraste legível e olho funcionando em todos os campos de senha (Senha mestra, Definir, Confirmar, Senha).
- [ ] 5.3 Confirmar segurança preservada: `FLAG_SECURE` ativo (screenshot bloqueado) e senha mestra ainda lida como `CharArray` e zerada após uso, mesmo após revelar/ocultar.
