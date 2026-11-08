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
    val targetThresholdFlow: Flow<Double> = prefs.targetThresholdFlow
    val cachedAttendanceFlow: Flow<AttendanceSummary?> = prefs.cachedAttendanceFlow
    val cachedSubjectsFlow: Flow<List<SubjectAttendance>> = prefs.cachedSubjectsFlow
    val syncIntervalHoursFlow: Flow<Int> = prefs.syncIntervalHoursFlow
    val themeModeFlow: Flow<ThemeMode> = prefs.themeModeFlow
    val notificationsEnabledFlow: Flow<Boolean> = prefs.notificationsEnabledFlow

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

    suspend fun loadDemoData() {
        val sampleSummary = AttendanceSummary(
            present = 342,
            absent = 58,
            marked = 400,
            notMarked = 10,
            totalWorking = 410,
            apiPercentage = 86,
            tillDate = "Today (Demo Mode)"
        )
        val sampleSubjects = listOf(
            SubjectAttendance("CS8501", "Theory of Computation", "Dr. Raman", 50, 44, 6, 0, 50, 88.0),
            SubjectAttendance("CS8591", "Computer Networks", "Prof. Priya", 50, 42, 8, 0, 50, 84.0),
            SubjectAttendance("CS8592", "Object Oriented Analysis & Design", "Dr. Karthik", 50, 45, 5, 0, 50, 90.0),
            SubjectAttendance("EC8691", "Microprocessors & Microcontrollers", "Prof. Selvan", 50, 39, 11, 0, 50, 78.0),
            SubjectAttendance("CS8581", "Networks Laboratory", "Dr. Meena", 40, 38, 2, 0, 40, 95.0),
            SubjectAttendance("MA8551", "Algebra & Number Theory", "Prof. Anitha", 50, 41, 9, 0, 50, 82.0)
        )
        prefs.saveCredentials("DEMO-7100", "demo_mode_pass")
        prefs.saveCachedAttendance(sampleSummary)
        prefs.saveCachedSubjects(sampleSubjects)
        prefs.saveStudentName("Autonomous Student (Demo)")
        updateWidget()
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
