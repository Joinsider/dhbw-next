/*
 * SPDX-FileCopyrightText: 2024 Joinside <suitor-fall-life@duck.com>
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package de.joinside.dhbw.services.notifications

import de.joinside.dhbw.data.dualis.remote.services.DualisLectureService
import de.joinside.dhbw.data.storage.database.dao.timetable.LectureEventDao
import de.joinside.dhbw.data.storage.database.dao.timetable.LectureLecturerCrossRefDao
import de.joinside.dhbw.data.storage.database.entities.timetable.LectureEventEntity
import de.joinside.dhbw.data.storage.database.entities.timetable.LectureWithLecturers
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay

/**
 * Service for monitoring lecture changes and detecting differences
 * between current Dualis data and stored database data.
 *
 * Implements 5-second delayed refetch for cancellation confirmation.
 */
class LectureChangeMonitor(
    private val dualisLectureService: DualisLectureService,
    private val lectureEventDao: LectureEventDao,
    private val lectureLecturerCrossRefDao: LectureLecturerCrossRefDao
) {
    companion object {
        private const val TAG = "LectureChangeMonitor"
        private const val CANCELLATION_RECHECK_DELAY_MS = 5000L
    }

    /**
     * Check for lecture changes by comparing current Dualis data with database.
     * Detects time changes, lecturer changes, type changes, location changes, and cancellations.
     *
     * @return MonitorResult containing detected changes or errors
     */
    suspend fun checkForChanges(): MonitorResult {
        try {
            Napier.d("🔍 Starting lecture change check", tag = TAG)

            // Step 1: Fetch current lectures from Dualis
            Napier.d("📥 Step 1: Fetching current lectures from Dualis...", tag = TAG)
            val fetchResult = dualisLectureService.getWeeklyLecturesForCurrentWeek()
            if (fetchResult.isFailure) {
                val error = fetchResult.exceptionOrNull()
                Napier.e("❌ Failed to fetch current lectures: ${error?.message}", tag = TAG)
                return MonitorResult.Error(
                    "Failed to fetch lectures: ${error?.message}",
                    error
                )
            }

            val currentLectures = fetchResult.getOrNull() ?: emptyList()
            Napier.d("✅ Fetched ${currentLectures.size} current lectures from Dualis", tag = TAG)
            currentLectures.forEachIndexed { index, lecture ->
                Napier.d("   ${index + 1}. ${lecture.shortSubjectName} at ${lecture.startTime}", tag = TAG)
            }

            // Step 2: Get stored lectures from database
            Napier.d("💾 Step 2: Retrieving stored lectures from database...", tag = TAG)
            val storedLectures = lectureEventDao.getAllWithLecturers()
            Napier.d("✅ Retrieved ${storedLectures.size} stored lectures from database", tag = TAG)
            storedLectures.forEachIndexed { index, lectureWithLecturers ->
                Napier.d("   ${index + 1}. ${lectureWithLecturers.lecture.shortSubjectName} at ${lectureWithLecturers.lecture.startTime}", tag = TAG)
            }

            // Step 3: Detect changes
            Napier.d("🔍 Step 3: Detecting changes...", tag = TAG)
            val changes = mutableListOf<LectureChange>()

            // Create lookup maps for efficient comparison
            val currentLectureMap = currentLectures.associateBy { it.lectureId }
            val storedLectureMap = storedLectures.associateBy { it.lecture.lectureId }

            Napier.d("📊 Comparison maps created:", tag = TAG)
            Napier.d("   Current lectures: ${currentLectureMap.size} entries", tag = TAG)
            Napier.d("   Stored lectures: ${storedLectureMap.size} entries", tag = TAG)

            // Check for modifications and deletions (cancellations)
            Napier.d("🔄 Checking for modifications and cancellations...", tag = TAG)
            for (stored in storedLectures) {
                val current = currentLectureMap[stored.lecture.lectureId]

                if (current == null) {
                    // Potential cancellation - needs confirmation
                    Napier.w("⚠️  Potential cancellation detected for lecture ${stored.lecture.lectureId}: ${stored.lecture.shortSubjectName}", tag = TAG)
                    val confirmedCancellation = confirmCancellation(stored.lecture.lectureId)
                    if (confirmedCancellation) {
                        Napier.e("❌ CONFIRMED: Lecture cancelled - ${stored.lecture.shortSubjectName}", tag = TAG)
                        changes.add(
                            LectureChange.Cancellation(
                                lectureId = stored.lecture.lectureId,
                                courseName = stored.lecture.shortSubjectName,
                                cancelledLecture = stored.lecture,
                                confirmedAfterDelay = true
                            )
                        )
                    } else {
                        Napier.d("✅ False alarm: Lecture still exists after recheck", tag = TAG)
                    }
                } else {
                    // Compare existing lecture for changes
                    Napier.d("🔎 Comparing lecture ${stored.lecture.lectureId}: ${stored.lecture.shortSubjectName}", tag = TAG)
                    val lectureChanges = detectLectureChanges(stored, current)
                    if (lectureChanges.isNotEmpty()) {
                        Napier.d("   📝 Found ${lectureChanges.size} change(s) in this lecture", tag = TAG)
                        lectureChanges.forEach { change ->
                            Napier.d("      - ${change::class.simpleName}", tag = TAG)
                        }
                    } else {
                        Napier.d("   ✅ No changes detected", tag = TAG)
                    }
                    changes.addAll(lectureChanges)
                }
            }

            // Check for new lectures
            Napier.d("➕ Checking for new lectures...", tag = TAG)
            for (current in currentLectures) {
                if (!storedLectureMap.containsKey(current.lectureId)) {
                    Napier.d("🆕 New lecture detected: ${current.lectureId} - ${current.shortSubjectName}", tag = TAG)
                    changes.add(
                        LectureChange.NewLecture(
                            lectureId = current.lectureId,
                            courseName = current.shortSubjectName,
                            lecture = current
                        )
                    )
                }
            }

            Napier.d("📊 Change detection complete: ${changes.size} total change(s) found", tag = TAG)
            if (changes.isNotEmpty()) {
                Napier.d("📋 Summary of changes:", tag = TAG)
                changes.groupBy { it::class.simpleName }.forEach { (type, list) ->
                    Napier.d("   - $type: ${list.size}", tag = TAG)
                }
            }

            return if (changes.isEmpty()) {
                MonitorResult.NoChanges(currentLectures.size)
            } else {
                MonitorResult.Changes(changes)
            }

        } catch (e: Exception) {
            Napier.e("Exception during change check: ${e.message}", e, tag = TAG)
            return MonitorResult.Error("Exception: ${e.message}", e)
        }
    }

    /**
     * Detect specific changes between a stored lecture and current lecture.
     */
    private suspend fun detectLectureChanges(
        stored: LectureWithLecturers,
        current: LectureEventEntity
    ): List<LectureChange> {
        val changes = mutableListOf<LectureChange>()
        val storedLecture = stored.lecture

        // Check time changes
        if (storedLecture.startTime != current.startTime || storedLecture.endTime != current.endTime) {
            Napier.d("      ⏰ Time change: ${storedLecture.startTime} -> ${current.startTime}", tag = TAG)
            changes.add(
                LectureChange.TimeChange(
                    lectureId = storedLecture.lectureId,
                    courseName = storedLecture.shortSubjectName,
                    oldStartTime = storedLecture.startTime,
                    newStartTime = current.startTime,
                    oldEndTime = storedLecture.endTime,
                    newEndTime = current.endTime
                )
            )
        }

        // Check location changes
        if (storedLecture.location != current.location) {
            Napier.d("      📍 Location change: '${storedLecture.location}' -> '${current.location}'", tag = TAG)
            changes.add(
                LectureChange.LocationChange(
                    lectureId = storedLecture.lectureId,
                    courseName = storedLecture.shortSubjectName,
                    oldLocation = storedLecture.location,
                    newLocation = current.location
                )
            )
        }

        // Check type changes (lecture <-> test)
        if (storedLecture.isTest != current.isTest) {
            Napier.d("      📝 Type change: ${if (storedLecture.isTest) "Test" else "Lecture"} -> ${if (current.isTest) "Test" else "Lecture"}", tag = TAG)
            changes.add(
                LectureChange.TypeChange(
                    lectureId = storedLecture.lectureId,
                    courseName = storedLecture.shortSubjectName,
                    oldIsTest = storedLecture.isTest,
                    newIsTest = current.isTest
                )
            )
        }

        // Check lecturer changes
        val currentLecturers = getLecturersForLecture(current.lectureId)
        val oldLecturerNames = stored.lecturers.map { it.lecturerName }
        val newLecturerNames = currentLecturers.map { it.lecturerName }

        if (oldLecturerNames.sorted() != newLecturerNames.sorted()) {
            Napier.d("      👨‍🏫 Lecturer change:", tag = TAG)
            Napier.d("         Old: ${oldLecturerNames.joinToString(", ")}", tag = TAG)
            Napier.d("         New: ${newLecturerNames.joinToString(", ")}", tag = TAG)
            changes.add(
                LectureChange.LecturerChange(
                    lectureId = storedLecture.lectureId,
                    courseName = storedLecture.shortSubjectName,
                    oldLecturers = oldLecturerNames,
                    newLecturers = newLecturerNames
                )
            )
        }

        return changes
    }

    /**
     * Confirm a cancellation by waiting 5 seconds and refetching.
     * Returns true if the lecture is still missing after the delay.
     */
    private suspend fun confirmCancellation(lectureId: Long): Boolean {
        Napier.d("⏳ Confirming cancellation for lecture $lectureId (waiting ${CANCELLATION_RECHECK_DELAY_MS}ms)...", tag = TAG)
        delay(CANCELLATION_RECHECK_DELAY_MS)

        Napier.d("🔄 Re-fetching lectures to confirm cancellation...", tag = TAG)
        val recheckResult = dualisLectureService.getWeeklyLecturesForCurrentWeek()
        if (recheckResult.isFailure) {
            Napier.w("⚠️  Failed to recheck for cancellation confirmation - treating as NOT cancelled for safety", tag = TAG)
            // If recheck fails, we can't confirm - return false to be safe
            return false
        }

        val recheckLectures = recheckResult.getOrNull() ?: emptyList()
        val stillMissing = recheckLectures.none { it.lectureId == lectureId }

        if (stillMissing) {
            Napier.e("✅ Cancellation CONFIRMED: Lecture $lectureId still missing after recheck", tag = TAG)
        } else {
            Napier.d("❌ Cancellation NOT confirmed: Lecture $lectureId found in recheck", tag = TAG)
        }

        return stillMissing
    }

    /**
     * Get lecturers for a specific lecture by querying cross-reference table.
     * This is a helper to get current lecturer associations.
     */
    private suspend fun getLecturersForLecture(lectureId: Long): List<de.joinside.dhbw.data.storage.database.entities.timetable.LecturerEntity> {
        try {
            // Get cross-references for this lecture
            val crossRefs = lectureLecturerCrossRefDao.getByLectureId(lectureId)

            if (crossRefs.isEmpty()) {
                return emptyList()
            }

            // Fetch lecturer entities
            val lecturers = mutableListOf<de.joinside.dhbw.data.storage.database.entities.timetable.LecturerEntity>()
            for (crossRef in crossRefs) {
                val lecturer = lectureEventDao.getAllWithLecturers()
                    .firstOrNull { it.lecture.lectureId == lectureId }
                    ?.lecturers
                    ?.firstOrNull { it.lecturerId == crossRef.lecturerId }

                if (lecturer != null) {
                    lecturers.add(lecturer)
                }
            }

            return lecturers
        } catch (e: Exception) {
            Napier.w("Error fetching lecturers for lecture $lectureId: ${e.message}", tag = TAG)
            return emptyList()
        }
    }
}

