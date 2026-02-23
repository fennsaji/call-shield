package com.fenn.callshield.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
        val NOTIFY_ON_BLOCK = booleanPreferencesKey("notify_on_block")
        val NOTIFY_ON_FLAG = booleanPreferencesKey("notify_on_flag")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val TRIAL_TRIGGERED = booleanPreferencesKey("trial_triggered")
        val FAMILY_WAITLIST_EMAIL = stringPreferencesKey("family_waitlist_email")
        val TRAI_REPORTS_COUNT = intPreferencesKey("trai_reports_count")
        val DND_OPERATOR = stringPreferencesKey("dnd_operator")
        val THEME = stringPreferencesKey("theme_mode")
    }

    suspend fun autoBlockHighConfidence(): Boolean =
        context.dataStore.data.first()[Keys.AUTO_BLOCK] ?: false

    suspend fun blockHiddenNumbers(): Boolean =
        context.dataStore.data.first()[Keys.BLOCK_HIDDEN] ?: false

    suspend fun notifyOnBlock(): Boolean =
        context.dataStore.data.first()[Keys.NOTIFY_ON_BLOCK] ?: true

    suspend fun notifyOnFlag(): Boolean =
        context.dataStore.data.first()[Keys.NOTIFY_ON_FLAG] ?: true

    suspend fun isOnboardingComplete(): Boolean =
        context.dataStore.data.first()[Keys.ONBOARDING_COMPLETE] ?: false

    fun observeOnboardingComplete(): Flow<Boolean> =
        context.dataStore.data.map { it[Keys.ONBOARDING_COMPLETE] ?: false }

    suspend fun setAutoBlockHighConfidence(value: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_BLOCK] = value }
    }

    suspend fun setBlockHiddenNumbers(value: Boolean) {
        context.dataStore.edit { it[Keys.BLOCK_HIDDEN] = value }
    }

    suspend fun setNotifyOnBlock(value: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFY_ON_BLOCK] = value }
    }

    suspend fun setNotifyOnFlag(value: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFY_ON_FLAG] = value }
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
}
