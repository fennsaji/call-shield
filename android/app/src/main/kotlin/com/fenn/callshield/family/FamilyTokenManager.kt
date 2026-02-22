package com.fenn.callshield.family

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fenn.callshield.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

private val Context.familyDataStore: DataStore<Preferences> by preferencesDataStore(name = "family_prefs")

/**
 * Manages the local family pairing token.
 *
 * Guardian: generates a UUID → stores it → shows QR containing raw UUID.
 * Dependent: receives UUID via QR scan → stores it.
 *
 * Only HMAC-SHA256(token, bundled_salt) is ever sent to the backend.
 * The raw UUID stays on-device.
 */
@Singleton
class FamilyTokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private object Keys {
        val TOKEN = stringPreferencesKey("family_token")
        val ROLE  = stringPreferencesKey("family_role")
    }

    /** Generates a new UUID, stores it as GUARDIAN, and returns the raw UUID (for QR encoding). */
    suspend fun generateGuardianToken(): String {
        val token = UUID.randomUUID().toString()
        context.familyDataStore.edit { prefs ->
            prefs[Keys.TOKEN] = token
            prefs[Keys.ROLE]  = FamilyRole.GUARDIAN.name
        }
        return token
    }

    /** Stores a UUID received via QR scan and marks this device as DEPENDENT. */
    suspend fun storeAsDependentToken(token: String) {
        context.familyDataStore.edit { prefs ->
            prefs[Keys.TOKEN] = token
            prefs[Keys.ROLE]  = FamilyRole.DEPENDENT.name
        }
    }

    /** Returns the raw UUID, or null if not currently paired. */
    suspend fun getToken(): String? =
        context.familyDataStore.data.first()[Keys.TOKEN]

    /** Returns HMAC-SHA256(token) for backend calls, or null if not paired. */
    suspend fun getTokenHash(): String? = getToken()?.let { hashToken(it) }

    /** Observes the current role. Emits null if not paired. */
    fun observeRole(): Flow<FamilyRole?> =
        context.familyDataStore.data.map { prefs ->
            prefs[Keys.ROLE]?.let { FamilyRole.valueOf(it) }
        }

    /** Clears the pairing token and role (called after unpair completes). */
    suspend fun clearPairing() {
        context.familyDataStore.edit { prefs ->
            prefs.remove(Keys.TOKEN)
            prefs.remove(Keys.ROLE)
        }
    }

    private fun hashToken(token: String): String {
        val salt = BuildConfig.HMAC_SALT.toByteArray(Charsets.UTF_8)
        val mac  = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(salt, "HmacSHA256"))
        return mac.doFinal(token.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
