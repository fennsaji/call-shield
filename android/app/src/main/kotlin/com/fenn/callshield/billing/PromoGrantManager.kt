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

private const val PREFS_NAME              = "callshield_secure_prefs"   // shared with DeviceTokenManager
private const val PREF_PROMO_GRANT_PRO    = "promo_grant_pro_token"
private const val PREF_PROMO_GRANT_FAMILY = "promo_grant_family_token"
private const val GRANT_DOMAIN_PRO        = "callshield-promo-pro-v1"
private const val GRANT_DOMAIN_FAMILY     = "callshield-promo-family-v1"

/** The plan tier unlocked by a promo grant. */
enum class PromoGrant { NONE, PRO, FAMILY }

/**
 * Manages tester/reviewer promo code grants.
 *
 * Security model:
 * - Raw promo codes are NEVER stored or compiled into the APK.
 * - BuildConfig.PROMO_CODE_PRO_HASH / PROMO_CODE_FAMILY_HASH = SHA-256(rawCode),
 *   set in local.properties / CI secrets.
 * - Each grant has a hard expiry stored in BuildConfig (epoch-ms); expired codes
 *   are rejected at both redemption time and every subsequent check.
 * - On redemption: grantToken = HMAC-SHA256(deviceTokenHash, codeHash + ":" + domain)
 * - The stored token is device-bound — copying it to another device produces a
 *   mismatch because each device has a different deviceTokenHash (Keystore-backed UUID).
 */
@Singleton
class PromoGrantManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Returns the highest active promo grant for this device, respecting expiry.
     * Family > Pro. Returns [PromoGrant.NONE] if no valid grant is stored.
     */
    fun activeGrant(deviceTokenHash: String): PromoGrant {
        if (checkSlot(deviceTokenHash, BuildConfig.PROMO_CODE_FAMILY_HASH, PREF_PROMO_GRANT_FAMILY, GRANT_DOMAIN_FAMILY, BuildConfig.PROMO_CODE_FAMILY_EXPIRY))
            return PromoGrant.FAMILY
        if (checkSlot(deviceTokenHash, BuildConfig.PROMO_CODE_PRO_HASH, PREF_PROMO_GRANT_PRO, GRANT_DOMAIN_PRO, BuildConfig.PROMO_CODE_PRO_EXPIRY))
            return PromoGrant.PRO
        return PromoGrant.NONE
    }

    /**
     * Validates [inputCode] against both configured hashes in order (Family first, then Pro).
     * On a match that is not yet expired, writes a device-bound grant token and returns
     * the matching [PromoGrant]. Returns [PromoGrant.NONE] if no code matched or the
     * matching code's expiry has passed.
     */
    fun redeem(inputCode: String, deviceTokenHash: String): PromoGrant {
        val inputHash = sha256Hex(inputCode.trim())

        val familyHash = BuildConfig.PROMO_CODE_FAMILY_HASH
        if (familyHash.isNotBlank() && constantTimeEquals(inputHash, familyHash)) {
            if (System.currentTimeMillis() > BuildConfig.PROMO_CODE_FAMILY_EXPIRY) return PromoGrant.NONE
            writeGrantToken(computeGrantToken(deviceTokenHash, familyHash, GRANT_DOMAIN_FAMILY), PREF_PROMO_GRANT_FAMILY)
            return PromoGrant.FAMILY
        }

        val proHash = BuildConfig.PROMO_CODE_PRO_HASH
        if (proHash.isNotBlank() && constantTimeEquals(inputHash, proHash)) {
            if (System.currentTimeMillis() > BuildConfig.PROMO_CODE_PRO_EXPIRY) return PromoGrant.NONE
            writeGrantToken(computeGrantToken(deviceTokenHash, proHash, GRANT_DOMAIN_PRO), PREF_PROMO_GRANT_PRO)
            return PromoGrant.PRO
        }

        return PromoGrant.NONE
    }

    /** Removes all stored grants (e.g. for testing / forced revocation). */
    fun revokeAll() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(PREF_PROMO_GRANT_PRO)
            .remove(PREF_PROMO_GRANT_FAMILY)
            .apply()
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun checkSlot(
        deviceTokenHash: String,
        codeHash: String,
        prefKey: String,
        domain: String,
        expiryEpochMs: Long,
    ): Boolean {
        if (codeHash.isBlank()) return false
        if (System.currentTimeMillis() > expiryEpochMs) return false
        val stored = readGrantToken(prefKey) ?: return false
        return constantTimeEquals(stored, computeGrantToken(deviceTokenHash, codeHash, domain))
    }

    private fun computeGrantToken(deviceTokenHash: String, codeHash: String, domain: String): String {
        val key = "$codeHash:$domain".toByteArray(StandardCharsets.UTF_8)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(deviceTokenHash.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    }

    /** Timing-safe comparison — prevents timing attacks on token comparison. */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }

    private fun readGrantToken(key: String): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(key, null)

    private fun writeGrantToken(token: String, key: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(key, token).apply()
    }
}
