package com.fenn.callguard.ui.screens.prefix

import androidx.lifecycle.ViewModel
import com.fenn.callguard.data.local.entity.PrefixRule
import com.fenn.callguard.domain.repository.PrefixRuleRepository
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
