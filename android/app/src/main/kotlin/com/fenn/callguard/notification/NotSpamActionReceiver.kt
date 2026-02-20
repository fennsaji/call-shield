package com.fenn.callguard.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fenn.callguard.domain.usecase.MarkNotSpamUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

const val ACTION_NOT_SPAM = "com.fenn.callguard.action.NOT_SPAM"
const val EXTRA_NUMBER_HASH = "number_hash"
const val EXTRA_DISPLAY_LABEL = "display_label"
const val EXTRA_NOTIFICATION_ID = "notification_id"

/**
 * Handles the "Mark as Not Spam" action from the blocked call notification.
 * PRD §3.13: "Undo / Mark as Not Spam" action must be present on auto-block notification.
 */
@AndroidEntryPoint
class NotSpamActionReceiver : BroadcastReceiver() {

    @Inject lateinit var markNotSpamUseCase: MarkNotSpamUseCase

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_NOT_SPAM) return

        val numberHash = intent.getStringExtra(EXTRA_NUMBER_HASH) ?: return
        val displayLabel = intent.getStringExtra(EXTRA_DISPLAY_LABEL) ?: "Unknown"
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        // goAsync() keeps the receiver process alive until pendingResult.finish()
        // is called, preventing the OS from killing the process before the
        // coroutine (DB write + network call) completes.
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                markNotSpamUseCase.execute(numberHash, displayLabel)
            } finally {
                pendingResult.finish()
            }
        }

        // Dismiss the notification (safe to do synchronously before finish())
        if (notifId != -1) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(notifId)
        }
    }
}
