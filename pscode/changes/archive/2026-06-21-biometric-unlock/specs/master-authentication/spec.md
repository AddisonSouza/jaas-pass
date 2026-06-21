# master-authentication

## ADDED Requirements

### Requirement: Desbloqueio alternativo por biometria
O sistema SHALL oferecer a biometria como **caminho alternativo** de desbloqueio, mantendo a senha
mestra como **raiz de confiança** e fallback obrigatório. O caminho biométrico SHALL produzir a
**mesma DEK** que o caminho da senha mestra, e SHALL NOT substituir, enfraquecer ou tornar
dispensável a senha mestra. O detalhamento do mecanismo biométrico vive na capacidade
`biometric-unlock`.

#### Scenario: Ambos os caminhos abrem o mesmo cofre
- **WHEN** o cofre é desbloqueado por biometria em vez da senha mestra
- **THEN** a sessão passa para DESBLOQUEADO com a mesma DEK que a senha mestra produziria
- **AND** todas as entradas permanecem decriptáveis normalmente

#### Scenario: Senha mestra permanece como raiz de confiança
- **WHEN** a biometria está ativa
- **THEN** a senha mestra continua aceita para desbloqueio a qualquer momento
- **AND** nenhuma forma recuperável da senha mestra é persistida pela ativação da biometria

### Requirement: Biometria sobrevive à troca de senha mestra
Como o envelope biométrico cifra diretamente a DEK (e não a KEK derivada da senha), o sistema SHALL
manter o atalho biométrico funcional após uma troca de senha mestra bem-sucedida, já que a DEK não
muda na troca.

#### Scenario: Troca de senha não quebra o atalho biométrico
- **WHEN** o usuário troca a senha mestra com sucesso e a biometria está ativa
- **THEN** o envelope biométrico da DEK permanece válido
- **AND** desbloquear por biometria continua produzindo a mesma DEK
