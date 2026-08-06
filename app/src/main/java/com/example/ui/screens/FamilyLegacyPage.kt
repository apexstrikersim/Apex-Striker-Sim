package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.GameStateEntity
import com.example.data.PlayerEntity
import com.example.data.TrophyEntity
import com.example.data.formatSeasonYear
import com.example.ui.CareerViewModel
import com.example.ui.tabs.CompetitionTrophyIcon
import com.example.ui.theme.*

@Composable
fun FamilyLegacyPage(
    viewModel: CareerViewModel,
    onClose: () -> Unit
) {
    val legacies by viewModel.legaciesFlow.collectAsStateWithLifecycle()
    val activePlayer by viewModel.playerFlow.collectAsStateWithLifecycle()
    val allSeasonRecords by viewModel.seasonRecordsFlow.collectAsStateWithLifecycle()
    val allTrophies by viewModel.trophiesFlow.collectAsStateWithLifecycle()
    val gameState by viewModel.gameStateFlow.collectAsStateWithLifecycle()
    val clubs by viewModel.clubsFlow.collectAsStateWithLifecycle()
    val clubsMap = remember(clubs) { clubs.associateBy { it.id } }

    data class GenInfo(
        val generation: Int,
        val name: String,
        val isRetired: Boolean,
        val legacy: com.example.data.LegacyEntity?,
        val player: PlayerEntity?
    )

    val generationList = remember(legacies, activePlayer) {
        val list = mutableListOf<GenInfo>()
        legacies.sortedBy { it.generation }.forEach { leg ->
            list.add(GenInfo(leg.generation, leg.name, true, leg, null))
        }
        activePlayer?.let { p ->
            if (list.none { it.generation == p.generation }) {
                list.add(GenInfo(p.generation, p.name, p.isRetired, null, p))
            }
        }
        list.sortedBy { it.generation }
    }

    var expandedGeneration by remember(generationList) {
        mutableStateOf<Int?>(generationList.lastOrNull()?.generation)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SportsDarkBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Slim Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(36.dp).testTag("family_page_back_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "FAMILY LEGACY",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary,
                letterSpacing = 1.sp
            )
        }

        // 2. Family Tree Vertical List (Gen 1 on top, connected by thin vertical lines)
        Card(
            colors = CardDefaults.cardColors(containerColor = SportsCardBg),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "FAMILY LINEAGE & GENERATIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PitchGreen,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (generationList.isEmpty()) {
                    Text(
                        text = "No family records yet.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                } else {
                    generationList.forEachIndexed { index, gen ->
                        val isSelected = expandedGeneration == gen.generation

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Surface(
                                onClick = {
                                    expandedGeneration = if (isSelected) null else gen.generation
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) DarkSlate else SportsCardBg,
                                border = BorderStroke(1.dp, if (isSelected) PitchGreen else BorderColor.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(
                                                    if (isSelected) PitchGreen else DarkSlate,
                                                    CircleShape
                                                )
                                                .border(1.dp, if (isSelected) PitchGreen else BorderColor, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "G${gen.generation}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.Black else TextPrimary
                                            )
                                        }

                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = gen.name,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                                if (!gen.isRetired) {
                                                    Surface(
                                                        color = PitchGreen.copy(alpha = 0.2f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "ACTIVE",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = PitchGreen,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = if (gen.isRetired) "Generation ${gen.generation} • Retired Legend" else "Generation ${gen.generation} • Current Player",
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Expand",
                                        tint = if (isSelected) PitchGreen else TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            if (index < generationList.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 27.dp)
                                        .width(2.dp)
                                        .height(18.dp)
                                        .background(BorderColor.copy(alpha = 0.7f))
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Expanded Panel for Selected Generation
        val selectedGenInfo = generationList.find { it.generation == expandedGeneration }
        AnimatedVisibility(
            visible = selectedGenInfo != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            selectedGenInfo?.let { gen ->
                val rawRecords = allSeasonRecords.filter { it.generation == gen.generation }.sortedBy { it.seasonNumber }
                val displayRecords = remember(rawRecords, activePlayer, gameState, gen) {
                    val list = rawRecords.toMutableList()
                    val p = activePlayer
                    val gs = gameState
                    if (!gen.isRetired && p != null && gs != null && !p.isRetired && p.generation == gen.generation) {
                        if (p.careerPhase == com.example.data.PHASE_SENIOR) {
                            val currentSeasonNum = gs.currentSeason
                            if (list.none { it.seasonNumber == currentSeasonNum }) {
                                val clubName = clubsMap[p.currentClubId]?.name ?: "Unknown"
                                list.add(
                                    com.example.data.PlayerSeasonRecordEntity(
                                        id = -1,
                                        playerName = p.name,
                                        generation = p.generation,
                                        seasonNumber = currentSeasonNum,
                                        clubName = clubName,
                                        matchesPlayed = p.seasonGamesPlayed,
                                        goals = p.seasonGoals,
                                        assists = p.seasonAssists,
                                        ovrAtSeasonEnd = p.ovr,
                                        playerAge = p.age
                                    )
                                )
                            }
                        }
                    }
                    list.sortedBy { it.seasonNumber }
                }

                val genTrophies = remember(allTrophies, gen) {
                    allTrophies.filter { it.generation == gen.generation }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = SportsCardBg),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, PitchGreen.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "GEN ${gen.generation} • ${gen.name.uppercase()}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PitchGreen
                                )
                                Text(
                                    text = if (gen.isRetired) "Retired Legend" else "Active Career",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Divider(color = BorderColor)

                        // a. BIOGRAPHY
                        if (gen.isRetired) {
                            val biographyText = remember(gen, displayRecords, genTrophies, activePlayer) {
                                gen.legacy?.retirementDescription?.takeIf { it.isNotBlank() }
                                    ?: generateBiography(gen.generation, gen.name, gen.isRetired, gen.player ?: activePlayer, displayRecords, genTrophies)
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkSlate, RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "CAREER BIOGRAPHY",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TrophyGold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = biographyText,
                                    fontSize = 12.sp,
                                    color = TextPrimary,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        // b. SEASON-BY-SEASON TABLE
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "SEASON-BY-SEASON STATS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                letterSpacing = 0.5.sp
                            )

                            if (displayRecords.isEmpty()) {
                                Text(
                                    text = "No professional seasons recorded yet.",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = DarkSlate),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(SportsCardBg, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Season", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.weight(1.2f))
                                            Text("Club", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.weight(1.8f))
                                            Text("MP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                                            Text("G", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                                            Text("A", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Box(
                                            modifier = Modifier
                                                .heightIn(max = 200.dp)
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                displayRecords.forEach { rec ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 8.dp, vertical = 5.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = formatSeasonYear(rec.seasonNumber),
                                                            fontSize = 11.sp,
                                                            color = TextPrimary,
                                                            modifier = Modifier.weight(1.2f)
                                                        )
                                                        Text(
                                                            text = rec.clubName,
                                                            fontSize = 11.sp,
                                                            color = TextPrimary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1.8f)
                                                        )
                                                        Text(
                                                            text = "${rec.matchesPlayed}",
                                                            fontSize = 11.sp,
                                                            color = TextPrimary,
                                                            textAlign = TextAlign.Center,
                                                            modifier = Modifier.weight(0.7f)
                                                        )
                                                        Text(
                                                            text = "${rec.goals}",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = PitchGreen,
                                                            textAlign = TextAlign.Center,
                                                            modifier = Modifier.weight(0.7f)
                                                        )
                                                        Text(
                                                            text = "${rec.assists}",
                                                            fontSize = 11.sp,
                                                            color = TextPrimary,
                                                            textAlign = TextAlign.Center,
                                                            modifier = Modifier.weight(0.7f)
                                                        )
                                                    }
                                                    Divider(color = BorderColor.copy(alpha = 0.3f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // c. OVR GROWTH GRAPH
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "OVR PROGRESSION",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                letterSpacing = 0.5.sp
                            )

                            if (displayRecords.isEmpty()) {
                                Text(
                                    text = "No rating progression data available.",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            } else {
                                OvrProgressionCanvas(
                                    displayRecords = displayRecords,
                                    activePlayer = activePlayer,
                                    gameState = gameState
                                )
                            }
                        }

                        // d. TROPHIES WON
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "TROPHIES & HONORS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                letterSpacing = 0.5.sp
                            )

                            if (genTrophies.isEmpty()) {
                                Text(
                                    text = "No trophies won yet.",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            } else {
                                val groupedTrophies = remember(genTrophies) {
                                    genTrophies.groupBy { it.competitionName }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    groupedTrophies.forEach { (compName, trophies) ->
                                        val count = trophies.size
                                        val isMajor = trophies.any { it.isMajor }
                                        val isPots = compName.contains("Player of the Season", ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isPots) Color(0xFF2A2100) else DarkSlate)
                                                .border(1.dp, if (isPots) TrophyGold else if (isMajor) TrophyGold else BorderColor, RoundedCornerShape(8.dp))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                CompetitionTrophyIcon(
                                                    title = compName,
                                                    isUnlocked = true,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = "$compName${if (count > 1) " ×$count" else ""}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isPots || isMajor) TrophyGold else TextPrimary
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
}

private fun generateBiography(
    generation: Int,
    playerName: String,
    isRetired: Boolean,
    activePlayer: PlayerEntity?,
    records: List<com.example.data.PlayerSeasonRecordEntity>,
    trophies: List<TrophyEntity>
): String {
    val rand = kotlin.random.Random(generation)

    val backgroundStory = activePlayer?.takeIf { it.generation == generation }?.backgroundStory ?: "Street Cages"
    val startLine = when {
        backgroundStory.contains("School", ignoreCase = true) ->
            "$playerName was raised on structured school-team football before stepping onto the professional stage."
        backgroundStory.contains("Family", ignoreCase = true) ->
            "$playerName developed in a prestigious family club environment, carrying natural expectations from day one."
        else ->
            "$playerName forged their game in high-stakes street cages, developing raw flair and fearless instinct."
    }

    val bestSeasonLine = if (records.isNotEmpty()) {
        val bestRecord = records.maxByOrNull { rec ->
            val trophyBonus = if (trophies.any { it.seasonYear == rec.seasonNumber }) 15 else 0
            rec.goals * 2 + rec.assists * 1 + trophyBonus
        } ?: records.first()

        val seasonYearStr = formatSeasonYear(bestRecord.seasonNumber)
        val seasonTrophies = trophies.filter { it.seasonYear == bestRecord.seasonNumber }
        val potsAward = seasonTrophies.find { it.competitionName.contains("Player of the Season", ignoreCase = true) }
        val otherTrophy = seasonTrophies.find { !it.competitionName.contains("Player of the Season", ignoreCase = true) }

        val trophyText = when {
            potsAward != null && otherTrophy != null -> " crowning a magnificent double with the Player of the Season award and victory in the ${otherTrophy.competitionName}"
            potsAward != null -> " earning the prestigious Player of the Season award"
            otherTrophy != null -> " with a triumph in the ${otherTrophy.competitionName}"
            else -> ""
        }

        "Their standout campaign arrived in $seasonYearStr with ${bestRecord.clubName}, delivering ${bestRecord.goals} goals and ${bestRecord.assists} assists in ${bestRecord.matchesPlayed} matches$trophyText."
    } else {
        "Currently laying the foundations of a promising career through grassroots and academy ranks."
    }

    val traitPool1 = listOf(
        "Renowned for surgical composure under pressure and exceptional big-match awareness.",
        "A fan favorite revered for tireless work rate, explosive pace, and relentless drive.",
        "Celebrated for creative vision and unpredictable skill that mesmerized opposition defenders.",
        "Feared by opposition backlines for sharp tactical instinct and lethal finishing ability."
    )
    val traitPool2 = listOf(
        "Maintained a fierce rivalry with domestic heavyweights throughout a memorable career.",
        "Overcame early career setbacks to establish an indelible mark on the family lineage.",
        "Commanded immense respect in the dressing room as a vocal leader and generational talent.",
        "Consistently produced clutch moments in high-pressure cup matches and title deciders."
    )

    val trait1 = traitPool1[rand.nextInt(traitPool1.size)]
    val trait2 = traitPool2[rand.nextInt(traitPool2.size)]

    return "$startLine $bestSeasonLine $trait1 $trait2"
}

@Composable
private fun OvrProgressionCanvas(
    displayRecords: List<com.example.data.PlayerSeasonRecordEntity>,
    activePlayer: PlayerEntity? = null,
    gameState: GameStateEntity? = null
) {
    val textMeasurer = rememberTextMeasurer()

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSlate),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OVERALL RATING HISTORY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Y: OVR Rating • X: Player Age",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = PitchGreen
                )
            }

            val ovrList = remember(displayRecords) { displayRecords.map { it.ovrAtSeasonEnd } }
            val rawMin = ovrList.minOrNull() ?: 60
            val rawMax = ovrList.maxOrNull() ?: 80

            val diff = (rawMax - rawMin).coerceAtLeast(4)
            val step = when {
                diff <= 6 -> 2
                diff <= 15 -> 5
                diff <= 30 -> 10
                else -> 15
            }

            val minOvr = (((rawMin - 2) / step) * step).coerceAtLeast(30)
            val maxOvr = ((((rawMax + 3) / step) + 1) * step).coerceAtMost(99)
            val gridLevels = (((maxOvr - minOvr) / step) + 1).coerceIn(3, 8)

            val yAxisTextStyle = TextStyle(
                fontSize = 9.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )
            val pointTextStyle = TextStyle(
                fontSize = 10.sp,
                color = PitchGreen,
                fontWeight = FontWeight.Bold
            )
            val xAxisTextStyle = TextStyle(
                fontSize = 9.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clipToBounds()
            ) {
                val width = size.width
                val height = size.height

                val paddingLeft = 46.dp.toPx()
                val paddingRight = 20.dp.toPx()
                val paddingTop = 28.dp.toPx()
                val paddingBottom = 32.dp.toPx()

                val chartWidth = width - paddingLeft - paddingRight
                val chartHeight = height - paddingTop - paddingBottom

                // Y-axis grid lines and labels
                for (i in 0 until gridLevels) {
                    val ratio = i.toFloat() / (gridLevels - 1)
                    val y = paddingTop + chartHeight * (1f - ratio)
                    val ovrVal = minOvr + (i * step)

                    drawLine(
                        color = BorderColor.copy(alpha = 0.4f),
                        start = Offset(paddingLeft, y),
                        end = Offset(width - paddingRight, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )

                    val textLayout = textMeasurer.measure(
                        text = "$ovrVal",
                        style = yAxisTextStyle
                    )
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(
                            x = paddingLeft - textLayout.size.width - 6.dp.toPx(),
                            y = y - textLayout.size.height / 2f
                        )
                    )
                }

                // X-axis baseline
                drawLine(
                    color = BorderColor,
                    start = Offset(paddingLeft, paddingTop + chartHeight),
                    end = Offset(width - paddingRight, paddingTop + chartHeight),
                    strokeWidth = 1.5.dp.toPx()
                )

                val points = displayRecords.mapIndexed { index, rec ->
                    val x = if (displayRecords.size == 1) {
                        paddingLeft + chartWidth / 2f
                    } else {
                        paddingLeft + (index.toFloat() / (displayRecords.size - 1)) * chartWidth
                    }
                    val yRatio = (rec.ovrAtSeasonEnd - minOvr).toFloat() / (maxOvr - minOvr).coerceAtLeast(1)
                    val y = paddingTop + chartHeight * (1f - yRatio)
                    Offset(x, y)
                }

                // Line & Fill
                if (points.size > 1) {
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }
                    val fillPath = Path().apply {
                        addPath(path)
                        lineTo(points.last().x, paddingTop + chartHeight)
                        lineTo(points.first().x, paddingTop + chartHeight)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(PitchGreen.copy(alpha = 0.35f), Color.Transparent),
                            startY = paddingTop,
                            endY = paddingTop + chartHeight
                        )
                    )
                    drawPath(
                        path = path,
                        color = PitchGreen,
                        style = Stroke(width = 3.dp.toPx())
                    )
                } else if (points.size == 1) {
                    drawLine(
                        color = PitchGreen.copy(alpha = 0.35f),
                        start = Offset(paddingLeft, points[0].y),
                        end = Offset(width - paddingRight, points[0].y),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }

                // Points, OVR text labels, and X-axis Age labels
                val labelSkip = when {
                    displayRecords.size <= 7 -> 1
                    displayRecords.size <= 14 -> 2
                    displayRecords.size <= 22 -> 3
                    else -> 4
                }

                var lastAgeLabelRight = -1000f

                points.forEachIndexed { idx, pt ->
                    val rec = displayRecords[idx]
                    val ovr = rec.ovrAtSeasonEnd
                    val age = if (rec.playerAge > 0) rec.playerAge else {
                        if (activePlayer != null && gameState != null && rec.generation == activePlayer.generation) {
                            (activePlayer.age - (gameState.currentSeason - rec.seasonNumber)).coerceAtLeast(14)
                        } else {
                            17 + idx
                        }
                    }

                    // Point circles
                    drawCircle(color = PitchGreen, radius = 5.dp.toPx(), center = pt)
                    drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = pt)

                    // Top OVR text label
                    val isFirst = idx == 0
                    val isLast = idx == displayRecords.size - 1
                    val isPeak = ovr == ovrList.maxOrNull()
                    val showOvr = isFirst || isLast || isPeak || (idx % labelSkip == 0)

                    if (showOvr) {
                        val ovrTextLayout = textMeasurer.measure(
                            text = "$ovr",
                            style = pointTextStyle
                        )
                        val ovrX = (pt.x - ovrTextLayout.size.width / 2f).coerceIn(
                            paddingLeft,
                            (width - paddingRight - ovrTextLayout.size.width).coerceAtLeast(paddingLeft)
                        )
                        drawText(
                            textLayoutResult = ovrTextLayout,
                            topLeft = Offset(
                                x = ovrX,
                                y = pt.y - ovrTextLayout.size.height - 4.dp.toPx()
                            )
                        )
                    }

                    // Bottom Age text label (X-axis)
                    val showAge = isFirst || isLast || (idx % labelSkip == 0)
                    if (showAge) {
                        val ageStr = if (displayRecords.size <= 5) "Age $age" else "$age"
                        val ageTextLayout = textMeasurer.measure(
                            text = ageStr,
                            style = xAxisTextStyle
                        )
                        val textWidth = ageTextLayout.size.width.toFloat()
                        var ageX = pt.x - textWidth / 2f

                        ageX = ageX.coerceIn(
                            paddingLeft - 4.dp.toPx(),
                            (width - paddingRight - textWidth + 4.dp.toPx()).coerceAtLeast(paddingLeft - 4.dp.toPx())
                        )

                        if (ageX >= lastAgeLabelRight + 6.dp.toPx() || isFirst) {
                            drawText(
                                textLayoutResult = ageTextLayout,
                                topLeft = Offset(
                                    x = ageX,
                                    y = paddingTop + chartHeight + 8.dp.toPx()
                                )
                            )
                            lastAgeLabelRight = ageX + textWidth
                        }
                    }
                }
            }
        }
    }
}
