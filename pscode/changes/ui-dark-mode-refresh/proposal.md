## Why

A UI atual é funcional, mas crua: usa o tema nativo **claro** (`Theme.Material.Light.NoActionBar`), com widgets nativos sem estilização, alinhados à esquerda e encostados nas bordas. Para um gerenciador de senhas usado no dia a dia, a aparência transmite pouca confiança e contrasta mal em ambientes com pouca luz. Queremos um visual **escuro por padrão**, mais moderno e centralizado, e dar ao usuário o controle de **revelar/ocultar a senha** diretamente nos campos de entrada — hoje só existe um toggle na tela de Detalhe, sobre um texto, nunca nos campos editáveis.

## What Changes

- **Modo escuro por padrão**: o app passa a usar um tema escuro nativo. **BREAKING** (visual): o modo claro é removido — não há alternância de tema.
- **Helper de tema central** (`com.jaaspass.ui.Theme`): um único objeto Kotlin define paleta escura, tokens de espaçamento/raio e **fábricas de widgets estilizados** (inputs e botões com cantos arredondados via `GradientDrawable`, container central com largura máxima). Todas as telas passam a consumir esse helper, eliminando estilo duplicado.
- **Layout moderno e centralizado** nas 4 telas (Desbloquear/Criar cofre, Lista, Detalhe, Adicionar/Editar): conteúdo num cartão central com largura máxima, espaçamento consistente, cor de acento nos botões primários.
- **Botão de mostrar/ocultar senha (olho)** em **todos os campos de senha**: Senha mestra (Desbloquear), Definir + Confirmar (Criar cofre) e Senha (Adicionar/Editar). Estado inicial **mascarado**; o toggle alterna entre oculto e visível preservando a posição do cursor.
- Toda a estilização é **100% programática em Kotlin**, sem qualquer dependência nova (constraint inegociável: apenas `android.*` / `java.*` + Kotlin stdlib; nada de AndroidX/Material).

## Capabilities

### New Capabilities
- `ui-appearance`: aparência e tema da interface — modo escuro por padrão, tokens de design centralizados (paleta, espaçamento, raio), widgets estilizados e layout centralizado aplicados de forma consistente em todas as telas.
- `password-visibility`: revelar/ocultar o valor de um campo de senha sob demanda, via botão (olho), preservando o cursor e mantendo o estado mascarado por padrão.

### Modified Capabilities
<!-- Nenhuma mudança de REQUISITO nas capabilities existentes. As telas de master-authentication
     e entry-management ganham apresentação nova, mas seus requisitos de comportamento/segurança
     (senha mestra não persistida, falha segura, FLAG_SECURE, auto-lock, AEAD) permanecem idênticos. -->
- _(nenhuma)_

## Impact

- **Código afetado**: `AndroidManifest.xml` (tema do `application`); novo arquivo `app/src/main/java/com/jaaspass/ui/Theme.kt`; refatoração de apresentação em `UnlockActivity`, `ListActivity`, `DetailActivity`, `AddEditActivity` (e possivelmente helpers em `SecureActivity`, que já expõe `dp()`).
- **Sem mudança** em cripto, dados, sessão ou manifesto além do atributo `android:theme`. Nenhuma permissão nova; offline absoluto preservado.
- **Dependências**: nenhuma nova (constraint zero-deps mantida).
- **Segurança**: revelar a senha mestra é aceitável porque `FLAG_SECURE` (em `SecureActivity`) já bloqueia screenshots e preview em "recentes". O toggle não persiste a senha em nenhum lugar.
- **Risco**: baixo — mudança puramente de apresentação/UX; sem alteração de fluxo de dados.
