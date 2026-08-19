package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect

/**
 * 1. SHOOTING DRILL: Penalty Box & Goalmouth Finishing Ground
 * Features:
 * - Stadium backdrop with training wall / stands at the top
 * - Realistic 3D goal structure with crossbar, side posts, and clean, undistorted net mesh
 * - 6-yard box, 18-yard penalty area, penalty spot directly under the ball, and penalty arc
 * - Lush horizontal mowing stripes tuned for goalmouth perspective
 */
fun DrawScope.drawShootingDrillBackground(
    goalLeft: Float,
    goalRight: Float,
    goalTop: Float,
    goalBottom: Float,
    penaltySpot: Offset
) {
    val w = size.width
    val h = size.height

    // 1. Top Backdrop (Training facility / stadium stands / wall behind the goal)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F172A), // Dark navy stadium backdrop
                Color(0xFF1E293B),
                Color(0xFF153E26)  // Transition to turf
            ),
            startY = 0f,
            endY = goalTop + 10f
        ),
        topLeft = Offset(0f, 0f),
        size = Size(w, goalTop + 10f)
    )

    // Stadium advertising / training barrier board right behind the goal line
    val barrierY = goalTop - 14f
    if (barrierY > 0) {
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF334155), Color(0xFF475569), Color(0xFF334155))
            ),
            topLeft = Offset(0f, barrierY),
            size = Size(w, 14f)
        )
        drawLine(
            color = Color(0xFF94A3B8).copy(alpha = 0.6f),
            start = Offset(0f, barrierY),
            end = Offset(w, barrierY),
            strokeWidth = 1.5f
        )
    }

    // 2. Pitch Turf with alternating horizontal mower stripes
    val turfTop = goalTop
    val turfHeight = h - turfTop
    val stripeCount = 10
    val stripeH = turfHeight / stripeCount

    for (i in 0 until stripeCount) {
        val y = turfTop + i * stripeH
        val stripeColor = if (i % 2 == 0) Color(0xFF1B4D2E) else Color(0xFF225E38)
        drawRect(
            color = stripeColor,
            topLeft = Offset(0f, y),
            size = Size(w, stripeH)
        )
    }

    // Subtle pitch lighting vignette
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
            center = Offset(w / 2f, h * 0.5f),
            radius = w * 0.85f
        )
    )

    val lineCol = Color.White.copy(alpha = 0.55f)
    val lineStroke = Stroke(width = 2.5f)

    // Goal Line (underneath the goal posts)
    drawLine(
        color = lineCol,
        start = Offset(14f, goalBottom),
        end = Offset(w - 14f, goalBottom),
        strokeWidth = 3f
    )

    // 6-Yard Box (Goal Area)
    val sixYardLeft = w * 0.22f
    val sixYardRight = w * 0.78f
    val sixYardBottom = goalBottom + (penaltySpot.y - goalBottom) * 0.45f
    drawRect(
        color = lineCol,
        topLeft = Offset(sixYardLeft, goalBottom),
        size = Size(sixYardRight - sixYardLeft, sixYardBottom - goalBottom),
        style = lineStroke
    )

    // 18-Yard Box (Penalty Area)
    val boxLeft = w * 0.08f
    val boxRight = w * 0.92f
    val boxBottom = penaltySpot.y + (penaltySpot.y - goalBottom) * 0.18f
    drawRect(
        color = lineCol,
        topLeft = Offset(boxLeft, goalBottom),
        size = Size(boxRight - boxLeft, boxBottom - goalBottom),
        style = lineStroke
    )

    // Penalty Spot (Crisp white circle directly on penalty spot location)
    // Outer chalk ring
    drawCircle(
        color = Color.White.copy(alpha = 0.4f),
        radius = 7.5f,
        center = penaltySpot,
        style = Stroke(width = 1.5f)
    )
    // Solid chalk core
    drawCircle(
        color = Color.White,
        radius = 4f,
        center = penaltySpot
    )

    // Penalty Arc (D-Box) curving below the 18-yard box line
    val arcRadius = (boxRight - boxLeft) * 0.24f
    drawArc(
        color = lineCol,
        startAngle = 24f,
        sweepAngle = 132f,
        useCenter = false,
        topLeft = Offset(penaltySpot.x - arcRadius, penaltySpot.y - arcRadius),
        size = Size(arcRadius * 2f, arcRadius * 2f),
        style = lineStroke
    )

    // 3. Realistic 3D Goal Structure & Clean, Undistorted Netting
    val goalWidth = goalRight - goalLeft
    val goalHeight = goalBottom - goalTop

    // Net interior drawn with clean clipping so it never distorts or bleeds
    clipRect(left = goalLeft, top = goalTop, right = goalRight, bottom = goalBottom) {
        // Deep realistic 3D net interior gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF09160E), // Deep shadow beneath crossbar
                    Color(0xFF10281A),
                    Color(0xFF173D26)
                ),
                startY = goalTop,
                endY = goalBottom
            ),
            topLeft = Offset(goalLeft, goalTop),
            size = Size(goalWidth, goalHeight)
        )

        // Clean, undistorted diamond netting pattern
        val netSpacing = 12f
        val netColor = Color.White.copy(alpha = 0.30f)
        val span = goalWidth + goalHeight

        // Diagonal strings (\)
        var diag = -goalHeight
        while (diag <= span) {
            drawLine(
                color = netColor,
                start = Offset(goalLeft + diag, goalTop),
                end = Offset(goalLeft + diag + goalHeight, goalBottom),
                strokeWidth = 1f
            )
            diag += netSpacing
        }

        // Diagonal strings (/)
        diag = -goalHeight
        while (diag <= span) {
            drawLine(
                color = netColor,
                start = Offset(goalLeft + diag, goalBottom),
                end = Offset(goalLeft + diag + goalHeight, goalTop),
                strokeWidth = 1f
            )
            diag += netSpacing
        }

        // Subtle horizontal tension cords
        for (i in 1..4) {
            val cordY = goalTop + (goalHeight / 5f) * i
            drawLine(
                color = Color.White.copy(alpha = 0.18f),
                start = Offset(goalLeft, cordY),
                end = Offset(goalRight, cordY),
                strokeWidth = 1f
            )
        }

        // Internal depth shadow under crossbar and posts
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent),
                startY = goalTop,
                endY = goalTop + 16f
            ),
            topLeft = Offset(goalLeft, goalTop),
            size = Size(goalWidth, 16f)
        )
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent),
                startX = goalLeft,
                endX = goalLeft + 12f
            ),
            topLeft = Offset(goalLeft, goalTop),
            size = Size(12f, goalHeight)
        )
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
                startX = goalRight - 12f,
                endX = goalRight
            ),
            topLeft = Offset(goalRight - 12f, goalTop),
            size = Size(12f, goalHeight)
        )
    }

    // Goal White Posts & Crossbar (3D Metallic finish)
    val postThick = 6f

    // Top Crossbar
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White, Color(0xFFF1F5F9), Color(0xFFCBD5E1))
        ),
        topLeft = Offset(goalLeft - postThick / 2f, goalTop - postThick / 2f),
        size = Size(goalWidth + postThick, postThick)
    )

    // Left Post
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.White, Color(0xFFE2E8F0), Color(0xFF94A3B8))
        ),
        topLeft = Offset(goalLeft - postThick / 2f, goalTop),
        size = Size(postThick, goalHeight)
    )

    // Right Post
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color(0xFF94A3B8), Color(0xFFE2E8F0), Color.White)
        ),
        topLeft = Offset(goalRight - postThick / 2f, goalTop),
        size = Size(postThick, goalHeight)
    )

    // Post corner joint highlights
    drawCircle(
        color = Color.White,
        radius = postThick * 0.75f,
        center = Offset(goalLeft, goalTop)
    )
    drawCircle(
        color = Color.White,
        radius = postThick * 0.75f,
        center = Offset(goalRight, goalTop)
    )
}

