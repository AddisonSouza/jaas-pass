# entry-management

## MODIFIED Requirements

### Requirement: Criação de entrada
O sistema SHALL permitir criar uma entrada com rótulo/serviço, usuário opcional, senha e uma
**categoria opcional**, cifrando os campos sensíveis (incluindo a categoria, quando informada) com a
DEK antes de persistir.

#### Scenario: Adicionar nova senha
- **WHEN** o usuário (em sessão desbloqueada) salva uma nova entrada com a senha em texto
- **THEN** o sistema cifra a senha (e idealmente rótulo, usuário e categoria) com AES-256-GCM/DEK
- **AND** persiste apenas o ciphertext e o nonce

#### Scenario: Adicionar com categoria
- **WHEN** o usuário informa uma categoria ao salvar a entrada
- **THEN** o sistema cifra também a categoria com a DEK e a persiste apenas como ciphertext

### Requirement: Listagem de entradas
O sistema SHALL exibir a lista de entradas apenas em sessão desbloqueada, decriptando os rótulos
**e as categorias** em memória.

#### Scenario: Listar com sessão desbloqueada
- **WHEN** a sessão está DESBLOQUEADA
- **THEN** o sistema decripta e exibe os rótulos das entradas, agrupados por categoria
- **WHEN** a sessão está BLOQUEADA
- **THEN** nenhuma entrada é exibida

### Requirement: Edição e exclusão
O sistema SHALL permitir editar e excluir entradas, regravando com novo nonce ao editar. A edição
SHALL permitir **definir, alterar ou remover a categoria** da entrada.

#### Scenario: Edição regenera nonce
- **WHEN** o usuário edita e salva uma entrada
- **THEN** o sistema regrava o campo cifrado com um novo nonce

#### Scenario: Alterar categoria na edição
- **WHEN** o usuário edita a categoria de uma entrada e salva
- **THEN** o sistema regrava a categoria cifrada (ou a remove, se esvaziada)
- **AND** a entrada passa a aparecer no novo grupo na listagem
