/*
 * SPDX-FileCopyrightText: 2024 Joinside <suitor-fall-life@duck.com>
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package de.joinside.dhbw.services.notifications

import io.github.aakira.napier.Napier
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.hours

/**
 * macOS coroutine-based periodic scheduler for lecture change monitoring.
 * Runs every 2 hours using coroutine delay in a background scope.
 */
class LectureMonitorScheduler(private val scope: CoroutineScope) {

    companion object {
        private const val TAG = "LectureMonitorScheduler"
        private val REPEAT_INTERVAL = 2.hours
    }

    private var monitorJob: Job? = null

    /**
     * Start periodic lecture monitoring.
     */
    fun schedule() {
        if (monitorJob?.isActive == true) {
            Napier.d("Lecture monitoring already running", tag = TAG)
            return
        }

        monitorJob = scope.launch {
            Napier.d("Starting lecture monitoring (every $REPEAT_INTERVAL)", tag = TAG)

            while (isActive) {
                try {
                    // Check if NotificationManager is initialized
                    if (NotificationServiceLocator.isInitialized()) {
                        val notificationManager = NotificationServiceLocator.getNotificationManager()
                        Napier.d("Performing periodic lecture change check", tag = TAG)
                        notificationManager.checkAndNotify()
                    } else {
                        Napier.w("NotificationManager not initialized, skipping check", tag = TAG)
                    }

                } catch (e: CancellationException) {
                    throw e // Re-throw to stop the loop
                } catch (e: Exception) {
                    Napier.e("Error during lecture monitoring: ${e.message}", e, tag = TAG)
                }

                delay(REPEAT_INTERVAL)
            }
        }

        Napier.d("Lecture monitoring scheduled", tag = TAG)
    }

    /**
     * Cancel scheduled lecture monitoring.
     */
    fun cancel() {
        monitorJob?.cancel()
        monitorJob = null
        Napier.d("Lecture monitoring cancelled", tag = TAG)
    }

    /**
     * Check if monitoring is currently active.
     */
    fun isScheduled(): Boolean = monitorJob?.isActive == true
}

