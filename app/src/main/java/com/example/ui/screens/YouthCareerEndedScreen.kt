package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PlayerEntity
import com.example.ui.CareerViewModel
import com.example.ui.theme.*

@Composable
fun YouthCareerEndedScreen(viewModel: CareerViewModel, player: PlayerEntity?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SportsDarkBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, CoralRed)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(CoralRed.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = CoralRed,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "YOUTH CAREER ENDED",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CoralRed,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "You reached Age 19 without signing a senior professional contract. Your grassroots football journey has come to an end.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (player != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkBackground),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("CAREER SUMMARY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldStar)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Player Name", color = TextSecondary, fontSize = 13.sp)
                                Text(player.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Final Age Reached", color = TextSecondary, fontSize = 13.sp)
                                Text("${player.age}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Peak Overall Rating (OVR)", color = TextSecondary, fontSize = 13.sp)
                                Text("${player.ovr}", color = PitchGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Phase Reached", color = TextSecondary, fontSize = 13.sp)
                                Text(
                                    if (player.careerPhase == com.example.data.PHASE_STREET) "Grassroots Street Football" else "Youth Academy",
                                    color = GoldStar,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.dismissYouthCareerEnded() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PitchGreen, contentColor = DarkBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Continue", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
