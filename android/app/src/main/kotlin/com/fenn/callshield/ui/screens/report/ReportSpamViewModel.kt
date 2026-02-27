package com.fenn.callshield.ui.screens.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenn.callshield.BuildConfig
import com.fenn.callshield.data.local.dao.TraiReportDao
import com.fenn.callshield.data.local.entity.TraiReportEntry
import com.fenn.callshield.data.preferences.ScreeningPreferences
import com.fenn.callshield.domain.repository.ReputationRepository
import com.fenn.callshield.util.HomeCountryProvider
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
    val isIndiaDevice: Boolean = true,
)

@HiltViewModel
class ReportSpamViewModel @Inject constructor(
    private val reputationRepo: ReputationRepository,
    private val traiReportDao: TraiReportDao,
    private val screeningPreferences: ScreeningPreferences,
    private val homeCountryProvider: HomeCountryProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(ReportSpamState(isIndiaDevice = homeCountryProvider.isoCode == "IN"))
    val state: StateFlow<ReportSpamState> = _state.asStateFlow()

    fun saveTraiReport(numberHash: String, displayLabel: String) {
        viewModelScope.launch {
            traiReportDao.insert(TraiReportEntry(numberHash = numberHash, displayLabel = displayLabel))
            screeningPreferences.incrementTraiReportsCount()
        }
    }

    fun submitReport(numberHash: String, category: String) {
        viewModelScope.launch {
            val isIndia = _state.value.isIndiaDevice
            _state.value = ReportSpamState(loading = true, isIndiaDevice = isIndia)
            val result = reputationRepo.submitReport(numberHash, category)
            _state.value = if (result.isSuccess) {
                ReportSpamState(submitted = true, isIndiaDevice = isIndia)
            } else {
                val msg = if (BuildConfig.DEBUG)
                    result.exceptionOrNull()?.message ?: "Unknown error"
                else
                    "Could not submit report. Try again."
                ReportSpamState(error = msg, isIndiaDevice = isIndia)
            }
        }
    }
}
