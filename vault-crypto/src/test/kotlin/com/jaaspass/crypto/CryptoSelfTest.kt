package com.jaaspass.crypto

import java.security.SecureRandom
import javax.crypto.AEADBadTagException

/**
 * Tarefa 1.5 — Self-tests do [CryptoManager] com harness próprio (ZERO dependências de terceiros,
 * nem de teste). Rode com:  `./gradlew :vault-crypto:cryptoSelfTest`
 *
 * `main` retorna exit code != 0 se qualquer teste falhar (CI-friendly).
 */
private val crypto = CryptoManager(SecureRandom())
private const val ITER = 10_000 // iterações reduzidas só para os testes serem rápidos

private val results = mutableListOf<Pair<String, Boolean>>()

private fun test(name: String, body: () -> Unit) {
    val ok = try {
        body(); true
    } catch (t: Throwable) {
        println("   ↳ ${t::class.simpleName}: ${t.message}")
        false
    }
    results += name to ok
    println("${if (ok) "✅" else "❌"} $name")
}

private fun assert(cond: Boolean, msg: String) {
    if (!cond) throw AssertionError(msg)
}

/** True se [needle] aparece como subsequência contígua de [haystack]. */
private fun containsSubsequence(haystack: ByteArray, needle: ByteArray): Boolean {
    if (needle.isEmpty() || needle.size > haystack.size) return false
    outer@ for (i in 0..haystack.size - needle.size) {
        for (j in needle.indices) if (haystack[i + j] != needle[j]) continue@outer
        return true
    }
    return false
}

/** Store de metadados em memória para testar a [VaultSession] sem Android. */
private class FakeStore : VaultMetaStore {
    private var meta: VaultMeta? = null
    override fun loadMeta(): VaultMeta? = meta
    override fun saveMeta(meta: VaultMeta) { this.meta = meta }
    override fun updateAuthMaterial(salt: ByteArray, wrappedDek: ByteArray) {
        meta = (meta ?: error("sem meta")).copy(salt = salt, wrappedDek = wrappedDek)
    }
    override fun saveBiometricMaterial(wrappedDek: ByteArray, iv: ByteArray) {
        meta = (meta ?: error("sem meta")).copy(biometricWrappedDek = wrappedDek, biometricIv = iv)
    }
    override fun clearBiometricMaterial() {
        meta = meta?.copy(biometricWrappedDek = null, biometricIv = null)
    }
}

