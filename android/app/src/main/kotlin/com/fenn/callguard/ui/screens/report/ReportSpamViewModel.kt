package com.fenn.callguard.ui.screens.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenn.callguard.domain.repository.ReputationRepository
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
) : ViewModel() {

    private val _state = MutableStateFlow(ReportSpamState())
    val state: StateFlow<ReportSpamState> = _state.asStateFlow()

    fun submitReport(numberHash: String, category: String) {
        viewModelScope.launch {
            _state.value = ReportSpamState(loading = true)
            val result = reputationRepo.submitReport(numberHash, category)
            _state.value = if (result.isSuccess) {
                ReportSpamState(submitted = true)
            } else {
                ReportSpamState(error = "Could not submit report. Try again.")
            }
        }
    }
}
