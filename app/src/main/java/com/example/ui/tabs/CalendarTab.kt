package com.example.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ClubEntity
import com.example.data.FixtureEntity
import com.example.data.PlayerEntity
import com.example.data.formatSeasonYear
import com.example.ui.CareerViewModel
import com.example.ui.components.BadgeIcon
import com.example.ui.getMonthName
import com.example.ui.theme.*

@Composable
fun CalendarTab(viewModel: CareerViewModel) {
    val fixtures by viewModel.fixturesFlow.collectAsStateWithLifecycle()
    val playerState by viewModel.playerFlow.collectAsStateWithLifecycle()
    val player = playerState ?: return
    val clubs by viewModel.clubsFlow.collectAsStateWithLifecycle()
    val gameStateState by viewModel.gameStateFlow.collectAsStateWithLifecycle()
    val currentMonthIndex = gameStateState?.currentMonthIndex ?: 0

    val clubsMap = remember(clubs) { clubs.associateBy { it.id } }

    var selectedMonthIndex by remember(currentMonthIndex) { mutableStateOf(currentMonthIndex) }

    val monthAbbreviations = listOf("AUG", "SEP", "OCT", "NOV", "DEC", "JAN", "FEB", "MAR", "APR", "MAY")
    val daysInMonth = remember { listOf(31, 30, 31, 30, 31, 31, 28, 31, 30, 31) }
    val totalDays = daysInMonth[selectedMonthIndex]

    val monthStartOffsets = remember(daysInMonth) {
        val offsets = IntArray(10)
        var currentOffset = 5 // August 1st starts on Saturday (0=Mon, 5=Sat)
        for (i in 0 until 10) {
            offsets[i] = currentOffset
            currentOffset = (currentOffset + daysInMonth[i]) % 7
        }
        offsets
    }
    val firstWeekdayOffset = monthStartOffsets[selectedMonthIndex]
    val currentSeason = gameStateState?.currentSeason ?: 1
    val seasonYearDisplay = formatSeasonYear(currentSeason)

    val monthFixtures = remember(fixtures, selectedMonthIndex, player.currentClubId) {
        fixtures.filter { 
            it.monthIndex == selectedMonthIndex && 
            (it.homeClubId == player.currentClubId || it.awayClubId == player.currentClubId)
        }
    }

    val sortedFixtures = remember(monthFixtures) {
        monthFixtures.sortedWith(compareBy({ it.round }, { it.id }))
    }

    val dayFixtureMap = remember(sortedFixtures, selectedMonthIndex) {
        val map = mutableMapOf<Int, FixtureEntity>()
        val n = sortedFixtures.size
        if (n > 0) {
            val start = 3
            val end = totalDays - 2
            if (n == 1) {
                map[totalDays / 2] = sortedFixtures[0]
            } else {
                sortedFixtures.forEachIndexed { index, fixture ->
                    val day = start + ((index.toFloat() / (n - 1).toFloat()) * (end - start)).toInt()
                    map[day] = fixture
                }
            }
        }
        map
    }

    var selectedFixtureForDetail by remember { mutableStateOf<FixtureEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var totalDrag = 0f
                var hasNavigated = false
                detectHorizontalDragGestures(
                    onDragStart = {
                        totalDrag = 0f
                        hasNavigated = false
                    },
                    onDragEnd = {
                        totalDrag = 0f
                        hasNavigated = false
                    },
                    onDragCancel = {
                        totalDrag = 0f
                        hasNavigated = false
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                        if (!hasNavigated) {
                            if (totalDrag < -50f && selectedMonthIndex < 9) {
                                selectedMonthIndex++
                                hasNavigated = true
                            } else if (totalDrag > 50f && selectedMonthIndex > 0) {
                                selectedMonthIndex--
                                hasNavigated = true
                            }
                        }
                    }
                )
            }
            .testTag("calendar_tab")
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedMonthIndex,
            containerColor = Color.Transparent,
            contentColor = PitchGreen,
            edgePadding = 16.dp,
            divider = {}
        ) {
            monthAbbreviations.forEachIndexed { index, abbrev ->
                val isCurrent = index == currentMonthIndex
                Tab(
                    selected = selectedMonthIndex == index,
                    onClick = { selectedMonthIndex = index },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = abbrev,
                                fontWeight = if (selectedMonthIndex == index) FontWeight.Black else FontWeight.Normal,
                                color = if (selectedMonthIndex == index) PitchGreen else if (isCurrent) TrophyGold else TextSecondary,
                                fontSize = 13.sp
                            )
                            if (isCurrent) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(TrophyGold)
                                )
                            }
                        }
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "${getMonthName(selectedMonthIndex).uppercase()} ($seasonYearDisplay) STATUS",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = PitchGreen,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Text(
            text = "Tap any highlighted matchday to view details",
            fontSize = 11.sp,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Header row for days of the week: Mon, Tue, Wed, Thu, Fri, Sat, Sun
        val daysOfWeek = listOf("M", "T", "W", "T", "F", "S", "S")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (dayName in daysOfWeek) {
                Text(
                    text = dayName,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(firstWeekdayOffset) {
                Spacer(modifier = Modifier.aspectRatio(1f))
            }

            items(
                count = totalDays,
                key = { dayIndex -> dayIndex + 1 }
            ) { dayIndex ->
                val day = dayIndex + 1
                val fixture = dayFixtureMap[day]
                Box(
                    modifier = Modifier.aspectRatio(1f)
                ) {
                    CalendarDayCell(
                        day = day,
                        fixture = fixture,
                        player = player,
                        clubsMap = clubsMap,
                        onClick = {
                            if (fixture != null) {
                                selectedFixtureForDetail = fixture
                            }
                        }
                    )
                }
            }
        }
        
        selectedFixtureForDetail?.let { fx ->
            FixtureDetailDialog(
                fixture = fx,
                player = player,
                clubsMap = clubsMap,
                onDismiss = { selectedFixtureForDetail = null }
            )
        }
    }
}