/**
 * 2. PASSING DRILL: Midfield Rondo & Precision Passing Grid
 * Features:
 * - Checkered grass pattern typical of elite training facilities
 * - Central passing circle with tactical quadrant divisions
 * - Precision concentric passing range rings around target post
 * - 4 Corner training cones (fluorescent orange/yellow) marking the passing grid zone
 * - Faint dashed passing corridors directing visual flow
 */
fun DrawScope.drawPassingDrillBackground() {
    val w = size.width
    val h = size.height

    // 1. Checkered Turf Grid (4 columns x 6 rows)
    val cols = 4
    val rows = 6
    val cellW = w / cols
    val cellH = h / rows

    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val isAlt = (r + c) % 2 == 0
            val color = if (isAlt) Color(0xFF1B4D2E) else Color(0xFF225E38)
            drawRect(
                color = color,
                topLeft = Offset(c * cellW, r * cellH),
                size = Size(cellW, cellH)
            )
        }
    }

    // Vignette for depth
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.22f)),
            center = Offset(w / 2f, h / 2f),
            radius = w * 0.75f
        )
    )

    val gridLineColor = Color.White.copy(alpha = 0.35f)
    val gridStroke = Stroke(width = 1.8f)
    val m = 18f

    // Outer Training Box Boundary
    drawRect(
        color = gridLineColor,
        topLeft = Offset(m, m),
        size = Size(w - 2 * m, h - 2 * m),
        style = gridStroke
    )

    // Midfield / Rondo Center Circle
    val center = Offset(w / 2f, h * 0.42f)
    val rondoRadius = (w - 2 * m) * 0.35f
    drawCircle(
        color = gridLineColor,
        radius = rondoRadius,
        center = center,
        style = gridStroke
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.5f),
        radius = 3.5f,
        center = center
    )

    // Inner Precision Passing Ring
    drawCircle(
        color = Color(0xFF38BDF8).copy(alpha = 0.25f),
        radius = rondoRadius * 0.55f,
        center = center,
        style = Stroke(width = 1.5f)
    )

    // Tactical Passing Quadrant Axes
    drawLine(
        color = gridLineColor.copy(alpha = 0.2f),
        start = Offset(m, center.y),
        end = Offset(w - m, center.y),
        strokeWidth = 1.5f
    )
    drawLine(
        color = gridLineColor.copy(alpha = 0.2f),
        start = Offset(w / 2f, m),
        end = Offset(w / 2f, h - m),
        strokeWidth = 1.5f
    )

    // Target Landing Zone Range Ring at top
    val targetAreaY = h * 0.22f
    drawCircle(
        color = Color(0xFF60A5FA).copy(alpha = 0.15f),
        radius = 58f,
        center = Offset(w * 0.5f, targetAreaY),
        style = Stroke(width = 2f)
    )

    // Corner Cones (High-visibility Fluorescent Orange & Yellow Training Cones)
    fun drawTrainingCone(x: Float, y: Float, isOrange: Boolean) {
        val coneColor = if (isOrange) Color(0xFFF97316) else Color(0xFFFACC15)
        val shadowColor = Color.Black.copy(alpha = 0.3f)

        // Drop shadow
        drawCircle(color = shadowColor, radius = 7f, center = Offset(x + 1.5f, y + 2f))
        // Cone base
        drawCircle(color = coneColor, radius = 6.5f, center = Offset(x, y))
        // Inner ring
        drawCircle(color = Color.White, radius = 4f, center = Offset(x, y), style = Stroke(width = 1.2f))
        // Cone tip
        drawCircle(color = Color.White, radius = 1.5f, center = Offset(x, y))
    }

    // 4 Boundary Cones
    val coneInset = 28f
    drawTrainingCone(coneInset, coneInset, isOrange = true)
    drawTrainingCone(w - coneInset, coneInset, isOrange = true)
    drawTrainingCone(coneInset, h - coneInset, isOrange = false)
    drawTrainingCone(w - coneInset, h - coneInset, isOrange = false)

    // Midfield Cones
    drawTrainingCone(coneInset, center.y, isOrange = false)
    drawTrainingCone(w - coneInset, center.y, isOrange = false)
}

