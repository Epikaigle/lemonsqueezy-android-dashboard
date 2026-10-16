package com.epikaigle.lemondashboard

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypted values never leave app-private storage. No Android backup is permitted.
 * An Android Keystore AES/GCM key encrypts the API token and offline snapshot.
 */
class SecureStore(context: Context) {
    private val prefs = context.getSharedPreferences("lemon_private", Context.MODE_PRIVATE)
    private val alias = "lemon-dashboard-data-v1"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = ks.getKey(alias, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private fun encrypt(clear: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val bytes = cipher.iv + cipher.doFinal(clear.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        require(bytes.size > 28) { "Cache chiffré invalide" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        return cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8)
    }

    fun readToken(): String? = prefs.getString("token", null)?.let { runCatching { decrypt(it) }.getOrNull() }
    fun saveToken(value: String) { check(prefs.edit().putString("token", encrypt(value)).commit()) }
    fun readSnapshot(): String? = prefs.getString("snapshot", null)?.let { runCatching { decrypt(it) }.getOrNull() }
    fun saveSnapshot(value: String) { check(prefs.edit().putString("snapshot", encrypt(value)).commit()) }
    fun storeId(): String = prefs.getString("store_id", "") ?: ""
    fun saveStoreId(value: String) { prefs.edit().putString("store_id", value).apply() }
    fun showTests(): Boolean = prefs.getBoolean("show_test", false)
    fun saveShowTests(value: Boolean) { prefs.edit().putBoolean("show_test", value).apply() }
    fun currency(): String = prefs.getString("currency", "USD") ?: "USD"
    fun saveCurrency(value: String) { prefs.edit().putString("currency", value).apply() }

    fun clear() {
        check(prefs.edit().clear().commit())
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(alias) }
    }
}
