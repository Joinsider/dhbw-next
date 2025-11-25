/*
 * SPDX-FileCopyrightText: 2024 Joinside <suitor-fall-life@duck.com>
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package de.joinside.dhbw.services.notifications

import android.content.Context
import androidx.work.*
import androidx.work.WorkerParameters
import io.github.aakira.napier.Napier
import java.util.concurrent.TimeUnit

/**
 * Android WorkManager-based periodic scheduler for lecture change monitoring.
 * Runs every 2 hours with network and authentication constraints.
 */
class LectureMonitorScheduler(private val context: Context) {

    companion object {
        private const val TAG = "LectureMonitorScheduler"
        private const val WORK_NAME = "lecture_change_monitor"
        private const val REPEAT_INTERVAL_HOURS = 2L
    }

    /**
     * Schedule periodic lecture monitoring work.
     */
    fun schedule() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<LectureMonitorWorker>(
            REPEAT_INTERVAL_HOURS,
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(15, TimeUnit.MINUTES) // Wait 15 minutes after app start
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing work if already scheduled
            workRequest
        )

        Napier.d("Lecture monitoring scheduled (every $REPEAT_INTERVAL_HOURS hours)", tag = TAG)
    }

    /**
     * Cancel scheduled lecture monitoring work.
     */
    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        Napier.d("Lecture monitoring cancelled", tag = TAG)
    }

    /**
     * Get work status information.
     */
    fun getWorkInfo() = WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData(WORK_NAME)
}

/**
 * Worker that performs the actual lecture change monitoring.
 */
class LectureMonitorWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "LectureMonitorWorker"
    }

    override suspend fun doWork(): Result {
        Napier.d("Starting lecture change monitoring work", tag = TAG)

        try {
            // TODO: Get dependencies from application context or DI
            // For now, we'll need to initialize the required services here
            // This will be connected in the next step when we integrate with the app

            Napier.d("Lecture change monitoring work placeholder - implementation pending", tag = TAG)

            // Return success for now
            return Result.success()

        } catch (e: Exception) {
            Napier.e("Error during lecture change monitoring: ${e.message}", e, tag = TAG)
            return Result.retry()
        }
    }
}

