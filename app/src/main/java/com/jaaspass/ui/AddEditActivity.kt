package com.jaaspass.ui

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
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
    private lateinit var passField: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        id = intent.getLongExtra(EXTRA_ID, -1)

        labelField = field("Serviço / rótulo", InputType.TYPE_CLASS_TEXT)
        userField = field("Usuário (opcional)", InputType.TYPE_CLASS_TEXT)
        passField = field("Senha", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
            )
            addView(TextView(this@AddEditActivity).apply {
                text = if (id >= 0) "Editar entrada" else "Nova entrada"
                textSize = 22f; setPadding(0, 0, 0, dp(12))
            })
            addView(labelField)
            addView(userField)
            addView(passField)
            addView(Button(this@AddEditActivity).apply { text = "Salvar"; setOnClickListener { save() } })
        }
        setContentView(root)
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
                passField.setText(it.password)
            }
        }
    }

    private fun field(hint: String, type: Int) = EditText(this).apply {
        this.hint = hint
        inputType = type
    }

    private fun save() {
        val label = labelField.text.toString().trim()
        val user = userField.text.toString().trim().ifEmpty { null }
        val pass = passField.text.toString()
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
