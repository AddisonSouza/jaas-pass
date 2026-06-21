## ADDED Requirements

### Requirement: Geração de senha forte sob demanda
O sistema SHALL fornecer um gerador local de senhas que, ao ser acionado, produz uma senha aleatória forte usando exclusivamente `java.security.SecureRandom` (a mesma fonte de aleatoriedade da criptografia do app), sem qualquer chamada de rede ou dependência de terceiros.

#### Scenario: Geração produz senha do comprimento padrão
- **WHEN** o gerador é acionado com a política padrão
- **THEN** a senha retornada MUST ter exatamente 20 caracteres

#### Scenario: Cada geração é única
- **WHEN** o gerador é acionado duas vezes em sequência
- **THEN** as duas senhas retornadas MUST ser diferentes entre si (com probabilidade desprezível de colisão)

### Requirement: Composição de categorias garantida
A senha gerada SHALL conter ao menos um caractere de cada categoria — letra maiúscula, letra minúscula, dígito e símbolo — de modo a satisfazer políticas comuns de complexidade.

#### Scenario: Todas as categorias presentes
- **WHEN** uma senha é gerada com a política padrão
- **THEN** ela MUST conter ≥1 maiúscula, ≥1 minúscula, ≥1 dígito e ≥1 símbolo

#### Scenario: Posição das categorias não é previsível
- **WHEN** várias senhas são geradas
- **THEN** os caracteres das categorias garantidas MUST aparecer em posições embaralhadas (não fixas no início), via embaralhamento Fisher–Yates da sequência final

### Requirement: Exclusão de caracteres ambíguos
O conjunto de caracteres usado na geração SHALL excluir caracteres visualmente ambíguos (`O`, `0`, `l`, `1`, `I`) para reduzir erro humano ao copiar ou transcrever a senha.

#### Scenario: Senha não contém ambíguos
- **WHEN** uma senha é gerada com a política padrão
- **THEN** ela MUST NOT conter nenhum dos caracteres `O`, `0`, `l`, `1`, `I`

### Requirement: Seleção sem viés estatístico
A escolha de cada caractere SHALL ser uniformemente distribuída sobre o alfabeto permitido, sem viés de módulo (por exemplo, usando `SecureRandom.nextInt(bound)`), preservando a entropia esperada da senha.

#### Scenario: Distribuição uniforme dos caracteres
- **WHEN** um grande número de caracteres é gerado a partir do alfabeto permitido
- **THEN** a frequência de cada caractere MUST ser aproximadamente uniforme, sem caracteres sistematicamente favorecidos por viés de módulo

### Requirement: Acionamento na tela de entrada
A tela de adicionar/editar entrada SHALL oferecer um controle visível para gerar uma senha forte, que preenche o campo de senha com o valor gerado e o exibe para conferência, reutilizando o campo de senha existente.

#### Scenario: Botão preenche o campo de senha
- **WHEN** o usuário toca em "Gerar senha" na tela de nova entrada
- **THEN** o campo de senha MUST ser preenchido com uma senha forte recém-gerada
- **AND** o valor gerado MUST ficar visível (revelado) para o usuário conferir antes de salvar

#### Scenario: Regerar substitui o valor anterior
- **WHEN** o usuário toca em "Gerar senha" novamente
- **THEN** o conteúdo anterior do campo MUST ser substituído por uma nova senha gerada
