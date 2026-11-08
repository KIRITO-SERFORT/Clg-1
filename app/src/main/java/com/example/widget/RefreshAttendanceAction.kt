package com.example.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.example.data.repository.AttendanceRepository

class RefreshAttendanceAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        try {
            val repository = AttendanceRepository(context)
            repository.refreshAttendance()
        } catch (_: Exception) {
            // Error handling handled by widget state
        }
    }
}
