package com.example.ui

import com.example.ui.theme.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SportsFootball
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.PlayerEntity
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class YouthDrillType {
    SHOOTING, PASSING, PACE, TECHNICAL, PHYSICAL
}

@Composable
fun YouthTrainingDialog(
    player: PlayerEntity,
    onCompleteTraining: (drillType: YouthDrillType, qualityScore: Float) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDrill by remember { mutableStateOf<YouthDrillType?>(null) }
    var drillCompleted by remember { mutableStateOf(false) }
    var finalQualityScore by remember { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val isDrillActive = selectedDrill != null && !drillCompleted
        Card(
            modifier = if (isDrillActive) {
                Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            } else {
                Modifier
                    .fillMaxWidth(0.95f)
                    .padding(12.dp)
            },
            colors = CardDefaults.cardColors(containerColor = DarkBackground),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, PitchGreen.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .let { if (isDrillActive) it.fillMaxSize() else it.fillMaxWidth() }
                    .padding(if (isDrillActive) 12.dp else 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (selectedDrill == null) {
                    // Drill Selection Screen
                    Text(
                        text = "YOUTH DEVELOPMENT DRILLS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PitchGreen,
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Select Youth Drill",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    if (player.hasTrainedThisMonth == 1) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CoralRed.copy(alpha = 0.15f))
                                .border(1.dp, CoralRed, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Already trained this month. Advance time to train again next month!",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val canTrain = player.hasTrainedThisMonth == 0
                        YouthDrillCard(
                            title = "1. Target Shooting Drill",
                            category = "Finishing",
                            currentStat = player.finishing,
                            bonusProgress = player.finishingTrainingBonus,
                            count = player.shootingDrillsCompleted,
                            icon = Icons.Default.SportsSoccer,
                            color = if (canTrain) CoralRed else TextSecondary,
                            onClick = { if (canTrain) selectedDrill = YouthDrillType.SHOOTING }
                        )

                        YouthDrillCard(
                            title = "2. Precision Passing Drill",
                            category = "Passing",
                            currentStat = player.passing,
                            bonusProgress = player.passingTrainingBonus,
                            count = player.passingDrillsCompleted,
                            icon = Icons.Default.SportsFootball,
                            color = if (canTrain) MutedBlue else TextSecondary,
                            onClick = { if (canTrain) selectedDrill = YouthDrillType.PASSING }
                        )

                        YouthDrillCard(
                            title = "3. Speed Sprints Drill",
                            category = "Pace",
                            currentStat = player.pace,
                            bonusProgress = player.paceTrainingBonus,
                            count = player.paceDrillsCompleted,
                            icon = Icons.Default.DirectionsRun,
                            color = if (canTrain) PitchGreen else TextSecondary,
                            onClick = { if (canTrain) selectedDrill = YouthDrillType.PACE }
                        )

                        YouthDrillCard(
                            title = "4. Obstacle Dribble Drill",
                            category = "Technique",
                            currentStat = player.technique,
                            bonusProgress = player.techniqueTrainingBonus,
                            count = player.technicalDrillsCompleted,
                            icon = Icons.Default.Star,
                            color = if (canTrain) GoldStar else TextSecondary,
                            onClick = { if (canTrain) selectedDrill = YouthDrillType.TECHNICAL }
                        )

                        YouthDrillCard(
                            title = "5. Reaction Pop Drill",
                            category = "Physical",
                            currentStat = player.physical,
                            bonusProgress = player.physicalTrainingBonus,
                            count = player.physicalDrillsCompleted,
                            icon = Icons.Default.FitnessCenter,
                            color = if (canTrain) Color(0xFFA855F7) else TextSecondary,
                            onClick = { if (canTrain) selectedDrill = YouthDrillType.PHYSICAL }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("Cancel")
                    }

                } else if (!drillCompleted) {
                    // Active Drill Canvas - Expanded to Fill Screen
                    val drill = selectedDrill!!
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = drill.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PitchGreen
                        )
                        IconButton(onClick = { selectedDrill = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Exit Drill", tint = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardBackground)
                            .border(1.dp, PitchGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    ) {
                        when (drill) {
                            YouthDrillType.SHOOTING -> ShootingDrillCanvas { score ->
                                finalQualityScore = score
                                drillCompleted = true
                            }
                            YouthDrillType.PASSING -> PassingDrillCanvas { score ->
                                finalQualityScore = score
                                drillCompleted = true
                            }
                            YouthDrillType.PACE -> PaceDrillCanvas { score ->
                                finalQualityScore = score
                                drillCompleted = true
                            }
                            YouthDrillType.TECHNICAL -> TechnicalDrillCanvas { score ->
                                finalQualityScore = score
                                drillCompleted = true
                            }
                            YouthDrillType.PHYSICAL -> PhysicalDrillCanvas { score ->
                                finalQualityScore = score
                                drillCompleted = true
                            }
                        }
                    }

                } else {
                    // Result Summary Screen
                    Text(
                        text = "DRILL COMPLETED",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PitchGreen
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${(finalQualityScore * 100).toInt()}% Performance Quality",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (finalQualityScore >= 0.7f) PitchGreen else if (finalQualityScore >= 0.4f) GoldStar else CoralRed
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val grade = when {
                        finalQualityScore >= 0.85f -> "Mastery Performance (+2.0 Bonus)"
                        finalQualityScore >= 0.65f -> "Solid Effort (+1.2 Bonus)"
                        finalQualityScore >= 0.40f -> "Basic Practice (+0.6 Bonus)"
                        else -> "Rough Attempt (+0.2 Bonus)"
                    }

                    Text(
                        text = grade,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            selectedDrill?.let { onCompleteTraining(it, finalQualityScore) }
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PitchGreen)
                    ) {
                        Text("Apply Training & Continue", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun YouthDrillCard(
    title: String,
    category: String,
    currentStat: Int,
    bonusProgress: Float,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Focus: $category • Stat: $currentStat ",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        "(+${String.format("%.1f", bonusProgress)}/1.0)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PitchGreen,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            if (count >= 25) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(PitchGreen.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("Specialized", fontSize = 10.sp, color = PitchGreen, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 1. Shooting Drill Canvas
@Composable
fun ShootingDrillCanvas(onComplete: (Float) -> Unit) {
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragCurrent by remember { mutableStateOf<Offset?>(null) }
    var shotTaken by remember { mutableStateOf(false) }

    var targetOffset by remember { mutableStateOf<Offset?>(null) }
    var animatedBallPos by remember { mutableStateOf<Offset?>(null) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(shotTaken, canvasSize) {
                if (shotTaken) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        dragStart = offset
                        dragCurrent = offset
                    },
                    onDrag = { change, _ ->
                        dragCurrent = change.position
                    },
                    onDragEnd = {
                        val start = dragStart
                        val curr = dragCurrent
                        val h = if (canvasSize.height > 0) canvasSize.height else 500f
                        val w = if (canvasSize.width > 0) canvasSize.width else 300f
                        val bPos = animatedBallPos ?: Offset(w * 0.5f, h * 0.60f)

                        if (start != null && curr != null && targetOffset != null) {
                            val rawVector = start - curr
                            val rawLen = hypot(rawVector.x, rawVector.y)
                            if (rawLen > 10f) {
                                val maxDragLen = (h * 0.42f).coerceIn(220f, 420f)
                                val powerRatio = (rawLen / maxDragLen).coerceIn(0.1f, 1.0f)
                                val maxTravelDist = h * 0.48f
                                val shotTarget = bPos + Offset(rawVector.x / rawLen, rawVector.y / rawLen) * (powerRatio * maxTravelDist)

                                shotTaken = true

                                val targetDist = hypot(shotTarget.x - targetOffset!!.x, shotTarget.y - targetOffset!!.y)
                                val closeness = (1f - (targetDist / 180f)).coerceIn(0.1f, 1f)
                                val score = closeness

                                coroutineScope.launch {
                                    val steps = 18
                                    val startBall = bPos
                                    for (i in 1..steps) {
                                        val t = i.toFloat() / steps
                                        animatedBallPos = Offset(
                                            startBall.x + (shotTarget.x - startBall.x) * t,
                                            startBall.y + (shotTarget.y - startBall.y) * t
                                        )
                                        delay(18L)
                                    }
                                    onComplete(score)
                                }
                            }
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            if (canvasSize != size) {
                canvasSize = size
            }

            // Pitch & Goal area
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1B4332), Color(0xFF0F2A1E))
                )
            )
            val stripeHeight = height / 8f
            for (i in 0..7 step 2) {
                drawRect(
                    color = Color.White.copy(alpha = 0.04f),
                    topLeft = Offset(0f, i * stripeHeight),
                    size = Size(width, stripeHeight)
                )
            }

            val goalLeft = width * 0.15f
            val goalRight = width * 0.85f
            val goalTop = height * 0.10f
            val goalBottom = height * 0.28f

            drawRect(
                color = Color.White,
                topLeft = Offset(goalLeft, goalTop),
                size = Size(goalRight - goalLeft, goalBottom - goalTop),
                style = Stroke(width = 4f)
            )

            // Highlighted Target Zone
            if (targetOffset == null) {
                val targetX = goalLeft + 40f + Random.nextFloat() * (goalRight - goalLeft - 80f)
                val targetY = goalTop + 40f + Random.nextFloat() * (goalBottom - goalTop - 60f)
                targetOffset = Offset(targetX, targetY)
            }

            drawCircle(
                color = CoralRed.copy(alpha = 0.25f),
                radius = 48f,
                center = targetOffset!!
            )
            drawCircle(
                color = CoralRed.copy(alpha = 0.8f),
                radius = 32f,
                center = targetOffset!!
            )
            drawCircle(
                color = Color.White,
                radius = 12f,
                center = targetOffset!!
            )

            // Ball position placed safely in mid-lower section
            val bPos = animatedBallPos ?: Offset(width * 0.5f, height * 0.60f)
            if (animatedBallPos == null) {
                animatedBallPos = bPos
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color(0xFFDDDDDD)),
                    center = bPos,
                    radius = 16f
                ),
                radius = 16f,
                center = bPos
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.25f),
                radius = 16f,
                center = bPos,
                style = Stroke(width = 2f)
            )

            // Visual Aiming Line
            val start = dragStart
            val curr = dragCurrent
            if (start != null && curr != null && !shotTaken) {
                val rawVector = start - curr
                val rawLen = hypot(rawVector.x, rawVector.y)
                if (rawLen > 5f) {
                    val maxDragLen = (height * 0.42f).coerceIn(220f, 420f)
                    val powerRatio = (rawLen / maxDragLen).coerceIn(0.1f, 1.0f)
                    val maxTravelDist = height * 0.48f
                    val aimTarget = bPos + Offset(rawVector.x / rawLen, rawVector.y / rawLen) * (powerRatio * maxTravelDist)

                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(GoldStar.copy(alpha = 0.4f), GoldStar),
                            start = bPos,
                            end = aimTarget
                        ),
                        start = bPos,
                        end = aimTarget,
                        strokeWidth = 6f
                    )
                    drawCircle(
                        color = GoldStar,
                        radius = 10f,
                        center = aimTarget
                    )
                }
            }
        }

        Text(
            text = "Drag BACK slightly to aim & release to shoot!",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
        )
    }
}

// 2. Passing Drill Canvas
@Composable
fun PassingDrillCanvas(onComplete: (Float) -> Unit) {
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragCurrent by remember { mutableStateOf<Offset?>(null) }
    var passTaken by remember { mutableStateOf(false) }

    var targetOffset by remember { mutableStateOf<Offset?>(null) }
    var animatedBallPos by remember { mutableStateOf<Offset?>(null) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(passTaken, canvasSize) {
                if (passTaken) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        dragStart = offset
                        dragCurrent = offset
                    },
                    onDrag = { change, _ ->
                        dragCurrent = change.position
                    },
                    onDragEnd = {
                        val start = dragStart
                        val curr = dragCurrent
                        val h = if (canvasSize.height > 0) canvasSize.height else 500f
                        val w = if (canvasSize.width > 0) canvasSize.width else 300f
                        val bPos = animatedBallPos ?: Offset(w * 0.5f, h * 0.60f)

                        if (start != null && curr != null && targetOffset != null) {
                            val rawVector = start - curr
                            val rawLen = hypot(rawVector.x, rawVector.y)
                            if (rawLen > 10f) {
                                val maxDragLen = (h * 0.42f).coerceIn(220f, 420f)
                                val powerRatio = (rawLen / maxDragLen).coerceIn(0.1f, 1.0f)
                                val maxTravelDist = h * 0.48f
                                val passTarget = bPos + Offset(rawVector.x / rawLen, rawVector.y / rawLen) * (powerRatio * maxTravelDist)

                                passTaken = true

                                val targetDist = hypot(passTarget.x - targetOffset!!.x, passTarget.y - targetOffset!!.y)
                                val closeness = (1f - (targetDist / 180f)).coerceIn(0.1f, 1f)
                                val score = closeness

                                coroutineScope.launch {
                                    val steps = 18
                                    val startBall = bPos
                                    for (i in 1..steps) {
                                        val t = i.toFloat() / steps
                                        animatedBallPos = Offset(
                                            startBall.x + (passTarget.x - startBall.x) * t,
                                            startBall.y + (passTarget.y - startBall.y) * t
                                        )
                                        delay(18L)
                                    }
                                    onComplete(score)
                                }
                            }
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            if (canvasSize != size) {
                canvasSize = size
            }

            // Pitch & Grid lines
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1B4332), Color(0xFF0F2A1E))
                )
            )
            val stripeHeight = height / 8f
            for (i in 0..7 step 2) {
                drawRect(
                    color = Color.White.copy(alpha = 0.04f),
                    topLeft = Offset(0f, i * stripeHeight),
                    size = Size(width, stripeHeight)
                )
            }

            // Target Post
            if (targetOffset == null) {
                targetOffset = Offset(width * 0.5f, height * 0.22f)
            }
            val postPos = targetOffset!!
            drawCircle(color = MutedBlue.copy(alpha = 0.25f), radius = 48f, center = postPos)
            drawCircle(color = MutedBlue, radius = 28f, center = postPos)
            drawCircle(color = Color.White, radius = 14f, center = postPos)

            // Ball
            val bPos = animatedBallPos ?: Offset(width * 0.5f, height * 0.60f)
            if (animatedBallPos == null) {
                animatedBallPos = bPos
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color(0xFFDDDDDD)),
                    center = bPos,
                    radius = 16f
                ),
                radius = 16f,
                center = bPos
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.25f),
                radius = 16f,
                center = bPos,
                style = Stroke(width = 2f)
            )

            // Visual Aiming Line directly to pass landing target!
            val start = dragStart
            val curr = dragCurrent
            if (start != null && curr != null && !passTaken) {
                val rawVector = start - curr
                val rawLen = hypot(rawVector.x, rawVector.y)
                if (rawLen > 5f) {
                    val maxDragLen = (height * 0.42f).coerceIn(220f, 420f)
                    val powerRatio = (rawLen / maxDragLen).coerceIn(0.1f, 1.0f)
                    val maxTravelDist = height * 0.48f
                    val aimTarget = bPos + Offset(rawVector.x / rawLen, rawVector.y / rawLen) * (powerRatio * maxTravelDist)

                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(PitchGreen.copy(alpha = 0.4f), PitchGreen),
                            start = bPos,
                            end = aimTarget
                        ),
                        start = bPos,
                        end = aimTarget,
                        strokeWidth = 6f
                    )
                    drawCircle(
                        color = PitchGreen,
                        radius = 10f,
                        center = aimTarget
                    )
                }
            }
        }

        Text(
            text = "Pass directly to the target post with optimal power!",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
        )
    }
}

