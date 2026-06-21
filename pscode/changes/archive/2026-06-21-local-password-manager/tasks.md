# Tasks

> Implementar por fases, validando os cenários das specs a cada uma. Não escrever código antes de
> proposal/specs/design estarem alinhados. Em qualquer ambiguidade de segurança, escolher a opção
> mais conservadora.

## 1. CryptoManager (núcleo) + testes
- [x] 1.1 `deriveKEK(masterPassword: CharArray, salt, iterations)` com PBKDF2-HMAC-SHA256
- [x] 1.2 `generateDEK()` e wrap/unwrap da DEK com AES-256-GCM (KEK)
- [x] 1.3 `encryptField` / `decryptField` com AES-256-GCM e nonce único de 12 bytes
- [x] 1.4 Zeragem de `CharArray`/`ByteArray` após uso (`Arrays.fill`)
- [x] 1.5 Testes: senha errada → tag inválida; nonce único por regravação; rejeição de adulteração

## 2. VaultRepository (persistência)
- [x] 2.1 Schema SQLite: tabela de entradas (blob + nonce) e tabela de metadados (salt, iterações, versão, blob de cofre)
- [x] 2.2 Garantir que nenhum plaintext é gravado (teste de inspeção do `.db`) — garantido por construção (repo só grava blobs cifrados) + teste cripto "sem plaintext no blob"; inspeção literal do `.db` on-device fica no critério 6.2

## 3. Sessão e autenticação
- [x] 3.1 Setup inicial (definir senha mestra, gerar salt/DEK, persistir blob de cofre)
- [x] 3.2 Desbloqueio (deriva KEK, unwrap da DEK, transição de estado)
- [x] 3.3 Bloqueio manual + auto-lock por timeout e ao ir para background (descartar DEK)
- [x] 3.4 Troca de senha mestra (re-cifrar apenas a DEK)

## 4. UI mínima
- [x] 4.1 Tela de Desbloqueio
- [x] 4.2 Lista de entradas (decriptada em memória)
- [x] 4.3 Detalhe/Editar (mostrar/ocultar, copiar)
- [x] 4.4 Adicionar entrada

## 5. Endurecimento de plataforma
- [x] 5.1 Manifesto sem permissão `INTERNET`
- [x] 5.2 `android:allowBackup="false"`
- [x] 5.3 `FLAG_SECURE` na janela (via `SecureActivity` base, em todas as telas)
- [x] 5.4 Clipboard com auto-limpeza (~30 s) + `EXTRA_IS_SENSITIVE` (Android 13+)
- [x] 5.5 Revisar ausência de logs de segredos (inclusive build de debug) — nenhum Log/print no código de produção; `PlainEntry.toString()` redige a senha

## 6. Validação final (critérios de aceitação)
- [x] 6.1 Build sem nenhuma dependência de terceiros no Gradle — verificado (só kotlin-stdlib + :vault-crypto)
- [x] 6.2 Inspeção do banco não revela senhas nem a senha mestra — probe gera `.db` real (esquema do VaultRepository + cripto real); `sqlite3`/`grep` confirmam ausência de todos os plaintexts
- [x] 6.3 Cópia dos arquivos para outro ambiente não dá acesso sem a senha mestra — `unlock(senha-errada)=false` sobre o mesmo store
- [x] 6.4 Após bloqueio, revelar senha exige novo desbloqueio — `lock()` descarta a DEK; `requireDek()` falha e as telas redirecionam ao desbloqueio (coberto por teste de sessão)
- [x] 6.5 Aviso na UI: esquecer a senha mestra = perda total e irreversível — exibido na tela de setup

> Pendente de confirmação manual on-device (sideload do APK): 6.2/6.3 com o `.db` real do app e 6.4 pela navegação. APK em `C:\Users\addis\Downloads\jaas-pass-debug.apk`.
