package com.jaaspass.data

import com.jaaspass.crypto.CryptoManager
import com.jaaspass.crypto.VaultSession

/**
 * Fachada entre a UI (plaintext transitório) e a persistência cifrada. Cifra/decifra os campos das
 * entradas com a DEK da sessão; o plaintext só existe em memória durante a operação.
 * Toda operação exige a sessão DESBLOQUEADA (via [VaultSession.requireDek]).
 */
class Vault(
    private val repo: VaultRepository,
    private val session: VaultSession,
    private val crypto: CryptoManager = CryptoManager(),
) {
    /** Resumo para a lista (apenas o rótulo decriptado). */
    data class EntrySummary(val id: Long, val label: String)

    /** Entrada completa decriptada (uso transitório na tela de detalhe/edição). */
    data class PlainEntry(val id: Long, val label: String, val username: String?, val password: String)

    fun add(label: String, username: String?, password: String): Long {
        val dek = session.requireDek()
        return repo.insertEntry(
            label = crypto.encryptField(label.toByteArray(), dek),
            username = username?.takeIf { it.isNotEmpty() }?.let { crypto.encryptField(it.toByteArray(), dek) },
            password = crypto.encryptField(password.toByteArray(), dek),
        )
    }

    /** Edição: re-cifra todos os campos (novo nonce por campo) e regrava. */
    fun update(id: Long, label: String, username: String?, password: String) {
        val dek = session.requireDek()
        repo.updateEntry(
            id = id,
            label = crypto.encryptField(label.toByteArray(), dek),
            username = username?.takeIf { it.isNotEmpty() }?.let { crypto.encryptField(it.toByteArray(), dek) },
            password = crypto.encryptField(password.toByteArray(), dek),
        )
    }

    fun delete(id: Long) = repo.deleteEntry(id)

    /** Lista decriptando apenas os rótulos (só em sessão desbloqueada). */
    fun list(): List<EntrySummary> {
        val dek = session.requireDek()
        return repo.listEntries().map { EntrySummary(it.id, String(crypto.decryptField(it.label, dek))) }
    }

    fun detail(id: Long): PlainEntry? {
        val dek = session.requireDek()
        val e = repo.getEntry(id) ?: return null
        return PlainEntry(
            id = e.id,
            label = String(crypto.decryptField(e.label, dek)),
            username = e.username?.let { String(crypto.decryptField(it, dek)) },
            password = String(crypto.decryptField(e.password, dek)),
        )
    }
}
