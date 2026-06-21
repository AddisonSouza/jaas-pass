package com.jaaspass.crypto

/**
 * Porta de armazenamento dos metadados do cofre (envelope da DEK). Abstrai a persistência para
 * que a lógica de sessão ([VaultSession]) fique no módulo JVM puro e testável sem Android.
 * No app, é implementada pelo `VaultRepository` (SQLite).
 */
interface VaultMetaStore {
    fun loadMeta(): VaultMeta?
    fun saveMeta(meta: VaultMeta)
    /**
     * Re-grava o material de autenticação (salt + DEK cifrada) na troca de senha, de forma
     * atômica — não toca nas entradas. O salt é rotacionado a cada troca.
     */
    fun updateAuthMaterial(salt: ByteArray, wrappedDek: ByteArray)

    /**
     * Grava o envelope biométrico da DEK (ativar biometria), sem tocar em salt/wrappedDek.
     * @param wrappedDek DEK cifrada pela chave do Keystore.
     * @param iv IV do GCM correspondente.
     */
    fun saveBiometricMaterial(wrappedDek: ByteArray, iv: ByteArray)

    /** Remove o envelope biométrico (desativar biometria / chave invalidada). */
    fun clearBiometricMaterial()
}
