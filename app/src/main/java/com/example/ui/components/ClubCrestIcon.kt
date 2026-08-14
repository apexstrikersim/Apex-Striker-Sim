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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Curated, traditional palettes — (base, secondary, trim). "trim" is used for
// the border stroke, the emblem, and the initials, so it must contrast with
// both base and secondary. Deliberately avoids neon/gradient combos.
private val CREST_PALETTES = listOf(
    Triple(Color(0xFF1B2A4A), Color(0xFFD4AF37), Color(0xFFFFFFFF)), // navy / gold / white
    Triple(Color(0xFF7A1F2B), Color(0xFFF2E9DC), Color(0xFFD4AF37)), // maroon / cream / gold
    Triple(Color(0xFF14532D), Color(0xFFFFFFFF), Color(0xFFD4AF37)), // forest green / white / gold
    Triple(Color(0xFF0B3C5D), Color(0xFFFFFFFF), Color(0xFF1B2A4A)), // steel blue / white / navy
    Triple(Color(0xFF1A1A1A), Color(0xFFC0392B), Color(0xFFFFFFFF)), // black / red / white
    Triple(Color(0xFF4B2E1E), Color(0xFFE8C97A), Color(0xFFF2E9DC)), // brown / tan / cream
    Triple(Color(0xFF2C2C54), Color(0xFF9AA5CE), Color(0xFFFFFFFF)), // indigo / lilac-grey / white
    Triple(Color(0xFF6B1E1E), Color(0xFFD9B382), Color(0xFFF2E9DC)), // brick red / sand / cream
    Triple(Color(0xFF1E3A8A), Color(0xFF60A5FA), Color(0xFFFFFFFF)), // royal blue / sky blue / white
    Triple(Color(0xFF5C1A2E), Color(0xFFD4AF37), Color(0xFF1A1A1A)), // burgundy / gold / black
    Triple(Color(0xFF0F5132), Color(0xFF1A1A1A), Color(0xFFFFFFFF)), // emerald / black / white
    Triple(Color(0xFF3B1F5C), Color(0xFFD4AF37), Color(0xFFFFFFFF)), // purple / gold / white
    Triple(Color(0xFF0E5A5A), Color(0xFFFFFFFF), Color(0xFF1B2A4A)), // teal / white / navy
    Triple(Color(0xFFA6192E), Color(0xFF1B2A4A), Color(0xFFFFFFFF)), // crimson / navy / white
    Triple(Color(0xFF3D4451), Color(0xFFC0392B), Color(0xFFFFFFFF)), // slate / red / white
    Triple(Color(0xFF4A5D23), Color(0xFFF2E9DC), Color(0xFFD4AF37))  // olive / cream / gold
)

fun getClubColors(clubId: Int): Pair<Color, Color> {
    val rng = Random(clubId)
    val (base, secondary, _) = CREST_PALETTES[rng.nextInt(CREST_PALETTES.size)]
    return Pair(base, secondary)
}

private enum class CrestShape { CIRCLE, SHIELD, HEXAGON, PENTAGON, ROUNDED_SQUARE }
private enum class CrestPattern { SOLID, VERTICAL_SPLIT, HORIZONTAL_HALVES, DIAGONAL_SPLIT, QUARTERED }
private enum class CrestEmblem { STAR, CHEVRON, DIAMOND_BALL, TREE, WREATH_BAR, RING, CROSS, ARCH }

