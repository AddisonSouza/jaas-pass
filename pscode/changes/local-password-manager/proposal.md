# Proposal: Gerenciador de Senhas Local (Android / Kotlin)

## Why

Quero um gerenciador de senhas pessoal **100% offline** para Android, sem nuvem e sem
dependências de terceiros. O objetivo de segurança é direto: se o aparelho for roubado ou
os arquivos do app forem extraídos/inspecionados, o conteúdo deve ser **ilegível** e o app
**inacessível** sem a senha mestra correta. As senhas devem ser persistidas criptografadas e
recuperáveis de forma segura para uso em outros aplicativos.

## What Changes

- Novo app Android nativo em **Kotlin**, usando **apenas o SDK Android + biblioteca padrão**
  (`java.*`, `javax.crypto.*`, `android.*`). Nenhuma dependência de terceiros no Gradle.
- Autenticação por **senha mestra**, que nunca é armazenada (nem como hash recuperável).
- Persistência local de entradas (serviço/usuário/senha) **cifradas com AES-256-GCM**.
- Derivação de chave a partir da senha mestra via **PBKDF2-HMAC-SHA256** com hierarquia
  KEK/DEK (envelope encryption).
- Bloqueio/desbloqueio de sessão, auto-lock e endurecimento de plataforma (sem `INTERNET`,
  `allowBackup=false`, `FLAG_SECURE`, auto-limpeza do clipboard).

## Capabilities (specs)

- `master-authentication` — definição/verificação da senha mestra e ciclo de sessão.
- `vault-encryption` — derivação de chaves, criptografia autenticada e armazenamento.
- `entry-management` — CRUD e recuperação segura de entradas.

## Out of Scope (futuro)

- Preenchimento automático (Android Autofill Framework).
- Desbloqueio biométrico (`BiometricPrompt` amarrado ao Android Keystore).
- Gerador de senhas, export/import de cofre cifrado.

## Threat Model (resumo)

**Garante (com o app bloqueado):** dados em repouso ilegíveis; sem a senha mestra a
decriptação é inviável; cópia dos arquivos para outro aparelho não dá acesso; resistência a
brute-force offline proporcional à força da senha mestra e ao custo do KDF.

**Não garante (documentar na UI):** aparelho com root e app **já desbloqueado** (chave em
memória); keylogger/malware com acessibilidade; senha mestra fraca; esquecimento da senha
mestra = perda total e irreversível (sem nuvem = sem recuperação).
