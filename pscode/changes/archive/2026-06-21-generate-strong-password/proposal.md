## Why

Hoje, ao adicionar uma entrada, o usuário precisa inventar e digitar a senha à mão — o que tende a produzir senhas curtas, reutilizadas ou previsíveis, justamente o problema que um gerenciador de senhas existe para resolver. Oferecer um gerador embutido de senhas fortes torna o caminho seguro o caminho mais fácil, sem depender de nenhuma ferramenta externa (coerente com o offline absoluto do app).

## What Changes

- Novo gerador de senhas forte, **puro e testável**, no módulo `vault-crypto`, usando o `SecureRandom` já presente ali (mesma fonte de aleatoriedade da criptografia).
- Política fixa e segura por padrão: **20 caracteres**, com pelo menos um de cada categoria (maiúsculas, minúsculas, dígitos, símbolos), **excluindo caracteres ambíguos** (`O/0`, `l/1/I`).
- Seleção **sem viés de módulo** (`SecureRandom.nextInt(bound)`) e embaralhamento final Fisher–Yates, para não vazar a posição das categorias garantidas.
- Novo gatilho na UI: botão **"Gerar senha"** na tela de Nova/Editar entrada (`AddEditActivity`), que preenche o campo de senha e **revela** o valor gerado para conferência.
- Reaproveita os componentes existentes (`Theme.secondaryButton`, `PasswordField`) — sem nova dependência, sem novo tema.

## Capabilities

### New Capabilities
- `password-generation`: geração local de senhas fortes sob demanda (política de comprimento/categorias, ausência de viés, integração com o campo de senha da entrada).

### Modified Capabilities
<!-- Nenhum requisito de capability existente muda. A integração na tela de entrada
     adiciona um gatilho de UI, mas não altera o contrato de `entry-management` nem de
     `password-visibility` (o reveal pós-geração reusa o comportamento já especificado). -->

## Impact

- **Código novo:** `vault-crypto/.../PasswordGenerator.kt` (gerador puro) + teste unitário em `kotlin.test`.
- **Código alterado:** `app/.../ui/AddEditActivity.kt` (adicionar botão e ação de gerar) — chamando o gerador e preenchendo `passField`.
- **Dependências:** nenhuma nova; apenas `java.security.SecureRandom` e Kotlin stdlib (respeita o constraint zero-deps).
- **Rede/permize:** nenhuma — operação 100% local e offline.
- **Segurança:** usa a mesma fonte (`SecureRandom`) da cripto; senha gerada vive apenas em memória no `EditText` até o save normal da entrada.
