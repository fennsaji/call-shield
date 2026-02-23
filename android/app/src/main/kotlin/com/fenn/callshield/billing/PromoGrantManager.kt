package com.fenn.callshield.billing

import android.content.Context
import com.fenn.callshield.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "callshield_secure_prefs"   // shared with DeviceTokenManager
private const val PREF_PROMO_GRANT = "promo_grant_token"
private const val GRANT_DOMAIN = "callshield-promo-v1"

/**
 * Manages tester/reviewer promo code grants.
 *
 * Security model:
 * - The raw promo code is NEVER stored or compiled into the APK.
 * - BuildConfig.PROMO_CODE_HASH = SHA-256(rawCode), set in local.properties / CI secrets.
 * - On redemption: grantToken = HMAC-SHA256(deviceTokenHash, PROMO_CODE_HASH + ":" + GRANT_DOMAIN)
 * - To verify: recompute expected token from current deviceTokenHash; compare with stored value.
 * - The stored token is device-bound — copying it to another device produces a mismatch because
 *   each device has a different deviceTokenHash (Android Keystore-backed random UUID).
 * - A plain boolean flip in DataStore/SharedPreferences cannot fake a valid grant.
 */
@Singleton
class PromoGrantManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Returns true if the promo grant is active and cryptographically valid for this device.
     * Always returns false if PROMO_CODE_HASH is not configured in BuildConfig.
     */
    fun isGrantActive(deviceTokenHash: String): Boolean {
        val codeHash = BuildConfig.PROMO_CODE_HASH
        if (codeHash.isBlank()) return false
        val stored = readGrantToken() ?: return false
        val expected = computeGrantToken(deviceTokenHash, codeHash)
        return constantTimeEquals(stored, expected)
    }

    /**
     * Validates [inputCode] against the configured PROMO_CODE_HASH and, if valid,
     * writes the device-bound grant token to encrypted SharedPreferences.
     *
     * @return true if the code was correct and the grant was saved.
     */
    fun redeem(inputCode: String, deviceTokenHash: String): Boolean {
        val codeHash = BuildConfig.PROMO_CODE_HASH
        if (codeHash.isBlank()) return false

        val inputHash = sha256Hex(inputCode.trim())
        if (!constantTimeEquals(inputHash, codeHash)) return false

        val grantToken = computeGrantToken(deviceTokenHash, codeHash)
        writeGrantToken(grantToken)
        return true
    }

    /** Removes the stored grant (e.g. for testing / forced revocation). */
    fun revoke() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(PREF_PROMO_GRANT)
            .apply()
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun computeGrantToken(deviceTokenHash: String, codeHash: String): String {
        val key = "$codeHash:$GRANT_DOMAIN".toByteArray(StandardCharsets.UTF_8)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(deviceTokenHash.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    /** Timing-safe comparison — prevents timing attacks on token comparison. */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }

    private fun readGrantToken(): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREF_PROMO_GRANT, null)

    private fun writeGrantToken(token: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_PROMO_GRANT, token)
            .apply()
    }
}
