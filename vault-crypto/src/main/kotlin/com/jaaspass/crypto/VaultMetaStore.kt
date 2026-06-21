package com.jaaspass.crypto

/**
 * Porta de armazenamento dos metadados do cofre (envelope da DEK). Abstrai a persistência para
 * que a lógica de sessão ([VaultSession]) fique no módulo JVM puro e testável sem Android.
 * No app, é implementada pelo `VaultRepository` (SQLite).
 */
interface VaultMetaStore {
    fun loadMeta(): VaultMeta?
    fun saveMeta(meta: VaultMeta)
    /** Re-grava apenas a DEK cifrada (troca de senha) — não toca nas entradas. */
    fun updateWrappedDek(wrappedDek: ByteArray)
}
