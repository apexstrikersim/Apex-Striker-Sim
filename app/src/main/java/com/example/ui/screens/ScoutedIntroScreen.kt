package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.GameStateEntity
import com.example.ui.CareerViewModel
import com.example.ui.theme.BorderColor
import com.example.ui.theme.PitchGreen
import com.example.ui.theme.SportsCardBg
import com.example.ui.theme.TextPrimary

@Composable
fun ScoutedIntroScreen(viewModel: CareerViewModel, gameState: GameStateEntity?) {
    val hapticFeedback = androidx.compose.ui.platform.LocalHapticFeedback.current
    val isHapticEnabled by viewModel.isHapticEnabled.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (isHapticEnabled) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("intro_screen"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SportsCardBg),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Scout Report",
                    tint = PitchGreen,
                    modifier = Modifier.size(56.dp)
                )

                Text(
                    text = "SCOUT REPORT #803",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PitchGreen,
                    letterSpacing = 2.sp
                )

                Divider(color = BorderColor)

                Text(
                    text = gameState?.narrativeLog?.split("---")?.firstOrNull() ?: "You've been scouted!",
                    fontSize = 15.sp,
                    color = TextPrimary,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center
                )

                Divider(color = BorderColor)

                Button(
                    onClick = { viewModel.skipIntro() },
                    colors = ButtonDefaults.buttonColors(containerColor = PitchGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("skip_intro_button")
                ) {
                    Text(text = "STEP ONTO THE PITCH", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
