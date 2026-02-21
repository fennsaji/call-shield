package com.fenn.callshield.domain.repository

import com.fenn.callshield.data.local.entity.PrefixRule
import kotlinx.coroutines.flow.Flow

interface PrefixRuleRepository {
    fun observeAll(): Flow<List<PrefixRule>>

    /**
     * Returns the first matching rule for [e164Number] using longest-prefix-first matching,
     * or null if no rule matches.
     */
    suspend fun findMatch(e164Number: String): PrefixRule?

    suspend fun add(prefix: String, action: String, label: String)
    suspend fun remove(prefix: String)
}
