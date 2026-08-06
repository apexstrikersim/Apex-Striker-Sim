package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSlate
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.PitchGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TrophyGold

@Composable
fun JerseyNumberIcon(number: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(width = 56.dp, height = 62.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Full Jersey Contour Path
            val jerseyPath = Path().apply {
                moveTo(w * 0.32f, 0f)
                lineTo(w * 0.68f, 0f)
                lineTo(w * 0.90f, h * 0.12f)
                lineTo(w, h * 0.38f)
                lineTo(w * 0.82f, h * 0.44f)
                lineTo(w * 0.82f, h * 0.96f)
                quadraticTo(w * 0.5f, h, w * 0.18f, h * 0.96f)
                lineTo(w * 0.18f, h * 0.44f)
                lineTo(0f, h * 0.38f)
                lineTo(w * 0.10f, h * 0.12f)
                close()
            }

            // Fill jersey base
            drawPath(path = jerseyPath, color = DarkSlate)

            // Outer border outline
            drawPath(path = jerseyPath, color = PitchGreen, style = Stroke(width = 2.dp.toPx()))

            // V-Neck collar line
            val collarPath = Path().apply {
                moveTo(w * 0.35f, 0f)
                lineTo(w * 0.50f, h * 0.22f)
                lineTo(w * 0.65f, 0f)
            }
            drawPath(path = collarPath, color = TrophyGold, style = Stroke(width = 2.dp.toPx()))

            // Sleeve cuffs trim
            val leftCuff = Path().apply {
                moveTo(w * 0.03f, h * 0.26f)
                lineTo(0f, h * 0.38f)
                lineTo(w * 0.18f, h * 0.44f)
            }
            val rightCuff = Path().apply {
                moveTo(w * 0.97f, h * 0.26f)
                lineTo(w, h * 0.38f)
                lineTo(w * 0.82f, h * 0.44f)
            }
            drawPath(path = leftCuff, color = PitchGreen, style = Stroke(width = 1.5.dp.toPx()))
            drawPath(path = rightCuff, color = PitchGreen, style = Stroke(width = 1.5.dp.toPx()))
        }

        Text(
            text = number.toString(),
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            fontFamily = DisplayFontFamily,
            color = TextPrimary,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}
