package com.example.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Group
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
import com.example.data.GameStateEntity
import com.example.data.PlayerEntity
import com.example.data.formatSeasonYear
import com.example.ui.CareerViewModel
import com.example.ui.components.SocialStatBar
import com.example.ui.components.StatItem
import com.example.ui.components.StatSnapshotItem
import com.example.ui.getMonthName
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HomeTab(
    viewModel: CareerViewModel,
    player: PlayerEntity,
    myClub: ClubEntity?,
    gameState: GameStateEntity
) {
    val fixtures by viewModel.fixturesFlow.collectAsStateWithLifecycle()
    val clubs by viewModel.clubsFlow.collectAsStateWithLifecycle()
    val isAdvancing by viewModel.isAdvancing.collectAsStateWithLifecycle()

    val currentMonthFixtures = remember(fixtures, gameState.currentMonthIndex, player.currentClubId) {
        fixtures.filter {
            it.monthIndex == gameState.currentMonthIndex &&
            !it.isSimulated &&
            (it.homeClubId == player.currentClubId || it.awayClubId == player.currentClubId)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("home_tab"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Player Profile Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SportsCardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "STRIKER PROFILE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PitchGreen,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "• ${formatSeasonYear(gameState.currentSeason)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TrophyGold
                                )
                            }
                            Text(text = "Age: ${player.age} (Gen ${player.generation})", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.showFamilyPage(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSlate, contentColor = PitchGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp).testTag("family_button")
                        ) {
                            Icon(imageVector = Icons.Default.Group, contentDescription = "Family", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Family", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5 key stats visualizer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem(label = "FIN", value = player.finishing)
                        StatItem(label = "PAC", value = player.pace)
                        StatItem(label = "PAS", value = player.passing)
                        StatItem(label = "PHY", value = player.physical)
                        StatItem(label = "TEC", value = player.technique)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = BorderColor)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Season stat snapshot
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatSnapshotItem(label = "APPS", value = player.seasonGamesPlayed.toString())
                        StatSnapshotItem(label = "GOALS", value = player.seasonGoals.toString(), tint = PitchGreen)
                        StatSnapshotItem(label = "ASSISTS", value = player.seasonAssists.toString(), tint = PitchGreen)
                        StatSnapshotItem(label = "MVPS", value = player.seasonMvps.toString(), tint = TrophyGold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = BorderColor)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Fatigue and Overtraining Risk Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Fatigue", fontSize = 12.sp, color = TextSecondary)
                                Text(
                                    text = "${player.fatigue}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (player.fatigue > 60) MutedRed else PitchGreen
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { player.fatigue / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (player.fatigue > 60) MutedRed else PitchGreen,
                                trackColor = DarkSlate
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Overtraining", fontSize = 12.sp, color = TextSecondary)
                                Text(
                                    text = "${player.overtrainingRisk}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (player.overtrainingRisk > 60) MutedRed else PitchGreen
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { player.overtrainingRisk / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (player.overtrainingRisk > 60) MutedRed else PitchGreen,
                                trackColor = DarkSlate
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = BorderColor)
                    Spacer(modifier = Modifier.height(8.dp))

                    var isSocialStatsExpanded by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isSocialStatsExpanded = !isSocialStatsExpanded }
                            .padding(vertical = 4.dp)
                            .testTag("social_panel_header"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = "Social Status",
                                tint = PitchGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SOCIAL STATUS & REPUTATION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PitchGreen,
                                letterSpacing = 1.sp
                            )
                        }
                        Icon(
                            imageVector = if (isSocialStatsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle Social Stats",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (isSocialStatsExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("social_panel_content"),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Morale
                            val moraleDesc = when {
                                player.morale < 30 -> "Depressed 😔"
                                player.morale < 50 -> "Dissatisfied 😟"
                                player.morale < 70 -> "Motivated 🙂"
                                else -> "Ecstatic 😄"
                            }
                            SocialStatBar(
                                label = "Squad Morale",
                                value = player.morale,
                                descriptor = moraleDesc,
                                progressColor = PitchGreen,
                                tag = "morale_bar"
                            )

                            // Fan Reputation
                            val fanDesc = when {
                                player.fanReputation < 30 -> "Jeered & Unpopular 😠"
                                player.fanReputation < 50 -> "Indifferent 😐"
                                player.fanReputation < 70 -> "Liked & Respected 😊"
                                else -> "Fan Favorite ⭐"
                            }
                            SocialStatBar(
                                label = "Fan Reputation",
                                value = player.fanReputation,
                                descriptor = fanDesc,
                                progressColor = TrophyGold,
                                tag = "fan_reputation_bar"
                            )

                            // Manager Trust
                            val trustDesc = when {
                                player.managerTrust < 30 -> "Outcast / Benched 🚫"
                                player.managerTrust < 50 -> "Fringe Player 🪑"
                                player.managerTrust < 70 -> "Trusted Regular 👟"
                                else -> "Untouchable Starter 👑"
                            }
                            SocialStatBar(
                                label = "Manager Trust",
                                value = player.managerTrust,
                                descriptor = trustDesc,
                                progressColor = SubLineYellow,
                                tag = "manager_trust_bar"
                            )

                            // Rival Relationship
                            val rivalDesc = when {
                                player.rivalRelationship < 30 -> "Hostile Rivals ⚔️"
                                player.rivalRelationship < 50 -> "Tense Competitors 🏃"
                                player.rivalRelationship < 70 -> "Professional Colleagues 🤝"
                                else -> "Friends & Mentors 🌟"
                            }
                            SocialStatBar(
                                label = "Rival Relationship",
                                value = player.rivalRelationship,
                                descriptor = rivalDesc,
                                progressColor = if (player.rivalRelationship < 30) MutedRed else PitchGreen,
                                tag = "rival_relationship_bar"
                            )
                        }
                    }
                }
            }
        }

        // Circular Green Advance Button with Preview
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SportsCardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "UPCOMING FIXTURES PREVIEW",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PitchGreen,
                        letterSpacing = 1.sp
                    )

                    if (currentMonthFixtures.isEmpty()) {
                        Text(
                            text = "No remaining matches scheduled for this month.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        val european = currentMonthFixtures.filter { it.competition != "LEAGUE" }
                        val league = currentMonthFixtures.filter { it.competition == "LEAGUE" }
                        val leagueShowCount = maxOf(0, 3 - european.size)
                        val displayedFixtures = european + league.take(leagueShowCount)
                        val leftoverLeagueCount = maxOf(0, league.size - leagueShowCount)

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            displayedFixtures.forEach { fixture ->
                                val oppId = if (fixture.homeClubId == player.currentClubId) fixture.awayClubId else fixture.homeClubId
                                val oppClubName = remember(clubs) { clubs.find { it.id == oppId }?.name ?: "Unknown FC" }
                                val venue = if (fixture.homeClubId == player.currentClubId) "Home" else "Away"
                                
                                if (fixture.competition != "LEAGUE") {
                                    val badgeColor = when (fixture.competition) {
                                        "CHAMPIONS_LEAGUE" -> Color(0xFF1A73E8) // Deep Blue
                                        "EUROPA_LEAGUE" -> Color(0xFFE8710A) // Orange/Amber
                                        "CONFERENCE_LEAGUE" -> Color(0xFF137333) // Green
                                        "SUPER_CUP" -> TrophyGold
                                        else -> DarkSlate
                                    }
                                    val compLabel = when (fixture.competition) {
                                        "CHAMPIONS_LEAGUE" -> "UCL"
                                        "EUROPA_LEAGUE" -> "UEL"
                                        "CONFERENCE_LEAGUE" -> "UECL"
                                        "SUPER_CUP" -> "Super Cup"
                                        else -> "Cup"
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .border(0.5.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = compLabel.uppercase(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = badgeColor
                                            )
                                        }
                                        Text(
                                            text = "🆚 vs $oppClubName ($venue)",
                                            fontSize = 13.sp,
                                            color = TextPrimary
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "🆚 League vs $oppClubName ($venue)",
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                }
                            }
                            if (leftoverLeagueCount > 0) {
                                Text(text = "+ $leftoverLeagueCount more fixtures", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ADVANCE BUTTON
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(if (isAdvancing) MutedGrey else PitchGreen)
                            .clickable(enabled = !isAdvancing) { viewModel.advanceMonth() }
                            .testTag("advance_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (isAdvancing) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "PLAYING...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.Black
                                )
                            } else {
                                Text(
                                    text = "ADVANCE",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = Color.Black
                                )
                                Text(
                                    text = getMonthName(gameState.currentMonthIndex).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color.Black.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // BitLife Narrative Feed
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "CAREER TIMELINE LOGS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PitchGreen,
                    letterSpacing = 1.sp,
                    modifier = Modifier.align(Alignment.Start)
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = SportsCardBg),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                ) {
                    val logs = gameState.narrativeLog.split("\n\n").filter { it.isNotBlank() }
                    val logListState = androidx.compose.foundation.lazy.rememberLazyListState()
                    val coroutineScope = rememberCoroutineScope()
                    val showScrollToTopButton by remember {
                        derivedStateOf {
                            logListState.firstVisibleItemIndex > 0
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (logs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Your career history will unfold here. Press ADVANCE to play matches!",
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            LazyColumn(
                                state = logListState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(logs) { logItem ->
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = logItem,
                                            fontSize = 13.sp,
                                            color = TextPrimary,
                                            lineHeight = 18.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Divider(color = BorderColor.copy(alpha = 0.4f))
                                    }
                                }
                            }

                            // Floating Scroll-To-Top Button
                            if (showScrollToTopButton) {
                                FloatingActionButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            logListState.animateScrollToItem(0)
                                        }
                                    },
                                    containerColor = PitchGreen,
                                    contentColor = DarkSlate,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(16.dp)
                                        .size(40.dp),
                                    shape = CircleShape
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Scroll to top",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
