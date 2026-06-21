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

    val failed = results.count { !it.second }
    println("== ${results.size - failed}/${results.size} passaram ==")
    if (failed > 0) kotlin.system.exitProcess(1)
}
