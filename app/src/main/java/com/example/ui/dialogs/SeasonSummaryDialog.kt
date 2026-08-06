package com.example.ui.dialogs

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.SeasonSummaryData
import com.example.data.formatSeasonYear
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SeasonSummaryDialog(
    summary: SeasonSummaryData,
    onDismiss: () -> Unit
) {
    val award = summary.playerOfTheSeasonAward
    val coroutineScope = rememberCoroutineScope()

    val scaleAnim = remember { Animatable(0.3f) }
    val alphaAnim = remember { Animatable(0f) }
    val rotationAnim = remember { Animatable(-15f) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            scaleAnim.animateTo(
                1f,
                animationSpec = spring(
                    dampingRatio = 0.55f,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        coroutineScope.launch {
            alphaAnim.animateTo(1f, animationSpec = tween(400))
        }
        coroutineScope.launch {
            rotationAnim.animateTo(
                0f,
                animationSpec = spring(
                    dampingRatio = 0.6f
                )
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "seasonTrophyShine")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing)
        ),
        label = "seasonTrophyAngle"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "seasonTrophyPulse"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SportsCardBg,
            border = BorderStroke(
                1.5.dp,
                if (award.isWinner) TrophyGold else PitchGreen.copy(alpha = 0.6f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = DarkSlate,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Text(
                        text = "🏆 END OF SEASON ${summary.seasonNumber} SUMMARY • ${formatSeasonYear(summary.seasonNumber)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TrophyGold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = summary.playerName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Gen ${summary.generation} • ${summary.clubName}",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = DarkSlate,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("⚽ Goals", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                text = "${summary.goals}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "+${award.goalsPoints} pts",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PitchGreen
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        color = DarkSlate,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🅰️ Assists", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                text = "${summary.assists}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "+${award.assistsPoints} pts",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PitchGreen
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        color = DarkSlate,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("⭐ Rating", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                text = if (summary.averageRating > 0f) "%.1f".format(summary.averageRating) else "N/A",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (summary.averageRating >= 8f) PitchGreen else if (summary.averageRating >= 6.5f) TrophyGold else TextPrimary
                            )
                            Text(
                                text = "+${award.ratingPoints} pts",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PitchGreen
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        color = DarkSlate,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🏟️ Matches", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                text = "${summary.matchesPlayed}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Played",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (summary.trophiesWon.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = DarkSlate,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, TrophyGold.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Trophies",
                                    tint = TrophyGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "TROPHIES WON THIS SEASON (+${award.trophiesPoints} pts)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TrophyGold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            summary.trophiesWon.forEach { trophy ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Text("🥇", fontSize = 12.sp)
                                    Text(trophy, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = scaleAnim.value
                            scaleY = scaleAnim.value
                            alpha = alphaAnim.value
                            rotationZ = rotationAnim.value
                        },
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Unspecified,
                    border = BorderStroke(
                        2.dp,
                        Brush.linearGradient(
                            if (award.isWinner) listOf(TrophyGold, PitchGreen, TrophyGold)
                            else listOf(BorderColor, PitchGreen, BorderColor)
                        )
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    if (award.isWinner) listOf(
                                        Color(0xFF2A2100),
                                        Color(0xFF1B1600),
                                        DarkSlate
                                    ) else listOf(
                                        DarkSlate,
                                        SportsCardBg
                                    )
                                )
                            )
                    ) {
                        Canvas(
                            modifier = Modifier
                                .matchParentSize()
                                .clipToBounds()
                        ) {
                            val center = Offset(size.width / 2f, size.height / 2.2f)
                            val rayCount = 12
                            val rayLength = size.width.coerceAtLeast(size.height)

                            for (i in 0 until rayCount) {
                                val rayAngle = angle + (i * (360f / rayCount))
                                val rad = Math.toRadians(rayAngle.toDouble()).toFloat()
                                val endX = center.x + rayLength * kotlin.math.cos(rad)
                                val endY = center.y + rayLength * kotlin.math.sin(rad)

                                drawLine(
                                    color = (if (award.isWinner) TrophyGold else PitchGreen).copy(alpha = 0.08f),
                                    start = center,
                                    end = Offset(endX, endY),
                                    strokeWidth = 14f
                                )
                            }

                            val sparkCount = 8
                            for (j in 0 until sparkCount) {
                                val sparkRad = Math.toRadians((angle * 1.5f + j * 45f).toDouble()).toFloat()
                                val dist = 40.dp.toPx() + (j * 8.dp.toPx())
                                val sx = center.x + dist * kotlin.math.cos(sparkRad)
                                val sy = center.y + dist * kotlin.math.sin(sparkRad)
                                drawCircle(
                                    color = if (j % 2 == 0) TrophyGold.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.8f),
                                    radius = (3f + (j % 3) * 2f),
                                    center = Offset(sx, sy)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .graphicsLayer {
                                        scaleX = pulseScale
                                        scaleY = pulseScale
                                    }
                                    .background(
                                        if (award.isWinner) Brush.radialGradient(
                                            listOf(TrophyGold.copy(alpha = 0.4f), Color.Transparent)
                                        ) else Brush.radialGradient(
                                            listOf(PitchGreen.copy(alpha = 0.3f), Color.Transparent)
                                        ),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Player of the Season Award",
                                    tint = if (award.isWinner) TrophyGold else PitchGreen,
                                    modifier = Modifier.size(52.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = award.awardTitle,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = if (award.isWinner) TrophyGold else PitchGreen,
                                letterSpacing = 1.sp,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = if (award.isWinner) "OFFICIAL PLAYER OF THE SEASON WINNER" else "OUTSTANDING SEASON PERFORMANCE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Surface(
                                color = Color.Black.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.6f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "AWARD SCORE BREAKDOWN",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Goals (${award.goalsPoints}) + Assists (${award.assistsPoints}) + Rating (${award.ratingPoints}) + Trophies (${award.trophiesPoints})",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "TOTAL AWARD SCORE: ${award.totalPoints} PTS",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (award.isWinner) TrophyGold else PitchGreen
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (award.isWinner) TrophyGold else PitchGreen,
                        contentColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "Continue to Next Season",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Next Season",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
