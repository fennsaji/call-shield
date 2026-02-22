package com.fenn.callshield.ui.screens.backup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenn.callshield.data.local.dao.BlocklistDao
import com.fenn.callshield.data.local.dao.PrefixRuleDao
import com.fenn.callshield.data.local.dao.WhitelistDao
import com.fenn.callshield.util.BackupManager
import com.fenn.callshield.util.BackupPayload
import com.fenn.callshield.util.WrongPinException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class BackupUiState(
    val status: BackupStatus = BackupStatus.Idle,
    val showPinDialog: PinDialogPurpose? = null,
    val pendingUri: Uri? = null,
    val pendingPayload: BackupPayload? = null,
    val showImportConfirm: Boolean = false,
)

sealed interface BackupStatus {
    data object Idle : BackupStatus
    data object Processing : BackupStatus
    data class Success(val message: String) : BackupStatus
    data class Error(val message: String) : BackupStatus
}

enum class PinDialogPurpose { EXPORT, IMPORT }

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupManager: BackupManager,
    private val blocklistDao: BlocklistDao,
    private val whitelistDao: WhitelistDao,
    private val prefixRuleDao: PrefixRuleDao,
) : ViewModel() {

    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    fun onExportFileChosen(uri: Uri) {
        _state.value = _state.value.copy(pendingUri = uri, showPinDialog = PinDialogPurpose.EXPORT)
    }

    fun onImportFileChosen(uri: Uri) {
        _state.value = _state.value.copy(pendingUri = uri, showPinDialog = PinDialogPurpose.IMPORT)
    }

    fun onPinEntered(pin: String) {
        val purpose = _state.value.showPinDialog ?: return
        val uri = _state.value.pendingUri ?: return
        _state.value = _state.value.copy(showPinDialog = null)
        when (purpose) {
            PinDialogPurpose.EXPORT -> doExport(uri, pin)
            PinDialogPurpose.IMPORT -> doImport(uri, pin)
        }
    }

    fun onPinDialogDismissed() {
        _state.value = _state.value.copy(showPinDialog = null, pendingUri = null)
    }

    fun onImportConfirmed() {
        val payload = _state.value.pendingPayload ?: return
        _state.value = _state.value.copy(showImportConfirm = false, status = BackupStatus.Processing)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val entities = backupManager.toRoomEntities(payload)
                blocklistDao.deleteAll()
                whitelistDao.deleteAll()
                prefixRuleDao.deleteAll()
                entities.blocklist.forEach { blocklistDao.insert(it) }
                entities.whitelist.forEach { whitelistDao.insert(it) }
                entities.prefixRules.forEach { prefixRuleDao.insert(it) }
            }
            val total = payload.blocklist.size + payload.whitelist.size + payload.prefixRules.size
            _state.value = _state.value.copy(
                status = BackupStatus.Success("Restored $total entries"),
                pendingPayload = null,
            )
        }
    }

    fun onImportCancelled() {
        _state.value = _state.value.copy(showImportConfirm = false, pendingPayload = null)
    }

    fun clearStatus() {
        _state.value = _state.value.copy(status = BackupStatus.Idle)
    }

    private fun doExport(uri: Uri, pin: String) {
        _state.value = _state.value.copy(status = BackupStatus.Processing, pendingUri = null)
        viewModelScope.launch {
            try {
                val blocklist   = withContext(Dispatchers.IO) { blocklistDao.observeAll().first() }
                val whitelist   = withContext(Dispatchers.IO) { whitelistDao.observeAll().first() }
                val prefixRules = withContext(Dispatchers.IO) { prefixRuleDao.observeAll().first() }
                val bytes = withContext(Dispatchers.Default) {
                    backupManager.exportBackup(blocklist, whitelist, prefixRules, pin)
                }
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                        ?: error("Could not open output stream")
                }
                val total = blocklist.size + whitelist.size + prefixRules.size
                _state.value = _state.value.copy(status = BackupStatus.Success("Exported $total entries"))
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    status = BackupStatus.Error("Export failed: ${e.message}")
                )
            }
        }
    }

    private fun doImport(uri: Uri, pin: String) {
        _state.value = _state.value.copy(status = BackupStatus.Processing, pendingUri = null)
        viewModelScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.readBytes()
                        ?: error("Could not read file")
                }
                val payload = withContext(Dispatchers.Default) {
                    backupManager.importBackup(bytes, pin)
                }
                _state.value = _state.value.copy(
                    status = BackupStatus.Idle,
                    pendingPayload = payload,
                    showImportConfirm = true,
                )
            } catch (_: WrongPinException) {
                _state.value = _state.value.copy(
                    status = BackupStatus.Error("Incorrect PIN. Please try again.")
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    status = BackupStatus.Error("Import failed: ${e.message}")
                )
            }
        }
    }
}
