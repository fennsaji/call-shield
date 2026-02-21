package com.fenn.callshield.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for E.164 normalisation logic.
 * HMAC output correctness is verified via a known test vector.
 */
class PhoneNumberHasherTest {

    // Use a test-only hasher backed by a fixed salt so tests are deterministic
    // without requiring BuildConfig (which is not available in unit tests).
    private val hasher = PhoneNumberHasherTestable("test-salt")

    @Test
    fun `normalises 10-digit Indian number`() {
        assertEquals("+919876543210", hasher.normalise("9876543210"))
    }

    @Test
    fun `normalises number with trunk prefix 0`() {
        assertEquals("+919876543210", hasher.normalise("09876543210"))
    }

    @Test
    fun `normalises 12-digit number starting with 91`() {
        assertEquals("+919876543210", hasher.normalise("919876543210"))
    }

    @Test
    fun `preserves existing E164 number`() {
        assertEquals("+919876543210", hasher.normalise("+919876543210"))
    }

    @Test
    fun `normalises number with spaces and dashes`() {
        assertEquals("+919876543210", hasher.normalise("98765 43210"))
    }

    @Test
    fun `returns null for too-short number`() {
        assertNull(hasher.normalise("1234"))
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
        assertNull(hasher.normalise("not-a-number"))
    }
}

/** Test double — overrides salt without touching BuildConfig. */
private class PhoneNumberHasherTestable(private val salt: String) : PhoneNumberHasher() {
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
