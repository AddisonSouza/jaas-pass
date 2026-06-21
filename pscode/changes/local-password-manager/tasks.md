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
- [ ] 3.1 Setup inicial (definir senha mestra, gerar salt/DEK, persistir blob de cofre)
- [ ] 3.2 Desbloqueio (deriva KEK, unwrap da DEK, transição de estado)
- [ ] 3.3 Bloqueio manual + auto-lock por timeout e ao ir para background (descartar DEK)
- [ ] 3.4 Troca de senha mestra (re-cifrar apenas a DEK)

## 4. UI mínima
- [ ] 4.1 Tela de Desbloqueio
- [ ] 4.2 Lista de entradas (decriptada em memória)
- [ ] 4.3 Detalhe/Editar (mostrar/ocultar, copiar)
- [ ] 4.4 Adicionar entrada

## 5. Endurecimento de plataforma
- [ ] 5.1 Manifesto sem permissão `INTERNET`
- [ ] 5.2 `android:allowBackup="false"`
- [ ] 5.3 `FLAG_SECURE` na janela
- [ ] 5.4 Clipboard com auto-limpeza (~30 s) + `EXTRA_IS_SENSITIVE` (Android 13+)
- [ ] 5.5 Revisar ausência de logs de segredos (inclusive build de debug)

## 6. Validação final (critérios de aceitação)
- [ ] 6.1 Build sem nenhuma dependência de terceiros no Gradle
- [ ] 6.2 Inspeção do banco não revela senhas nem a senha mestra
- [ ] 6.3 Cópia dos arquivos para outro ambiente não dá acesso sem a senha mestra
- [ ] 6.4 Após bloqueio, revelar senha exige novo desbloqueio
- [ ] 6.5 Aviso na UI: esquecer a senha mestra = perda total e irreversível
