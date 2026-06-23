# entry-search

## ADDED Requirements

### Requirement: Busca de entradas por rótulo
O sistema SHALL oferecer, na tela de listagem e apenas em sessão DESBLOQUEADA, um campo de busca
que filtra as entradas exibidas pelo seu rótulo. A filtragem SHALL operar exclusivamente sobre os
rótulos já decifrados em memória (`Vault.list()`), sem realizar consultas ao banco nem
decriptações adicionais.

#### Scenario: Filtrar enquanto digita
- **WHEN** a sessão está DESBLOQUEADA e o usuário digita texto no campo de busca
- **THEN** o sistema exibe somente as entradas cujo rótulo contém o texto digitado
- **AND** atualiza a lista a cada alteração do texto

#### Scenario: Busca vazia mostra tudo
- **WHEN** o campo de busca está vazio (ou só com espaços)
- **THEN** o sistema exibe todas as entradas, na ordem original

### Requirement: Casamento insensível a caixa e a acentos
A busca SHALL casar o texto de forma insensível a maiúsculas/minúsculas e a acentos, normalizando
tanto o termo digitado quanto o rótulo (ex.: decomposição Unicode via `java.text.Normalizer` com
remoção de diacríticos). O casamento SHALL ser por substring (contém), não apenas prefixo.

#### Scenario: Ignora caixa
- **WHEN** o usuário digita `gmail` e existe uma entrada com rótulo `Gmail`
- **THEN** a entrada `Gmail` aparece nos resultados

#### Scenario: Ignora acento
- **WHEN** o usuário digita `joao` e existe uma entrada com rótulo `João`
- **THEN** a entrada `João` aparece nos resultados

#### Scenario: Casa no meio do rótulo
- **WHEN** o usuário digita `bank` e existe uma entrada com rótulo `My Bank App`
- **THEN** a entrada `My Bank App` aparece nos resultados

### Requirement: Estado vazio de resultados
Quando nenhuma entrada casar com o termo de busca, o sistema SHALL indicar ausência de resultados
em vez de exibir uma lista vazia silenciosa.

#### Scenario: Nenhum resultado
- **WHEN** o usuário digita um termo que não casa com nenhum rótulo
- **THEN** o sistema exibe uma mensagem de "nenhum resultado" e oculta a lista

### Requirement: Busca não enfraquece o modelo de segurança
A funcionalidade de busca SHALL operar somente com a sessão desbloqueada e SHALL NOT persistir o
termo de busca nem expor campos sensíveis. O escopo da busca SHALL se limitar ao rótulo; `username`
e `password` permanecem fora da busca.

#### Scenario: Some ao bloquear
- **WHEN** a sessão é BLOQUEADA (manual ou auto-lock) enquanto havia um termo de busca ativo
- **THEN** a tela redireciona para o desbloqueio e nenhum termo ou resultado de busca permanece visível

#### Scenario: Busca não alcança senha nem usuário
- **WHEN** o usuário digita um texto que existe apenas no `username` ou `password` de uma entrada (não no rótulo)
- **THEN** essa entrada NÃO aparece nos resultados
