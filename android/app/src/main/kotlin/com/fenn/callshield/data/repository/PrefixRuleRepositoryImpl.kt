package com.fenn.callshield.data.repository

import com.fenn.callshield.data.local.dao.PrefixRuleDao
import com.fenn.callshield.data.local.entity.PrefixRule
import com.fenn.callshield.domain.repository.PrefixRuleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PrefixRuleRepositoryImpl @Inject constructor(
    private val dao: PrefixRuleDao,
) : PrefixRuleRepository {
    override fun observeAll(): Flow<List<PrefixRule>> = dao.observeAll()

    override suspend fun findMatch(e164Number: String): PrefixRule? {
        // Rules sorted longest-pattern-first — most specific match wins
        return dao.getAllSortedByLength().firstOrNull { rule ->
            when (rule.matchType) {
                "suffix"   -> e164Number.endsWith(rule.pattern)
                "contains" -> e164Number.contains(rule.pattern)
                else       -> e164Number.startsWith(rule.pattern) // "prefix" default
            }
        }
    }

    override suspend fun add(pattern: String, matchType: String, action: String, label: String) =
        dao.insert(PrefixRule(pattern = pattern, matchType = matchType, action = action, label = label))

    override suspend fun remove(id: Int) = dao.deleteById(id)
}
