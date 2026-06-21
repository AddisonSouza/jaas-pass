## ADDED Requirements

### Requirement: Dark theme by default
O aplicativo SHALL apresentar todas as telas em um tema escuro nativo por padrão, sem oferecer alternância para tema claro. A estilização SHALL ser feita exclusivamente com APIs do SDK Android (`android.*`) — nenhuma dependência de terceiros (AndroidX/Material) SHALL ser introduzida.

#### Scenario: App abre em modo escuro
- **WHEN** o usuário abre qualquer tela do app (Desbloquear, Lista, Detalhe, Adicionar/Editar)
- **THEN** o fundo SHALL ser escuro e o texto claro, com contraste legível em ambientes com pouca luz

#### Scenario: Sem modo claro
- **WHEN** o app é executado em um dispositivo configurado em modo claro do sistema
- **THEN** o app SHALL continuar exibindo seu tema escuro (não segue o tema do sistema nem oferece opção de tema claro)

### Requirement: Centralized design tokens
O app SHALL definir um único ponto de verdade para os tokens de aparência (cores da paleta escura, espaçamentos, raios de canto e cor de acento) e SHALL reutilizá-lo em todas as telas, evitando estilo duplicado por Activity.

#### Scenario: Tokens reutilizados entre telas
- **WHEN** a cor de acento ou um espaçamento padrão é alterado no ponto central de tokens
- **THEN** a mudança SHALL refletir consistentemente em todas as telas que consomem esse token, sem edição tela a tela

#### Scenario: Widgets estilizados consistentes
- **WHEN** dois botões primários são renderizados em telas diferentes
- **THEN** ambos SHALL compartilhar o mesmo estilo (cantos arredondados, cor de acento, espaçamento) por usarem a mesma fábrica de widgets

### Requirement: Centered modern layout
Cada tela SHALL apresentar seu conteúdo em um container central com largura máxima e espaçamento consistente, de modo que em telas largas o conteúdo não fique encostado nas bordas.

#### Scenario: Conteúdo centralizado
- **WHEN** uma tela é exibida
- **THEN** o conteúdo principal SHALL estar horizontalmente centralizado, respeitando uma largura máxima e margens internas consistentes

### Requirement: Appearance changes preserve security and behavior
A mudança de aparência SHALL ser puramente de apresentação e NÃO SHALL alterar nenhum requisito de segurança ou de fluxo existente: `FLAG_SECURE`, auto-lock, não persistência da senha mestra, falha segura e ausência de permissão de rede permanecem inalterados.

#### Scenario: FLAG_SECURE mantido
- **WHEN** qualquer tela do app é exibida após o restyling
- **THEN** screenshots e preview em "recentes" SHALL continuar bloqueados (FLAG_SECURE ativo)

#### Scenario: Sem novas permissões ou dependências
- **WHEN** o app é compilado após a mudança de aparência
- **THEN** o manifesto NÃO SHALL declarar novas permissões e o Gradle NÃO SHALL conter novas dependências de terceiros
