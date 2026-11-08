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
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoadingScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.SPLASH_MIN_MILLIS
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: AttendanceViewModel = viewModel()
            val themeMode by viewModel.themeModeFlow.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)

            MyApplicationTheme(themeMode = themeMode) {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }

                // The loading screen stays for at least SPLASH_MIN_MILLIS, and until the saved login is checked.
                var splashFinished by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(SPLASH_MIN_MILLIS)
                    splashFinished = true
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

                                is AttendanceUiState.LoggedOut -> {
                                    LoginScreen(
                                        isLoading = isLoading,
                                        onLoginClick = { user, pass ->
                                            viewModel.login(user, pass)
                                        },
                                        onDemoClick = {
                                            viewModel.enterDemoMode()
                                        }
                                    )
                                }

                                is AttendanceUiState.LoggedIn -> {
                                    DashboardScreen(
                                        username = state.username,
                                        studentName = state.studentName,
                                        summary = state.summary,
                                        subjects = state.subjects,
                                        targetThreshold = state.targetThreshold,
                                        themeMode = state.themeMode,
                                        notificationsEnabled = state.notificationsEnabled,
                                        isRefreshing = state.isRefreshing || isLoading,
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

                    if (!splashFinished && uiState is AttendanceUiState.Initializing) {
                        LoadingScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(onClickLabel = "Skip intro") { splashFinished = true }
                        )
                    }
                }
            }
        }
    }
}
