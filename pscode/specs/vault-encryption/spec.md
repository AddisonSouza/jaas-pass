# vault-encryption

### Requirement: Derivação de chave por KDF lento
O sistema SHALL derivar a chave de criptografia da senha mestra usando PBKDF2-HMAC-SHA256 com
salt aleatório e um número de iterações configurável e versionado, com no mínimo 600.000
iterações.

#### Scenario: Derivação com parâmetros versionados
- **WHEN** o sistema deriva a KEK a partir da senha mestra
- **THEN** usa o salt persistido e o número de iterações registrado nos metadados (≥ 600.000)
- **AND** a versão do esquema cripto é registrada para permitir migração futura

### Requirement: Hierarquia de chaves (envelope encryption)
O sistema SHALL cifrar as entradas com uma DEK aleatória, e SHALL armazenar a DEK apenas cifrada
com a KEK via AES-256-GCM.

#### Scenario: DEK protegida pela KEK
- **WHEN** o material criptográfico é persistido
- **THEN** a DEK em repouso está cifrada com AES-256-GCM usando a KEK
- **AND** o blob de cofre tem o formato `nonce ‖ ciphertext ‖ tag`

### Requirement: Criptografia autenticada das entradas
O sistema SHALL cifrar cada campo sensível com AES-256-GCM usando a DEK, com nonce único de 12
bytes por operação, e SHALL verificar a tag antes de usar qualquer valor decriptado.

#### Scenario: Nonce nunca reutilizado
- **WHEN** uma entrada é criada ou regravada após edição
- **THEN** um novo nonce de 12 bytes é gerado por `SecureRandom`
- **AND** nenhum nonce é reutilizado com a mesma DEK

#### Scenario: Rejeição de dado adulterado
- **WHEN** um blob de entrada está corrompido ou adulterado
- **THEN** a verificação da tag GCM falha
- **AND** o sistema rejeita o valor sem expô-lo

### Requirement: Armazenamento somente de ciphertext
O sistema SHALL persistir apenas dados cifrados, nonces e metadados não sensíveis (salt,
iterações, versão), sem nenhum plaintext de senha ou da senha mestra.

#### Scenario: Inspeção do banco não revela segredos
- **WHEN** os arquivos/banco do app são inspecionados externamente
- **THEN** nenhuma senha em claro nem a senha mestra são encontradas
- **AND** apenas ciphertext, nonces, salt, iterações e versão estão presentes

### Requirement: Aleatoriedade criptográfica
O sistema SHALL gerar todo salt, nonce e chave usando `java.security.SecureRandom`.

#### Scenario: Geração de material aleatório
- **WHEN** o sistema precisa de salt, nonce ou DEK
- **THEN** o valor é obtido de `SecureRandom`
