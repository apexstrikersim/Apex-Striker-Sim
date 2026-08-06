package com.example.ui.tabs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.PlayerEntity
import com.example.data.formatSeasonYear
import com.example.ui.CareerViewModel
import com.example.ui.theme.*

@Composable
fun CompetitionTrophyIcon(
    title: String,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier
) {
    val compType = when {
        title.contains("Player of the Season", ignoreCase = true) || title.contains("Boot", ignoreCase = true) || title.contains("Ballon", ignoreCase = true) || title.contains("Award", ignoreCase = true) -> "AWARD"
        title.contains("Champions League", ignoreCase = true) -> "CL"
        title.contains("Europa League", ignoreCase = true) -> "EL"
        title.contains("Conference League", ignoreCase = true) -> "CONF"
        title.contains("Super Cup", ignoreCase = true) -> "SUPER"
        title.contains("League", ignoreCase = true) -> "LEAGUE"
        title.contains("Cup", ignoreCase = true) -> "CUP"
        else -> "LEAGUE"
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when (compType) {
            "CL" -> {
                // Champions League "Big Ears" Starball Cup
                val primaryColor = if (isUnlocked) Color(0xFFE0F7FA) else Color(0xFF37474F)
                val strokeColor = if (isUnlocked) Color(0xFF00E5FF) else Color(0xFF78909C)
                val accentColor = if (isUnlocked) TrophyGold else Color(0xFF546E7A)

                // Base
                drawRoundRect(
                    color = strokeColor,
                    topLeft = Offset(w * 0.3f, h * 0.82f),
                    size = Size(w * 0.4f, h * 0.12f),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
                // Stem
                drawRect(
                    color = primaryColor,
                    topLeft = Offset(w * 0.42f, h * 0.68f),
                    size = Size(w * 0.16f, h * 0.15f)
                )

                // Main Bowl
                val bowlPath = Path().apply {
                    moveTo(w * 0.22f, h * 0.22f)
                    lineTo(w * 0.78f, h * 0.22f)
                    cubicTo(w * 0.78f, h * 0.45f, w * 0.68f, h * 0.68f, w * 0.5f, h * 0.7f)
                    cubicTo(w * 0.32f, h * 0.68f, w * 0.22f, h * 0.45f, w * 0.22f, h * 0.22f)
                    close()
                }
                drawPath(path = bowlPath, color = primaryColor)
                drawPath(path = bowlPath, color = strokeColor, style = Stroke(width = 1.8.dp.toPx()))

                // Left "Big Ear" Handle
                val leftHandle = Path().apply {
                    moveTo(w * 0.23f, h * 0.26f)
                    cubicTo(w * 0.04f, h * 0.18f, w * 0.02f, h * 0.52f, w * 0.28f, h * 0.58f)
                }
                drawPath(path = leftHandle, color = strokeColor, style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round))

                // Right "Big Ear" Handle
                val rightHandle = Path().apply {
                    moveTo(w * 0.77f, h * 0.26f)
                    cubicTo(w * 0.96f, h * 0.18f, w * 0.98f, h * 0.52f, w * 0.72f, h * 0.58f)
                }
                drawPath(path = rightHandle, color = strokeColor, style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round))

                // Star emblem in center
                drawCircle(color = accentColor, radius = w * 0.07f, center = Offset(w * 0.5f, h * 0.42f))
            }
            "LEAGUE" -> {
                // Domestic Championship Crown Trophy
                val goldColor = if (isUnlocked) TrophyGold else Color(0xFF455A64)
                val highlightColor = if (isUnlocked) Color(0xFFFFF59D) else Color(0xFF78909C)

                // Base
                drawRoundRect(
                    color = goldColor,
                    topLeft = Offset(w * 0.25f, h * 0.8f),
                    size = Size(w * 0.5f, h * 0.14f),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
                // Stem
                drawRect(
                    color = highlightColor,
                    topLeft = Offset(w * 0.4f, h * 0.65f),
                    size = Size(w * 0.2f, h * 0.16f)
                )

                // Cup Body
                val cupPath = Path().apply {
                    moveTo(w * 0.2f, h * 0.3f)
                    lineTo(w * 0.8f, h * 0.3f)
                    lineTo(w * 0.65f, h * 0.66f)
                    lineTo(w * 0.35f, h * 0.66f)
                    close()
                }
                drawPath(path = cupPath, color = goldColor)
                drawPath(path = cupPath, color = highlightColor, style = Stroke(width = 1.5.dp.toPx()))

                // Crown points on top
                val crownPath = Path().apply {
                    moveTo(w * 0.22f, h * 0.3f)
                    lineTo(w * 0.28f, h * 0.15f)
                    lineTo(w * 0.4f, h * 0.25f)
                    lineTo(w * 0.5f, h * 0.1f)
                    lineTo(w * 0.6f, h * 0.25f)
                    lineTo(w * 0.72f, h * 0.15f)
                    lineTo(w * 0.78f, h * 0.3f)
                    close()
                }
                drawPath(path = crownPath, color = highlightColor)

                // Side loop handles
                drawArc(
                    color = highlightColor,
                    startAngle = 120f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.08f, h * 0.32f),
                    size = Size(w * 0.22f, h * 0.26f),
                    style = Stroke(width = 2.dp.toPx())
                )
                drawArc(
                    color = highlightColor,
                    startAngle = -60f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.7f, h * 0.32f),
                    size = Size(w * 0.22f, h * 0.26f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            "EL" -> {
                // Europa League Faceted Chalice Trophy
                val copperColor = if (isUnlocked) Color(0xFFFF9800) else Color(0xFF37474F)
                val silverColor = if (isUnlocked) Color(0xFFFFD54F) else Color(0xFF616161)
                val darkBase = if (isUnlocked) Color(0xFF3E2723) else Color(0xFF212121)

                // Heavy base
                drawRect(color = darkBase, topLeft = Offset(w * 0.22f, h * 0.75f), size = Size(w * 0.56f, h * 0.18f))

                // Octagonal/tapering chalice body
                val chalicePath = Path().apply {
                    moveTo(w * 0.26f, h * 0.12f)
                    lineTo(w * 0.74f, h * 0.12f)
                    lineTo(w * 0.64f, h * 0.55f)
                    lineTo(w * 0.56f, h * 0.75f)
                    lineTo(w * 0.44f, h * 0.75f)
                    lineTo(w * 0.36f, h * 0.55f)
                    close()
                }
                drawPath(path = chalicePath, color = copperColor)
                drawPath(path = chalicePath, color = silverColor, style = Stroke(width = 1.5.dp.toPx()))

                // Facet lines
                drawLine(color = silverColor, start = Offset(w * 0.42f, h * 0.12f), end = Offset(w * 0.45f, h * 0.75f), strokeWidth = 1.2.dp.toPx())
                drawLine(color = silverColor, start = Offset(w * 0.58f, h * 0.12f), end = Offset(w * 0.55f, h * 0.75f), strokeWidth = 1.2.dp.toPx())
            }
            "CONF" -> {
                // Conference League Ribbon Column Trophy
                val emeraldColor = if (isUnlocked) PitchGreen else Color(0xFF2E7D32).copy(alpha = 0.5f)
                val silverBody = if (isUnlocked) Color(0xFFECEFF1) else Color(0xFF37474F)
                val ribbonColor = if (isUnlocked) Color(0xFF69F0AE) else Color(0xFF546E7A)

                // Round base
                drawOval(color = silverBody, topLeft = Offset(w * 0.25f, h * 0.8f), size = Size(w * 0.5f, h * 0.12f))

                // Main column vessel
                val colPath = Path().apply {
                    moveTo(w * 0.28f, h * 0.18f)
                    lineTo(w * 0.72f, h * 0.18f)
                    lineTo(w * 0.62f, h * 0.81f)
                    lineTo(w * 0.38f, h * 0.81f)
                    close()
                }
                drawPath(path = colPath, color = silverBody)
                drawPath(path = colPath, color = emeraldColor, style = Stroke(width = 1.5.dp.toPx()))

                // Vertical curved ribbons
                val r1 = Path().apply { moveTo(w * 0.35f, h * 0.18f); cubicTo(w * 0.45f, h * 0.4f, w * 0.3f, h * 0.6f, w * 0.42f, h * 0.81f) }
                val r2 = Path().apply { moveTo(w * 0.5f, h * 0.18f); cubicTo(w * 0.6f, h * 0.4f, w * 0.45f, h * 0.6f, w * 0.54f, h * 0.81f) }
                val r3 = Path().apply { moveTo(w * 0.65f, h * 0.18f); cubicTo(w * 0.72f, h * 0.4f, w * 0.58f, h * 0.6f, w * 0.62f, h * 0.81f) }
                drawPath(path = r1, color = ribbonColor, style = Stroke(width = 1.5.dp.toPx()))
                drawPath(path = r2, color = ribbonColor, style = Stroke(width = 1.5.dp.toPx()))
                drawPath(path = r3, color = ribbonColor, style = Stroke(width = 1.5.dp.toPx()))
            }
            "AWARD" -> {
                // Player of the Season / Individual Golden Award Trophy
                val goldColor = if (isUnlocked) TrophyGold else Color(0xFF455A64)
                val highlightColor = if (isUnlocked) Color(0xFFFFF176) else Color(0xFF78909C)
                val darkBase = if (isUnlocked) Color(0xFF2A2100) else Color(0xFF212121)
                val accentGlow = if (isUnlocked) Color(0xFFFFD54F) else Color(0xFF546E7A)

                // 1. Sleek Pedestal Base
                drawRoundRect(
                    color = darkBase,
                    topLeft = Offset(w * 0.22f, h * 0.78f),
                    size = Size(w * 0.56f, h * 0.16f),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
                drawRoundRect(
                    color = goldColor,
                    topLeft = Offset(w * 0.22f, h * 0.78f),
                    size = Size(w * 0.56f, h * 0.16f),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                    style = Stroke(width = 1.2.dp.toPx())
                )

                // 2. Sculpted Tapered Pillar / Stem
                val stemPath = Path().apply {
                    moveTo(w * 0.38f, h * 0.78f)
                    lineTo(w * 0.62f, h * 0.78f)
                    lineTo(w * 0.55f, h * 0.46f)
                    lineTo(w * 0.45f, h * 0.46f)
                    close()
                }
                drawPath(path = stemPath, color = goldColor)
                drawPath(path = stemPath, color = highlightColor, style = Stroke(width = 1.2.dp.toPx()))

                // 3. Glowing Backing Ring / Halo behind Star
                drawCircle(
                    color = accentGlow.copy(alpha = if (isUnlocked) 0.35f else 0.15f),
                    radius = w * 0.26f,
                    center = Offset(w * 0.5f, h * 0.28f)
                )

                // 4. 5-Point Golden Star Award Top
                val centerX = w * 0.5f
                val centerY = h * 0.28f
                val outerRadius = w * 0.24f
                val innerRadius = w * 0.1f

                val starPath = Path().apply {
                    val numPoints = 5
                    val angleStep = Math.PI / numPoints
                    var angle = -Math.PI / 2
                    for (i in 0 until (numPoints * 2)) {
                        val r = if (i % 2 == 0) outerRadius else innerRadius
                        val x = (centerX + r * kotlin.math.cos(angle)).toFloat()
                        val y = (centerY + r * kotlin.math.sin(angle)).toFloat()
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                        angle += angleStep
                    }
                    close()
                }
                drawPath(path = starPath, color = highlightColor)
                drawPath(path = starPath, color = goldColor, style = Stroke(width = 1.5.dp.toPx()))

                // 5. Shining Center Gem Dot
                drawCircle(
                    color = if (isUnlocked) Color.White else Color(0xFFB0BEC5),
                    radius = w * 0.05f,
                    center = Offset(centerX, centerY)
                )
            }
            "SUPER" -> {
                // European Super Cup Slender Goblet
                val platColor = if (isUnlocked) Color(0xFFF5F5F5) else Color(0xFF37474F)
                val blueAccent = if (isUnlocked) Color(0xFF40C4FF) else Color(0xFF546E7A)
                val strokeCol = if (isUnlocked) Color(0xFFB0BEC5) else Color(0xFF78909C)

                // Double pedestal
                drawRoundRect(color = platColor, topLeft = Offset(w * 0.28f, h * 0.84f), size = Size(w * 0.44f, h * 0.09f), cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()))
                drawRoundRect(color = blueAccent, topLeft = Offset(w * 0.34f, h * 0.76f), size = Size(w * 0.32f, h * 0.08f), cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx()))

                // Slender cup
                val superPath = Path().apply {
                    moveTo(w * 0.28f, h * 0.12f)
                    lineTo(w * 0.72f, h * 0.12f)
                    cubicTo(w * 0.68f, h * 0.45f, w * 0.56f, h * 0.68f, w * 0.5f, h * 0.76f)
                    cubicTo(w * 0.44f, h * 0.68f, w * 0.32f, h * 0.45f, w * 0.28f, h * 0.12f)
                    close()
                }
                drawPath(path = superPath, color = platColor)
                drawPath(path = superPath, color = strokeCol, style = Stroke(width = 1.5.dp.toPx()))

                // Thin tall handles extending down
                val leftSuperH = Path().apply { moveTo(w * 0.28f, h * 0.14f); cubicTo(w * 0.12f, h * 0.3f, w * 0.2f, h * 0.65f, w * 0.42f, h * 0.72f) }
                val rightSuperH = Path().apply { moveTo(w * 0.72f, h * 0.14f); cubicTo(w * 0.88f, h * 0.3f, w * 0.8f, h * 0.65f, w * 0.58f, h * 0.72f) }
                drawPath(path = leftSuperH, color = blueAccent, style = Stroke(width = 1.8.dp.toPx()))
                drawPath(path = rightSuperH, color = blueAccent, style = Stroke(width = 1.8.dp.toPx()))
            }
            else -> {
                // Domestic Cup / Generic Cup
                val cupGold = if (isUnlocked) TrophyGold else Color(0xFF455A64)
                val cupStroke = if (isUnlocked) Color(0xFFFFF176) else Color(0xFF78909C)

                drawRect(color = cupGold, topLeft = Offset(w * 0.3f, h * 0.8f), size = Size(w * 0.4f, h * 0.12f))
                val baseCup = Path().apply {
                    moveTo(w * 0.25f, h * 0.22f)
                    lineTo(w * 0.75f, h * 0.22f)
                    lineTo(w * 0.62f, h * 0.62f)
                    lineTo(w * 0.38f, h * 0.62f)
                    close()
                }
                drawPath(path = baseCup, color = cupGold)
                drawPath(path = baseCup, color = cupStroke, style = Stroke(width = 1.5.dp.toPx()))
                drawArc(color = cupStroke, startAngle = 90f, sweepAngle = 180f, useCenter = false, topLeft = Offset(w * 0.1f, h * 0.25f), size = Size(w * 0.22f, h * 0.3f), style = Stroke(width = 2.dp.toPx()))
                drawArc(color = cupStroke, startAngle = -90f, sweepAngle = 180f, useCenter = false, topLeft = Offset(w * 0.68f, h * 0.25f), size = Size(w * 0.22f, h * 0.3f), style = Stroke(width = 2.dp.toPx()))
            }
        }
    }
}

