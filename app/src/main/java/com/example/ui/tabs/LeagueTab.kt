package com.example.ui.tabs

import com.example.ui.components.ClubCrestIcon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.FictionalData
import com.example.data.PlayerEntity
import com.example.ui.CareerViewModel
import com.example.ui.getMonthName
import com.example.ui.theme.*

@Composable
fun LeagueTab(viewModel: CareerViewModel, player: PlayerEntity) {
    val selectedCountry by viewModel.selectedStandingCountry.collectAsStateWithLifecycle()
    val standings by viewModel.currentStandings.collectAsStateWithLifecycle()
    val clubs by viewModel.clubsFlow.collectAsStateWithLifecycle()
    val fixtures by viewModel.fixturesFlow.collectAsStateWithLifecycle()

    val clubsMap = remember(clubs) { clubs.associateBy { it.id } }
    val myClubCountry = remember(clubs, player.currentClubId) {
        clubsMap[player.currentClubId]?.country
    }

    var viewMode by remember { mutableStateOf("DOMESTIC") } // "DOMESTIC" or "EUROPE"
    var selectedComp by remember { mutableStateOf("CHAMPIONS_LEAGUE") } // "CHAMPIONS_LEAGUE", "EUROPA_LEAGUE", "CONFERENCE_LEAGUE"

    val compFixtures = remember(fixtures, selectedComp) {
        fixtures.filter { it.competition == selectedComp }
    }

    val sortedRounds = remember(compFixtures) {
        compFixtures.groupBy { it.round }.toSortedMap(compareByDescending { it })
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("league_tab"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (viewMode == "DOMESTIC") PitchGreen else DarkSlate)
                        .clickable { viewMode = "DOMESTIC" }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DOMESTIC LEAGUES",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (viewMode == "DOMESTIC") Color.Black else TextPrimary
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (viewMode == "EUROPE") PitchGreen else DarkSlate)
                        .clickable { viewMode = "EUROPE" }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "EUROPEAN CUPS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (viewMode == "EUROPE") Color.Black else TextPrimary
                    )
                }
            }
        }

        if (viewMode == "DOMESTIC") {
            item {
                Text(
                    text = "DOMESTIC STANDINGS",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PitchGreen
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Country selectors
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FictionalData.COUNTRIES.forEach { country ->
                        val isSelected = selectedCountry == country
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) PitchGreen else DarkSlate)
                                .clickable { viewModel.selectStandingCountry(country) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = country.take(3).uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isSelected) Color.Black else TextPrimary
                            )
                            if (country == myClubCountry) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(3.dp)
                                        .size(5.dp)
                                        .background(if (isSelected) Color.Black.copy(alpha = 0.4f) else PitchGreen, CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            // Standings Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSlate, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "POS", modifier = Modifier.width(48.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextSecondary)
                    Text(text = "CLUB", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextSecondary)
                    Text(text = "PL", modifier = Modifier.width(30.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextSecondary, textAlign = TextAlign.Center)
                    Text(text = "GD", modifier = Modifier.width(30.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextSecondary, textAlign = TextAlign.Center)
                    Text(text = "PTS", modifier = Modifier.width(35.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextSecondary, textAlign = TextAlign.Center)
                }
            }

            // Standings list
            items(standings.size) { index ->
                val standing = standings[index]
                val club = clubsMap[standing.clubId]
                val isMyClub = club?.id == player.currentClubId

                val cardBg = if (isMyClub) PitchGreen.copy(alpha = 0.15f) else SportsCardBg
                val borderModifier = if (isMyClub) {
                    Modifier.border(1.dp, PitchGreen, RoundedCornerShape(6.dp))
                } else Modifier

                val zoneLabel = when (index) {
                    0, 1 -> "UCL"
                    2, 3 -> "UEL"
                    4, 5 -> "UECL"
                    else -> null
                }
                val zoneColor = when (index) {
                    0, 1 -> Color(0xFF00B0FF)
                    2, 3 -> TrophyGold
                    4, 5 -> Color(0xFF00E676)
                    else -> Color.Transparent
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(borderModifier)
                        .clickable {
                            if (club != null) {
                                viewModel.selectClubForProfile(club.id)
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.width(56.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = if (index < 2) PitchGreen else TextPrimary,
                                modifier = Modifier.width(24.dp)
                            )
                            if (zoneLabel != null) {
                                Box(
                                    modifier = Modifier
                                        .width(28.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(zoneColor.copy(alpha = 0.15f))
                                        .padding(vertical = 1.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = zoneLabel,
                                        fontSize = if (zoneLabel.length > 3) 6.sp else 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = zoneColor,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        ClubCrestIcon(
                            clubId = standing.clubId,
                            clubName = club?.name ?: "Unknown FC",
                            size = 20.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = club?.name ?: "Unknown FC",
                            modifier = Modifier.weight(1f),
                            fontWeight = if (isMyClub) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            color = if (isMyClub) PitchGreen else TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = standing.played.toString(),
                            modifier = Modifier.width(30.dp),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = TextPrimary
                        )

                        val gd = standing.goalsFor - standing.goalsAgainst
                        val gdSign = if (gd > 0) "+$gd" else "$gd"
                        Text(
                            text = gdSign,
                            modifier = Modifier.width(30.dp),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = if (gd > 0) PitchGreen else if (gd < 0) MutedRed else TextSecondary
                        )

                        Text(
                            text = standing.points.toString(),
                            modifier = Modifier.width(35.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = if (isMyClub) PitchGreen else TextPrimary
                        )
                    }
                }
            }
        } else {
            // European Cups view
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val comps = listOf(
                        "CHAMPIONS_LEAGUE" to "UCL",
                        "EUROPA_LEAGUE" to "UEL",
                        "CONFERENCE_LEAGUE" to "UECL"
                    )
                    comps.forEach { (compKey, compLabel) ->
                        val isSelected = selectedComp == compKey
                        val activeColor = when (compKey) {
                            "CHAMPIONS_LEAGUE" -> Color(0xFF00B0FF)
                            "EUROPA_LEAGUE" -> TrophyGold
                            else -> Color(0xFF00E676)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) activeColor else DarkSlate)
                                .clickable { selectedComp = compKey }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = compLabel,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isSelected) Color.Black else TextPrimary
                            )
                        }
                    }
                }
            }

            if (sortedRounds.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SportsCardBg),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🏆 NO EUROPEAN MATCHES YET",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TrophyGold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "European competitions will generate their brackets once the preliminary qualifying conditions are met next season.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                sortedRounds.forEach { (round, roundFixtures) ->
                    val roundTitle = when (round) {
                        11 -> "DIRECT QUALIFIERS (BYE TO QUARTER-FINALS)"
                        10 -> "PRELIMINARY QUALIFYING ROUND"
                        8 -> "QUARTER-FINALS (ROUND OF 8)"
                        4 -> "SEMI-FINALS"
                        2 -> "THE FINAL"
                        else -> "ROUND OF $round"
                    }

                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = roundTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PitchGreen,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (round == 11) {
                        // Direct entrants listing
                        items(roundFixtures.size) { fIndex ->
                            val fx = roundFixtures[fIndex]
                            val homeClub = clubsMap[fx.homeClubId]
                            val isMyClub = fx.homeClubId == player.currentClubId

                            Card(
                                colors = CardDefaults.cardColors(containerColor = SportsCardBg),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (isMyClub) 1.dp else 0.dp,
                                        color = if (isMyClub) PitchGreen else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        ClubCrestIcon(
                                            clubId = homeClub?.id ?: 0,
                                            clubName = homeClub?.name ?: "FC",
                                            size = 28.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = (homeClub?.name ?: "Unknown") + (if (isMyClub) " (YOU)" else ""),
                                            fontSize = 13.sp,
                                            fontWeight = if (isMyClub) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isMyClub) PitchGreen else TextPrimary
                                        )
                                    }
                                    Text(
                                        text = "BYE 🏆",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TrophyGold
                                    )
                                }
                            }
                        }
                    } else {
                        // Standard knockout matchup display grouped by set of participating teams
                        val matchupGroups = roundFixtures.groupBy { setOf(it.homeClubId, it.awayClubId) }.values.toList()

                        items(matchupGroups.size) { mIndex ->
                            val legs = matchupGroups[mIndex]
                            val isDoubleLeg = legs.size >= 2
                            val leg1 = legs.find { it.leg == 1 } ?: legs[0]
                            val leg2 = legs.find { it.leg == 2 }

                            val clubA = clubsMap[leg1.homeClubId]
                            val clubB = clubsMap[leg1.awayClubId]

                            val isClubAMyTeam = leg1.homeClubId == player.currentClubId
                            val isClubBMyTeam = leg1.awayClubId == player.currentClubId
                            val anyMyTeam = isClubAMyTeam || isClubBMyTeam

                            Card(
                                colors = CardDefaults.cardColors(containerColor = SportsCardBg),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (anyMyTeam) 1.dp else 0.dp,
                                        color = if (anyMyTeam) PitchGreen else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // Matchup teams header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = (clubA?.name ?: "Unknown") + (if (isClubAMyTeam) " (YOU)" else ""),
                                            fontSize = 13.sp,
                                            fontWeight = if (isClubAMyTeam) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isClubAMyTeam) PitchGreen else TextPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "VS",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondary,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                        Text(
                                            text = (clubB?.name ?: "Unknown") + (if (isClubBMyTeam) " (YOU)" else ""),
                                            fontSize = 13.sp,
                                            fontWeight = if (isClubBMyTeam) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isClubBMyTeam) PitchGreen else TextPrimary,
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.End
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = BorderColor.copy(alpha = 0.3f), thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Scores and details
                                    if (isDoubleLeg && leg2 != null) {
                                        val l1ScoreA = leg1.homeScore
                                        val l1ScoreB = leg1.awayScore
                                        val l2ScoreA = leg2.awayScore // leg 1 home is leg 2 away
                                        val l2ScoreB = leg2.homeScore

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Leg 1 (Home ${clubA?.name?.take(3)?.uppercase()}): " +
                                                            (if (leg1.isSimulated) "$l1ScoreA - $l1ScoreB" else "Pending (Month ${getMonthName(leg1.monthIndex)})"),
                                                    fontSize = 11.sp,
                                                    color = TextSecondary
                                                )
                                                val leg2Text = if (leg2.isSimulated) {
                                                    val baseScore = "$l2ScoreB - $l2ScoreA"
                                                    if (leg2.homePens != null && leg2.awayPens != null) {
                                                        "$baseScore (a.e.t., ${leg2.homePens}-${leg2.awayPens} pens)"
                                                    } else if (leg2.wentToExtraTime) {
                                                        "$baseScore (a.e.t.)"
                                                    } else {
                                                        baseScore
                                                    }
                                                } else {
                                                    "Pending (Month ${getMonthName(leg2.monthIndex)})"
                                                }
                                                Text(
                                                    text = "Leg 2 (Home ${clubB?.name?.take(3)?.uppercase()}): $leg2Text",
                                                    fontSize = 11.sp,
                                                    color = TextSecondary
                                                )
                                            }

                                            if (leg1.isSimulated && leg2.isSimulated) {
                                                val aggA = (l1ScoreA ?: 0) + (l2ScoreA ?: 0)
                                                val aggB = (l1ScoreB ?: 0) + (l2ScoreB ?: 0)
                                                val aggText = if (leg2.homePens != null && leg2.awayPens != null) {
                                                    "AGG: $aggA - $aggB (${leg2.awayPens}-${leg2.homePens} pens)"
                                                } else if (leg2.wentToExtraTime) {
                                                    "AGG: $aggA - $aggB (a.e.t.)"
                                                } else {
                                                    "AGG: $aggA - $aggB"
                                                }
                                                Text(
                                                    text = aggText,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TrophyGold
                                                )
                                            }
                                        }
                                    } else {
                                        // Single leg (The Final)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Single Leg Final",
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                            val finalScoreText = if (leg1.isSimulated) {
                                                val baseScore = "${leg1.homeScore} - ${leg1.awayScore}"
                                                if (leg1.homePens != null && leg1.awayPens != null) {
                                                    "$baseScore (a.e.t., ${leg1.homePens}-${leg1.awayPens} pens)"
                                                } else if (leg1.wentToExtraTime) {
                                                    "$baseScore (a.e.t.)"
                                                } else {
                                                    baseScore
                                                }
                                            } else {
                                                "Pending (Month ${getMonthName(leg1.monthIndex)})"
                                            }
                                            Text(
                                                text = finalScoreText,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (leg1.isSimulated) TrophyGold else TextSecondary
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
    }
}
