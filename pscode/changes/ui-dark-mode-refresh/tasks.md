## 1. Tema central (Theme.kt)

- [x] 1.1 Criar `app/src/main/java/com/jaaspass/ui/Theme.kt` como `object` com a paleta escura (ARGB `Int`): `windowBg`, `surface`, `accent`, `onBackground`, `onSurfaceMuted`, `hintColor`, `danger` (renomeados para evitar colisão com membros de `View` dentro de `apply{}`).
- [x] 1.2 Adicionar tokens de espaçamento (`SPACE`) e raio de canto (`RADIUS`) em dp, e helper `roundedBg()` com `GradientDrawable`.
- [x] 1.3 Implementar fábricas `screen(context)` (fundo + centralização) e `card(context)`: `LinearLayout` vertical, fundo `surface` arredondado, largura máxima (`CARD_MAX_WIDTH`) e padding consistente.
- [x] 1.4 Implementar `primaryButton(context, text)` e `secondaryButton(context, text, destructive)`: `Button` com fundo arredondado (acento/superfície) e cor de texto contrastante.
- [x] 1.5 Implementar `input(context, hint, inputType)`: `EditText` estilizado (fundo arredondado, cores de texto/hint da paleta).
- [x] 1.6 Implementar helpers de texto `titleText(context, text)` e `bodyText(context, text, muted)` com cores/tamanhos da paleta.
- [x] 1.7 Compilar isolado (`./gradlew :app:assembleDebug`) garantindo que `Theme.kt` não quebra o build.

## 2. Modo escuro por padrão

- [x] 2.1 Trocar `android:theme` em `AndroidManifest.xml` de `@android:style/Theme.Material.Light.NoActionBar` para `@android:style/Theme.Material.NoActionBar`.
- [ ] 2.2 Build + sideload e verificar visualmente que diálogos do sistema (ex.: `AlertDialog` de exclusão) e seleção de texto ficam escuros e legíveis. _(fase de testes)_

## 3. Campo de senha com olho (mostrar/ocultar)

- [x] 3.1 Implementar `passwordField(context, hint)` em `Theme.kt`: row horizontal com `input` mascarado (`TYPE_TEXT_VARIATION_PASSWORD`) + botão de olho acoplado; estado inicial mascarado. Retorna `PasswordField`.
- [x] 3.2 Implementar a lógica de toggle alternando `inputType`, salvando e reaplicando `selectionStart/End` e `typeface` para preservar cursor e fonte (extraída em helpers puros `passwordInputType`/`clampSelection`).
- [x] 3.3 Expor a partir do `passwordField` o `EditText` interno (`PasswordField.edit`, para `extractChars()`/leitura) sem vazar o controle de toggle; rótulo do botão alterna "Mostrar"/"Ocultar".
- [x] 3.4 (Validação spec `password-visibility`) **AJUSTADO**: a lógica de toggle foi isolada em funções puras (`passwordInputType`/`clampSelection`); teste `kotlin.test` não foi adicionado porque o repo segue convenção de **zero deps de teste** (`:vault-crypto` usa harness próprio) e o toggle depende de `EditText`/`InputType` (precisaria de Robolectric ou emulador, indisponível). Validação do cursor/visibilidade fica manual na tarefa 5.2.

## 4. Restyling das telas

- [x] 4.1 `UnlockActivity` — Desbloquear: montar com `card()` central, `titleText`, `passwordField("Senha mestra")` (olho) e `primaryButton("Desbloquear")`; preservar `extractChars()` + zeragem do `CharArray`.
- [x] 4.2 `UnlockActivity` — Criar cofre: `card()` central com aviso, `passwordField("Defina a senha mestra")` e `passwordField("Confirme a senha mestra")` (ambos com olho) e `primaryButton("Criar cofre")`; manter validações (MIN_LEN, coincidência) e zeragem.
- [x] 4.3 `ListActivity`: header e ações com widgets do `Theme` (título, `primaryButton("+ Adicionar")`, `secondaryButton("Bloquear")`), conteúdo centralizado; manter `ListView`/empty-state e fluxo de auto-lock.
- [x] 4.4 `AddEditActivity`: `card()` central, `input` para rótulo/usuário e `passwordField("Senha")` (olho, **inicia mascarado** — substitui o `VISIBLE_PASSWORD` atual); manter validação e `vault.add/update`.
- [x] 4.5 `DetailActivity`: aplicar paleta/widgets do `Theme` aos botões e textos; manter o toggle de revelar sobre o `TextView` e o `AlertDialog` de exclusão coerentes com o tema escuro.

## 5. Validação e fechamento

- [x] 5.1 `./gradlew :app:assembleDebug` sem erros; confirmado que nenhuma dependência nova foi adicionada e que o manifesto não declara novas permissões (constraints zero-deps/offline).
- [ ] 5.2 Sideload e validação visual das 4 telas: fundo escuro, conteúdo centralizado, contraste legível e olho funcionando em todos os campos de senha (Senha mestra, Definir, Confirmar, Senha).
- [ ] 5.3 Confirmar segurança preservada: `FLAG_SECURE` ativo (screenshot bloqueado) e senha mestra ainda lida como `CharArray` e zerada após uso, mesmo após revelar/ocultar.
