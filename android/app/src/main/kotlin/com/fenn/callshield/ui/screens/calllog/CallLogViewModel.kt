package com.fenn.callshield.ui.screens.calllog

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenn.callshield.data.local.DeviceCallLogEntry
import com.fenn.callshield.data.local.DeviceCallLogReader
import com.fenn.callshield.domain.repository.BlocklistRepository
import com.fenn.callshield.util.PhoneNumberHasher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Entry paired with its pre-computed hash (null if number can't be normalised). */
data class CallLogEntryWithHash(
    val entry: DeviceCallLogEntry,
    val hash: String?,
)

data class CallLogUiState(
    val permissionGranted: Boolean = false,
    val entries: List<CallLogEntryWithHash> = emptyList(),
    val blockedHashes: Set<String> = emptySet(),
    val loading: Boolean = false,
    val snackbar: String? = null,
)

@HiltViewModel
class CallLogViewModel @Inject constructor(
    private val reader: DeviceCallLogReader,
    private val blocklistRepo: BlocklistRepository,
    private val hasher: PhoneNumberHasher,
) : ViewModel() {

    private val _state = MutableStateFlow(CallLogUiState())
    val state: StateFlow<CallLogUiState> = _state.asStateFlow()

    fun checkPermissionAndLoad(context: Context) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CALL_LOG,
        ) == PackageManager.PERMISSION_GRANTED
        _state.value = _state.value.copy(permissionGranted = granted)
        if (granted) loadLogs()
    }

    fun onPermissionResult(granted: Boolean) {
        _state.value = _state.value.copy(permissionGranted = granted)
        if (granted) loadLogs()
    }

    private fun loadLogs() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val rawEntries = withContext(Dispatchers.IO) { reader.read() }
            // Hash each number synchronously (HMAC-SHA256 is fast)
            val entries = rawEntries.map { entry ->
                CallLogEntryWithHash(
                    entry = entry,
                    hash = if (entry.number.isBlank()) null else hasher.hash(entry.number),
                )
            }
            _state.value = _state.value.copy(entries = entries, loading = false)
        }
    }

    /** Adds [rawNumber] to the personal blocklist (hashed on-device, never uploaded). */
    fun blockNumber(rawNumber: String) {
        if (rawNumber.isBlank()) return
        viewModelScope.launch {
            val hash = hasher.hash(rawNumber) ?: return@launch
            val label = rawNumber.takeLast(4).let { "****$it" }
            blocklistRepo.add(hash, label)
            _state.value = _state.value.copy(
                blockedHashes = _state.value.blockedHashes + hash,
                snackbar = "Number blocked",
            )
        }
    }

    fun clearSnackbar() {
        _state.value = _state.value.copy(snackbar = null)
    }
}
