/*
 * SPDX-FileCopyrightText: 2024 Joinside <suitor-fall-life@duck.com>
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package de.joinside.dhbw.data.storage.preferences

import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Interactor for notification preferences providing Flow-based observation
 * and suspend functions for UI and background schedulers.
 */
class NotificationPreferencesInteractor(
    private val preferences: NotificationPreferences
) {
    private val _notificationsEnabled = MutableStateFlow(preferences.getNotificationsEnabled())
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _lectureAlertsEnabled = MutableStateFlow(preferences.getLectureAlertsEnabled())
    val lectureAlertsEnabled: StateFlow<Boolean> = _lectureAlertsEnabled.asStateFlow()

    init {
        Napier.d("NotificationPreferencesInteractor initialized: notifications=${_notificationsEnabled.value}, lectureAlerts=${_lectureAlertsEnabled.value}")
    }

    /**
     * Get the master notifications enabled preference
     */
    fun getNotificationsEnabled(): Boolean {
        return _notificationsEnabled.value
    }

    /**
     * Set the master notifications enabled preference and update flow
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        Napier.d("Preference change: setNotificationsEnabled -> $enabled")
        preferences.setNotificationsEnabled(enabled)
        _notificationsEnabled.value = enabled
    }

    /**
     * Get the lecture alerts enabled preference
     */
    fun getLectureAlertsEnabled(): Boolean {
        return _lectureAlertsEnabled.value
    }

    /**
     * Set the lecture alerts enabled preference and update flow
     */
    fun setLectureAlertsEnabled(enabled: Boolean) {
        Napier.d("Preference change: setLectureAlertsEnabled -> $enabled")
        preferences.setLectureAlertsEnabled(enabled)
        _lectureAlertsEnabled.value = enabled
    }

    /**
     * Check if lecture alerts should be processed (both master and lecture alerts enabled)
     */
    suspend fun shouldProcessLectureAlerts(): Boolean {
        val should = getNotificationsEnabled() && getLectureAlertsEnabled()
        Napier.d("shouldProcessLectureAlerts -> $should (notifications=${getNotificationsEnabled()}, lectureAlerts=${getLectureAlertsEnabled()})")
        return should
    }

    /**
     * Refresh the state flows from stored preferences
     * Useful after app restart or external preference changes
     */
    fun refresh() {
        val oldNotifications = _notificationsEnabled.value
        val oldLectureAlerts = _lectureAlertsEnabled.value

        _notificationsEnabled.value = preferences.getNotificationsEnabled()
        _lectureAlertsEnabled.value = preferences.getLectureAlertsEnabled()

        Napier.d("Preferences refreshed: notifications ${oldNotifications} -> ${_notificationsEnabled.value}, lectureAlerts ${oldLectureAlerts} -> ${_lectureAlertsEnabled.value}")
    }
}
