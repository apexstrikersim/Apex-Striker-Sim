package com.example.ui.tutorial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.ImmersiveDialogEffect
import com.example.ui.theme.PitchGreen
import com.example.ui.theme.TextSecondary

@Composable
fun TutorialOverlay(
    steps: List<TutorialStep>,
    onFinished: () -> Unit
) {
    var currentIndex by remember { mutableStateOf(0) }
    val currentStep = steps.getOrNull(currentIndex) ?: run {
        onFinished()
        return
    }

    Dialog(
        onDismissRequest = { /* dismiss not allowed via outside tap or back press */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        ImmersiveDialogEffect()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .testTag("tutorial_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Icon Container
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(PitchGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = currentStep.icon,
                            contentDescription = null,
                            tint = PitchGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Title
                    Text(
                        text = currentStep.title,
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. Description
                    Text(
                        text = currentStep.description,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 4. Step Dot Indicators
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        steps.forEachIndexed { index, _ ->
                            if (index == currentIndex) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 18.dp, height = 6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(PitchGreen)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.25f))
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 5. Back / Skip / Next Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (currentIndex > 0) {
                                TextButton(
                                    onClick = { currentIndex-- },
                                    modifier = Modifier.testTag("tutorial_back_button")
                                ) {
                                    Text(
                                        text = "Back",
                                        color = TextSecondary,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            TextButton(
                                onClick = onFinished,
                                modifier = Modifier.testTag("tutorial_skip_button")
                            ) {
                                Text(
                                    text = "Skip",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (currentIndex < steps.size - 1) {
                                    currentIndex++
                                } else {
                                    onFinished()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PitchGreen,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("tutorial_next_button")
                        ) {
                            Text(
                                text = if (currentIndex < steps.size - 1) "Next" else "Done",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
