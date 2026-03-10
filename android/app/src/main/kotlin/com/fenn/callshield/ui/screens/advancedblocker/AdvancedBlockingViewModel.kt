package com.fenn.callshield.ui.screens.advancedblocker

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenn.callshield.billing.BillingManager
import com.fenn.callshield.data.local.BlocklistAgingWorker
import com.fenn.callshield.data.local.ContactsLookupHelper
import com.fenn.callshield.data.local.entity.VipContactEntry
import com.fenn.callshield.data.preferences.ScreeningPreferences
import com.fenn.callshield.domain.model.AdvancedBlockingPolicy
import com.fenn.callshield.domain.model.BlockingPreset
import com.fenn.callshield.domain.model.toDefaultPolicy
import com.fenn.callshield.domain.repository.VipContactsRepository
import com.fenn.callshield.util.HomeCountryProvider
import com.fenn.callshield.util.PhoneNumberHasher
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AdvancedBlockingViewModel @Inject constructor(
    private val prefs: ScreeningPreferences,
    private val billingManager: BillingManager,
    private val homeCountryProvider: HomeCountryProvider,
    private val vipContactsRepository: VipContactsRepository,
    private val contactsLookupHelper: ContactsLookupHelper,
    private val hasher: PhoneNumberHasher,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    val isPro: StateFlow<Boolean> = billingManager.isPro

    /** E.164 calling code for the device's home country, e.g. "+91", "+1". */
    val homeCallingCode: String get() = homeCountryProvider.callingCodePrefix

    /** ISO 3166-1 alpha-2 code for the device's home country, e.g. "IN", "US". */
    val homeIsoCode: String get() = homeCountryProvider.isoCode

    val policy: StateFlow<AdvancedBlockingPolicy> = prefs.observeAdvancedBlockingPolicy()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AdvancedBlockingPolicy(),
        )

    val vipContacts: StateFlow<List<VipContactEntry>> = vipContactsRepository.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _vipSearchQuery = MutableStateFlow("")

    /**
     * Search results as (displayName, e164, isAlreadyAdded).
     * The isAlreadyAdded flag is computed by comparing HMAC-SHA256(e164) against the stored VIP hashes.
     */
    @OptIn(FlowPreview::class)
    val vipSearchResults: StateFlow<List<Triple<String, String, Boolean>>> = _vipSearchQuery
        .debounce(300)
        .mapLatest { query ->
            val results = withContext(Dispatchers.IO) { contactsLookupHelper.queryContacts(query) }
            val currentHashes = vipContacts.value.map { it.numberHash }.toHashSet()
            results.map { (name, e164) ->
                val hash = hasher.hash(e164)
                Triple(name, e164, hash != null && hash in currentHashes)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun setVipSearchQuery(query: String) { _vipSearchQuery.value = query }

    init {
        // Keep BlocklistAgingWorker in sync with the blocklistAgingEnabled toggle
        viewModelScope.launch {
            prefs.observeAdvancedBlockingPolicy().collect { p ->
                if (p.blocklistAgingEnabled) BlocklistAgingWorker.schedule(appContext)
                else BlocklistAgingWorker.cancel(appContext)
            }
        }
    }

    fun setPreset(preset: BlockingPreset) {
        viewModelScope.launch {
            prefs.setAdvancedBlockingPolicy(preset.toDefaultPolicy())
        }
    }

    fun updatePolicy(policy: AdvancedBlockingPolicy) {
        viewModelScope.launch {
            prefs.setAdvancedBlockingPolicy(policy)
        }
    }

    fun addVipContact(e164: String, displayLabel: String) {
        viewModelScope.launch { vipContactsRepository.add(e164, displayLabel) }
    }

    fun removeVipContact(numberHash: String) {
        viewModelScope.launch { vipContactsRepository.remove(numberHash) }
    }
}