@Composable
fun ClubCrestIcon(
    clubId: Int,
    clubName: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val rng = remember(clubId) { Random(clubId) }
    val (base, secondary, trim) = remember(clubId) { CREST_PALETTES[rng.nextInt(CREST_PALETTES.size)] }
    val shape = remember(clubId) { CrestShape.entries[rng.nextInt(CrestShape.entries.size)] }
    val pattern = remember(clubId) { CrestPattern.entries[rng.nextInt(CrestPattern.entries.size)] }
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
                    CrestShape.PENTAGON -> {
                        for (i in 0 until 5) {
                            val angle = (2 * Math.PI / 5) * i - Math.PI / 2
                            val x = w / 2 + (w / 2) * cos(angle).toFloat()
                            val y = h / 2 + (h / 2) * sin(angle).toFloat()
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                        close()
                    }
                    CrestShape.ROUNDED_SQUARE -> {
                        addRoundRect(
                            RoundRect(0f, 0f, w, h, CornerRadius(w * 0.22f, h * 0.22f))
                        )
                    }
                }
            }

            // Two-color fill pattern, clipped to the outline shape.
            clipPath(outline) {
                when (pattern) {
                    CrestPattern.SOLID ->
                        drawRect(color = base, size = this.size)
                    CrestPattern.VERTICAL_SPLIT -> {
                        drawRect(color = base, size = Size(w / 2f, h))
                        drawRect(color = secondary, topLeft = Offset(w / 2f, 0f), size = Size(w / 2f, h))
                    }
                    CrestPattern.HORIZONTAL_HALVES -> {
                        drawRect(color = base, size = Size(w, h / 2f))
                        drawRect(color = secondary, topLeft = Offset(0f, h / 2f), size = Size(w, h / 2f))
                    }
                    CrestPattern.DIAGONAL_SPLIT -> {
                        drawRect(color = secondary, size = this.size)
                        val diag = Path().apply {
                            moveTo(0f, 0f); lineTo(w, 0f); lineTo(0f, h); close()
                        }
                        drawPath(diag, color = base)
                    }
                    CrestPattern.QUARTERED -> {
                        drawRect(color = base, topLeft = Offset(0f, 0f), size = Size(w / 2f, h / 2f))
                        drawRect(color = secondary, topLeft = Offset(w / 2f, 0f), size = Size(w / 2f, h / 2f))
                        drawRect(color = secondary, topLeft = Offset(0f, h / 2f), size = Size(w / 2f, h / 2f))
                        drawRect(color = base, topLeft = Offset(w / 2f, h / 2f), size = Size(w / 2f, h / 2f))
                    }
                }
            }
            drawPath(outline, color = trim, style = Stroke(width = w * 0.045f))

            // Emblem, always centered in the upper 60% so the initials ribbon below has room.
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
                    drawPath(star, color = trim)
                }
                CrestEmblem.CHEVRON -> {
                    val chevron = Path().apply {
                        moveTo(cx - r, cy + r * 0.6f)
                        lineTo(cx, cy - r * 0.6f)
                        lineTo(cx + r, cy + r * 0.6f)
                    }
                    drawPath(chevron, color = trim, style = Stroke(width = w * 0.07f, cap = StrokeCap.Round))
                }
                CrestEmblem.DIAMOND_BALL -> {
                    val diamond = Path().apply {
                        moveTo(cx, cy - r)
                        lineTo(cx + r * 0.75f, cy)
                        lineTo(cx, cy + r)
                        lineTo(cx - r * 0.75f, cy)
                        close()
                    }
                    drawPath(diamond, color = trim)
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
                    drawPath(tree, color = trim)
                }
                CrestEmblem.WREATH_BAR -> {
                    drawCircle(color = trim, radius = r * 0.12f, center = Offset(cx, cy))
                    drawLine(trim, Offset(cx - r, cy), Offset(cx + r, cy), strokeWidth = w * 0.05f, cap = StrokeCap.Round)
                }
                CrestEmblem.RING ->
                    drawCircle(color = trim, radius = r, center = Offset(cx, cy), style = Stroke(width = w * 0.09f))
                CrestEmblem.CROSS -> {
                    drawLine(trim, Offset(cx, cy - r), Offset(cx, cy + r), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
                    drawLine(trim, Offset(cx - r * 0.7f, cy - r * 0.3f), Offset(cx + r * 0.7f, cy - r * 0.3f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
                }
                CrestEmblem.ARCH ->
                    drawArc(
                        color = trim,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(cx - r, cy - r * 0.2f),
                        size = Size(r * 2f, r * 1.3f),
                        style = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
                    )
            }
        }

        // Initials ribbon at the bottom third — a very common real-crest convention.
        Text(
            text = initials,
            color = trim,
            fontSize = (size.value * 0.24f).sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(y = size * 0.28f)
        )
    }
}
