package com.fenn.callshield.ui.screens.prefix

import androidx.lifecycle.ViewModel
import com.fenn.callshield.data.local.entity.PrefixRule
import com.fenn.callshield.domain.repository.PrefixRuleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class PrefixRulesViewModel @Inject constructor(
    private val repo: PrefixRuleRepository,
) : ViewModel() {

    val rules: Flow<List<PrefixRule>> = repo.observeAll()

    suspend fun add(prefix: String, action: String, label: String) =
        repo.add(prefix, action, label)

    suspend fun remove(prefix: String) = repo.remove(prefix)
}
