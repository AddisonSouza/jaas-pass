package com.jaaspass.crypto

import java.security.SecureRandom

/**
 * Gerador de senhas fortes (change "generate-strong-password"). Módulo Kotlin/JVM **puro** —
 * usa apenas `java.security.SecureRandom` (a mesma fonte de aleatoriedade do [CryptoManager];
 * zero dependências de terceiros, design §1).
 *
 * Política (design §Decisions):
 * - Comprimento padrão de [DEFAULT_LENGTH] caracteres.
 * - Garante ao menos um caractere de cada categoria (maiúscula, minúscula, dígito, símbolo).
 * - Exclui caracteres visualmente ambíguos (`I O l 0 1`) para reduzir erro de transcrição.
 * - Cada índice é sorteado com [SecureRandom.nextInt] (uniforme, **sem viés de módulo**).
 * - A sequência final é embaralhada com Fisher–Yates, para que as posições das categorias
 *   garantidas não fiquem previsíveis.
 *
 * Retorna `CharArray` (coerente com [CryptoManager.wipe]); o caller é responsável por zerá-lo.
 */
class PasswordGenerator(private val secureRandom: SecureRandom = SecureRandom()) {

    companion object {
        /** Comprimento padrão da senha gerada. */
        const val DEFAULT_LENGTH: Int = 20

        /** Categorias já depuradas dos caracteres ambíguos (`I`, `O`, `l`, `0`, `1`). */
        const val UPPER: String = "ABCDEFGHJKLMNPQRSTUVWXYZ" // sem I, O
        const val LOWER: String = "abcdefghijkmnopqrstuvwxyz" // sem l
        const val DIGITS: String = "23456789"                 // sem 0, 1
        const val SYMBOLS: String = "!@#\$%^&*()-_=+[]{}"      // sem aspas/crases/espaço

        private val POOLS: List<String> = listOf(UPPER, LOWER, DIGITS, SYMBOLS)

        /** Alfabeto unificado usado para preencher as posições não garantidas. */
        val ALPHABET: String = UPPER + LOWER + DIGITS + SYMBOLS
    }

    /**
     * Gera uma senha forte de [length] caracteres satisfazendo a política da classe.
     *
     * @throws IllegalArgumentException se [length] for menor que o número de categorias garantidas.
     */
    fun generate(length: Int = DEFAULT_LENGTH): CharArray {
        require(length >= POOLS.size) { "length deve ser >= ${POOLS.size} (uma por categoria)" }

        val out = CharArray(length)

        // 1) Garante ≥1 caractere de cada categoria nas primeiras posições...
        for (i in POOLS.indices) {
            val pool = POOLS[i]
            out[i] = pool[secureRandom.nextInt(pool.length)]
        }
        // 2) ...e preenche o restante a partir do alfabeto unificado.
        for (i in POOLS.size until length) {
            out[i] = ALPHABET[secureRandom.nextInt(ALPHABET.length)]
        }
        // 3) Embaralha (Fisher–Yates) para não vazar a posição das categorias garantidas.
        for (i in length - 1 downTo 1) {
            val j = secureRandom.nextInt(i + 1)
            val tmp = out[i]; out[i] = out[j]; out[j] = tmp
        }
        return out
    }
}
