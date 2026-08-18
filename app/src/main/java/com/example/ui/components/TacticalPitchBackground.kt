package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Draws a full football pitch (grass stripes + all standard markings) into the
 * given DrawScope. Extracted from MatchScreen's Key Moment tactical view so
 * training drills can reuse the exact same look.
 */
fun DrawScope.drawTacticalPitch() {
    val w = size.width
    val h = size.height

    // Grass stripes
    val stripeW = w / 8f
    for (i in 0..7) {
        val sColor = if (i % 2 == 0) Color(0xFF1B4D2E) else Color(0xFF1E5432)
        drawRect(color = sColor, topLeft = Offset(i * stripeW, 0f), size = Size(stripeW, h))
    }

    val lineCol = Color.White.copy(alpha = 0.45f)
    val lineStroke = Stroke(width = 2.2f)
    val m = 12f

    // Boundary
    drawRect(color = lineCol, topLeft = Offset(m, m), size = Size(w - 2 * m, h - 2 * m), style = lineStroke)

    // Halfway line
    drawLine(color = lineCol, start = Offset(w / 2f, m), end = Offset(w / 2f, h - m), strokeWidth = 2.2f)

    // Center circle + spot
    val centerR = (h - 2 * m) * 0.22f
    drawCircle(color = lineCol, center = Offset(w / 2f, h / 2f), radius = centerR, style = lineStroke)
    drawCircle(color = lineCol, center = Offset(w / 2f, h / 2f), radius = 2.5f)

    // 18-yard boxes
    val boxW = (w - 2 * m) * 0.17f
    val boxH = (h - 2 * m) * 0.58f
    drawRect(color = lineCol, topLeft = Offset(m, h / 2f - boxH / 2f), size = Size(boxW, boxH), style = lineStroke)
    drawRect(color = lineCol, topLeft = Offset(w - m - boxW, h / 2f - boxH / 2f), size = Size(boxW, boxH), style = lineStroke)

    // 6-yard boxes
    val gBoxW = boxW * 0.36f
    val gBoxH = boxH * 0.44f
    drawRect(color = lineCol, topLeft = Offset(m, h / 2f - gBoxH / 2f), size = Size(gBoxW, gBoxH), style = lineStroke)
    drawRect(color = lineCol, topLeft = Offset(w - m - gBoxW, h / 2f - gBoxH / 2f), size = Size(gBoxW, gBoxH), style = lineStroke)

    // Penalty spots + arcs
    val leftPenSpotX = m + boxW * 0.65f
    drawCircle(color = lineCol, center = Offset(leftPenSpotX, h / 2f), radius = 2.5f)
    drawArc(
        color = lineCol, startAngle = -55f, sweepAngle = 110f, useCenter = false,
        topLeft = Offset(leftPenSpotX - centerR * 0.7f, h / 2f - centerR * 0.7f),
        size = Size(centerR * 1.4f, centerR * 1.4f), style = lineStroke
    )
    val rightPenSpotX = w - m - boxW * 0.65f
    drawCircle(color = lineCol, center = Offset(rightPenSpotX, h / 2f), radius = 2.5f)
    drawArc(
        color = lineCol, startAngle = 125f, sweepAngle = 110f, useCenter = false,
        topLeft = Offset(rightPenSpotX - centerR * 0.7f, h / 2f - centerR * 0.7f),
        size = Size(centerR * 1.4f, centerR * 1.4f), style = lineStroke
    )

    // Corner arcs
    val cornerR = 10f
    drawArc(color = lineCol, startAngle = 0f, sweepAngle = 90f, useCenter = false, topLeft = Offset(m - cornerR, m - cornerR), size = Size(cornerR * 2, cornerR * 2), style = lineStroke)
    drawArc(color = lineCol, startAngle = 90f, sweepAngle = 90f, useCenter = false, topLeft = Offset(w - m - cornerR, m - cornerR), size = Size(cornerR * 2, cornerR * 2), style = lineStroke)
    drawArc(color = lineCol, startAngle = 270f, sweepAngle = 90f, useCenter = false, topLeft = Offset(m - cornerR, h - m - cornerR), size = Size(cornerR * 2, cornerR * 2), style = lineStroke)
    drawArc(color = lineCol, startAngle = 180f, sweepAngle = 90f, useCenter = false, topLeft = Offset(w - m - cornerR, h - m - cornerR), size = Size(cornerR * 2, cornerR * 2), style = lineStroke)

    // Goal posts
    val gDepth = 6f
    val gHeight = gBoxH * 0.8f
    drawRect(color = Color.White.copy(alpha = 0.35f), topLeft = Offset(m - gDepth, h / 2f - gHeight / 2f), size = Size(gDepth, gHeight), style = Stroke(width = 1.5f))
    drawRect(color = Color.White.copy(alpha = 0.35f), topLeft = Offset(w - m, h / 2f - gHeight / 2f), size = Size(gDepth, gHeight), style = Stroke(width = 1.5f))
}
