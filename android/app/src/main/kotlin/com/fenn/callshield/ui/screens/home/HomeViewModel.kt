package com.fenn.callshield.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenn.callshield.data.local.dao.ScamDigestDao
import com.fenn.callshield.data.local.entity.CallHistoryEntry
import com.fenn.callshield.data.local.entity.ScamDigestEntry
import com.fenn.callshield.domain.repository.CallHistoryRepository
import com.fenn.callshield.domain.repository.CallStats
import com.fenn.callshield.domain.usecase.MarkNotSpamUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val recentCalls: List<CallHistoryEntry> = emptyList(),
    val stats: CallStats = CallStats(0, 0),
    val isScreeningActive: Boolean = true,
    val scamDigest: ScamDigestEntry? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val callHistoryRepo: CallHistoryRepository,
    private val markNotSpamUseCase: MarkNotSpamUseCase,
    private val scamDigestDao: ScamDigestDao,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        callHistoryRepo.observeRecent(),
        scamDigestDao.observeLatest(),
        callHistoryRepo.observeStats(),
    ) { calls, digest, stats ->
        HomeUiState(recentCalls = calls, stats = stats, scamDigest = digest)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun markNotSpam(numberHash: String, displayLabel: String) {
        viewModelScope.launch {
            markNotSpamUseCase.execute(numberHash, displayLabel)
        }
    }

    fun dismissScamDigest(id: Int) {
        viewModelScope.launch {
            scamDigestDao.dismiss(id)
        }
    }
}