// 3. Pace Drill Canvas
@Composable
fun PaceDrillCanvas(onComplete: (Float) -> Unit) {
    var tapCount by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableIntStateOf(5) }
    var isRunning by remember { mutableStateOf(true) }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (timeLeft > 0) {
                delay(1000L)
                timeLeft--
            }
            isRunning = false
            val score = (tapCount / 18f).coerceIn(0.1f, 1.0f)
            onComplete(score)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isRunning) {
                if (!isRunning) return@pointerInput
                detectTapGestures {
                    tapCount++
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("TAP AS FAST AS YOU CAN!", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = PitchGreen)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Time Left: ${timeLeft}s", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GoldStar)
            Spacer(modifier = Modifier.height(12.dp))

            // Sprint Track Visual
            Canvas(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBorder.copy(alpha = 0.5f))
            ) {
                val trackWidth = size.width
                val trackHeight = size.height

                // Track lane lines
                drawLine(
                    color = Color.White.copy(alpha = 0.4f),
                    start = Offset(20f, trackHeight * 0.3f),
                    end = Offset(trackWidth - 20f, trackHeight * 0.3f),
                    strokeWidth = 3f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.4f),
                    start = Offset(20f, trackHeight * 0.7f),
                    end = Offset(trackWidth - 20f, trackHeight * 0.7f),
                    strokeWidth = 3f
                )

                // Finish line
                drawLine(
                    color = PitchGreen,
                    start = Offset(trackWidth - 30f, 10f),
                    end = Offset(trackWidth - 30f, trackHeight - 10f),
                    strokeWidth = 6f
                )

                // Runner / Ball marker position along track
                val progress = (tapCount / 18f).coerceIn(0f, 1f)
                val runnerX = 30f + progress * (trackWidth - 60f)
                val runnerY = trackHeight * 0.5f

                drawCircle(
                    color = GoldStar,
                    radius = 16f,
                    center = Offset(runnerX, runnerY)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Taps: $tapCount", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
    }
}

