# entry-management

## ADDED Requirements

### Requirement: Criação de entrada
O sistema SHALL permitir criar uma entrada com rótulo/serviço, usuário opcional e senha, cifrando
os campos sensíveis com a DEK antes de persistir.

#### Scenario: Adicionar nova senha
- **WHEN** o usuário (em sessão desbloqueada) salva uma nova entrada com a senha em texto
- **THEN** o sistema cifra a senha (e idealmente rótulo e usuário) com AES-256-GCM/DEK
- **AND** persiste apenas o ciphertext e o nonce

### Requirement: Listagem de entradas
O sistema SHALL exibir a lista de entradas apenas em sessão desbloqueada, decriptando os rótulos
em memória.

#### Scenario: Listar com sessão desbloqueada
- **WHEN** a sessão está DESBLOQUEADA
- **THEN** o sistema decripta e exibe os rótulos das entradas
- **WHEN** a sessão está BLOQUEADA
- **THEN** nenhuma entrada é exibida

### Requirement: Recuperação segura para uso
O sistema SHALL decriptar uma senha sob demanda para exibição (com toggle mostrar/ocultar) e/ou
cópia para a área de transferência com auto-limpeza temporizada.

#### Scenario: Copiar com auto-limpeza
- **WHEN** o usuário copia a senha de uma entrada
- **THEN** o conteúdo é marcado como sensível e a área de transferência é limpa após ~30 s

#### Scenario: Revelar sob demanda
- **WHEN** o usuário aciona "mostrar" em uma entrada
- **THEN** o sistema decripta e exibe a senha apenas enquanto solicitado

### Requirement: Edição e exclusão
O sistema SHALL permitir editar e excluir entradas, regravando com novo nonce ao editar.

#### Scenario: Edição regenera nonce
- **WHEN** o usuário edita e salva uma entrada
- **THEN** o sistema regrava o campo cifrado com um novo nonce
