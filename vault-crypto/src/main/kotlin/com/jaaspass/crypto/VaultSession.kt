package com.jaaspass.crypto

import javax.crypto.AEADBadTagException
import javax.crypto.SecretKey

/**
 * Estado e ciclo de vida da sessão do cofre (design §Arquitetura). Conecta [CryptoManager] e a
 * porta [VaultMetaStore]. A **DEK só existe em memória enquanto DESBLOQUEADO**; bloquear a descarta.
 *
 * Falha segura: senha incorreta ⇒ a decriptação autenticada falha (`AEADBadTagException`) e os
 * métodos retornam `false` sem revelar a causa (design §5). A senha mestra nunca é persistida.
 *
 * Os `CharArray` de senha recebidos NÃO são zerados aqui — é responsabilidade do chamador (a UI)
 * zerá-los após o uso (design §Endurecimento).
 */
class VaultSession(
    private val store: VaultMetaStore,
    private val crypto: CryptoManager = CryptoManager(),
    private val iterations: Int = CryptoManager.DEFAULT_ITERATIONS,
    private val autoLockTimeoutMs: Long = DEFAULT_AUTO_LOCK_MS,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    enum class State { LOCKED, UNLOCKED }

    @Volatile
    private var dek: SecretKey? = null

    @Volatile
    private var lastActivityAt: Long = 0L

    val state: State get() = if (dek != null) State.UNLOCKED else State.LOCKED

    /** True após o setup inicial (existe metadado de cofre). */
    val isInitialized: Boolean get() = store.loadMeta() != null

    /**
     * Tarefa 3.1 — Setup inicial: gera salt + DEK, deriva a KEK da senha mestra, persiste a DEK
     * cifrada. Deixa a sessão DESBLOQUEADA. Não grava a senha mestra em nenhuma forma.
     */
    fun setup(masterPassword: CharArray) {
        check(!isInitialized) { "cofre já inicializado" }
        val salt = crypto.generateSalt()
        val newDek = crypto.generateDEK()
        val kek = crypto.deriveKEK(masterPassword, salt, iterations)
        val wrapped = crypto.wrapDEK(newDek, kek)
        store.saveMeta(VaultMeta(CryptoManager.SCHEME_VERSION, iterations, salt, wrapped))
        dek = newDek
        touch()
    }

    /**
     * Tarefa 3.2 — Desbloqueio: deriva a KEK da senha digitada e tenta desenvelopar a DEK.
     * @return true se desbloqueou; false se a senha está errada (sem vazar a causa).
     */
    fun unlock(masterPassword: CharArray): Boolean {
        val meta = store.loadMeta() ?: return false
        val kek = crypto.deriveKEK(masterPassword, meta.salt, meta.iterations)
        return try {
            dek = crypto.unwrapDEK(meta.wrappedDek, kek)
            touch()
            true
        } catch (e: AEADBadTagException) {
            false // senha incorreta — permanece BLOQUEADO
        }
    }

    /** Tarefa 3.3 — Bloqueio: descarta a DEK da memória. */
    fun lock() {
        // Nota (design): SecretKeySpec copia o array internamente e não é trivialmente apagável;
        // mitigamos mantendo a vida da chave curta e bloqueando cedo.
        dek = null
    }

    /** A DEK atual; exige sessão DESBLOQUEADA. Atualiza o marcador de atividade. */
    fun requireDek(): SecretKey {
        val d = dek ?: throw IllegalStateException("cofre bloqueado")
        touch()
        return d
    }

    /**
     * Tarefa 3.4 — Troca da senha mestra: re-cifra **apenas a DEK** com a KEK da nova senha,
     * mantendo todas as entradas íntegras. Requer a senha atual correta.
     * @return true se trocou; false se a senha atual está errada.
     */
    fun changeMasterPassword(current: CharArray, new: CharArray): Boolean {
        val meta = store.loadMeta() ?: return false
        val kekCurrent = crypto.deriveKEK(current, meta.salt, meta.iterations)
        val currentDek = try {
            crypto.unwrapDEK(meta.wrappedDek, kekCurrent)
        } catch (e: AEADBadTagException) {
            return false
        }
        // Re-cifra a MESMA DEK com a KEK da nova senha (mesmo salt: re-cifra só a DEK, design §3.4).
        val kekNew = crypto.deriveKEK(new, meta.salt, iterations)
        val rewrapped = crypto.wrapDEK(currentDek, kekNew)
        store.updateWrappedDek(rewrapped)
        dek = currentDek
        touch()
        return true
    }

    /** Marca atividade recente (resetar o timer de inatividade). Chamar em interações da UI. */
    fun touch() {
        lastActivityAt = clock()
    }

    /**
     * Tarefa 3.3 — Auto-lock por timeout de inatividade. Bloqueia se o tempo desde a última
     * atividade exceder [autoLockTimeoutMs]. Chamar ao retomar o app / periodicamente.
     */
    fun enforceTimeout() {
        if (state == State.UNLOCKED && clock() - lastActivityAt >= autoLockTimeoutMs) lock()
    }

    companion object {
        const val DEFAULT_AUTO_LOCK_MS: Long = 5 * 60_000L // 5 minutos
    }
}
