package com.jaaspass.crypto

/**
 * Metadados do cofre (linha única). Contém APENAS dados não sensíveis + a DEK **cifrada**
 * (design §Armazenamento). A senha mestra nunca aparece aqui em nenhuma forma.
 *
 * @property wrappedDek blob de cofre = DEK cifrada com a KEK (`nonce ‖ ct ‖ tag`).
 * @property biometricWrappedDek DEK cifrada por uma chave do Android Keystore protegida por
 *   biometria (atalho de desbloqueio). `null` quando a biometria não está ativa. Diferente do
 *   `wrappedDek`, o IV do GCM é guardado à parte em [biometricIv] (a chave do Keystore é dona do IV).
 * @property biometricIv IV do GCM usado em [biometricWrappedDek]; `null` quando a biometria não está ativa.
 */
data class VaultMeta(
    val schemeVersion: Int,
    val iterations: Int,
    val salt: ByteArray,
    val wrappedDek: ByteArray,
    val biometricWrappedDek: ByteArray? = null,
    val biometricIv: ByteArray? = null,
)