/**
 * 3. TECHNICAL DRILL: Agility Slalom Course & Dribbling Gauntlet
 * Features:
 * - Dynamic vertical slalom track with speed direction chevrons in the turf
 * - Dual side agility ladders on the left & right margins
 * - Hazard boundary lines & start/finish safety stripes
 * - High-contrast training turf designed for intense obstacle navigation
 */
fun DrawScope.drawObstacleDrillBackground() {
    val w = size.width
    val h = size.height

    // 1. Dark Athletic Training Turf Background
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF133E25),
                Color(0xFF1B4D2E),
                Color(0xFF113520)
            )
        ),
        topLeft = Offset.Zero,
        size = size
    )

    // Vertical Track Slopes / Lanes
    val laneW = w / 5f
    for (i in 0..4) {
        val alpha = if (i % 2 == 0) 0.08f else 0.02f
        drawRect(
            color = Color.White.copy(alpha = alpha),
            topLeft = Offset(i * laneW, 0f),
            size = Size(laneW, h)
        )
    }

    // 2. Agility Ladder Tracks on Left & Right Margins
    val ladderW = 20f
    val rungsCount = 14
    val rungSpacing = h / rungsCount
    val ladderColor = Color(0xFFFBBF24).copy(alpha = 0.35f)

    // Left Ladder rails & rungs
    val leftRailX1 = 8f
    val leftRailX2 = leftRailX1 + ladderW
    drawLine(color = ladderColor, start = Offset(leftRailX1, 0f), end = Offset(leftRailX1, h), strokeWidth = 2f)
    drawLine(color = ladderColor, start = Offset(leftRailX2, 0f), end = Offset(leftRailX2, h), strokeWidth = 2f)
    for (i in 0 until rungsCount) {
        val ry = i * rungSpacing + rungSpacing / 2f
        drawLine(color = ladderColor, start = Offset(leftRailX1, ry), end = Offset(leftRailX2, ry), strokeWidth = 1.8f)
    }

    // Right Ladder rails & rungs
    val rightRailX2 = w - 8f
    val rightRailX1 = rightRailX2 - ladderW
    drawLine(color = ladderColor, start = Offset(rightRailX1, 0f), end = Offset(rightRailX1, h), strokeWidth = 2f)
    drawLine(color = ladderColor, start = Offset(rightRailX2, 0f), end = Offset(rightRailX2, h), strokeWidth = 2f)
    for (i in 0 until rungsCount) {
        val ry = i * rungSpacing + rungSpacing / 2f
        drawLine(color = ladderColor, start = Offset(rightRailX1, ry), end = Offset(rightRailX2, ry), strokeWidth = 1.8f)
    }

    // 3. Central Downward Flow Chevrons (indicating obstacle rush)
    val chevronColor = Color.White.copy(alpha = 0.05f)
    val chevronCount = 5
    val chevronSpacing = h / chevronCount
    for (i in 0 until chevronCount) {
        val cy = i * chevronSpacing + chevronSpacing * 0.5f
        val path = Path().apply {
            moveTo(w * 0.35f, cy - 14f)
            lineTo(w * 0.5f, cy + 10f)
            lineTo(w * 0.65f, cy - 14f)
        }
        drawPath(path = path, color = chevronColor, style = Stroke(width = 5f))
    }

    // 4. Slalom Safety Boundary Lines
    val boundaryColor = Color.White.copy(alpha = 0.4f)
    drawLine(
        color = boundaryColor,
        start = Offset(leftRailX2 + 6f, 0f),
        end = Offset(leftRailX2 + 6f, h),
        strokeWidth = 2f
    )
    drawLine(
        color = boundaryColor,
        start = Offset(rightRailX1 - 6f, 0f),
        end = Offset(rightRailX1 - 6f, h),
        strokeWidth = 2f
    )

    // Start & Finish Hazard Stripes at Top and Bottom
    val topH = 14f
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFEF4444).copy(alpha = 0.3f),
                Color(0xFFF59E0B).copy(alpha = 0.3f),
                Color(0xFFEF4444).copy(alpha = 0.3f)
            )
        ),
        topLeft = Offset(leftRailX2 + 6f, 0f),
        size = Size(rightRailX1 - leftRailX2 - 12f, topH)
    )

    // Agility slalom cones scattered along the sides
    fun drawSlalomMarker(x: Float, y: Float) {
        drawCircle(color = Color.Black.copy(alpha = 0.3f), radius = 6f, center = Offset(x + 1f, y + 2f))
        drawCircle(color = Color(0xFFF97316), radius = 5.5f, center = Offset(x, y))
        drawCircle(color = Color.White, radius = 2f, center = Offset(x, y))
    }

    val slalomTrackY1 = h * 0.25f
    val slalomTrackY2 = h * 0.50f
    val slalomTrackY3 = h * 0.75f
    drawSlalomMarker(leftRailX2 + 14f, slalomTrackY1)
    drawSlalomMarker(rightRailX1 - 14f, slalomTrackY2)
    drawSlalomMarker(leftRailX2 + 14f, slalomTrackY3)
}
