package com.fenn.callshield.ui.screens.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenn.callshield.BuildConfig
import com.fenn.callshield.data.local.dao.TraiReportDao
import com.fenn.callshield.data.local.entity.TraiReportEntry
import com.fenn.callshield.data.preferences.ScreeningPreferences
import com.fenn.callshield.domain.repository.ReputationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportSpamState(
    val loading: Boolean = false,
    val submitted: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ReportSpamViewModel @Inject constructor(
    private val reputationRepo: ReputationRepository,
    private val traiReportDao: TraiReportDao,
    private val screeningPreferences: ScreeningPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(ReportSpamState())
    val state: StateFlow<ReportSpamState> = _state.asStateFlow()

    fun saveTraiReport(numberHash: String, displayLabel: String) {
        viewModelScope.launch {
            traiReportDao.insert(TraiReportEntry(numberHash = numberHash, displayLabel = displayLabel))
            screeningPreferences.incrementTraiReportsCount()
        }
    }

    fun submitReport(numberHash: String, category: String) {
        viewModelScope.launch {
            _state.value = ReportSpamState(loading = true)
            val result = reputationRepo.submitReport(numberHash, category)
            _state.value = if (result.isSuccess) {
                ReportSpamState(submitted = true)
            } else {
                val msg = if (BuildConfig.DEBUG)
                    result.exceptionOrNull()?.message ?: "Unknown error"
                else
                    "Could not submit report. Try again."
                ReportSpamState(error = msg)
            }
        }
    }
}
