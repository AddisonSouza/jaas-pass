package com.jaaspass.ui

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle

/**
 * Tarefa 5.4 — Cópia segura para a área de transferência:
 * - marca o conteúdo como **sensível** (`EXTRA_IS_SENSITIVE`, Android 13+/API 33), evitando preview;
 * - **auto-limpa** após ~30s, mas só se o conteúdo ainda for o nosso (não apaga algo que o
 *   usuário tenha copiado depois).
 */
object SecureClipboard {

    private const val CLEAR_DELAY_MS = 30_000L
    private val handler = Handler(Looper.getMainLooper())

    fun copySensitive(context: Context, label: String, secret: String) {
        val cm = context.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, secret)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        cm.setPrimaryClip(clip)

        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ clearIfStillOurs(cm, secret) }, CLEAR_DELAY_MS)
    }

    private fun clearIfStillOurs(cm: ClipboardManager, secret: String) {
        val current = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text
        if (current != null && current.toString() == secret) clear(cm)
    }

    private fun clear(cm: ClipboardManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            cm.clearPrimaryClip()
        } else {
            cm.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }
}
