package com.fenn.callshield.data.local

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fenn.callshield.data.local.dao.BlocklistDao
import com.fenn.callshield.data.preferences.ScreeningPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Daily WorkManager job that removes blocklist entries which haven't triggered a call
 * in [AdvancedBlockingPolicy.blocklistAgingDays] days.
 *
 * Only runs when [AdvancedBlockingPolicy.blocklistAgingEnabled] is true.
 */
@HiltWorker
class BlocklistAgingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val blocklistDao: BlocklistDao,
    private val screeningPrefs: ScreeningPreferences,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val policy = screeningPrefs.getAdvancedBlockingPolicy()
        if (!policy.blocklistAgingEnabled) return Result.success()

        val cutoff = System.currentTimeMillis() - policy.blocklistAgingDays.toLong() * 24 * 60 * 60 * 1_000
        // Single bulk DELETE — replaces the N+1 fetch-and-delete loop.
        blocklistDao.deleteEntriesWithNoCallsSince(cutoff)
        return Result.success()
    }

    companion object {
        private const val WORK_TAG = "blocklist_aging"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()
            val request = PeriodicWorkRequestBuilder<BlocklistAgingWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_TAG)
        }
    }
}
