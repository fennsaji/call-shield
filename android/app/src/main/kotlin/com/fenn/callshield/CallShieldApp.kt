package com.fenn.callshield

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.fenn.callshield.billing.BillingManager
import com.fenn.callshield.data.behavioral.BehavioralPurgeWorker
import com.fenn.callshield.data.local.ContactsLookupHelper
import com.fenn.callshield.screening.CallStateMonitor
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class CallShieldApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var billingManager: BillingManager
    @Inject lateinit var callStateMonitor: CallStateMonitor
    @Inject lateinit var contactsLookupHelper: ContactsLookupHelper

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        callStateMonitor.start()
        contactsLookupHelper.initialize()
        BehavioralPurgeWorker.schedule(this)
        appScope.launch {
            if (billingManager.connect()) billingManager.refreshSubscriptionStatus()
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
