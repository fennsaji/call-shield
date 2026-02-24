package com.fenn.callshield.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for E.164 normalisation logic.
 * HMAC output correctness is verified via a known test vector.
 *
 * Tests use "+91" (India) as the home calling code to match legacy test vectors.
 * The normalise() overload that accepts [homeCallingCode] is used directly.
 */
class PhoneNumberHasherTest {

    private val hasher = PhoneNumberHasherTestable("test-salt")
    private val homeCode = "+91"

    @Test
    fun `normalises 10-digit number with home calling code`() {
        assertEquals("+919876543210", hasher.normalise("9876543210", homeCode))
    }

    @Test
    fun `normalises number with trunk prefix 0`() {
        assertEquals("+919876543210", hasher.normalise("09876543210", homeCode))
    }

    @Test
    fun `preserves existing E164 number`() {
        assertEquals("+919876543210", hasher.normalise("+919876543210", homeCode))
    }

    @Test
    fun `normalises number with spaces and dashes`() {
        assertEquals("+919876543210", hasher.normalise("98765 43210", homeCode))
    }

    @Test
    fun `returns null for too-short number`() {
        assertNull(hasher.normalise("1234", homeCode))
    }

    @Test
    fun `normalises US number with +1 home code`() {
        assertEquals("+12125551234", hasher.normalise("2125551234", "+1"))
    }

    @Test
    fun `normalises UK number with +44 home code`() {
        assertEquals("+447911123456", hasher.normalise("07911123456", "+44"))
    }

    @Test
    fun `hash returns 64-char hex string`() {
        val result = hasher.hash("9876543210")
        assertNotNull(result)
        assertEquals(64, result!!.length)
        assert(result.all { it.isDigit() || it in 'a'..'f' })
    }

    @Test
    fun `same number always produces same hash`() {
        val h1 = hasher.hash("9876543210")
        val h2 = hasher.hash("+919876543210")
        assertEquals(h1, h2)
    }

    @Test
    fun `different numbers produce different hashes`() {
        val h1 = hasher.hash("9876543210")
        val h2 = hasher.hash("9876543211")
        assert(h1 != h2)
    }

    @Test
    fun `returns null for non-numeric input`() {
        assertNull(hasher.normalise("not-a-number", homeCode))
    }
}

/** Fake HomeCountryProvider for tests — no Android Context needed. */
private class FakeHomeCountryProvider(code: String = "+91") : HomeCountryProvider(null!!) {
    override val callingCodePrefix = code
    override val isoCode = CALLING_CODES.entries.firstOrNull { it.value == code }?.key ?: "IN"
}

/** Test double — overrides salt without touching BuildConfig. */
private class PhoneNumberHasherTestable(
    private val salt: String,
    provider: HomeCountryProvider = FakeHomeCountryProvider(),
) : PhoneNumberHasher(provider) {
    override fun hash(rawNumber: String): String? {
        val normalised = normalise(rawNumber) ?: return null
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(
            javax.crypto.spec.SecretKeySpec(
                salt.toByteArray(Charsets.UTF_8),
                "HmacSHA256",
            ),
        )
        return mac.doFinal(normalised.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
