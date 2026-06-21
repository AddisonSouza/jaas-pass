## 1. Gerador no módulo vault-crypto

- [x] 1.1 Criar `vault-crypto/src/main/kotlin/com/jaaspass/crypto/PasswordGenerator.kt` com a classe `PasswordGenerator(secureRandom: SecureRandom = SecureRandom())` e `DEFAULT_LENGTH = 20`.
- [x] 1.2 Definir os pools de categorias sem caracteres ambíguos (maiúsc. sem `I/O`, minúsc. sem `l`, dígitos `2-9`, símbolos seguros) e o alfabeto unificado.
- [x] 1.3 Implementar `generate(length)`: garantir ≥1 de cada categoria, preencher o restante do alfabeto unificado e embaralhar com Fisher–Yates; toda seleção via `secureRandom.nextInt(bound)` (sem viés de módulo). Retornar `CharArray`.
- [x] 1.4 Validar parâmetro: `length` deve ser ≥ número de categorias (4); senão lançar `IllegalArgumentException`.

## 2. Testes do gerador

> Nota: o módulo `vault-crypto` é zero-deps até nos testes (constraint §1) — não usa `kotlin.test`/JUnit.
> Os testes seguem o harness próprio existente (`main()` + task `JavaExec`), igual ao `CryptoSelfTest`.

- [x] 2.1 Criar self-test (`PasswordGeneratorSelfTest.kt`, harness próprio) cobrindo: comprimento padrão = 20 (Scenario "comprimento padrão") + `length` inválido lança `IllegalArgumentException`.
- [x] 2.2 Testar presença das 4 categorias e ausência de caracteres ambíguos `O/0/l/1/I` (Scenarios "todas as categorias" e "não contém ambíguos").
- [x] 2.3 Testar unicidade entre gerações consecutivas (Scenario "cada geração é única").
- [x] 2.4 Testar distribuição aproximadamente uniforme sobre o alfabeto em amostra grande (Scenario "distribuição uniforme") e o embaralhamento das posições garantidas (Scenario "posição não previsível").

## 3. Integração na UI (AddEditActivity)

- [x] 3.1 Em `AddEditActivity`, adicionar `Theme.secondaryButton(this, "Gerar senha")` logo após `passField.view` no `card`.
- [x] 3.2 No clique: usar `PasswordGenerator`, gerar a senha, fazer `passField.edit.setText(...)`, posicionar o cursor no fim e revelar o valor (`PasswordField.reveal()`) para conferência (Scenarios "botão preenche o campo" e "regerar substitui").
- [x] 3.3 Garantir que o fluxo de salvar (`save()`) continua funcionando com a senha gerada, sem alterações no contrato de `vault.add/update`.

## 4. Validação

- [x] 4.1 Rodar os self-tests do módulo: `./gradlew :vault-crypto:passwordGenSelfTest` (7/7 verdes).
- [x] 4.2 Build do APK debug: `./gradlew :app:assembleDebug` sem novas dependências.
- [x] 4.3 Sideload e verificação manual: Nova entrada → "Gerar senha" preenche e revela senha forte de 20 chars; salvar e reabrir a entrada confirma persistência.
