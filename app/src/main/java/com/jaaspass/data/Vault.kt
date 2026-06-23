package com.jaaspass.data

import com.jaaspass.crypto.CryptoManager
import com.jaaspass.crypto.VaultSession
import com.jaaspass.search.LabelSearch

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
    /** Resumo para a lista (rótulo + categoria decriptados; `category` nulo ≡ "Sem categoria"). */
    data class EntrySummary(val id: Long, val label: String, val category: String?)

    /** Entrada completa decriptada (uso transitório na tela de detalhe/edição). */
    data class PlainEntry(val id: Long, val label: String, val username: String?, val password: String, val category: String?) {
        /** Tarefa 5.5 — nunca expor a senha em logs/stacktraces acidentalmente. */
        override fun toString(): String = "PlainEntry(id=$id, label=$label, username=$username, password=***, category=$category)"
    }

    fun add(label: String, username: String?, password: String, category: String?): Long {
        val dek = session.requireDek()
        return repo.insertEntry(
            label = crypto.encryptField(label.toByteArray(), dek),
            username = username?.takeIf { it.isNotEmpty() }?.let { crypto.encryptField(it.toByteArray(), dek) },
            password = crypto.encryptField(password.toByteArray(), dek),
            category = category?.takeIf { it.isNotBlank() }?.let { crypto.encryptField(it.trim().toByteArray(), dek) },
        )
    }

    /** Edição: re-cifra todos os campos (novo nonce por campo) e regrava. */
    fun update(id: Long, label: String, username: String?, password: String, category: String?) {
        val dek = session.requireDek()
        repo.updateEntry(
            id = id,
            label = crypto.encryptField(label.toByteArray(), dek),
            username = username?.takeIf { it.isNotEmpty() }?.let { crypto.encryptField(it.toByteArray(), dek) },
            password = crypto.encryptField(password.toByteArray(), dek),
            category = category?.takeIf { it.isNotBlank() }?.let { crypto.encryptField(it.trim().toByteArray(), dek) },
        )
    }

    fun delete(id: Long) = repo.deleteEntry(id)

    /** Lista decriptando rótulo e categoria (só em sessão desbloqueada). */
    fun list(): List<EntrySummary> {
        val dek = session.requireDek()
        return repo.listEntries().map {
            EntrySummary(
                id = it.id,
                label = String(crypto.decryptField(it.label, dek)),
                category = it.category?.let { c -> String(crypto.decryptField(c, dek)) },
            )
        }
    }

    /**
     * Categorias já cadastradas (distintas), para sugestão no autocomplete. Derivadas em memória das
     * entradas decifradas; deduplicadas de forma insensível a caixa/acento (via [LabelSearch.normalize]),
     * preservando a primeira grafia encontrada e ordenadas de forma estável pela forma normalizada.
     */
    fun categories(): List<String> {
        val seen = HashSet<String>()
        return list().mapNotNull { it.category?.takeIf { c -> c.isNotBlank() } }
            .filter { seen.add(LabelSearch.normalize(it)) }
            .sortedBy { LabelSearch.normalize(it) }
    }

    fun detail(id: Long): PlainEntry? {
        val dek = session.requireDek()
        val e = repo.getEntry(id) ?: return null
        return PlainEntry(
            id = e.id,
            label = String(crypto.decryptField(e.label, dek)),
            username = e.username?.let { String(crypto.decryptField(it, dek)) },
            password = String(crypto.decryptField(e.password, dek)),
            category = e.category?.let { String(crypto.decryptField(it, dek)) },
        )
    }
}
