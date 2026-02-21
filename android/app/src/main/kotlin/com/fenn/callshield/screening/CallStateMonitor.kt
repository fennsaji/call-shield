package com.fenn.callshield.screening

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import com.fenn.callshield.data.local.dao.CallerEventDao
import com.fenn.callshield.data.local.entity.CallerEventEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registers a TelephonyCallback (API 31+) or PhoneStateListener (API 29-30) to
 * detect when an incoming call ends without being answered (RINGING → IDLE).
 *
 * When a short-ring is detected it inserts a SHORT_RING [CallerEventEntry] so
 * [CallFrequencyAnalyzer] / [ScreenCallUseCase] can flag repeat bait callers.
 *
 * Note: Both paths (API 31+ TelephonyCallback and legacy PhoneStateListener) are
 * wrapped in try-catch SecurityException. On Android 14 the system still enforces
 * READ_PHONE_STATE even for state-only TelephonyCallback registration. Short-ring
 * detection degrades gracefully — other screening functions are unaffected.
 */
@Singleton
class CallStateMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ringTimeRegistry: RingTimeRegistry,
    private val callerEventDao: CallerEventDao,
) {
    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                telephonyManager.registerTelephonyCallback(context.mainExecutor, api31Callback)
                Log.d(TAG, "CallStateMonitor started via TelephonyCallback (API ${Build.VERSION.SDK_INT})")
            } catch (se: SecurityException) {
                Log.w(TAG, "TelephonyCallback registration denied — short-ring detection disabled (API ${Build.VERSION.SDK_INT})")
            }
        } else {
            registerLegacyListener()
        }
    }

    fun stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                telephonyManager.unregisterTelephonyCallback(api31Callback)
            } catch (se: SecurityException) {
                // Was never registered — nothing to do.
            }
        } else {
            @Suppress("DEPRECATION")
            telephonyManager.listen(legacyListener, PhoneStateListener.LISTEN_NONE)
        }
    }

    private fun onCallIdle() {
        val (hash, isShort) = ringTimeRegistry.onCallEnded() ?: return
        if (!isShort) return
        scope.launch {
            callerEventDao.insert(CallerEventEntry(numberHash = hash, eventType = "SHORT_RING"))
            callerEventDao.enforceCapForHash(hash)
            Log.d(TAG, "SHORT_RING recorded for hash=${hash.take(8)}…")
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private val api31Callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) {
            if (state == TelephonyManager.CALL_STATE_IDLE) onCallIdle()
        }
    }

    @Suppress("DEPRECATION")
    private val legacyListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            if (state == TelephonyManager.CALL_STATE_IDLE) onCallIdle()
        }
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun registerLegacyListener() {
        try {
            telephonyManager.listen(legacyListener, PhoneStateListener.LISTEN_CALL_STATE)
        } catch (se: SecurityException) {
            Log.w(TAG, "READ_PHONE_STATE not granted — short-ring detection disabled on API ${Build.VERSION.SDK_INT}")
        }
    }

    companion object {
        private const val TAG = "CallShield.StateMonitor"
    }
}
