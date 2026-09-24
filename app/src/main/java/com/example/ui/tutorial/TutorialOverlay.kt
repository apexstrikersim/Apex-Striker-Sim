package com.example.ui.tutorial

import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewParent
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
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

    // Bug 1 Step 1: Reactive read using derivedStateOf to prevent coordinate staleness
    val targetRect by remember(currentStep.targetId) {
        derivedStateOf { TutorialTargetRegistry.bounds[currentStep.targetId] }
    }

    Dialog(
        onDismissRequest = { /* not dismissible by back/outside tap, only via Skip button */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false
        )
    ) {
        ImmersiveDialogEffect()

        // Bug 2: Ensure dialog window dim is completely removed only for TutorialOverlay's dialog
        val view = LocalView.current
        fun applyDialogWindowConfig(window: Window) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setDimAmount(0f)
            val lp = window.attributes
            lp.dimAmount = 0f
            lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
            window.attributes = lp
            window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        }

        fun findDialogWindow(v: View): Window? {
            var p: ViewParent? = v.parent
            while (p != null) {
                if (p is DialogWindowProvider) return p.window
                p = p.parent
            }
            var ctx: Context? = v.context
            while (ctx is ContextWrapper) {
                if (ctx is DialogWindowProvider) return ctx.window
                if (ctx is android.app.Dialog) return ctx.window
                ctx = ctx.baseContext
            }
            return null
        }

        DisposableEffect(view) {
            findDialogWindow(view)?.let { applyDialogWindowConfig(it) }

            val attachListener = object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    findDialogWindow(v)?.let { applyDialogWindowConfig(it) }
                }
                override fun onViewDetachedFromWindow(v: View) {}
            }
            view.addOnAttachStateChangeListener(attachListener)
            onDispose {
                view.removeOnAttachStateChangeListener(attachListener)
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val maxHeightPx = constraints.maxHeight.toFloat()
            val density = LocalDensity.current
            val gapPx = with(density) { 16.dp.toPx() }
            val cutoutPaddingPx = with(density) { 8.dp.toPx() }

            // Bug 2: Scrim with cutout using PathFillType.EvenOdd so pixels inside cutout are never drawn
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (targetRect != null) {
                    val cutout = Rect(
                        left = targetRect!!.left - cutoutPaddingPx,
                        top = targetRect!!.top - cutoutPaddingPx,
                        right = targetRect!!.right + cutoutPaddingPx,
                        bottom = targetRect!!.bottom + cutoutPaddingPx
                    )
                    val path = Path().apply {
                        fillType = PathFillType.EvenOdd
                        addRect(Rect(0f, 0f, size.width, size.height))
                        addRoundRect(
                            RoundRect(
                                rect = cutout,
                                cornerRadius = CornerRadius(16.dp.toPx())
                            )
                        )
                    }
                    drawPath(path = path, color = Color.Black.copy(alpha = 0.75f))
                } else {
                    drawRect(color = Color.Black.copy(alpha = 0.75f))
                }
            }

            // Bug 1 Step 2: Constrain tooltip in a container Box above or below cutout
            val tooltipContainerModifier: Modifier
            val tooltipAlignment: Alignment
            if (targetRect != null) {
                val cutoutTop = targetRect!!.top - cutoutPaddingPx
                val cutoutBottom = targetRect!!.bottom + cutoutPaddingPx
                val spaceBelow = maxHeightPx - cutoutBottom
                val spaceAbove = cutoutTop

                if (spaceBelow >= spaceAbove) {
                    tooltipContainerModifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(y = with(density) { (cutoutBottom + gapPx).toDp() })
                        .fillMaxWidth()
                        .heightIn(max = with(density) { (spaceBelow - gapPx).coerceAtLeast(0f).toDp() })
                        .padding(horizontal = 24.dp)
                    tooltipAlignment = Alignment.TopCenter
                } else {
                    tooltipContainerModifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .height(with(density) { (cutoutTop - gapPx).coerceAtLeast(0f).toDp() })
                        .padding(horizontal = 24.dp)
                    tooltipAlignment = Alignment.BottomCenter
                }
            } else {
                tooltipContainerModifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(24.dp)
                tooltipAlignment = Alignment.BottomCenter
            }

            Box(
                modifier = tooltipContainerModifier,
                contentAlignment = tooltipAlignment
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
}
