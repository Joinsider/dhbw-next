/*
 * SPDX-FileCopyrightText: 2024 Joinside <suitor-fall-life@duck.com>
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package de.joinside.dhbw.services.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.aakira.napier.Napier

/**
 * Android implementation of NotificationDispatcher using NotificationCompat.
 */
actual class NotificationDispatcher {

    companion object {
        private const val TAG = "NotificationDispatcher"
        private const val CHANNEL_ID = "lecture_changes"
        private const val CHANNEL_NAME = "Lecture Changes"
        private const val CHANNEL_DESCRIPTION = "Notifications for lecture time and content changes"
        private const val NOTIFICATION_ID_BASE = 10000

        @Volatile
        private var applicationContext: Context? = null

        /**
         * Initialize the NotificationDispatcher with application context.
         * This should be called from Application.onCreate() or MainActivity.onCreate()
         */
        fun initialize(context: Context) {
            applicationContext = context.applicationContext
        }

        private fun getContext(): Context {
            return applicationContext
                ?: throw IllegalStateException(
                    "NotificationDispatcher not initialized. Call NotificationDispatcher.initialize(context) first."
                )
        }
    }

    private val context: Context
        get() = getContext()

    init {
        createNotificationChannel()
    }

    /**
     * Create notification channel for Android O and above.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Napier.d("Notification channel created", tag = TAG)
        }
    }

    /**
     * Request notification permission from the user (Android 13+).
     */
    actual suspend fun requestPermission(): Boolean {
        // Permission request must be initiated from an Activity
        // This method returns current permission state
        // The actual permission request should be done in the UI layer
        return hasPermission()
    }

    /**
     * Check if notification permission is currently granted.
     */
    actual suspend fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Before Android 13, notifications don't require runtime permission
            true
        }
    }

    /**
     * Show a notification for a single lecture change.
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    actual suspend fun showNotification(title: String, message: String, lectureId: Long) {
        if (!hasPermission()) {
            Napier.w("Cannot show notification: permission not granted", tag = TAG)
            return
        }

        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: Use app icon
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            val notificationId = (NOTIFICATION_ID_BASE + lectureId % 1000).toInt()
            NotificationManagerCompat.from(context).notify(notificationId, notification)

            Napier.d("Notification shown for lecture $lectureId", tag = TAG)
        } catch (e: SecurityException) {
            Napier.e("SecurityException showing notification: ${e.message}", tag = TAG)
        }
    }

    /**
     * Show a summary notification for multiple lecture changes.
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    actual suspend fun showSummaryNotification(title: String, message: String, changeCount: Int) {
        if (!hasPermission()) {
            Napier.w("Cannot show notification: permission not granted", tag = TAG)
            return
        }

        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: Use app icon
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setNumber(changeCount)
                .build()

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BASE, notification)

            Napier.d("Summary notification shown for $changeCount changes", tag = TAG)
        } catch (e: SecurityException) {
            Napier.e("SecurityException showing notification: ${e.message}", tag = TAG)
        }
    }
}

