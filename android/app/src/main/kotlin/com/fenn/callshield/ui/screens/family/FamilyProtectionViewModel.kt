package com.fenn.callshield.ui.screens.family

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenn.callshield.family.FamilyRole
import com.fenn.callshield.family.FamilySyncRepository
import com.fenn.callshield.family.FamilySyncRule
import com.fenn.callshield.family.FamilyTokenManager
import com.fenn.callshield.util.QrCodeGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class FamilyProtectionUiState(
    val role: FamilyRole? = null,
    val qrBitmap: Bitmap? = null,
    val syncedRules: List<FamilySyncRule> = emptyList(),
    val showScanner: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class FamilyProtectionViewModel @Inject constructor(
    private val familyTokenManager: FamilyTokenManager,
    private val familySyncRepository: FamilySyncRepository,
    private val qrCodeGenerator: QrCodeGenerator,
) : ViewModel() {

    private val _state = MutableStateFlow(FamilyProtectionUiState())
    val state: StateFlow<FamilyProtectionUiState> = _state.asStateFlow()

    init {
        // Observe role changes from DataStore (handles app restart / re-pairing)
        viewModelScope.launch {
            familyTokenManager.observeRole().collect { role ->
                _state.update { it.copy(role = role) }
            }
        }
    }

    // ── Guardian flow ─────────────────────────────────────────────────────────

    /** Generates a new pairing UUID, shows QR, and registers with backend. */
    fun startAsGuardian() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val token     = familyTokenManager.generateGuardianToken()
            val tokenHash = familyTokenManager.getTokenHash()!!

            // Show QR immediately so the user can see it while the network call completes
            _state.update { it.copy(qrBitmap = qrCodeGenerator.generate(token)) }

            // Register with backend — 9-min expiry to stay safely within the 10-min window
            val expiresAt = Instant.now().plus(9, ChronoUnit.MINUTES).toString()
            familySyncRepository.registerPairing(tokenHash, expiresAt)
                .onFailure { ex ->
                    _state.update { it.copy(error = "Registration failed: ${ex.message}") }
                }

            _state.update { it.copy(isLoading = false) }
        }
    }

    /** Re-generates a fresh QR (e.g. after the 10-min window expires). */
    fun refreshGuardianQr() = startAsGuardian()

    // ── Dependent flow ────────────────────────────────────────────────────────

    fun showScanner()    { _state.update { it.copy(showScanner = true) } }
    fun dismissScanner() { _state.update { it.copy(showScanner = false) } }

    /** Called by QrScanner when a QR code is decoded. Stores token and pulls initial rules. */
    fun onQrScanned(rawToken: String) {
        _state.update { it.copy(showScanner = false) }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            familyTokenManager.storeAsDependentToken(rawToken)
            val tokenHash = familyTokenManager.getTokenHash()!!

            // Pull rules — also marks paired_at on the server (see assertPaired in Edge Function)
            familySyncRepository.pullRules(tokenHash)
                .onSuccess { rules -> _state.update { it.copy(syncedRules = rules) } }
                .onFailure { ex  -> _state.update { it.copy(error = "Pairing failed: ${ex.message}") } }

            _state.update { it.copy(isLoading = false) }
        }
    }

    /** Dependent: manually refreshes rules from the guardian. */
    fun syncNow() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val tokenHash = familyTokenManager.getTokenHash() ?: run {
                _state.update { it.copy(isLoading = false) }
                return@launch
            }
            familySyncRepository.pullRules(tokenHash)
                .onSuccess { rules -> _state.update { it.copy(syncedRules = rules) } }
                .onFailure { ex  -> _state.update { it.copy(error = "Sync failed: ${ex.message}") } }
            _state.update { it.copy(isLoading = false) }
        }
    }

    // ── Shared ────────────────────────────────────────────────────────────────

    /** Removes the pairing: best-effort backend delete, then clears local state. */
    fun unpair() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val tokenHash = familyTokenManager.getTokenHash()
            if (tokenHash != null) {
                familySyncRepository.unpair(tokenHash) // best-effort — ignore failure
            }
            familyTokenManager.clearPairing()
            _state.update { it.copy(qrBitmap = null, syncedRules = emptyList(), isLoading = false) }
        }
    }

    fun clearError() { _state.update { it.copy(error = null) } }
}
