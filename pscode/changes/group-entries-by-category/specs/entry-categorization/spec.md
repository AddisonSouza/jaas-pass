# entry-categorization

## ADDED Requirements

### Requirement: Categoria opcional por entrada
O sistema SHALL permitir associar a cada entrada **uma categoria opcional** (zero ou uma), definida
como texto livre na criação e na edição. A categoria SHALL ser cifrada com a DEK (AES-256-GCM) antes
de persistir, como os demais campos sensíveis, e SHALL ser decifrada apenas em memória com a sessão
DESBLOQUEADA. Uma entrada sem categoria SHALL ser tratada como pertencente ao grupo "Sem categoria".

#### Scenario: Salvar entrada com categoria
- **WHEN** o usuário (em sessão desbloqueada) salva uma entrada informando uma categoria
- **THEN** o sistema cifra a categoria com a DEK e persiste apenas o ciphertext e o nonce
- **AND** a entrada passa a pertencer àquele grupo na visualização

#### Scenario: Categoria é opcional
- **WHEN** o usuário salva uma entrada sem informar categoria (campo vazio ou só espaços)
- **THEN** o sistema persiste a entrada sem categoria
- **AND** a entrada aparece no grupo "Sem categoria"

### Requirement: Sugestão das categorias já cadastradas
Ao informar a categoria, o sistema SHALL sugerir as categorias **já existentes** no cofre,
derivadas em memória das entradas decifradas (sem tabela nem tela de gerenciamento dedicada). A
deduplicação e o casamento das sugestões SHALL ser insensíveis a caixa e a acentos. Digitar uma
categoria inédita pela primeira vez SHALL, por si só, "cadastrá-la" para sugestões futuras.

#### Scenario: Sugerir categoria existente
- **WHEN** o usuário começa a digitar e existe uma categoria já usada que casa com o texto
- **THEN** o sistema oferece essa categoria como sugestão de autocomplete

#### Scenario: Sugestão ignora caixa e acento
- **WHEN** existe a categoria `Cartão` e o usuário digita `cartao`
- **THEN** a categoria `Cartão` é oferecida como sugestão

#### Scenario: Nova categoria fica disponível depois
- **WHEN** o usuário salva uma entrada com uma categoria que não existia antes
- **THEN** essa categoria passa a ser sugerida em criações/edições seguintes

### Requirement: Visualização agrupada por categoria
A tela de listagem SHALL exibir as entradas **agrupadas por categoria**, com um cabeçalho por
categoria, apenas em sessão DESBLOQUEADA. O grupo **"Sem categoria"** SHALL aparecer sempre por
último. As categorias SHALL ser ordenadas de forma insensível a caixa e a acentos; dentro de cada
grupo as entradas mantêm a ordem da listagem (`updated_at` decrescente).

#### Scenario: Entradas agrupadas sob cabeçalhos
- **WHEN** a sessão está DESBLOQUEADA e há entradas com categorias distintas
- **THEN** o sistema exibe um cabeçalho por categoria com as entradas correspondentes abaixo
- **AND** as entradas sem categoria aparecem sob o grupo "Sem categoria", ao final

#### Scenario: Grupo sem categoria só quando existir
- **WHEN** todas as entradas possuem categoria
- **THEN** o grupo "Sem categoria" não é exibido

### Requirement: Categorização não enfraquece o modelo de segurança
A categoria SHALL ser persistida somente cifrada (`nonce ‖ ciphertext ‖ tag`), nunca em texto claro,
e SHALL ser manipulada (decifrada, agrupada, sugerida) apenas com a sessão DESBLOQUEADA. A migração
de schema que introduz a coluna de categoria SHALL ser aditiva e nullable, preservando cofres
existentes sem perda nem necessidade de re-cifragem das demais entradas.

#### Scenario: Inspeção do banco não revela categorias
- **WHEN** o banco do app é inspecionado externamente
- **THEN** nenhuma categoria em texto claro é encontrada, apenas ciphertext e nonce

#### Scenario: Cofre antigo continua válido após atualização
- **WHEN** um cofre criado antes desta mudança é aberto após a atualização do app
- **THEN** o sistema migra o schema adicionando a coluna de categoria como nula
- **AND** as entradas existentes aparecem no grupo "Sem categoria" sem alteração de conteúdo
