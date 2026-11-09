package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.kit.ClgBackground
import com.example.ui.kit.ClgColors
import com.example.ui.kit.GlassCard
import com.example.ui.kit.GradientButton
import com.example.util.GoogleSignInButton

@Composable
fun OnboardingScreen(
    initialName: String = "",
    initialCollege: String = "",
    initialBatch: String = "",
    onContinueWorkspace: (name: String, college: String, batch: String) -> Unit,
    onConnectErp: (name: String, college: String, batch: String) -> Unit,
    onSaveProfileBeforeAuth: ((name: String, college: String, batch: String) -> Unit)? = null,
    onGoogleAuthSuccess: () -> Unit = {}
) {
    var name by remember { mutableStateOf(initialName) }
    var collegeName by remember { mutableStateOf(initialCollege) }
    var batch by remember { mutableStateOf(initialBatch) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        if (name.trim().isBlank()) {
            errorMessage = "Please enter your name"
            return false
        }
        if (collegeName.trim().isBlank()) {
            errorMessage = "Please enter your college or university name"
            return false
        }
        if (batch.trim().isBlank()) {
            errorMessage = "Please enter your batch year (e.g. 2024 - 2028)"
            return false
        }
        errorMessage = null
        return true
    }

    ClgBackground(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // New App Logo
                Image(
                    painter = painterResource(id = R.drawable.clg1_logo),
                    contentDescription = "Clg 1% Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(86.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.5.dp, ClgColors.Cyan.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Welcome to Clg 1%",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Smart academic companion built for students across all colleges.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ClgColors.TextMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(ClgColors.CyanDeep, ClgColors.Cyan))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Stars,
                                contentDescription = null,
                                tint = ClgColors.Navy,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Student Profile Setup",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Personalize your workspace and cloud records.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ClgColors.TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Name Input
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; errorMessage = null },
                        label = { Text("Your Full Name") },
                        placeholder = { Text("e.g. Arun Kumar", color = Color.Gray) },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = "Student Name", tint = ClgColors.Cyan)
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ClgColors.Cyan,
                            unfocusedBorderColor = ClgColors.Outline,
                            focusedLabelColor = ClgColors.Cyan,
                            cursorColor = ClgColors.Cyan
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("name_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // College Input
                    OutlinedTextField(
                        value = collegeName,
                        onValueChange = { collegeName = it; errorMessage = null },
                        label = { Text("College / University Name") },
                        placeholder = { Text("e.g. College of Engineering / University", color = Color.Gray) },
                        leadingIcon = {
                            Icon(Icons.Default.School, contentDescription = "College Name", tint = ClgColors.Cyan)
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ClgColors.Cyan,
                            unfocusedBorderColor = ClgColors.Outline,
                            focusedLabelColor = ClgColors.Cyan,
                            cursorColor = ClgColors.Cyan
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("college_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Batch Input
                    OutlinedTextField(
                        value = batch,
                        onValueChange = { batch = it; errorMessage = null },
                        label = { Text("Batch / Graduation Year") },
                        placeholder = { Text("e.g. 2024 - 2028", color = Color.Gray) },
                        leadingIcon = {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Batch", tint = ClgColors.Cyan)
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ClgColors.Cyan,
                            unfocusedBorderColor = ClgColors.Outline,
                            focusedLabelColor = ClgColors.Cyan,
                            cursorColor = ClgColors.Cyan
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (validate()) {
                                    onContinueWorkspace(name.trim(), collegeName.trim(), batch.trim())
                                }
                            }
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("batch_input")
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = ClgColors.Danger,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Enter Workspace Button (Zero fake data)
                    GradientButton(
                        text = "Continue to Workspace",
                        onClick = {
                            if (validate()) {
                                onContinueWorkspace(name.trim(), collegeName.trim(), batch.trim())
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("enter_workspace_button")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Connect ERP Portal Button (Optional)
                    OutlinedButton(
                        onClick = {
                            if (validate()) {
                                onConnectErp(name.trim(), collegeName.trim(), batch.trim())
                            }
                        },
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ClgColors.Cyan.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("connect_erp_button")
                    ) {
                        Icon(Icons.Default.AccountBalance, contentDescription = "ERP Portal", tint = ClgColors.Cyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connect College ERP (Optional)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = ClgColors.Outline.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Google Sign-In with Firebase Firestore
                    GoogleSignInButton(
                        label = "Save & Sync with Google Cloud",
                        onAuthSuccess = {
                            if (name.isNotBlank()) {
                                onSaveProfileBeforeAuth?.invoke(name.trim(), collegeName.trim(), batch.trim())
                            }
                            onGoogleAuthSuccess()
                        },
                        onAuthError = { errorMessage = it }
                    )
                }
            }
        }
    }
}
