# entry-search

## MODIFIED Requirements

### Requirement: Busca de entradas por rótulo
O sistema SHALL oferecer, na tela de listagem e apenas em sessão DESBLOQUEADA, um campo de busca
que filtra as entradas exibidas pelo seu rótulo. A filtragem SHALL operar exclusivamente sobre os
rótulos já decifrados em memória (`Vault.list()`), sem realizar consultas ao banco nem
decriptações adicionais. Os resultados filtrados SHALL ser apresentados **dentro da visualização
agrupada por categoria**: apenas os grupos que contêm pelo menos um resultado são exibidos.

#### Scenario: Filtrar enquanto digita
- **WHEN** a sessão está DESBLOQUEADA e o usuário digita texto no campo de busca
- **THEN** o sistema exibe somente as entradas cujo rótulo contém o texto digitado
- **AND** atualiza a lista a cada alteração do texto

#### Scenario: Resultados permanecem agrupados
- **WHEN** o usuário digita um termo que casa com entradas de categorias diferentes
- **THEN** o sistema exibe os resultados sob os cabeçalhos das respectivas categorias
- **AND** oculta os grupos sem nenhum resultado

#### Scenario: Busca vazia mostra tudo
- **WHEN** o campo de busca está vazio (ou só com espaços)
- **THEN** o sistema exibe todas as entradas, agrupadas por categoria
