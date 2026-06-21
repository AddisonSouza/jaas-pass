package com.jaaspass.ui

import android.app.Activity
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import javax.crypto.Cipher

/**
 * Wrapper fino sobre o `android.hardware.biometrics.BiometricPrompt` nativo do SDK (API 28+),
 * mantendo a constraint de zero dependências (sem `androidx.biometric`). Amarra a autenticação ao
 * [Cipher] via `CryptoObject` (design §4): o sucesso entrega o `Cipher` liberado para de fato
 * (de)cifrar a DEK — não é um gate decorativo.
 */
object BiometricGate {

    /**
     * Exibe o prompt para o [cipher] dado.
     * @param onSuccess recebe o `Cipher` autenticado (pronto para `doFinal`).
     * @param onError chamado em cancelamento, toque no botão de fallback ou erro irrecuperável.
     */
    fun authenticate(
        activity: Activity,
        cipher: Cipher,
        title: String,
        subtitle: String,
        negativeLabel: String,
        onSuccess: (Cipher) -> Unit,
        onError: (CharSequence?) -> Unit,
    ) {
        val prompt = BiometricPrompt.Builder(activity)
            .setTitle(title)
            .setSubtitle(subtitle)
            // O clique no botão negativo chega via onAuthenticationError (ERROR_NEGATIVE_BUTTON);
            // o listener aqui é apenas obrigatório pela API.
            .setNegativeButton(negativeLabel, activity.mainExecutor) { _, _ -> }
            .build()
        prompt.authenticate(
            BiometricPrompt.CryptoObject(cipher),
            CancellationSignal(),
            activity.mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess(result.cryptoObject!!.cipher!!)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString)
                }
                // onAuthenticationFailed (uma tentativa que não bateu): o prompt segue ativo.
            },
        )
    }
}
