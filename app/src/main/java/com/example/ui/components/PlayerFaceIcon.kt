package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.data.FaceDescriptor

/**
 * Color palette matching the minimalist flat vector art style.
 */
private val SKIN_TONES = listOf(
    Color(0xFFF7C8A4), // 0: Fair / Pale
    Color(0xFFECA374), // 1: Light Tan
    Color(0xFFDC8B54), // 2: Warm Tan / Olive
    Color(0xFFB86B3E), // 3: Rich Caramel / Terracotta
    Color(0xFF8B4B24), // 4: Deep Warm Brown
    Color(0xFF532E1A)  // 5: Dark Espresso
)

private val HAIR_COLORS = listOf(
    Color(0xFF1B1A19), // 0: Jet Black
    Color(0xFF382319), // 1: Dark Espresso Brown
    Color(0xFF6B3E26), // 2: Medium Warm Brown
    Color(0xFFD8B162), // 3: Golden Blonde
    Color(0xFFB84E29), // 4: Auburn / Ginger
    Color(0xFF646E78)  // 5: Slate Grey (veterans)
)

private val FEATURE_DARK = Color(0xFF261814) // Dark tone for eyes and neutral mouth
private val TEETH_WHITE = Color(0xFFF5F5F5)

@Composable
fun PlayerFaceIcon(
    descriptor: FaceDescriptor,
    age: Int,
    form: Int = 0, // -5..+5; 0 for neutral / frozen contexts
    modifier: Modifier = Modifier
) {
    val skinTone = SKIN_TONES.getOrElse(descriptor.skinTone.coerceIn(0, SKIN_TONES.size - 1)) { SKIN_TONES[0] }
    
    // Hair color selection: if player is a veteran (36+), slightly graying
    val rawHairColor = HAIR_COLORS.getOrElse(descriptor.hairColor.coerceIn(0, 4)) { HAIR_COLORS[0] }
    val hairColor = if (age >= 36 && descriptor.hairColor <= 2) {
        Color(0xFF687076) // Salt & pepper gray for older veterans
    } else {
        rawHairColor
    }

    // Dynamic hair tone variations for layered realism
    val hairDark = hairColor.copy(
        red = (hairColor.red * 0.72f).coerceIn(0f, 1f),
        green = (hairColor.green * 0.72f).coerceIn(0f, 1f),
        blue = (hairColor.blue * 0.72f).coerceIn(0f, 1f)
    )
    val hairHighlight = hairColor.copy(
        red = (hairColor.red * 1.25f + 0.08f).coerceIn(0f, 1f),
        green = (hairColor.green * 1.25f + 0.08f).coerceIn(0f, 1f),
        blue = (hairColor.blue * 1.25f + 0.08f).coerceIn(0f, 1f),
        alpha = 0.55f
    )

    val isYouth = age < 20
    val isVeteran = age in 30..33
    val isLateCareer = age >= 34
    val canRenderBeard = !isYouth && descriptor.hasBeardTrait && age >= descriptor.beardRevealAge

    Canvas(
        modifier = modifier.size(48.dp)
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        // Head bounds - vector proportions
        val headWidth = w * 0.58f
        val headHeight = h * 0.70f
        val headLeft = cx - headWidth / 2f
        val headTop = cy - headHeight / 2f + h * 0.02f

        // Shaded Nose Color (richer, slightly darker tone than the skin)
        val noseColor = skinTone.copy(
            red = (skinTone.red * 0.82f).coerceIn(0f, 1f),
            green = (skinTone.green * 0.76f).coerceIn(0f, 1f),
            blue = (skinTone.blue * 0.72f).coerceIn(0f, 1f)
        )

        // 1. Draw Ears (side capsules placed at eye/nose height)
        val earW = w * 0.09f
        val earH = h * 0.16f
        val earY = headTop + headHeight * 0.40f

        // Left ear
        drawRoundRect(
            color = skinTone,
            topLeft = Offset(headLeft - earW * 0.65f, earY),
            size = Size(earW, earH),
            cornerRadius = CornerRadius(earW / 2f, earW / 2f)
        )
        // Right ear
        drawRoundRect(
            color = skinTone,
            topLeft = Offset(headLeft + headWidth - earW * 0.35f, earY),
            size = Size(earW, earH),
            cornerRadius = CornerRadius(earW / 2f, earW / 2f)
        )

        // 2. Base Head Path (Structured, flat-topped vector silhouette with soft rounded chin)
        val headPath = Path().apply {
            val topCornerRadius = headWidth * 0.28f
            val bottomCornerRadius = headWidth * 0.42f
            val rect = RoundRect(
                left = headLeft,
                top = headTop,
                right = headLeft + headWidth,
                bottom = headTop + headHeight,
                topLeftCornerRadius = CornerRadius(topCornerRadius, topCornerRadius),
                topRightCornerRadius = CornerRadius(topCornerRadius, topCornerRadius),
                bottomLeftCornerRadius = CornerRadius(bottomCornerRadius, bottomCornerRadius * 1.2f),
                bottomRightCornerRadius = CornerRadius(bottomCornerRadius, bottomCornerRadius * 1.2f)
            )
            addRoundRect(rect)
        }

        // Draw Face Skin
        drawPath(headPath, color = skinTone)

        // 3. REVAMPED ORGANIC HAIR STYLES (Smooth curves, flow, depth layers, realistic volume)
        val hairStyle = descriptor.hairStyle.coerceIn(0, 7)
        val strandStroke = (w * 0.022f).coerceAtLeast(1.2f)

        when (hairStyle) {
            0 -> {
                // Style 0: Modern Textured French Crop / Low Fade
                // Curved crown with layered flowing forward fringe & subtle strand highlights
                val cropPath = Path().apply {
                    moveTo(headLeft - 1f, headTop + headHeight * 0.28f)
                    lineTo(headLeft - 1f, headTop + headHeight * 0.10f)
                    cubicTo(
                        headLeft + headWidth * 0.15f, headTop - headHeight * 0.12f,
                        headLeft + headWidth * 0.85f, headTop - headHeight * 0.12f,
                        headLeft + headWidth + 1f, headTop + headHeight * 0.10f
                    )
                    lineTo(headLeft + headWidth + 1f, headTop + headHeight * 0.28f)
                    // Soft curved fringe waves across forehead
                    cubicTo(
                        headLeft + headWidth * 0.80f, headTop + headHeight * 0.17f,
                        headLeft + headWidth * 0.55f, headTop + headHeight * 0.21f,
                        cx, headTop + headHeight * 0.17f
                    )
                    cubicTo(
                        headLeft + headWidth * 0.35f, headTop + headHeight * 0.20f,
                        headLeft + headWidth * 0.15f, headTop + headHeight * 0.16f,
                        headLeft - 1f, headTop + headHeight * 0.28f
                    )
                    close()
                }
                drawPath(cropPath, color = hairColor)

                // Flowing curved strand layers
                val cropHighlight1 = Path().apply {
                    moveTo(headLeft + headWidth * 0.25f, headTop + headHeight * 0.02f)
                    quadraticTo(cx - headWidth * 0.05f, headTop + headHeight * 0.08f, cx - headWidth * 0.1f, headTop + headHeight * 0.16f)
                }
                drawPath(cropHighlight1, color = hairHighlight, style = Stroke(width = strandStroke, cap = StrokeCap.Round))
                val cropHighlight2 = Path().apply {
                    moveTo(headLeft + headWidth * 0.70f, headTop + headHeight * 0.01f)
                    quadraticTo(cx + headWidth * 0.15f, headTop + headHeight * 0.08f, cx + headWidth * 0.1f, headTop + headHeight * 0.15f)
                }
                drawPath(cropHighlight2, color = hairHighlight, style = Stroke(width = strandStroke, cap = StrokeCap.Round))
            }

            1 -> {
                // Style 1: Sleek Voluminous Quiff / Pompadour
                // High sweeping curved crest with natural arc and side taper
                val pompHair = Path().apply {
                    moveTo(headLeft - 2f, headTop + headHeight * 0.30f)
                    cubicTo(
                        headLeft - 1f, headTop + headHeight * 0.06f,
                        cx - headWidth * 0.25f, headTop - headHeight * 0.22f,
                        cx + headWidth * 0.10f, headTop - headHeight * 0.20f
                    )
                    cubicTo(
                        headLeft + headWidth * 0.85f, headTop - headHeight * 0.16f,
                        headLeft + headWidth + 2f, headTop + headHeight * 0.05f,
                        headLeft + headWidth + 2f, headTop + headHeight * 0.30f
                    )
                    // Swept hairline curvature
                    cubicTo(
                        headLeft + headWidth * 0.75f, headTop + headHeight * 0.15f,
                        cx + headWidth * 0.25f, headTop + headHeight * 0.11f,
                        cx - headWidth * 0.05f, headTop + headHeight * 0.13f
                    )
                    cubicTo(
                        headLeft + headWidth * 0.20f, headTop + headHeight * 0.18f,
                        headLeft + 2f, headTop + headHeight * 0.26f,
                        headLeft - 2f, headTop + headHeight * 0.30f
                    )
                    close()
                }
                drawPath(pompHair, color = hairColor)

                // Elegant upward-sweeping strand flow lines
                val quiffStrand1 = Path().apply {
                    moveTo(cx - headWidth * 0.15f, headTop + headHeight * 0.10f)
                    cubicTo(
                        cx - headWidth * 0.20f, headTop - headHeight * 0.08f,
                        cx - headWidth * 0.05f, headTop - headHeight * 0.16f,
                        cx + headWidth * 0.15f, headTop - headHeight * 0.14f
                    )
                }
                drawPath(quiffStrand1, color = hairHighlight, style = Stroke(width = strandStroke * 1.3f, cap = StrokeCap.Round))
                val quiffStrand2 = Path().apply {
                    moveTo(cx + headWidth * 0.15f, headTop + headHeight * 0.08f)
                    cubicTo(
                        cx + headWidth * 0.20f, headTop - headHeight * 0.04f,
                        cx + headWidth * 0.35f, headTop - headHeight * 0.08f,
                        headLeft + headWidth * 0.75f, headTop - headHeight * 0.06f
                    )
                }
                drawPath(quiffStrand2, color = hairHighlight, style = Stroke(width = strandStroke, cap = StrokeCap.Round))
            }

            2 -> {
                // Style 2: Wavy Flow / Surfer Curtains
                // Organic S-curved locks framing the head and parting softly
                val wavyPath = Path().apply {
                    moveTo(headLeft - w * 0.05f, headTop + headHeight * 0.42f)
                    cubicTo(
                        headLeft - w * 0.06f, headTop + headHeight * 0.15f,
                        headLeft - 2f, headTop - headHeight * 0.12f,
                        cx - headWidth * 0.08f, headTop - headHeight * 0.14f
                    )
                    cubicTo(
                        cx + headWidth * 0.10f, headTop - headHeight * 0.14f,
                        headLeft + headWidth + 2f, headTop - headHeight * 0.12f,
                        headLeft + headWidth + w * 0.05f, headTop + headHeight * 0.15f
                    )
                    lineTo(headLeft + headWidth + w * 0.05f, headTop + headHeight * 0.42f)
                    // Right curtain wave back in
                    cubicTo(
                        headLeft + headWidth - 1f, headTop + headHeight * 0.28f,
                        headLeft + headWidth * 0.75f, headTop + headHeight * 0.18f,
                        cx + headWidth * 0.12f, headTop + headHeight * 0.16f
                    )
                    // Part dip in middle
                    lineTo(cx, headTop + headHeight * 0.10f)
                    // Left curtain wave
                    cubicTo(
                        cx - headWidth * 0.12f, headTop + headHeight * 0.16f,
                        headLeft + headWidth * 0.25f, headTop + headHeight * 0.18f,
                        headLeft - w * 0.05f, headTop + headHeight * 0.42f
                    )
                    close()
                }
                drawPath(wavyPath, color = hairColor)

                // Left wavy strand
                val leftWave = Path().apply {
                    moveTo(cx - headWidth * 0.10f, headTop - headHeight * 0.06f)
                    cubicTo(
                        headLeft + headWidth * 0.20f, headTop + headHeight * 0.08f,
                        headLeft + headWidth * 0.05f, headTop + headHeight * 0.22f,
                        headLeft - w * 0.02f, headTop + headHeight * 0.35f
                    )
                }
                drawPath(leftWave, color = hairHighlight, style = Stroke(width = strandStroke, cap = StrokeCap.Round))

                // Right wavy strand
                val rightWave = Path().apply {
                    moveTo(cx + headWidth * 0.12f, headTop - headHeight * 0.06f)
                    cubicTo(
                        headLeft + headWidth * 0.75f, headTop + headHeight * 0.08f,
                        headLeft + headWidth * 0.90f, headTop + headHeight * 0.22f,
                        headLeft + headWidth + w * 0.02f, headTop + headHeight * 0.35f
                    )
                }
                drawPath(rightWave, color = hairHighlight, style = Stroke(width = strandStroke, cap = StrokeCap.Round))
            }

            3 -> {
                // Style 3: Curly Taper / Messy Ringlet Mop
                // Layered natural circular curls and interlocking soft loops
                val curlRadius = headWidth * 0.16f
                val curlCenters = listOf(
                    Offset(cx - headWidth * 0.35f, headTop + headHeight * 0.15f),
                    Offset(cx - headWidth * 0.42f, headTop + headHeight * 0.02f),
                    Offset(cx - headWidth * 0.25f, headTop - headHeight * 0.08f),
                    Offset(cx, headTop - headHeight * 0.12f),
                    Offset(cx + headWidth * 0.25f, headTop - headHeight * 0.08f),
                    Offset(cx + headWidth * 0.42f, headTop + headHeight * 0.02f),
                    Offset(cx + headWidth * 0.35f, headTop + headHeight * 0.15f),
                    Offset(cx - headWidth * 0.15f, headTop + headHeight * 0.05f),
                    Offset(cx + headWidth * 0.15f, headTop + headHeight * 0.05f)
                )

                // Base shadow backing
                for (center in curlCenters) {
                    drawCircle(color = hairDark, radius = curlRadius * 1.15f, center = center)
                }
                // Main organic curl bodies
                for (center in curlCenters) {
                    drawCircle(color = hairColor, radius = curlRadius, center = center)
                }
                // Inner ringlet curve accents
                for (center in curlCenters) {
                    val ringlet = Path().apply {
                        moveTo(center.x - curlRadius * 0.5f, center.y - curlRadius * 0.2f)
                        quadraticTo(center.x, center.y + curlRadius * 0.5f, center.x + curlRadius * 0.4f, center.y - curlRadius * 0.1f)
                    }
                    drawPath(ringlet, color = hairHighlight, style = Stroke(width = strandStroke * 0.85f, cap = StrokeCap.Round))
                }
            }

            4 -> {
                // Style 4: Side-Part Executive Sweep
                // Natural clean comb over with soft curves and defined parting
                val sidePartHair = Path().apply {
                    moveTo(headLeft - 2f, headTop + headHeight * 0.30f)
                    lineTo(headLeft - 2f, headTop + headHeight * 0.08f)
                    cubicTo(
                        headLeft + headWidth * 0.20f, headTop - headHeight * 0.15f,
                        headLeft + headWidth * 0.80f, headTop - headHeight * 0.13f,
                        headLeft + headWidth + 2f, headTop + headHeight * 0.08f
                    )
                    lineTo(headLeft + headWidth + 2f, headTop + headHeight * 0.30f)
                    // Parting curve sweeps across forehead
                    cubicTo(
                        headLeft + headWidth * 0.70f, headTop + headHeight * 0.18f,
                        cx, headTop + headHeight * 0.14f,
                        headLeft + headWidth * 0.12f, headTop + headHeight * 0.14f
                    )
                    lineTo(headLeft - 2f, headTop + headHeight * 0.30f)
                    close()
                }
                drawPath(sidePartHair, color = hairColor)

                // Part line indentation
                val partLine = Path().apply {
                    moveTo(headLeft + headWidth * 0.14f, headTop + headHeight * 0.14f)
                    quadraticTo(headLeft + headWidth * 0.18f, headTop + headHeight * 0.02f, headLeft + headWidth * 0.22f, headTop - headHeight * 0.08f)
                }
                drawPath(partLine, color = hairDark, style = Stroke(width = strandStroke * 1.2f, cap = StrokeCap.Round))

                // Smooth combed highlight strokes
                val sweepStroke1 = Path().apply {
                    moveTo(headLeft + headWidth * 0.30f, headTop - headHeight * 0.04f)
                    cubicTo(
                        cx + headWidth * 0.05f, headTop - headHeight * 0.06f,
                        headLeft + headWidth * 0.70f, headTop + headHeight * 0.02f,
                        headLeft + headWidth * 0.82f, headTop + headHeight * 0.14f
                    )
                }
                drawPath(sweepStroke1, color = hairHighlight, style = Stroke(width = strandStroke, cap = StrokeCap.Round))
                val sweepStroke2 = Path().apply {
                    moveTo(headLeft + headWidth * 0.35f, headTop + headHeight * 0.04f)
                    cubicTo(
                        cx + headWidth * 0.10f, headTop + headHeight * 0.03f,
                        headLeft + headWidth * 0.65f, headTop + headHeight * 0.08f,
                        headLeft + headWidth * 0.78f, headTop + headHeight * 0.20f
                    )
                }
                drawPath(sweepStroke2, color = hairHighlight, style = Stroke(width = strandStroke, cap = StrokeCap.Round))
            }

            5 -> {
                // Style 5: Modern Layered Spiky Volume / Textured Faux-Hawk
                // Realistic curved textured tufts with organic rounded tips (no rigid polygons)
                val spikeCluster = Path().apply {
                    moveTo(headLeft - 2f, headTop + headHeight * 0.28f)
                    // Left soft tuft
                    cubicTo(
                        headLeft - 1f, headTop + headHeight * 0.12f,
                        headLeft + headWidth * 0.08f, headTop - headHeight * 0.05f,
                        headLeft + headWidth * 0.16f, headTop - headHeight * 0.08f
                    )
                    // Mid-left raised tuft
                    cubicTo(
                        headLeft + headWidth * 0.18f, headTop + headHeight * 0.02f,
                        cx - headWidth * 0.18f, headTop - headHeight * 0.16f,
                        cx - headWidth * 0.08f, headTop - headHeight * 0.20f
                    )
                    // Center crest tuft
                    cubicTo(
                        cx - headWidth * 0.02f, headTop - headHeight * 0.06f,
                        cx + headWidth * 0.08f, headTop - headHeight * 0.20f,
                        cx + headWidth * 0.18f, headTop - headHeight * 0.18f
                    )
                    // Mid-right tuft
                    cubicTo(
                        cx + headWidth * 0.22f, headTop + headHeight * 0.02f,
                        headLeft + headWidth * 0.78f, headTop - headHeight * 0.10f,
                        headLeft + headWidth * 0.86f, headTop - headHeight * 0.05f
                    )
                    // Right temple descent
                    cubicTo(
                        headLeft + headWidth * 0.92f, headTop + headHeight * 0.10f,
                        headLeft + headWidth + 2f, headTop + headHeight * 0.16f,
                        headLeft + headWidth + 2f, headTop + headHeight * 0.28f
                    )
                    // Natural layered fringe along forehead
                    cubicTo(
                        headLeft + headWidth * 0.75f, headTop + headHeight * 0.16f,
                        headLeft + headWidth * 0.50f, headTop + headHeight * 0.20f,
                        cx, headTop + headHeight * 0.15f
                    )
                    cubicTo(
                        headLeft + headWidth * 0.30f, headTop + headHeight * 0.19f,
                        headLeft + headWidth * 0.12f, headTop + headHeight * 0.18f,
                        headLeft - 2f, headTop + headHeight * 0.28f
                    )
                    close()
                }
                drawPath(spikeCluster, color = hairColor)

                // Interior strand contours on tufts
                val spikeStrand1 = Path().apply {
                    moveTo(cx - headWidth * 0.08f, headTop - headHeight * 0.14f)
                    quadraticTo(cx - headWidth * 0.06f, headTop + headHeight * 0.02f, cx - headWidth * 0.04f, headTop + headHeight * 0.12f)
                }
                drawPath(spikeStrand1, color = hairHighlight, style = Stroke(width = strandStroke, cap = StrokeCap.Round))
                val spikeStrand2 = Path().apply {
                    moveTo(cx + headWidth * 0.14f, headTop - headHeight * 0.13f)
                    quadraticTo(cx + headWidth * 0.10f, headTop + headHeight * 0.02f, cx + headWidth * 0.08f, headTop + headHeight * 0.12f)
                }
                drawPath(spikeStrand2, color = hairHighlight, style = Stroke(width = strandStroke, cap = StrokeCap.Round))
            }

            6 -> {
                // Style 6: Braided / Dreads Fade / Textured Afro Taper
                // Curved crown with organic textured dread/braid bands and crisp temple fade
                val afroBase = Path().apply {
                    moveTo(headLeft - w * 0.02f, headTop + headHeight * 0.28f)
                    cubicTo(
                        headLeft - w * 0.03f, headTop + headHeight * 0.04f,
                        headLeft + headWidth * 0.15f, headTop - headHeight * 0.16f,
                        cx, headTop - headHeight * 0.16f
                    )
                    cubicTo(
                        headLeft + headWidth * 0.85f, headTop - headHeight * 0.16f,
                        headLeft + headWidth + w * 0.03f, headTop + headHeight * 0.04f,
                        headLeft + headWidth + w * 0.02f, headTop + headHeight * 0.28f
                    )
                    // Crisp edge-up hairline
                    lineTo(headLeft + headWidth, headTop + headHeight * 0.18f)
                    cubicTo(
                        headLeft + headWidth * 0.70f, headTop + headHeight * 0.14f,
                        headLeft + headWidth * 0.30f, headTop + headHeight * 0.14f,
                        headLeft, headTop + headHeight * 0.18f
                    )
                    close()
                }
                drawPath(afroBase, color = hairColor)

                // Textured braid / twist channels
                val braidCount = 5
                for (i in 0 until braidCount) {
                    val bx = headLeft + headWidth * (0.18f + i * 0.16f)
                    val braidPath = Path().apply {
                        moveTo(bx, headTop + headHeight * 0.15f)
                        cubicTo(
                            bx + (if (i % 2 == 0) 2f else -2f), headTop + headHeight * 0.02f,
                            bx + (if (i % 2 == 0) -2f else 2f), headTop - headHeight * 0.06f,
                            bx, headTop - headHeight * 0.13f
                        )
                    }
                    drawPath(braidPath, color = hairHighlight, style = Stroke(width = strandStroke * 1.1f, cap = StrokeCap.Round))
                }
            }

            7 -> {
                // Style 7: Buzz Cut / Skin Fade with Crisp Line-up
                // Smooth head silhouette with realistic shaved-hair gradient overlay and micro-textured grain
                val buzzPath = Path().apply {
                    moveTo(headLeft - 0.5f, headTop + headHeight * 0.24f)
                    cubicTo(
                        headLeft, headTop + headHeight * 0.02f,
                        cx - headWidth * 0.30f, headTop - headHeight * 0.07f,
                        cx, headTop - headHeight * 0.07f
                    )
                    cubicTo(
                        cx + headWidth * 0.30f, headTop - headHeight * 0.07f,
                        headLeft + headWidth, headTop + headHeight * 0.02f,
                        headLeft + headWidth + 0.5f, headTop + headHeight * 0.24f
                    )
                    // Crisp sharp hairline
                    lineTo(headLeft + headWidth, headTop + headHeight * 0.16f)
                    cubicTo(
                        headLeft + headWidth * 0.70f, headTop + headHeight * 0.13f,
                        headLeft + headWidth * 0.30f, headTop + headHeight * 0.13f,
                        headLeft, headTop + headHeight * 0.16f
                    )
                    close()
                }
                drawPath(buzzPath, color = hairColor.copy(alpha = 0.88f))

                // Subtle edge highlight along razor lineup
                val lineUp = Path().apply {
                    moveTo(headLeft + headWidth * 0.05f, headTop + headHeight * 0.17f)
                    cubicTo(
                        headLeft + headWidth * 0.35f, headTop + headHeight * 0.13f,
                        headLeft + headWidth * 0.65f, headTop + headHeight * 0.13f,
                        headLeft + headWidth * 0.95f, headTop + headHeight * 0.17f
                    )
                }
                drawPath(lineUp, color = hairHighlight, style = Stroke(width = strandStroke * 0.9f, cap = StrokeCap.Round))
            }
        }

        // 4. Draw Eyebrows (Horizontal vector bars directly above eyes)
        val browY = headTop + headHeight * (if (isYouth) 0.38f else 0.39f)
        val eyeSpacing = headWidth * 0.24f
        val browW = headWidth * 0.22f
        val browH = (h * 0.038f).coerceAtLeast(2f)

        val leftBrowLeft = cx - eyeSpacing - browW / 2f
        val rightBrowLeft = cx + eyeSpacing - browW / 2f

        drawRoundRect(
            color = hairColor,
            topLeft = Offset(leftBrowLeft, browY),
            size = Size(browW, browH),
            cornerRadius = CornerRadius(1.5f, 1.5f)
        )
        drawRoundRect(
            color = hairColor,
            topLeft = Offset(rightBrowLeft, browY),
            size = Size(browW, browH),
            cornerRadius = CornerRadius(1.5f, 1.5f)
        )

        // 5. Draw Eyes (Geometric vector blocks)
        val eyeY = browY + browH + h * 0.032f
        val eyeW = headWidth * 0.19f
        val eyeH = (h * 0.072f).coerceAtLeast(3.5f)

        val leftEyeLeft = cx - eyeSpacing - eyeW / 2f
        val rightEyeLeft = cx + eyeSpacing - eyeW / 2f

        drawRoundRect(
            color = FEATURE_DARK,
            topLeft = Offset(leftEyeLeft, eyeY),
            size = Size(eyeW, eyeH),
            cornerRadius = CornerRadius(1.5f, 1.5f)
        )
        drawRoundRect(
            color = FEATURE_DARK,
            topLeft = Offset(rightEyeLeft, eyeY),
            size = Size(eyeW, eyeH),
            cornerRadius = CornerRadius(1.5f, 1.5f)
        )

        // 6. Draw Nose (Downward tapering shaded geometric triangle)
        val noseTop = eyeY - h * 0.005f
        val noseBottom = headTop + headHeight * 0.63f
        val noseHalfW = headWidth * 0.055f

        val noseTriangle = Path().apply {
            moveTo(cx - noseHalfW, noseTop)
            lineTo(cx + noseHalfW, noseTop)
            lineTo(cx, noseBottom)
            close()
        }
        drawPath(noseTriangle, color = noseColor)

        // 7. Draw Beard & Mustache Templates (Clean geometric shapes)
        val mouthY = headTop + headHeight * 0.77f
        val mouthW = headWidth * 0.34f

        if (canRenderBeard) {
            when (descriptor.beardStyle.coerceIn(0, 3)) {
                0 -> {
                    // Refined Chevron / Tapered Mustache (natural curve sitting above upper lip)
                    val stacheW = headWidth * 0.38f
                    val stachePath = Path().apply {
                        moveTo(cx - stacheW / 2f, mouthY - h * 0.010f)
                        cubicTo(
                            cx - stacheW * 0.28f, mouthY - h * 0.055f,
                            cx - stacheW * 0.08f, mouthY - h * 0.048f,
                            cx, mouthY - h * 0.032f
                        )
                        cubicTo(
                            cx + stacheW * 0.08f, mouthY - h * 0.048f,
                            cx + stacheW * 0.28f, mouthY - h * 0.055f,
                            cx + stacheW / 2f, mouthY - h * 0.010f
                        )
                        cubicTo(
                            cx + stacheW * 0.25f, mouthY - h * 0.018f,
                            cx + stacheW * 0.08f, mouthY - h * 0.015f,
                            cx, mouthY - h * 0.012f
                        )
                        cubicTo(
                            cx - stacheW * 0.08f, mouthY - h * 0.015f,
                            cx - stacheW * 0.25f, mouthY - h * 0.018f,
                            cx - stacheW / 2f, mouthY - h * 0.010f
                        )
                        close()
                    }
                    drawPath(stachePath, color = hairColor)
                }
                1 -> {
                    // Refined Goatee + Soul Patch + Mustache
                    val stacheW = headWidth * 0.36f
                    val stachePath = Path().apply {
                        moveTo(cx - stacheW / 2f, mouthY - h * 0.010f)
                        cubicTo(cx - stacheW * 0.25f, mouthY - h * 0.045f, cx - stacheW * 0.08f, mouthY - h * 0.040f, cx, mouthY - h * 0.028f)
                        cubicTo(cx + stacheW * 0.08f, mouthY - h * 0.040f, cx + stacheW * 0.25f, mouthY - h * 0.045f, cx + stacheW / 2f, mouthY - h * 0.010f)
                        cubicTo(cx + stacheW * 0.25f, mouthY - h * 0.016f, cx + stacheW * 0.08f, mouthY - h * 0.014f, cx, mouthY - h * 0.010f)
                        cubicTo(cx - stacheW * 0.08f, mouthY - h * 0.014f, cx - stacheW * 0.25f, mouthY - h * 0.016f, cx - stacheW / 2f, mouthY - h * 0.010f)
                        close()
                    }
                    drawPath(stachePath, color = hairColor)

                    // Chin goatee with smooth tapered chin curve
                    val chinW = headWidth * 0.36f
                    val chinTop = mouthY + h * 0.038f
                    val chinPath = Path().apply {
                        moveTo(cx - chinW / 2f, chinTop)
                        quadraticTo(cx - chinW * 0.45f, headTop + headHeight, cx, headTop + headHeight + 1f)
                        quadraticTo(cx + chinW * 0.45f, headTop + headHeight, cx + chinW / 2f, chinTop)
                        quadraticTo(cx, chinTop + h * 0.02f, cx - chinW / 2f, chinTop)
                        close()
                    }
                    drawPath(chinPath, color = hairColor)

                    // Soul patch under lip
                    val soulPatchPath = Path().apply {
                        moveTo(cx - 2.5f, mouthY + h * 0.016f)
                        lineTo(cx + 2.5f, mouthY + h * 0.016f)
                        lineTo(cx, mouthY + h * 0.034f)
                        close()
                    }
                    drawPath(soulPatchPath, color = hairColor)
                }
                2 -> {
                    // Refined Full Beard with natural jawline contour and framed mouth
                    val fullBeardPath = Path().apply {
                        // Outer jawline contour
                        moveTo(headLeft + 1.5f, headTop + headHeight * 0.48f)
                        lineTo(headLeft + 1.5f, headTop + headHeight * 0.82f)
                        quadraticTo(cx, headTop + headHeight + 2.5f, headLeft + headWidth - 1.5f, headTop + headHeight * 0.82f)
                        lineTo(headLeft + headWidth - 1.5f, headTop + headHeight * 0.48f)
                        // Inner cheek & mustache line
                        lineTo(headLeft + headWidth * 0.76f, mouthY - h * 0.035f)
                        quadraticTo(cx, mouthY - h * 0.020f, headLeft + headWidth * 0.24f, mouthY - h * 0.035f)
                        close()
                    }
                    drawPath(fullBeardPath, color = hairColor)

                    // Organic mouth cutout in skin tone so mouth expression remains clearly readable
                    val cutoutPath = Path().apply {
                        val cutoutW = mouthW * 1.35f
                        val cutoutH = h * 0.080f
                        moveTo(cx - cutoutW / 2f, mouthY - cutoutH * 0.25f)
                        quadraticTo(cx, mouthY - cutoutH * 0.45f, cx + cutoutW / 2f, mouthY - cutoutH * 0.25f)
                        quadraticTo(cx + cutoutW * 0.45f, mouthY + cutoutH * 0.65f, cx, mouthY + cutoutH * 0.65f)
                        quadraticTo(cx - cutoutW * 0.45f, mouthY + cutoutH * 0.65f, cx - cutoutW / 2f, mouthY - cutoutH * 0.25f)
                        close()
                    }
                    drawPath(cutoutPath, color = skinTone)
                }
                3 -> {
                    // Designer Stubble / 5 O'clock Shadow (Soft alpha hugging jaw and lip)
                    val stubbleRadius = headWidth * 0.42f
                    val stubbleRect = RoundRect(
                        left = headLeft + 1.5f,
                        top = headTop + headHeight * 0.56f,
                        right = headLeft + headWidth - 1.5f,
                        bottom = headTop + headHeight + 0.5f,
                        bottomLeftCornerRadius = CornerRadius(stubbleRadius, stubbleRadius * 1.2f),
                        bottomRightCornerRadius = CornerRadius(stubbleRadius, stubbleRadius * 1.2f)
                    )
                    val stubblePath = Path().apply { addRoundRect(stubbleRect) }
                    drawPath(stubblePath, color = hairColor.copy(alpha = 0.35f))
                }
            }
        }

        // 8. Draw Mouth driven by form (-5 to +5)
        val mouthStroke = (w * 0.045f).coerceAtLeast(2.4f)

        when {
            form >= 4 -> {
                // Wide smile with clean white teeth bar (Peak form / high morale)
                val mouthOpenH = h * 0.085f
                val mouthPath = Path().apply {
                    moveTo(cx - mouthW / 2f, mouthY)
                    quadraticTo(cx, mouthY + mouthOpenH, cx + mouthW / 2f, mouthY)
                    quadraticTo(cx, mouthY - mouthOpenH * 0.15f, cx - mouthW / 2f, mouthY)
                    close()
                }
                drawPath(mouthPath, color = FEATURE_DARK)
                // Teeth bar
                val teethW = mouthW * 0.68f
                val teethH = mouthOpenH * 0.45f
                drawRoundRect(
                    color = TEETH_WHITE,
                    topLeft = Offset(cx - teethW / 2f, mouthY - 0.5f),
                    size = Size(teethW, teethH),
                    cornerRadius = CornerRadius(1.5f, 1.5f)
                )
            }
            form in 1..3 -> {
                // Gentle smile
                val smilePath = Path().apply {
                    moveTo(cx - mouthW / 2f, mouthY)
                    quadraticTo(cx, mouthY + h * 0.035f, cx + mouthW / 2f, mouthY)
                }
                drawPath(smilePath, color = FEATURE_DARK, style = Stroke(width = mouthStroke, cap = StrokeCap.Round))
            }
            form in -1..0 -> {
                // Neutral flat horizontal line (Classic reference art neutral expression)
                drawLine(
                    color = FEATURE_DARK,
                    start = Offset(cx - mouthW * 0.45f, mouthY),
                    end = Offset(cx + mouthW * 0.45f, mouthY),
                    strokeWidth = mouthStroke,
                    cap = StrokeCap.Round
                )
            }
            form in -4..-2 -> {
                // Slight frown
                val frownPath = Path().apply {
                    moveTo(cx - mouthW / 2f, mouthY + h * 0.018f)
                    quadraticTo(cx, mouthY - h * 0.018f, cx + mouthW / 2f, mouthY + h * 0.018f)
                }
                drawPath(frownPath, color = FEATURE_DARK, style = Stroke(width = mouthStroke, cap = StrokeCap.Round))
            }
            else -> {
                // Pronounced frown (Slump)
                val bigFrownPath = Path().apply {
                    moveTo(cx - mouthW / 2f, mouthY + h * 0.035f)
                    quadraticTo(cx, mouthY - h * 0.035f, cx + mouthW / 2f, mouthY + h * 0.035f)
                }
                drawPath(bigFrownPath, color = FEATURE_DARK, style = Stroke(width = mouthStroke, cap = StrokeCap.Round))
            }
        }

        // 9. Draw Wrinkle Lines for Veteran (30-33) and Late Career (34-42)
        if (isVeteran || isLateCareer) {
            val wrinkleStroke = (w * 0.018f).coerceAtLeast(1.2f)
            val wrinkleAlpha = 0.22f
            val wrinkleColor = FEATURE_DARK.copy(alpha = wrinkleAlpha)

            // 1st forehead line
            val fLineY1 = headTop + headHeight * 0.22f
            drawLine(
                color = wrinkleColor,
                start = Offset(cx - headWidth * 0.22f, fLineY1),
                end = Offset(cx + headWidth * 0.22f, fLineY1),
                strokeWidth = wrinkleStroke,
                cap = StrokeCap.Round
            )

            if (isLateCareer) {
                // 2nd forehead line
                val fLineY2 = headTop + headHeight * 0.27f
                drawLine(
                    color = wrinkleColor,
                    start = Offset(cx - headWidth * 0.16f, fLineY2),
                    end = Offset(cx + headWidth * 0.16f, fLineY2),
                    strokeWidth = wrinkleStroke,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
