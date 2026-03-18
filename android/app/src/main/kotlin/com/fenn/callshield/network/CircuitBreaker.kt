package com.fenn.callshield.network

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Three-state circuit breaker for the remote reputation API.
 *
 * States:
 *   CLOSED  — normal operation; failures tracked in a sliding window
 *   OPEN    — calls rejected immediately; reopens after [reopenAfterMs]
 *   HALF_OPEN — single probe attempt allowed; success → CLOSED, failure → OPEN
 *
 * Opens when the failure rate exceeds [failureThreshold] in the last [windowSize] calls.
 *
 * HALF_OPEN race protection: only one probe is allowed in-flight at a time. Concurrent
 * callers that arrive while a probe is already in progress receive [CircuitOpenException]
 * immediately rather than racing through as additional probes.
 */
class CircuitBreaker(
    private val windowSize: Int = 10,
    private val failureThreshold: Double = 0.5,
    private val reopenAfterMs: Long = 60_000L,
) {
    enum class State { CLOSED, OPEN, HALF_OPEN }

    private val mutex = Mutex()
    private val outcomes = ArrayDeque<Boolean>() // true = success, false = failure
    private var state = State.CLOSED
    private var openedAt: Long = 0L

    /** True while a HALF_OPEN probe is executing. Prevents concurrent probes. */
    private val probeInFlight = AtomicBoolean(false)

    suspend fun <T> execute(block: suspend () -> T): T {
        val current = mutex.withLock { checkState() }
        return when (current) {
            State.OPEN -> throw CircuitOpenException()
            State.HALF_OPEN -> {
                // Only one probe may be in-flight at a time; reject all others immediately.
                if (!probeInFlight.compareAndSet(false, true)) {
                    throw CircuitOpenException()
                }
                try {
                    val result = block()
                    mutex.withLock { recordOutcome(success = true) }
                    result
                } catch (e: Exception) {
                    mutex.withLock { recordOutcome(success = false) }
                    throw e
                } finally {
                    probeInFlight.set(false)
                }
            }
            State.CLOSED -> {
                try {
                    val result = block()
                    mutex.withLock { recordOutcome(success = true) }
                    result
                } catch (e: Exception) {
                    mutex.withLock { recordOutcome(success = false) }
                    throw e
                }
            }
        }
    }

    private fun checkState(): State {
        if (state == State.OPEN) {
            val elapsed = System.currentTimeMillis() - openedAt
            if (elapsed >= reopenAfterMs) {
                state = State.HALF_OPEN
            }
        }
        return state
    }

    private fun recordOutcome(success: Boolean) {
        outcomes.addLast(success)
        if (outcomes.size > windowSize) outcomes.removeFirst()

        when (state) {
            State.HALF_OPEN -> {
                state = if (success) State.CLOSED else {
                    openedAt = System.currentTimeMillis()
                    State.OPEN
                }
                if (success) outcomes.clear()
            }
            State.CLOSED -> {
                if (outcomes.size >= windowSize) {
                    val failureRate = outcomes.count { !it }.toDouble() / outcomes.size
                    if (failureRate > failureThreshold) {
                        state = State.OPEN
                        openedAt = System.currentTimeMillis()
                    }
                }
            }
            State.OPEN -> Unit
        }
    }

    /** Visible for testing. */
    fun currentState(): State = state
}

class CircuitOpenException : Exception("Circuit breaker is OPEN — remote calls suppressed")