data class TechnicalObstacle(val id: Int, var x: Float, var y: Float, val speed: Float)

// 4. Technical Drill Canvas
@Composable
fun TechnicalDrillCanvas(onComplete: (Float) -> Unit) {
    var survivedSeconds by remember { mutableFloatStateOf(0f) }
    var lost by remember { mutableStateOf(false) }
    var ballPos by remember { mutableStateOf(Offset(200f, 450f)) }

    val obstacles = remember { mutableStateListOf<TechnicalObstacle>() }
    var obstacleIdCounter by remember { mutableIntStateOf(0) }
    var lastSpawnTime by remember { mutableFloatStateOf(0f) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    LaunchedEffect(lost) {
        if (!lost) {
            var lastFrameTimeNanos = 0L
            while (survivedSeconds < 15f && !lost) {
                withFrameNanos { frameTimeNanos ->
                    if (lastFrameTimeNanos == 0L) lastFrameTimeNanos = frameTimeNanos
                    val deltaSeconds = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f
                    lastFrameTimeNanos = frameTimeNanos
                    survivedSeconds += deltaSeconds

                    if (survivedSeconds - lastSpawnTime >= 0.8f) {
                        lastSpawnTime = survivedSeconds
                        obstacleIdCounter++
                        val playWidth = if (canvasSize.width > 0) canvasSize.width else 360f
                        val spawnX = playWidth * 0.08f + Random.nextFloat() * (playWidth * 0.84f)
                        val speed = 8f + Random.nextFloat() * 6f
                        obstacles.add(TechnicalObstacle(obstacleIdCounter, spawnX, 0f, speed))
                    }

                    val toRemove = mutableListOf<TechnicalObstacle>()
                    val playHeight = if (canvasSize.height > 0) canvasSize.height else 600f
                    for (obs in obstacles) {
                        obs.y += obs.speed * deltaSeconds * 30f
                        if (obs.y > playHeight) {
                            toRemove.add(obs)
                        }

                        val dist = hypot(obs.x - ballPos.x, obs.y - ballPos.y)
                        if (dist < 38f) { // radius ball (16f) + radius obstacle (22f)
                            lost = true
                            val score = (survivedSeconds / 15f * 0.5f).coerceIn(0.1f, 0.5f)
                            onComplete(score)
                            return@withFrameNanos
                        }
                    }
                    obstacles.removeAll(toRemove)
                }
            }

            if (!lost && survivedSeconds >= 15f) {
                onComplete(1.0f)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(lost, canvasSize) {
                if (lost) return@pointerInput
                detectDragGestures(
                    onDrag = { change, _ ->
                        val w = if (canvasSize.width > 0) canvasSize.width else 360f
                        val h = if (canvasSize.height > 0) canvasSize.height else 600f
                        ballPos = Offset(
                            change.position.x.coerceIn(18f, w - 18f),
                            change.position.y.coerceIn(18f, h - 18f)
                        )
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (canvasSize != size) {
                canvasSize = size
            }
            // Draw obstacles
            for (obs in obstacles) {
                drawCircle(color = CoralRed, radius = 22f, center = Offset(obs.x, obs.y))
            }

            // Draw player ball
            drawCircle(color = GoldStar, radius = 18f, center = ballPos)
        }

        Text(
            text = "Dodge red obstacles! Time: ${String.format("%.1f", survivedSeconds)}s / 15.0s",
            fontSize = 12.sp,
            color = GoldStar,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(12.dp)
        )
    }
}

// 5. Physical Drill Canvas
@Composable
fun PhysicalDrillCanvas(onComplete: (Float) -> Unit) {
    var popCount by remember { mutableIntStateOf(0) }
    var misses by remember { mutableIntStateOf(0) }
    var buttonOffset by remember { mutableStateOf<IntOffset?>(null) }
    var timerLeft by remember { mutableIntStateOf(10) }

    val buttonAlpha = remember { androidx.compose.animation.core.Animatable(0f) }
    var currentPopTapped by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()

        LaunchedEffect(Unit) {
            while (timerLeft > 0 && misses < 3) {
                currentPopTapped = false
                val maxX = (maxWidthPx - 160f).coerceAtLeast(40f)
                val maxY = (maxHeightPx - 200f).coerceAtLeast(40f)
                val nextX = (40f + Random.nextFloat() * maxX).toInt()
                val nextY = (60f + Random.nextFloat() * maxY).toInt()
                buttonOffset = IntOffset(nextX, nextY)

                // Fade in
                buttonAlpha.snapTo(0f)
                buttonAlpha.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(200))

                // Active window
                delay(1100L)

                // Fade out
                buttonAlpha.animateTo(0f, animationSpec = androidx.compose.animation.core.tween(200))

                if (!currentPopTapped) {
                    misses++
                }
                timerLeft--
            }

            val score = (popCount / 10f - misses * 0.2f).coerceIn(0.1f, 1.0f)
            onComplete(score)
        }

        if (buttonOffset != null) {
            Box(
                modifier = Modifier
                    .offset { buttonOffset!! }
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFA855F7).copy(alpha = buttonAlpha.value))
                    .pointerInput(buttonAlpha.value, currentPopTapped) {
                        if (buttonAlpha.value > 0.05f && !currentPopTapped) {
                            detectTapGestures {
                                currentPopTapped = true
                                popCount++
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "POP!",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = buttonAlpha.value)
                )
            }
        }

        Text(
            text = "Pops: $popCount • Misses: $misses/3 • Time: ${timerLeft}s",
            fontSize = 13.sp,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
        )
    }
}