@Composable
fun CalendarDayCell(
    day: Int,
    fixture: FixtureEntity?,
    player: PlayerEntity,
    clubsMap: Map<Int, ClubEntity>,
    onClick: () -> Unit
) {
    val isMatchDay = fixture != null
    val isHome = fixture?.homeClubId == player.currentClubId
    val oppClub = if (isHome) clubsMap[fixture?.awayClubId] else clubsMap[fixture?.homeClubId]
    
    val isPlayed = fixture?.isSimulated == true
    val isBenched = isPlayed && fixture?.playerCameOnMinute == null
    
    val homeScore = fixture?.homeScore ?: 0
    val awayScore = fixture?.awayScore ?: 0
    val homePens = fixture?.homePens
    val awayPens = fixture?.awayPens
    
    val isPlayerWin = if (homePens != null && awayPens != null) {
        (isHome && homePens > awayPens) || (!isHome && awayPens > homePens)
    } else {
        (isHome && homeScore > awayScore) || (!isHome && awayScore > homeScore)
    }
    val isDraw = homeScore == awayScore && (homePens == null || awayPens == null)
    
    val resultColor = when {
        isDraw -> MutedGrey
        isPlayerWin -> PitchGreen
        else -> MutedRed
    }
    
    val isEuro = fixture?.competition != "LEAGUE" && fixture != null
    val borderColor = when {
        isEuro -> Color(0xFF1A73E8)
        isMatchDay -> MutedGrey
        else -> BorderColor.copy(alpha = 0.3f)
    }
    
    val bgColor = when {
        isPlayed && isBenched -> DarkSlate.copy(alpha = 0.3f)
        isPlayed -> resultColor.copy(alpha = 0.25f)
        isMatchDay -> SportsCardBg
        else -> Color.Transparent
    }
    
    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .border(
                width = if (isEuro) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(enabled = isMatchDay) { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$day",
                    fontSize = 9.sp,
                    color = if (isMatchDay) TextPrimary else TextSecondary.copy(alpha = 0.6f),
                    fontWeight = if (isMatchDay) FontWeight.Bold else FontWeight.Normal
                )
                if (fixture != null) {
                    val hasContrib = fixture.playerGoals > 0 || fixture.playerAssists > 0
                    if (hasContrib) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(TrophyGold, CircleShape)
                        )
                    }
                }
            }
            
            if (fixture != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = oppClub?.name?.take(2)?.uppercase() ?: "OP",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isEuro) Color(0xFF64B5F6) else PitchGreen,
                        maxLines = 1
                    )
                    
                    val compAbbrev = when (fixture.competition) {
                        "LEAGUE" -> "Lg"
                        "CHAMPIONS_LEAGUE" -> "UCL"
                        "EUROPA_LEAGUE" -> "UEL"
                        "CONFERENCE_LEAGUE" -> "UEC"
                        "SUPER_CUP" -> "USC"
                        else -> "Cup"
                    }
                    Text(
                        text = compAbbrev,
                        fontSize = 7.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(1.dp))
            }
        }
    }
}

