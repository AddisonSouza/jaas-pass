## MODIFIED Requirements

### Requirement: Troca da senha mestra
O sistema SHALL permitir trocar a senha mestra gerando um **novo salt aleatório** a cada troca,
re-derivando a KEK da nova senha com esse salt novo e re-cifrando apenas a DEK, mantendo todas as
entradas íntegras e decriptáveis com a nova senha. O sistema SHALL persistir o novo salt junto com
a DEK re-cifrada de forma **atômica**, de modo que uma falha durante a gravação não deixe o cofre
inacessível.

#### Scenario: Troca de senha preserva os dados
- **WHEN** o usuário troca a senha mestra fornecendo a senha atual e a nova
- **THEN** o sistema gera um novo salt aleatório (`SecureRandom`) e re-deriva a KEK da nova senha com esse salt
- **AND** re-cifra apenas o blob de cofre (DEK) com a nova KEK
- **AND** todas as entradas existentes permanecem decriptáveis com a nova senha

#### Scenario: Salt é renovado a cada troca
- **WHEN** o usuário troca a senha mestra com sucesso
- **THEN** o salt persistido após a troca é diferente do salt anterior
- **AND** desbloquear posteriormente exige a nova senha derivada com o novo salt

#### Scenario: Senha atual incorreta não altera o material
- **WHEN** o usuário tenta trocar a senha fornecendo uma senha atual incorreta
- **THEN** a operação falha sem revelar a causa e retorna negativa
- **AND** o salt e o blob de cofre persistidos permanecem inalterados

#### Scenario: Gravação atômica do novo material
- **WHEN** o novo salt e a DEK re-cifrada são persistidos durante a troca de senha
- **THEN** ambos são gravados na mesma transação (tudo-ou-nada)
- **AND** uma falha na gravação não deixa o cofre num estado em que a senha (antiga ou nova) não desbloqueie
