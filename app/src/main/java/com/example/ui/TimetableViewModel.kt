package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Timetable
import com.example.data.network.ErpConnectors
import com.example.data.preferences.UserPreferences
import com.example.util.DayOrderAnchor
import com.example.util.DayOrderCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Self-contained state for the timetable card: cached timetable, day-order anchor,
 * and a weekly refresh. It reads/writes only through UserPreferences.
 */
class TimetableViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = UserPreferences(application)

    val timetable: StateFlow<Timetable?> =
        prefs.cachedTimetableFlow.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val anchor: StateFlow<DayOrderAnchor?> =
        prefs.dayOrderAnchorFlow.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var lastAttemptMillis = 0L

    /** Called whenever the card appears. Fetches only if there is no timetable or it is older than a week. */
    fun ensureFresh() {
        viewModelScope.launch {
            val cached = prefs.cachedTimetableFlow.first()
            val stale = cached == null ||
                System.currentTimeMillis() - cached.fetchedAtMillis > WEEK_MILLIS
            val coolDownOver = System.currentTimeMillis() - lastAttemptMillis > COOL_DOWN_MILLIS
            if (stale && coolDownOver) fetchAndSave()
        }
    }

    /** Manual refresh button. */
    fun refresh() {
        viewModelScope.launch { fetchAndSave() }
    }

    fun setTodayDayOrder(dayOrder: Int) {
        viewModelScope.launch {
            prefs.saveDayOrderAnchor(DayOrderCalculator.anchorForToday(dayOrder))
        }
    }

    private suspend fun fetchAndSave() {
        if (_isRefreshing.value) return
        val creds = prefs.getCredentials() ?: return
        lastAttemptMillis = System.currentTimeMillis()
        _isRefreshing.value = true
        _error.value = null
        try {
            val fresh = withContext(Dispatchers.IO) {
                ErpConnectors.create().fetchTimetable(creds.first, creds.second)
            }
            prefs.saveCachedTimetable(fresh)
        } catch (e: Exception) {
            _error.value = "Couldn't update the timetable. Showing the saved one."
        } finally {
            _isRefreshing.value = false
        }
    }

    private companion object {
        const val WEEK_MILLIS = 7L * 24 * 60 * 60 * 1000
        const val COOL_DOWN_MILLIS = 10L * 60 * 1000
    }
}
