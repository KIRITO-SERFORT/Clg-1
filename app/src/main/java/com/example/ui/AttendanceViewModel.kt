package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AttendanceSummary
import com.example.data.model.SubjectAttendance
import com.example.data.network.ErpException
import com.example.data.preferences.ThemeMode
import com.example.data.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException

sealed class AttendanceUiState {
    object Initializing : AttendanceUiState()
    object LoggedOut : AttendanceUiState()
    data class LoggedIn(
        val username: String,
        val studentName: String,
        val summary: AttendanceSummary,
        val subjects: List<SubjectAttendance>,
        val targetThreshold: Double,
        val syncIntervalHours: Int,
        val themeMode: ThemeMode,
        val notificationsEnabled: Boolean,
        val isRefreshing: Boolean = false
    ) : AttendanceUiState()
}

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AttendanceRepository(application)

    private val _uiState = MutableStateFlow<AttendanceUiState>(AttendanceUiState.Initializing)
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val themeModeFlow = repository.themeModeFlow

    init {
        checkSavedSession()
    }

    private fun checkSavedSession() {
        viewModelScope.launch {
            val creds = repository.getSavedCredentials()
            val cached = repository.cachedAttendanceFlow.first()
            val subjects = repository.cachedSubjectsFlow.first()
            val studentName = repository.studentNameFlow.first()
            val threshold = repository.targetThresholdFlow.first()
            val syncInterval = repository.syncIntervalHoursFlow.first()
            val themeMode = repository.themeModeFlow.first()
            val notificationsEnabled = repository.notificationsEnabledFlow.first()

            if (creds != null && cached != null) {
                _uiState.value = AttendanceUiState.LoggedIn(
                    username = creds.first,
                    studentName = studentName,
                    summary = cached,
                    subjects = subjects,
                    targetThreshold = threshold,
                    syncIntervalHours = syncInterval,
                    themeMode = themeMode,
                    notificationsEnabled = notificationsEnabled
                )
                refresh(isBackground = true)
            } else if (creds != null) {
                refresh(isBackground = false)
            } else {
                _uiState.value = AttendanceUiState.LoggedOut
            }
        }
    }

    fun login(user: String, pass: String) {
        val cleanUser = user.trim()
        if (cleanUser.isEmpty() || pass.isEmpty()) {
            viewModelScope.launch {
                _errorMessage.emit("Please enter both Register Number and Password.")
            }
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.loginAndFetch(cleanUser, pass)
                val threshold = repository.targetThresholdFlow.first()
                val syncInterval = repository.syncIntervalHoursFlow.first()
                val themeMode = repository.themeModeFlow.first()
                val notificationsEnabled = repository.notificationsEnabledFlow.first()

                _uiState.value = AttendanceUiState.LoggedIn(
                    username = cleanUser,
                    studentName = result.studentName,
                    summary = result.summary,
                    subjects = result.subjects,
                    targetThreshold = threshold,
                    syncIntervalHours = syncInterval,
                    themeMode = themeMode,
                    notificationsEnabled = notificationsEnabled
                )
            } catch (e: ErpException.BadCredentials) {
                _errorMessage.emit("Login failed: Incorrect register number or password.")
            } catch (e: ErpException.PortalChanged) {
                _errorMessage.emit("Portal notice: ${e.message}")
            } catch (e: IOException) {
                _errorMessage.emit("Network error: Please check your internet connection.")
            } catch (e: Exception) {
                _errorMessage.emit("Error: ${e.message ?: "Failed to log in"}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refresh(isBackground: Boolean = false) {
        val currentState = _uiState.value
        if (!isBackground) {
            _isLoading.value = true
        }

        if (currentState is AttendanceUiState.LoggedIn) {
            _uiState.value = currentState.copy(isRefreshing = true)
        }

        viewModelScope.launch {
            try {
                val creds = repository.getSavedCredentials()
                if (creds == null) {
                    _uiState.value = AttendanceUiState.LoggedOut
                    return@launch
                }
                val result = repository.refreshAttendance()
                val threshold = repository.targetThresholdFlow.first()
                val syncInterval = repository.syncIntervalHoursFlow.first()
                val themeMode = repository.themeModeFlow.first()
                val notificationsEnabled = repository.notificationsEnabledFlow.first()

                _uiState.value = AttendanceUiState.LoggedIn(
                    username = creds.first,
                    studentName = result.studentName,
                    summary = result.summary,
                    subjects = result.subjects,
                    targetThreshold = threshold,
                    syncIntervalHours = syncInterval,
                    themeMode = themeMode,
                    notificationsEnabled = notificationsEnabled,
                    isRefreshing = false
                )
            } catch (e: Exception) {
                if (!isBackground) {
                    val msg = when (e) {
                        is ErpException.BadCredentials -> "Session expired. Please sign in again."
                        is IOException -> "Network error: Unable to reach the college portal."
                        else -> e.message ?: "Failed to refresh attendance"
                    }
                    _errorMessage.emit(msg)
                }
                if (currentState is AttendanceUiState.LoggedIn) {
                    _uiState.value = currentState.copy(isRefreshing = false)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setTargetThreshold(threshold: Double) {
        viewModelScope.launch {
            repository.saveTargetThreshold(threshold)
            val currentState = _uiState.value
            if (currentState is AttendanceUiState.LoggedIn) {
                _uiState.value = currentState.copy(targetThreshold = threshold)
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.saveThemeMode(mode)
            val currentState = _uiState.value
            if (currentState is AttendanceUiState.LoggedIn) {
                _uiState.value = currentState.copy(themeMode = mode)
            }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveNotificationsEnabled(enabled)
            val currentState = _uiState.value
            if (currentState is AttendanceUiState.LoggedIn) {
                _uiState.value = currentState.copy(notificationsEnabled = enabled)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.value = AttendanceUiState.LoggedOut
        }
    }
}
