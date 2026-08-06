package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ClubEntity
import com.example.data.GameStateEntity
import com.example.data.PlayerEntity
import com.example.data.formatSeasonYear
import com.example.ui.*
import com.example.ui.dialogs.ChoiceEventDialog
import com.example.ui.dialogs.SeasonSummaryDialog
import com.example.ui.dialogs.SettingsDialog
import com.example.ui.dialogs.TransferOffersDialog
import com.example.ui.tabs.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameplayScreen(
    viewModel: CareerViewModel,
    player: PlayerEntity?,
    clubs: List<ClubEntity>,
    gameState: GameStateEntity?,
    selectedTab: GameTab
) {
    if (player == null || gameState == null) return

    val clubsMap = remember(clubs) { clubs.associateBy { it.id } }
    val myClub = clubsMap[player.currentClubId]

    val isShowingTransfer by viewModel.isShowingTransferDialog.collectAsStateWithLifecycle()
    val isShowingFamilyPage by viewModel.isShowingFamilyPage.collectAsStateWithLifecycle()
    val isShowingSettings by viewModel.isShowingSettingsDialog.collectAsStateWithLifecycle()
    val isSimulatingSeason by viewModel.isSimulatingSeason.collectAsStateWithLifecycle()

    val selectedClubIdForProfile by viewModel.selectedClubIdForProfile.collectAsStateWithLifecycle()
    val selectedClubForProfile by viewModel.selectedClubForProfileFlow.collectAsStateWithLifecycle()
    val selectedClubHistory by viewModel.selectedClubHistoryFlow.collectAsStateWithLifecycle()
    val selectedClubTrophies by viewModel.selectedClubTrophiesFlow.collectAsStateWithLifecycle()
    val selectedClubRecords by viewModel.selectedClubRecordsFlow.collectAsStateWithLifecycle()

    val streetGames by viewModel.streetGamesFlow.collectAsStateWithLifecycle()
    val youthAcademies by viewModel.youthAcademiesFlow.collectAsStateWithLifecycle()
    val youthStandings by viewModel.youthStandingsFlow.collectAsStateWithLifecycle()
    val youthFixtures by viewModel.youthFixturesFlow.collectAsStateWithLifecycle()
    val scoutOffers by viewModel.scoutOffersFlow.collectAsStateWithLifecycle()
    val seniorYouthOffers by viewModel.seniorYouthOffersFlow.collectAsStateWithLifecycle()

    val isShowingYouthTraining by viewModel.isShowingYouthTrainingDialog.collectAsStateWithLifecycle()
    val isShowingYouthScout by viewModel.isShowingYouthScoutDialog.collectAsStateWithLifecycle()
    val isShowingSeniorYouthScout by viewModel.isShowingSeniorYouthScoutDialog.collectAsStateWithLifecycle()
    val latestSeasonSummary by viewModel.latestSeasonSummaryFlow.collectAsStateWithLifecycle()

    val hapticFeedback = LocalHapticFeedback.current
    val isHapticEnabled by viewModel.isHapticEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SportsDarkBg),
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Crest initials
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DarkSlate)
                                .border(1.dp, PitchGreen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = myClub?.name?.take(2)?.uppercase() ?: "FC",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = PitchGreen
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Name, Club
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = player.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = (if (player.age in 13..15) "Youth Acad. - " else "") + (myClub?.name ?: "No Club") + " • " + formatSeasonYear(gameState.currentSeason),
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Form & OVR Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Form indicator
                            val formIcon = when {
                                player.form >= 3 -> Icons.Default.ArrowUpward
                                player.form <= -3 -> Icons.Default.ArrowDownward
                                else -> Icons.Default.ArrowForward
                            }
                            val formColor = when {
                                player.form >= 3 -> PitchGreen
                                player.form <= -3 -> MutedRed
                                else -> TextSecondary
                            }
                            Icon(
                                imageVector = formIcon,
                                contentDescription = "Form Trend",
                                tint = formColor,
                                modifier = Modifier.size(16.dp)
                            )

                            // OVR display
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PitchGreen)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "OVR ${player.ovr}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.showSettings(true) },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = TextPrimary)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SportsNavBg,
                tonalElevation = 8.dp
            ) {
                val tabs = listOf(
                    Triple(GameTab.CALENDAR, Icons.Default.RssFeed, "Feed"),
                    Triple(GameTab.TROPHIES, Icons.Default.EmojiEvents, "Trophies"),
                    Triple(GameTab.HOME, Icons.Default.Home, "Home"),
                    Triple(GameTab.LEAGUE, Icons.Default.FormatListNumbered, "League"),
                    Triple(GameTab.CLUB, Icons.Default.Shield, "Club")
                )

                tabs.forEach { (tab, icon, label) ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            viewModel.showFamilyPage(false)
                            viewModel.setTab(tab)
                            if (isHapticEnabled) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                try {
                                    val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                                    if (vibrator != null && vibrator.hasVibrator()) {
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            vibrator.vibrate(android.os.VibrationEffect.createOneShot(15, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                                        } else {
                                            @Suppress("DEPRECATION")
                                            vibrator.vibrate(15)
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                        },
                        icon = { Icon(imageVector = icon, contentDescription = label) },
                        label = { Text(label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = PitchGreen,
                            indicatorColor = PitchGreen,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("tab_${label.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                GameTab.CALENDAR -> FeedAndCalendarTab(viewModel)
                GameTab.TROPHIES -> TrophiesTab(viewModel, player)
                GameTab.HOME -> {
                    if (isShowingFamilyPage) {
                        FamilyLegacyPage(
                            viewModel = viewModel,
                            onClose = { viewModel.showFamilyPage(false) }
                        )
                    } else if (player.careerPhase == com.example.data.PHASE_STREET) {
                        StreetFootballScreen(
                            player = player,
                            streetGames = streetGames,
                            scoutOffersCount = scoutOffers.size,
                            onAdvanceMonth = { viewModel.advanceMonth() },
                            onOpenTraining = { viewModel.openYouthTrainingDialog() },
                            onOpenScoutOffers = { viewModel.openYouthScoutDialog() }
                        )
                    } else {
                        HomeTab(viewModel, player, myClub, gameState)
                    }
                }
                GameTab.LEAGUE -> {
                    if (player.careerPhase == com.example.data.PHASE_STREET) {
                        StreetLeagueTab()
                    } else if (player.careerPhase == com.example.data.PHASE_YOUTH) {
                        YouthLeagueScreen(
                            player = player,
                            academies = youthAcademies,
                            standings = youthStandings,
                            fixtures = youthFixtures,
                            scoutOffersCount = seniorYouthOffers.size,
                            onAdvanceMonth = { viewModel.advanceMonth() },
                            onOpenTraining = { viewModel.openYouthTrainingDialog() },
                            onOpenScoutOffers = { viewModel.openSeniorYouthScoutDialog() },
                            onRequestProUpgrade = { viewModel.openSeniorYouthScoutDialog() }
                        )
                    } else {
                        LeagueTab(viewModel, player)
                    }
                }
                GameTab.CLUB -> {
                    if (player.careerPhase == com.example.data.PHASE_STREET) {
                        StreetClubTab(onOpenTraining = { viewModel.openYouthTrainingDialog() })
                    } else if (player.careerPhase == com.example.data.PHASE_YOUTH) {
                        YouthClubTab(
                            player = player,
                            academies = youthAcademies,
                            onOpenTraining = { viewModel.openYouthTrainingDialog() }
                        )
                    } else {
                        ClubTab(viewModel, player, myClub)
                    }
                }
            }
        }
    }

    // Dialogs
    if (isShowingYouthTraining) {
        YouthTrainingDialog(
            player = player,
            onCompleteTraining = { drill, quality -> viewModel.completeYouthTraining(drill, quality) },
            onDismiss = { viewModel.closeYouthTrainingDialog() }
        )
    }

    val scoutOfferMessage by viewModel.scoutOfferMessage.collectAsStateWithLifecycle()
    LaunchedEffect(scoutOfferMessage) {
        scoutOfferMessage?.let { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearScoutOfferMessage()
        }
    }

    LaunchedEffect(player.pendingNewManagerNotice, gameState.activeChoicePrompt) {
        if (player.pendingNewManagerNotice && gameState.activeChoicePrompt == null) {
            viewModel.triggerNewManagerFirstMeeting()
        }
    }

    if (isShowingYouthScout && scoutOffers.isNotEmpty()) {
        YouthScoutOfferDialog(
            offers = scoutOffers,
            onAccept = { offer -> viewModel.acceptYouthScoutOffer(offer) },
            onReject = { offer -> viewModel.rejectYouthScoutOffer(offer) },
            onDeclineAll = { viewModel.declineAllYouthScoutOffers() },
            onDismiss = { viewModel.closeYouthScoutDialog() }
        )
    }

    if (isShowingSeniorYouthScout && seniorYouthOffers.isNotEmpty()) {
        SeniorScoutOfferDialog(
            offers = seniorYouthOffers,
            onAccept = { offer -> viewModel.acceptSeniorYouthOffer(offer) },
            onReject = { offer -> viewModel.rejectSeniorYouthOffer(offer) },
            onDeclineAll = { viewModel.declineAllSeniorYouthOffers() },
            onDismiss = { viewModel.closeSeniorYouthScoutDialog() }
        )
    }

    // Dialogs
    if (selectedClubIdForProfile != null && selectedClubForProfile != null) {
        ClubProfileDialog(
            club = selectedClubForProfile!!,
            history = selectedClubHistory,
            trophies = selectedClubTrophies,
            records = selectedClubRecords,
            viewModel = viewModel,
            onDismiss = { viewModel.selectClubForProfile(null) }
        )
    }
    if (isShowingTransfer) {
        TransferOffersDialog(viewModel)
    }
    if (isShowingSettings && !isSimulatingSeason) {
        SettingsDialog(viewModel, gameState)
    }
    if (gameState.activeChoicePrompt != null && !isSimulatingSeason) {
        ChoiceEventDialog(viewModel, gameState)
    }
    if (latestSeasonSummary != null && !isSimulatingSeason) {
        SeasonSummaryDialog(
            summary = latestSeasonSummary!!,
            onDismiss = { viewModel.clearSeasonSummary() }
        )
    }
}
