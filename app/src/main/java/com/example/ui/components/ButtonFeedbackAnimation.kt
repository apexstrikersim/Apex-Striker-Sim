package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSlate
import com.example.ui.theme.PitchGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun Modifier.bounceClick(
    scaleDownRatio: Float = 0.94f,
    onClick: (() -> Unit)? = null
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDownRatio else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounceScale"
    )

    return this
        .scale(scale)
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
            } else Modifier
        )
}

@Composable
fun AnimatedSaveButton(
    modifier: Modifier = Modifier,
    idleText: String = "MANUAL SAVE GAME",
    successText: String = "✓ GAME SAVED SUCCESSFULLY!",
    onSaveClick: () -> Unit
) {
    var isSuccess by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val buttonColor by animateColorAsState(
        targetValue = if (isSuccess) PitchGreen else DarkSlate,
        animationSpec = tween(durationMillis = 250),
        label = "buttonColor"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSuccess) Color.Black else PitchGreen,
        animationSpec = tween(durationMillis = 250),
        label = "textColor"
    )

    val checkScale by animateFloatAsState(
        targetValue = if (isSuccess) 1.25f else 0.7f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "checkScale"
    )

    Button(
        onClick = {
            if (!isSuccess) {
                onSaveClick()
                coroutineScope.launch {
                    isSuccess = true
                    delay(2000)
                    isSuccess = false
                }
            }
        },
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor, contentColor = textColor),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .bounceClick()
            .border(
                width = 1.5.dp,
                color = if (isSuccess) PitchGreen else PitchGreen.copy(alpha = 0.6f),
                shape = RoundedCornerShape(10.dp)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSuccess) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = Color.Black,
                    modifier = Modifier
                        .size(20.dp)
                        .scale(checkScale)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = if (isSuccess) successText else idleText,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}
