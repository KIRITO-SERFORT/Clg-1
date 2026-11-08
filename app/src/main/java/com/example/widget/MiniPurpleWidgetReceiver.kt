package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.example.data.repository.AttendanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MiniPurpleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MiniPurpleWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = AttendanceRepository(context)
                if (repository.getSavedCredentials() != null) {
                    repository.schedulePeriodicSync(4)
                    repository.syncImmediately()
                }
            } catch (_: Exception) {
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = AttendanceRepository(context)
                if (repository.getSavedCredentials() != null) {
                    repository.syncIfStale()
                }
            } catch (_: Exception) {
            }
        }
    }
}
