package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.PlayerEntity
import com.example.ui.CareerViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun TrainingDialog(
    viewModel: CareerViewModel,
    player: PlayerEntity,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1 = Pick Focus, 2 = Timing Game, 3 = Result
    var selectedFocus by remember { mutableStateOf("") }
    var finalGrade by remember { mutableStateOf("") }
    var trainingLogMessage by remember { mutableStateOf("") }

    var indicatorProgress by remember { mutableStateOf(0.0f) }
    var movingRight by remember { mutableStateOf(true) }

    LaunchedEffect(step) {
        if (step == 2) {
            while (step == 2) {
                delay(20)
                if (movingRight) {
                    indicatorProgress += 0.05f
                    if (indicatorProgress >= 1.0f) {
                        indicatorProgress = 1.0f
                        movingRight = false
                    }
                } else {
                    indicatorProgress -= 0.05f
                    if (indicatorProgress <= 0.0f) {
                        indicatorProgress = 0.0f
                        movingRight = true
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = { if (step != 2) onDismiss() }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SportsCardBg),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .padding(16.dp)
                .border(2.dp, PitchGreen, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (step == 1) {
                    Text(
                        text = "🏋️ CHOOSE FOCUS",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PitchGreen
                    )
                    Text(
                        text = "Pick a department to train. Perfect timing gives the highest stat modifier!",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val focuses = listOf(
                        "Finishing" to "🎯 Finishing skill & shooting accuracy",
                        "Pace" to "⚡ Sprint speed & acceleration",
                        "Passing" to "⚽ Link-up play & direct assists",
                        "Physical" to "💪 Strength, stamina & aerial presence",
                        "Technique" to "👟 Ball control, dribbling & composure"
                    )

                    focuses.forEach { (focusName, desc) ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedFocus == focusName) DarkSlate else SportsDarkBg
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFocus = focusName }
                                .border(
                                    1.dp,
                                    if (selectedFocus == focusName) PitchGreen else BorderColor,
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = focusName.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (selectedFocus == focusName) PitchGreen else TextPrimary
                                )
                                Text(text = desc, fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { if (selectedFocus.isNotEmpty()) step = 2 },
                        enabled = selectedFocus.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PitchGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(text = "PROCEED TO TRAINING", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                } else if (step == 2) {
                    Text(
                        text = "⚡ TIMING MINI-GAME",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PitchGreen
                    )
                    Text(
                        text = "Tap STRIKE when the indicator is exactly in the center green zone!",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSlate)
                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.60f)
                                .align(Alignment.Center)
                                .background(TrophyGold.copy(alpha = 0.15f))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.30f)
                                .align(Alignment.Center)
                                .background(PitchGreen.copy(alpha = 0.25f))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.10f)
                                .align(Alignment.Center)
                                .background(PitchGreen)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(2.dp)
                                .align(Alignment.Center)
                                .background(Color.White.copy(alpha = 0.5f))
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.04f)
                                .align(
                                    androidx.compose.ui.BiasAlignment(
                                        horizontalBias = (indicatorProgress * 2f) - 1.0f,
                                        verticalBias = 0f
                                    )
                                )
                                .background(TrophyGold)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val value = indicatorProgress
                            val grade = when {
                                value in 0.45f..0.55f -> "S"
                                value in 0.35f..0.65f -> "A"
                                value in 0.20f..0.80f -> "B"
                                else -> "C"
                            }
                            finalGrade = grade
                            viewModel.trainPlayer(selectedFocus, grade)
                            
                            val overtrainingFactor = if (player.overtrainingRisk > 50) 1.5f else 1.0f
                            val fatigueAdded = (10 * overtrainingFactor).toInt()
                            val nextOvertrainingRisk = (player.overtrainingRisk + 25).coerceAtMost(100)
                            
                            val isKnock = nextOvertrainingRisk > 70 && player.overtrainingRisk > 70
                            
                            trainingLogMessage = if (isKnock) {
                                "⚠️ Overtraining Alert: You pushed yourself too hard and suffered a minor knock! Fatigue spiked by +20."
                            } else {
                                "💪 Training Complete: Focus on $selectedFocus (Grade $grade). You earned a nice bonus to season-end growth."
                            }
                            
                            step = 3
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PitchGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("strike_button")
                    ) {
                        Text(text = "STRIKE NOW! ⚡", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                } else {
                    Text(
                        text = "📊 SESSION GRADED",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PitchGreen
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(DarkSlate)
                            .border(
                                3.dp,
                                when (finalGrade) {
                                    "S" -> TrophyGold
                                    "A" -> PitchGreen
                                    "B" -> Color.Cyan
                                    else -> TextSecondary
                                },
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = finalGrade,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black,
                            color = when (finalGrade) {
                                "S" -> TrophyGold
                                "A" -> PitchGreen
                                "B" -> Color.Cyan
                                else -> TextSecondary
                            }
                        )
                    }

                    Text(
                        text = "Grade $finalGrade Performance",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = trainingLogMessage,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onDismiss() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSlate,
                            contentColor = PitchGreen
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(text = "CLOSE", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
