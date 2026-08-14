package com.example.ui

import com.example.ui.components.ClubCrestIcon
import com.example.ui.components.getClubColors

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
import androidx.compose.ui.graphics.PathEffect
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
    KEEPER_1V1, THROUGH_BALL, PENALTY, LATE_DEFENSIVE,

    // OFFENSIVE SCENARIOS (can score)
    CROSS_FROM_WING,
    COUNTER_ATTACK,
    FREE_KICK_OPPORTUNITY,
    ONE_ON_ONE_BREAKS,
    REBOUND_OPPORTUNITY,
    CORNER_KICK,
    BUILDUP_PLAY,
    DETAILED_CROSS,
    FINAL_MINUTE_PRESSURE,
    DEFENSIVE_PRESSURE,

    // DEFENSIVE SCENARIOS (cannot directly score)
    DEFENSIVE_TRANSITION,
    PRESSING_TRIGGER,
    SET_PIECE_DEFENSE,
    GOAL_KICK_PRESSURE
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

private fun selectMomentType(
    min: Int,
    isLateAndWinning: Boolean,
    penaltiesGenerated: Int,
    oppReputation: String
): MomentType {
    if (isLateAndWinning && Random.nextFloat() < 0.60f) {
        return MomentType.LATE_DEFENSIVE
    }

    val (offWeight, defWeight, origWeight) = when (oppReputation) {
        "ELITE", "BIG" -> Triple(0.20f, 0.35f, 0.45f)
        "SMALL", "MID" -> Triple(0.40f, 0.10f, 0.50f)
        else -> Triple(0.30f, 0.20f, 0.50f)
    }

    val roll = Random.nextFloat()
    return when {
        roll < offWeight -> {
            listOf(
                MomentType.CROSS_FROM_WING,
                MomentType.COUNTER_ATTACK,
                MomentType.FREE_KICK_OPPORTUNITY,
                MomentType.ONE_ON_ONE_BREAKS,
                MomentType.REBOUND_OPPORTUNITY,
                MomentType.CORNER_KICK,
                MomentType.BUILDUP_PLAY,
                MomentType.DETAILED_CROSS,
                MomentType.FINAL_MINUTE_PRESSURE,
                MomentType.DEFENSIVE_PRESSURE
            ).random()
        }
        roll < offWeight + defWeight -> {
            listOf(
                MomentType.DEFENSIVE_TRANSITION,
                MomentType.PRESSING_TRIGGER,
                MomentType.SET_PIECE_DEFENSE,
                MomentType.GOAL_KICK_PRESSURE
            ).random()
        }
        else -> {
            if (penaltiesGenerated >= 2) {
                if (Random.nextFloat() < 0.50f) MomentType.KEEPER_1V1 else MomentType.THROUGH_BALL
            } else {
                val origRoll = Random.nextFloat()
                when {
                    origRoll < 0.40f -> MomentType.KEEPER_1V1
                    origRoll < 0.90f -> MomentType.THROUGH_BALL
                    else -> MomentType.PENALTY
                }
            }
        }
    }
}

