package com.jaaspass.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.jaaspass.crypto.VaultSession
import com.jaaspass.data.Vault

/**
 * Tarefa 4.3 — Detalhe da entrada: revela a senha sob demanda (mostrar/ocultar) e permite copiar,
 * editar e excluir. A senha só é decriptada em memória ao abrir; some ao sair.
 * (A cópia segura — auto-limpeza ~30s + EXTRA_IS_SENSITIVE — é endurecida na tarefa 5.4.)
 */
class DetailActivity : SecureActivity() {

    private var id: Long = -1
    private var entry: Vault.PlainEntry? = null
    private var revealed = false

    private lateinit var passwordView: TextView
    private lateinit var toggle: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        id = intent.getLongExtra(EXTRA_ID, -1)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
        passwordView = TextView(this).apply { textSize = 18f; setPadding(0, dp(8), 0, dp(8)) }
        toggle = Button(this).apply { text = "Mostrar"; setOnClickListener { toggleReveal() } }

        root.addView(labelView())
        root.addView(usernameView())
        root.addView(passwordView)
        root.addView(toggle)
        root.addView(Button(this).apply { text = "Copiar senha"; setOnClickListener { copyPassword() } })
        root.addView(Button(this).apply {
            text = "Editar"
            setOnClickListener {
                // this@DetailActivity.id: dentro de Button(...).apply{} o `id` cru seria o View.id do botão.
                startActivity(Intent(this@DetailActivity, AddEditActivity::class.java).putExtra(AddEditActivity.EXTRA_ID, this@DetailActivity.id))
            }
        })
        root.addView(Button(this).apply { text = "Excluir"; setOnClickListener { confirmDelete() } })
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        if (session.state == VaultSession.State.LOCKED) {
            startActivity(Intent(this, UnlockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            finish()
            return
        }
        entry = vault.detail(id)
        if (entry == null) { finish(); return } // entrada removida
        revealed = false
        render()
    }

    private val labelTv by lazy { TextView(this).apply { textSize = 22f; setPadding(0, 0, 0, dp(8)) } }
    private val userTv by lazy { TextView(this).apply { setPadding(0, 0, 0, dp(8)) } }
    private fun labelView() = labelTv
    private fun usernameView() = userTv

    private fun render() {
        val e = entry ?: return
        labelTv.text = e.label
        userTv.text = "Usuário: " + (e.username ?: "—")
        passwordView.text = if (revealed) e.password else "••••••••"
        toggle.text = if (revealed) "Ocultar" else "Mostrar"
    }

    private fun toggleReveal() { revealed = !revealed; render() }

    private fun copyPassword() {
        val e = entry ?: return
        SecureClipboard.copySensitive(this, "senha", e.password)
        Toast.makeText(this, "Senha copiada (marcada como sensível; limpa em ~30s)", Toast.LENGTH_SHORT).show()
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Excluir entrada")
            .setMessage("Esta ação não pode ser desfeita.")
            .setPositiveButton("Excluir") { _, _ -> vault.delete(id); finish() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    companion object {
        const val EXTRA_ID = "entry_id"
    }
}
