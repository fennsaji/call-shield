package com.fenn.callguard.ui.screens.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenn.callguard.data.local.dao.BlocklistDao
import com.fenn.callguard.data.local.dao.CallHistoryDao
import com.fenn.callguard.data.local.dao.PrefixRuleDao
import com.fenn.callguard.data.local.dao.SeedDbDao
import com.fenn.callguard.data.local.dao.WhitelistDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class PrivacyDashboardState(
    val hashedLookupsSent: Int = 0,
    val reportsSubmitted: Int = 0,
    val seedDbVersion: String? = null,
    val lastSyncDisplay: String = "Never",
)

@HiltViewModel
class PrivacyDashboardViewModel @Inject constructor(
    private val seedDbDao: SeedDbDao,
    private val blocklistDao: BlocklistDao,
    private val whitelistDao: WhitelistDao,
    private val prefixRuleDao: PrefixRuleDao,
    private val callHistoryDao: CallHistoryDao,
) : ViewModel() {

    private val _state = MutableStateFlow(PrivacyDashboardState())
    val state: StateFlow<PrivacyDashboardState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val meta = seedDbDao.getMeta()
            val lastSync = meta?.downloadedAt?.let {
                SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(it))
            } ?: "Never"
            _state.value = _state.value.copy(
                seedDbVersion = meta?.version?.let { "v$it" },
                lastSyncDisplay = lastSync,
            )
        }
    }

    /** PRD §3.9: one-tap option to delete all local data. */
    fun deleteAllData() {
        viewModelScope.launch {
            // Clear all user data from Room — preferences intentionally preserved
            // (user settings should survive a data reset)
            callHistoryDao.pruneOldEntries()
            // Wipe via a direct clear approach — Room doesn't have a bulk delete, use DAO queries
            deleteAllTables()
            _state.value = PrivacyDashboardState()
        }
    }

    private suspend fun deleteAllTables() {
        blocklistDao.deleteAll()
        whitelistDao.deleteAll()
        prefixRuleDao.deleteAll()
        callHistoryDao.deleteAll()
        seedDbDao.clearAll()
    }
}
