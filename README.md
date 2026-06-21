# jaas-pass

Gerenciador de senhas pessoal **100% offline** para Android, em **Kotlin**, usando **apenas o
SDK Android + biblioteca padrão** (zero dependências de terceiros). Se o aparelho for roubado ou
os arquivos do app forem extraídos, o conteúdo deve ser **ilegível** sem a senha mestra correta.

## Segurança (resumo)

- Autenticação por **senha mestra**, nunca persistida (nem como hash recuperável).
- Entradas cifradas com **AES-256-GCM** (AEAD).
- Derivação de chave via **PBKDF2-HMAC-SHA256** (≥ 600.000 iterações, versionado) com hierarquia
  **KEK/DEK** (envelope encryption).
- Endurecimento: sem permissão `INTERNET`, `allowBackup=false`, `FLAG_SECURE`, auto-limpeza de
  clipboard, auto-lock.

> ⚠️ Esquecer a senha mestra = **perda total e irreversível** (sem nuvem = sem recuperação).

Modelo de ameaça completo em [`pscode/changes/local-password-manager/proposal.md`](pscode/changes/local-password-manager/proposal.md).

## Stack & build

- Kotlin · Android `compileSdk/targetSdk 35`, `minSdk 26`
- Gradle 8.9 (wrapper) · AGP 8.7.3 · JDK 21

```bash
./gradlew :app:assembleDebug      # gera app/build/outputs/apk/debug/app-debug.apk
```

## Fluxo de desenvolvimento (SDD)

Desenvolvimento orientado a specs via [pscode](https://github.com/thiagodiogo/Pscode):
`proposal → specs → design → tasks → apply`. Artefatos em `pscode/changes/local-password-manager/`.
