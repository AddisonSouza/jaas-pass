package com.jaaspass.crypto

import java.security.SecureRandom

/**
 * Self-tests do [PasswordGenerator] com harness próprio (ZERO dependências de terceiros, nem de
 * teste — mesmo padrão do [CryptoSelfTest]). Rode com:  `./gradlew :vault-crypto:passwordGenSelfTest`
 *
 * `main` retorna exit code != 0 se qualquer teste falhar (CI-friendly).
 */
private val gen = PasswordGenerator(SecureRandom())

private const val AMBIGUOUS = "IOl01"

private val pgResults = mutableListOf<Pair<String, Boolean>>()

private fun pgTest(name: String, body: () -> Unit) {
    val ok = try {
        body(); true
    } catch (t: Throwable) {
        println("   ↳ ${t::class.simpleName}: ${t.message}")
        false
    }
    pgResults += name to ok
    println("${if (ok) "✅" else "❌"} $name")
}

private fun pgAssert(cond: Boolean, msg: String) {
    if (!cond) throw AssertionError(msg)
}

fun main() {
    println("== PasswordGenerator self-test ==")

    // Scenario "comprimento padrão"
    pgTest("comprimento padrão = 20") {
        pgAssert(gen.generate().size == PasswordGenerator.DEFAULT_LENGTH, "comprimento padrão deveria ser 20")
        pgAssert(gen.generate(32).size == 32, "comprimento explícito deveria ser respeitado")
    }

    // Scenario "length inválido" (task 1.4)
    pgTest("length < nº de categorias lança IllegalArgumentException") {
        var threw = false
        try { gen.generate(3) } catch (e: IllegalArgumentException) { threw = true }
        pgAssert(threw, "length=3 deveria lançar IllegalArgumentException")
    }

    // Scenario "todas as categorias"
    pgTest("contém ≥1 de cada categoria (amostra)") {
        repeat(500) {
            val p = String(gen.generate())
            pgAssert(p.any { it in PasswordGenerator.UPPER }, "faltou maiúscula em '$p'")
            pgAssert(p.any { it in PasswordGenerator.LOWER }, "faltou minúscula em '$p'")
            pgAssert(p.any { it in PasswordGenerator.DIGITS }, "faltou dígito em '$p'")
            pgAssert(p.any { it in PasswordGenerator.SYMBOLS }, "faltou símbolo em '$p'")
        }
    }

    // Scenario "não contém ambíguos"
    pgTest("nunca contém caracteres ambíguos I O l 0 1 (amostra)") {
        repeat(500) {
            val p = String(gen.generate())
            pgAssert(p.none { it in AMBIGUOUS }, "ambíguo encontrado em '$p'")
        }
    }

    // Scenario "cada geração é única"
    pgTest("gerações consecutivas são diferentes") {
        val seen = HashSet<String>()
        repeat(200) {
            val p = String(gen.generate())
            pgAssert(seen.add(p), "colisão inesperada: '$p'")
        }
    }

    // Scenario "distribuição uniforme" — sem viés de módulo (tolerância estatística folgada)
    pgTest("distribuição aproximadamente uniforme sobre o alfabeto") {
        val counts = HashMap<Char, Int>()
        val draws = 200_000
        repeat(draws / PasswordGenerator.DEFAULT_LENGTH) {
            for (c in gen.generate()) counts[c] = (counts[c] ?: 0) + 1
        }
        val alphabet = PasswordGenerator.ALPHABET
        val expected = draws.toDouble() / alphabet.length
        // Cada caractere deve aparecer; nenhum pode desviar > 40% do esperado (viés de módulo
        // produziria caracteres sistematicamente ~2x mais frequentes — muito além disso).
        for (c in alphabet) {
            val n = counts[c] ?: 0
            pgAssert(n > 0, "caractere '$c' nunca apareceu — alfabeto incompleto?")
            val ratio = n / expected
            pgAssert(ratio in 0.6..1.4, "frequência de '$c' fora da tolerância: ratio=$ratio")
        }
    }

    // Scenario "posição não previsível" — categorias garantidas não ficam fixas no início
    pgTest("posições das categorias são embaralhadas (não fixas no início)") {
        // Sem embaralhamento, a posição 0 seria SEMPRE maiúscula. Verifica que, numa amostra,
        // a posição 0 assume caracteres de mais de uma categoria.
        val categoriasNaPos0 = HashSet<String>()
        repeat(500) {
            val c0 = gen.generate()[0]
            val cat = when {
                c0 in PasswordGenerator.UPPER -> "U"
                c0 in PasswordGenerator.LOWER -> "L"
                c0 in PasswordGenerator.DIGITS -> "D"
                else -> "S"
            }
            categoriasNaPos0 += cat
        }
        pgAssert(categoriasNaPos0.size > 1, "posição 0 sempre da mesma categoria — sem embaralhamento?")
    }

    val failed = pgResults.count { !it.second }
    println("== ${pgResults.size - failed}/${pgResults.size} passaram ==")
    if (failed > 0) kotlin.system.exitProcess(1)
}
