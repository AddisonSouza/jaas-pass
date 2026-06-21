package com.jaaspass

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.widget.TextView

/**
 * Placeholder de scaffold — apenas para validar o build do APK.
 *
 * A implementação real (Unlock, Lista, Detalhe/Editar, Adicionar + CryptoManager/VaultRepository)
 * entra pelas tasks via /ps:apply, seguindo proposal/spec/design. Usa android.app.Activity do
 * framework (NÃO AppCompat/AndroidX) para respeitar a constraint de zero dependências de terceiros.
 */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // FLAG_SECURE (design §Endurecimento): bloqueia screenshots e o preview na tela de recentes.
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContentView(
            TextView(this).apply {
                text = "jaas-pass — scaffold pronto.\nImplementação via /ps:apply."
                setPadding(48, 48, 48, 48)
                textSize = 18f
            }
        )
    }
}
