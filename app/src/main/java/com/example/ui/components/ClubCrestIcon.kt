package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Curated, traditional two-tone palettes — deliberately avoids saturated/neon
// or gradient combos, which is what reads as "AI generated" rather than a
// real club's branding.
private val CREST_PALETTES = listOf(
    Color(0xFF1B2A4A) to Color(0xFFD4AF37), // navy / gold
    Color(0xFF7A1F2B) to Color(0xFFF2E9DC), // maroon / cream
    Color(0xFF14532D) to Color(0xFFFFFFFF), // forest green / white
    Color(0xFF0B3C5D) to Color(0xFFFFFFFF), // steel blue / white
    Color(0xFF1A1A1A) to Color(0xFFC0392B), // black / red
    Color(0xFF4B2E1E) to Color(0xFFE8C97A), // brown / tan
    Color(0xFF2C2C54) to Color(0xFF9AA5CE), // deep indigo / lilac-grey
    Color(0xFF6B1E1E) to Color(0xFFD9B382), // brick red / sand
)

private enum class CrestShape { CIRCLE, SHIELD, HEXAGON }
private enum class CrestEmblem { STAR, CHEVRON, DIAMOND_BALL, TREE, WREATH_BAR }

@Composable
fun ClubCrestIcon(
    clubId: Int,
    clubName: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val rng = remember(clubId) { Random(clubId) }
    val (base, accent) = remember(clubId) { CREST_PALETTES[rng.nextInt(CREST_PALETTES.size)] }
    val shape = remember(clubId) { CrestShape.entries[rng.nextInt(CrestShape.entries.size)] }
    val emblem = remember(clubId) { CrestEmblem.entries[rng.nextInt(CrestEmblem.entries.size)] }
    val initials = remember(clubName) {
        clubName.trim().split(" ").filter { it.isNotBlank() }
            .let { words ->
                if (words.size >= 2) "${words[0].first()}${words[1].first()}"
                else clubName.take(2)
            }.uppercase()
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val outline = Path().apply {
                when (shape) {
                    CrestShape.CIRCLE -> addOval(androidx.compose.ui.geometry.Rect(0f, 0f, w, h))
                    CrestShape.SHIELD -> {
                        moveTo(w * 0.5f, 0f)
                        lineTo(w * 0.95f, h * 0.18f)
                        lineTo(w * 0.95f, h * 0.55f)
                        cubicTo(w * 0.95f, h * 0.85f, w * 0.7f, h * 0.98f, w * 0.5f, h)
                        cubicTo(w * 0.3f, h * 0.98f, w * 0.05f, h * 0.85f, w * 0.05f, h * 0.55f)
                        lineTo(w * 0.05f, h * 0.18f)
                        close()
                    }
                    CrestShape.HEXAGON -> {
                        for (i in 0 until 6) {
                            val angle = Math.PI / 3 * i - Math.PI / 2
                            val x = w / 2 + (w / 2) * cos(angle).toFloat()
                            val y = h / 2 + (h / 2) * sin(angle).toFloat()
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                        close()
                    }
                }
            }
            drawPath(outline, color = base)
            drawPath(outline, color = accent, style = Stroke(width = w * 0.045f))

            // Simple bold emblem, always centered in the upper 60% so the
            // initials ribbon below has room.
            val cx = w / 2f
            val cy = h * 0.4f
            val r = w * 0.2f
            when (emblem) {
                CrestEmblem.STAR -> {
                    val star = Path()
                    for (i in 0 until 10) {
                        val angle = Math.PI / 5 * i - Math.PI / 2
                        val radius = if (i % 2 == 0) r else r * 0.45f
                        val x = cx + (radius * cos(angle)).toFloat()
                        val y = cy + (radius * sin(angle)).toFloat()
                        if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
                    }
                    star.close()
                    drawPath(star, color = accent)
                }
                CrestEmblem.CHEVRON -> {
                    val chevron = Path().apply {
                        moveTo(cx - r, cy + r * 0.6f)
                        lineTo(cx, cy - r * 0.6f)
                        lineTo(cx + r, cy + r * 0.6f)
                    }
                    drawPath(chevron, color = accent, style = Stroke(width = w * 0.07f, cap = StrokeCap.Round))
                }
                CrestEmblem.DIAMOND_BALL -> {
                    val diamond = Path().apply {
                        moveTo(cx, cy - r)
                        lineTo(cx + r * 0.75f, cy)
                        lineTo(cx, cy + r)
                        lineTo(cx - r * 0.75f, cy)
                        close()
                    }
                    drawPath(diamond, color = accent)
                }
                CrestEmblem.TREE -> {
                    val tree = Path().apply {
                        moveTo(cx, cy - r)
                        lineTo(cx + r * 0.7f, cy + r * 0.2f)
                        lineTo(cx - r * 0.7f, cy + r * 0.2f)
                        close()
                        moveTo(cx, cy + r * 0.1f)
                        lineTo(cx + r * 0.5f, cy + r * 0.6f)
                        lineTo(cx - r * 0.5f, cy + r * 0.6f)
                        close()
                    }
                    drawPath(tree, color = accent)
                }
                CrestEmblem.WREATH_BAR -> {
                    drawCircle(color = accent, radius = r * 0.12f, center = Offset(cx, cy))
                    drawLine(accent, Offset(cx - r, cy), Offset(cx + r, cy), strokeWidth = w * 0.05f, cap = StrokeCap.Round)
                }
            }
        }

        // Initials ribbon at the bottom third — a very common real-crest convention.
        Text(
            text = initials,
            color = accent,
            fontSize = (size.value * 0.24f).sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(y = size * 0.28f)
        )
    }
}
