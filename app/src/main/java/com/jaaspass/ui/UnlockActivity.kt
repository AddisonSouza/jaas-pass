package com.jaaspass.ui

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
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

    private fun buildUnlock() {
        val pwd = Theme.passwordField(this, "Senha mestra")
        val card = Theme.card(this).apply {
            addView(Theme.titleText(this@UnlockActivity, "jaas-pass"))
            addView(pwd.view)
            addView(Theme.primaryButton(this@UnlockActivity, "Desbloquear").apply {
                setOnClickListener {
                    val chars = pwd.edit.extractChars()
                    val ok = session.unlock(chars)
                    Arrays.fill(chars, ' ')
                    pwd.edit.text.clear()
                    if (ok) goToList() else toast("Senha incorreta")
                }
            })
        }
        setContentView(Theme.screen(this).apply { addView(card) })
    }

    private fun buildSetup() {
        val pwd = Theme.passwordField(this, "Defina a senha mestra")
        val confirm = Theme.passwordField(this, "Confirme a senha mestra")
        val card = Theme.card(this).apply {
            addView(Theme.titleText(this@UnlockActivity, "Criar cofre"))
            addView(warning())
            addView(pwd.view)
            addView(confirm.view)
            addView(Theme.primaryButton(this@UnlockActivity, "Criar cofre").apply {
                setOnClickListener {
                    val a = pwd.edit.extractChars()
                    val b = confirm.edit.extractChars()
                    try {
                        when {
                            a.size < MIN_LEN -> toast("Use ao menos $MIN_LEN caracteres")
                            !a.contentEquals(b) -> toast("As senhas não coincidem")
                            else -> {
                                session.setup(a)
                                pwd.edit.text.clear(); confirm.edit.text.clear()
                                goToList()
                            }
                        }
                    } finally {
                        Arrays.fill(a, ' '); Arrays.fill(b, ' ')
                    }
                }
            })
        }
        setContentView(Theme.screen(this).apply { addView(card) })
    }

    private fun warning() = Theme.bodyText(
        this,
        "⚠️ A senha mestra não é armazenada e não pode ser recuperada. " +
            "Se você esquecê-la, perderá o acesso a todas as senhas — de forma definitiva.",
        muted = true,
    )

    private fun goToList() {
        startActivity(Intent(this, ListActivity::class.java))
        finish()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private companion object {
        const val MIN_LEN = 8
    }
}

/** Extrai a senha como `CharArray` (evita reter a senha em `String` imutável). */
internal fun EditText.extractChars(): CharArray {
    val e = text
    val arr = CharArray(e.length)
    e.getChars(0, e.length, arr, 0)
    return arr
}
