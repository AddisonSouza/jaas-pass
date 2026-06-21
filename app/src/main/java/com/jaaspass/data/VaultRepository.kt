package com.jaaspass.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.jaaspass.crypto.VaultMeta
import com.jaaspass.crypto.VaultMetaStore

/**
 * Tarefa 2.1 — Persistência local via SQLite (`android.database.sqlite`, parte do SDK).
 *
 * Armazena **apenas** (design §Armazenamento):
 * - `entries`: blobs cifrados dos campos (label/username/password) — o nonce é o prefixo do blob.
 * - `vault_meta`: salt, nº de iterações, versão do esquema cripto e o blob de cofre (DEK cifrada).
 *
 * **NENHUM plaintext** é gravado: o repositório recebe e devolve apenas `ByteArray` já cifrados
 * pela camada [com.jaaspass.crypto.CryptoManager]. Ele não conhece chaves nem faz cripto.
 */
class VaultRepository(context: Context) : VaultMetaStore {

    private val helper = VaultDbHelper(context.applicationContext)

    // --- Metadados do cofre (implementa VaultMetaStore) ---

    /** True se o cofre já foi inicializado (primeiro uso concluído). */
    fun isInitialized(): Boolean =
        helper.readableDatabase.rawQuery("SELECT 1 FROM $META WHERE id = 1", null).use { it.moveToFirst() }

    /** Persiste/atualiza a linha única de metadados (setup inicial). */
    override fun saveMeta(meta: VaultMeta) {
        val values = ContentValues().apply {
            put("id", 1)
            put(META_SCHEME, meta.schemeVersion)
            put(META_ITER, meta.iterations)
            put(META_SALT, meta.salt)
            put(META_WRAPPED_DEK, meta.wrappedDek)
        }
        helper.writableDatabase.insertWithOnConflict(META, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    override fun loadMeta(): VaultMeta? =
        helper.readableDatabase.query(META, null, "id = 1", null, null, null, null).use { c ->
            if (!c.moveToFirst()) return null
            VaultMeta(
                schemeVersion = c.getInt(c.getColumnIndexOrThrow(META_SCHEME)),
                iterations = c.getInt(c.getColumnIndexOrThrow(META_ITER)),
                salt = c.getBlob(c.getColumnIndexOrThrow(META_SALT)),
                wrappedDek = c.getBlob(c.getColumnIndexOrThrow(META_WRAPPED_DEK)),
            )
        }

    /**
     * Troca de senha: re-grava salt (rotacionado) + DEK cifrada de forma atômica, sem tocar nas
     * entradas. Envolto em transação (tudo-ou-nada) para nunca deixar salt e DEK divergentes.
     */
    override fun updateAuthMaterial(salt: ByteArray, wrappedDek: ByteArray) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put(META_SALT, salt)
                put(META_WRAPPED_DEK, wrappedDek)
            }
            db.update(META, values, "id = 1", null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // --- Entradas (CRUD sobre blobs cifrados) ---

    fun insertEntry(label: ByteArray, username: ByteArray?, password: ByteArray): Long {
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put(E_LABEL, label)
            if (username != null) put(E_USERNAME, username) else putNull(E_USERNAME)
            put(E_PASSWORD, password)
            put(E_CREATED, now)
            put(E_UPDATED, now)
        }
        return helper.writableDatabase.insertOrThrow(ENTRIES, null, values)
    }

    /** Atualiza os blobs (já re-cifrados com novo nonce pela camada cripto) e o timestamp. */
    fun updateEntry(id: Long, label: ByteArray, username: ByteArray?, password: ByteArray) {
        val values = ContentValues().apply {
            put(E_LABEL, label)
            if (username != null) put(E_USERNAME, username) else putNull(E_USERNAME)
            put(E_PASSWORD, password)
            put(E_UPDATED, System.currentTimeMillis())
        }
        helper.writableDatabase.update(ENTRIES, values, "$E_ID = ?", arrayOf(id.toString()))
    }

    fun deleteEntry(id: Long) {
        helper.writableDatabase.delete(ENTRIES, "$E_ID = ?", arrayOf(id.toString()))
    }

    fun getEntry(id: Long): EncryptedEntry? =
        helper.readableDatabase.query(ENTRIES, null, "$E_ID = ?", arrayOf(id.toString()), null, null, null)
            .use { c -> if (c.moveToFirst()) c.toEntry() else null }

    fun listEntries(): List<EncryptedEntry> =
        helper.readableDatabase.query(ENTRIES, null, null, null, null, null, "$E_UPDATED DESC").use { c ->
            buildList { while (c.moveToNext()) add(c.toEntry()) }
        }

    fun close() = helper.close()

    private fun android.database.Cursor.toEntry(): EncryptedEntry = EncryptedEntry(
        id = getLong(getColumnIndexOrThrow(E_ID)),
        label = getBlob(getColumnIndexOrThrow(E_LABEL)),
        username = getColumnIndexOrThrow(E_USERNAME).let { if (isNull(it)) null else getBlob(it) },
        password = getBlob(getColumnIndexOrThrow(E_PASSWORD)),
        createdAt = getLong(getColumnIndexOrThrow(E_CREATED)),
        updatedAt = getLong(getColumnIndexOrThrow(E_UPDATED)),
    )

    private companion object {
        const val META = "vault_meta"
        const val META_SCHEME = "scheme_version"
        const val META_ITER = "iterations"
        const val META_SALT = "salt"
        const val META_WRAPPED_DEK = "wrapped_dek"

        const val ENTRIES = "entries"
        const val E_ID = "id"
        const val E_LABEL = "label"
        const val E_USERNAME = "username"
        const val E_PASSWORD = "password"
        const val E_CREATED = "created_at"
        const val E_UPDATED = "updated_at"
    }

    /** Schema do banco. Só blobs cifrados + metadados não sensíveis; nenhum plaintext. */
    private class VaultDbHelper(context: Context) :
        SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE $META (
                    id INTEGER PRIMARY KEY CHECK (id = 1),
                    $META_SCHEME INTEGER NOT NULL,
                    $META_ITER INTEGER NOT NULL,
                    $META_SALT BLOB NOT NULL,
                    $META_WRAPPED_DEK BLOB NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE $ENTRIES (
                    $E_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $E_LABEL BLOB NOT NULL,
                    $E_USERNAME BLOB,
                    $E_PASSWORD BLOB NOT NULL,
                    $E_CREATED INTEGER NOT NULL,
                    $E_UPDATED INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // Sem migrações ainda (DB_VERSION = 1). O versionamento do esquema *cripto* fica em
            // vault_meta.scheme_version, independente da versão do schema SQLite.
        }

        companion object {
            const val DB_NAME = "vault.db"
            const val DB_VERSION = 1
        }
    }
}
