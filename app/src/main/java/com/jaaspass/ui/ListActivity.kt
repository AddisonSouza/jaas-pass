package com.jaaspass.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import com.jaaspass.crypto.VaultSession
import com.jaaspass.data.Vault
import com.jaaspass.search.LabelSearch

/**
 * Tarefa 4.2 — Lista de entradas (rótulos decriptados em memória). Só funciona com a sessão
 * DESBLOQUEADA; se a sessão estiver bloqueada (ex.: após auto-lock em background), redireciona
 * para o desbloqueio. Inclui bloqueio manual (tarefa 3.3).
 */
class ListActivity : SecureActivity() {

    private lateinit var listView: ListView
    private lateinit var empty: TextView
    private lateinit var search: EditText

    /** Todas as entradas decifradas em memória (sessão desbloqueada). */
    private var allEntries: List<Vault.EntrySummary> = emptyList()

    /** Subconjunto exibido após aplicar o termo de busca; é a fonte do clique. */
    private var filtered: List<Vault.EntrySummary> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        listView = ListView(this)
        empty = Theme.bodyText(this, EMPTY_VAULT, muted = true)
        search = Theme.searchField(this, "Buscar por rótulo").apply {
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) = applyFilter()
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            })
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(Theme.titleText(this@ListActivity, "Cofre").apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(Theme.secondaryButton(this@ListActivity, "Bloquear").apply {
                // No header (horizontal), o botão não deve esticar; sobrescreve a largura padrão.
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { leftMargin = dp(8) }
                setOnClickListener { lockAndExit() }
            })
        }

        // Cartão ocupa a altura toda; o ListView recebe peso para rolar internamente.
        val card = Theme.card(this).apply {
            layoutParams = (layoutParams as LinearLayout.LayoutParams).apply {
                height = ViewGroup.LayoutParams.MATCH_PARENT
            }
            addView(header)
            addView(Theme.primaryButton(this@ListActivity, "+ Adicionar").apply {
                setOnClickListener {
                    startActivity(Intent(this@ListActivity, AddEditActivity::class.java))
                }
            })
            addView(search)
            addView(empty)
            addView(listView, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f,
            ))
        }
        setContentView(Theme.staticScreen(this, card))

        listView.setOnItemClickListener { _, _, pos, _ ->
            startActivity(
                Intent(this, DetailActivity::class.java)
                    .putExtra(DetailActivity.EXTRA_ID, filtered[pos].id),
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (session.state == VaultSession.State.LOCKED) {
            goToUnlock()
            return
        }
        refresh()
    }

    private fun refresh() {
        allEntries = vault.list()
        applyFilter()
    }

    /**
     * Filtra [allEntries] pelo termo atual (em memória, sobre os rótulos já decifrados) e atualiza
     * a lista. Cofre vazio e "sem resultado" recebem mensagens distintas; a busca some quando não
     * há nenhuma entrada (nada a filtrar).
     */
    private fun applyFilter() {
        val query = search.text?.toString().orEmpty()
        filtered = allEntries.filter { LabelSearch.matches(it.label, query) }
        listView.adapter = ArrayAdapter(
            this, android.R.layout.simple_list_item_1, filtered.map { it.label },
        )

        search.visibility = if (allEntries.isEmpty()) View.GONE else View.VISIBLE
        empty.text = if (allEntries.isEmpty()) EMPTY_VAULT else NO_RESULTS
        val hasResults = filtered.isNotEmpty()
        empty.visibility = if (hasResults) View.GONE else View.VISIBLE
        listView.visibility = if (hasResults) View.VISIBLE else View.GONE
    }

    private fun lockAndExit() {
        session.lock()
        goToUnlock()
    }

    private fun goToUnlock() {
        startActivity(
            Intent(this, UnlockActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }

    private companion object {
        const val EMPTY_VAULT = "Nenhuma entrada ainda. Toque em “Adicionar”."
        const val NO_RESULTS = "Nenhum resultado."
    }
}
