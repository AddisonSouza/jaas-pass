## ADDED Requirements

### Requirement: Toggle password visibility on password fields
Todo campo editável de senha SHALL oferecer um botão (olho) para alternar entre ocultar e revelar o valor digitado. Os campos cobertos SHALL ser: Senha mestra (Desbloquear), Definir senha mestra e Confirmar senha mestra (Criar cofre) e Senha (Adicionar/Editar).

#### Scenario: Estado inicial mascarado
- **WHEN** um campo de senha é exibido pela primeira vez
- **THEN** o valor SHALL aparecer mascarado (oculto) e o botão SHALL indicar a ação de revelar

#### Scenario: Revelar a senha
- **WHEN** o usuário toca no botão de olho com o campo mascarado
- **THEN** o texto digitado SHALL ficar visível e o botão SHALL passar a indicar a ação de ocultar

#### Scenario: Ocultar novamente
- **WHEN** o usuário toca no botão de olho com o campo revelado
- **THEN** o valor SHALL voltar a ficar mascarado

### Requirement: Cursor position preserved on toggle
Ao alternar a visibilidade, a posição do cursor (e qualquer texto já digitado) SHALL ser preservada, sem limpar ou reposicionar indevidamente o conteúdo do campo.

#### Scenario: Cursor mantido ao alternar
- **WHEN** o usuário digita uma senha, posiciona o cursor no meio do texto e alterna a visibilidade
- **THEN** o texto SHALL permanecer idêntico e o cursor SHALL permanecer na mesma posição

### Requirement: Visibility toggle does not weaken security
O toggle de visibilidade SHALL apenas alterar a forma de exibição na tela; NÃO SHALL persistir a senha em nenhum armazenamento, log ou estado fora do campo, e NÃO SHALL afetar o tratamento seguro existente (extração como `CharArray` e zeragem após uso na autenticação mestra).

#### Scenario: Revelar não persiste a senha
- **WHEN** o usuário revela e depois oculta uma senha mestra durante o desbloqueio
- **THEN** o valor revelado NÃO SHALL ser gravado em disco, log ou preferências, e a senha SHALL continuar sendo extraída como `CharArray` e zerada após o uso

#### Scenario: Revelar é seguro contra captura de tela
- **WHEN** o usuário revela uma senha em qualquer campo
- **THEN** `FLAG_SECURE` SHALL continuar ativo, bloqueando screenshots e preview em "recentes" enquanto a senha está visível
