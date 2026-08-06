package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RepeatingStepperButton(
    label: String,
    color: Color,
    onStep: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (isPressed) color.copy(alpha = 0.25f) else color.copy(alpha = 0.1f))
            .pointerInput(onStep) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onStep()
                        coroutineScope {
                            val repeatJob = launch {
                                delay(400L)
                                while (true) {
                                    onStep()
                                    delay(90L)
                                }
                            }
                            try {
                                awaitRelease()
                            } finally {
                                repeatJob.cancel()
                                isPressed = false
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

