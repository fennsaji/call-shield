package com.fenn.callguard.data.repository

import com.fenn.callguard.data.local.dao.PrefixRuleDao
import com.fenn.callguard.data.local.entity.PrefixRule
import com.fenn.callguard.domain.repository.PrefixRuleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PrefixRuleRepositoryImpl @Inject constructor(
    private val dao: PrefixRuleDao,
) : PrefixRuleRepository {
    override fun observeAll(): Flow<List<PrefixRule>> = dao.observeAll()

    override suspend fun findMatch(e164Number: String): PrefixRule? {
        // Rules sorted longest-prefix-first — first match wins
        return dao.getAllSortedByLength().firstOrNull { e164Number.startsWith(it.prefix) }
    }

    override suspend fun add(prefix: String, action: String, label: String) =
        dao.insert(PrefixRule(prefix = prefix, action = action, label = label))

    override suspend fun remove(prefix: String) = dao.deleteByPrefix(prefix)
}
