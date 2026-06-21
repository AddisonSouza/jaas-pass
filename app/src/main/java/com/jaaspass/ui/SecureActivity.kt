package com.jaaspass.ui

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import com.jaaspass.App
import com.jaaspass.crypto.VaultSession
import com.jaaspass.data.Vault

/**
 * Base de todas as telas. Aplica `FLAG_SECURE` (bloqueia screenshots / preview em recentes —
 * design §Endurecimento) e renova o marcador de atividade da sessão a cada interação (auto-lock).
 */
abstract class SecureActivity : Activity() {

    protected val session: VaultSession get() = App.session(this)
    protected val vault: Vault get() = App.vault(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (session.state == VaultSession.State.UNLOCKED) session.touch()
    }

    /** Converte dp em px. */
    protected fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
