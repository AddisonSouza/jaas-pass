package com.jaaspass.crypto

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Peça **Android-específica** do atalho biométrico (change biometric-unlock). Mantém o
 * `vault-crypto` puro: tudo que toca `android.security.keystore` / `BiometricManager` vive aqui,
 * no módulo `app`.
 *
 * Modelo (design §2-§4): uma chave AES no Android Keystore, exigindo autenticação do usuário e
 * invalidada por novo cadastro biométrico, **envelopa a DEK** (não a KEK/senha). O material da
 * chave nunca sai do Keystore; apenas um [Cipher] inicializado por ela viaja no `CryptoObject` do
 * `BiometricPrompt`. O IV do GCM é gerado pela chave (lido de `cipher.iv`) e guardado à parte.
 *
 * Falha segura: se a chave foi invalidada por novo cadastro biométrico, `Cipher.init` lança
 * [KeyPermanentlyInvalidatedException] — o caller limpa o material e cai para a senha mestra.
 */
class BiometricKeyStore {

    /** [Cipher] em modo cifragem (ativação). Recria a chave para ficar atrelada ao cadastro atual. */
    fun cipherForEncrypt(): Cipher {
        val key = createKey()
        return Cipher.getInstance(TRANSFORM).apply { init(Cipher.ENCRYPT_MODE, key) }
    }

    /**
     * [Cipher] em modo decifragem (desbloqueio), preso ao [iv] persistido.
     * @throws KeyPermanentlyInvalidatedException se a chave foi invalidada (novo cadastro biométrico).
     * @throws java.security.UnrecoverableKeyException/KeyStoreException se a chave não existe.
     */
    fun cipherForDecrypt(iv: ByteArray): Cipher {
        val key = loadKey() ?: throw IllegalStateException("chave biométrica ausente")
        return Cipher.getInstance(TRANSFORM).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        }
    }

    /**
     * Envelopa a DEK com o [cipher] já liberado pela biometria. Retorna `(ciphertext, iv)`.
     * O IV é gerado pela chave do Keystore e deve ser persistido junto ao ciphertext.
     */
    fun wrap(dek: SecretKey, cipher: Cipher): Pair<ByteArray, ByteArray> {
        val raw = dek.encoded
        try {
            val ct = cipher.doFinal(raw)
            return ct to cipher.iv
        } finally {
            java.util.Arrays.fill(raw, 0.toByte())
        }
    }

    /** Desenvelopa a DEK com o [cipher] já liberado pela biometria. */
    fun unwrap(wrappedDek: ByteArray, cipher: Cipher): SecretKey {
        val raw = cipher.doFinal(wrappedDek)
        try {
            return SecretKeySpec(raw, KeyProperties.KEY_ALGORITHM_AES)
        } finally {
            java.util.Arrays.fill(raw, 0.toByte())
        }
    }

    /** Apaga a chave do Keystore (desativar biometria / chave invalidada). Idempotente. */
    fun deleteKey() {
        keyStore().apply { if (containsAlias(ALIAS)) deleteEntry(ALIAS) }
    }

    private fun createKey(): SecretKey {
        deleteKey() // chave nova a cada ativação
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_BITS)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)
                .build(),
        )
        return gen.generateKey()
    }

    private fun loadKey(): SecretKey? = keyStore().getKey(ALIAS, null) as? SecretKey

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val ALIAS = "jaaspass.biometric.dek"
        private const val KEY_BITS = 256
        private const val TAG_BITS = 128
        private const val TRANSFORM =
            "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/${KeyProperties.ENCRYPTION_PADDING_NONE}"

        /**
         * Biometria utilizável neste aparelho (hardware presente e — quando detectável — com
         * credencial cadastrada). Em API ≥ 29 usa [BiometricManager.canAuthenticate]; em API 28
         * (sem `BiometricManager`) checa apenas o hardware via `PackageManager`, deixando o
         * cadastro ser validado pelo próprio `BiometricPrompt` (falha tratada com fallback).
         */
        @Suppress("DEPRECATION")
        fun isAvailable(context: Context): Boolean =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val bm = context.getSystemService(BiometricManager::class.java)
                bm != null && bm.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
            } else {
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
            }
    }
}
