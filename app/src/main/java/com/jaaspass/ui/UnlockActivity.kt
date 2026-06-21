package com.jaaspass.ui

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.Arrays

/**
 * Tarefa 4.1 — Tela de Desbloqueio (launcher). No primeiro uso, atua como **setup** da senha
 * mestra (com confirmação e aviso de perda irreversível — critério 6.5). Depois, desbloqueia.
 *
 * A senha é lida como `CharArray` e zerada após o uso (design §Endurecimento). Mensagens de erro
 * são genéricas (falha segura).
 */
class UnlockActivity : SecureActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (session.isInitialized) buildUnlock() else buildSetup()
    }

    private fun rootLayout(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(24), dp(24), dp(24), dp(24))
        layoutParams = ViewGroup.LayoutParams(MATCH, MATCH)
    }

    private fun passwordField(hint: String) = EditText(this).apply {
        this.hint = hint
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
    }

    private fun buildUnlock() {
        val pwd = passwordField("Senha mestra")
        val root = rootLayout().apply {
            addView(title("jaas-pass"))
            addView(pwd)
            addView(Button(this@UnlockActivity).apply {
                text = "Desbloquear"
                setOnClickListener {
                    val chars = pwd.extractChars()
                    val ok = session.unlock(chars)
                    Arrays.fill(chars, ' ')
                    pwd.text.clear()
                    if (ok) goToList() else toast("Senha incorreta")
                }
            })
        }
        setContentView(root)
    }

    private fun buildSetup() {
        val pwd = passwordField("Defina a senha mestra")
        val confirm = passwordField("Confirme a senha mestra")
        val root = rootLayout().apply {
            addView(title("Criar cofre"))
            addView(warning())
            addView(pwd)
            addView(confirm)
            addView(Button(this@UnlockActivity).apply {
                text = "Criar cofre"
                setOnClickListener {
                    val a = pwd.extractChars()
                    val b = confirm.extractChars()
                    try {
                        when {
                            a.size < MIN_LEN -> toast("Use ao menos $MIN_LEN caracteres")
                            !a.contentEquals(b) -> toast("As senhas não coincidem")
                            else -> {
                                session.setup(a)
                                pwd.text.clear(); confirm.text.clear()
                                goToList()
                            }
                        }
                    } finally {
                        Arrays.fill(a, ' '); Arrays.fill(b, ' ')
                    }
                }
            })
        }
        setContentView(root)
    }

    private fun title(text: String) = TextView(this).apply {
        this.text = text
        textSize = 24f
        setPadding(0, 0, 0, dp(16))
    }

    private fun warning() = TextView(this).apply {
        text = "⚠️ A senha mestra não é armazenada e não pode ser recuperada. " +
            "Se você esquecê-la, perderá o acesso a todas as senhas — de forma definitiva."
        setPadding(0, 0, 0, dp(16))
    }

    private fun goToList() {
        startActivity(Intent(this, ListActivity::class.java))
        finish()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private companion object {
        const val MIN_LEN = 8
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    }
}

/** Extrai a senha como `CharArray` (evita reter a senha em `String` imutável). */
internal fun EditText.extractChars(): CharArray {
    val e = text
    val arr = CharArray(e.length)
    e.getChars(0, e.length, arr, 0)
    return arr
}
