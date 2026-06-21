## 1. Gerador no módulo vault-crypto

- [ ] 1.1 Criar `vault-crypto/src/main/kotlin/com/jaaspass/crypto/PasswordGenerator.kt` com a classe `PasswordGenerator(secureRandom: SecureRandom = SecureRandom())` e `DEFAULT_LENGTH = 20`.
- [ ] 1.2 Definir os pools de categorias sem caracteres ambíguos (maiúsc. sem `I/O`, minúsc. sem `l`, dígitos `2-9`, símbolos seguros) e o alfabeto unificado.
- [ ] 1.3 Implementar `generate(length)`: garantir ≥1 de cada categoria, preencher o restante do alfabeto unificado e embaralhar com Fisher–Yates; toda seleção via `secureRandom.nextInt(bound)` (sem viés de módulo). Retornar `CharArray`.
- [ ] 1.4 Validar parâmetro: `length` deve ser ≥ número de categorias (4); senão lançar `IllegalArgumentException`.

## 2. Testes do gerador

- [ ] 2.1 Criar teste unitário (`kotlin.test`) cobrindo: comprimento padrão = 20 (Scenario "comprimento padrão").
- [ ] 2.2 Testar presença das 4 categorias e ausência de caracteres ambíguos `O/0/l/1/I` (Scenarios "todas as categorias" e "não contém ambíguos").
- [ ] 2.3 Testar unicidade entre gerações consecutivas (Scenario "cada geração é única").
- [ ] 2.4 Testar distribuição aproximadamente uniforme sobre o alfabeto em amostra grande (Scenario "distribuição uniforme") e o embaralhamento das posições garantidas (Scenario "posição não previsível"), usando `SecureRandom` semeado/determinístico se necessário para reprodutibilidade.

## 3. Integração na UI (AddEditActivity)

- [ ] 3.1 Em `AddEditActivity`, adicionar `Theme.secondaryButton(this, "Gerar senha")` logo após `passField.view` no `card`.
- [ ] 3.2 No clique: instanciar/usar `PasswordGenerator`, gerar a senha, fazer `passField.edit.setText(...)`, posicionar o cursor no fim e revelar o valor para conferência (Scenarios "botão preenche o campo" e "regerar substitui").
- [ ] 3.3 Garantir que o fluxo de salvar (`save()`) continua funcionando com a senha gerada, sem alterações no contrato de `vault.add/update`.

## 4. Validação

- [ ] 4.1 Rodar os testes do módulo: `./gradlew :vault-crypto:test` (todos verdes).
- [ ] 4.2 Build do APK debug: `./gradlew :app:assembleDebug` sem novas dependências.
- [ ] 4.3 Sideload e verificação manual: Nova entrada → "Gerar senha" preenche e revela senha forte de 20 chars; salvar e reabrir a entrada confirma persistência.
