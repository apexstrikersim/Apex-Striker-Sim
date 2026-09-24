package com.example.ui.tutorial

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.ImmersiveDialogEffect

@Composable
fun TutorialOverlay(
    steps: List<TutorialStep>,
    onFinished: () -> Unit
) {
    var currentIndex by remember { mutableStateOf(0) }
    val currentStep = steps.getOrNull(currentIndex) ?: run {
        onFinished()
        return
    }
    val targetRect = TutorialTargetRegistry.bounds[currentStep.targetId]

    Dialog(
        onDismissRequest = { /* not dismissible by back/outside tap, only via Skip button */ },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        ImmersiveDialogEffect()

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val maxHeightPx = constraints.maxHeight.toFloat()
            // Scrim with cutout
            Canvas(modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            ) {
                drawRect(color = Color.Black.copy(alpha = 0.75f))
                if (targetRect != null) {
                    val padding = 8.dp.toPx()
                    val cutout = Rect(
                        left = targetRect.left - padding,
                        top = targetRect.top - padding,
                        right = targetRect.right + padding,
                        bottom = targetRect.bottom + padding
                    )
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = Offset(cutout.left, cutout.top),
                        size = Size(cutout.width, cutout.height),
                        cornerRadius = CornerRadius(16.dp.toPx()),
                        blendMode = BlendMode.Clear
                    )
                }
            }

            // Tooltip card, positioned below the target if there's room, 
            // otherwise above it. Falls back to bottom-center if no 
            // target bounds are available yet (e.g. first frame before 
            // layout completes).
            val density = LocalDensity.current
            val tooltipModifier = if (targetRect != null) {
                // If target is in the bottom half of the screen (e.g. bottom navigation bar), position card above it
                val estimatedCardHeightPx = with(density) { 180.dp.toPx() }
                if (targetRect.bottom + estimatedCardHeightPx > maxHeightPx) {
                    val bottomPaddingPx = maxHeightPx - targetRect.top + with(density) { 16.dp.toPx() }
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = with(density) { bottomPaddingPx.toDp() }, start = 24.dp, end = 24.dp)
                } else {
                    val topPx = targetRect.bottom + with(density) { 16.dp.toPx() }
                    Modifier
                        .align(Alignment.TopStart)
                        .offset(y = with(density) { topPx.toDp() })
                        .padding(horizontal = 24.dp)
                }
            } else {
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
            }

            Card(
                modifier = tooltipModifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(currentStep.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(currentStep.description, color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onFinished) {
                            Text("Skip Tutorial")
                        }
                        Button(onClick = {
                            if (currentIndex < steps.size - 1) currentIndex++ else onFinished()
                        }) {
                            Text(if (currentIndex < steps.size - 1) "Next" else "Done")
                        }
                    }
                }
            }
        }
    }
}
