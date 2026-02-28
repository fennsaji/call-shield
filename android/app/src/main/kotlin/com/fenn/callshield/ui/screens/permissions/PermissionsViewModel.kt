package com.fenn.callshield.ui.screens.permissions

import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.Manifest
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.fenn.callshield.data.local.ContactsLookupHelper
import com.fenn.callshield.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class PermissionsState(
    val screeningRoleGranted: Boolean = false,
    val notificationsGranted: Boolean = false,
    val batteryOptimisationDisabled: Boolean = false,
    val oemBatteryHint: String = "",
    val callLogGranted: Boolean = false,
)

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactsLookupHelper: ContactsLookupHelper,
) : ViewModel() {

    private val _state = MutableStateFlow(PermissionsState())
    val state: StateFlow<PermissionsState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.value = PermissionsState(
            screeningRoleGranted = isScreeningRoleGranted(),
            notificationsGranted = isNotificationsGranted(),
            batteryOptimisationDisabled = isBatteryOptimisationDisabled(),
            oemBatteryHint = oemBatteryHint(),
            callLogGranted = isCallLogGranted(),
        )
        // Re-attempt contact set initialization in case READ_CONTACTS was just granted
        contactsLookupHelper.initialize()
    }

    private fun isCallLogGranted(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG,
        ) == PackageManager.PERMISSION_GRANTED

    private fun isScreeningRoleGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                context.getSystemService(RoleManager::class.java)
                    ?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
            } catch (_: Exception) {
                // Some OEM ROMs (e.g. MIUI) throw or return null on RoleManager calls
                false
            }
        } else false
    }

    private fun isNotificationsGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun isBatteryOptimisationDisabled(): Boolean {
        val pm = context.getSystemService(PowerManager::class.java)
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** PRD §3.13: provide OEM-specific battery optimization instructions. */
    private fun oemBatteryHint(): String {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val resId = when {
            "xiaomi" in manufacturer || "redmi" in manufacturer -> R.string.battery_oem_miui
            "samsung" in manufacturer -> R.string.battery_oem_samsung
            "oneplus" in manufacturer -> R.string.battery_oem_oneplus
            "huawei" in manufacturer || "honor" in manufacturer -> R.string.battery_oem_huawei
            "oppo" in manufacturer || "realme" in manufacturer -> R.string.battery_oem_oppo
            "vivo" in manufacturer || "iqoo" in manufacturer -> R.string.battery_oem_vivo
            else -> R.string.battery_oem_generic
        }
        return context.getString(resId)
    }
}
