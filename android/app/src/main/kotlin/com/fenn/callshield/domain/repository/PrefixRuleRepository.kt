package com.fenn.callshield.domain.repository

import com.fenn.callshield.data.local.entity.PrefixRule
import kotlinx.coroutines.flow.Flow

interface PrefixRuleRepository {
    fun observeAll(): Flow<List<PrefixRule>>

    /**
     * Returns the first matching rule for [e164Number] using longest-pattern-first matching.
     * Match type determines how the pattern is applied:
     *   "prefix"   → startsWith
     *   "suffix"   → endsWith
     *   "contains" → contains
     */
    suspend fun findMatch(e164Number: String): PrefixRule?

    suspend fun add(pattern: String, matchType: String, action: String, label: String)
    suspend fun remove(id: Int)
}