@Composable
fun TrophiesTab(viewModel: CareerViewModel, player: PlayerEntity) {
    val trophies by viewModel.trophiesFlow.collectAsStateWithLifecycle()

    val myTrophies = remember(trophies, player.name) {
        trophies.filter { it.playerName == player.name && it.generation == player.generation }
    }

    // Trophies count
    val potsTrophies = myTrophies.filter { it.competitionName.contains("Player of the Season", ignoreCase = true) || it.competitionName.contains("Award", ignoreCase = true) }
    val youthTrophies = myTrophies.filter { it.competitionName.contains("Youth Academy League", ignoreCase = true) }
    val leagueTrophies = myTrophies.filter {
        it.competitionName.contains("League") &&
        !it.competitionName.contains("Champions") &&
        !it.competitionName.contains("Europa") &&
        !it.competitionName.contains("Conference") &&
        !it.competitionName.contains("Player of the Season") &&
        !it.competitionName.contains("Youth Academy League", ignoreCase = true)
    }
    val clTrophies = myTrophies.filter { it.competitionName == "Champions League" }
    val elTrophies = myTrophies.filter { it.competitionName == "Europa League" }
    val confTrophies = myTrophies.filter { it.competitionName == "Conference League" }
    val superTrophies = myTrophies.filter { it.competitionName == "European Super Cup" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("trophies_tab"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "TROPHY CABINET",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PitchGreen
            )
            Text(
                text = "Your generation's complete historic achievements",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        // Grid of trophies
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TrophyCard(title = "Player of the Season", count = potsTrophies.size, isMajor = true, modifier = Modifier.weight(1f))
                    TrophyCard(title = "Champions League", count = clTrophies.size, isMajor = true, modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TrophyCard(title = "Domestic League", count = leagueTrophies.size, isMajor = true, modifier = Modifier.weight(1f))
                    TrophyCard(title = "Youth Academy", count = youthTrophies.size, isMajor = false, modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TrophyCard(title = "Europa League", count = elTrophies.size, isMajor = false, modifier = Modifier.weight(1f))
                    TrophyCard(title = "Conference League", count = confTrophies.size, isMajor = false, modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TrophyCard(title = "European Super Cup", count = superTrophies.size, isMajor = false, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "TROPHY LISTINGS",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = PitchGreen
            )
        }

        if (myTrophies.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SportsCardBg),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(text = "No trophies unlocked yet. Keep competing!", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(myTrophies) { trophy ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SportsCardBg),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CompetitionTrophyIcon(
                                title = trophy.competitionName,
                                isUnlocked = true,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = trophy.competitionName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(text = trophy.clubName, fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                        Text(
                            text = formatSeasonYear(trophy.seasonYear),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = PitchGreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrophyCard(title: String, count: Int, isMajor: Boolean, modifier: Modifier = Modifier) {
    val isUnlocked = count > 0
    Card(
        colors = CardDefaults.cardColors(containerColor = SportsCardBg),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.border(
            1.dp,
            if (isUnlocked) (if (isMajor) TrophyGold else PitchGreen) else BorderColor.copy(alpha = 0.5f),
            RoundedCornerShape(12.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CompetitionTrophyIcon(
                title = title,
                isUnlocked = isUnlocked,
                modifier = Modifier.size(38.dp)
            )

            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isUnlocked) TextPrimary else TextSecondary
            )

            Text(
                text = "x$count",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = if (isUnlocked) (if (isMajor) TrophyGold else PitchGreen) else MutedGrey
            )
        }
    }
}
