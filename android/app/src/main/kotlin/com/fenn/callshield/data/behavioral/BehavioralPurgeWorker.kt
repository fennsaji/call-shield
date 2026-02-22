package com.fenn.callshield.data.behavioral

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fenn.callshield.data.local.dao.CallerEventDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

private const val TTL_MS = 24 * 60 * 60 * 1000L  // 24 hours

/**
 * Periodic WorkManager job that enforces the 24h TTL on [recent_caller_events].
 * Runs every 12 hours; no network required.
 */
@HiltWorker
class BehavioralPurgeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val callerEventDao: CallerEventDao,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val cutoff = System.currentTimeMillis() - TTL_MS
        callerEventDao.deleteOlderThan(cutoff)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "behavioral_ttl_purge"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<BehavioralPurgeWorker>(
                repeatInterval = 12,
                repeatIntervalTimeUnit = TimeUnit.HOURS,
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
