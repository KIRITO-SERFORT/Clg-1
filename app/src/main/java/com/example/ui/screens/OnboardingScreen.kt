package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TealSecondary
import com.example.util.GoogleSignInButton

@Composable
fun OnboardingScreen(
    initialName: String = "",
    initialCollege: String = "",
    initialBatch: String = "",
    onContinueWorkspace: (name: String, college: String, batch: String) -> Unit,
    onConnectErp: (name: String, college: String, batch: String) -> Unit,
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
            errorMessage = "Please enter your college name"
            return false
        }
        if (batch.trim().isBlank()) {
            errorMessage = "Please enter your batch (e.g. 2024 - 2028)"
            return false
        }
        errorMessage = null
        return true
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
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
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App Logo
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "Clg 1% Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Welcome to Clg 1%",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Smart academic companion built for students across all colleges.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                ElevatedCard(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "Student Profile",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Set up your details once to personalize your academic workspace.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Name Input
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it; errorMessage = null },
                            label = { Text("Your Full Name") },
                            placeholder = { Text("e.g. Arun Kumar") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = "Student Name")
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("name_input")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // College Input
                        OutlinedTextField(
                            value = collegeName,
                            onValueChange = { collegeName = it; errorMessage = null },
                            label = { Text("College / University Name") },
                            placeholder = { Text("e.g. College of Engineering / University") },
                            leadingIcon = {
                                Icon(Icons.Default.School, contentDescription = "College Name")
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("college_input")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Batch Input
                        OutlinedTextField(
                            value = batch,
                            onValueChange = { batch = it; errorMessage = null },
                            label = { Text("Batch / Graduation Year") },
                            placeholder = { Text("e.g. 2024 - 2028") },
                            leadingIcon = {
                                Icon(Icons.Default.CalendarMonth, contentDescription = "Batch")
                            },
                            singleLine = true,
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
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("batch_input")
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Enter Workspace Button (Without ERP, clean real data)
                        Button(
                            onClick = {
                                if (validate()) {
                                    onContinueWorkspace(name.trim(), collegeName.trim(), batch.trim())
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("enter_workspace_button")
                        ) {
                            Text("Continue to Workspace", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = "Continue", modifier = Modifier.size(18.dp))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Connect ERP Portal Button (Optional)
                        OutlinedButton(
                            onClick = {
                                if (validate()) {
                                    onConnectErp(name.trim(), collegeName.trim(), batch.trim())
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("connect_erp_button")
                        ) {
                            Icon(Icons.Default.AccountBalance, contentDescription = "ERP Portal", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connect College ERP (Optional)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(18.dp))

                        // Google Sign-In with Firebase
                        GoogleSignInButton(
                            label = "Sync Profile with Google Cloud",
                            onAuthSuccess = onGoogleAuthSuccess,
                            onAuthError = { errorMessage = it }
                        )
                    }
                }
            }
        }
    }
}
