package com.example.ui

import com.example.ui.components.ClubCrestIcon

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ClubEntity
import com.example.data.FixtureEntity
import com.example.data.PlayerEntity
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.math.roundToInt
import kotlin.random.Random

enum class MatchPhase {
    PRE_MATCH, LIVE, COMPLETED
}

enum class MomentType {
    KEEPER_1V1, THROUGH_BALL, PENALTY, LATE_DEFENSIVE
}

data class BackgroundGoal(
    val isHome: Boolean,
    val minute: Int
)

data class LiveCommentary(
    val minute: Int,
    val text: String,
    val isGoal: Boolean = false,
    val isImportant: Boolean = false
)

data class KeyMoment(
    val minute: Int,
    val type: MomentType,
    val description: String,
    val choices: List<String>
)

@Composable
fun MatchScreen(viewModel: CareerViewModel) {
    val playerState by viewModel.playerFlow.collectAsStateWithLifecycle()
    val clubs by viewModel.clubsFlow.collectAsStateWithLifecycle()
    val activeFixture by viewModel.activeMatch.collectAsStateWithLifecycle()
    val playerCameOnMinute by viewModel.playerCameOnMinute.collectAsStateWithLifecycle()
    val isQuickSimCompleted by viewModel.isQuickSimCompleted.collectAsStateWithLifecycle()

    val fixture = activeFixture ?: return
    val p = playerState ?: return

    DisposableEffect(Unit) {
        onDispose {
            WhistlePlayer.release()
        }
    }

    val homeClub = remember(clubs) { clubs.find { it.id == fixture.homeClubId } } ?: return
    val awayClub = remember(clubs) { clubs.find { it.id == fixture.awayClubId } } ?: return

    val isPlayerHome = fixture.homeClubId == p.currentClubId
    val myClub = if (isPlayerHome) homeClub else awayClub
    val oppClub = if (isPlayerHome) awayClub else homeClub

    if (isQuickSimCompleted) {
        SimulatedMatchSummaryScreen(
            player = p,
            myClub = myClub,
            oppClub = oppClub,
            fixture = fixture,
            onContinue = { viewModel.dismissQuickSimSummary() }
        )
    } else {
        var matchPhase by remember { mutableStateOf(MatchPhase.PRE_MATCH) }

        when (matchPhase) {
            MatchPhase.PRE_MATCH -> {
                PreMatchScreen(
                    player = p,
                    myClub = myClub,
                    oppClub = oppClub,
                    fixture = fixture,
                    playerCameOnMinute = playerCameOnMinute,
                    onStartMatch = { matchPhase = MatchPhase.LIVE },
                    onQuickSim = { viewModel.quickSimCurrentMatch() }
                )
            }
            MatchPhase.LIVE -> {
                LiveMatchSimulation(
                    viewModel = viewModel,
                    player = p,
                    myClub = myClub,
                    oppClub = oppClub,
                    fixture = fixture,
                    playerCameOnMinute = playerCameOnMinute,
                    onMatchFinished = { goals, assists, homeFinal, awayFinal, minutesPlayed, matchRating, goalMinutes, assistMinutes ->
                        viewModel.resolvePlayedMatch(goals, assists, homeFinal, awayFinal, minutesPlayed, matchRating, goalMinutes, assistMinutes)
                    }
                )
            }
            MatchPhase.COMPLETED -> {
                // Handled automatically via state transition back to GAMEPLAY in viewmodel
            }
        }
    }
}

