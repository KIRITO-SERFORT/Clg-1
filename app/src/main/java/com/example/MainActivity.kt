package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.preferences.ThemeMode
import com.example.ui.AttendanceUiState
import com.example.ui.AttendanceViewModel
import com.example.ui.kit.ClgTheme
import com.example.ui.kit.LoadingScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.OnboardingScreen
import com.example.util.configureAppCheck
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure App Check before any UI or Firebase operations
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            val viewModel: AttendanceViewModel = viewModel()
            val themeMode by viewModel.themeModeFlow.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val isDark = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            ClgTheme(darkTheme = isDark) {
                com.example.ui.components.AppUpdateChecker()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
                val absentDays by viewModel.absentDays.collectAsStateWithLifecycle()
                val isLoadingAbsentDays by viewModel.isLoadingAbsentDays.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }
                var showLoading by remember { mutableStateOf(true) }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_START) {
                            showLoading = true
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                LaunchedEffect(Unit) {
                    viewModel.errorMessage.collectLatest { msg ->
                        snackbarHostState.showSnackbar(msg)
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (val state = uiState) {
                                is AttendanceUiState.Initializing -> {
                                    // Covered by the loading screen overlay below.
                                }

                                is AttendanceUiState.Onboarding -> {
                                    OnboardingScreen(
                                        initialName = state.initialName,
                                        initialCollege = state.initialCollege,
                                        initialBatch = state.initialBatch,
                                        onContinueWorkspace = { name, college, batch ->
                                            viewModel.completeOnboarding(name, college, batch, connectErp = false)
                                        },
                                        onConnectErp = { name, college, batch ->
                                            viewModel.completeOnboarding(name, college, batch, connectErp = true)
                                        },
                                        onSaveProfileBeforeAuth = { name, college, batch ->
                                            viewModel.saveProfileLocally(name, college, batch)
                                        },
                                        onGoogleAuthSuccess = {
                                            viewModel.handleGoogleAuthSuccess()
                                        }
                                    )
                                }

                                is AttendanceUiState.LoggedOut -> {
                                    LoginScreen(
                                        isLoading = isLoading,
                                        onLoginClick = { user, pass ->
                                            viewModel.login(user, pass)
                                        },
                                        onDemoClick = {
                                            viewModel.enterDemoMode()
                                        },
                                        onGoogleAuthSuccess = {
                                            viewModel.handleGoogleAuthSuccess()
                                        }
                                    )
                                }

                                is AttendanceUiState.LoggedIn -> {
                                    DashboardScreen(
                                        username = state.username,
                                        studentName = state.studentName,
                                        collegeName = state.collegeName,
                                        batch = state.batch,
                                        summary = state.summary,
                                        subjects = state.subjects,
                                        targetThreshold = state.targetThreshold,
                                        themeMode = state.themeMode,
                                        notificationsEnabled = state.notificationsEnabled,
                                        isRefreshing = state.isRefreshing || isLoading,
                                        absentDays = absentDays,
                                        isLoadingAbsentDays = isLoadingAbsentDays,
                                        onLoadAbsentDays = { viewModel.loadAbsentDays() },
                                        onRefreshClick = { viewModel.refresh(isBackground = false) },
                                        onThresholdChange = { viewModel.setTargetThreshold(it) },
                                        onThemeModeChange = { viewModel.setThemeMode(it) },
                                        onNotificationsEnabledChange = { viewModel.setNotificationsEnabled(it) },
                                        onLogoutClick = { viewModel.logout() }
                                    )
                                }
                            }
                        }
                    }

                    if (showLoading) {
                        LoadingScreen(
                            isReady = uiState !is AttendanceUiState.Initializing,
                            onFinished = { showLoading = false },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
