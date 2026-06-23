package com.jaaspass.ui

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.jaaspass.data.Vault
import com.jaaspass.search.LabelSearch

/**
 * Lista do cofre agrupada por categoria (change "group-entries-by-category"). Achata os grupos numa
 * sequência de linhas tipadas — um cabeçalho por categoria seguido de suas entradas — e as expõe a
 * um [android.widget.ListView] com dois view types. Cabeçalhos não são clicáveis ([isEnabled]).
 *
 * O agrupamento/ordenação acontece em memória (a categoria é cifrada em disco; não dá para ordenar
 * em SQL): categorias ordenadas de forma insensível a caixa/acento via [LabelSearch.normalize], com
 * o grupo "Sem categoria" sempre por último; dentro do grupo as entradas mantêm a ordem recebida.
 */
class GroupedEntriesAdapter(private val context: Context) : BaseAdapter() {

    sealed interface Row {
        data class Header(val title: String) : Row
        data class Item(val entry: Vault.EntrySummary) : Row
    }

    private var rows: List<Row> = emptyList()

    fun submit(newRows: List<Row>) {
        rows = newRows
        notifyDataSetChanged()
    }

    /** Entrada na [position], ou `null` se a linha for um cabeçalho (clique deve ser ignorado). */
    fun itemAt(position: Int): Vault.EntrySummary? = (rows.getOrNull(position) as? Row.Item)?.entry

    override fun getCount(): Int = rows.size
    override fun getItem(position: Int): Any = rows[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getViewTypeCount(): Int = 2
    override fun getItemViewType(position: Int): Int =
        if (rows[position] is Row.Header) TYPE_HEADER else TYPE_ITEM

    // Cabeçalhos não são selecionáveis: não recebem clique nem destaque.
    override fun areAllItemsEnabled(): Boolean = false
    override fun isEnabled(position: Int): Boolean = rows[position] is Row.Item

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val row = rows[position]
        val tv = (convertView as? TextView) ?: TextView(context)
        when (row) {
            is Row.Header -> {
                tv.text = row.title
                tv.setTextColor(Theme.onSurfaceMuted)
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                tv.setTypeface(Typeface.DEFAULT_BOLD)
                tv.setPadding(0, dp(Theme.SPACE), 0, dp(Theme.SPACE / 2))
            }
            is Row.Item -> {
                tv.text = row.entry.label
                tv.setTextColor(Theme.onBackground)
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                tv.setTypeface(Typeface.DEFAULT)
                val padV = dp(Theme.SPACE)
                tv.setPadding(dp(Theme.SPACE / 2), padV, dp(Theme.SPACE / 2), padV)
            }
        }
        return tv
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1

        const val NO_CATEGORY = "Sem categoria"

        /**
         * Achata [entries] (já filtradas) em linhas agrupadas por categoria. Grupos sem categoria
         * vão para "Sem categoria", ao final. Categorias iguais a menos de caixa/acento são unidas,
         * exibindo a primeira grafia encontrada.
         */
        fun buildRows(entries: List<Vault.EntrySummary>): List<Row> {
            val groups = LinkedHashMap<String, MutableList<Vault.EntrySummary>>() // key = normalizada
            val displayName = HashMap<String, String>()
            val uncategorized = mutableListOf<Vault.EntrySummary>()

            for (e in entries) {
                val cat = e.category?.takeIf { it.isNotBlank() }
                if (cat == null) {
                    uncategorized.add(e)
                    continue
                }
                val key = LabelSearch.normalize(cat)
                groups.getOrPut(key) { mutableListOf() }.add(e)
                displayName.getOrPut(key) { cat.trim() }
            }

            val rows = mutableListOf<Row>()
            for (key in groups.keys.sorted()) {
                rows.add(Row.Header(displayName.getValue(key)))
                groups.getValue(key).forEach { rows.add(Row.Item(it)) }
            }
            if (uncategorized.isNotEmpty()) {
                rows.add(Row.Header(NO_CATEGORY))
                uncategorized.forEach { rows.add(Row.Item(it)) }
            }
            return rows
        }
    }
}
