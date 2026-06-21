# biometric-unlock

### Requirement: Ativação opt-in da biometria
O sistema SHALL permitir ativar a biometria apenas como **opt-in explícito** e somente com a
sessão **DESBLOQUEADA** (a DEK presente em memória). A ativação SHALL ser oferecida após um
`setup`/`unlock` bem-sucedido com a senha mestra, e somente quando o aparelho possuir biometria
cadastrada. A ativação SHALL NOT persistir a senha mestra em nenhuma forma.

#### Scenario: Oferta após desbloqueio com biometria disponível
- **WHEN** o usuário conclui um `setup` ou `unlock` com a senha mestra e o aparelho tem biometria cadastrada e ela ainda não está ativa
- **THEN** o sistema oferece ativar a biometria
- **AND** ao aceitar, envelopa a DEK atual com a chave do Keystore e persiste esse envelope

#### Scenario: Sem hardware/cadastro biométrico não há oferta
- **WHEN** o aparelho não possui sensor biométrico ou não há biometria cadastrada
- **THEN** o sistema não oferece ativar a biometria
- **AND** o desbloqueio segue exclusivamente pela senha mestra

#### Scenario: Ativação não persiste a senha mestra
- **WHEN** a biometria é ativada
- **THEN** apenas a DEK envelopada pela chave do Keystore (e seu IV) é persistida
- **AND** nenhuma senha mestra, em texto ou hash recuperável, é gravada

### Requirement: Proteção da DEK por chave do Android Keystore
O sistema SHALL envelopar a DEK com uma chave AES gerada no Android Keystore configurada com
`setUserAuthenticationRequired(true)`, de modo que a chave só possa ser usada após autenticação
biométrica do usuário. O material da chave SHALL permanecer no Keystore (não exportável) e o
sistema SHALL persistir apenas o ciphertext da DEK e o IV.

#### Scenario: DEK envelopada por chave protegida por autenticação
- **WHEN** a biometria é ativada
- **THEN** a DEK é cifrada (AES-GCM) por uma chave do Keystore com autenticação de usuário exigida
- **AND** o blob resultante e seu IV são persistidos junto aos metadados do cofre

#### Scenario: Inspeção do armazenamento não revela segredos
- **WHEN** o banco do app é inspecionado externamente
- **THEN** apenas ciphertext da DEK, IV e metadados não sensíveis estão presentes
- **AND** nenhuma senha mestra nem DEK em claro são encontradas

### Requirement: Desbloqueio por biometria amarrado à decifragem
O sistema SHALL desbloquear via `BiometricPrompt` usando um `CryptoObject` que vincula a
autenticação biométrica ao `Cipher` da chave do Keystore, de modo que o sucesso da biometria
**produza** a decifragem da DEK (e não apenas libere uma tela). Em sucesso, a sessão SHALL passar
para DESBLOQUEADO com a DEK em memória, sem derivar a KEK via PBKDF2.

#### Scenario: Biometria válida desbloqueia o cofre
- **WHEN** o usuário autentica com biometria válida no prompt
- **THEN** o `Cipher` liberado pelo Keystore decifra a DEK envelopada
- **AND** a sessão passa para DESBLOQUEADO com a DEK disponível, sem usar a senha mestra

#### Scenario: Biometria cancelada ou inválida não concede acesso
- **WHEN** o usuário cancela o prompt ou a autenticação biométrica falha
- **THEN** o cofre permanece BLOQUEADO
- **AND** o sistema mantém o campo de senha mestra disponível como fallback, sem revelar a causa

### Requirement: Invalidação por novo cadastro biométrico
O sistema SHALL configurar a chave do Keystore com `setInvalidatedByBiometricEnrollment(true)`, de
modo que o cadastro de uma nova digital invalide a chave e o desbloqueio por biometria deixe de
funcionar, exigindo a senha mestra.

#### Scenario: Nova digital invalida o atalho biométrico
- **WHEN** uma nova biometria é cadastrada no aparelho após a ativação
- **THEN** a chave do Keystore é invalidada e a decifragem da DEK por biometria falha
- **AND** o sistema exige a senha mestra e trata o atalho biométrico como desativado

### Requirement: Senha mestra como fallback obrigatório
O sistema SHALL manter a senha mestra sempre disponível como caminho de desbloqueio, mesmo com a
biometria ativa. O sistema SHALL permitir desativar a biometria, descartando o envelope da DEK e a
chave do Keystore associada.

#### Scenario: Fallback para senha mestra sempre disponível
- **WHEN** a tela de desbloqueio é exibida com a biometria ativa
- **THEN** o campo da senha mestra permanece disponível como alternativa
- **AND** digitar a senha mestra correta desbloqueia normalmente

#### Scenario: Desativar biometria descarta o atalho
- **WHEN** o usuário desativa a biometria
- **THEN** o envelope da DEK pela chave do Keystore é removido do armazenamento
- **AND** a chave correspondente do Keystore é apagada
- **AND** o desbloqueio passa a exigir a senha mestra

### Requirement: Disparo automático do prompt na tela de desbloqueio
O sistema SHALL disparar o `BiometricPrompt` automaticamente ao abrir a tela de desbloqueio quando
a biometria está ativa e o cofre está BLOQUEADO (em cold start ou após auto-lock), sem exigir um
toque adicional, mantendo o fallback de senha mestra visível.

#### Scenario: Prompt automático com cofre bloqueado e biometria ativa
- **WHEN** a tela de desbloqueio abre com o cofre BLOQUEADO e a biometria ativa
- **THEN** o `BiometricPrompt` é exibido automaticamente
- **AND** o campo de senha mestra continua visível como fallback
