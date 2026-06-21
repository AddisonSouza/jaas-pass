# Design: Gerenciador de Senhas Local

## Constraints (inegociáveis)

1. **Sem dependências externas.** Apenas SDK Android + biblioteca padrão. Nada adicionado ao
   Gradle além do plugin Android/Kotlin essencial.
2. **Offline absoluto.** O manifesto **não declara `INTERNET`**. Nenhuma chamada de rede.
3. **Senha mestra nunca persistida** (nem texto, nem hash reversível).
4. **AEAD sempre** (AES-GCM); integridade verificada antes de qualquer uso.
5. **Falha segura.** Erro de decriptação/tag inválida → nega acesso, sem vazar a causa.

## Arquitetura

```
UI (telas mínimas: Unlock, Lista, Detalhe/Editar, Adicionar)
  └─ ViewModel / SessionState (BLOQUEADO | DESBLOQUEADO)
        └─ CryptoManager
              ├─ deriveKEK(masterPassword, salt, iterations) -> KEK
              ├─ unwrapDEK(vaultBlob, KEK)                    -> DEK | falha
              ├─ encryptField(plaintext, DEK)                 -> nonce‖ct‖tag
              └─ decryptField(blob, DEK)                      -> plaintext | falha
        └─ VaultRepository (SQLite via android.database.sqlite — só blobs)
```

O app inicia **bloqueado**. A DEK só existe em memória após desbloqueio bem-sucedido.
Bloquear descarta a DEK e zera o material de chave.

## Núcleo criptográfico

### Derivação (KEK)
- Algoritmo: `PBKDF2WithHmacSHA256` (`javax.crypto.SecretKeyFactory`).
- Salt: 16 bytes de `SecureRandom`, gerado uma vez por instalação, persistido em claro.
- Iterações: **600.000** (mínimo OWASP 2025 para PBKDF2-HMAC-SHA256). Parametrizável e
  **versionado** para permitir aumento futuro sem quebrar cofres existentes.
- Saída: 256 bits → **KEK** (Key Encryption Key).

### Hierarquia de chaves (envelope encryption)
- **DEK** (Data Encryption Key): 256 bits aleatórios de `SecureRandom`, gerada uma vez no setup.
  É a chave que cifra as entradas.
- **DEK em repouso:** cifrada com a KEK via **AES-256-GCM** (nonce de 12 bytes aleatório). O
  blob de cofre persistido é `nonce ‖ ciphertext ‖ tag`.
- Trocar a senha mestra re-cifra **apenas a DEK**, não o vault inteiro.

### Verificação da senha mestra (sem armazená-la)
- **Não** há hash separado da senha mestra. A verificação é a **própria decriptação autenticada
  da DEK**: deriva-se a KEK da senha digitada e tenta-se decriptar o blob de cofre. Tag GCM
  inválida (`AEADBadTagException`) ⇒ senha errada.

### Criptografia das entradas
- Cada campo sensível (no mínimo a senha; idealmente também rótulo e usuário) cifrado com
  **AES-256-GCM** usando a **DEK**.
- **Nonce único de 12 bytes por operação** (`SecureRandom`). Nunca reutilizar nonce com a mesma
  chave; ao reeditar/regravar, gerar novo nonce.
- Formato por campo: `nonce ‖ ciphertext ‖ tag`.

### Armazenamento
- SQLite (`android.database.sqlite`, parte do SDK). Guarda **apenas**: blobs cifrados das
  entradas + nonces, e uma tabela de metadados com salt, nº de iterações, versão do esquema
  cripto e o blob de cofre (DEK cifrada). **Nenhum plaintext** em lugar algum.

## Endurecimento de plataforma

- `CharArray`/`ByteArray` para a senha mestra (nunca `String`); `Arrays.fill(0)` logo após
  derivar a chave.
- Zerar KEK/DEK ao bloquear. *Nota JVM:* `SecretKeySpec` copia o array internamente e não é
  trivialmente apagável — mitigar mantendo o tempo de vida das chaves curto e bloqueando cedo.
- `FLAG_SECURE` na janela (bloqueia screenshots e preview na tela de recentes).
- `android:allowBackup="false"` (evita vazamento via backup do sistema).
- **Sem permissão `INTERNET`** no manifesto.
- Clipboard: ao copiar uma senha, auto-limpar após ~30 s e marcar como sensível
  (`ClipDescription` com `EXTRA_IS_SENSITIVE`, Android 13+).
- Nenhum log de segredos/chaves/nonces, nem em debug. Todo material aleatório via `SecureRandom`.

## Camada opcional (NÃO obrigatória): Android Keystore

`AndroidKeyStore` é do framework (não viola a restrição de dependências). **Trade-off:** chave
em hardware protege contra extração de arquivos, mas pode ser utilizável no próprio aparelho se
ele estiver desbloqueado — portanto **não substitui** a senha mestra como garantia principal.
Se usado, entra como camada adicional (envolver também a DEK com chave do Keystore exigindo
autenticação do usuário), mantendo a senha mestra como base.

## Trade-offs assumidos

- **PBKDF2 vs Argon2id:** Argon2id é memory-hard (melhor contra GPU/ASIC), mas **não vem na
  plataforma** e exigiria dependência externa, violando a restrição. Adotamos PBKDF2-HMAC-SHA256
  com iterações altas — o melhor disponível sem libs de terceiros. O esquema cripto é versionado
  para facilitar migração futura caso a restrição seja relaxada.
- **Rótulo cifrado vs em claro:** cifrar tudo maximiza privacidade em repouso ao custo de não
  permitir busca no banco sem desbloquear. Dado o modelo de ameaça, **cifrar tudo** é o padrão.
- A garantia é **at-rest, com o app bloqueado** — reforçar isso em comentários e na UI.
