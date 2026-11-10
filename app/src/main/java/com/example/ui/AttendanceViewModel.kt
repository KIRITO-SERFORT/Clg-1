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
    data class Onboarding(
        val initialName: String = "",
        val initialCollege: String = "",
        val initialBatch: String = ""
    ) : AttendanceUiState()
    object LoggedOut : AttendanceUiState()
    data class LoggedIn(
        val username: String,
        val studentName: String,
        val collegeName: String = "",
        val batch: String = "",
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
    private val firestoreRepo = com.example.data.repository.FirestoreUserRepository(application)

    private val _uiState = MutableStateFlow<AttendanceUiState>(AttendanceUiState.Initializing)
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _absentDays = MutableStateFlow<List<com.example.data.model.AbsentDayRecord>>(emptyList())
    val absentDays: StateFlow<List<com.example.data.model.AbsentDayRecord>> = _absentDays.asStateFlow()

    private val _isLoadingAbsentDays = MutableStateFlow(false)
    val isLoadingAbsentDays: StateFlow<Boolean> = _isLoadingAbsentDays.asStateFlow()

    val themeModeFlow = repository.themeModeFlow

    init {
        checkSavedSession()
    }

    private fun checkSavedSession() {
        viewModelScope.launch {
            try {
                val creds = repository.getSavedCredentials()
                val cached = repository.cachedAttendanceFlow.first()
                val subjects = repository.cachedSubjectsFlow.first()
                val studentName = repository.studentNameFlow.first()
                val collegeName = repository.collegeNameFlow.first()
                val batch = repository.batchFlow.first()
                val onboardingCompleted = repository.onboardingCompletedFlow.first()
                val threshold = repository.targetThresholdFlow.first()
                val syncInterval = repository.syncIntervalHoursFlow.first()
                val themeMode = repository.themeModeFlow.first()
                val notificationsEnabled = repository.notificationsEnabledFlow.first()

                if (!onboardingCompleted && creds == null) {
                    _uiState.value = AttendanceUiState.Onboarding(studentName, collegeName, batch)
                    return@launch
                }

                if (creds != null && cached != null) {
                    _uiState.value = AttendanceUiState.LoggedIn(
                        username = creds.first,
                        studentName = studentName.ifBlank { creds.first },
                        collegeName = collegeName,
                        batch = batch,
                        summary = cached,
                        subjects = subjects,
                        targetThreshold = threshold,
                        syncIntervalHours = syncInterval,
                        themeMode = themeMode,
                        notificationsEnabled = notificationsEnabled
                    )
                    if (creds.first != "STUDENT" && creds.second != "direct_access_pass") {
                        refresh(isBackground = true)
                    }
                } else if (creds != null && creds.second != "direct_access_pass") {
                    refresh(isBackground = false)
                } else if (!onboardingCompleted) {
                    _uiState.value = AttendanceUiState.Onboarding(studentName, collegeName, batch)
                } else {
                    _uiState.value = AttendanceUiState.LoggedOut
                }
            } catch (_: Throwable) {
                _uiState.value = AttendanceUiState.Onboarding()
            }
        }
    }

    fun completeOnboarding(name: String, college: String, batch: String, connectErp: Boolean) {
        viewModelScope.launch {
            repository.saveUserProfile(name, college, batch)
            trySyncToFirestore(name, college, batch)
            if (connectErp) {
                _uiState.value = AttendanceUiState.LoggedOut
            } else {
                enterDirectWorkspace(name, college, batch)
            }
        }
    }

    fun enterDirectWorkspace(name: String, college: String, batch: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.enterDirectMode(name, college, batch)
                trySyncToFirestore(name, college, batch)
                val threshold = repository.targetThresholdFlow.first()
                val syncInterval = repository.syncIntervalHoursFlow.first()
                val themeMode = repository.themeModeFlow.first()
                val notificationsEnabled = repository.notificationsEnabledFlow.first()

                _uiState.value = AttendanceUiState.LoggedIn(
                    username = name.ifBlank { "STUDENT" },
                    studentName = name.ifBlank { "Student" },
                    collegeName = college.ifBlank { "College of Engineering" },
                    batch = batch.ifBlank { "2024 - 2028" },
                    summary = AttendanceSummary(0, 0, 0, 0, 0, 0, "Direct Workspace"),
                    subjects = emptyList(),
                    targetThreshold = threshold,
                    syncIntervalHours = syncInterval,
                    themeMode = themeMode,
                    notificationsEnabled = notificationsEnabled
                )
            } catch (e: Exception) {
                _errorMessage.emit("Setup error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun enterDemoMode() {
        viewModelScope.launch {
            val studentName = repository.studentNameFlow.first().ifBlank { "Student" }
            val collegeName = repository.collegeNameFlow.first().ifBlank { "College of Engineering" }
            val batch = repository.batchFlow.first().ifBlank { "2024 - 2028" }
            enterDirectWorkspace(studentName, collegeName, batch)
        }
    }

    private fun trySyncToFirestore(name: String, college: String, batch: String) {
        viewModelScope.launch {
            if (firestoreRepo.currentUserId() != null) {
                try {
                    firestoreRepo.saveProfile(name, college, batch)
                } catch (e: Exception) {
                    android.util.Log.w("AttendanceVM", "Cloud profile sync deferred: ${e.message}")
                }
            }
        }
    }

    fun saveProfileLocally(name: String, college: String, batch: String) {
        viewModelScope.launch {
            repository.saveUserProfile(name, college, batch)
        }
    }

    fun handleGoogleAuthSuccess() {
        viewModelScope.launch {
            try {
                val remoteProfile = firestoreRepo.getProfile()
                if (remoteProfile != null && remoteProfile.name.isNotBlank()) {
                    repository.saveUserProfile(remoteProfile.name, remoteProfile.collegeName, remoteProfile.batch)
                    repository.setOnboardingCompleted(true)
                    val creds = repository.getSavedCredentials()
                    if (creds == null) {
                        enterDirectWorkspace(remoteProfile.name, remoteProfile.collegeName, remoteProfile.batch)
                    } else {
                        checkSavedSession()
                    }
                } else {
                    val localName = repository.studentNameFlow.first()
                    val localCollege = repository.collegeNameFlow.first()
                    val localBatch = repository.batchFlow.first()
                    if (localName.isNotBlank() && localCollege.isNotBlank()) {
                        firestoreRepo.saveProfile(localName, localCollege, localBatch)
                        repository.setOnboardingCompleted(true)
                        val creds = repository.getSavedCredentials()
                        if (creds == null) {
                            enterDirectWorkspace(localName, localCollege, localBatch)
                        } else {
                            checkSavedSession()
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("AttendanceVM", "Google auth sync handling: ${e.message}")
            }
        }
    }

    fun loadAbsentDays() {
        viewModelScope.launch {
            _isLoadingAbsentDays.value = true
            try {
                _absentDays.value = repository.fetchAbsentDays()
            } catch (e: Exception) {
                _errorMessage.emit("Could not load absent details: ${e.message}")
            } finally {
                _isLoadingAbsentDays.value = false
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
                val collegeName = repository.collegeNameFlow.first()
                val batch = repository.batchFlow.first()
                val threshold = repository.targetThresholdFlow.first()
                val syncInterval = repository.syncIntervalHoursFlow.first()
                val themeMode = repository.themeModeFlow.first()
                val notificationsEnabled = repository.notificationsEnabledFlow.first()

                _uiState.value = AttendanceUiState.LoggedIn(
                    username = cleanUser,
                    studentName = result.studentName,
                    collegeName = collegeName,
                    batch = batch,
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
                val collegeName = (currentState as? AttendanceUiState.LoggedIn)?.collegeName
                    ?: repository.collegeNameFlow.first()
                val batch = (currentState as? AttendanceUiState.LoggedIn)?.batch
                    ?: repository.batchFlow.first()
                val threshold = repository.targetThresholdFlow.first()
                val syncInterval = repository.syncIntervalHoursFlow.first()
                val themeMode = repository.themeModeFlow.first()
                val notificationsEnabled = repository.notificationsEnabledFlow.first()

                _uiState.value = AttendanceUiState.LoggedIn(
                    username = creds.first,
                    studentName = result.studentName,
                    collegeName = collegeName,
                    batch = batch,
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
