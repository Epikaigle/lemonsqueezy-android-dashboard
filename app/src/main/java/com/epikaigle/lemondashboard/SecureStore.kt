package com.epikaigle.lemondashboard

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.KeyStore
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * API token: AES-256-GCM in private preferences.
 * Large history: compressed then AES-256-GCM into an atomic private no-backup file.
 * Key alias is deliberately unchanged so installed v1.1 users retain their token/cache.
 */
class SecureStore(context: Context) {
    private val prefs = context.getSharedPreferences("lemon_private", Context.MODE_PRIVATE)
    private val snapshotFile = AtomicFile(File(context.noBackupFilesDir, "lemon_snapshot_v2.bin"))
    private val alias = "lemon-dashboard-data-v1"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build())
            generateKey()
        }
    }

    private fun encrypt(clear: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return cipher.iv + cipher.doFinal(clear)
    }

    private fun decrypt(blob: ByteArray): ByteArray {
        require(blob.size >= 29) { "Données chiffrées invalides" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, blob.copyOfRange(0, 12)))
        return cipher.doFinal(blob, 12, blob.size - 12)
    }

    fun readToken(): String? = prefs.getString("token", null)?.let { saved ->
        runCatching {
            decrypt(Base64.decode(saved, Base64.NO_WRAP)).toString(Charsets.UTF_8)
        }.getOrNull()
    }

    fun saveToken(value: String) {
        val encrypted = Base64.encodeToString(encrypt(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        check(prefs.edit().putString("token", encrypted).commit()) { "Impossible de conserver le jeton" }
    }

    fun readSnapshot(): String? {
        if (snapshotFile.baseFile.exists()) {
            // If an encrypted cache cannot be decrypted, fail closed; never return untrusted bytes.
            return runCatching {
                val bytes = snapshotFile.readFully()
                require(bytes.size <= 80 * 1024 * 1024) { "Cache trop volumineux" }
                val decrypted = decrypt(bytes)
                GZIPInputStream(ByteArrayInputStream(decrypted)).bufferedReader(Charsets.UTF_8).use {
                    it.readText()
                }
            }.getOrNull()
        }
        // Backward-compatible read of v1.1 cache. The old key stays usable.
        return prefs.getString("snapshot", null)?.let { saved ->
            runCatching {
                decrypt(Base64.decode(saved, Base64.NO_WRAP)).toString(Charsets.UTF_8)
            }.getOrNull()
        }
    }

    fun saveSnapshot(value: String) {
        val compressed = ByteArrayOutputStream().use { out ->
            GZIPOutputStream(out).use { zip -> zip.write(value.toByteArray(Charsets.UTF_8)) }
            out.toByteArray()
        }
        val encrypted = encrypt(compressed)
        var stream: java.io.FileOutputStream? = null
        try {
            stream = snapshotFile.startWrite()
            stream.write(encrypted)
            snapshotFile.finishWrite(stream)
        } catch (error: Exception) {
            if (stream != null) snapshotFile.failWrite(stream)
            throw error
        }
        // Remove old base64 history only after the new atomic file is fully durable.
        prefs.edit().remove("snapshot").apply()
    }

    fun storeId(): String = prefs.getString("store_id", "") ?: ""
    fun saveStoreId(value: String) { prefs.edit().putString("store_id", value).apply() }
    fun showTests(): Boolean = prefs.getBoolean("show_test", false)
    fun saveShowTests(value: Boolean) { prefs.edit().putBoolean("show_test", value).apply() }
    fun currency(): String = prefs.getString("currency", "USD") ?: "USD"
    fun saveCurrency(value: String) { prefs.edit().putString("currency", value).apply() }

    fun savePushConfig(config: PushConfig) {
        val secret = Base64.encodeToString(encrypt(config.secret.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        check(prefs.edit().putString("push_origin", config.origin)
            .putString("push_secret", secret).commit()) { "Impossible de conserver la configuration des alertes" }
    }

    fun readPushConfig(): PushConfig? {
        val origin = prefs.getString("push_origin", null) ?: return null
        val encrypted = prefs.getString("push_secret", null) ?: return null
        return runCatching {
            PushConfig(origin, decrypt(Base64.decode(encrypted, Base64.NO_WRAP)).toString(Charsets.UTF_8))
        }.getOrNull()
    }

    fun clearPushConfig() {
        check(prefs.edit().remove("push_origin").remove("push_secret").commit())
    }

    fun clear() {
        snapshotFile.delete()
        check(prefs.edit().clear().commit())
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(alias) }
    }
}
