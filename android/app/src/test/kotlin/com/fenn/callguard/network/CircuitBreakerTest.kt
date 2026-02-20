package com.fenn.callguard.network

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CircuitBreakerTest {

    @Test
    fun `starts CLOSED`() {
        val cb = CircuitBreaker()
        assertEquals(CircuitBreaker.State.CLOSED, cb.currentState())
    }

    @Test
    fun `opens after exceeding failure threshold`() = runTest {
        val cb = CircuitBreaker(windowSize = 10, failureThreshold = 0.5, reopenAfterMs = 60_000)
        // 6 failures out of 10 = 60% > 50% threshold
        repeat(4) { cb.execute { "ok" } }
        repeat(6) {
            try { cb.execute { throw RuntimeException("fail") } } catch (_: Exception) {}
        }
        assertEquals(CircuitBreaker.State.OPEN, cb.currentState())
    }

    @Test
    fun `throws CircuitOpenException when OPEN`() = runTest {
        val cb = CircuitBreaker(windowSize = 2, failureThreshold = 0.5, reopenAfterMs = 60_000)
        repeat(2) {
            try { cb.execute { throw RuntimeException("fail") } } catch (_: Exception) {}
        }
        var threw = false
        try { cb.execute { "ok" } } catch (_: CircuitOpenException) { threw = true }
        assert(threw)
    }

    @Test
    fun `transitions to HALF_OPEN after reopen delay`() = runTest {
        val cb = CircuitBreaker(windowSize = 2, failureThreshold = 0.5, reopenAfterMs = 0)
        repeat(2) {
            try { cb.execute { throw RuntimeException("fail") } } catch (_: Exception) {}
        }
        // With reopenAfterMs=0 it should immediately be eligible for HALF_OPEN
        Thread.sleep(1)
        // Next execute should attempt HALF_OPEN probe
        cb.execute { "ok" }
        assertEquals(CircuitBreaker.State.CLOSED, cb.currentState())
    }
}
