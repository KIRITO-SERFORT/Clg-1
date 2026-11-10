package com.example.worker

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.MainActivity
import com.example.util.NotificationHelper
import java.util.concurrent.TimeUnit

class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val taskTitle = inputData.getString(KEY_TASK_TITLE) ?: return Result.failure()
        val priority = inputData.getString(KEY_TASK_PRIORITY) ?: "HIGH"
        val isAutoDeadline = inputData.getBoolean(KEY_IS_DEADLINE_WARNING, false)
        val taskId = inputData.getString(KEY_TASK_ID) ?: taskTitle

        NotificationHelper.createNotificationChannel(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return Result.success()
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val titleText = if (isAutoDeadline) {
            "⚠️ Urgent: $priority Priority Task Due Tomorrow!"
        } else {
            "⏰ Task Reminder ($priority Priority)"
        }
        val bodyText = if (isAutoDeadline) {
            "\"$taskTitle\" is closing in 24 hours. Tap to mark completed."
        } else {
            "Reminder for \"$taskTitle\". Tap to view details."
        }

        val notification = NotificationCompat.Builder(applicationContext, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(titleText)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(taskId.hashCode() + 5000, notification)

        return Result.success()
    }

    companion object {
        const val KEY_TASK_TITLE = "task_title"
        const val KEY_TASK_PRIORITY = "task_priority"
        const val KEY_IS_DEADLINE_WARNING = "is_deadline_warning"
        const val KEY_TASK_ID = "task_id"
    }
}

/**
 * Schedules a local user-defined custom reminder using WorkManager (works offline).
 */
fun scheduleCustomReminder(
    context: Context,
    taskId: String,
    taskTitle: String,
    delayInMinutes: Long,
    priority: String = "HIGH"
) {
    val safeDelay = delayInMinutes.coerceAtLeast(1L)
    val reminderData = workDataOf(
        ReminderWorker.KEY_TASK_ID to taskId,
        ReminderWorker.KEY_TASK_TITLE to taskTitle,
        ReminderWorker.KEY_TASK_PRIORITY to priority,
        ReminderWorker.KEY_IS_DEADLINE_WARNING to false
    )

    val reminderRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
        .setInitialDelay(safeDelay, TimeUnit.MINUTES)
        .setInputData(reminderData)
        .build()

    WorkManager.getInstance(context).enqueueUniqueWork(
        "custom_reminder_$taskId",
        ExistingWorkPolicy.REPLACE,
        reminderRequest
    )
}

/**
 * Automatically schedules a local 1-day (24 hours) before deadline warning if the task is not completed.
 */
fun scheduleAutoDeadlineWarning(
    context: Context,
    taskId: String,
    taskTitle: String,
    deadlineMillis: Long,
    priority: String = "HIGH"
) {
    val oneDayBeforeMillis = deadlineMillis - TimeUnit.HOURS.toMillis(24)
    val delayMillis = oneDayBeforeMillis - System.currentTimeMillis()
    if (delayMillis <= 0L) return

    val reminderData = workDataOf(
        ReminderWorker.KEY_TASK_ID to taskId,
        ReminderWorker.KEY_TASK_TITLE to taskTitle,
        ReminderWorker.KEY_TASK_PRIORITY to priority,
        ReminderWorker.KEY_IS_DEADLINE_WARNING to true
    )

    val warningRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
        .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
        .setInputData(reminderData)
        .build()

    WorkManager.getInstance(context).enqueueUniqueWork(
        "auto_deadline_1d_$taskId",
        ExistingWorkPolicy.KEEP,
        warningRequest
    )
}
