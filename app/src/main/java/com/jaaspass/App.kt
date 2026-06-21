package com.jaaspass

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.jaaspass.crypto.VaultSession
import com.jaaspass.data.Vault
import com.jaaspass.data.VaultRepository

/**
 * Application: dona da [VaultSession] (escopo de processo) e responsável pelo **auto-lock**
 * (tarefa 3.3 / spec "Auto-bloqueio ao ir para segundo plano").
 *
 * - **Background:** quando a última Activity visível para (e não é troca de configuração, ex.:
 *   rotação), descarta a DEK imediatamente.
 * - **Retorno:** ao voltar para o app, aplica o timeout de inatividade.
 */
class App : Application() {

    lateinit var session: VaultSession
        private set

    lateinit var vault: Vault
        private set

    lateinit var repo: VaultRepository
        private set

    private var startedActivities = 0

    override fun onCreate() {
        super.onCreate()
        repo = VaultRepository(this)
        session = VaultSession(repo)
        vault = Vault(repo, session)
        registerActivityLifecycleCallbacks(AutoLockCallbacks())
    }

    private inner class AutoLockCallbacks : ActivityLifecycleCallbacks {
        override fun onActivityStarted(activity: Activity) {
            startedActivities++
            // Voltou ao primeiro plano: bloqueia se passou do timeout de inatividade.
            session.enforceTimeout()
        }

        override fun onActivityStopped(activity: Activity) {
            startedActivities--
            // App foi para segundo plano (e não é rotação/config change) → descarta a DEK.
            if (startedActivities == 0 && !activity.isChangingConfigurations) {
                session.lock()
            }
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }

    companion object {
        /** Acesso à sessão a partir de uma Activity. */
        fun session(activity: Activity): VaultSession = (activity.application as App).session

        /** Acesso à fachada do cofre a partir de uma Activity. */
        fun vault(activity: Activity): Vault = (activity.application as App).vault

        /** Acesso ao repositório (material biométrico) a partir de uma Activity. */
        fun repo(activity: Activity): VaultRepository = (activity.application as App).repo
    }
}
