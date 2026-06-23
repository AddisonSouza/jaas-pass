package com.jaaspass.data

/**
 * Entrada persistida. TODOS os campos sensíveis são blobs cifrados (`nonce ‖ ct ‖ tag`) — o nonce
 * é o prefixo de 12 bytes de cada blob. `createdAt`/`updatedAt` são timestamps não sensíveis.
 *
 * (O `VaultMeta` vive em `:vault-crypto` — `com.jaaspass.crypto.VaultMeta` — pois é compartilhado
 * com a camada de sessão.)
 */
data class EncryptedEntry(
    val id: Long,
    val label: ByteArray,
    val username: ByteArray?,
    val password: ByteArray,
    val category: ByteArray?,
    val createdAt: Long,
    val updatedAt: Long,
)
