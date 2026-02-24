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
open class PhoneNumberHasher @Inject constructor(
    private val homeCountryProvider: HomeCountryProvider,
) {

    private val saltBytes: ByteArray =
        BuildConfig.HMAC_SALT.toByteArray(StandardCharsets.UTF_8)

    /**
     * Normalises [rawNumber] to E.164 and returns HMAC-SHA256(normalised, salt).
     * Uses the device's SIM country calling code as the default when no country code is present.
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
     * - If the number already starts with '+', uses it as-is.
     * - If starts with '0' (local trunk prefix), strips it and prepends [homeCallingCode].
     * - If 7–11 digits with no country code, prepends [homeCallingCode].
     * - [homeCallingCode] defaults to the device's SIM country (e.g. "+91" in India, "+1" in US).
     * Returns null if the result is not 8–15 digits (ITU-T range).
     */
    fun normalise(
        rawNumber: String,
        homeCallingCode: String = homeCountryProvider.callingCodePrefix,
    ): String? {
        val stripped = rawNumber.trim()
        val digits = stripped.replace(Regex("[^\\d+]"), "")

        val e164 = when {
            digits.startsWith("+") -> digits
            digits.startsWith("0") -> "$homeCallingCode${digits.substring(1)}"
            digits.length in 7..11 -> "$homeCallingCode$digits"
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
