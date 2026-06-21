package com.jaaspass.ui

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.os.SystemClock

/**
 * Tarefa 5.4 — Cópia segura para a área de transferência.
 *
 * Proteções:
 * - **EXTRA_IS_SENSITIVE** (Android 13+/API 33): o sistema oculta o conteúdo do preview/histórico.
 * - **Auto-limpeza ~30s**, em duas vias (porque apps em background NÃO podem ler/alterar o
 *   clipboard no Android 10+, e o processo pode ser congelado):
 *     1. um timer, que limpa se o app continuar em primeiro plano;
 *     2. [onForeground], chamado ao voltar ao app — limpa se já passou o tempo (cobre o caso de
 *        ter saído para colar e depois voltar).
 *   Só limpa se o conteúdo ainda for o nosso (não apaga algo copiado depois).
 *
 * Limitação inerente do Android: se o usuário copiar e **nunca** voltar ao app, não há como o app
 * limpar o clipboard em background — daí a marcação como sensível ser a proteção principal.
 */
object SecureClipboard {

    private const val CLEAR_DELAY_MS = 30_000L
    private val handler = Handler(Looper.getMainLooper())

    @Volatile private var armedSecret: String? = null
    @Volatile private var armedAt: Long = 0L

    fun copySensitive(context: Context, label: String, secret: String) {
        val cm = clipboard(context)
        val clip = ClipData.newPlainText(label, secret)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        cm.setPrimaryClip(clip)

        armedSecret = secret
        armedAt = SystemClock.elapsedRealtime()
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ tryClear(cm) }, CLEAR_DELAY_MS)
    }

    /** Chamar ao trazer o app ao primeiro plano (de uma Activity). */
    fun onForeground(context: Context) {
        if (armedSecret == null) return
        if (SystemClock.elapsedRealtime() - armedAt >= CLEAR_DELAY_MS) tryClear(clipboard(context))
    }

    private fun tryClear(cm: ClipboardManager) {
        val secret = armedSecret ?: return
        // Em background, getPrimaryClip retorna null/vazio → não conseguimos decidir: mantém armado
        // para tentar de novo ao voltar ao primeiro plano.
        val current = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString() ?: return
        if (current == secret) clear(cm)
        armedSecret = null // ou era o nosso (limpamos) ou o usuário copiou outra coisa: desarma
    }

    private fun clear(cm: ClipboardManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            cm.clearPrimaryClip()
        } else {
            cm.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }

    private fun clipboard(context: Context): ClipboardManager =
        context.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
}
