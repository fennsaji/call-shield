package com.fenn.callshield.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fenn.callshield.domain.model.AdvancedBlockingPolicy
import com.fenn.callshield.util.BackupSettings
import com.fenn.callshield.domain.model.BlockingPreset
import com.fenn.callshield.domain.model.CountryFilterMode
import com.fenn.callshield.domain.model.UnknownCallAction
import com.fenn.callshield.ui.theme.ThemeMode
import com.fenn.callshield.util.DndOperator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "screening_prefs")

@Singleton
class ScreeningPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val AUTO_BLOCK = booleanPreferencesKey("auto_block_high_confidence")
        val BLOCK_HIDDEN = booleanPreferencesKey("block_hidden_numbers")
        val NOTIFY_ON_REJECT = booleanPreferencesKey("notify_on_reject")
        val NOTIFY_ON_SILENCE = booleanPreferencesKey("notify_on_silence")
        val NOTIFY_ON_FLAG = booleanPreferencesKey("notify_on_flag")
        val NOTIFY_ON_NIGHT_GUARD = booleanPreferencesKey("notify_on_night_guard")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val TRIAL_TRIGGERED = booleanPreferencesKey("trial_triggered")
        val FAMILY_WAITLIST_EMAIL = stringPreferencesKey("family_waitlist_email")
        val TRAI_REPORTS_COUNT = intPreferencesKey("trai_reports_count")
        val DND_OPERATOR = stringPreferencesKey("dnd_operator")
        val THEME = stringPreferencesKey("theme_mode")
        // Advanced Blocking Policy
        val ABP_PRESET = stringPreferencesKey("abp_preset")
        val ABP_ALLOW_CONTACTS_ONLY = booleanPreferencesKey("abp_allow_contacts_only")
        val ABP_SILENCE_UNKNOWN = booleanPreferencesKey("abp_silence_unknown")
        val ABP_NIGHT_GUARD_ENABLED = booleanPreferencesKey("abp_night_guard_enabled")
        val ABP_NIGHT_GUARD_START = intPreferencesKey("abp_night_guard_start")
        val ABP_NIGHT_GUARD_END = intPreferencesKey("abp_night_guard_end")
        val ABP_NIGHT_GUARD_ACTION = stringPreferencesKey("abp_night_guard_action")
        val ABP_BLOCK_INTERNATIONAL = booleanPreferencesKey("abp_block_international")
        val ABP_COUNTRY_FILTER_MODE = stringPreferencesKey("abp_country_filter_mode")
        val ABP_COUNTRY_FILTER_LIST = stringPreferencesKey("abp_country_filter_list") // comma-separated ISO codes
        val ABP_AUTO_ESCALATE = booleanPreferencesKey("abp_auto_escalate")
        val ABP_AUTO_ESCALATE_THRESHOLD = intPreferencesKey("abp_auto_escalate_threshold")
    }

    suspend fun autoBlockHighConfidence(): Boolean =
        context.dataStore.data.first()[Keys.AUTO_BLOCK] ?: false

    suspend fun blockHiddenNumbers(): Boolean =
        context.dataStore.data.first()[Keys.BLOCK_HIDDEN] ?: false

    suspend fun notifyOnReject(): Boolean =
        context.dataStore.data.first()[Keys.NOTIFY_ON_REJECT] ?: true

    suspend fun notifyOnSilence(): Boolean =
        context.dataStore.data.first()[Keys.NOTIFY_ON_SILENCE] ?: true

    suspend fun notifyOnFlag(): Boolean =
        context.dataStore.data.first()[Keys.NOTIFY_ON_FLAG] ?: true

    suspend fun notifyOnNightGuard(): Boolean =
        context.dataStore.data.first()[Keys.NOTIFY_ON_NIGHT_GUARD] ?: false

    /** Reads all 6 screening flags in a single DataStore read — use this in the screening hot path. */
    suspend fun getScreeningFlags(): ScreeningFlags {
        val prefs = context.dataStore.data.first()
        return ScreeningFlags(
            autoBlockHighConfidence = prefs[Keys.AUTO_BLOCK] ?: false,
            blockHiddenNumbers      = prefs[Keys.BLOCK_HIDDEN] ?: false,
            notifyOnReject          = prefs[Keys.NOTIFY_ON_REJECT] ?: true,
            notifyOnSilence         = prefs[Keys.NOTIFY_ON_SILENCE] ?: true,
            notifyOnFlag            = prefs[Keys.NOTIFY_ON_FLAG] ?: true,
            notifyOnNightGuard      = prefs[Keys.NOTIFY_ON_NIGHT_GUARD] ?: false,
        )
    }

    data class ScreeningFlags(
        val autoBlockHighConfidence: Boolean,
        val blockHiddenNumbers: Boolean,
        val notifyOnReject: Boolean,
        val notifyOnSilence: Boolean,
        val notifyOnFlag: Boolean,
        val notifyOnNightGuard: Boolean,
    )

    suspend fun isOnboardingComplete(): Boolean =
        context.dataStore.data.first()[Keys.ONBOARDING_COMPLETE] ?: false

    fun observeOnboardingComplete(): Flow<Boolean> =
        context.dataStore.data.map { it[Keys.ONBOARDING_COMPLETE] ?: false }

    /** Observes both protection toggles in a single DataStore stream. */
    fun observeProtectionFlags(): Flow<Pair<Boolean, Boolean>> =
        context.dataStore.data.map { prefs ->
            (prefs[Keys.AUTO_BLOCK] ?: false) to (prefs[Keys.BLOCK_HIDDEN] ?: false)
        }

    /** Observes all 6 screening + notification flags as a live Flow — reacts to backup restore. */
    fun observeAllSettingsFlags(): Flow<ScreeningFlags> =
        context.dataStore.data.map { prefs ->
            ScreeningFlags(
                autoBlockHighConfidence = prefs[Keys.AUTO_BLOCK] ?: false,
                blockHiddenNumbers      = prefs[Keys.BLOCK_HIDDEN] ?: false,
                notifyOnReject          = prefs[Keys.NOTIFY_ON_REJECT] ?: true,
                notifyOnSilence         = prefs[Keys.NOTIFY_ON_SILENCE] ?: true,
                notifyOnFlag            = prefs[Keys.NOTIFY_ON_FLAG] ?: true,
                notifyOnNightGuard      = prefs[Keys.NOTIFY_ON_NIGHT_GUARD] ?: false,
            )
        }

    suspend fun setAutoBlockHighConfidence(value: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_BLOCK] = value }
    }

    suspend fun setBlockHiddenNumbers(value: Boolean) {
        context.dataStore.edit { it[Keys.BLOCK_HIDDEN] = value }
    }

    suspend fun setNotifyOnReject(value: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFY_ON_REJECT] = value }
    }

    suspend fun setNotifyOnSilence(value: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFY_ON_SILENCE] = value }
    }

    suspend fun setNotifyOnFlag(value: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFY_ON_FLAG] = value }
    }

    suspend fun setNotifyOnNightGuard(value: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFY_ON_NIGHT_GUARD] = value }
    }

    suspend fun setOnboardingComplete(value: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = value }
    }

    suspend fun isTrialTriggered(): Boolean =
        context.dataStore.data.first()[Keys.TRIAL_TRIGGERED] ?: false

    suspend fun setTrialTriggered(value: Boolean) {
        context.dataStore.edit { it[Keys.TRIAL_TRIGGERED] = value }
    }

    suspend fun getFamilyWaitlistEmail(): String? =
        context.dataStore.data.first()[Keys.FAMILY_WAITLIST_EMAIL]

    suspend fun setFamilyWaitlistEmail(email: String) {
        context.dataStore.edit { it[Keys.FAMILY_WAITLIST_EMAIL] = email }
    }

    suspend fun getTraiReportsCount(): Int =
        context.dataStore.data.first()[Keys.TRAI_REPORTS_COUNT] ?: 0

    suspend fun incrementTraiReportsCount() {
        context.dataStore.edit {
            it[Keys.TRAI_REPORTS_COUNT] = (it[Keys.TRAI_REPORTS_COUNT] ?: 0) + 1
        }
    }

    fun observeTheme(): Flow<ThemeMode> =
        context.dataStore.data.map {
            it[Keys.THEME]?.let { name -> runCatching { ThemeMode.valueOf(name) }.getOrNull() }
                ?: ThemeMode.SYSTEM
        }

    suspend fun setTheme(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = mode.name }
    }

    fun observeDndOperator(): Flow<DndOperator?> =
        context.dataStore.data.map { it[Keys.DND_OPERATOR]?.let { name -> DndOperator.fromName(name) } }

    suspend fun setDndOperator(operator: DndOperator) {
        context.dataStore.edit { it[Keys.DND_OPERATOR] = operator.name }
    }

    fun observeAdvancedBlockingPolicy(): Flow<AdvancedBlockingPolicy> =
        context.dataStore.data.map { prefs ->
            val preset = prefs[Keys.ABP_PRESET]
                ?.let { runCatching { BlockingPreset.valueOf(it) }.getOrNull() }
                ?: BlockingPreset.BALANCED
            AdvancedBlockingPolicy(
                preset = preset,
                allowContactsOnly = prefs[Keys.ABP_ALLOW_CONTACTS_ONLY] ?: false,
                silenceUnknownNumbers = prefs[Keys.ABP_SILENCE_UNKNOWN] ?: false,
                nightGuardEnabled = prefs[Keys.ABP_NIGHT_GUARD_ENABLED] ?: false,
                nightGuardStartHour = prefs[Keys.ABP_NIGHT_GUARD_START] ?: 22,
                nightGuardEndHour = prefs[Keys.ABP_NIGHT_GUARD_END] ?: 7,
                nightGuardAction = prefs[Keys.ABP_NIGHT_GUARD_ACTION]
                    ?.let { runCatching { UnknownCallAction.valueOf(it) }.getOrNull() }
                    ?: UnknownCallAction.SILENCE,
                blockInternational = prefs[Keys.ABP_BLOCK_INTERNATIONAL] ?: false,
                countryFilterMode = prefs[Keys.ABP_COUNTRY_FILTER_MODE]
                    ?.let { runCatching { CountryFilterMode.valueOf(it) }.getOrNull() }
                    ?: CountryFilterMode.OFF,
                countryFilterList = prefs[Keys.ABP_COUNTRY_FILTER_LIST]
                    ?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet(),
                autoEscalateEnabled = prefs[Keys.ABP_AUTO_ESCALATE] ?: false,
                autoEscalateThreshold = prefs[Keys.ABP_AUTO_ESCALATE_THRESHOLD] ?: 3,
            )
        }

    suspend fun getAdvancedBlockingPolicy(): AdvancedBlockingPolicy =
        observeAdvancedBlockingPolicy().first()

    suspend fun setAdvancedBlockingPolicy(policy: AdvancedBlockingPolicy) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ABP_PRESET] = policy.preset.name
            prefs[Keys.ABP_ALLOW_CONTACTS_ONLY] = policy.allowContactsOnly
            prefs[Keys.ABP_SILENCE_UNKNOWN] = policy.silenceUnknownNumbers
            prefs[Keys.ABP_NIGHT_GUARD_ENABLED] = policy.nightGuardEnabled
            prefs[Keys.ABP_NIGHT_GUARD_START] = policy.nightGuardStartHour
            prefs[Keys.ABP_NIGHT_GUARD_END] = policy.nightGuardEndHour
            prefs[Keys.ABP_NIGHT_GUARD_ACTION] = policy.nightGuardAction.name
            prefs[Keys.ABP_BLOCK_INTERNATIONAL] = policy.blockInternational
            prefs[Keys.ABP_COUNTRY_FILTER_MODE] = policy.countryFilterMode.name
            prefs[Keys.ABP_COUNTRY_FILTER_LIST] = policy.countryFilterList.joinToString(",")
            prefs[Keys.ABP_AUTO_ESCALATE] = policy.autoEscalateEnabled
            prefs[Keys.ABP_AUTO_ESCALATE_THRESHOLD] = policy.autoEscalateThreshold
        }
    }

    /** Reads all user-configurable settings in a single DataStore pass for backup. */
    suspend fun readAllForBackup(): BackupSettings {
        val prefs = context.dataStore.data.first()
        return BackupSettings(
            autoBlockHighConfidence = prefs[Keys.AUTO_BLOCK] ?: false,
            blockHiddenNumbers      = prefs[Keys.BLOCK_HIDDEN] ?: false,
            notifyOnReject          = prefs[Keys.NOTIFY_ON_REJECT] ?: true,
            notifyOnSilence         = prefs[Keys.NOTIFY_ON_SILENCE] ?: true,
            notifyOnFlag            = prefs[Keys.NOTIFY_ON_FLAG] ?: true,
            notifyOnNightGuard      = prefs[Keys.NOTIFY_ON_NIGHT_GUARD] ?: false,
            themeMode               = prefs[Keys.THEME] ?: "SYSTEM",
            abpPreset               = prefs[Keys.ABP_PRESET] ?: "BALANCED",
            abpAllowContactsOnly    = prefs[Keys.ABP_ALLOW_CONTACTS_ONLY] ?: false,
            abpSilenceUnknown       = prefs[Keys.ABP_SILENCE_UNKNOWN] ?: false,
            abpNightGuardEnabled    = prefs[Keys.ABP_NIGHT_GUARD_ENABLED] ?: false,
            abpNightGuardStart      = prefs[Keys.ABP_NIGHT_GUARD_START] ?: 22,
            abpNightGuardEnd        = prefs[Keys.ABP_NIGHT_GUARD_END] ?: 7,
            abpNightGuardAction     = prefs[Keys.ABP_NIGHT_GUARD_ACTION] ?: "SILENCE",
            abpBlockInternational   = prefs[Keys.ABP_BLOCK_INTERNATIONAL] ?: false,
            abpCountryFilterMode    = prefs[Keys.ABP_COUNTRY_FILTER_MODE] ?: "OFF",
            abpCountryFilterList    = prefs[Keys.ABP_COUNTRY_FILTER_LIST] ?: "",
            abpAutoEscalate         = prefs[Keys.ABP_AUTO_ESCALATE] ?: false,
            abpAutoEscalateThreshold = prefs[Keys.ABP_AUTO_ESCALATE_THRESHOLD] ?: 3,
        )
    }

    /** Restores all backed-up settings in a single DataStore write. */
    suspend fun restoreFromBackup(s: BackupSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_BLOCK]            = s.autoBlockHighConfidence
            prefs[Keys.BLOCK_HIDDEN]          = s.blockHiddenNumbers
            prefs[Keys.NOTIFY_ON_REJECT]      = s.notifyOnReject
            prefs[Keys.NOTIFY_ON_SILENCE]     = s.notifyOnSilence
            prefs[Keys.NOTIFY_ON_FLAG]        = s.notifyOnFlag
            prefs[Keys.NOTIFY_ON_NIGHT_GUARD] = s.notifyOnNightGuard
            prefs[Keys.THEME]                 = s.themeMode
            prefs[Keys.ABP_PRESET]                 = s.abpPreset
            prefs[Keys.ABP_ALLOW_CONTACTS_ONLY]    = s.abpAllowContactsOnly
            prefs[Keys.ABP_SILENCE_UNKNOWN]        = s.abpSilenceUnknown
            prefs[Keys.ABP_NIGHT_GUARD_ENABLED]    = s.abpNightGuardEnabled
            prefs[Keys.ABP_NIGHT_GUARD_START]      = s.abpNightGuardStart
            prefs[Keys.ABP_NIGHT_GUARD_END]        = s.abpNightGuardEnd
            prefs[Keys.ABP_NIGHT_GUARD_ACTION]     = s.abpNightGuardAction
            prefs[Keys.ABP_BLOCK_INTERNATIONAL]    = s.abpBlockInternational
            prefs[Keys.ABP_COUNTRY_FILTER_MODE]    = s.abpCountryFilterMode
            prefs[Keys.ABP_COUNTRY_FILTER_LIST]    = s.abpCountryFilterList
            prefs[Keys.ABP_AUTO_ESCALATE]          = s.abpAutoEscalate
            prefs[Keys.ABP_AUTO_ESCALATE_THRESHOLD] = s.abpAutoEscalateThreshold
        }
    }
}
