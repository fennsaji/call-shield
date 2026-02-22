package com.fenn.callshield.ui.screens.dnd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenn.callshield.data.local.dao.DndCommandDao
import com.fenn.callshield.data.local.entity.DndCommandEntry
import com.fenn.callshield.data.preferences.ScreeningPreferences
import com.fenn.callshield.util.DndOperator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DndMode { FULL, PROMO, CUSTOM, NONE }

private fun DndCommandEntry?.toActiveMode(): DndMode? = when (this?.command) {
    "FULL" -> DndMode.FULL
    "PROMO" -> DndMode.PROMO
    "PARTIAL" -> DndMode.CUSTOM
    else -> null
}

private fun DndCommandEntry?.toActiveCategories(): Set<Int> =
    if (this?.command == "PARTIAL")
        this.categories?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.toSet() ?: emptySet()
    else emptySet()

data class DndUiState(
    val latestCommand: DndCommandEntry? = null,
    val operator: DndOperator? = null,
    // What's actually registered with TRAI (confirmed or pending):
    val activeMode: DndMode? = null,
    val activeCategories: Set<Int> = emptySet(),
    // User's explicit selection (null = user hasn't touched anything yet):
    val explicitMode: DndMode? = null,
    val explicitCategories: Set<Int>? = null,
    // True when selection differs from active state — enables the Send button:
    val hasChanges: Boolean = false,
) {
    /** Effective mode to display in the picker. Null when nothing has been configured yet. */
    val displayedMode: DndMode?
        get() = explicitMode ?: activeMode

    /** Effective categories to display in the chip grid. */
    val displayedCategories: Set<Int>
        get() = explicitCategories ?: activeCategories
}

@HiltViewModel
class DndManagementViewModel @Inject constructor(
    private val dndCommandDao: DndCommandDao,
    private val prefs: ScreeningPreferences,
) : ViewModel() {

    private val _explicitMode = MutableStateFlow<DndMode?>(null)
    private val _explicitCategories = MutableStateFlow<Set<Int>?>(null)

    val uiState: StateFlow<DndUiState> = combine(
        dndCommandDao.observeLatestActive(),
        combine(_explicitMode, _explicitCategories) { m, c -> m to c },
        prefs.observeDndOperator(),
    ) { latest, (explMode, explCats), operator ->
        val activeMode = latest.toActiveMode()
        val activeCats = latest.toActiveCategories()
        val effMode = explMode ?: activeMode ?: DndMode.FULL
        val effCats = explCats ?: activeCats
        val hasChanges = explMode != null && (
            effMode != activeMode ||
            (effMode == DndMode.CUSTOM && effCats != activeCats)
        )
        DndUiState(
            latestCommand = latest,
            operator = operator,
            activeMode = activeMode,
            activeCategories = activeCats,
            explicitMode = explMode,
            explicitCategories = explCats,
            hasChanges = hasChanges,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DndUiState(),
    )

    fun setMode(mode: DndMode) {
        _explicitMode.value = mode
        // Pre-fill categories with active ones when switching to CUSTOM for the first time
        if (mode == DndMode.CUSTOM && _explicitCategories.value == null) {
            _explicitCategories.value = uiState.value.activeCategories
        }
    }

    fun toggleCategory(code: Int) {
        val current = _explicitCategories.value ?: uiState.value.activeCategories
        _explicitCategories.value = current.toMutableSet().apply {
            if (contains(code)) remove(code) else add(code)
        }
    }

    fun recordCommand(command: String, smsBody: String, categories: String?) {
        viewModelScope.launch {
            dndCommandDao.insert(
                DndCommandEntry(
                    command = command,
                    smsBody = smsBody,
                    categories = categories,
                )
            )
            // Reset explicit selections so UI reflects new active state
            _explicitMode.value = null
            _explicitCategories.value = null
        }
    }

    fun confirmLatest() {
        val id = uiState.value.latestCommand?.id ?: return
        viewModelScope.launch {
            dndCommandDao.markConfirmed(id)
        }
    }

    fun setOperator(operator: DndOperator) {
        viewModelScope.launch { prefs.setDndOperator(operator) }
    }
}