@Composable
fun FixtureDetailDialog(
    fixture: FixtureEntity,
    player: PlayerEntity,
    clubsMap: Map<Int, ClubEntity>,
    onDismiss: () -> Unit
) {
    val homeClub = clubsMap[fixture.homeClubId]
    val awayClub = clubsMap[fixture.awayClubId]
    val isPlayed = fixture.isSimulated
    
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = PitchGreen, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Column {
                val compName = when (fixture.competition) {
                    "LEAGUE" -> "DOMESTIC LEAGUE"
                    "CHAMPIONS_LEAGUE" -> "UEFA CHAMPIONS LEAGUE"
                    "EUROPA_LEAGUE" -> "UEFA EUROPA LEAGUE"
                    "CONFERENCE_LEAGUE" -> "UEFA CONFERENCE LEAGUE"
                    "SUPER_CUP" -> "UEFA SUPER CUP"
                    else -> "CUP MATCH"
                }
                Text(
                    text = compName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (fixture.competition) {
                        "LEAGUE" -> PitchGreen
                        "SUPER_CUP" -> TrophyGold
                        else -> Color(0xFF64B5F6)
                    }
                )
                Text(
                    text = "Matchday Details",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSlate, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SportsCardBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = homeClub?.name?.take(2)?.uppercase() ?: "FC",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (fixture.homeClubId == player.currentClubId) PitchGreen else TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = homeClub?.name ?: "Home",
                            fontSize = 12.sp,
                            maxLines = 1,
                            fontWeight = if (fixture.homeClubId == player.currentClubId) FontWeight.Bold else FontWeight.Normal,
                            color = if (fixture.homeClubId == player.currentClubId) PitchGreen else TextPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        if (isPlayed) {
                            val scoreText = "${fixture.homeScore} - ${fixture.awayScore}"
                            Text(
                                text = scoreText,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                            if (fixture.wentToExtraTime) {
                                Text(
                                    text = "AET",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TrophyGold
                                )
                            }
                            if (fixture.homePens != null && fixture.awayPens != null) {
                                Text(
                                    text = "Pens: ${fixture.homePens} - ${fixture.awayPens}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TrophyGold
                                )
                            }
                        } else {
                            Text(
                                text = "VS",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = TextSecondary
                            )
                        }
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SportsCardBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = awayClub?.name?.take(2)?.uppercase() ?: "FC",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (fixture.awayClubId == player.currentClubId) PitchGreen else TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = awayClub?.name ?: "Away",
                            fontSize = 12.sp,
                            maxLines = 1,
                            fontWeight = if (fixture.awayClubId == player.currentClubId) FontWeight.Bold else FontWeight.Normal,
                            color = if (fixture.awayClubId == player.currentClubId) PitchGreen else TextPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                if (isPlayed) {
                    val isBenched = fixture.playerCameOnMinute == null
                    val isSubbed = fixture.playerCameOnMinute != null && fixture.playerCameOnMinute!! > 0
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "PLAYER PERFORMANCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SportsCardBg),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Match Rating:", fontSize = 13.sp, color = TextSecondary)
                                    val rating = fixture.playerRating ?: 6.0f
                                    Text(
                                        text = "%.1f".format(rating),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            rating >= 8.0f -> PitchGreen
                                            rating >= 6.0f -> TrophyGold
                                            else -> MutedRed
                                        }
                                    )
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Role:", fontSize = 13.sp, color = TextSecondary)
                                    val roleText = when {
                                        isBenched -> "Benched"
                                        isSubbed -> "Subbed in (${fixture.playerCameOnMinute}')"
                                        else -> "Starter"
                                    }
                                    Text(roleText, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Goals / Assists:", fontSize = 13.sp, color = TextSecondary)
                                    Text("${fixture.playerGoals}G / ${fixture.playerAssists}A", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Man of the Match:", fontSize = 13.sp, color = TextSecondary)
                                    Text(if (fixture.playerMvp) "YES ⭐" else "NO", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (fixture.playerMvp) TrophyGold else TextPrimary)
                                }
                            }
                        }
                        
                        val gMinutes = fixture.goalMinutes?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                        val aMinutes = fixture.assistMinutes?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                        
                        if (gMinutes.isNotEmpty() || aMinutes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "CONTRIBUTION MINUTES LOG",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                            
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SportsCardBg),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (gMinutes.isNotEmpty()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.SportsSoccer,
                                                contentDescription = "Goals",
                                                tint = PitchGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Goals: " + gMinutes.joinToString(", ") { "${it}'" },
                                                fontSize = 13.sp,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    if (aMinutes.isNotEmpty()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.DirectionsRun,
                                                contentDescription = "Assists",
                                                tint = TrophyGold,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Assists: " + aMinutes.joinToString(", ") { "${it}'" },
                                                fontSize = 13.sp,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "This match has not been played yet.",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        containerColor = SportsCardBg,
        textContentColor = TextPrimary,
        titleContentColor = TextPrimary,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun MatchFixtureBox(fixture: FixtureEntity, player: PlayerEntity, clubsMap: Map<Int, ClubEntity>) {
    val homeClub = clubsMap[fixture.homeClubId]
    val awayClub = clubsMap[fixture.awayClubId]

    val isHome = player.currentClubId == fixture.homeClubId
    val oppClub = if (isHome) awayClub else homeClub

    val isBenched = fixture.isSimulated && fixture.playerCameOnMinute == null
    val isSubbed = fixture.isSimulated && fixture.playerCameOnMinute != null && fixture.playerCameOnMinute!! > 0

    val opacity = if (isBenched) 0.5f else 1f

    Card(
        colors = CardDefaults.cardColors(containerColor = SportsCardBg.copy(alpha = opacity)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (fixture.playerMvp) TrophyGold else BorderColor,
                RoundedCornerShape(10.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left Subbed highlight line
            if (isSubbed) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(SubLineYellow)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Crests and Names
                Column(modifier = Modifier.weight(1f)) {
                    if (fixture.competition != "LEAGUE") {
                        val badgeColor = when (fixture.competition) {
                            "CHAMPIONS_LEAGUE" -> Color(0xFF1A73E8) // Deep Blue
                            "EUROPA_LEAGUE" -> Color(0xFFE8710A) // Orange/Amber
                            "CONFERENCE_LEAGUE" -> Color(0xFF137333) // Green
                            "SUPER_CUP" -> TrophyGold
                            else -> DarkSlate
                        }
                        val compName = when (fixture.competition) {
                            "CHAMPIONS_LEAGUE" -> "Champions League"
                            "EUROPA_LEAGUE" -> "Europa League"
                            "CONFERENCE_LEAGUE" -> "Conference League"
                            "SUPER_CUP" -> "Super Cup"
                            else -> "Cup"
                        }
                        Box(
                            modifier = Modifier
                                .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .border(0.5.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = compName.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                        }
                    } else {
                        Text(
                            text = "Domestic League",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Initials badge
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(DarkSlate),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = oppClub?.name?.take(2)?.uppercase() ?: "FC",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PitchGreen
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = oppClub?.name ?: "Opponent FC",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    // Played role description
                    if (fixture.isSimulated) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val roleStr = when {
                            isBenched -> "Benched (Did not play)"
                            isSubbed -> "Subbed in at ${fixture.playerCameOnMinute}'"
                            else -> "Started"
                        }
                        Text(text = roleStr, fontSize = 11.sp, color = TextSecondary)
                    }
                }

                // Match Outcome & Player Contrib icons
                if (fixture.isSimulated) {
                    val homeScore = fixture.homeScore ?: 0
                    val awayScore = fixture.awayScore ?: 0
                    val homePens = fixture.homePens
                    val awayPens = fixture.awayPens

                    val isPlayerWin = if (homePens != null && awayPens != null) {
                        (isHome && homePens > awayPens) || (!isHome && awayPens > homePens)
                    } else {
                        (isHome && homeScore > awayScore) || (!isHome && awayScore > homeScore)
                    }
                    val isDraw = homeScore == awayScore && (homePens == null || awayPens == null)

                    val resultChar = if (isDraw) "D" else if (isPlayerWin) "W" else "L"
                    val resultColor = if (isDraw) MutedGrey else if (isPlayerWin) PitchGreen else MutedRed

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Player contrib badges
                        if (!isBenched) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (fixture.playerGoals > 0) {
                                    BadgeIcon(Icons.Default.SportsSoccer, fixture.playerGoals)
                                }
                                if (fixture.playerAssists > 0) {
                                    BadgeIcon(Icons.Default.DirectionsRun, fixture.playerAssists) // cleats representation
                                }
                                if (fixture.playerMvp) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "MVP",
                                        tint = TrophyGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                fixture.playerRating?.let { rating ->
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                when {
                                                    rating >= 8.0f -> PitchGreen.copy(alpha = 0.15f)
                                                    rating >= 6.0f -> TrophyGold.copy(alpha = 0.15f)
                                                    else -> MutedRed.copy(alpha = 0.15f)
                                                },
                                                RoundedCornerShape(4.dp)
                                            )
                                            .border(
                                                0.5.dp,
                                                when {
                                                    rating >= 8.0f -> PitchGreen
                                                    rating >= 6.0f -> TrophyGold
                                                    else -> MutedRed
                                                }.copy(alpha = 0.5f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "%.1f".format(rating),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                rating >= 8.0f -> PitchGreen
                                                rating >= 6.0f -> TrophyGold
                                                else -> MutedRed
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Score box
                        val scoreText = if (homePens != null && awayPens != null) {
                            "$homeScore - $awayScore\n(a.e.t., $homePens-$awayPens pens)"
                        } else if (fixture.wentToExtraTime) {
                            "$homeScore - $awayScore\n(a.e.t.)"
                        } else {
                            "$homeScore - $awayScore"
                        }
                        Text(
                            text = scoreText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            lineHeight = 14.sp,
                            textAlign = TextAlign.Center
                        )

                        // Result char indicator
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(resultColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = resultChar,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = if (isPlayerWin) Color.Black else Color.White
                            )
                        }
                    }
                } else {
                    Text(
                        text = if (isHome) "HOME" else "AWAY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
