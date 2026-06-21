package com.jaaspass.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import com.jaaspass.crypto.VaultSession
import com.jaaspass.data.Vault

/**
 * Tarefa 4.2 — Lista de entradas (rótulos decriptados em memória). Só funciona com a sessão
 * DESBLOQUEADA; se a sessão estiver bloqueada (ex.: após auto-lock em background), redireciona
 * para o desbloqueio. Inclui bloqueio manual (tarefa 3.3).
 */
class ListActivity : SecureActivity() {

    private lateinit var listView: ListView
    private lateinit var empty: TextView
    private var entries: List<Vault.EntrySummary> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        listView = ListView(this)
        empty = TextView(this).apply {
            text = "Nenhuma entrada ainda. Toque em “Adicionar”."
            setPadding(dp(8), dp(16), dp(8), dp(16))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(TextView(this@ListActivity).apply {
                text = "Cofre"; textSize = 22f
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                gravity = Gravity.CENTER_VERTICAL
            })
            addView(Button(this@ListActivity).apply {
                text = "Bloquear"
                setOnClickListener { lockAndExit() }
            })
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
            )
            addView(header)
            addView(Button(this@ListActivity).apply {
                text = "+ Adicionar"
                setOnClickListener {
                    startActivity(Intent(this@ListActivity, AddEditActivity::class.java))
                }
            })
            addView(empty)
            addView(listView)
        }
        setContentView(root)

        listView.setOnItemClickListener { _, _, pos, _ ->
            startActivity(
                Intent(this, DetailActivity::class.java)
                    .putExtra(DetailActivity.EXTRA_ID, entries[pos].id),
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
        entries = vault.list()
        listView.adapter = ArrayAdapter(
            this, android.R.layout.simple_list_item_1, entries.map { it.label },
        )
        empty.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        listView.visibility = if (entries.isEmpty()) View.GONE else View.VISIBLE
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
}
