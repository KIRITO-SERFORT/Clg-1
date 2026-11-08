package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt
import com.example.MainActivity
import com.example.R
import com.example.data.model.SubjectAttendance

object NotificationHelper {

    const val CHANNEL_ID = "acgcet_attendance_warnings"
    private const val NOTIFICATION_ID = 2001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Attendance shortage alerts"
            val descriptionText = "Alerts when subject attendance drops below your target attendance."
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun checkAndNotifySubjectShortages(
        context: Context,
        subjects: List<SubjectAttendance>,
        threshold: Double = 0.75
    ) {
        createNotificationChannel(context)

        val shortages = subjects.filter { it.isBelowExamThreshold(threshold) }
        if (shortages.isEmpty()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pct = (threshold * 100).roundToInt()
        val title = if (shortages.size == 1) {
            "⚠️ Attendance warning: ${shortages.first().code} is below $pct%!"
        } else {
            "⚠️ Attendance warning: ${shortages.size} subjects below $pct%!"
        }

        val detailsBuilder = StringBuilder()
        shortages.forEach { subj ->
            val recover = subj.hoursToRecover(threshold)
            detailsBuilder.append("• ${subj.title}: ${String.format("%.1f%%", subj.percentage)} (Need $recover consecutive hours)\n")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText("Your target is at least $pct% attendance in every subject.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("These subjects are below your $pct% target:\n\n${detailsBuilder.toString().trimEnd()}")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
