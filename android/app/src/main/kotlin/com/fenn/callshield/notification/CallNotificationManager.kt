package com.fenn.callshield.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.fenn.callshield.MainActivity
import com.fenn.callshield.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

private const val CHANNEL_GROUP = "cg_group_calls"
private const val CHANNEL_BLOCKED = "cg_blocked_calls"
private const val CHANNEL_FLAGGED = "cg_flagged_calls"
private const val CHANNEL_MISSED_WARNING = "cg_missed_call_warning"
private const val CHANNEL_NIGHT_GUARD = "cg_night_guard"

@Singleton
class CallNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val notifManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val notifIdCounter = AtomicInteger(1000)

    init {
        createChannels()
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) Log.w(TAG, "POST_NOTIFICATIONS not granted — notification suppressed")
        return granted
    }

    /**
     * PRD §3.13: notification must show "Undo / Mark as Not Spam" action.
     * @param numberHash HMAC hash of the number — passed to MarkNotSpamUseCase on action tap.
     */
    /**
     * PRD §3.13: auto-block notification with reason + "Undo / Mark as Not Spam" action.
     * @param reason Human-readable reason e.g. "In your blocklist", "Reported by community".
     */
    fun showBlockedCallNotification(
        displayLabel: String,
        numberHash: String,
        reason: String? = null,
    ) {
        if (!hasNotificationPermission()) return
        val notifId = notifIdCounter.getAndIncrement()
        Log.d(TAG, "Posting blocked notification id=$notifId for $displayLabel reason=$reason")

        val openIntent = PendingIntent.getActivity(
            context, notifId, mainActivityIntent(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notSpamIntent = Intent(ACTION_NOT_SPAM).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_NUMBER_HASH, numberHash)
            putExtra(EXTRA_DISPLAY_LABEL, displayLabel)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
        }
        val notSpamPending = PendingIntent.getBroadcast(
            context,
            notifId + 10_000,
            notSpamIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val bodyText = if (reason != null) "$displayLabel · $reason" else "Blocked call from $displayLabel"

        val notification = NotificationCompat.Builder(context, CHANNEL_BLOCKED)
            .setSmallIcon(R.drawable.ic_shield_blocked)
            .setContentTitle("Spam call blocked")
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(
                R.drawable.ic_shield_warning,
                context.getString(R.string.not_spam_undo),
                notSpamPending,
            )
            .build()

        notifManager.notify(notifId, notification)
    }

    /**
     * PRD §3.3: Missed call callback risk indicator.
     * Fires after a call is silenced (ring suppressed → goes to missed calls).
     * Warns the user not to call back a reported spam number.
     */
    fun showMissedCallWarningNotification(
        displayLabel: String,
        numberHash: String,
        category: String?,
    ) {
        if (!hasNotificationPermission()) return
        val notifId = notifIdCounter.getAndIncrement()
        val categoryLabel = category
            ?.replace('_', ' ')
            ?.replaceFirstChar { it.uppercase() }
            ?: "Spam"

        val openIntent = PendingIntent.getActivity(
            context, notifId, mainActivityIntent(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notSpamIntent = Intent(ACTION_NOT_SPAM).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_NUMBER_HASH, numberHash)
            putExtra(EXTRA_DISPLAY_LABEL, displayLabel)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
        }
        val notSpamPending = PendingIntent.getBroadcast(
            context,
            notifId + 10_000,
            notSpamIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MISSED_WARNING)
            .setSmallIcon(R.drawable.ic_shield_warning)
            .setContentTitle("Missed call — $categoryLabel risk")
            .setContentText("$displayLabel has been reported as $categoryLabel. Avoid calling back.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$displayLabel has been reported as $categoryLabel. Proceed with caution before calling back — this may be a callback scam.")
            )
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(
                R.drawable.ic_shield_warning,
                context.getString(R.string.not_spam_undo),
                notSpamPending,
            )
            .build()

        notifManager.notify(notifId, notification)
        Log.d(TAG, "Missed call warning posted id=$notifId for $displayLabel category=$categoryLabel")
    }

    fun showFlaggedCallNotification(
        displayLabel: String,
        confidenceScore: Double,
        category: String?,
    ) {
        if (!hasNotificationPermission()) return
        val percent = (confidenceScore * 100).roundToInt()
        val categoryLabel = category?.replace('_', ' ')?.replaceFirstChar { it.uppercase() } ?: "Spam"
        Log.d(TAG, "Posting flagged notification for $displayLabel score=$percent%")
        val openIntent = PendingIntent.getActivity(
            context, 0, mainActivityIntent(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_FLAGGED)
            .setSmallIcon(R.drawable.ic_shield_warning)
            .setContentTitle("Possible spam call")
            .setContentText("$displayLabel — $categoryLabel ($percent% confidence)")
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notifManager.notify(notifIdCounter.getAndIncrement(), notification)
    }

    fun showNightGuardNotification(
        displayLabel: String,
        numberHash: String,
    ) {
        if (!hasNotificationPermission()) return
        val notifId = notifIdCounter.getAndIncrement()
        Log.d(TAG, "Posting Night Guard notification id=$notifId for $displayLabel")

        val openIntent = PendingIntent.getActivity(
            context, notifId, mainActivityIntent(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notSpamIntent = Intent(ACTION_NOT_SPAM).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_NUMBER_HASH, numberHash)
            putExtra(EXTRA_DISPLAY_LABEL, displayLabel)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
        }
        val notSpamPending = PendingIntent.getBroadcast(
            context,
            notifId + 10_000,
            notSpamIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_NIGHT_GUARD)
            .setSmallIcon(R.drawable.ic_shield_warning)
            .setContentTitle("Call silenced during sleep hours")
            .setContentText("$displayLabel was silenced by Night Guard")
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                R.drawable.ic_shield_warning,
                context.getString(R.string.not_spam_undo),
                notSpamPending,
            )
            .build()

        notifManager.notify(notifId, notification)
    }

    private fun createChannels() {
        notifManager.createNotificationChannelGroup(
            NotificationChannelGroup(CHANNEL_GROUP, "Call Protection")
        )

        val blockedChannel = NotificationChannel(
            CHANNEL_BLOCKED,
            "Spam blocked",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notifications when a spam call is automatically blocked"
            group = CHANNEL_GROUP
        }

        val flaggedChannel = NotificationChannel(
            CHANNEL_FLAGGED,
            "Possible spam",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Notifications when an incoming call is flagged as possible spam"
            group = CHANNEL_GROUP
        }

        val missedWarningChannel = NotificationChannel(
            CHANNEL_MISSED_WARNING,
            "Call silenced",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Warns you when a missed call is from a reported spam number — helps prevent callback scams"
            group = CHANNEL_GROUP
        }

        val nightGuardChannel = NotificationChannel(
            CHANNEL_NIGHT_GUARD,
            "Night Guard",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Notify when calls are silenced during sleep hours"
            group = CHANNEL_GROUP
        }

        notifManager.createNotificationChannels(
            listOf(blockedChannel, flaggedChannel, missedWarningChannel, nightGuardChannel)
        )
    }

    private fun mainActivityIntent() =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

    companion object {
        private const val TAG = "CallShield.Notification"
    }
}
