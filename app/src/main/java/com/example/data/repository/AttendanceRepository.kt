package com.example.data.repository

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.model.AttendanceSummary
import com.example.data.model.SubjectAttendance
import com.example.data.network.ErpConnectors
import com.example.data.network.FullAttendanceResult
import com.example.data.preferences.ThemeMode
import com.example.data.preferences.UserPreferences
import com.example.util.NotificationHelper
import com.example.widget.AttendanceWidget
import com.example.widget.CompactAttendanceWidget
import com.example.widget.MiniPurpleWidget
import com.example.worker.AttendanceSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class AttendanceRepository(private val context: Context) {

    private val prefs = UserPreferences(context)

    val usernameFlow: Flow<String?> = prefs.usernameFlow
    val studentNameFlow: Flow<String> = prefs.studentNameFlow
    val collegeNameFlow: Flow<String> = prefs.collegeNameFlow
    val batchFlow: Flow<String> = prefs.batchFlow
    val onboardingCompletedFlow: Flow<Boolean> = prefs.onboardingCompletedFlow
    val targetThresholdFlow: Flow<Double> = prefs.targetThresholdFlow
    val cachedAttendanceFlow: Flow<AttendanceSummary?> = prefs.cachedAttendanceFlow
    val cachedSubjectsFlow: Flow<List<SubjectAttendance>> = prefs.cachedSubjectsFlow
    val syncIntervalHoursFlow: Flow<Int> = prefs.syncIntervalHoursFlow
    val themeModeFlow: Flow<ThemeMode> = prefs.themeModeFlow
    val notificationsEnabledFlow: Flow<Boolean> = prefs.notificationsEnabledFlow

    suspend fun saveUserProfile(name: String, college: String, batch: String) {
        prefs.saveUserProfile(name, college, batch)
    }

    suspend fun setOnboardingCompleted(completed: Boolean = true) {
        prefs.setOnboardingCompleted(completed)
    }

    suspend fun getSavedCredentials(): Pair<String, String>? = prefs.getCredentials()

    suspend fun saveTargetThreshold(threshold: Double) {
        prefs.saveTargetThreshold(threshold)
        updateWidget()
    }

    suspend fun saveThemeMode(mode: ThemeMode) {
        prefs.saveThemeMode(mode)
    }

    suspend fun saveNotificationsEnabled(enabled: Boolean) {
        prefs.saveNotificationsEnabled(enabled)
    }

    suspend fun loginAndFetch(username: String, password: String): FullAttendanceResult =
        withContext(Dispatchers.IO) {
            val result = ErpConnectors.create().fetchFullAttendance(username, password)
            prefs.saveCredentials(username, password)
            prefs.saveCachedAttendance(result.summary)
            prefs.saveCachedSubjects(result.subjects)
            if (result.studentName.isNotBlank()) {
                prefs.saveStudentName(result.studentName)
            }
            schedulePeriodicSync(4)
            updateWidget()

            if (prefs.notificationsEnabledFlow.first()) {
                val threshold = prefs.targetThresholdFlow.first()
                NotificationHelper.checkAndNotifySubjectShortages(context, result.subjects, threshold)
            }

            result
        }

    suspend fun refreshAttendance(): FullAttendanceResult = withContext(Dispatchers.IO) {
        val creds = prefs.getCredentials()
            ?: throw IllegalStateException("Not logged in. Please sign in to fetch attendance.")
        val result = ErpConnectors.create().fetchFullAttendance(creds.first, creds.second)
        prefs.saveCachedAttendance(result.summary)
        prefs.saveCachedSubjects(result.subjects)
        if (result.studentName.isNotBlank()) {
            prefs.saveStudentName(result.studentName)
        }
        updateWidget()

        if (prefs.notificationsEnabledFlow.first()) {
            val threshold = prefs.targetThresholdFlow.first()
            NotificationHelper.checkAndNotifySubjectShortages(context, result.subjects, threshold)
        }

        result
    }

    suspend fun enterDirectMode(name: String, college: String, batch: String) {
        val emptySummary = AttendanceSummary(
            present = 0,
            absent = 0,
            marked = 0,
            notMarked = 0,
            totalWorking = 0,
            apiPercentage = 0,
            tillDate = "Direct Workspace"
        )
        prefs.saveCredentials(name.ifBlank { "STUDENT" }, "direct_access_pass")
        prefs.saveUserProfile(name, college, batch)
        prefs.saveCachedAttendance(emptySummary)
        prefs.saveCachedSubjects(emptyList())
        updateWidget()
    }

    suspend fun loadDemoData() {
        val currentName = prefs.studentNameFlow.first().ifBlank { "Student" }
        val currentCollege = prefs.collegeNameFlow.first().ifBlank { "Engineering College" }
        val currentBatch = prefs.batchFlow.first().ifBlank { "2024 - 2028" }
        enterDirectMode(currentName, currentCollege, currentBatch)
    }

    suspend fun logout() {
        prefs.clearAll()
        WorkManager.getInstance(context).cancelUniqueWork(AttendanceSyncWorker.WORK_NAME)
        updateWidget()
    }

    suspend fun updateWidget() {
        try {
            AttendanceWidget().updateAll(context)
        } catch (_: Exception) {
        }
        try {
            CompactAttendanceWidget().updateAll(context)
        } catch (_: Exception) {
        }
        try {
            MiniPurpleWidget().updateAll(context)
        } catch (_: Exception) {
        }
    }

    fun schedulePeriodicSync(intervalHours: Int = 4) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<AttendanceSyncWorker>(
            intervalHours.toLong().coerceAtLeast(1), TimeUnit.HOURS,
            15, TimeUnit.MINUTES
        ).setConstraints(constraints).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            AttendanceSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    /**
     * Widget update callbacks (resize, system refresh...) can fire often. Each sync is a full ERP login,
     * so only sync when the cached data is older than [minAgeMillis].
     */
    suspend fun syncIfStale(minAgeMillis: Long = 15 * 60 * 1000L) {
        val last = prefs.cachedAttendanceFlow.first()?.lastSyncedMillis ?: 0L
        if (System.currentTimeMillis() - last >= minAgeMillis) syncImmediately()
    }

    fun syncImmediately() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<AttendanceSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "acgcet_attendance_immediate_sync",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
