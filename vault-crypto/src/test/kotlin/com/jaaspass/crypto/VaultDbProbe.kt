package com.jaaspass.crypto

/**
 * Validação aproximada das tarefas 6.2/6.3 (sem rodar o app no aparelho).
 *
 * Usa o CryptoManager + VaultSession REAIS para cifrar uma entrada com plaintext conhecido e emite,
 * no stdout, um script SQL com o MESMO esquema do `VaultRepository` (app) e os blobs como literais
 * hex `X'..'`. Um script externo monta o `.db` com o `sqlite3` (platform-tools) e verifica que
 * nenhum plaintext aparece no arquivo. No stderr emite um relatório (inclui a checagem de que a
 * senha errada NÃO decifra — 6.3).
 *
 * Rodar:  ./gradlew :vault-crypto:vaultDbProbe -q
 */
private class MemStore : VaultMetaStore {
    private var meta: VaultMeta? = null
    override fun loadMeta(): VaultMeta? = meta
    override fun saveMeta(meta: VaultMeta) { this.meta = meta }
    override fun updateWrappedDek(wrappedDek: ByteArray) {
        meta = (meta ?: error("sem meta")).copy(wrappedDek = wrappedDek)
    }
}

private fun hex(b: ByteArray): String = b.joinToString("") { "%02x".format(it) }

// Plaintext conhecido — devem estar AUSENTES do .db (mesmas strings checadas pelo script shell).
private const val MASTER = "master-correct-horse-battery"
private const val LABEL = "GitHub"
private const val USERNAME = "addisonsouza"
private const val PASSWORD = "S3nh@-SECRETA-naodevevazar"

fun main() {
    val crypto = CryptoManager()
    val store = MemStore()
    val session = VaultSession(store, crypto, iterations = 10_000)
    session.setup(MASTER.toCharArray())
    val dek = session.requireDek()

    val lblob = crypto.encryptField(LABEL.toByteArray(), dek)
    val ublob = crypto.encryptField(USERNAME.toByteArray(), dek)
    val pblob = crypto.encryptField(PASSWORD.toByteArray(), dek)
    val meta = store.loadMeta()!!

    // --- stdout: SQL com o mesmo esquema do VaultRepository ---
    val sql = buildString {
        appendLine("CREATE TABLE vault_meta (id INTEGER PRIMARY KEY CHECK (id = 1), scheme_version INTEGER NOT NULL, iterations INTEGER NOT NULL, salt BLOB NOT NULL, wrapped_dek BLOB NOT NULL);")
        appendLine("CREATE TABLE entries (id INTEGER PRIMARY KEY AUTOINCREMENT, label BLOB NOT NULL, username BLOB, password BLOB NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL);")
        appendLine("INSERT INTO vault_meta VALUES (1, ${meta.schemeVersion}, ${meta.iterations}, X'${hex(meta.salt)}', X'${hex(meta.wrappedDek)}');")
        appendLine("INSERT INTO entries (label, username, password, created_at, updated_at) VALUES (X'${hex(lblob)}', X'${hex(ublob)}', X'${hex(pblob)}', 0, 0);")
    }
    print(sql)

    // --- stderr: relatório / checagem 6.3 (senha errada não decifra) ---
    val fresh = VaultSession(store, crypto, iterations = 10_000)
    val wrong = fresh.unlock("senha-errada".toCharArray())
    val right = fresh.unlock(MASTER.toCharArray())
    System.err.println("[probe] plaintext usado (deve estar AUSENTE do .db): LABEL=$LABEL USERNAME=$USERNAME PASSWORD=$PASSWORD")
    System.err.println("[probe] 6.3 unlock(senha-errada)=$wrong  unlock(correta)=$right")
}
