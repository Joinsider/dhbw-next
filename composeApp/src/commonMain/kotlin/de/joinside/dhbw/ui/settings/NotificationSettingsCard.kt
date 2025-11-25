/*
 * SPDX-FileCopyrightText: 2024 Joinside <suitor-fall-life@duck.com>
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package de.joinside.dhbw.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsCard(
    notificationsEnabled: Boolean = false,
    onNotificationsEnabledChange: (Boolean) -> Unit = {},
    lectureAlertsEnabled: Boolean = false,
    onLectureAlertsEnabledChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // Platform permission helpers
    val currentPermission = checkNotificationPermission()
    var hasPermission by remember { mutableStateOf(currentPermission) }
    LaunchedEffect(currentPermission) {
        hasPermission = currentPermission
    }
    val requestPermission = rememberNotificationPermissionRequest { granted ->
        hasPermission = granted
        if (granted && !notificationsEnabled) {
            // Auto-enable master toggle when user grants permission
            onNotificationsEnabledChange(true)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.elevatedCardElevation()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Card Title
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Master Notifications Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (notificationsEnabled) Icons.Default.Notifications
                                         else Icons.Default.NotificationsOff,
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 8.dp)
                        )
                        Text(
                            text = "Enable Notifications",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "Allow the app to send notifications",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 32.dp)
                    )
                }

                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { enabled ->
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ToggleOn)
                        if (enabled && !hasPermission) {
                            // Trigger platform permission flow; callback will update state
                            requestPermission()
                        } else {
                            onNotificationsEnabledChange(enabled)
                        }
                    },
                    modifier = Modifier.testTag("notificationsEnabledSwitch")
                )
            }

            // Permission Status
            if (!hasPermission && notificationsEnabled) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "⚠️ Notification permission denied. Please enable it in system settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Lecture Alerts Toggle (visible only when notifications enabled and permission granted)
            AnimatedVisibility(
                visible = notificationsEnabled && hasPermission
            ) {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Lecture Change Alerts",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Get notified about lecture time, location, and cancellation changes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = lectureAlertsEnabled,
                            onCheckedChange = { enabled ->
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.ToggleOn)
                                coroutineScope.launch {
                                    onLectureAlertsEnabledChange(enabled)
                                }
                            },
                            modifier = Modifier.testTag("lectureAlertsEnabledSwitch")
                        )
                    }
                }
            }
        }
    }
}
