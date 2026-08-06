package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.TrophyEntity
import com.example.ui.tabs.CompetitionTrophyIcon
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.random.Random

data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float
)

@Composable
fun TrophyWinAnimation(
    trophy: TrophyEntity,
    onDismiss: () -> Unit
) {
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    var canDismiss by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(300))
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        textAlpha.animateTo(1f, animationSpec = tween(400))
        delay(1500)
        canDismiss = true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val confettiProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti_progress"
    )

    val particles = remember {
        val colors = listOf(
            TrophyGold, PitchGreen, Color.White, Color(0xFFFFD700), Color(0xFFFF4500), Color(0xFF00BFFF)
        )
        List(60) {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * 0.3f,
                vx = (Random.nextFloat() - 0.5f) * 0.8f,
                vy = Random.nextFloat() * 0.6f + 0.4f,
                color = colors[Random.nextInt(colors.size)],
                size = Random.nextFloat() * 12f + 6f
            )
        }
    }

    Dialog(
        onDismissRequest = { if (canDismiss) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable { if (canDismiss) onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                particles.forEach { p ->
                    val currentX = (p.x + p.vx * confettiProgress) % 1f * canvasWidth
                    val currentY = (p.y + p.vy * confettiProgress) % 1f * canvasHeight
                    drawRect(
                        color = p.color,
                        topLeft = Offset(currentX, currentY),
                        size = Size(p.size, p.size)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(24.dp)
                    .scale(scale.value)
                    .alpha(alpha.value),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🏆 TROPHY UNLOCKED! 🏆",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = TrophyGold,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                CompetitionTrophyIcon(
                    title = trophy.competitionName,
                    isUnlocked = true,
                    modifier = Modifier.size(140.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier.alpha(textAlpha.value),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = trophy.competitionName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "${trophy.clubName} • Season ${trophy.seasonYear}",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = PitchGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(44.dp)
                    ) {
                        Text(text = "CONTINUE", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