@Composable
fun PreMatchScreen(
    player: PlayerEntity,
    myClub: ClubEntity,
    oppClub: ClubEntity,
    fixture: FixtureEntity,
    playerCameOnMinute: Int?,
    onStartMatch: () -> Unit,
    onQuickSim: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SportsDarkBg)
            .then(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()))
            .padding(24.dp)
            .testTag("pre_match_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SportsSoccer,
            contentDescription = "Soccer Stadium",
            tint = PitchGreen,
            modifier = Modifier.size(72.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "MATCHDAY!",
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = PitchGreen,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = when (fixture.competition) {
                "LEAGUE" -> "DOMESTIC LEAGUE MATCH"
                "CHAMPIONS_LEAGUE" -> "CHAMPIONS LEAGUE"
                "EUROPA_LEAGUE" -> "EUROPA LEAGUE"
                "CONFERENCE_LEAGUE" -> "CONFERENCE LEAGUE"
                "SUPER_CUP" -> "EUROPEAN SUPER CUP"
                else -> "CUP TOURNAMENT"
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Matchup Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = SportsCardBg)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top
                ) {
                    // Home
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ClubCrestIcon(
                            clubId = myClub.id,
                            clubName = myClub.name,
                            size = 64.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = myClub.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PitchGreen,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "YOUR TEAM",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TrophyGold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Text(
                        text = "VS",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 22.dp)
                    )

                    // Away
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ClubCrestIcon(
                            clubId = oppClub.id,
                            clubName = oppClub.name,
                            size = 64.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = oppClub.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "YOUR TEAM",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Transparent,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                val isPlayerHome = fixture.homeClubId == player.currentClubId
                if (player.fanReputation >= 70 && isPlayerHome) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = BorderColor.copy(alpha = 0.5f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_crowd_boost_badge")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = "Home Crowd",
                            tint = PitchGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Home Crowd Boost Active (+5% success)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PitchGreen
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Team selection status
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSlate)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (playerCameOnMinute) {
                        0 -> Icons.Default.CheckCircle
                        null -> Icons.Default.RemoveCircleOutline
                        else -> Icons.Default.Info
                    },
                    contentDescription = "Role",
                    tint = when (playerCameOnMinute) {
                        0 -> PitchGreen
                        null -> TextSecondary
                        else -> SubLineYellow
                    },
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = when (playerCameOnMinute) {
                            0 -> "Starting XI"
                            null -> "Benched / Unused Sub"
                            else -> "Substitute Role"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = when (playerCameOnMinute) {
                            0 -> "You are on the pitch from the first minute. Make an impact!"
                            null -> "The manager left you on the bench for this match. You will not come on."
                            else -> "You are on the bench. Projected to enter around the ${playerCameOnMinute}' minute."
                        },
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Fatigue status
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSlate)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.OfflineBolt,
                            contentDescription = "Fatigue",
                            tint = if (player.fatigue > 60) MutedRed else PitchGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Fatigue Level",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "${player.fatigue}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (player.fatigue > 60) MutedRed else TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { player.fatigue / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (player.fatigue > 60) MutedRed else PitchGreen,
                    trackColor = BorderColor
                )
                if (player.fatigue > 60) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "⚠️ High fatigue reduces your success chance in key moments!",
                        fontSize = 11.sp,
                        color = MutedRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Action Buttons
        Button(
            onClick = onStartMatch,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("play_match_button"),
            colors = ButtonDefaults.buttonColors(containerColor = PitchGreen),
            shape = RoundedCornerShape(26.dp)
        ) {
            Text(
                text = if (playerCameOnMinute == null) "WATCH SIMULATION" else "PLAY KEY MOMENTS",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onQuickSim,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .border(1.dp, BorderColor, RoundedCornerShape(26.dp))
                .testTag("quick_sim_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextPrimary),
            shape = RoundedCornerShape(26.dp)
        ) {
            Text(
                text = if (playerCameOnMinute == null) "SKIP TO RESULT" else "QUICK SIM (-15% ODDS PENALTY)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LiveMatchSimulation(
    viewModel: CareerViewModel,
    player: PlayerEntity,
    myClub: ClubEntity,
    oppClub: ClubEntity,
    fixture: FixtureEntity,
    playerCameOnMinute: Int?,
    onMatchFinished: (goals: Int, assists: Int, homeFinal: Int, awayFinal: Int, minutesPlayed: Int, matchRating: Float, goalMinutes: String?, assistMinutes: String?) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val fixturesList by viewModel.fixturesFlow.collectAsStateWithLifecycle()

    // Match variables
    var currentMinute by remember { mutableStateOf(0) }
    var runningHomeScore by remember { mutableStateOf(0) }
    var runningAwayScore by remember { mutableStateOf(0) }
    var playerGoalsScored by remember { mutableStateOf(0) }
    var playerAssistsByMe by remember { mutableStateOf(0) }
    var opportunitiesMissed by remember { mutableStateOf(0) }
    var isMatchCompleted by remember { mutableStateOf(false) }
    
    val playerGoalMinutes = remember { mutableStateListOf<Int>() }
    val playerAssistMinutes = remember { mutableStateListOf<Int>() }
    
    // Half-time & Subbed off states
    var isAtHalfTime by remember { mutableStateOf(false) }
    var hasDoneHalfTime by remember { mutableStateOf(false) }
    var wasSubbedOff by remember { mutableStateOf(false) }
    var subbedOffMinute by remember { mutableStateOf<Int?>(null) }
    
    val isPlaying = playerCameOnMinute != null && (playerCameOnMinute == 0 || currentMinute >= playerCameOnMinute) && !wasSubbedOff

    val isPlayerHome = fixture.homeClubId == player.currentClubId
    val homeClub = if (isPlayerHome) myClub else oppClub
    val awayClub = if (isPlayerHome) oppClub else myClub

    // Pre-generate background match goals and key moments
    val finalBackgroundResult = remember {
        val homeWeight = fixture.homeClubId // stand-in
        val isHome = fixture.homeClubId == player.currentClubId
        
        // Simulating standard match background goals
        val homeWeightRep = if (isHome) myClub.reputationPoints else oppClub.reputationPoints
        val awayWeightRep = if (isHome) oppClub.reputationPoints else myClub.reputationPoints
        val homeBase = (Random.nextInt(0, 3) + (homeWeightRep - awayWeightRep) / 30f).coerceIn(0f, 4f).toInt()
        val awayBase = (Random.nextInt(0, 3) + (awayWeightRep - homeWeightRep) / 30f).coerceIn(0f, 4f).toInt()
        
        Pair(homeBase, awayBase)
    }

    val backgroundGoalsList = remember {
        val list = mutableListOf<BackgroundGoal>()
        for (i in 0 until finalBackgroundResult.first) {
            list.add(BackgroundGoal(isHome = true, minute = Random.nextInt(1, 90)))
        }
        for (i in 0 until finalBackgroundResult.second) {
            list.add(BackgroundGoal(isHome = false, minute = Random.nextInt(1, 90)))
        }
        list.sortBy { it.minute }
        list
    }

    val generatedKeyMoments: MutableList<KeyMoment> = remember {
        if (playerCameOnMinute == null) return@remember mutableListOf()
        val repBonus = when (myClub.reputation) {
            "ELITE" -> 2
            "BIG" -> 1
            "MID" -> 0
            "SMALL" -> -1
            else -> 0
        }
        var count = (3 + player.form / 3 + repBonus).coerceIn(2, 5)
        if (player.rivalRelationship >= 85) {
            count += 1
        }
        count = count.coerceAtMost(5)

        val startMinForScale = playerCameOnMinute ?: 0
        count = (count * (90 - startMinForScale) / 90f).roundToInt().coerceAtLeast(1)

        val moments = mutableListOf<KeyMoment>()
        var penaltiesGenerated = 0

        // Generate spaced intervals
        val startMin = if (playerCameOnMinute == 0 || playerCameOnMinute == null) 1 else playerCameOnMinute
        val availableLength = 90 - startMin
        val interval = availableLength / count

        for (i in 0 until count) {
            val minRangeStart = startMin + i * interval
            val minRangeEnd = minRangeStart + interval - 2
            val min = Random.nextInt(minRangeStart, minRangeEnd.coerceAtLeast(minRangeStart + 1)).coerceIn(1, 89)

            // Random Moment Type
            val isLateAndWinning = min > 75 && (if (fixture.homeClubId == player.currentClubId) runningHomeScore > runningAwayScore else runningAwayScore > runningHomeScore)
            val type = if (isLateAndWinning && Random.nextFloat() < 0.6f) {
                MomentType.LATE_DEFENSIVE
            } else {
                if (penaltiesGenerated >= 2) {
                    val roll = Random.nextFloat()
                    if (roll < 0.435f) MomentType.KEEPER_1V1 else MomentType.THROUGH_BALL
                } else {
                    val roll = Random.nextFloat()
                    when {
                        roll < 0.40f -> MomentType.KEEPER_1V1
                        roll < 0.92f -> MomentType.THROUGH_BALL
                        else -> {
                            penaltiesGenerated++
                            MomentType.PENALTY
                        }
                    }
                }
            }

            val desc = when (type) {
                MomentType.KEEPER_1V1 -> "You break free from the defensive line! It's just you and the goalkeeper approaching at high speed."
                MomentType.THROUGH_BALL -> "A teammate whips a beautiful through-ball into the penalty box. A defender is pressing closely behind you."
                MomentType.PENALTY -> "PENALTY KICK! You step up to take the penalty under high pressure from the hostile crowd."
                MomentType.LATE_DEFENSIVE -> "The opponents are launching a desperate late offensive. You need to drop back and help lock down the victory."
            }

            val choices = when (type) {
                MomentType.KEEPER_1V1 -> listOf("Chip the keeper", "Shoot low far post", "Round the keeper", "Square to teammate")
                MomentType.THROUGH_BALL -> listOf("First-time shot", "Take a touch and shoot", "Shield and lay off")
                MomentType.PENALTY -> listOf("Top Left Corner", "Top Right Corner", "Bottom Left Corner", "Bottom Right Corner")
                MomentType.LATE_DEFENSIVE -> listOf("Track your marker", "Push forward for the counter")
            }

            moments.add(KeyMoment(min, type, desc, choices))
        }
        moments.sortBy { it.minute }
        moments
    }

    var activeMomentIndex by remember { mutableStateOf(-1) }
    val isMomentActive = activeMomentIndex in generatedKeyMoments.indices

    // Commentary logs
    val commentaryLogs = remember { mutableStateListOf<LiveCommentary>() }
    val lazyListState = rememberLazyListState()

    var activeChoice by remember { mutableStateOf<String?>(null) }
    var choiceResolutionResult by remember { mutableStateOf<String?>(null) }
    var isChoiceSuccessful by remember { mutableStateOf(false) }

    // Penalty timing bar variables
    var penaltyIndicatorProgress by remember { mutableStateOf(0.0f) }
    var penaltyMovingRight by remember { mutableStateOf(true) }

    // LaunchedEffect for penalty moving progress
    LaunchedEffect(isMomentActive, activeMomentIndex, activeChoice) {
        val currentMoment = generatedKeyMoments.getOrNull(activeMomentIndex)
        if (currentMoment?.type == MomentType.PENALTY && activeChoice == null) {
            while (true) {
                delay(20)
                if (penaltyMovingRight) {
                    penaltyIndicatorProgress += 0.05f
                    if (penaltyIndicatorProgress >= 1.0f) {
                        penaltyIndicatorProgress = 1.0f
                        penaltyMovingRight = false
                    }
                } else {
                    penaltyIndicatorProgress -= 0.05f
                    if (penaltyIndicatorProgress <= 0.0f) {
                        penaltyIndicatorProgress = 0.0f
                        penaltyMovingRight = true
                    }
                }
            }
        }
    }

    // Scroll commentary to bottom when modified
    LaunchedEffect(commentaryLogs.size) {
        if (commentaryLogs.isNotEmpty()) {
            lazyListState.animateScrollToItem(commentaryLogs.size - 1)
        }
    }

    // Main Match Loop
    LaunchedEffect(Unit) {
        commentaryLogs.add(LiveCommentary(0, "Game starts! Kickoff in the stadium.", isImportant = true))
        viewModel.playWhistleSound("kickoff")
        delay(500)

        while (currentMinute < 90) {
            if (activeMomentIndex in generatedKeyMoments.indices) {
                // Wait until the moment is fully resolved by the user
                delay(200)
                continue
            }

            if (isAtHalfTime) {
                delay(200)
                continue
            }

            if (currentMinute == 45 && !hasDoneHalfTime) {
                isAtHalfTime = true
                commentaryLogs.add(LiveCommentary(45, "⏸️ HALF TIME! The referee blows the whistle for the break.", isImportant = true))
                viewModel.playWhistleSound("halftime")
                delay(200)
                continue
            }

            currentMinute++

            // Sub-in narrative trigger
            if (playerCameOnMinute != null && playerCameOnMinute > 0 && currentMinute == playerCameOnMinute) {
                commentaryLogs.add(
                    LiveCommentary(
                        currentMinute,
                        "🔄 SUBSTITUTION: You are coming on to replace the tired striker!",
                        isImportant = true
                    )
                )
                delay(800)
            }

            // Check background goals
            val goalsAtThisMinute = backgroundGoalsList.filter { it.minute == currentMinute }
            for (bgGoal in goalsAtThisMinute) {
                if (bgGoal.isHome) {
                    runningHomeScore++
                    commentaryLogs.add(
                        LiveCommentary(
                            currentMinute,
                            "⚽ GOAL for ${homeClub.name}! An impressive buildup play leads to a score.",
                            isGoal = true
                        )
                    )
                } else {
                    runningAwayScore++
                    commentaryLogs.add(
                        LiveCommentary(
                            currentMinute,
                            "⚽ GOAL for ${awayClub.name}! They slot it past the goalkeeper with ease.",
                            isGoal = true
                        )
                    )
                }
                delay(800)
            }

            // Check if there is a Key Moment at this minute
            val momentIndex = generatedKeyMoments.indexOfFirst { it.minute == currentMinute }
            if (momentIndex != -1) {
                if (wasSubbedOff) {
                    commentaryLogs.add(LiveCommentary(currentMinute, "⚡ Teammate takes the chance while you watch from the bench."))
                    delay(500)
                } else {
                    val originalMoment = generatedKeyMoments[momentIndex]
                    val min = originalMoment.minute
                    val isLateAndWinning = min > 75 && (if (fixture.homeClubId == player.currentClubId) runningHomeScore > runningAwayScore else runningAwayScore > runningHomeScore)
                    var penaltiesPreceding = 0
                    for (idx in 0 until momentIndex) {
                        if (generatedKeyMoments[idx].type == MomentType.PENALTY) {
                            penaltiesPreceding++
                        }
                    }
                    val type = if (isLateAndWinning && Random.nextFloat() < 0.6f) {
                        MomentType.LATE_DEFENSIVE
                    } else {
                        if (penaltiesPreceding >= 2) {
                            val roll = Random.nextFloat()
                            if (roll < 0.435f) MomentType.KEEPER_1V1 else MomentType.THROUGH_BALL
                        } else {
                            val roll = Random.nextFloat()
                            when {
                                roll < 0.40f -> MomentType.KEEPER_1V1
                                roll < 0.92f -> MomentType.THROUGH_BALL
                                else -> MomentType.PENALTY
                            }
                        }
                    }

                    val desc = when (type) {
                        MomentType.KEEPER_1V1 -> "You break free from the defensive line! It's just you and the goalkeeper approaching at high speed."
                        MomentType.THROUGH_BALL -> "A teammate whips a beautiful through-ball into the penalty box. A defender is pressing closely behind you."
                        MomentType.PENALTY -> "PENALTY KICK! You step up to take the penalty under high pressure from the hostile crowd."
                        MomentType.LATE_DEFENSIVE -> "The opponents are launching a late offensive. You need to drop back and help lock down the victory."
                    }

                    val choices = when (type) {
                        MomentType.KEEPER_1V1 -> listOf("Chip the keeper", "Shoot low far post", "Round the keeper", "Square to teammate")
                        MomentType.THROUGH_BALL -> listOf("First-time shot", "Take a touch and shoot", "Shield and lay off")
                        MomentType.PENALTY -> listOf("Top Left Corner", "Top Right Corner", "Bottom Left Corner", "Bottom Right Corner")
                        MomentType.LATE_DEFENSIVE -> listOf("Track your marker", "Push forward for the counter")
                    }

                    generatedKeyMoments[momentIndex] = KeyMoment(min, type, desc, choices)

                    activeMomentIndex = momentIndex
                    commentaryLogs.add(
                        LiveCommentary(
                            currentMinute,
                            "🔥 CRITICAL KEY MOMENT DEVELOPING...",
                            isImportant = true
                        )
                    )
                    delay(500)
                    continue
                }
            }

            // Standard ticking commentary (every 10 minutes or so)
            if (currentMinute % 12 == 0 && currentMinute < 90) {
                val genericComments = listOf(
                    "Possession is fiercely contested in midfield.",
                    "Both tactical setups are neutralising each other.",
                    "The atmosphere is tense as supporters cheer on.",
                    "Tactical reshuffling happening on the benches."
                )
                commentaryLogs.add(LiveCommentary(currentMinute, genericComments.random()))
            }

            delay(100)
        }

        commentaryLogs.add(LiveCommentary(90, "🏁 FULL TIME! The referee blows the whistle.", isImportant = true))
        viewModel.playWhistleSound("fulltime")

        val isKnockoutFinal = fixture.competition in listOf("CHAMPIONS_LEAGUE", "EUROPA_LEAGUE", "CONFERENCE_LEAGUE") && fixture.round == 2
        val isSuperCup = fixture.competition == "SUPER_CUP"
        if ((isKnockoutFinal || isSuperCup) && runningHomeScore == runningAwayScore) {
            delay(1500)
            viewModel.playWhistleSound("extra")
            delay(1500)
            viewModel.playWhistleSound("pens")
        }

        delay(1000)
        isMatchCompleted = true
    }

    // Function to resolve Key Moment choice
    fun resolveKeyMomentChoice(choice: String?) {
        if (activeMomentIndex !in generatedKeyMoments.indices) return
        val moment = generatedKeyMoments[activeMomentIndex]
        activeChoice = choice ?: "Timer Expired"

        if (activeChoice == "Timer Expired" && moment.type != MomentType.PENALTY) {
            isChoiceSuccessful = false
            opportunitiesMissed++
            choiceResolutionResult = when (moment.type) {
                MomentType.KEEPER_1V1, MomentType.THROUGH_BALL -> {
                    "You hesitate a split-second too long and a recovering defender pokes the ball away"
                }
                MomentType.LATE_DEFENSIVE -> {
                    "The cross is overhit and rolls out for a goal kick"
                }
                else -> ""
            }
            return
        }

        // Compute success rate based on attributes and variables
        val myTeamRepFactor = when (myClub.reputation) {
            "ELITE" -> 1.20f
            "BIG" -> 1.10f
            "MID" -> 1.00f
            "SMALL" -> 0.80f
            else -> 1.00f
        }
        val oppRepFactor = when (oppClub.reputation) {
            "ELITE" -> 0.70f
            "BIG" -> 0.80f
            "MID" -> 1.00f
            "SMALL" -> 1.20f
            else -> 1.00f
        }
        val formFactor = 1.0f + (player.form * 0.05f)
        val fatigueFactor = 1.0f - (player.fatigue * 0.0025f)

        val relevantStat = when (moment.type) {
            MomentType.KEEPER_1V1 -> player.finishing
            MomentType.THROUGH_BALL -> {
                if (choice == "First-time shot") player.finishing
                else if (choice == "Take a touch and shoot") player.technique
                else player.passing
            }
            MomentType.PENALTY -> player.finishing
            MomentType.LATE_DEFENSIVE -> {
                if (choice == "Track your marker") player.physical
                else player.pace
            }
        }

        // Choice tactical bonus or penalty
        var tacticalSwing = when (moment.type) {
            MomentType.KEEPER_1V1 -> {
                when (choice) {
                    "Chip the keeper" -> 0.15f
                    "Shoot low far post" -> 0.10f
                    "Round the keeper" -> 0.05f
                    "Square to teammate" -> 0.20f // high success, counts as assist
                    else -> -0.15f
                }
            }
            MomentType.THROUGH_BALL -> {
                when (choice) {
                    "First-time shot" -> 0.05f
                    "Take a touch and shoot" -> 0.10f
                    "Shield and lay off" -> 0.20f // Assist chance
                    else -> -0.15f
                }
            }
            MomentType.PENALTY -> {
                // Moving timing bar calculation
                val inGreenZone = penaltyIndicatorProgress in 0.40f..0.60f
                if (inGreenZone) 0.25f else -0.20f
            }
            MomentType.LATE_DEFENSIVE -> {
                when (choice) {
                    "Track your marker" -> 0.20f
                    "Push forward for the counter" -> -0.10f // risky counter
                    else -> -0.20f
                }
            }
        }

        if (choice == "Timer Expired") {
            tacticalSwing = -0.40f // massive penalty
        }

        val moraleFactor = (player.morale - 50) / 50.0f * 0.08f
        val fanBoost = if (player.fanReputation >= 70 && isPlayerHome) 0.05f else 0.0f

        var successChance = (relevantStat / 100.0f) * myTeamRepFactor * oppRepFactor * formFactor * fatigueFactor + moraleFactor + fanBoost + tacticalSwing
        successChance = successChance.coerceIn(0.05f, 0.95f)

        val success = Random.nextFloat() < successChance
        isChoiceSuccessful = success

        // Outcome mapping
        if (success) {
            when (moment.type) {
                MomentType.KEEPER_1V1 -> {
                    if (choice == "Square to teammate") {
                        playerAssistsByMe++
                        playerAssistMinutes.add(currentMinute)
                        if (fixture.homeClubId == player.currentClubId) runningHomeScore++ else runningAwayScore++
                        choiceResolutionResult = "Beautiful team play! You squared the ball across the face of the goal, and your strike partner tapped it in! ASSIST UNLOCKED."
                    } else {
                        playerGoalsScored++
                        playerGoalMinutes.add(currentMinute)
                        if (fixture.homeClubId == player.currentClubId) runningHomeScore++ else runningAwayScore++
                        choiceResolutionResult = when (choice) {
                            "Chip the keeper" -> "GOAL! You delicately chip the ball over the rushing goalkeeper! Pure audacity and finesse!"
                            "Shoot low far post" -> "GOAL! You clinically side-foot the ball past the keeper into the bottom far corner. Picture perfect!"
                            "Round the keeper" -> "GOAL! You round the goalkeeper with a swift body feint and roll the ball into the empty net!"
                            else -> "GOAL! A magnificent finish past the keeper into the back of the net!"
                        }
                    }
                }
                MomentType.THROUGH_BALL -> {
                    if (choice == "Shield and lay off") {
                        playerAssistsByMe++
                        playerAssistMinutes.add(currentMinute)
                        if (fixture.homeClubId == player.currentClubId) runningHomeScore++ else runningAwayScore++
                        choiceResolutionResult = "Masterful hold-up play! You shield off the pressing defender and lay a soft touch to your midfielder, who smashes it in! ASSIST."
                    } else {
                        playerGoalsScored++
                        playerGoalMinutes.add(currentMinute)
                        if (fixture.homeClubId == player.currentClubId) runningHomeScore++ else runningAwayScore++
                        choiceResolutionResult = when (choice) {
                            "First-time shot" -> "GOAL! You hit it first-time on the volley! It rockets into the top corner, leaving the keeper helpless!"
                            "Take a touch and shoot" -> "GOAL! You compose yourself with a crisp first touch and drill a low drive past the diving keeper!"
                            else -> "GOAL! You latch onto the through ball and finish with authority!"
                        }
                    }
                }
                MomentType.PENALTY -> {
                    playerGoalsScored++
                    playerGoalMinutes.add(currentMinute)
                    if (fixture.homeClubId == player.currentClubId) runningHomeScore++ else runningAwayScore++
                    choiceResolutionResult = when (choice) {
                        "Top Left Corner" -> "GOAL! You drive the penalty straight into the top left stanchion! Unstoppable precision!"
                        "Top Right Corner" -> "GOAL! You blast the penalty into the top right corner off the underside of the crossbar!"
                        "Bottom Left Corner" -> "GOAL! You place it precisely into the bottom left corner, sending the keeper the wrong way!"
                        "Bottom Right Corner" -> "GOAL! A hard, low strike into the bottom right corner that sneaks past the keeper's fingertips!"
                        else -> "GOAL! You hit the spot-kick with absolute power and accuracy!"
                    }
                }
                MomentType.LATE_DEFENSIVE -> {
                    if (choice == "Track your marker") {
                        choiceResolutionResult = "Defensive masterclass! You track the opponent's winger, intercept their cross, and clear the danger out of the box."
                    } else {
                        // Counters successfully! Adds an extra counter goal
                        playerGoalsScored++
                        playerGoalMinutes.add(currentMinute)
                        if (fixture.homeClubId == player.currentClubId) runningHomeScore++ else runningAwayScore++
                        choiceResolutionResult = "STUNNING COUNTER-ATTACK! You intercepted a loose clearance, sprinted the length of the pitch, and slotted it home. Game over!"
                    }
                }
            }
            // If player scored, and rivalRelationship is high, they occasionally assist!
            val didScore = choice != "Square to teammate" && choice != "Shield and lay off" && moment.type != MomentType.LATE_DEFENSIVE && moment.type != MomentType.PENALTY
            if (didScore && player.rivalRelationship > 70 && Random.nextFloat() < 0.30f) {
                choiceResolutionResult += " (A wonderful, selfless assist by your rival striker, ${myClub.rivalStrikerName}!)"
            }
        } else {
            // Failed
            opportunitiesMissed++
            choiceResolutionResult = when (moment.type) {
                MomentType.KEEPER_1V1 -> when (choice) {
                    "Chip the keeper" -> "MISSED! You tried to chip the keeper, but they read your intention and plucked the ball out of the air."
                    "Shoot low far post" -> "MISSED! Your low strike dragged agonizingly wide of the far post."
                    "Round the keeper" -> "MISSED! The keeper stuck out a hand and smothered the ball right off your boots."
                    "Square to teammate" -> "INTERCEPTED! The recovering defender read your pass across goal and intercepted it."
                    else -> "MISSED! The goalkeeper smothered your shot at your feet."
                }
                MomentType.THROUGH_BALL -> when (choice) {
                    "First-time shot" -> "OVER THE BAR! You attempted a first-time volley but leaned back and sent it soaring over the crossbar."
                    "Take a touch and shoot" -> "BLOCKED! Taking a touch gave the defender time to slide in and block your shot."
                    "Shield and lay off" -> "DISPOSSESSED! The physical defender muscled you off the ball before you could lay it off."
                    else -> "OUT OF PLAY! Under heavy pressure, your shot drifted wide."
                }
                MomentType.PENALTY -> when (choice) {
                    "Top Left Corner", "Top Right Corner" -> "OFF THE BAR! You went for the top corner, but the ball rattled off the crossbar!"
                    "Bottom Left Corner", "Bottom Right Corner" -> "SAVED! The goalkeeper guessed right and made a fingertip save to push it around the post!"
                    else -> "SAVED! The goalkeeper guesses right and makes a save!"
                }
                MomentType.LATE_DEFENSIVE -> {
                    if (choice == "Track your marker") {
                        "CONCEDED! You lost your marker for a split second, allowing them to whip in a cross that is headed in by their striker."
                    } else {
                        // Counter failed: Opponent scores!
                        if (fixture.homeClubId == player.currentClubId) runningAwayScore++ else runningHomeScore++
                        "PUNISHED! You pushed too far forward. The opponent intercepted the pass, exploited the empty space, and scored an equalizer."
                    }
                }
            }

            // Apply conceding background goals on late defensive failures
            if (moment.type == MomentType.LATE_DEFENSIVE && choice == "Track your marker") {
                if (fixture.homeClubId == player.currentClubId) runningAwayScore++ else runningHomeScore++
            }
        }

        commentaryLogs.add(
            LiveCommentary(
                moment.minute,
                choiceResolutionResult ?: "Possession is lost in an attempt.",
                isGoal = success && choice != "Square to teammate" && choice != "Shield and lay off" && moment.type != MomentType.LATE_DEFENSIVE,
                isImportant = true
            )
        )
    }

    fun dismissKeyMoment() {
        activeMomentIndex = -1
        activeChoice = null
        choiceResolutionResult = null
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(SportsDarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(16.dp)
        ) {
            // MATCH HEADER / LIVE SCOREBOARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SportsCardBg)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "LIVE MATCH",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PitchGreen,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = myClub.name.take(3).uppercase(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = PitchGreen
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (fixture.homeClubId == player.currentClubId) "$runningHomeScore - $runningAwayScore" else "$runningAwayScore - $runningHomeScore",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = TrophyGold
                            )

                            val leg1Fixture = remember(fixturesList, fixture) {
                                if (fixture.competition in listOf("CHAMPIONS_LEAGUE", "EUROPA_LEAGUE", "CONFERENCE_LEAGUE") && fixture.round == 2) {
                                    fixturesList.find {
                                        it.competition == fixture.competition &&
                                        it.round == 1 &&
                                        ((it.homeClubId == fixture.homeClubId && it.awayClubId == fixture.awayClubId) ||
                                         (it.homeClubId == fixture.awayClubId && it.awayClubId == fixture.homeClubId))
                                    }
                                } else null
                            }

                            if (leg1Fixture != null) {
                                val leg1Home = leg1Fixture.homeScore ?: 0
                                val leg1Away = leg1Fixture.awayScore ?: 0
                                val homeAgg = if (fixture.homeClubId == leg1Fixture.homeClubId) leg1Home + runningHomeScore else leg1Away + runningHomeScore
                                val awayAgg = if (fixture.awayClubId == leg1Fixture.awayClubId) leg1Away + runningAwayScore else leg1Home + runningAwayScore
                                val myAgg = if (fixture.homeClubId == player.currentClubId) homeAgg else awayAgg
                                val oppAgg = if (fixture.homeClubId == player.currentClubId) awayAgg else homeAgg
                                Text(
                                    text = "AGG: $myAgg - $oppAgg",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SubLineYellow
                                )
                            }
                        }

                        Text(
                            text = oppClub.name.take(3).uppercase(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⏱️ ${currentMinute}'",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isPlaying -> PitchGreen.copy(alpha = 0.15f)
                                    wasSubbedOff -> MutedRed.copy(alpha = 0.15f)
                                    else -> SubLineYellow.copy(alpha = 0.15f)
                                }
                            )
                            .border(
                                1.dp,
                                when {
                                    isPlaying -> PitchGreen.copy(alpha = 0.4f)
                                    wasSubbedOff -> MutedRed.copy(alpha = 0.4f)
                                    else -> SubLineYellow.copy(alpha = 0.4f)
                                },
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = when {
                                isPlaying -> "🏃 ON THE PITCH"
                                wasSubbedOff -> "🪑 SUBBED OFF"
                                else -> "🪑 BENCHED (Subbing on at ${playerCameOnMinute}')"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isPlaying -> PitchGreen
                                wasSubbedOff -> MutedRed
                                else -> SubLineYellow
                            }
                        )
                    }

                    if (isPlaying && !isMatchCompleted && !isAtHalfTime) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                wasSubbedOff = true
                                subbedOffMinute = currentMinute
                                commentaryLogs.add(LiveCommentary(currentMinute, "🔄 SUBSTITUTION: You requested a substitution. You head to the bench to preserve your energy.", isImportant = true))
                            },
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("request_sub_button"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            border = BorderStroke(1.dp, MutedRed.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MutedRed)
                        ) {
                            Text(text = "🪑 Request Sub", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Display Player live stats
                    if (playerGoalsScored > 0 || playerAssistsByMe > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "⭐ Your Impact: ${playerGoalsScored}G, ${playerAssistsByMe}A",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PitchGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // LIVE FEED / COMMENTARY LOGS
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSlate)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Live Match Commentary",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(commentaryLogs) { log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${log.minute}'",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (log.isImportant) PitchGreen else TextSecondary,
                                    modifier = Modifier.width(36.dp)
                                )
                                Text(
                                    text = log.text,
                                    fontSize = 12.sp,
                                    fontWeight = if (log.isImportant) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        log.isGoal -> TrophyGold
                                        log.isImportant -> TextPrimary
                                        else -> TextSecondary
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // KEY MOMENT POP-UP LAYER / SPLIT-PANE DIALOG
        if (isMomentActive) {
            val moment = generatedKeyMoments[activeMomentIndex]

            Card(
                modifier = Modifier
                    .width(420.dp)
                    .fillMaxHeight()
                    .background(SportsDarkBg)
                    .border(1.dp, BorderColor),
                colors = CardDefaults.cardColors(containerColor = SportsCardBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚡ KEY MOMENT",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = TrophyGold
                        )
                        Text(
                            text = "${moment.minute}'",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // SCHEMATIC PITCH WHITEBOARD GRAPHIC
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E3A1E))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Outer border line
                            drawRect(
                                color = Color.White.copy(alpha = 0.2f),
                                topLeft = Offset(10f, 10f),
                                size = Size(w - 20f, h - 20f),
                                style = Stroke(width = 2f)
                            )

                            // Goal Box (Right side Goal box)
                            drawRect(
                                color = Color.White.copy(alpha = 0.2f),
                                topLeft = Offset(w - 60f, h / 2f - 40f),
                                size = Size(50f, 80f),
                                style = Stroke(width = 2f)
                            )

                            // Goal Box (Left side Goal box)
                            drawRect(
                                color = Color.White.copy(alpha = 0.2f),
                                topLeft = Offset(10f, h / 2f - 40f),
                                size = Size(50f, 80f),
                                style = Stroke(width = 2f)
                            )

                            // Center line & circle
                            drawLine(
                                color = Color.White.copy(alpha = 0.2f),
                                start = Offset(w / 2f, 10f),
                                end = Offset(w / 2f, h - 10f),
                                strokeWidth = 2f
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.2f),
                                center = Offset(w / 2f, h / 2f),
                                radius = 30f,
                                style = Stroke(width = 2f)
                            )

                            // Draw dots based on key moment type
                            when (moment.type) {
                                MomentType.KEEPER_1V1 -> {
                                    // Player dot (glowing green) running toward right goal
                                    drawCircle(color = PitchGreen, center = Offset(w * 0.7f, h / 2f), radius = 8f)
                                    // Ball dot (yellow)
                                    drawCircle(color = TrophyGold, center = Offset(w * 0.72f, h / 2f), radius = 5f)
                                    // Goalkeeper (blue)
                                    drawCircle(color = Color(0xFF3B82F6), center = Offset(w - 30f, h / 2f), radius = 8f)
                                    // Red Defender trailing from behind
                                    drawCircle(color = MutedRed, center = Offset(w * 0.55f, h / 2f - 20f), radius = 7f)
                                    drawCircle(color = MutedRed, center = Offset(w * 0.55f, h / 2f + 20f), radius = 7f)
                                }
                                MomentType.THROUGH_BALL -> {
                                    // Player dot
                                    drawCircle(color = PitchGreen, center = Offset(w * 0.65f, h * 0.35f), radius = 8f)
                                    // Ball dot being passed
                                    drawCircle(color = TrophyGold, center = Offset(w * 0.55f, h * 0.45f), radius = 5f)
                                    // Midfielder teammate (green outlines)
                                    drawCircle(color = PitchGreen.copy(alpha = 0.4f), center = Offset(w * 0.4f, h * 0.6f), radius = 7f)
                                    // Red defender marking closely
                                    drawCircle(color = MutedRed, center = Offset(w * 0.7f, h * 0.32f), radius = 7f)
                                }
                                MomentType.PENALTY -> {
                                    // Penalty Spot
                                    drawCircle(color = Color.White.copy(alpha = 0.6f), center = Offset(w - 60f, h / 2f), radius = 3f)
                                    // Player dot
                                    drawCircle(color = PitchGreen, center = Offset(w - 80f, h / 2f), radius = 8f)
                                    // Ball dot
                                    drawCircle(color = TrophyGold, center = Offset(w - 60f, h / 2f), radius = 5f)
                                    // Goalkeeper on the line
                                    drawCircle(color = Color(0xFF3B82F6), center = Offset(w - 15f, h / 2f), radius = 8f)
                                }
                                MomentType.LATE_DEFENSIVE -> {
                                    // Player dot in defense (left half)
                                    drawCircle(color = PitchGreen, center = Offset(w * 0.25f, h * 0.45f), radius = 8f)
                                    // Teammate defenders
                                    drawCircle(color = PitchGreen.copy(alpha = 0.4f), center = Offset(w * 0.15f, h * 0.3f), radius = 7f)
                                    drawCircle(color = PitchGreen.copy(alpha = 0.4f), center = Offset(w * 0.15f, h * 0.7f), radius = 7f)
                                    // Ball
                                    drawCircle(color = TrophyGold, center = Offset(w * 0.32f, h * 0.5f), radius = 5f)
                                    // Opponent attacker (red) threatening
                                    drawCircle(color = MutedRed, center = Offset(w * 0.35f, h * 0.55f), radius = 8f)
                                    drawCircle(color = MutedRed, center = Offset(w * 0.4f, h * 0.3f), radius = 7f)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // COUNTDOWN RING TIMER
                    if (activeChoice == null) {
                        var timerProgress by remember { mutableStateOf(5.0f) }
                        LaunchedEffect(moment) {
                            timerProgress = 5.0f
                            while (timerProgress > 0f && activeChoice == null) {
                                delay(100)
                                timerProgress -= 0.1f
                            }
                            if (timerProgress <= 0f && activeChoice == null) {
                                resolveKeyMomentChoice(null)
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { timerProgress / 5.0f },
                                color = if (timerProgress < 1.5f) MutedRed else PitchGreen,
                                trackColor = BorderColor,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Decide in ${"%.1f".format(timerProgress)}s",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (timerProgress < 1.5f) MutedRed else TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // MOMENT DESCRIPTION
                    Text(
                        text = moment.description,
                        fontSize = 13.sp,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // SPECIAL MECHANIC: PENALTY TIMING BAR
                    if (moment.type == MomentType.PENALTY && activeChoice == null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "TIMING BAR: Strike in the GREEN zone for a massive boost!",
                                fontSize = 11.sp,
                                color = SubLineYellow,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DarkSlate)
                                    .border(1.dp, BorderColor)
                            ) {
                                // Green zone in the middle
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(0.20f)
                                        .align(Alignment.Center)
                                        .background(PitchGreen.copy(alpha = 0.5f))
                                )
                                // Red/orange side zones
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(0.02f)
                                        .align(Alignment.Center)
                                        .background(PitchGreen)
                                )
                                // Sliding marker
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(0.03f)
                                        .align(BiasAlignment(horizontalBias = (penaltyIndicatorProgress * 2f) - 1.0f, verticalBias = 0f))
                                        .background(TrophyGold)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // CHOICES / ACTIONS SELECTOR
                    if (activeChoice == null) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            moment.choices.forEachIndexed { idx, choice ->
                                Button(
                                    onClick = { resolveKeyMomentChoice(choice) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("moment_choice_${idx}"),
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSlate),
                                    shape = RoundedCornerShape(22.dp)
                                ) {
                                    Text(
                                        text = choice,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    } else {
                        // RESOLVED OUTCOME VIEWER
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (isChoiceSuccessful) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = "Result",
                                tint = if (isChoiceSuccessful) PitchGreen else MutedRed,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isChoiceSuccessful) "SUCCESS!" else "MISSED CHANCE",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isChoiceSuccessful) PitchGreen else MutedRed
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = choiceResolutionResult ?: "The attempt failed.",
                                fontSize = 12.sp,
                                color = TextPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { dismissKeyMoment() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("continue_match_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = PitchGreen),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text(
                                    text = "CONTINUE MATCH",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }
        } else if (isAtHalfTime) {
            Card(
                modifier = Modifier
                    .width(420.dp)
                    .fillMaxHeight()
                    .background(SportsDarkBg)
                    .border(1.dp, BorderColor),
                colors = CardDefaults.cardColors(containerColor = SportsCardBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "⏸️ HALF TIME",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = TrophyGold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "The referee blows the whistle for half-time. Head to the dressing room to regroup.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Box summarizing scores
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSlate)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "CURRENT SCORE",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = myClub.name + " (YOU)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PitchGreen,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    text = if (fixture.homeClubId == player.currentClubId) " $runningHomeScore - $runningAwayScore " else " $runningAwayScore - $runningHomeScore ",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TrophyGold,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                Text(
                                    text = oppClub.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Player stats at half time
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSlate)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "YOUR PERFORMANCE (45')",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Goals", fontSize = 11.sp, color = TextSecondary)
                                    Text(text = "$playerGoalsScored", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PitchGreen)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Assists", fontSize = 11.sp, color = TextSecondary)
                                    Text(text = "$playerAssistsByMe", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PitchGreen)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Missed", fontSize = 11.sp, color = TextSecondary)
                                    Text(text = "$opportunitiesMissed", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MutedRed)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = BorderColor.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val halfTimeRating = (6.5f + (playerGoalsScored * 1.2f) + (playerAssistsByMe * 0.8f) - (opportunitiesMissed * 0.5f)).coerceIn(1.0f, 10.0f)
                            val halfTimeRatingFormatted = "%.1f".format(halfTimeRating)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚽ MATCH RATING",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .background(
                                            when {
                                                halfTimeRating >= 8.0f -> PitchGreen.copy(alpha = 0.2f)
                                                halfTimeRating >= 6.0f -> TrophyGold.copy(alpha = 0.2f)
                                                else -> MutedRed.copy(alpha = 0.2f)
                                            },
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "$halfTimeRatingFormatted / 10",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = when {
                                            halfTimeRating >= 8.0f -> PitchGreen
                                            halfTimeRating >= 6.0f -> TrophyGold
                                            else -> MutedRed
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Option to Request Substitution during half time
                    if (isPlaying && !wasSubbedOff) {
                        Button(
                            onClick = {
                                wasSubbedOff = true
                                subbedOffMinute = 45
                                commentaryLogs.add(LiveCommentary(45, "🔄 SUBSTITUTION: You opted to be subbed off at half-time to rest and lower your fatigue.", isImportant = true))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("halftime_sub_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MutedRed),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Text(
                                text = "🪑 REQUEST SUB OFF (Save Stamina)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    } else if (wasSubbedOff) {
                        Text(
                            text = "🔄 Subbed off. You will watch the 2nd half from the bench.",
                            fontSize = 12.sp,
                            color = MutedRed,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    Button(
                        onClick = {
                            isAtHalfTime = false
                            hasDoneHalfTime = true
                            commentaryLogs.add(LiveCommentary(45, "▶️ SECOND HALF KICKS OFF!", isImportant = true))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("start_second_half_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PitchGreen),
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        Text(
                            text = "START THE 2ND HALF ▶️",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                }
            }
        } else if (isMatchCompleted) {
            Card(
                modifier = Modifier
                    .width(420.dp)
                    .fillMaxHeight()
                    .background(SportsDarkBg)
                    .border(1.dp, BorderColor),
                colors = CardDefaults.cardColors(containerColor = SportsCardBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Match Finished",
                        tint = TrophyGold,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "MATCH COMPLETED",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = TrophyGold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🏁 Full-time whistle has blown!",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Box summarizing scores
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSlate)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "FINAL SCORE",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = myClub.name + " (YOU)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PitchGreen,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    text = if (fixture.homeClubId == player.currentClubId) " $runningHomeScore - $runningAwayScore " else " $runningAwayScore - $runningHomeScore ",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TrophyGold,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                Text(
                                    text = oppClub.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Player Impact summary
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSlate)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "YOUR PERFORMANCE",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Goals", fontSize = 11.sp, color = TextSecondary)
                                    Text(text = "$playerGoalsScored", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PitchGreen)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Assists", fontSize = 11.sp, color = TextSecondary)
                                    Text(text = "$playerAssistsByMe", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PitchGreen)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Missed", fontSize = 11.sp, color = TextSecondary)
                                    Text(text = "$opportunitiesMissed", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MutedRed)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = BorderColor.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(8.dp))

                            val finalRating = (6.5f + (playerGoalsScored * 1.2f) + (playerAssistsByMe * 0.8f) - (opportunitiesMissed * 0.5f)).coerceIn(1.0f, 10.0f)
                            val ratingFormatted = "%.1f".format(finalRating)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚽ MATCH RATING",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .background(
                                            when {
                                                finalRating >= 8.0f -> PitchGreen.copy(alpha = 0.2f)
                                                finalRating >= 6.0f -> TrophyGold.copy(alpha = 0.2f)
                                                else -> MutedRed.copy(alpha = 0.2f)
                                            },
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "$ratingFormatted / 10",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = when {
                                            finalRating >= 8.0f -> PitchGreen
                                            finalRating >= 6.0f -> TrophyGold
                                            else -> MutedRed
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = {
                            val startMinute = if (playerCameOnMinute != null && playerCameOnMinute > 0) playerCameOnMinute else 0
                            val endMinute = if (wasSubbedOff) (subbedOffMinute ?: 90) else 90
                            val minutesPlayed = if (playerCameOnMinute == null) 0 else (endMinute - startMinute).coerceAtLeast(0)
                            val finalRating = (6.5f + (playerGoalsScored * 1.2f) + (playerAssistsByMe * 0.8f) - (opportunitiesMissed * 0.5f)).coerceIn(1.0f, 10.0f)

                            val gMin = if (playerGoalMinutes.isNotEmpty()) playerGoalMinutes.sorted().joinToString(",") else null
                            val aMin = if (playerAssistMinutes.isNotEmpty()) playerAssistMinutes.sorted().joinToString(",") else null
                            onMatchFinished(playerGoalsScored, playerAssistsByMe, runningHomeScore, runningAwayScore, minutesPlayed, finalRating, gMin, aMin)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("finish_match_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PitchGreen),
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        Text(
                            text = "FINISH & RETURN",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SimulatedMatchSummaryScreen(
    player: PlayerEntity,
    myClub: ClubEntity,
    oppClub: ClubEntity,
    fixture: FixtureEntity,
    onContinue: () -> Unit
) {
    val isHome = fixture.homeClubId == player.currentClubId
    val homeScore = fixture.homeScore ?: 0
    val awayScore = fixture.awayScore ?: 0
    val rating = fixture.playerRating ?: 6.5f
    val ratingFormatted = "%.1f".format(rating)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SportsDarkBg)
            .padding(24.dp)
            .testTag("simulated_summary_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.SportsSoccer,
                contentDescription = "Match Ball",
                tint = PitchGreen,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "SIMULATED SUMMARY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = PitchGreen,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Scoreboard Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SportsCardBg)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = myClub.name + " (YOU)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PitchGreen,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End
                        )
                        Text(
                            text = if (isHome) " $homeScore - $awayScore " else " $awayScore - $homeScore ",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = TrophyGold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Text(
                            text = oppClub.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Performance Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSlate)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "YOUR SIMULATED PERFORMANCE",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Goals", fontSize = 11.sp, color = TextSecondary)
                            Text(text = "${fixture.playerGoals}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PitchGreen)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Assists", fontSize = 11.sp, color = TextSecondary)
                            Text(text = "${fixture.playerAssists}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PitchGreen)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = BorderColor.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚽ MATCH RATING",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    when {
                                        rating >= 8.0f -> PitchGreen.copy(alpha = 0.2f)
                                        rating >= 6.0f -> TrophyGold.copy(alpha = 0.2f)
                                        else -> MutedRed.copy(alpha = 0.2f)
                                    },
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$ratingFormatted / 10",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
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

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("dismiss_sim_summary_button"),
                colors = ButtonDefaults.buttonColors(containerColor = PitchGreen),
                shape = RoundedCornerShape(26.dp)
            ) {
                Text(
                    text = "CONTINUE",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }
        }
    }
}
