package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.R

/** One-time local reminder. WorkManager persists scheduled work across process death and reboot. */
class ExamReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE)?.takeIf { it.isNotBlank() } ?: return Result.failure()
        val subject = inputData.getString(KEY_SUBJECT).orEmpty()
        val examId = inputData.getString(KEY_EXAM_ID).orEmpty().hashCode()
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Exam reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Opt-in reminders for exams added in Clg 1%"
            })
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Upcoming exam: $title")
            .setContentText(if (subject.isBlank()) "Your exam is scheduled soon." else subject)
            .setStyle(NotificationCompat.BigTextStyle().bigText(if (subject.isBlank()) "Your exam is scheduled soon." else "Your $subject exam is scheduled soon."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        return try {
            NotificationManagerCompat.from(applicationContext).notify(examId, notification)
            Result.success()
        } catch (_: SecurityException) {
            // Permission may have been revoked after scheduling.
            Result.success()
        }
    }

    companion object {
        const val KEY_TITLE = "exam_title"
        const val KEY_SUBJECT = "exam_subject"
        const val KEY_EXAM_ID = "exam_id"
        private const val CHANNEL_ID = "clg1_exam_reminders"
    }
}
