package com.jaaspass.data

/**
 * Metadados do cofre (linha única). Contém APENAS dados não sensíveis + a DEK **cifrada**
 * (design §Armazenamento). A senha mestra nunca aparece aqui em nenhuma forma.
 *
 * @property wrappedDek blob de cofre = DEK cifrada com a KEK (`nonce ‖ ct ‖ tag`).
 */
data class VaultMeta(
    val schemeVersion: Int,
    val iterations: Int,
    val salt: ByteArray,
    val wrappedDek: ByteArray,
)

/**
 * Entrada persistida. TODOS os campos sensíveis são blobs cifrados (`nonce ‖ ct ‖ tag`) — o nonce
 * é o prefixo de 12 bytes de cada blob. `createdAt`/`updatedAt` são timestamps não sensíveis.
 */
data class EncryptedEntry(
    val id: Long,
    val label: ByteArray,
    val username: ByteArray?,
    val password: ByteArray,
    val createdAt: Long,
    val updatedAt: Long,
)
