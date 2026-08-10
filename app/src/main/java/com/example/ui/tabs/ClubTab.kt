package com.example.ui.tabs

import com.example.ui.components.ClubCrestIcon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ClubEntity
import com.example.data.PlayerEntity
import com.example.data.calculateProratedTarget
import com.example.ui.CareerViewModel
import com.example.ui.dialogs.TrainingDialog
import com.example.ui.theme.*

@Composable
fun ClubTab(viewModel: CareerViewModel, player: PlayerEntity, club: ClubEntity?) {
    if (club == null) return

    val gameStateState by viewModel.gameStateFlow.collectAsStateWithLifecycle()
    val currentMonthIndex = gameStateState?.currentMonthIndex ?: 0
    val isTransferOpen = currentMonthIndex == 0 || currentMonthIndex == 5

    var showTrainingDialog by remember { mutableStateOf(false) }
    var showRetireConfirmDialog by remember { mutableStateOf(false) }

    var managerSeasonsAtClub by remember(club.id) { mutableStateOf(0) }
    LaunchedEffect(club.id) {
        val mgr = viewModel.getActiveManagerForClub(club.id)
        if (mgr != null) {
            managerSeasonsAtClub = mgr.seasonsAtCurrentClub
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("club_tab"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "CLUB HUB",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PitchGreen
            )
            Text(
                text = "Squad status, target tracker, and rival comparison",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        // Club Profile Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SportsCardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ClubCrestIcon(
                        clubId = club.id,
                        clubName = club.name,
                        size = 56.dp
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(text = club.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(text = "Country: ${club.country}", fontSize = 13.sp, color = TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(DarkSlate)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(text = "Rep: ${club.reputation}", fontSize = 11.sp, color = PitchGreen)
                            }

                            val academyLevel = when (club.reputation) {
                                "ELITE" -> "Lvl 5 (Elite)"
                                "BIG" -> "Lvl 4 (High)"
                                "MID" -> "Lvl 3 (Standard)"
                                "SMALL" -> "Lvl 2 (Basic)"
                                else -> "Lvl 1"
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(DarkSlate)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(text = "Academy: $academyLevel", fontSize = 11.sp, color = TrophyGold)
                            }
                        }
                    }
                }
            }
        }

        // Training Ground Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SportsCardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("training_ground_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🏋️ TRAINING GROUND",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PitchGreen,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Fatigue Widget
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Current Fatigue:", fontSize = 13.sp, color = TextSecondary)
                        Text(
                            text = "${player.fatigue}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                player.fatigue > 70 -> MutedRed
                                player.fatigue > 40 -> TrophyGold
                                else -> PitchGreen
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { player.fatigue / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = when {
                            player.fatigue > 70 -> MutedRed
                            player.fatigue > 40 -> TrophyGold
                            else -> PitchGreen
                        },
                        trackColor = DarkSlate
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Overtraining Risk Widget
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Overtraining Risk:", fontSize = 13.sp, color = TextSecondary)
                        Text(
                            text = "${player.overtrainingRisk}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                player.overtrainingRisk > 70 -> MutedRed
                                player.overtrainingRisk > 40 -> TrophyGold
                                else -> PitchGreen
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { player.overtrainingRisk / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = when {
                            player.overtrainingRisk > 70 -> MutedRed
                            player.overtrainingRisk > 40 -> TrophyGold
                            else -> PitchGreen
                        },
                        trackColor = DarkSlate
                    )

                    if (player.overtrainingRisk > 70) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⚠️ HIGH OVERTRAINING RISK: High chance of minor knocks and slower fatigue recovery. Rest is recommended.",
                            fontSize = 11.sp,
                            color = MutedRed,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Start Session button
                    val alreadyTrained = player.hasTrainedThisMonth == 1
                    if (alreadyTrained) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Trained",
                                tint = PitchGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Trained this month. Reset in next month advance.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    } else {
                        Button(
                            onClick = { showTrainingDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PitchGreen,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("start_training_button")
                        ) {
                            Text(text = "START TRAINING SESSION", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Rival Striker Comparison Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SportsCardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "RIVAL STRIKER COMPARISON",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PitchGreen,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val rivalName = if (player.age in 13..15) "Academy Rival" else club.rivalStrikerName
                    val rivalOvr = if (player.age in 13..15) 30 else club.rivalStrikerOvr

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // My OVR
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "YOU", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            Text(text = player.name.split(" ").lastOrNull() ?: player.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(PitchGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = player.ovr.toString(), fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.Black)
                            }
                        }

                        // VS
                        Text(text = "VS", fontWeight = FontWeight.Black, fontSize = 20.sp, color = TextSecondary)

                        // Rival OVR
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "RIVAL", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            Text(text = rivalName.split(" ").lastOrNull() ?: rivalName, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(DarkSlate)
                                    .border(1.dp, BorderColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = rivalOvr.toString(), fontWeight = FontWeight.Black, fontSize = 16.sp, color = TextPrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Starting odds estimate
                    val diff = (player.ovr + player.form) - rivalOvr
                    val startProb = (0.5f + (diff * 0.035f)).coerceIn(0.05f, 0.95f)
                    val percent = (startProb * 100).toInt()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Est. Starting Chance:", fontSize = 13.sp, color = TextSecondary)
                        Text(text = "$percent%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PitchGreen)
                    }
                }
            }
        }

        // Season Contract Target Progress Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SportsCardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SEASON CONTRACT TARGETS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PitchGreen,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (player.age in 13..15) {
                        Text(
                            text = "Youth Academy Trainee: No goal quotas are enforced yet. Keep training to grow OVR!",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    } else {
                        val scaledTarget = if (player.seasonGamesPlayed == 0) player.contractTargetGoalsAssists else calculateProratedTarget(player.contractTargetGoalsAssists, player.seasonGamesPlayed)

                        val currentTally = player.seasonGoals + player.seasonAssists
                        val progressPct = if (scaledTarget > 0) (currentTally.toFloat() / scaledTarget).coerceIn(0f, 1f) else 0f

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Deal Remaining:", fontSize = 13.sp, color = TextSecondary)
                            Text(text = "${player.contractYearsRemaining} Years", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Pro-rated G+A Target:", fontSize = 13.sp, color = TextSecondary)
                            Text(text = "$scaledTarget G+A (Full deal: ${player.contractTargetGoalsAssists})", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Current Achieved:", fontSize = 13.sp, color = TextSecondary)
                            Text(
                                text = "$currentTally G+A",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentTally >= scaledTarget) PitchGreen else TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { progressPct.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = PitchGreen,
                            trackColor = DarkSlate
                        )
                    }
                }
            }
        }

        // Manager's Office Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SportsCardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "👔 MANAGER'S OFFICE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PitchGreen,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (club.managerName.isNotEmpty()) {
                        Column {
                            Text(text = club.managerName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "${club.managerReputationTier} Manager · $managerSeasonsAtClub seasons in charge", fontSize = 11.sp, color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Text(
                        text = "Discuss your squad status, request a start, or offer promises to secure your place.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Manager Trust Meter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Manager Trust:", fontSize = 13.sp, color = TextSecondary)
                        Text(
                            text = "${player.managerTrust}/100",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                player.managerTrust < 40 -> MutedRed
                                player.managerTrust > 70 -> PitchGreen
                                else -> TrophyGold
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { player.managerTrust / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = when {
                            player.managerTrust < 40 -> MutedRed
                            player.managerTrust > 70 -> PitchGreen
                            else -> TrophyGold
                        },
                        trackColor = DarkSlate
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Active promise status (if any)
                    val promiseGoals = player.activePromiseGoalsAssists
                    val promiseGames = player.activePromiseGamesRemaining
                    if (promiseGoals != null && promiseGames != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSlate)
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "🎯 ACTIVE PROMISE TO MANAGER",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TrophyGold
                                )
                                Text(
                                    text = "Deliver $promiseGoals goal contributions (G/A) in the next $promiseGames matches.",
                                    fontSize = 12.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    } else {
                        // Request meeting button logic
                        val diff = (player.ovr + player.form) - (if (player.age in 13..15) 30 else club.rivalStrikerOvr)
                        val startProb = (0.5f + (diff * 0.035f)).coerceIn(0.05f, 0.95f)
                        val isBenched = startProb < 0.40f || player.form < 0
                        val isLowTrust = player.managerTrust < 40
                        val isMeetingUnlocked = isBenched || isLowTrust

                        val cooldown = gameStateState?.managerTalkCooldownMonths ?: 0
                        if (cooldown > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSlate.copy(alpha = 0.5f))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "🔒 MEETING COOLDOWN: You recently spoke with the manager. Wait $cooldown more month(s) to talk again.",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else if (isMeetingUnlocked) {
                            Button(
                                onClick = { viewModel.requestManagerTalk() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PitchGreen,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("request_manager_meeting_button")
                            ) {
                                Text(text = "REQUEST MEETING WITH MANAGER", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSlate.copy(alpha = 0.5f))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "🔒 MEETING LOCKED: The manager has nothing to discuss right now. Keep performing in training and matches!",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }

        // Actions: Request Transfer / Voluntary Retire
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (player.age in 13..15) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SportsCardBg.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🎓 Youth Academy Striker: Professional transfer requests are locked until graduation at age 16.",
                            fontSize = 12.sp,
                            color = PitchGreen,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp).fillMaxWidth()
                        )
                    }
                } else if (!isTransferOpen) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SportsCardBg.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⏳ Transfer Window Closed. Standard windows are open in August (Month 0) and January (Month 5).",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp).fillMaxWidth()
                        )
                    }
                }

                Button(
                    onClick = { viewModel.checkTransferOffers() },
                    enabled = isTransferOpen && (player.age >= 16),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PitchGreen,
                        contentColor = Color.Black,
                        disabledContainerColor = DarkSlate,
                        disabledContentColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("transfer_request_button")
                ) {
                    Text(text = "REQUEST TRANSFERS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                if (player.age >= 32) {
                    Button(
                        onClick = { showRetireConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MutedRed, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("retire_button")
                    ) {
                        Text(text = "ANNOUNCE RETIREMENT", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    if (showTrainingDialog) {
        TrainingDialog(
            viewModel = viewModel,
            player = player,
            onDismiss = { showTrainingDialog = false }
        )
    }

    if (showRetireConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRetireConfirmDialog = false },
            title = {
                Text(
                    text = "Retire from Professional Football?",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "This will permanently end your career as ${player.name}. This action cannot be undone.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.retirePlayer()
                        showRetireConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MutedRed, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("retire_confirm_button")
                ) {
                    Text(text = "RETIRE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showRetireConfirmDialog = false },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("retire_cancel_button")
                ) {
                    Text(text = "CANCEL", color = TextPrimary)
                }
            },
            containerColor = SportsCardBg,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.testTag("retire_confirm_dialog")
        )
    }
}
