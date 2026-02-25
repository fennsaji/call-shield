package com.fenn.callshield.ui.screens.prefix

import androidx.lifecycle.ViewModel
import com.fenn.callshield.billing.BillingManager
import com.fenn.callshield.data.local.entity.PrefixRule
import com.fenn.callshield.domain.repository.PrefixRuleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Free tier: maximum 5 pattern rules. Pro: unlimited. */
const val FREE_PREFIX_RULE_LIMIT = 5

@HiltViewModel
class PrefixRulesViewModel @Inject constructor(
    private val repo: PrefixRuleRepository,
    private val billingManager: BillingManager,
) : ViewModel() {

    val rules: Flow<List<PrefixRule>> = repo.observeAll()
    val isPro: StateFlow<Boolean> = billingManager.isPro

    suspend fun add(pattern: String, matchType: String, action: String, label: String) =
        repo.add(pattern, matchType, action, label)

    suspend fun remove(id: Int) = repo.remove(id)
}
