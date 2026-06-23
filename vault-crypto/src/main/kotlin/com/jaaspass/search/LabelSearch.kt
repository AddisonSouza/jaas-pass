package com.jaaspass.search

import java.text.Normalizer
import java.util.Locale

/**
 * Casamento de busca por rótulo (change "search-entries-by-label").
 *
 * Função pura, sem dependências de Android nem de cripto — vive no módulo JVM `vault-crypto`
 * apenas por ser o lar do código puro testável por harness próprio (zero deps). A `app` a consome
 * para filtrar, em memória, os rótulos já decifrados por `Vault.list()`.
 *
 * O casamento é insensível a caixa e a acentos: ambos o rótulo e o termo são normalizados via
 * decomposição Unicode (NFD) com remoção de diacríticos (`\p{Mn}`) e `lowercase`. O casamento é
 * por substring ("contém"), não apenas prefixo.
 */
object LabelSearch {

    private val DIACRITICS = Regex("\\p{Mn}+")

    /** Normaliza para comparação: sem acento, sem caixa. */
    fun normalize(s: String): String =
        DIACRITICS.replace(Normalizer.normalize(s, Normalizer.Form.NFD), "").lowercase(Locale.ROOT)

    /**
     * `true` se [label] casa com [query]. Termo vazio (ou só espaços) casa com tudo, para que a
     * busca vazia mostre todas as entradas.
     */
    fun matches(label: String, query: String): Boolean {
        val q = normalize(query.trim())
        if (q.isEmpty()) return true
        return normalize(label).contains(q)
    }
}
