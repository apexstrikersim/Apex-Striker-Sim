package com.example.ui

import com.example.ui.theme.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PlayerEntity
import com.example.data.YouthAcademyEntity
import com.example.data.YouthFixtureEntity
import com.example.data.YouthStandingEntity

@Composable
fun YouthLeagueScreen(
    player: PlayerEntity,
    academies: List<YouthAcademyEntity>,
    standings: List<YouthStandingEntity>,
    fixtures: List<YouthFixtureEntity>,
    scoutOffersCount: Int,
    onAdvanceMonth: () -> Unit,
    onOpenTraining: () -> Unit,
    onOpenScoutOffers: () -> Unit,
    onRequestProUpgrade: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val academyMap = remember(academies) { academies.associateBy { it.id } }
    val myAcademy = academyMap[player.currentAcademyId]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(12.dp)
    ) {
        // Header Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, PitchGreen.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "YOUTH ACADEMY LEAGUE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PitchGreen,
                            letterSpacing = 1.1.sp
                        )
                        Text(
                            text = myAcademy?.academyName ?: "Free Agent Youth",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(PitchGreen.copy(alpha = 0.2f))
                            .border(1.dp, PitchGreen, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Age ${player.age}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PitchGreen,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    YouthStatBadge("Goals", player.youthGoals.toString(), CoralRed)
                    YouthStatBadge("Assists", player.youthAssists.toString(), PitchGreen)
                    YouthStatBadge("MVPs", player.youthMvps.toString(), GoldStar)
                    YouthStatBadge("OVR", player.ovr.toString(), Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CardBackground,
            contentColor = PitchGreen,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = PitchGreen
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Standings", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Fixtures", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tab Content
        Box(modifier = Modifier.weight(1f)) {
            if (selectedTab == 0) {
                // League Standings Table
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardBackground)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("#", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.width(24.dp))
                            Text("Academy", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                            Text("MP", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                            Text("GD", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                            Text("Pts", fontSize = 11.sp, color = PitchGreen, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                        }
                    }

                    itemsIndexed(standings) { index, row ->
                        val academy = academyMap[row.academyId]
                        val isMyAcademy = row.academyId == player.currentAcademyId

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isMyAcademy) PitchGreen.copy(alpha = 0.25f) else CardBackground)
                                .border(1.dp, if (isMyAcademy) PitchGreen else CardBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMyAcademy) PitchGreen else Color.White,
                                modifier = Modifier.width(24.dp)
                            )

                            Text(
                                text = academy?.academyName ?: "Academy #${row.academyId}",
                                fontSize = 12.sp,
                                fontWeight = if (isMyAcademy) FontWeight.Bold else FontWeight.Normal,
                                color = if (isMyAcademy) PitchGreen else Color.White,
                                modifier = Modifier.weight(1f)
                            )

                            Text("${row.played}", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                            Text("${row.goalsFor - row.goalsAgainst}", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                            Text("${row.points}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isMyAcademy) PitchGreen else Color.White, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                // Fixtures List
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(fixtures) { _, fix ->
                        val homeName = academyMap[fix.homeAcademyId]?.academyName ?: "Academy #${fix.homeAcademyId}"
                        val awayName = academyMap[fix.awayAcademyId]?.academyName ?: "Academy #${fix.awayAcademyId}"
                        val isUserMatch = fix.homeAcademyId == player.currentAcademyId || fix.awayAcademyId == player.currentAcademyId

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = if (isUserMatch) PitchGreen.copy(alpha = 0.15f) else CardBackground),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isUserMatch) PitchGreen else CardBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = homeName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End
                                )

                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(DarkBackground)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    if (fix.isSimulated && fix.homeScore != null && fix.awayScore != null) {
                                        Text(
                                            text = "${fix.homeScore} - ${fix.awayScore}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PitchGreen
                                        )
                                    } else {
                                        Text(
                                            text = "VS",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Text(
                                    text = awayName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onOpenTraining,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldStar),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldStar)
            ) {
                Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Drills", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            if (scoutOffersCount > 0 || player.age >= 16) {
                Button(
                    onClick = if (player.age >= 16) onRequestProUpgrade else onOpenScoutOffers,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldStar)
                ) {
                    Icon(Icons.Default.MilitaryTech, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (player.age >= 16) "Pro Offers" else "Offers ($scoutOffersCount)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkBackground)
                }
            }

            Button(
                onClick = onAdvanceMonth,
                modifier = Modifier.weight(1.2f),
                colors = ButtonDefaults.buttonColors(containerColor = PitchGreen)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Advance", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkBackground)
            }
        }
    }
}

@Composable
fun YouthStatBadge(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
        Text(label, fontSize = 10.sp, color = TextSecondary)
    }
}
