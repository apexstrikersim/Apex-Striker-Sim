package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.dialogs.*
import com.example.ui.screens.*
import com.example.ui.tabs.*
import com.example.ui.theme.*

@Composable
fun MainGameScreen(viewModel: CareerViewModel) {
    val player by viewModel.playerFlow.collectAsStateWithLifecycle()
    val clubs by viewModel.clubsFlow.collectAsStateWithLifecycle()
    val gameState by viewModel.gameStateFlow.collectAsStateWithLifecycle()
    val screen by viewModel.activeScreen.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val isSimulatingSeason by viewModel.isSimulatingSeason.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SportsDarkBg)
    ) {
        when (screen) {
            Screen.MAIN_MENU -> MainMenuScreen(viewModel)
            Screen.SETUP -> SetupScreen(viewModel)
            Screen.INTRO -> ScoutedIntroScreen(viewModel, gameState)
            Screen.GAMEPLAY -> GameplayScreen(viewModel, player, clubs, gameState, selectedTab)
            Screen.RETIRED_SUMMARY -> RetiredSummaryScreen(viewModel, player)
            Screen.SON_SETUP -> SonSetupScreen(viewModel, player)
            Screen.MATCH_SCREEN -> MatchScreen(viewModel)
            Screen.YOUTH_CAREER_ENDED -> YouthCareerEndedScreen(viewModel, player)
        }

        if (isSimulatingSeason) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SportsDarkBg)
                    .clickable(enabled = true, onClick = {}),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = PitchGreen,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        val isShowingSaveSlotPicker by viewModel.isShowingSaveSlotPicker.collectAsStateWithLifecycle()
        val saveSlotPickerMode by viewModel.saveSlotPickerMode.collectAsStateWithLifecycle()
        val pendingTrophyAnimation by viewModel.pendingTrophyAnimation.collectAsStateWithLifecycle()

        if (isShowingSaveSlotPicker) {
            SaveSlotPickerDialog(
                mode = saveSlotPickerMode,
                saveSlotManager = viewModel.saveSlotManager,
                onSlotSelected = { slotId ->
                    viewModel.onSlotSelectedFromPicker(slotId)
                },
                onDismiss = {
                    viewModel.dismissSaveSlotPicker()
                }
            )
        }

        pendingTrophyAnimation?.let { trophy ->
            TrophyWinAnimation(
                trophy = trophy,
                onDismiss = {
                    viewModel.dismissTrophyAnimation()
                }
            )
        }
    }
}
