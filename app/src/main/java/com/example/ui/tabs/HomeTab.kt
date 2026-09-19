package com.example.ui.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ClubEntity
import com.example.data.FaceDescriptor
import com.example.data.GameStateEntity
import com.example.data.PlayerEntity
import com.example.data.formatSeasonYear
import com.example.ui.CareerViewModel
import com.example.ui.components.PlayerFaceIcon
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkSlate)
                                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                PlayerFaceIcon(
                                    descriptor = FaceDescriptor.deserialize(player.faceDescriptor),
                                    age = player.age,
                                    form = player.form,
                                    modifier = Modifier.size(48.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "STRIKER PROFILE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PitchGreen,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = player.name,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Age: ${player.age} (Gen ${player.generation})",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.showFamilyPage(true) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSlate)
                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                .testTag("family_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = "Family",
                                tint = PitchGreen,
                                modifier = Modifier.size(18.dp)
                            )
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
                    HorizontalDivider(color = BorderColor)
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // National Team Row
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSlate
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val natCode = player.nationalTeamCode
                            val natName = if (natCode != null) com.example.data.nationByCode(natCode)?.name ?: natCode else null
                            if (natName != null) {
                                Text(
                                    text = "🌐 $natName",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PitchGreen
                                )
                                Text(
                                    text = "${player.nationalTeamCaps} caps",
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            } else {
                                Text(
                                    text = "🌐 International",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "Uncapped",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = BorderColor)
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
                    HorizontalDivider(color = BorderColor)
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

                    Spacer(modifier = Modifier.height(4.dp))

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
                                            text = "vs $oppClubName ($venue)",
                                            fontSize = 13.sp,
                                            color = TextPrimary
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "League vs $oppClubName ($venue)",
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
                }
            }
        }

        // Redesigned BitLife Narrative Feed / Career Timeline
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val logs = remember(gameState.narrativeLog) {
                    gameState.narrativeLog.split("\n\n").filter { it.isNotBlank() }
                }
                val parsedEvents = remember(logs) {
                    logs.map { parseCareerLog(it) }
                }

                var selectedFilter by remember { mutableStateOf(CareerTimelineType.ALL) }
                var searchQuery by remember { mutableStateOf("") }
                var showSearchField by remember { mutableStateOf(false) }

                val filteredEvents = remember(parsedEvents, selectedFilter, searchQuery) {
                    parsedEvents.filter { event ->
                        val matchesType = selectedFilter == CareerTimelineType.ALL || event.type == selectedFilter
                        val matchesSearch = searchQuery.isBlank() ||
                                event.rawText.contains(searchQuery, ignoreCase = true) ||
                                event.period.contains(searchQuery, ignoreCase = true)
                        matchesType && matchesSearch
                    }
                }

                val groupedEvents = remember(filteredEvents) {
                    filteredEvents.groupBy { it.period }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CAREER TIMELINE LOGS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PitchGreen,
                        letterSpacing = 1.sp
                    )

                    IconButton(
                        onClick = {
                            showSearchField = !showSearchField
                            if (!showSearchField) searchQuery = ""
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (showSearchField) Icons.Default.Clear else Icons.Default.Search,
                            contentDescription = "Search Logs",
                            tint = if (showSearchField) PitchGreen else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Search Bar
                AnimatedVisibility(
                    visible = showSearchField,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search goals, trophies, clubs...", fontSize = 12.sp, color = TextSecondary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PitchGreen,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = DarkSlate,
                            unfocusedContainerColor = DarkSlate,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(CareerTimelineType.entries) { filterType ->
                        val isSelected = selectedFilter == filterType
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) PitchGreen else DarkSlate,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) PitchGreen else BorderColor
                            ),
                            modifier = Modifier.clickable { selectedFilter = filterType }
                        ) {
                            Text(
                                text = "${filterType.icon} ${filterType.label}",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) DarkSlate else TextPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = SportsCardBg),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    if (groupedEvents.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank() || selectedFilter != CareerTimelineType.ALL)
                                    "No career events found matching filters."
                                else
                                    "Your career history will unfold here. Press ADVANCE to play matches!",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }
                        val logListState = androidx.compose.foundation.lazy.rememberLazyListState()
                        val coroutineScope = rememberCoroutineScope()
                        val showScrollToTopButton by remember {
                            derivedStateOf { logListState.firstVisibleItemIndex > 0 }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                state = logListState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                groupedEvents.forEach { (period, eventsInPeriod) ->
                                    val isExpanded = expandedGroups[period] ?: true
                                    item(key = "header_$period") {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = DarkSlate,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    expandedGroups[period] = !isExpanded
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(PitchGreen)
                                                    )
                                                    Text(
                                                        text = period,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        text = "(${eventsInPeriod.size})",
                                                        fontSize = 11.sp,
                                                        color = TextSecondary
                                                    )
                                                }
                                                Icon(
                                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (isExpanded) {
                                        items(eventsInPeriod) { eventItem ->
                                            EventTimelineCard(event = eventItem)
                                        }
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
                                        .padding(14.dp)
                                        .size(38.dp),
                                    shape = CircleShape
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Scroll to top",
                                        modifier = Modifier.size(18.dp)
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

@Composable
private fun EventTimelineCard(event: ParsedCareerEvent) {
    var expandedDetails by remember { mutableStateOf(false) }

    val bgModifier = when {
        event.type == CareerTimelineType.TROPHIES ->
            Modifier.background(TrophyGold.copy(alpha = 0.12f)).border(1.dp, TrophyGold.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
        event.type == CareerTimelineType.TRANSFERS ->
            Modifier.background(MutedBlue.copy(alpha = 0.12f)).border(1.dp, MutedBlue.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
        event.isMajor ->
            Modifier.background(PitchGreen.copy(alpha = 0.10f)).border(1.dp, PitchGreen.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
        else ->
            Modifier.background(DarkSlate.copy(alpha = 0.65f)).border(1.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .then(bgModifier)
            .clickable { expandedDetails = !expandedDetails }
            .padding(10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = event.type.icon,
                    fontSize = 16.sp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = event.typeName.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (event.type) {
                                CareerTimelineType.TROPHIES -> TrophyGold
                                CareerTimelineType.TRANSFERS -> MutedBlue
                                CareerTimelineType.MATCHES -> PitchGreen
                                else -> TextSecondary
                            },
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "• ${event.period}",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = if (expandedDetails) event.rawText else event.summary,
                        fontSize = 12.sp,
                        fontWeight = if (event.isMajor) FontWeight.SemiBold else FontWeight.Normal,
                        color = TextPrimary,
                        maxLines = if (expandedDetails) Int.MAX_VALUE else 2,
                        overflow = if (expandedDetails) TextOverflow.Clip else TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

private enum class CareerTimelineType(val label: String, val icon: String, val chipColor: Color) {
    ALL("All", "⚡", PitchGreen),
    TROPHIES("Trophies", "🏆", TrophyGold),
    TRANSFERS("Transfers", "🔄", MutedBlue),
    MATCHES("Matches", "⚽", PitchGreen),
    TRAINING("Training", "🏋️", TextSecondary),
    DECISIONS("Events", "💡", TrophyGold)
}

private data class ParsedCareerEvent(
    val rawText: String,
    val period: String,
    val type: CareerTimelineType,
    val typeName: String,
    val summary: String,
    val isMajor: Boolean
)

private fun parseCareerLog(log: String): ParsedCareerEvent {
    val clean = log.trim()
    val lines = clean.lines().filter { it.isNotBlank() }
    val firstLine = lines.firstOrNull() ?: ""

    val periodRegex = Regex("""^Season\s+(\d+)(?:\s+\([^)]+\))?,\s+([A-Za-z]+):""")
    val match = periodRegex.find(firstLine)
    val period = if (match != null) {
        val sNum = match.groupValues[1]
        val month = match.groupValues[2]
        "Season $sNum • $month"
    } else {
        if (firstLine.contains("Season ", ignoreCase = true) && firstLine.contains(":")) {
            firstLine.substringBefore(":").trim()
        } else {
            "Early Career"
        }
    }

    val contentLines = if (match != null || (firstLine.endsWith(":") && firstLine.contains("Season"))) lines.drop(1) else lines
    val mainBody = contentLines.joinToString(" ").trim()
    val upper = clean.uppercase()

    val (type, typeName, isMajor) = when {
        upper.contains("TROPHY") || upper.contains("CHAMPION") || upper.contains("BALLON D'OR") || upper.contains("PLAYER OF THE YEAR") || upper.contains("🏆") ->
            Triple(CareerTimelineType.TROPHIES, "Trophy", true)
        upper.contains("TRANSFER") || upper.contains("SIGNED") || upper.contains("SCOUTED") || upper.contains("OFFER ACCEPTED") || upper.contains("🔄") ->
            Triple(CareerTimelineType.TRANSFERS, "Transfer", true)
        upper.contains("MATCH REPORT") || upper.contains("GOALS:") || upper.contains("RATING:") || upper.contains("⚽") ->
            Triple(CareerTimelineType.MATCHES, "Match", false)
        upper.contains("TRAIN") || upper.contains("DRILL") || upper.contains("OVERTRAINED") || upper.contains("🏋") ->
            Triple(CareerTimelineType.TRAINING, "Training", false)
        upper.contains("EVENT RESULT") || upper.contains("PROMISE") || upper.contains("DECISION") || upper.contains("💡") || upper.contains("👔") ->
            Triple(CareerTimelineType.DECISIONS, "Choice", false)
        else ->
            Triple(CareerTimelineType.MATCHES, "Update", false)
    }

    val summary = when {
        mainBody.startsWith("⚽ MATCH REPORT:") -> mainBody.removePrefix("⚽ MATCH REPORT:").trim()
        mainBody.startsWith("💡 Event Result:") -> mainBody.removePrefix("💡 Event Result:").trim()
        mainBody.startsWith("🔄 TRANSFER COMPLETED:") -> mainBody.removePrefix("🔄 TRANSFER COMPLETED:").trim()
        mainBody.startsWith("🏆") || mainBody.startsWith("🏋️") || mainBody.startsWith("👔") -> mainBody.drop(2).trim()
        else -> mainBody.ifEmpty { clean }
    }.take(90)

    return ParsedCareerEvent(
        rawText = clean,
        period = period,
        type = type,
        typeName = typeName,
        summary = summary,
        isMajor = isMajor
    )
}
