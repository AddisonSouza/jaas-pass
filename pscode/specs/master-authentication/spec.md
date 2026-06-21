# master-authentication

### Requirement: Definição da senha mestra no primeiro uso
No primeiro uso, o sistema SHALL exigir que o usuário defina uma senha mestra e SHALL gerar o
material criptográfico inicial (salt e DEK) sem armazenar a senha mestra em nenhuma forma
recuperável.

#### Scenario: Setup inicial bem-sucedido
- **WHEN** o usuário abre o app pela primeira vez e define uma senha mestra
- **THEN** o sistema gera um salt aleatório (`SecureRandom`) e uma DEK aleatória
- **AND** persiste o blob de cofre (DEK cifrada com a KEK derivada da senha mestra)
- **AND** não grava a senha mestra em texto nem como hash recuperável

### Requirement: Verificação da senha mestra sem armazená-la
O sistema SHALL verificar a senha mestra exclusivamente por meio de decriptação autenticada do
blob de cofre, sem manter um hash de verificação separado.

#### Scenario: Senha mestra correta
- **WHEN** o usuário digita a senha mestra correta na tela de desbloqueio
- **THEN** a KEK derivada decripta o blob de cofre com tag GCM válida
- **AND** a sessão passa para o estado DESBLOQUEADO com a DEK disponível em memória

#### Scenario: Senha mestra incorreta
- **WHEN** o usuário digita uma senha mestra incorreta
- **THEN** a decriptação do blob de cofre falha com tag GCM inválida
- **AND** o sistema nega o acesso com mensagem genérica, sem revelar a causa

### Requirement: Ciclo de sessão e bloqueio
O sistema SHALL iniciar bloqueado, SHALL permitir bloqueio manual e SHALL bloquear
automaticamente por timeout e ao ir para segundo plano, descartando o material de chave.

#### Scenario: Auto-bloqueio ao ir para segundo plano
- **WHEN** o app vai para segundo plano ou o timeout de inatividade expira
- **THEN** o sistema descarta a DEK e zera o material de chave da memória
- **AND** revelar qualquer senha passa a exigir novo desbloqueio

### Requirement: Troca da senha mestra
O sistema SHALL permitir trocar a senha mestra re-cifrando apenas a DEK, mantendo todas as
entradas íntegras e decriptáveis com a nova senha.

#### Scenario: Troca de senha preserva os dados
- **WHEN** o usuário troca a senha mestra fornecendo a senha atual e a nova
- **THEN** o sistema re-deriva a KEK e re-cifra apenas o blob de cofre (DEK)
- **AND** todas as entradas existentes permanecem decriptáveis com a nova senha
