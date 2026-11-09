package com.example.billing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.kit.ClgColors
import kotlinx.coroutines.delay

@Composable
fun AdMobRewardedAdModal(
    onRewardEarned: () -> Unit,
    onDismiss: () -> Unit
) {
    var secondsLeft by remember { mutableIntStateOf(5) }
    var rewardGranted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
        }
        rewardGranted = true
    }

    val progress by animateFloatAsState(
        targetValue = (5 - secondsLeft) / 5f,
        animationSpec = tween(1000, easing = LinearEasing),
        label = "adProgress"
    )

    Dialog(
        onDismissRequest = {
            if (rewardGranted) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = rewardGranted,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF030712).copy(alpha = 0.96f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        1.dp,
                        Brush.linearGradient(listOf(ClgColors.Cyan, ClgColors.Outline)),
                        RoundedCornerShape(24.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = ClgColors.NavySurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(ClgColors.Cyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Ad",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ClgColors.Cyan
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Google AdMob • Sponsored",
                                style = MaterialTheme.typography.labelMedium,
                                color = ClgColors.TextMuted
                            )
                        }

                        if (rewardGranted) {
                            IconButton(
                                onClick = {
                                    onRewardEarned()
                                    onDismiss()
                                },
                                modifier = Modifier.testTag("ad_close_button")
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close Ad",
                                    tint = Color.White
                                )
                            }
                        } else {
                            Text(
                                "Reward in ${secondsLeft}s",
                                style = MaterialTheme.typography.labelSmall,
                                color = ClgColors.Cyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Ad Video Container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(ClgColors.NavyCard, ClgColors.NavyDeep)
                                )
                            )
                            .border(1.dp, ClgColors.Outline, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(ClgColors.Cyan, ClgColors.CyanDeep)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (rewardGranted) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                                    contentDescription = "Playing Ad",
                                    tint = ClgColors.Navy,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Text(
                                text = if (rewardGranted) "Ad Completed!" else "Clg 1% Campus Ecosystem",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = if (rewardGranted) "1 AI Credit Unlocked" else "Empowering college students nationwide",
                                style = MaterialTheme.typography.bodySmall,
                                color = ClgColors.TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(ClgColors.CyanDeep, ClgColors.Cyan)
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Reward Claim CTA
                    AnimatedVisibility(visible = rewardGranted) {
                        Button(
                            onClick = {
                                onRewardEarned()
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("claim_reward_button"),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = ClgColors.Cyan)
                        ) {
                            Icon(
                                Icons.Default.Stars,
                                contentDescription = null,
                                tint = ClgColors.Navy,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Claim AI Analysis & Continue",
                                color = ClgColors.Navy,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (!rewardGranted) {
                        Text(
                            text = "Please watch until completion to unlock your personalized Gemini AI strategy.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ClgColors.TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
