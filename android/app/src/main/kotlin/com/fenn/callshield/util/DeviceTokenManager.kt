package com.fenn.callshield.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.fenn.callshield.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
private const val KEY_ALIAS = "callshield_device_token_key"
private const val PREFS_NAME = "callshield_secure_prefs"
private const val PREF_ENCRYPTED_TOKEN = "device_token_enc"
private const val PREF_TOKEN_IV = "device_token_iv"
private const val GCM_TAG_LENGTH = 128

/**
 * Manages a persistent device token used to deduplicate reports on the backend.
 *
 * The token is a random UUID generated on first launch and stored encrypted
 * in SharedPreferences using an AES-256-GCM key in the Android Keystore.
 *
 * Lifecycle:
 * - Survives app updates (Keystore key tied to app UID, not version).
 * - Does NOT survive full uninstall (Keystore entry is deleted with app UID).
 * - Only the HMAC-SHA256 hash of this token is ever sent to the backend.
 */
@Singleton
class DeviceTokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hasher: PhoneNumberHasher,
) {

    // Lazily initialised — safe to call from any thread after first access
    val deviceTokenHash: String by lazy { getOrCreateTokenHash() }

    private fun getOrCreateTokenHash(): String {
        val token = getStoredToken() ?: createAndStoreToken()
        return hashToken(token)
    }

    private fun getStoredToken(): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encBase64 = prefs.getString(PREF_ENCRYPTED_TOKEN, null) ?: return null
        val ivBase64 = prefs.getString(PREF_TOKEN_IV, null) ?: return null

        return try {
            val key = getOrCreateKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = android.util.Base64.decode(ivBase64, android.util.Base64.NO_WRAP)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val enc = android.util.Base64.decode(encBase64, android.util.Base64.NO_WRAP)
            String(cipher.doFinal(enc), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            // Key or ciphertext corrupted (e.g. after a factory reset mid-flight) — regenerate
            null
        }
    }

    private fun createAndStoreToken(): String {
        val token = UUID.randomUUID().toString()
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val enc = cipher.doFinal(token.toByteArray(StandardCharsets.UTF_8))

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(PREF_ENCRYPTED_TOKEN, android.util.Base64.encodeToString(enc, android.util.Base64.NO_WRAP))
            .putString(PREF_TOKEN_IV, android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP))
            .apply()

        return token
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        }

        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        keyGen.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return keyGen.generateKey()
    }

    private fun hashToken(token: String): String {
        // Reuse HMAC infrastructure; token is already random so salt just keeps it consistent
        val salt = BuildConfig.HMAC_SALT.toByteArray(StandardCharsets.UTF_8)
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(javax.crypto.spec.SecretKeySpec(salt, "HmacSHA256"))
        return mac.doFinal(token.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
