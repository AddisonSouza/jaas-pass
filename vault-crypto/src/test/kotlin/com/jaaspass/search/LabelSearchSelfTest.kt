package com.jaaspass.search

/**
 * Self-tests do [LabelSearch] com harness próprio (ZERO dependências, nem de teste — mesmo padrão
 * de [com.jaaspass.crypto.PasswordGeneratorSelfTest]). Rode com:
 *   `./gradlew :vault-crypto:labelSearchSelfTest`
 *
 * `main` retorna exit code != 0 se qualquer teste falhar (CI-friendly).
 */
private val lsResults = mutableListOf<Pair<String, Boolean>>()

private fun lsTest(name: String, body: () -> Unit) {
    val ok = try {
        body(); true
    } catch (t: Throwable) {
        println("   ↳ ${t::class.simpleName}: ${t.message}")
        false
    }
    lsResults += name to ok
    println("${if (ok) "✅" else "❌"} $name")
}

private fun lsAssert(cond: Boolean, msg: String) {
    if (!cond) throw AssertionError(msg)
}

fun main() {
    println("== LabelSearch self-test ==")

    // Scenario "Ignora caixa"
    lsTest("ignora caixa: 'gmail' casa 'Gmail'") {
        lsAssert(LabelSearch.matches("Gmail", "gmail"), "'gmail' deveria casar 'Gmail'")
        lsAssert(LabelSearch.matches("GMAIL", "gMaIl"), "casamento deveria ignorar caixa")
    }

    // Scenario "Ignora acento"
    lsTest("ignora acento: 'joao' casa 'João'") {
        lsAssert(LabelSearch.matches("João", "joao"), "'joao' deveria casar 'João'")
        lsAssert(LabelSearch.matches("Inscrição", "inscricao"), "diacríticos deveriam ser ignorados")
    }

    // Scenario "Casa no meio do rótulo" (substring, não só prefixo)
    lsTest("casa substring no meio: 'bank' casa 'My Bank App'") {
        lsAssert(LabelSearch.matches("My Bank App", "bank"), "'bank' deveria casar no meio do rótulo")
        lsAssert(!LabelSearch.matches("My App", "bank"), "'bank' não deveria casar 'My App'")
    }

    // Scenario "Busca vazia mostra tudo"
    lsTest("termo vazio (ou só espaços) casa com tudo") {
        lsAssert(LabelSearch.matches("Qualquer", ""), "termo vazio deveria casar tudo")
        lsAssert(LabelSearch.matches("Qualquer", "   "), "termo só com espaços deveria casar tudo")
    }

    // normalize é idempotente e remove acento/caixa
    lsTest("normalize remove acento e caixa") {
        lsAssert(LabelSearch.normalize("Ámbar") == "ambar", "normalize deveria produzir 'ambar'")
        lsAssert(
            LabelSearch.normalize(LabelSearch.normalize("Café")) == LabelSearch.normalize("Café"),
            "normalize deveria ser idempotente",
        )
    }

    val failed = lsResults.count { !it.second }
    println("== ${lsResults.size - failed}/${lsResults.size} passaram ==")
    if (failed > 0) kotlin.system.exitProcess(1)
}
