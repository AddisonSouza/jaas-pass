## Context

O `AddEditActivity` hoje exige que o usuário digite a senha manualmente (`passField`, um `PasswordField`). O módulo `vault-crypto` já encapsula toda a aleatoriedade segura do app via `CryptoManager(private val secureRandom: SecureRandom = SecureRandom())`, usando `secureRandom.nextBytes(...)` para salt/nonce/DEK. Esta change adiciona geração de senhas fortes reutilizando essa mesma infraestrutura, mantendo os constraints inegociáveis: zero dependências de terceiros, offline absoluto e postura conservadora em segurança.

## Goals / Non-Goals

**Goals:**
- Gerar, sob demanda, uma senha forte de 20 caracteres com todas as quatro categorias garantidas e sem caracteres ambíguos.
- Garantir aleatoriedade criptográfica (`SecureRandom`) e seleção sem viés de módulo.
- Expor um gatilho simples (um toque) na tela de nova/editar entrada que preenche e revela o campo de senha.
- Manter o gerador **puro e testável** com `kotlin.test`, sem depender de Android para a lógica de geração.

**Non-Goals:**
- Medidor/indicador de força de senha.
- Opções configuráveis pelo usuário (tamanho, incluir/excluir símbolos) — política é fixa nesta change.
- Histórico ou persistência de senhas geradas além do fluxo normal de salvar a entrada.
- Geração de passphrases (palavras) — apenas senhas de caracteres.

## Decisions

**1. Gerador no módulo `vault-crypto` (não na camada `ui`).**
Novo arquivo `PasswordGenerator.kt`. A lógica é independente de Android, então fica testável por unidade e ao lado do `CryptoManager`, que já é o dono do `SecureRandom`. Assinatura proposta:
```kotlin
class PasswordGenerator(private val secureRandom: SecureRandom = SecureRandom()) {
    fun generate(length: Int = DEFAULT_LENGTH): CharArray
    companion object { const val DEFAULT_LENGTH = 20 }
}
```
*Alternativa considerada:* colocar a função na `ui`. Rejeitada — acoplaria lógica testável a uma Activity e separaria a aleatoriedade da sua fonte natural.

**2. Política de alfabeto fixa, sem ambíguos.**
Quatro pools de categorias, já depurados dos ambíguos:
- Maiúsculas: `ABCDEFGHJKLMNPQRSTUVWXYZ` (sem `I`, `O`)
- Minúsculas: `abcdefghijkmnopqrstuvwxyz` (sem `l`)
- Dígitos: `23456789` (sem `0`, `1`)
- Símbolos: `!@#$%^&*()-_=+[]{}` (conjunto seguro, sem aspas/crases que quebram shells/CSV)

**3. Construção: garantir categorias + preencher + embaralhar.**
Algoritmo: (a) sorteia 1 caractere de cada um dos 4 pools (garante a composição); (b) preenche os caracteres restantes a partir do alfabeto unificado; (c) aplica **Fisher–Yates** sobre o array final, para que as posições garantidas não fiquem previsíveis no início. Toda escolha de índice usa `secureRandom.nextInt(bound)` — uniforme e **sem viés de módulo**.
*Alternativa considerada:* sortear tudo do alfabeto unificado e rejeitar/repetir até bater a política. Rejeitada — mais complexa e com tempo de execução não determinístico.

**4. Retorno como `CharArray`.**
Coerente com o `wipe(chars: CharArray)` já existente no `CryptoManager` e evita criar `String` imutável imediatamente. A UI converte para texto apenas ao setar no `EditText`.

**5. UI: `secondaryButton` "Gerar senha" abaixo do campo de senha.**
Em `AddEditActivity`, adicionar `Theme.secondaryButton(this, "Gerar senha")` logo após `passField.view`. Ao clicar: gerar, `passField.edit.setText(...)`, mover o cursor para o fim e **revelar** o valor (para conferência). Disponível tanto em nova entrada quanto em edição (regerar).
*Alternativa considerada:* um terceiro botão dentro da `row` do `PasswordField`. Rejeitada — a linha já tem input + toggle "Mostrar"; um botão dedicado abaixo é mais legível e não aperta o layout responsivo.

## Risks / Trade-offs

- **[Site rejeita os símbolos usados]** → O conjunto de símbolos é conservador (sem aspas/espaços); ainda assim alguns sites restringem símbolos. Mitigação: fora de escopo agora; uma futura opção "sem símbolos" pode ser adicionada sem alterar o contrato do gerador.
- **[Caracteres ambíguos excluídos reduzem levemente a entropia por caractere]** → 20 caracteres com ~73 símbolos no alfabeto ainda fornecem ampla margem (>120 bits). Trade-off aceito em favor da legibilidade.
- **[Senha revelada na tela ao gerar]** → Decisão deliberada para conferência; a tela já é `SecureActivity` (FLAG_SECURE presumido) e o app é offline. Risco residual baixo.
- **[`String` residual em memória ao setar no EditText]** → O `EditText` mantém o texto; é o mesmo modelo de exposição já existente para senha digitada. Não regride a postura atual.

## Migration Plan

Mudança puramente aditiva: novo arquivo no `vault-crypto` + alteração isolada no `AddEditActivity`. Sem migração de dados, sem mudança de formato do cofre. Rollback = reverter o commit; entradas existentes não são afetadas.

## Open Questions

- Nenhuma bloqueante. (Possível evolução futura: tornar a política configurável — explicitamente fora de escopo nesta change.)
