package com.example.worker

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.network.ErpConnectors
import androidx.work.WorkManager
import com.example.data.network.ErpException
import com.example.data.preferences.UserPreferences
import com.example.util.NotificationHelper
import com.example.widget.AttendanceWidget
import com.example.widget.CompactAttendanceWidget
import com.example.widget.MiniPurpleWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class AttendanceSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "acgcet_attendance_periodic_sync"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = UserPreferences(applicationContext)
        val creds = prefs.getCredentials() ?: return@withContext Result.success()
        if (creds.second == "direct_access_pass") return@withContext Result.success()

        try {
            val result = ErpConnectors.create().fetchFullAttendance(creds.first, creds.second)
            prefs.saveCachedAttendance(result.summary)
            if (result.subjects.isNotEmpty()) prefs.saveCachedSubjects(result.subjects)
            if (result.studentName.isNotBlank()) {
                prefs.saveStudentName(result.studentName)
            }

            try {
                AttendanceWidget().updateAll(applicationContext)
            } catch (_: Exception) {
            }
            try {
                CompactAttendanceWidget().updateAll(applicationContext)
            } catch (_: Exception) {
            }
            try {
                MiniPurpleWidget().updateAll(applicationContext)
            } catch (_: Exception) {
            }

            if (prefs.notificationsEnabledFlow.first()) {
                val threshold = prefs.targetThresholdFlow.first()
                NotificationHelper.checkAndNotifySubjectShortages(applicationContext, result.subjects, threshold)
            }

            Result.success()
        } catch (e: ErpException.BadCredentials) {
            // A periodic job keeps running after failure(), and repeated wrong logins can lock the
            // student's ERP account. Stop the schedule; the app asks the user to sign in again.
            WorkManager.getInstance(applicationContext).cancelUniqueWork(WORK_NAME)
            Result.failure()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
