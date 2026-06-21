package com.jaaspass.ui

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import com.jaaspass.crypto.VaultSession

/**
 * Tarefa 4.4 — Adicionar entrada (e edição da 4.3, quando recebe [EXTRA_ID]).
 * Ao salvar uma edição, a camada [com.jaaspass.data.Vault] re-cifra com novo nonce (tarefa 3 spec).
 */
class AddEditActivity : SecureActivity() {

    private var id: Long = -1
    private lateinit var labelField: EditText
    private lateinit var userField: EditText
    private lateinit var passField: PasswordField

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        id = intent.getLongExtra(EXTRA_ID, -1)

        labelField = field("Serviço / rótulo", InputType.TYPE_CLASS_TEXT)
        userField = field("Usuário (opcional)", InputType.TYPE_CLASS_TEXT)
        // Senha agora nasce mascarada, com olho de mostrar/ocultar (antes: VISIBLE_PASSWORD sempre visível).
        passField = Theme.passwordField(this, "Senha")

        val title = if (id >= 0) "Editar entrada" else "Nova entrada"
        val card = Theme.card(this).apply {
            addView(Theme.titleText(this@AddEditActivity, title))
            addView(labelField)
            addView(userField)
            addView(passField.view)
            addView(Theme.primaryButton(this@AddEditActivity, "Salvar").apply { setOnClickListener { save() } })
        }
        setContentView(Theme.screen(this).apply { addView(card) })
    }

    override fun onResume() {
        super.onResume()
        if (session.state == VaultSession.State.LOCKED) {
            startActivity(Intent(this, UnlockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            finish()
            return
        }
        if (id >= 0 && labelField.text.isEmpty()) {
            vault.detail(id)?.let {
                labelField.setText(it.label)
                userField.setText(it.username ?: "")
                passField.edit.setText(it.password)
            }
        }
    }

    private fun field(hint: String, type: Int) = Theme.input(this, hint, type)

    private fun save() {
        val label = labelField.text.toString().trim()
        val user = userField.text.toString().trim().ifEmpty { null }
        val pass = passField.edit.text.toString()
        if (label.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Rótulo e senha são obrigatórios", Toast.LENGTH_SHORT).show()
            return
        }
        if (id >= 0) vault.update(id, label, user, pass) else vault.add(label, user, pass)
        finish()
    }

    companion object {
        const val EXTRA_ID = "entry_id"
    }
}
