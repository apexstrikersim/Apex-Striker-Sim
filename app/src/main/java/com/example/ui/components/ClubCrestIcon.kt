package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
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

// Maps a color word embedded in a club's name to a specific curated palette index,
// so clubs whose names encode a color identity (e.g. "Manchester Red", "FC Blaugrana")
// render with a matching crest instead of a purely random one. Matched as whole words
// only, so place-name substrings (e.g. "Terreblanche") never false-positive.
private val NAME_COLOR_TOKENS: Map<String, Int> = mapOf(
    "red" to 4,          // black / red / white  (CREST_PALETTES index 4)
    "rouge" to 4,
    "rojiblanco" to 4,
    "rossonera" to 4,
    "blue" to 8,         // royal blue / sky blue / white
    "bleu" to 8,
    "claret" to 1,       // maroon / cream / gold
    "blanco" to 0,       // navy / gold / white (closest neutral "clean" palette)
    "blanc" to 0,
    "blaugrana" to 12,   // teal/navy-leaning palette used as the closest blue+garnet stand-in
    "amarillo" to 2,     // forest green / white / gold — swap below if a true yellow palette is added
    "gelb" to 2,
    "verdiblanco" to 2,  // forest green / white / gold
    "nerazzurro" to 8    // royal blue / sky blue / white (blue half of black/blue)
)

private fun colorTokenPaletteIndex(clubName: String): Int? {
    val words = clubName.lowercase().split(" ", "-", "í", "á", "ó").map { it.trim() }
    for (word in words) {
        NAME_COLOR_TOKENS[word]?.let { return it }
    }
    return null
}

fun getClubColors(clubId: Int, clubName: String = ""): Pair<Color, Color> {
    val forcedIndex = colorTokenPaletteIndex(clubName)
    val paletteIndex = forcedIndex ?: Random(clubId).nextInt(CREST_PALETTES.size)
    val (base, secondary, _) = CREST_PALETTES[paletteIndex]
    return Pair(base, secondary)
}

private enum class CrestShape { CIRCLE, SHIELD, HEXAGON, PENTAGON, ROUNDED_SQUARE }
private enum class CrestPattern { SOLID, VERTICAL_SPLIT, HORIZONTAL_HALVES, DIAGONAL_SPLIT, QUARTERED }
private enum class CrestEmblem { STAR, CHEVRON, DIAMOND_BALL, TREE, WREATH_BAR, RING, ARCH }

@Composable
fun ClubCrestIcon(
    clubId: Int,
    clubName: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val rng = remember(clubId) { Random(clubId) }
    val (base, secondary, trim) = remember(clubId, clubName) {
        val forcedIndex = colorTokenPaletteIndex(clubName)
        CREST_PALETTES[forcedIndex ?: rng.nextInt(CREST_PALETTES.size)]
    }
    val shape = remember(clubId) { CrestShape.entries[rng.nextInt(CrestShape.entries.size)] }
    val pattern = remember(clubId) { CrestPattern.entries[rng.nextInt(CrestPattern.entries.size)] }
    val emblem = remember(clubId) { CrestEmblem.entries[rng.nextInt(CrestEmblem.entries.size)] }

    Canvas(modifier = modifier.size(size)) {
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

        // Emblem, always centered in the upper 60% so the initials text below has room.
        val cx = w / 2f
        val cy = h * 0.36f
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
}
