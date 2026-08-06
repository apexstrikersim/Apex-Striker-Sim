package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PlayerEntity
import com.example.ui.CareerViewModel
import com.example.ui.theme.*

@Composable
fun RetiredSummaryScreen(viewModel: CareerViewModel, player: PlayerEntity?) {
    if (player == null) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("retired_summary_screen"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SportsCardBg),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Legacy",
                        tint = TrophyGold,
                        modifier = Modifier.size(64.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "CAREER RETROSPECTIVE",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TrophyGold,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = player.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = BorderColor)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "APPS", fontSize = 12.sp, color = TextSecondary)
                            Text(text = player.gamesPlayed.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "GOALS", fontSize = 12.sp, color = TextSecondary)
                            Text(text = player.goals.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PitchGreen)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "ASSISTS", fontSize = 12.sp, color = TextSecondary)
                            Text(text = player.assists.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PitchGreen)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "MVPS", fontSize = 12.sp, color = TextSecondary)
                            Text(text = player.mvps.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TrophyGold)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSlate),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Peak OVR reached: ${player.peakOvr}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "Final OVR at retirement: ${player.ovr}", fontSize = 13.sp, color = TextSecondary)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "You have hung up your boots after a legendary striker career. The football world honors your dedication. Your legacy now passes onto your son!",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.proceedToLegacy() },
                        colors = ButtonDefaults.buttonColors(containerColor = TrophyGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("start_son_career_button")
                    ) {
                        Text(text = "START SON'S CAREER", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}
