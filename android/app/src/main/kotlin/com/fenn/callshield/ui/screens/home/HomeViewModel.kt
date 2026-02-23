package com.fenn.callshield.ui.screens.home

import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenn.callshield.billing.BillingManager
import com.fenn.callshield.data.local.dao.ScamDigestDao
import com.fenn.callshield.data.local.dao.TraiReportDao
import com.fenn.callshield.data.local.entity.CallHistoryEntry
import com.fenn.callshield.data.local.entity.ScamDigestEntry
import com.fenn.callshield.data.local.entity.TraiReportEntry
import com.fenn.callshield.data.preferences.ScreeningPreferences
import com.fenn.callshield.domain.repository.CallHistoryRepository
import com.fenn.callshield.domain.repository.CallStats
import com.fenn.callshield.domain.usecase.MarkNotSpamUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val recentCalls: List<CallHistoryEntry> = emptyList(),
    val stats: CallStats = CallStats(0, 0),
    val isScreeningActive: Boolean = true,
    val scamDigest: ScamDigestEntry? = null,
    val autoBlock: Boolean = false,
    val blockHidden: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val callHistoryRepo: CallHistoryRepository,
    private val markNotSpamUseCase: MarkNotSpamUseCase,
    private val scamDigestDao: ScamDigestDao,
    private val screeningPreferences: ScreeningPreferences,
    private val traiReportDao: TraiReportDao,
    private val billingManager: BillingManager,
) : ViewModel() {

    val isPro: StateFlow<Boolean> = billingManager.isPro

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private val _protectionState = MutableStateFlow(Pair(false, false)) // autoBlock, blockHidden
    private val _isScreeningActive = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            _protectionState.value = Pair(
                screeningPreferences.autoBlockHighConfidence(),
                screeningPreferences.blockHiddenNumbers(),
            )
        }
    }

    /** Call on every screen resume to reflect current Call Screening role status. */
    fun refreshScreeningRole(context: Context) {
        _isScreeningActive.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getSystemService(RoleManager::class.java)
                ?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        } else {
            true
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        callHistoryRepo.observeRecent(),
        scamDigestDao.observeLatest(),
        callHistoryRepo.observeStats(),
        _protectionState,
        _isScreeningActive,
    ) { calls, digest, stats, protection, isActive ->
        HomeUiState(
            recentCalls = calls,
            stats = stats,
            isScreeningActive = isActive,
            scamDigest = digest,
            autoBlock = protection.first,
            blockHidden = protection.second,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun markNotSpam(numberHash: String, displayLabel: String) {
        viewModelScope.launch {
            val result = markNotSpamUseCase.execute(numberHash, displayLabel)
            _snackbarMessage.tryEmit(
                if (result.isSuccess) "Marked as not spam and whitelisted"
                else "Could not submit correction. Try again."
            )
        }
    }

    fun dismissScamDigest(id: Int) {
        viewModelScope.launch {
            scamDigestDao.dismiss(id)
        }
    }

    fun setAutoBlock(v: Boolean) {
        viewModelScope.launch {
            screeningPreferences.setAutoBlockHighConfidence(v)
            _protectionState.value = _protectionState.value.copy(first = v)
        }
    }

    fun setBlockHidden(v: Boolean) {
        viewModelScope.launch {
            screeningPreferences.setBlockHiddenNumbers(v)
            _protectionState.value = _protectionState.value.copy(second = v)
        }
    }

    fun recordTraiReport(numberHash: String, displayLabel: String) {
        viewModelScope.launch {
            traiReportDao.insert(TraiReportEntry(numberHash = numberHash, displayLabel = displayLabel))
            screeningPreferences.incrementTraiReportsCount()
        }
    }
}