private fun getMomentDetails(type: MomentType): Pair<String, List<String>> {
    return when (type) {
        MomentType.KEEPER_1V1 -> Pair(
            "You break free from the defensive line! It's just you and the goalkeeper approaching at high speed.",
            listOf("Chip the keeper", "Shoot low far post", "Round the keeper", "Square to teammate")
        )
        MomentType.THROUGH_BALL -> Pair(
            "A teammate whips a beautiful through-ball into the penalty box. A defender is pressing closely behind you.",
            listOf("First-time shot", "Take a touch and shoot", "Shield and lay off")
        )
        MomentType.PENALTY -> Pair(
            "PENALTY KICK! You step up to take the penalty under high pressure from the hostile crowd.",
            listOf("Top Left Corner", "Top Right Corner", "Bottom Left Corner", "Bottom Right Corner")
        )
        MomentType.LATE_DEFENSIVE -> Pair(
            "The opponents are launching a desperate late offensive. You need to drop back and help lock down the victory.",
            listOf("Track your marker", "Push forward for the counter")
        )
        MomentType.CROSS_FROM_WING -> Pair(
            "⚡ WING CROSS! A dangerous ball whips in from the byline. The box is crowded — can you get a clean connection?",
            listOf("Dive header", "Stretch to meet it", "Let it bounce", "Cut back for a better chance")
        )
        MomentType.COUNTER_ATTACK -> Pair(
            "⚡ COUNTER ATTACK! Your team wins the ball in the opponent's half. Numbers are ahead — what's your move?",
            listOf("Run in behind", "Take a quick shot", "Square it early", "Hold up play")
        )
        MomentType.FREE_KICK_OPPORTUNITY -> Pair(
            "⚡ FREE KICK! You win a dangerous set piece opportunity 25 yards from goal. The wall is set...",
            listOf("Curl it over the wall", "Drive it low", "Lay it short", "Go for the knuckleball")
        )
        MomentType.ONE_ON_ONE_BREAKS -> Pair(
            "⚡ ONE-ON-ONE! You're through on goal but a defender is recovering. Pick your finish carefully.",
            listOf("Go alone and shoot", "Square to overlapping fullback", "Cut inside onto stronger foot", "Wait for support to arrive")
        )
        MomentType.REBOUND_OPPORTUNITY -> Pair(
            "⚡ REBOUND! The keeper parries the shot but the ball falls loose. First to react gets the chance.",
            listOf("Pounce on the rebound", "Pass to the edge of the box", "Shoot first-time", "Wait for it to drop")
        )
        MomentType.CORNER_KICK -> Pair(
            "⚡ CORNER KICK! A dangerous delivery is coming in. Where will you attack the ball?",
            listOf("Attack the near post", "Make a late run to the far post", "Stay back as an outlet", "Go for a crowd-induced scramble")
        )
        MomentType.BUILDUP_PLAY -> Pair(
            "⚡ BUILDUP PLAY! Your team works the ball patiently against a deep block. What's the best way to stretch them?",
            listOf("Make a diagonal run", "Drop into the pocket", "Switch play to the weak side", "Call for a through ball")
        )
        MomentType.DETAILED_CROSS -> Pair(
            "⚡ BOX CHAOS! A whipped ball into a crowded penalty area. Multiple players are jostling for position.",
            listOf("Power header", "Glancing effort", "Chest down and shoot", "Lay it off to the edge")
        )
        MomentType.FINAL_MINUTE_PRESSURE -> Pair(
            "⚡ FINAL PUSH! Your team is pouring forward late looking for a winner. Make it count.",
            listOf("Make a decoy run", "Go for goal yourself", "Create space for a teammate", "Hold the ball up")
        )
        MomentType.DEFENSIVE_PRESSURE -> Pair(
            "⚡ MIDFIELD PRESSURE! Your team is under intense pressure in midfield. Retain or risk losing it?",
            listOf("Play a risky forward pass", "Safe square pass", "Take on my marker", "Drop deep to regroup")
        )
        MomentType.DEFENSIVE_TRANSITION -> Pair(
            "⚡ COUNTER-PRESSURE! The opponent wins the ball and is breaking forward. Your defensive positioning is critical here.",
            listOf("Track back and delay the runner", "Cut off the central passing lane", "Hold position for offside trap", "Sprint to press high up the pitch")
        )
        MomentType.PRESSING_TRIGGER -> Pair(
            "⚡ HIGH PRESS! The opponent has the ball under pressure. As the forward, your positioning determines whether they play out safely or lose possession.",
            listOf("Jump the pass early", "Let it come and win it clean", "Force them wide", "Press the space they want to receive")
        )
        MomentType.SET_PIECE_DEFENSE -> Pair(
            "⚡ SET-PIECE DANGER! The opponent has a corner kick / free kick into the box. Your marking and aerial ability will be tested.",
            listOf("Jump to head clear the first ball", "Stick tight on my assigned man", "Track the incoming runner", "Cover near post danger")
        )
        MomentType.GOAL_KICK_PRESSURE -> Pair(
            "⚡ GOAL KICK PRESS! You receive the ball as the first attacker but two defenders are closing you down instantly. One touch or three?",
            listOf("Hold up and wait for support", "Turn and take on the defender", "Quick layoff to midfield", "Attempt the nutmeg and drive")
        )
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
    var defensiveActionsCount by remember { mutableStateOf(0) }
    var possessionLostCount by remember { mutableStateOf(0) }
    var matchRatingDelta by remember { mutableStateOf(0.0f) }
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

            // Random Moment Type using weighted selection
            val isLateAndWinning = min > 75 && (if (fixture.homeClubId == player.currentClubId) runningHomeScore > runningAwayScore else runningAwayScore > runningHomeScore)
            val type = selectMomentType(min, isLateAndWinning, penaltiesGenerated, oppClub.reputation)
            if (type == MomentType.PENALTY) {
                penaltiesGenerated++
            }

            val (desc, choices) = getMomentDetails(type)
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
                    val type = selectMomentType(min, isLateAndWinning, penaltiesPreceding, oppClub.reputation)
                    val (desc, choices) = getMomentDetails(type)

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

        val isDefensiveScenario = moment.type in listOf(
            MomentType.LATE_DEFENSIVE,
            MomentType.DEFENSIVE_TRANSITION,
            MomentType.PRESSING_TRIGGER,
            MomentType.SET_PIECE_DEFENSE,
            MomentType.GOAL_KICK_PRESSURE
        )

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
            MomentType.CROSS_FROM_WING -> player.finishing
            MomentType.COUNTER_ATTACK -> player.pace
            MomentType.FREE_KICK_OPPORTUNITY -> player.technique
            MomentType.ONE_ON_ONE_BREAKS -> player.finishing
            MomentType.REBOUND_OPPORTUNITY -> player.finishing
            MomentType.CORNER_KICK -> player.physical
            MomentType.BUILDUP_PLAY -> player.passing
            MomentType.DETAILED_CROSS -> player.physical
            MomentType.FINAL_MINUTE_PRESSURE -> player.technique
            MomentType.DEFENSIVE_PRESSURE -> player.passing
            MomentType.DEFENSIVE_TRANSITION -> player.physical
            MomentType.PRESSING_TRIGGER -> player.pace
            MomentType.SET_PIECE_DEFENSE -> player.physical
            MomentType.GOAL_KICK_PRESSURE -> player.technique
        }

        var tacticalSwing = when (moment.type) {
            MomentType.KEEPER_1V1 -> when (choice) {
                "Chip the keeper" -> 0.15f
                "Shoot low far post" -> 0.10f
                "Round the keeper" -> 0.05f
                "Square to teammate" -> 0.20f
                else -> -0.15f
            }
            MomentType.THROUGH_BALL -> when (choice) {
                "First-time shot" -> 0.05f
                "Take a touch and shoot" -> 0.10f
                "Shield and lay off" -> 0.20f
                else -> -0.15f
            }
            MomentType.PENALTY -> {
                val inGreenZone = penaltyIndicatorProgress in 0.40f..0.60f
                if (inGreenZone) 0.25f else -0.20f
            }
            MomentType.LATE_DEFENSIVE -> when (choice) {
                "Track your marker" -> 0.20f
                "Push forward for the counter" -> -0.10f
                else -> -0.20f
            }
            MomentType.CROSS_FROM_WING -> when (choice) {
                "Dive header" -> 0.10f
                "Stretch to meet it" -> -0.05f
                "Let it bounce" -> -0.20f
                "Cut back for a better chance" -> 0.15f
                else -> 0.0f
            }
            MomentType.COUNTER_ATTACK -> when (choice) {
                "Run in behind" -> 0.15f
                "Take a quick shot" -> 0.05f
                "Square it early" -> 0.20f
                "Hold up play" -> -0.10f
                else -> 0.0f
            }
            MomentType.FREE_KICK_OPPORTUNITY -> when (choice) {
                "Curl it over the wall" -> 0.15f
                "Drive it low" -> 0.10f
                "Lay it short" -> 0.20f
                "Go for the knuckleball" -> -0.05f
                else -> 0.0f
            }
            MomentType.ONE_ON_ONE_BREAKS -> when (choice) {
                "Go alone and shoot" -> 0.05f
                "Square to overlapping fullback" -> 0.20f
                "Cut inside onto stronger foot" -> 0.10f
                "Wait for support to arrive" -> -0.15f
                else -> 0.0f
            }
            MomentType.REBOUND_OPPORTUNITY -> when (choice) {
                "Pounce on the rebound" -> 0.25f
                "Pass to the edge of the box" -> 0.15f
                "Shoot first-time" -> 0.20f
                "Wait for it to drop" -> -0.20f
                else -> 0.0f
            }
            MomentType.CORNER_KICK -> when (choice) {
                "Attack the near post" -> 0.10f
                "Make a late run to the far post" -> 0.20f
                "Stay back as an outlet" -> 0.15f
                "Go for a crowd-induced scramble" -> -0.10f
                else -> 0.0f
            }
            MomentType.BUILDUP_PLAY -> when (choice) {
                "Make a diagonal run" -> 0.10f
                "Drop into the pocket" -> 0.15f
                "Switch play to the weak side" -> 0.18f
                "Call for a through ball" -> 0.05f
                else -> 0.0f
            }
            MomentType.DETAILED_CROSS -> when (choice) {
                "Power header" -> 0.15f
                "Glancing effort" -> 0.10f
                "Chest down and shoot" -> 0.08f
                "Lay it off to the edge" -> 0.20f
                else -> 0.0f
            }
            MomentType.FINAL_MINUTE_PRESSURE -> when (choice) {
                "Make a decoy run" -> 0.15f
                "Go for goal yourself" -> -0.05f
                "Create space for a teammate" -> 0.20f
                "Hold the ball up" -> 0.25f
                else -> 0.0f
            }
            MomentType.DEFENSIVE_PRESSURE -> when (choice) {
                "Play a risky forward pass" -> 0.05f
                "Safe square pass" -> 0.20f
                "Take on my marker" -> -0.10f
                "Drop deep to regroup" -> 0.15f
                else -> 0.0f
            }
            MomentType.DEFENSIVE_TRANSITION -> when (choice) {
                "Track back and delay the runner" -> 0.25f
                "Cut off the central passing lane" -> 0.15f
                "Hold position for offside trap" -> -0.10f
                "Sprint to press high up the pitch" -> -0.20f
                else -> 0.0f
            }
            MomentType.PRESSING_TRIGGER -> when (choice) {
                "Jump the pass early" -> -0.10f
                "Let it come and win it clean" -> 0.25f
                "Force them wide" -> 0.15f
                "Press the space they want to receive" -> -0.15f
                else -> 0.0f
            }
            MomentType.SET_PIECE_DEFENSE -> when (choice) {
                "Jump to head clear the first ball" -> 0.30f
                "Stick tight on my assigned man" -> 0.20f
                "Track the incoming runner" -> 0.10f
                "Cover near post danger" -> 0.15f
                else -> 0.0f
            }
            MomentType.GOAL_KICK_PRESSURE -> when (choice) {
                "Hold up and wait for support" -> 0.30f
                "Turn and take on the defender" -> -0.05f
                "Quick layoff to midfield" -> 0.25f
                "Attempt the nutmeg and drive" -> -0.20f
                else -> 0.0f
            }
        }

        if (choice == "Timer Expired") {
            tacticalSwing = -0.40f
        }

        val moraleFactor = (player.morale - 50) / 50.0f * 0.08f
        val fanBoost = if (player.fanReputation >= 70 && isPlayerHome) 0.05f else 0.0f

        val isStrongerOpponent = when (oppClub.reputation) {
            "ELITE" -> myClub.reputation != "ELITE"
            "BIG" -> myClub.reputation in listOf("MID", "SMALL")
            "MID" -> myClub.reputation == "SMALL"
            else -> false
        }
        val oppPenalty = if (isStrongerOpponent) 0.12f else 0.0f

        var successChance = (relevantStat / 100.0f) * myTeamRepFactor * oppRepFactor * formFactor * fatigueFactor + moraleFactor + fanBoost + tacticalSwing - oppPenalty
        successChance = successChance.coerceIn(0.05f, 0.95f)

        if (choice == "Timer Expired") {
            isChoiceSuccessful = false
            if (isDefensiveScenario) {
                matchRatingDelta -= 0.6f
                if (isPlayerHome) runningAwayScore++ else runningHomeScore++
                choiceResolutionResult = "TIMEOUT! You failed to react in time. The opponent exploited your indecision and scored!"
            } else {
                opportunitiesMissed++
                possessionLostCount++
                matchRatingDelta -= 0.4f
                choiceResolutionResult = "TIMEOUT! You hesitated too long and lost the opportunity."
            }
            commentaryLogs.add(
                LiveCommentary(moment.minute, choiceResolutionResult ?: "", isGoal = isDefensiveScenario, isImportant = true)
            )
            return
        }

        val success = Random.nextFloat() < successChance
        isChoiceSuccessful = success
        var isGoalOutcome = false

        if (isDefensiveScenario) {
            if (success) {
                defensiveActionsCount++
                matchRatingDelta += 0.2f
                choiceResolutionResult = when (moment.type) {
                    MomentType.LATE_DEFENSIVE -> "Defensive masterclass! You tracked your marker, intercepted the cross, and cleared the danger."
                    MomentType.DEFENSIVE_TRANSITION -> "SUPERB TRACKING! You delayed the counter-attack, forcing the opponent back into their own half."
                    MomentType.PRESSING_TRIGGER -> "BALL WON! Excellent anticipation — you won the ball cleanly and set up a new attack!"
                    MomentType.SET_PIECE_DEFENSE -> "CLEARANCE! You rose highest and headed the set piece forcefully out of the box."
                    MomentType.GOAL_KICK_PRESSURE -> "GREAT SHIELDING! You held up the ball smartly under pressure and layed it off safely."
                    else -> "Solid defending! You stopped the danger."
                }
            } else {
                val concedeChance = when (oppClub.reputation) {
                    "ELITE" -> 0.25f
                    "BIG" -> 0.15f
                    "MID" -> 0.10f
                    "SMALL" -> 0.05f
                    else -> 0.10f
                }
                val opponentScores = Random.nextFloat() < concedeChance || choice in listOf("Push forward for the counter", "Sprint to press high up the pitch")
                if (opponentScores) {
                    if (isPlayerHome) runningAwayScore++ else runningHomeScore++
                    matchRatingDelta -= 0.8f
                    isGoalOutcome = true
                    choiceResolutionResult = "CONCEDED! The opponent punished your defensive choice and scored past the helpless goalkeeper."
                } else {
                    possessionLostCount++
                    matchRatingDelta -= 0.3f
                    choiceResolutionResult = "POSSESSION LOST! You were caught out of position, but your teammates managed to clear the loose ball."
                }
            }
        } else {
            // Offensive scenario
            val assistChoices = listOf(
                "Square to teammate", "Shield and lay off", "Square it early", "Lay it short",
                "Square to overlapping fullback", "Pass to the edge of the box", "Lay it off to the edge",
                "Create space for a teammate", "Switch play to the weak side"
            )
            val nonGoalSuccessChoices = listOf(
                "Let it bounce", "Cut back for a better chance", "Hold up play", "Wait for support to arrive",
                "Stay back as an outlet", "Drop into the pocket", "Make a decoy run", "Hold the ball up",
                "Safe square pass", "Drop deep to regroup"
            )

            if (success) {
                if (choice in assistChoices) {
                    playerAssistsByMe++
                    playerAssistMinutes.add(currentMinute)
                    matchRatingDelta += 1.2f
                    isGoalOutcome = true
                    if (isPlayerHome) runningHomeScore++ else runningAwayScore++
                    choiceResolutionResult = "GREAT VISION! Your brilliant pass picked out a teammate who finished with composure! ASSIST LOGGED."
                } else if (choice in nonGoalSuccessChoices) {
                    matchRatingDelta += 0.3f
                    choiceResolutionResult = "SMART PLAY! You retained possession under pressure and created a promising position."
                } else {
                    // Goal scored!
                    playerGoalsScored++
                    playerGoalMinutes.add(currentMinute)
                    matchRatingDelta += 1.8f
                    isGoalOutcome = true
                    if (isPlayerHome) runningHomeScore++ else runningAwayScore++
                    choiceResolutionResult = when (moment.type) {
                        MomentType.KEEPER_1V1 -> "GOAL! A clinical finish past the rushing goalkeeper!"
                        MomentType.THROUGH_BALL -> "GOAL! Latched onto the through-ball and struck it clean into the net!"
                        MomentType.PENALTY -> "GOAL! Slotted the penalty away with absolute composure!"
                        MomentType.CROSS_FROM_WING -> "GOAL! Connected cleanly with the cross to power it past the keeper!"
                        MomentType.COUNTER_ATTACK -> "GOAL! Punished the high line on the counter with a ruthless finish!"
                        MomentType.FREE_KICK_OPPORTUNITY -> "GOAL! Beautiful free kick curling straight into the net!"
                        MomentType.ONE_ON_ONE_BREAKS -> "GOAL! Coolly beat the defender and slotted it into the bottom corner!"
                        MomentType.REBOUND_OPPORTUNITY -> "GOAL! Quickest to react to the rebound to smash it home!"
                        MomentType.CORNER_KICK -> "GOAL! Attacked the corner delivery and turned it past the keeper!"
                        MomentType.BUILDUP_PLAY -> "GOAL! Magnificent buildup play finished off in style!"
                        MomentType.DETAILED_CROSS -> "GOAL! Out-jumped the defenders in a crowded box to score!"
                        MomentType.FINAL_MINUTE_PRESSURE -> "GOAL! Clutch late strike under intense pressure!"
                        MomentType.DEFENSIVE_PRESSURE -> "GOAL! Escaped the midfield trap and fired home!"
                        else -> "GOAL! What a magnificent strike!"
                    }
                }

                if (isGoalOutcome && choice !in assistChoices && player.rivalRelationship > 70 && Random.nextFloat() < 0.30f) {
                    choiceResolutionResult += " (Selfless assist from ${myClub.rivalStrikerName}!)"
                }
            } else {
                opportunitiesMissed++
                possessionLostCount++
                matchRatingDelta -= 0.5f
                choiceResolutionResult = "MISSED! The chance went begging and possession was turned over."
            }
        }

        commentaryLogs.add(
            LiveCommentary(
                moment.minute,
                choiceResolutionResult ?: "Possession is lost in an attempt.",
                isGoal = isGoalOutcome,
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
                    val homeCrestColors = remember(fixture.homeClubId) { getClubColors(fixture.homeClubId) }
                    val awayCrestColors = remember(fixture.awayClubId) { getClubColors(fixture.awayClubId) }

                    val (homePitchColor, awayPitchColor) = remember(homeCrestColors, awayCrestColors) {
                        val rDiff = kotlin.math.abs(homeCrestColors.first.red - awayCrestColors.first.red)
                        val gDiff = kotlin.math.abs(homeCrestColors.first.green - awayCrestColors.first.green)
                        val bDiff = kotlin.math.abs(homeCrestColors.first.blue - awayCrestColors.first.blue)
                        if ((rDiff + gDiff + bDiff) < 0.35f) {
                            // Badge primary colors are identical or very close -> use secondary color for away team
                            val secRDiff = kotlin.math.abs(homeCrestColors.first.red - awayCrestColors.second.red)
                            val secGDiff = kotlin.math.abs(homeCrestColors.first.green - awayCrestColors.second.green)
                            val secBDiff = kotlin.math.abs(homeCrestColors.first.blue - awayCrestColors.second.blue)
                            if ((secRDiff + secGDiff + secBDiff) < 0.35f) {
                                Pair(homeCrestColors.first, Color(0xFFFACC15))
                            } else {
                                Pair(homeCrestColors.first, awayCrestColors.second)
                            }
                        } else {
                            Pair(homeCrestColors.first, awayCrestColors.first)
                        }
                    }

                    val userTeamColor = if (isPlayerHome) homePitchColor else awayPitchColor
                    val oppTeamColor = if (isPlayerHome) awayPitchColor else homePitchColor

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1B4D2E))
                            .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // 1. Draw grass background with alternating subtle vertical stripes
                            val stripeW = w / 8f
                            for (i in 0..7) {
                                val sColor = if (i % 2 == 0) Color(0xFF1B4D2E) else Color(0xFF1E5432)
                                drawRect(
                                    color = sColor,
                                    topLeft = Offset(i * stripeW, 0f),
                                    size = Size(stripeW, h)
                                )
                            }

                            // 2. Realistic Football Pitch Markings
                            val lineCol = Color.White.copy(alpha = 0.45f)
                            val lineStroke = Stroke(width = 2.2f)
                            val m = 12f // pitch margin

                            // Boundary touchlines & goal lines
                            drawRect(
                                color = lineCol,
                                topLeft = Offset(m, m),
                                size = Size(w - 2 * m, h - 2 * m),
                                style = lineStroke
                            )

                            // Halfway Line
                            drawLine(
                                color = lineCol,
                                start = Offset(w / 2f, m),
                                end = Offset(w / 2f, h - m),
                                strokeWidth = 2.2f
                            )

                            // Center Circle & Spot
                            val centerR = (h - 2 * m) * 0.22f
                            drawCircle(
                                color = lineCol,
                                center = Offset(w / 2f, h / 2f),
                                radius = centerR,
                                style = lineStroke
                            )
                            drawCircle(color = lineCol, center = Offset(w / 2f, h / 2f), radius = 2.5f)

                            // Penalty Boxes (18-yard box)
                            val boxW = (w - 2 * m) * 0.17f
                            val boxH = (h - 2 * m) * 0.58f
                            // Left 18-yard box
                            drawRect(color = lineCol, topLeft = Offset(m, h / 2f - boxH / 2f), size = Size(boxW, boxH), style = lineStroke)
                            // Right 18-yard box
                            drawRect(color = lineCol, topLeft = Offset(w - m - boxW, h / 2f - boxH / 2f), size = Size(boxW, boxH), style = lineStroke)

                            // Goal Boxes (6-yard box)
                            val gBoxW = boxW * 0.36f
                            val gBoxH = boxH * 0.44f
                            // Left 6-yard box
                            drawRect(color = lineCol, topLeft = Offset(m, h / 2f - gBoxH / 2f), size = Size(gBoxW, gBoxH), style = lineStroke)
                            // Right 6-yard box
                            drawRect(color = lineCol, topLeft = Offset(w - m - gBoxW, h / 2f - gBoxH / 2f), size = Size(gBoxW, gBoxH), style = lineStroke)

                            // Penalty Spots & Arcs
                            val leftPenSpotX = m + boxW * 0.65f
                            drawCircle(color = lineCol, center = Offset(leftPenSpotX, h / 2f), radius = 2.5f)
                            drawArc(
                                color = lineCol,
                                startAngle = -55f,
                                sweepAngle = 110f,
                                useCenter = false,
                                topLeft = Offset(leftPenSpotX - centerR * 0.7f, h / 2f - centerR * 0.7f),
                                size = Size(centerR * 1.4f, centerR * 1.4f),
                                style = lineStroke
                            )

                            val rightPenSpotX = w - m - boxW * 0.65f
                            drawCircle(color = lineCol, center = Offset(rightPenSpotX, h / 2f), radius = 2.5f)
                            drawArc(
                                color = lineCol,
                                startAngle = 125f,
                                sweepAngle = 110f,
                                useCenter = false,
                                topLeft = Offset(rightPenSpotX - centerR * 0.7f, h / 2f - centerR * 0.7f),
                                size = Size(centerR * 1.4f, centerR * 1.4f),
                                style = lineStroke
                            )

                            // Corner Arcs
                            val cornerR = 10f
                            drawArc(color = lineCol, startAngle = 0f, sweepAngle = 90f, useCenter = false, topLeft = Offset(m - cornerR, m - cornerR), size = Size(cornerR * 2, cornerR * 2), style = lineStroke)
                            drawArc(color = lineCol, startAngle = 90f, sweepAngle = 90f, useCenter = false, topLeft = Offset(w - m - cornerR, m - cornerR), size = Size(cornerR * 2, cornerR * 2), style = lineStroke)
                            drawArc(color = lineCol, startAngle = 270f, sweepAngle = 90f, useCenter = false, topLeft = Offset(m - cornerR, h - m - cornerR), size = Size(cornerR * 2, cornerR * 2), style = lineStroke)
                            drawArc(color = lineCol, startAngle = 180f, sweepAngle = 90f, useCenter = false, topLeft = Offset(w - m - cornerR, h - m - cornerR), size = Size(cornerR * 2, cornerR * 2), style = lineStroke)

                            // Goal posts
                            val gDepth = 6f
                            val gHeight = gBoxH * 0.8f
                            drawRect(color = Color.White.copy(alpha = 0.35f), topLeft = Offset(m - gDepth, h / 2f - gHeight / 2f), size = Size(gDepth, gHeight), style = Stroke(width = 1.5f))
                            drawRect(color = Color.White.copy(alpha = 0.35f), topLeft = Offset(w - m, h / 2f - gHeight / 2f), size = Size(gDepth, gHeight), style = Stroke(width = 1.5f))

                            // Helper functions for drawing tactical dots
                            fun drawDot(xFrac: Float, yFrac: Float, color: Color, isUser: Boolean = false, isGk: Boolean = false) {
                                val px = w * xFrac
                                val py = h * yFrac
                                val dotLum = color.red * 0.299f + color.green * 0.587f + color.blue * 0.114f
                                val outlineColor = if (dotLum < 0.55f) Color.White else Color(0xFF111827)

                                if (isGk) {
                                    val gkColor = if (dotLum > 0.5f) Color(0xFF1E3A8A) else Color(0xFF10B981)
                                    val gkLum = gkColor.red * 0.299f + gkColor.green * 0.587f + gkColor.blue * 0.114f
                                    val gkOutline = if (gkLum < 0.55f) Color.White else Color(0xFF111827)
                                    drawCircle(color = gkColor, center = Offset(px, py), radius = 7.5f)
                                    drawCircle(color = gkOutline, center = Offset(px, py), radius = 7.5f, style = Stroke(width = 1.6f))
                                } else if (isUser) {
                                    // USER PLAYER: Distinct outer halo ring + bright border + star core
                                    drawCircle(color = TrophyGold.copy(alpha = 0.45f), center = Offset(px, py), radius = 14f)
                                    drawCircle(color = TrophyGold, center = Offset(px, py), radius = 11f, style = Stroke(width = 2f))
                                    drawCircle(color = color, center = Offset(px, py), radius = 8.5f)
                                    drawCircle(color = outlineColor, center = Offset(px, py), radius = 8.5f, style = Stroke(width = 2f))
                                    drawCircle(color = Color.White, center = Offset(px, py), radius = 3f)
                                } else {
                                    drawCircle(color = color, center = Offset(px, py), radius = 6.5f)
                                    drawCircle(color = outlineColor, center = Offset(px, py), radius = 6.5f, style = Stroke(width = 1.5f))
                                }
                            }

                            fun drawBall(xFrac: Float, yFrac: Float) {
                                val bx = w * xFrac
                                val by = h * yFrac
                                drawCircle(color = Color.Black.copy(alpha = 0.5f), center = Offset(bx + 2f, by + 2f), radius = 5f)
                                drawCircle(color = Color.White, center = Offset(bx, by), radius = 5f)
                                drawCircle(color = Color.Black, center = Offset(bx, by), radius = 5f, style = Stroke(width = 1.2f))
                                drawCircle(color = Color(0xFF1E293B), center = Offset(bx, by), radius = 2f)
                            }

                            fun drawTrajectory(sx: Float, sy: Float, ex: Float, ey: Float) {
                                drawLine(
                                    color = Color.White.copy(alpha = 0.6f),
                                    start = Offset(w * sx, h * sy),
                                    end = Offset(w * ex, h * ey),
                                    strokeWidth = 2f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                                )
                            }

                            // 3. Render 11 vs 11 Tactical Positions & Ball per Key Moment
                            when (moment.type) {
                                MomentType.KEEPER_1V1 -> {
                                    val userDots = listOf(
                                        Pair(0.06f, 0.50f), Pair(0.38f, 0.20f), Pair(0.35f, 0.40f), Pair(0.35f, 0.60f), Pair(0.38f, 0.80f),
                                        Pair(0.50f, 0.35f), Pair(0.50f, 0.65f), Pair(0.58f, 0.50f), Pair(0.68f, 0.22f), Pair(0.72f, 0.50f), Pair(0.68f, 0.78f)
                                    )
                                    val oppDots = listOf(
                                        Pair(0.92f, 0.50f), Pair(0.64f, 0.44f), Pair(0.64f, 0.56f), Pair(0.62f, 0.18f), Pair(0.62f, 0.82f),
                                        Pair(0.55f, 0.35f), Pair(0.55f, 0.65f), Pair(0.48f, 0.50f), Pair(0.32f, 0.42f), Pair(0.32f, 0.58f), Pair(0.20f, 0.50f)
                                    )
                                    userDots.forEachIndexed { i, (x, y) -> drawDot(x, y, userTeamColor, isUser = (i == 9), isGk = (i == 0)) }
                                    oppDots.forEachIndexed { i, (x, y) -> drawDot(x, y, oppTeamColor, isUser = false, isGk = (i == 0)) }
                                    drawTrajectory(0.58f, 0.50f, 0.72f, 0.50f)
                                    drawBall(0.74f, 0.50f)
                                }
                                MomentType.THROUGH_BALL -> {
                                    val userDots = listOf(
                                        Pair(0.06f, 0.50f), Pair(0.36f, 0.18f), Pair(0.34f, 0.38f), Pair(0.34f, 0.62f), Pair(0.36f, 0.82f),
                                        Pair(0.48f, 0.35f), Pair(0.48f, 0.65f), Pair(0.58f, 0.50f), Pair(0.70f, 0.20f), Pair(0.76f, 0.38f), Pair(0.70f, 0.80f)
                                    )
                                    val oppDots = listOf(
                                        Pair(0.94f, 0.50f), Pair(0.78f, 0.36f), Pair(0.75f, 0.55f), Pair(0.72f, 0.18f), Pair(0.72f, 0.82f),
                                        Pair(0.56f, 0.38f), Pair(0.56f, 0.62f), Pair(0.45f, 0.50f), Pair(0.30f, 0.40f), Pair(0.30f, 0.60f), Pair(0.20f, 0.50f)
                                    )
                                    userDots.forEachIndexed { i, (x, y) -> drawDot(x, y, userTeamColor, isUser = (i == 9), isGk = (i == 0)) }
                                    oppDots.forEachIndexed { i, (x, y) -> drawDot(x, y, oppTeamColor, isUser = false, isGk = (i == 0)) }
                                    drawTrajectory(0.58f, 0.50f, 0.76f, 0.38f)
                                    drawBall(0.68f, 0.43f)
                                }
                                MomentType.PENALTY -> {
                                    val userDots = listOf(
                                        Pair(0.06f, 0.50f), Pair(0.80f, 0.50f), Pair(0.70f, 0.28f), Pair(0.71f, 0.38f), Pair(0.72f, 0.48f),
                                        Pair(0.72f, 0.58f), Pair(0.71f, 0.68f), Pair(0.70f, 0.78f), Pair(0.60f, 0.35f), Pair(0.60f, 0.65f), Pair(0.50f, 0.50f)
                                    )
                                    val oppDots = listOf(
                                        Pair(0.96f, 0.50f), Pair(0.73f, 0.22f), Pair(0.73f, 0.33f), Pair(0.74f, 0.43f), Pair(0.74f, 0.53f),
                                        Pair(0.73f, 0.63f), Pair(0.73f, 0.73f), Pair(0.62f, 0.30f), Pair(0.62f, 0.70f), Pair(0.52f, 0.45f), Pair(0.52f, 0.55f)
                                    )
                                    userDots.forEachIndexed { i, (x, y) -> drawDot(x, y, userTeamColor, isUser = (i == 1), isGk = (i == 0)) }
                                    oppDots.forEachIndexed { i, (x, y) -> drawDot(x, y, oppTeamColor, isUser = false, isGk = (i == 0)) }
                                    drawBall(0.86f, 0.50f)
                                }
                                MomentType.LATE_DEFENSIVE -> {
                                    val userDots = listOf(
                                        Pair(0.06f, 0.50f), Pair(0.20f, 0.22f), Pair(0.18f, 0.38f), Pair(0.18f, 0.62f), Pair(0.20f, 0.78f),
                                        Pair(0.26f, 0.40f), Pair(0.24f, 0.58f), Pair(0.32f, 0.25f), Pair(0.32f, 0.75f), Pair(0.42f, 0.40f), Pair(0.42f, 0.60f)
                                    )
                                    val oppDots = listOf(
                                        Pair(0.94f, 0.50f), Pair(0.12f, 0.18f), Pair(0.22f, 0.42f), Pair(0.20f, 0.55f), Pair(0.14f, 0.32f),
                                        Pair(0.30f, 0.30f), Pair(0.30f, 0.50f), Pair(0.30f, 0.70f), Pair(0.45f, 0.25f), Pair(0.42f, 0.48f), Pair(0.45f, 0.85f)
                                    )
                                    userDots.forEachIndexed { i, (x, y) -> drawDot(x, y, userTeamColor, isUser = (i == 5), isGk = (i == 0)) }
                                    oppDots.forEachIndexed { i, (x, y) -> drawDot(x, y, oppTeamColor, isUser = false, isGk = (i == 0)) }
                                    drawTrajectory(0.12f, 0.18f, 0.22f, 0.42f)
                                    drawBall(0.12f, 0.18f)
                                }
                                MomentType.CROSS_FROM_WING -> {
                                    val userDots = listOf(
                                        Pair(0.06f, 0.50f), Pair(0.90f, 0.15f), Pair(0.82f, 0.48f), Pair(0.80f, 0.32f), Pair(0.78f, 0.65f),
                                        Pair(0.72f, 0.50f), Pair(0.55f, 0.30f), Pair(0.55f, 0.70f), Pair(0.40f, 0.20f), Pair(0.38f, 0.50f), Pair(0.40f, 0.80f)
                                    )
                                    val oppDots = listOf(
                                        Pair(0.95f, 0.50f), Pair(0.84f, 0.42f), Pair(0.84f, 0.55f), Pair(0.86f, 0.28f), Pair(0.82f, 0.70f),
                                        Pair(0.68f, 0.35f), Pair(0.68f, 0.65f), Pair(0.60f, 0.50f), Pair(0.45f, 0.40f), Pair(0.45f, 0.60f), Pair(0.35f, 0.50f)
                                    )
                                    userDots.forEachIndexed { i, (x, y) -> drawDot(x, y, userTeamColor, isUser = (i == 2), isGk = (i == 0)) }
                                    oppDots.forEachIndexed { i, (x, y) -> drawDot(x, y, oppTeamColor, isUser = false, isGk = (i == 0)) }
                                    drawTrajectory(0.90f, 0.15f, 0.82f, 0.48f)
                                    drawBall(0.85f, 0.30f)
                                }
                                MomentType.COUNTER_ATTACK -> {
                                    val userDots = listOf(
                                        Pair(0.06f, 0.50f), Pair(0.28f, 0.20f), Pair(0.25f, 0.40f), Pair(0.25f, 0.60f), Pair(0.28f, 0.80f),
                                        Pair(0.38f, 0.35f), Pair(0.38f, 0.65f), Pair(0.56f, 0.48f), Pair(0.68f, 0.22f), Pair(0.68f, 0.78f), Pair(0.64f, 0.50f)
                                    )
                                    val oppDots = listOf(
                                        Pair(0.94f, 0.50f), Pair(0.72f, 0.40f), Pair(0.72f, 0.60f), Pair(0.60f, 0.15f), Pair(0.60f, 0.85f),
                                        Pair(0.45f, 0.30f), Pair(0.45f, 0.50f), Pair(0.45f, 0.70f), Pair(0.35f, 0.35f), Pair(0.35f, 0.65f), Pair(0.22f, 0.50f)
                                    )
                                    userDots.forEachIndexed { i, (x, y) -> drawDot(x, y, userTeamColor, isUser = (i == 7), isGk = (i == 0)) }
                                    oppDots.forEachIndexed { i, (x, y) -> drawDot(x, y, oppTeamColor, isUser = false, isGk = (i == 0)) }
                                    drawBall(0.58f, 0.48f)
                                }
                                MomentType.FREE_KICK_OPPORTUNITY -> {
                                    val userDots = listOf(
                                        Pair(0.06f, 0.50f), Pair(0.68f, 0.50f), Pair(0.80f, 0.25f), Pair(0.82f, 0.35f), Pair(0.83f, 0.50f),
                                        Pair(0.82f, 0.65f), Pair(0.80f, 0.75f), Pair(0.65f, 0.35f), Pair(0.65f, 0.65f), Pair(0.50f, 0.30f), Pair(0.50f, 0.70f)
                                    )
                                    val oppDots = listOf(
                                        Pair(0.95f, 0.42f), Pair(0.81f, 0.44f), Pair(0.81f, 0.48f), Pair(0.81f, 0.52f), Pair(0.81f, 0.56f),
                                        Pair(0.85f, 0.28f), Pair(0.86f, 0.38f), Pair(0.86f, 0.62f), Pair(0.85f, 0.72f), Pair(0.72f, 0.20f), Pair(0.72f, 0.80f)
                                    )
                                    userDots.forEachIndexed { i, (x, y) -> drawDot(x, y, userTeamColor, isUser = (i == 1), isGk = (i == 0)) }
                                    oppDots.forEachIndexed { i, (x, y) -> drawDot(x, y, oppTeamColor, isUser = false, isGk = (i == 0)) }
                                    drawBall(0.70f, 0.50f)
                                }
                                MomentType.ONE_ON_ONE_BREAKS -> {
                                    val userDots = listOf(
                                        Pair(0.06f, 0.50f), Pair(0.35f, 0.18f), Pair(0.32f, 0.40f), Pair(0.32f, 0.60f), Pair(0.35f, 0.82f),
                                        Pair(0.48f, 0.35f), Pair(0.48f, 0.65f), Pair(0.58f, 0.50f), Pair(0.74f, 0.35f), Pair(0.82f, 0.18f), Pair(0.68f, 0.60f)
                                    )
                                    val oppDots = listOf(
                                        Pair(0.94f, 0.40f), Pair(0.78f, 0.48f), Pair(0.72f, 0.22f), Pair(0.70f, 0.65f), Pair(0.68f, 0.85f),
                                        Pair(0.55f, 0.40f), Pair(0.55f, 0.60f), Pair(0.45f, 0.50f), Pair(0.30f, 0.40f), Pair(0.30f, 0.60f), Pair(0.20f, 0.50f)
                                    )
                                    userDots.forEachIndexed { i, (x, y) -> drawDot(x, y, userTeamColor, isUser = (i == 8), isGk = (i == 0)) }
                                    oppDots.forEachIndexed { i, (x, y) -> drawDot(x, y, oppTeamColor, isUser = false, isGk = (i == 0)) }
                                    drawBall(0.76f, 0.35f)
                                }
                                MomentType.REBOUND_OPPORTUNITY -> {
                                    val userDots = listOf(
                                        Pair(0.06f, 0.50f), Pair(0.40f, 0.20f), Pair(0.38f, 0.42f), Pair(0.38f, 0.58f), Pair(0.40f, 0.80f),
                                        Pair(0.55f, 0.35f), Pair(0.55f, 0.65f), Pair(0.68f, 0.50f), Pair(0.81f, 0.52f), Pair(0.80f, 0.30f), Pair(0.80f, 0.70f)
                                    )
                                    val oppDots = listOf(
                                        Pair(0.92f, 0.38f), Pair(0.87f, 0.58f), Pair(0.86f, 0.32f), Pair(0.85f, 0.70f), Pair(0.75f, 0.45f),
                                        Pair(0.75f, 0.55f), Pair(0.60f, 0.30f), Pair(0.60f, 0.70f), Pair(0.45f, 0.40f), Pair(0.45f, 0.60f), Pair(0.30f, 0.50f)
                                    )
                                    userDots.forEachIndexed { i, (x, y) -> drawDot(x, y, userTeamColor, isUser = (i == 8), isGk = (i == 0)) }
                                    oppDots.forEachIndexed { i, (x, y) -> drawDot(x, y, oppTeamColor, isUser = false, isGk = (i == 0)) }
                                    drawBall(0.85f, 0.52f)
                                }
                                MomentType.CORNER_KICK -> {
                                    val userDots = listOf(
                                        Pair(0.06f, 0.50f), Pair(0.97f, 0.05f), Pair(0.82f, 0.48f), Pair(0.84f, 0.35f), Pair(0.85f, 0.58f),
                                        Pair(0.78f, 0.65f), Pair(0.68f, 0.50f), Pair(0.55f, 0.30f), Pair(0.55f, 0.70f), Pair(0.40f, 0.25f), Pair(0.40f, 0.75f)
                                    )
                                    val oppDots = listOf(
                                        Pair(0.95f, 0.42f), Pair(0.85f, 0.38f), Pair(0.84f, 0.46f), Pair(0.86f, 0.55f), Pair(0.83f, 0.62f),
                                        Pair(0.78f, 0.42f), Pair(0.70f, 0.50f), Pair(0.60f, 0.30f), Pair(0.60f, 0.70f), Pair(0.48f, 0.40f), Pair(0.48f, 0.60f)
                                    )
                                    userDots.forEachIndexed { i, (x, y) -> drawDot(x, y, userTeamColor, isUser = (i == 2), isGk = (i == 0)) }
                                    oppDots.forEachIndexed { i, (x, y) -> drawDot(x, y, oppTeamColor, isUser = false, isGk = (i == 0)) }
                                    drawTrajectory(0.97f, 0.05f, 0.82f, 0.48f)
                                    drawBall(0.88f, 0.30f)
                                }
                                MomentType.BUILDUP_PLAY -> {
                                    val userDots = listOf(
                                        Pair(0.06f, 0.50f), Pair(0.32f, 0.18f), Pair(0.28f, 0.38f), Pair(0.28f, 0.62f), Pair(0.32f, 0.82f),
                                        Pair(0.40f, 0.50f), Pair(0.52f, 0.50f), Pair(0.48f, 0.28f), Pair(0.65f, 0.18f), Pair(0.65f, 0.82f), Pair(0.75f, 0.50f)
                                    )
                                    val oppDots = listOf(
                                        Pair(0.94f, 0.50f), Pair(0.78f, 0.20f), Pair(0.76f, 0.40f), Pair(0.76f, 0.60f), Pair(0.78f, 0.80f),
                                        Pair(0.60f, 0.22f), Pair(0.58f, 0.42f), Pair(0.58f, 0.58f), Pair(0.60f, 0.78f), Pair(0.42f, 0.42f), Pair(0.42f, 0.58f)
                                    )
                                    userDots.forEachIndexed { i, (x, y) -> drawDot(x, y, userTeamColor, isUser = (i == 6), isGk = (i == 0)) }
                                    oppDots.forEachIndexed { i, (x, y) -> drawDot(x, y, oppTeamColor, isUser = false, isGk = (i == 0)) }
                                    drawTrajectory(0.40f, 0.50f, 0.52f, 0.50f)
                                    drawBall(0.51f, 0.50f)
                                }
                                MomentType.DETAILED_CROSS -> {
                                    val userDots = listOf(
                                        Pair(0.06f, 0.50f), Pair(0.83f, 0.48f), Pair(0.85f, 0.38f), Pair(0.82f, 0.60f), Pair(0.72f, 0.20f),
                                        Pair(0.70f, 0.50f), Pair(0.72f, 0.80f), Pair(0.55f, 0.35f), Pair(0.55f, 0.65f), Pair(0.38f, 0.30f), Pair(0.38f, 0.70f)
                                    )
                                    val oppDots = listOf(
                                        Pair(0.95f, 0.45f), Pair(0.85f, 0.42f), Pair(0.84f, 0.52f), Pair(0.86f, 0.32f), Pair(0.86f, 0.65f),
                                        Pair(0.78f, 0.48f), Pair(0.65f, 0.30f), Pair(0.65f, 0.70f), Pair(0.50f, 0.40f), Pair(0.50f, 0.60f), Pair(0.35f, 0.50f)
                                    )
                                    userDots.forEachIndexed { i, (x, y) -> drawDot(x, y, userTeamColor, isUser = (i == 1), isGk = (i == 0)) }
                                    oppDots.forEachIndexed { i, (x, y) -> drawDot(x, y, oppTeamColor, isUser = false, isGk = (i == 0)) }
                                    drawBall(0.83f, 0.42f)
                                }
                                MomentType.FINAL_MINUTE_PRESSURE -> {
                                    val userDots = listOf(
                                        Pair(0.30f, 0.50f), Pair(0.58f, 0.25f), Pair(0.55f, 0.50f), Pair(0.58f, 0.75f), Pair(0.68f, 0.30f),
                                        Pair(0.68f, 0.70f), Pair(0.78f, 0.50f), Pair(0.84f, 0.25f), Pair(0.86f, 0.42f), Pair(0.86f, 0.58f), Pair(0.84f, 0.75f)
                                    )
                                    val oppDots = listOf(
                                        Pair(0.95f, 0.50f), Pair(0.88f, 0.20f), Pair(0.88f, 0.32f), Pair(0.89f, 0.42f), Pair(0.89f, 0.58f),
                                        Pair(0.88f, 0.68f), Pair(0.88f, 0.80f), Pair(0.82f, 0.30f), Pair(0.82f, 0.45f), Pair(0.82f, 0.55f), Pair(0.82f, 0.70f)
                                    )
                                    userDots.forEachIndexed { i, (x, y) -> drawDot(x, y, userTeamColor, isUser = (i == 6), isGk = (i == 0)) }
                                    oppDots.forEachIndexed { i, (x, y) -> drawDot(x, y, oppTeamColor, isUser = false, isGk = (i == 0)) }
                                    drawBall(0.76f, 0.50f)
                                }
                                MomentType.DEFENSIVE_PRESSURE -> {
                                    val userDots = listOf(
                                        Pair(0.06f, 0.50f), Pair(0.28f, 0.20f), Pair(0.25f, 0.40f), Pair(0.25f, 0.60f), Pair(0.28f, 0.80f),
                                        Pair(0.46f, 0.50f), Pair(0.38f, 0.35f), Pair(0.38f, 0.65f), Pair(0.60f, 0.20f), Pair(0.65f, 0.50f), Pair(0.60f, 0.80f)
                                    )
                                    val oppDots = listOf(
                                        Pair(0.94f, 0.50f), Pair(0.42f, 0.40f), Pair(0.52f, 0.52f), Pair(0.44f, 0.62f), Pair(0.65f, 0.25f),
                                        Pair(0.62f, 0.48f), Pair(0.62f, 0.72f), Pair(0.65f, 0.88f), Pair(0.30f, 0.35f), Pair(0.30f, 0.65f), Pair(0.20f, 0.50f)
                                    )
                                    userDots.forEachIndexed { i, (x, y) -> drawDot(x, y, userTeamColor, isUser = (i == 5), isGk = (i == 0)) }
                                    oppDots.forEachIndexed { i, (x, y) -> drawDot(x, y, oppTeamColor, isUser = false, isGk = (i == 0)) }
                                    drawBall(0.45f, 0.50f)
                                }
                                MomentType.DEFENSIVE_TRANSITION -> {
                                    val userDots = listOf(
                                        Pair(0.06f, 0.50f), Pair(0.22f, 0.20f), Pair(0.20f, 0.38f), Pair(0.20f, 0.62f), Pair(0.22f, 0.80f),
                                        Pair(0.36f, 0.44f), Pair(0.30f, 0.60f), Pair(0.42f, 0.25f), Pair(0.42f, 0.75f), Pair(0.55f, 0.40f), Pair(0.55f, 0.60f)
                                    )
                                    val oppDots = listOf(
                                        Pair(0.94f, 0.50f), Pair(0.42f, 0.50f), Pair(0.48f, 0.22f), Pair(0.48f, 0.78f), Pair(0.32f, 0.35f),
                                        Pair(0.65f, 0.30f), Pair(0.65f, 0.70f), Pair(0.75f, 0.20f), Pair(0.72f, 0.40f), Pair(0.72f, 0.60f), Pair(0.75f, 0.80f)
                                    )
                                    userDots.forEachIndexed { i, (x, y) -> drawDot(x, y, userTeamColor, isUser = (i == 5), isGk = (i == 0)) }
                                    oppDots.forEachIndexed { i, (x, y) -> drawDot(x, y, oppTeamColor, isUser = false, isGk = (i == 0)) }
                                    drawTrajectory(0.42f, 0.50f, 0.32f, 0.35f)
                                    drawBall(0.43f, 0.50f)
                                }
                                MomentType.PRESSING_TRIGGER -> {
                                    val userDots = listOf(
                                        Pair(0.06f, 0.50f), Pair(0.42f, 0.20f), Pair(0.38f, 0.40f), Pair(0.38f, 0.60f), Pair(0.42f, 0.80f),
                                        Pair(0.50f, 0.35f), Pair(0.50f, 0.65f), Pair(0.40f, 0.50f), Pair(0.28f, 0.38f), Pair(0.30f, 0.18f), Pair(0.30f, 0.65f)
                                    )
                                    val oppDots = listOf(
                                        Pair(0.08f, 0.50f), Pair(0.20f, 0.35f), Pair(0.18f, 0.18f), Pair(0.18f, 0.62f), Pair(0.18f, 0.82f),
                                        Pair(0.32f, 0.35f), Pair(0.32f, 0.65f), Pair(0.45f, 0.50f), Pair(0.60f, 0.30f), Pair(0.60f, 0.70f), Pair(0.70f, 0.50f)
                                    )
                                    userDots.forEachIndexed { i, (x, y) -> drawDot(x, y, userTeamColor, isUser = (i == 8), isGk = (i == 0)) }
                                    oppDots.forEachIndexed { i, (x, y) -> drawDot(x, y, oppTeamColor, isUser = false, isGk = (i == 0)) }
                                    drawTrajectory(0.28f, 0.38f, 0.20f, 0.35f)
                                    drawBall(0.21f, 0.35f)
                                }
                                MomentType.SET_PIECE_DEFENSE -> {
                                    val userDots = listOf(
                                        Pair(0.06f, 0.50f), Pair(0.18f, 0.48f), Pair(0.15f, 0.35f), Pair(0.15f, 0.60f), Pair(0.12f, 0.42f),
                                        Pair(0.12f, 0.55f), Pair(0.24f, 0.30f), Pair(0.24f, 0.70f), Pair(0.32f, 0.50f), Pair(0.48f, 0.35f), Pair(0.48f, 0.65f)
                                    )
                                    val oppDots = listOf(
                                        Pair(0.94f, 0.50f), Pair(0.05f, 0.05f), Pair(0.18f, 0.42f), Pair(0.16f, 0.52f), Pair(0.20f, 0.35f),
                                        Pair(0.20f, 0.65f), Pair(0.28f, 0.40f), Pair(0.28f, 0.60f), Pair(0.42f, 0.30f), Pair(0.42f, 0.70f), Pair(0.60f, 0.50f)
                                    )
                                    userDots.forEachIndexed { i, (x, y) -> drawDot(x, y, userTeamColor, isUser = (i == 1), isGk = (i == 0)) }
                                    oppDots.forEachIndexed { i, (x, y) -> drawDot(x, y, oppTeamColor, isUser = false, isGk = (i == 0)) }
                                    drawTrajectory(0.05f, 0.05f, 0.18f, 0.45f)
                                    drawBall(0.18f, 0.45f)
                                }
                                MomentType.GOAL_KICK_PRESSURE -> {
                                    val userDots = listOf(
                                        Pair(0.08f, 0.50f), Pair(0.18f, 0.22f), Pair(0.16f, 0.42f), Pair(0.16f, 0.58f), Pair(0.18f, 0.78f),
                                        Pair(0.32f, 0.50f), Pair(0.28f, 0.32f), Pair(0.28f, 0.68f), Pair(0.50f, 0.25f), Pair(0.55f, 0.50f), Pair(0.50f, 0.75f)
                                    )
                                    val oppDots = listOf(
                                        Pair(0.94f, 0.50f), Pair(0.26f, 0.42f), Pair(0.36f, 0.58f), Pair(0.35f, 0.25f), Pair(0.35f, 0.75f),
                                        Pair(0.42f, 0.50f), Pair(0.62f, 0.20f), Pair(0.58f, 0.40f), Pair(0.58f, 0.60f), Pair(0.62f, 0.80f), Pair(0.30f, 0.50f)
                                    )
                                    userDots.forEachIndexed { i, (x, y) -> drawDot(x, y, userTeamColor, isUser = (i == 5), isGk = (i == 0)) }
                                    oppDots.forEachIndexed { i, (x, y) -> drawDot(x, y, oppTeamColor, isUser = false, isGk = (i == 0)) }
                                    drawTrajectory(0.08f, 0.50f, 0.32f, 0.50f)
                                    drawBall(0.31f, 0.50f)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // LEGEND BAR BELOW PITCH
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(userTeamColor)
                                    .border(1.5.dp, TrophyGold, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${myClub.name.take(12)} (You ★)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TrophyGold
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val oppLum = oppTeamColor.red * 0.299f + oppTeamColor.green * 0.587f + oppTeamColor.blue * 0.114f
                            val oppOutline = if (oppLum < 0.55f) Color.White else Color(0xFF111827)
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(oppTeamColor)
                                    .border(1.dp, oppOutline, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = oppClub.name.take(12),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.dp, Color.Black, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Ball ⚽",
                                fontSize = 11.sp,
                                color = TextSecondary.copy(alpha = 0.7f)
                            )
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
                                    Text(text = "Defense", fontSize = 11.sp, color = TextSecondary)
                                    Text(text = "$defensiveActionsCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TrophyGold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Missed", fontSize = 11.sp, color = TextSecondary)
                                    Text(text = "$opportunitiesMissed", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MutedRed)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = BorderColor.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(8.dp))

                            val finalRating = (6.5f + matchRatingDelta + (playerGoalsScored * 1.2f) + (playerAssistsByMe * 0.8f) - (opportunitiesMissed * 0.5f) + (defensiveActionsCount * 0.3f)).coerceIn(1.0f, 10.0f)
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
                            val finalRating = (6.5f + matchRatingDelta + (playerGoalsScored * 1.2f) + (playerAssistsByMe * 0.8f) - (opportunitiesMissed * 0.5f) + (defensiveActionsCount * 0.3f)).coerceIn(1.0f, 10.0f)

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
