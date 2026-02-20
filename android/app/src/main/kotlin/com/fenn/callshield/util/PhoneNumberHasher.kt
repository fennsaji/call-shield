package com.fenn.callshield.util

import com.fenn.callshield.BuildConfig
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hashes phone numbers using HMAC-SHA256 with a static app-bundled salt.
 *
 * The salt is NOT a secret — it's compiled into the APK. Its purpose is to
 * prevent trivial rainbow-table attacks against the backend. Numbers are
 * normalised to E.164 before hashing.
 *
 * Output: lowercase hex string (64 chars).
 */
@Singleton
class PhoneNumberHasher @Inject constructor() {

    private val saltBytes: ByteArray =
        BuildConfig.HMAC_SALT.toByteArray(StandardCharsets.UTF_8)

    /**
     * Normalises [rawNumber] to E.164 (Indian context: prepend +91 if no country code)
     * and returns HMAC-SHA256(normalised, salt) as a lowercase hex string.
     *
     * Returns null if the number cannot be normalised to a plausible E.164 form.
     */
    open fun hash(rawNumber: String): String? {
        val normalised = normalise(rawNumber) ?: return null
        return hmacSha256Hex(normalised, saltBytes)
    }

    /**
     * Normalises a phone number to E.164.
     * - Strips non-digit characters except leading '+'.
     * - If 10 digits and no country code, assumes India (+91).
     * - If starts with 0 (local trunk), strips leading 0 and prepends +91.
     * Returns null if the result is not 8–15 digits (ITU-T range).
     */
    fun normalise(rawNumber: String): String? {
        val stripped = rawNumber.trim()
        val digits = stripped.replace(Regex("[^\\d+]"), "")

        val e164 = when {
            digits.startsWith("+") -> digits
            digits.startsWith("91") && digits.length == 12 -> "+$digits"
            digits.startsWith("0") && digits.length == 11 -> "+91${digits.substring(1)}"
            digits.length == 10 -> "+91$digits"
            else -> "+$digits"
        }

        val digitCount = e164.removePrefix("+").length
        return if (digitCount in 8..15) e164 else null
    }

    private fun hmacSha256Hex(data: String, key: ByteArray): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        val raw = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return raw.joinToString("") { "%02x".format(it) }
    }
}
