package com.jaaspass.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.widget.EditText
import android.widget.Toast
import com.jaaspass.App
import com.jaaspass.crypto.BiometricKeyStore
import com.jaaspass.crypto.VaultSession
import java.util.Arrays

/**
 * Tarefa 4.1 — Tela de Desbloqueio (launcher). No primeiro uso, atua como **setup** da senha
 * mestra (com confirmação e aviso de perda irreversível — critério 6.5). Depois, desbloqueia.
 *
 * Change biometric-unlock: além da senha mestra, oferece **biometria como atalho**. A senha mestra
 * permanece como raiz de confiança e fallback obrigatório (specs `biometric-unlock` /
 * `master-authentication`).
 *
 * A senha é lida como `CharArray` e zerada após o uso (design §Endurecimento). Mensagens de erro
 * são genéricas (falha segura).
 */
class UnlockActivity : SecureActivity() {

    private val keyStore = BiometricKeyStore()
    private val repo get() = App.repo(this)
    private var biometricPromptInFlight = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (session.isInitialized) buildUnlock() else buildSetup()
    }

    override fun onResume() {
        super.onResume()
        // Disparo automático do prompt: cofre bloqueado, biometria ativa e disponível (spec 6.1).
        if (session.isInitialized && session.state == VaultSession.State.LOCKED &&
            biometricEnabled() && BiometricKeyStore.isAvailable(this) && !biometricPromptInFlight
        ) {
            promptBiometricUnlock()
        }
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
                    if (ok) onUnlocked() else toast("Senha incorreta")
                }
            })
            // Atalho biométrico: fallback sempre visível (a senha acima) + retomar prompt / desativar.
            if (biometricEnabled() && BiometricKeyStore.isAvailable(this@UnlockActivity)) {
                addView(Theme.secondaryButton(this@UnlockActivity, "Usar biometria").apply {
                    setOnClickListener { promptBiometricUnlock() }
                })
                addView(Theme.secondaryButton(this@UnlockActivity, "Desativar biometria", destructive = true).apply {
                    setOnClickListener {
                        disableBiometric()
                        toast("Biometria desativada")
                        recreate()
                    }
                })
            }
        }
        setContentView(Theme.screen(this, card))
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
                                onUnlocked()
                            }
                        }
                    } finally {
                        Arrays.fill(a, ' '); Arrays.fill(b, ' ')
                    }
                }
            })
        }
        setContentView(Theme.screen(this, card))
    }

    /** Pós-desbloqueio por senha: oferece ativar a biometria, senão segue para a lista. */
    private fun onUnlocked() {
        if (BiometricKeyStore.isAvailable(this) && !biometricEnabled()) offerBiometric() else goToList()
    }

    // --- Biometria (change biometric-unlock) ---

    private fun biometricEnabled(): Boolean = repo.loadMeta()?.biometricWrappedDek != null

    /** Oferta opt-in após desbloqueio com a sessão DESBLOQUEADA (spec: oferta após desbloqueio). */
    private fun offerBiometric() {
        AlertDialog.Builder(this)
            .setTitle("Ativar biometria?")
            .setMessage(
                "Desbloqueie o cofre com sua biometria nas próximas vezes. " +
                    "A senha mestra continua válida e é exigida como alternativa.",
            )
            .setPositiveButton("Ativar") { _, _ -> enableBiometric() }
            .setNegativeButton("Agora não") { _, _ -> goToList() }
            .setOnCancelListener { goToList() }
            .show()
    }

    /** Envelopa a DEK atual com a chave do Keystore liberada pela biometria e persiste (spec ativação). */
    private fun enableBiometric() {
        // A sessão pode ter sido bloqueada (auto-lock em background) com o diálogo de oferta aberto.
        // Sem a DEK em memória não há o que envelopar — segue para o desbloqueio sem ativar.
        if (session.state != VaultSession.State.UNLOCKED) {
            toast("Sessão bloqueada. Tente ativar a biometria após desbloquear.")
            recreate(); return
        }
        val cipher = try {
            keyStore.cipherForEncrypt()
        } catch (e: Exception) {
            toast("Não foi possível ativar a biometria"); goToList(); return
        }
        val dek = session.exportDekForBiometric() // exige sessão DESBLOQUEADA
        BiometricGate.authenticate(
            activity = this,
            cipher = cipher,
            title = "Ativar biometria",
            subtitle = "Confirme sua biometria para proteger o cofre",
            negativeLabel = "Cancelar",
            onSuccess = { c ->
                try {
                    val (ct, iv) = keyStore.wrap(dek, c)
                    repo.saveBiometricMaterial(ct, iv) // grava só ciphertext + IV (sem senha mestra)
                    toast("Biometria ativada")
                } catch (e: Exception) {
                    toast("Falha ao ativar a biometria")
                }
                goToList()
            },
            onError = { goToList() },
        )
    }

    /** Desbloqueio por biometria: decifra a DEK e injeta na sessão, sem PBKDF2/senha mestra. */
    private fun promptBiometricUnlock() {
        val meta = repo.loadMeta() ?: return
        val ct = meta.biometricWrappedDek ?: return
        val iv = meta.biometricIv ?: return
        val cipher = try {
            keyStore.cipherForDecrypt(iv)
        } catch (e: KeyPermanentlyInvalidatedException) {
            // Nova biometria cadastrada invalidou a chave → exige senha mestra (spec invalidação).
            disableBiometric()
            toast("Biometria alterada. Use a senha mestra.")
            recreate()
            return
        } catch (e: Exception) {
            disableBiometric()
            recreate()
            return
        }
        biometricPromptInFlight = true
        BiometricGate.authenticate(
            activity = this,
            cipher = cipher,
            title = "Desbloquear jaas-pass",
            subtitle = "Use sua biometria",
            negativeLabel = "Usar senha mestra",
            onSuccess = { c ->
                biometricPromptInFlight = false
                try {
                    session.unlockWithDek(keyStore.unwrap(ct, c))
                    goToList()
                } catch (e: Exception) {
                    toast("Falha ao desbloquear") // permanece bloqueado; fallback por senha
                }
            },
            // Cancelar / falha: permanece BLOQUEADO com o campo de senha visível como fallback.
            onError = { biometricPromptInFlight = false },
        )
    }

    /** Remove o envelope biométrico e apaga a chave do Keystore (desativar / invalidação). */
    private fun disableBiometric() {
        repo.clearBiometricMaterial()
        keyStore.deleteKey()
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