fun main() {
    println("== CryptoManager self-test ==")

    test("round-trip: wrap/unwrap DEK + encrypt/decrypt campo") {
        val salt = crypto.generateSalt()
        val kek = crypto.deriveKEK("senha-mestra-forte".toCharArray(), salt, ITER)
        val dek = crypto.generateDEK()
        val wrapped = crypto.wrapDEK(dek, kek)
        val dek2 = crypto.unwrapDEK(wrapped, kek)

        val plaintext = "minha-senha-secreta".toByteArray()
        val blob = crypto.encryptField(plaintext, dek2)
        val out = crypto.decryptField(blob, dek2)
        assert(out.contentEquals(plaintext), "decriptação não bate com o original")
    }

    test("senha mestra errada -> AEADBadTagException no unwrap") {
        val salt = crypto.generateSalt()
        val kekCerta = crypto.deriveKEK("certa".toCharArray(), salt, ITER)
        val dek = crypto.generateDEK()
        val wrapped = crypto.wrapDEK(dek, kekCerta)

        val kekErrada = crypto.deriveKEK("errada".toCharArray(), salt, ITER)
        var threw = false
        try {
            crypto.unwrapDEK(wrapped, kekErrada)
        } catch (e: AEADBadTagException) {
            threw = true
        }
        assert(threw, "esperava AEADBadTagException com a senha errada")
    }

    test("nonce único por regravação (mesmo plaintext -> blobs diferentes)") {
        val dek = crypto.generateDEK()
        val pt = "valor".toByteArray()
        val b1 = crypto.encryptField(pt, dek)
        val b2 = crypto.encryptField(pt, dek)
        val nonce1 = b1.copyOfRange(0, CryptoManager.NONCE_LENGTH)
        val nonce2 = b2.copyOfRange(0, CryptoManager.NONCE_LENGTH)
        assert(!nonce1.contentEquals(nonce2), "nonce foi reutilizado")
        assert(!b1.contentEquals(b2), "ciphertext idêntico entre regravações")
    }

    test("rejeição de adulteração (flip de 1 byte -> tag inválida)") {
        val dek = crypto.generateDEK()
        val blob = crypto.encryptField("integridade".toByteArray(), dek)
        blob[blob.size - 1] = (blob[blob.size - 1].toInt() xor 0x01).toByte() // adultera o tag
        var threw = false
        try {
            crypto.decryptField(blob, dek)
        } catch (e: AEADBadTagException) {
            threw = true
        }
        assert(threw, "blob adulterado deveria falhar na verificação da tag")
    }

    test("nenhum plaintext vaza no blob cifrado (suporte à task 2.2)") {
        val dek = crypto.generateDEK()
        val plaintext = "super-secreta-123".toByteArray()
        val blob = crypto.encryptField(plaintext, dek)
        assert(!containsSubsequence(blob, plaintext), "o plaintext aparece no blob cifrado")
        // o salt/iterações/versão são metadados não sensíveis; só o ciphertext+nonce é persistido.
    }

    test("sessão: setup -> DESBLOQUEADO; lock descarta a DEK (3.1/3.3)") {
        val s = VaultSession(FakeStore(), crypto, iterations = ITER)
        assert(!s.isInitialized, "não deveria estar inicializado antes do setup")
        s.setup("mestra".toCharArray())
        assert(s.state == VaultSession.State.UNLOCKED, "setup deveria deixar DESBLOQUEADO")
        assert(s.isInitialized, "deveria estar inicializado após setup")
        s.lock()
        assert(s.state == VaultSession.State.LOCKED, "lock deveria BLOQUEAR")
        var threw = false
        try { s.requireDek() } catch (e: IllegalStateException) { threw = true }
        assert(threw, "requireDek deveria falhar com a sessão bloqueada")
    }

    test("sessão: unlock com senha certa vs errada (3.2)") {
        val s = VaultSession(FakeStore(), crypto, iterations = ITER)
        s.setup("certa".toCharArray()); s.lock()
        assert(!s.unlock("errada".toCharArray()), "senha errada não deveria desbloquear")
        assert(s.state == VaultSession.State.LOCKED, "deveria continuar BLOQUEADO após senha errada")
        assert(s.unlock("certa".toCharArray()), "senha certa deveria desbloquear")
        assert(s.state == VaultSession.State.UNLOCKED, "deveria ficar DESBLOQUEADO")
    }

    test("sessão: troca de senha preserva os dados (3.4)") {
        val store = FakeStore()
        val s = VaultSession(store, crypto, iterations = ITER)
        s.setup("velha".toCharArray())
        val secret = "dado-importante".toByteArray()
        val blob = crypto.encryptField(secret, s.requireDek()) // cifrado com a DEK atual
        assert(s.changeMasterPassword("velha".toCharArray(), "nova".toCharArray()), "troca deveria funcionar")
        assert(!s.changeMasterPassword("velha".toCharArray(), "x".toCharArray()), "a senha velha não deveria mais valer")
        s.lock()
        assert(!s.unlock("velha".toCharArray()), "senha velha não deveria desbloquear após a troca")
        assert(s.unlock("nova".toCharArray()), "senha nova deveria desbloquear")
        assert(crypto.decryptField(blob, s.requireDek()).contentEquals(secret), "dado deveria continuar íntegro")
    }

    test("sessão: troca de senha rotaciona o salt (rotate-salt)") {
        val store = FakeStore()
        val s = VaultSession(store, crypto, iterations = ITER)
        s.setup("velha".toCharArray())
        val saltAntes = store.loadMeta()!!.salt.copyOf()
        assert(s.changeMasterPassword("velha".toCharArray(), "nova".toCharArray()), "troca deveria funcionar")
        val saltDepois = store.loadMeta()!!.salt
        assert(!saltAntes.contentEquals(saltDepois), "o salt deveria ser rotacionado na troca de senha")
        s.lock()
        assert(s.unlock("nova".toCharArray()), "nova senha deveria desbloquear com o salt novo")
    }

    test("sessão: senha atual incorreta não altera o material (rotate-salt)") {
        val store = FakeStore()
        val s = VaultSession(store, crypto, iterations = ITER)
        s.setup("velha".toCharArray())
        val saltAntes = store.loadMeta()!!.salt.copyOf()
        val wrappedAntes = store.loadMeta()!!.wrappedDek.copyOf()
        assert(!s.changeMasterPassword("errada".toCharArray(), "nova".toCharArray()), "senha atual errada não deveria trocar")
        val metaDepois = store.loadMeta()!!
        assert(saltAntes.contentEquals(metaDepois.salt), "salt não deveria mudar com a senha atual errada")
        assert(wrappedAntes.contentEquals(metaDepois.wrappedDek), "wrappedDek não deveria mudar com a senha atual errada")
    }

    test("sessão: auto-lock por timeout de inatividade (3.3)") {
        var now = 1_000L
        val s = VaultSession(FakeStore(), crypto, iterations = ITER, autoLockTimeoutMs = 60_000L, clock = { now })
        s.setup("m".toCharArray())
        now += 30_000L; s.enforceTimeout()
        assert(s.state == VaultSession.State.UNLOCKED, "não deveria bloquear antes do timeout")
        now += 31_000L; s.enforceTimeout()
        assert(s.state == VaultSession.State.LOCKED, "deveria bloquear após o timeout")
    }

    test("sessão: exportDekForBiometric exige DESBLOQUEADO (3.1)") {
        val s = VaultSession(FakeStore(), crypto, iterations = ITER)
        s.setup("m".toCharArray())
        // Desbloqueado: expõe a MESMA DEK (mesma instância da sessão).
        assert(s.exportDekForBiometric() === s.requireDek(), "deveria expor a DEK da sessão")
        s.lock()
        var threw = false
        try { s.exportDekForBiometric() } catch (e: IllegalStateException) { threw = true }
        assert(threw, "exportDekForBiometric deveria falhar com a sessão bloqueada")
    }

    test("sessão: unlockWithDek injeta a DEK sem senha (3.2)") {
        val s = VaultSession(FakeStore(), crypto, iterations = ITER)
        s.setup("m".toCharArray())
        val secret = "dado".toByteArray()
        val blob = crypto.encryptField(secret, s.requireDek())
        val dek = s.exportDekForBiometric() // simula a DEK decifrada pela biometria
        s.lock()
        assert(s.state == VaultSession.State.LOCKED, "deveria estar BLOQUEADO antes da injeção")
        s.unlockWithDek(dek)
        assert(s.state == VaultSession.State.UNLOCKED, "unlockWithDek deveria DESBLOQUEAR")
        assert(crypto.decryptField(blob, s.requireDek()).contentEquals(secret), "a DEK injetada deveria decifrar os dados")
    }

    val failed = results.count { !it.second }
    println("== ${results.size - failed}/${results.size} passaram ==")
    if (failed > 0) kotlin.system.exitProcess(1)
}
