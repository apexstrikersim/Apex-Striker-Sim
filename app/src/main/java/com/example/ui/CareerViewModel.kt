package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CareerRepository
import com.example.data.ClubEntity
import com.example.data.FixtureEntity
import com.example.data.GameStateEntity
import com.example.data.LegacyEntity
import com.example.data.PlayerEntity
import com.example.data.StandingEntity
import com.example.data.TrophyEntity
import com.example.data.TransferOffer
import com.example.data.FictionalData
import com.example.data.SaveSlotManager
import com.example.data.SlotMetadata
import kotlin.math.roundToInt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Screen {
    MAIN_MENU, SETUP, INTRO, GAMEPLAY, RETIRED_SUMMARY, SON_SETUP, MATCH_SCREEN, YOUTH_CAREER_ENDED
}

enum class GameTab {
    CALENDAR, TROPHIES, HOME, LEAGUE, CLUB
}

@OptIn(ExperimentalCoroutinesApi::class)
class CareerViewModel(application: Application) : AndroidViewModel(application) {

    val saveSlotManager = SaveSlotManager(application)

    private val _activeSlotId = MutableStateFlow(
        saveSlotManager.getLastActiveSlot().takeIf { it != 0 } ?: 1
    )
    val activeSlotId: StateFlow<Int> = _activeSlotId.asStateFlow()

    private var repository = CareerRepository(application, _activeSlotId.value)

    private val _hasActiveSave = MutableStateFlow(
        saveSlotManager.getLastActiveSlot() != 0 && saveSlotManager.getSlotMetadata(saveSlotManager.getLastActiveSlot()).hasData
    )
    val hasActiveSave: StateFlow<Boolean> = _hasActiveSave.asStateFlow()

    private val _isShowingSaveSlotPicker = MutableStateFlow(false)
    val isShowingSaveSlotPicker: StateFlow<Boolean> = _isShowingSaveSlotPicker.asStateFlow()

    private val _saveSlotPickerMode = MutableStateFlow("LOAD")
    val saveSlotPickerMode: StateFlow<String> = _saveSlotPickerMode.asStateFlow()

    private val _pendingTrophyAnimation = MutableStateFlow<TrophyEntity?>(null)
    val pendingTrophyAnimation: StateFlow<TrophyEntity?> = _pendingTrophyAnimation.asStateFlow()

    private val _playerFlow = MutableStateFlow<PlayerEntity?>(null)
    val playerFlow: StateFlow<PlayerEntity?> = _playerFlow.asStateFlow()

    private val _clubsFlow = MutableStateFlow<List<ClubEntity>>(emptyList())
    val clubsFlow: StateFlow<List<ClubEntity>> = _clubsFlow.asStateFlow()

    private val _gameStateFlow = MutableStateFlow<GameStateEntity?>(null)
    val gameStateFlow: StateFlow<GameStateEntity?> = _gameStateFlow.asStateFlow()

    private val _trophiesFlow = MutableStateFlow<List<TrophyEntity>>(emptyList())
    val trophiesFlow: StateFlow<List<TrophyEntity>> = _trophiesFlow.asStateFlow()

    private val _legaciesFlow = MutableStateFlow<List<LegacyEntity>>(emptyList())
    val legaciesFlow: StateFlow<List<LegacyEntity>> = _legaciesFlow.asStateFlow()

    private val _seasonRecordsFlow = MutableStateFlow<List<com.example.data.PlayerSeasonRecordEntity>>(emptyList())
    val seasonRecordsFlow: StateFlow<List<com.example.data.PlayerSeasonRecordEntity>> = _seasonRecordsFlow.asStateFlow()

    private val _fixturesFlow = MutableStateFlow<List<FixtureEntity>>(emptyList())
    val fixturesFlow: StateFlow<List<FixtureEntity>> = _fixturesFlow.asStateFlow()

    private val _youthAcademiesFlow = MutableStateFlow<List<com.example.data.YouthAcademyEntity>>(emptyList())
    val youthAcademiesFlow: StateFlow<List<com.example.data.YouthAcademyEntity>> = _youthAcademiesFlow.asStateFlow()

    private val _youthStandingsFlow = MutableStateFlow<List<com.example.data.YouthStandingEntity>>(emptyList())
    val youthStandingsFlow: StateFlow<List<com.example.data.YouthStandingEntity>> = _youthStandingsFlow.asStateFlow()

    private val _youthFixturesFlow = MutableStateFlow<List<com.example.data.YouthFixtureEntity>>(emptyList())
    val youthFixturesFlow: StateFlow<List<com.example.data.YouthFixtureEntity>> = _youthFixturesFlow.asStateFlow()

    private val _socialPostsFlow = MutableStateFlow<List<com.example.data.SocialPostEntity>>(emptyList())
    val socialPostsFlow: StateFlow<List<com.example.data.SocialPostEntity>> = _socialPostsFlow.asStateFlow()

    private val _isSocialFeedPlayerOnly = MutableStateFlow(false)
    val isSocialFeedPlayerOnly: StateFlow<Boolean> = _isSocialFeedPlayerOnly.asStateFlow()

    fun setSocialFeedPlayerOnly(only: Boolean) {
        _isSocialFeedPlayerOnly.value = only
    }

    private var currentJob: kotlinx.coroutines.Job? = null

    private fun reloadStateFromRepository() {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            repository.onTrophyUnlockedListener = { trophy -> triggerTrophyAnimation(trophy) }
            repository.migrateClubRecordsIfNeeded()
            launch { repository.playerFlow.collect { _playerFlow.value = it; syncSlotMetadata() } }
            launch { repository.allClubsFlow.collect { _clubsFlow.value = it } }
            launch { repository.gameStateFlow.collect { _gameStateFlow.value = it } }
            launch { repository.allTrophiesFlow.collect { _trophiesFlow.value = it } }
            launch { repository.allLegaciesFlow.collect { _legaciesFlow.value = it } }
            launch { repository.allSeasonRecordsFlow.collect { _seasonRecordsFlow.value = it } }
            launch { repository.allFixturesFlow.collect { _fixturesFlow.value = it } }
            launch { repository.youthAcademiesFlow.collect { _youthAcademiesFlow.value = it } }
            launch { repository.youthStandingsFlow.collect { _youthStandingsFlow.value = it } }
            launch { repository.youthFixturesFlow.collect { _youthFixturesFlow.value = it } }
            launch { repository.allSocialPostsFlow.collect { _socialPostsFlow.value = it } }
        }
    }

    fun submitSocialPostReply(post: com.example.data.SocialPostEntity, replyIndex: Int, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.submitSocialPostReply(post, replyIndex)
            onResult(result)
        }
    }

    private val prefs = application.getSharedPreferences("audio_settings", android.content.Context.MODE_PRIVATE)

    private val _isMuted = MutableStateFlow(prefs.getBoolean("is_muted", false))
    val isMuted = _isMuted.asStateFlow()

    private val _isHapticEnabled = MutableStateFlow(prefs.getBoolean("is_haptic", true))
    val isHapticEnabled = _isHapticEnabled.asStateFlow()

    private val _audioVolume = MutableStateFlow(prefs.getFloat("audio_volume", 0.8f))
    val audioVolume = _audioVolume.asStateFlow()

    private val _scoutOfferMessage = MutableStateFlow<String?>(null)
    val scoutOfferMessage: StateFlow<String?> = _scoutOfferMessage.asStateFlow()

    fun clearScoutOfferMessage() {
        _scoutOfferMessage.value = null
    }

    init {
        reloadStateFromRepository()
    }

    fun dismissTrophyAnimation() {
        _pendingTrophyAnimation.value = null
    }

    fun triggerTrophyAnimation(trophy: TrophyEntity) {
        _pendingTrophyAnimation.value = trophy
    }

    fun syncSlotMetadata() {
        val player = _playerFlow.value ?: return
        val slotId = _activeSlotId.value
        val clubName = _clubsFlow.value.find { it.id == player.currentClubId }?.name ?: "Free Agent"
        saveSlotManager.updateSlotMetadata(
            slotId = slotId,
            playerName = player.name,
            generation = player.generation,
            ovr = player.ovr,
            clubName = clubName
        )
        saveSlotManager.setLastActiveSlot(slotId)
        _hasActiveSave.value = true
    }

    fun switchToSlot(slotId: Int) {
        _activeSlotId.value = slotId
        repository = CareerRepository(getApplication(), slotId)
        saveSlotManager.setLastActiveSlot(slotId)
        reloadStateFromRepository()
    }

    fun startNewCareerFlow() {
        _saveSlotPickerMode.value = "NEW"
        _isShowingSaveSlotPicker.value = true
    }

    fun continueLastCareer() {
        val lastSlot = saveSlotManager.getLastActiveSlot()
        val slotToUse = if (lastSlot != 0 && saveSlotManager.getSlotMetadata(lastSlot).hasData) lastSlot else 1
        switchToSlot(slotToUse)
        viewModelScope.launch {
            kotlinx.coroutines.delay(100)
            val p = repository.getPlayerSync()
            if (p != null) {
                _activeScreen.value = Screen.GAMEPLAY
            } else {
                _activeScreen.value = Screen.SETUP
            }
        }
    }

    fun openLoadSlotPicker() {
        _saveSlotPickerMode.value = "LOAD"
        _isShowingSaveSlotPicker.value = true
    }

    fun openSaveSlotPicker() {
        _saveSlotPickerMode.value = "SAVE"
        _isShowingSaveSlotPicker.value = true
    }

    fun dismissSaveSlotPicker() {
        _isShowingSaveSlotPicker.value = false
    }

    fun onSlotSelectedFromPicker(slotId: Int) {
        val mode = _saveSlotPickerMode.value
        dismissSaveSlotPicker()
        switchToSlot(slotId)
        when (mode) {
            "NEW" -> {
                viewModelScope.launch {
                    repository.clearAllData()
                    _activeScreen.value = Screen.SETUP
                }
            }
            "LOAD" -> {
                viewModelScope.launch {
                    kotlinx.coroutines.delay(100)
                    val p = repository.getPlayerSync()
                    if (p != null) {
                        _activeScreen.value = Screen.GAMEPLAY
                    } else {
                        _activeScreen.value = Screen.SETUP
                    }
                }
            }
            "SAVE" -> {
                syncSlotMetadata()
            }
        }
    }

    fun returnToMainMenu() {
        _isShowingSettingsDialog.value = false
        _activeScreen.value = Screen.MAIN_MENU
    }

    fun toggleMute(muted: Boolean) {
        _isMuted.value = muted
        prefs.edit().putBoolean("is_muted", muted).apply()
    }

    fun toggleHaptic(enabled: Boolean) {
        _isHapticEnabled.value = enabled
        prefs.edit().putBoolean("is_haptic", enabled).apply()
    }

    fun setAudioVolume(vol: Float) {
        _audioVolume.value = vol
        prefs.edit().putFloat("audio_volume", vol).apply()
    }

    fun playWhistleSound(pattern: String) {
        if (_isMuted.value) return
        viewModelScope.launch {
            WhistlePlayer.playWhistle(getApplication(), pattern, _audioVolume.value)
        }
    }

    val latestSeasonSummaryFlow: StateFlow<com.example.data.SeasonSummaryData?> = repository.latestSeasonSummaryFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun clearSeasonSummary() {
        repository.clearSeasonSummary()
    }

    // UI Local state flows
    private val _activeScreen = MutableStateFlow(Screen.MAIN_MENU)
    val activeScreen = _activeScreen.asStateFlow()

    private val _activeMatch = MutableStateFlow<FixtureEntity?>(null)
    val activeMatch = _activeMatch.asStateFlow()

    private val _playerCameOnMinute = MutableStateFlow<Int?>(null)
    val playerCameOnMinute = _playerCameOnMinute.asStateFlow()

    private val _selectedTab = MutableStateFlow(GameTab.HOME)
    val selectedTab = _selectedTab.asStateFlow()

    private val _selectedStandingCountry = MutableStateFlow("England")
    val selectedStandingCountry = _selectedStandingCountry.asStateFlow()

    private val _currentStandings = MutableStateFlow<List<StandingEntity>>(emptyList())
    val currentStandings = _currentStandings.asStateFlow()

    private val _transferOffers = MutableStateFlow<List<TransferOffer>>(emptyList())
    val transferOffers = _transferOffers.asStateFlow()

    private val _isShowingTransferDialog = MutableStateFlow(false)
    val isShowingTransferDialog = _isShowingTransferDialog.asStateFlow()

    private val _isShowingFamilyPage = MutableStateFlow(false)
    val isShowingFamilyPage = _isShowingFamilyPage.asStateFlow()

    private val _isShowingSettingsDialog = MutableStateFlow(false)
    val isShowingSettingsDialog = _isShowingSettingsDialog.asStateFlow()

    private val _selectedClubIdForProfile = MutableStateFlow<Int?>(null)
    val selectedClubIdForProfile = _selectedClubIdForProfile.asStateFlow()

    val selectedClubForProfileFlow = combine(_selectedClubIdForProfile, clubsFlow) { id, clubs ->
        clubs.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedClubHistoryFlow = _selectedClubIdForProfile.flatMapLatest { id ->
        if (id == null) kotlinx.coroutines.flow.flowOf(emptyList())
        else repository.getHistoryForClubFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedClubTrophiesFlow = selectedClubForProfileFlow.flatMapLatest { club ->
        if (club == null) kotlinx.coroutines.flow.flowOf(emptyList())
        else repository.getTrophiesByClubFlow(club.name)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedClubRecordsFlow = _selectedClubIdForProfile.flatMapLatest { id ->
        if (id == null) kotlinx.coroutines.flow.flowOf(emptyList())
        else repository.getRecordsForClubFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val streetGamesFlow: StateFlow<List<com.example.data.StreetFootballGameEntity>> = combine(gameStateFlow) { (gameState) ->
        gameState?.currentSeason ?: 1
    }.flatMapLatest { season ->
        repository.getStreetFootballGamesFlow(season)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scoutOffersFlow: StateFlow<List<com.example.data.YouthScoutOffer>> = gameStateFlow.combine(playerFlow) { gameState, player ->
        val raw = gameState?.persistedYouthOffers
        if (!raw.isNullOrEmpty()) {
            com.example.data.YouthCareerLogic.deserializeYouthOffers(raw)
        } else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val seniorYouthOffersFlow: StateFlow<List<com.example.data.YouthToSeniorOffer>> = gameStateFlow.combine(playerFlow) { gameState, player ->
        val raw = gameState?.persistedSeniorYouthOffers
        if (!raw.isNullOrEmpty()) {
            com.example.data.YouthCareerLogic.deserializeSeniorYouthOffers(raw)
        } else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isShowingYouthTrainingDialog = MutableStateFlow(false)
    val isShowingYouthTrainingDialog = _isShowingYouthTrainingDialog.asStateFlow()

    private val _isShowingYouthScoutDialog = MutableStateFlow(false)
    val isShowingYouthScoutDialog = _isShowingYouthScoutDialog.asStateFlow()

    private val _isShowingSeniorYouthScoutDialog = MutableStateFlow(false)
    val isShowingSeniorYouthScoutDialog = _isShowingSeniorYouthScoutDialog.asStateFlow()

    fun openYouthTrainingDialog() { _isShowingYouthTrainingDialog.value = true }
    fun closeYouthTrainingDialog() { _isShowingYouthTrainingDialog.value = false }

    fun openYouthScoutDialog() { _isShowingYouthScoutDialog.value = true }
    fun closeYouthScoutDialog() { _isShowingYouthScoutDialog.value = false }

    fun openSeniorYouthScoutDialog() { _isShowingSeniorYouthScoutDialog.value = true }
    fun closeSeniorYouthScoutDialog() { _isShowingSeniorYouthScoutDialog.value = false }

    fun acceptYouthScoutOffer(offer: com.example.data.YouthScoutOffer) {
        viewModelScope.launch {
            repository.acceptYouthScoutOffer(offer.academyId)
            closeYouthScoutDialog()
        }
    }

    fun rejectYouthScoutOffer(offer: com.example.data.YouthScoutOffer) {
        viewModelScope.launch {
            repository.rejectYouthScoutOffer(offer.academyId)
        }
    }

    fun declineAllYouthScoutOffers() {
        viewModelScope.launch {
            repository.declineAllYouthScoutOffers()
            closeYouthScoutDialog()
        }
    }

    fun acceptSeniorYouthOffer(offer: com.example.data.YouthToSeniorOffer) {
        viewModelScope.launch {
            val error = repository.acceptSeniorYouthOffer(offer.clubId, offer.contractYears, offer.targetGplusA)
            if (error != null) {
                _scoutOfferMessage.value = error
            } else {
                closeSeniorYouthScoutDialog()
            }
        }
    }

    fun rejectSeniorYouthOffer(offer: com.example.data.YouthToSeniorOffer) {
        viewModelScope.launch {
            repository.rejectSeniorYouthOffer(offer.clubId)
        }
    }

    fun declineAllSeniorYouthOffers() {
        viewModelScope.launch {
            repository.declineAllSeniorYouthOffers()
            closeSeniorYouthScoutDialog()
        }
    }

    fun completeYouthTraining(drillType: YouthDrillType, qualityScore: Float) {
        viewModelScope.launch {
            val player = playerFlow.value ?: return@launch
            if (player.hasTrainedThisMonth == 1) return@launch
            val rawBonus = 0.08f + (qualityScore * 0.62f) // 0.08 to 0.7 before damping
            when (drillType) {
                YouthDrillType.SHOOTING -> {
                    player.finishingTrainingBonus += rawBonus * repository.proximityDamping(player.finishing, player.potentialCeiling)
                    player.shootingDrillsCompleted += 1
                }
                YouthDrillType.PASSING -> {
                    player.passingTrainingBonus += rawBonus * repository.proximityDamping(player.passing, player.potentialCeiling)
                    player.passingDrillsCompleted += 1
                }
                YouthDrillType.PACE -> {
                    player.paceTrainingBonus += rawBonus * repository.proximityDamping(player.pace, player.potentialCeiling)
                    player.paceDrillsCompleted += 1
                }
                YouthDrillType.TECHNICAL -> {
                    player.techniqueTrainingBonus += rawBonus * repository.proximityDamping(player.technique, player.potentialCeiling)
                    player.technicalDrillsCompleted += 1
                }
                YouthDrillType.PHYSICAL -> {
                    player.physicalTrainingBonus += rawBonus * repository.proximityDamping(player.physical, player.potentialCeiling)
                    player.physicalDrillsCompleted += 1
                }
            }
            // Check bonus integer levels
            if (player.finishingTrainingBonus >= 1.0f) {
                val inc = player.finishingTrainingBonus.toInt()
                player.finishing = (player.finishing + inc).coerceIn(1, player.potentialCeiling)
                player.finishingTrainingBonus -= inc
            }
            if (player.passingTrainingBonus >= 1.0f) {
                val inc = player.passingTrainingBonus.toInt()
                player.passing = (player.passing + inc).coerceIn(1, player.potentialCeiling)
                player.passingTrainingBonus -= inc
            }
            if (player.paceTrainingBonus >= 1.0f) {
                val inc = player.paceTrainingBonus.toInt()
                player.pace = (player.pace + inc).coerceIn(1, player.potentialCeiling)
                player.paceTrainingBonus -= inc
            }
            if (player.techniqueTrainingBonus >= 1.0f) {
                val inc = player.techniqueTrainingBonus.toInt()
                player.technique = (player.technique + inc).coerceIn(1, player.potentialCeiling)
                player.techniqueTrainingBonus -= inc
            }
            if (player.physicalTrainingBonus >= 1.0f) {
                val inc = player.physicalTrainingBonus.toInt()
                player.physical = (player.physical + inc).coerceIn(1, player.potentialCeiling)
                player.physicalTrainingBonus -= inc
            }
            player.ovr = repository.calculateOvr(player.finishing, player.pace, player.passing, player.physical, player.technique).coerceAtMost(player.potentialCeiling)
            player.hasTrainedThisMonth = 1
            repository.updatePlayer(player)
            closeYouthTrainingDialog()
        }
    }

    fun dismissYouthCareerEnded() {
        viewModelScope.launch {
            repository.resetGame()
            _activeScreen.value = Screen.SETUP
            _selectedTab.value = GameTab.HOME
        }
    }

    fun selectClubForProfile(clubId: Int?) {
        _selectedClubIdForProfile.value = clubId
    }

    private val _isAdvancing = MutableStateFlow(false)
    val isAdvancing = _isAdvancing.asStateFlow()

    private val _isSimulatingSeason = MutableStateFlow(false)
    val isSimulatingSeason = _isSimulatingSeason.asStateFlow()

    private val _isQuickSimCompleted = MutableStateFlow(false)
    val isQuickSimCompleted = _isQuickSimCompleted.asStateFlow()

    init {
        // Observe player to update standing country if empty
        viewModelScope.launch {
            playerFlow.collect { p ->
                if (p != null && _selectedStandingCountry.value.isEmpty()) {
                    _selectedStandingCountry.value = p.academyCountry
                }
            }
        }

        // Fetch standings for selected country
        viewModelScope.launch {
            _selectedStandingCountry
                .flatMapLatest { country -> repository.getStandingsFlow(country) }
                .collect { list ->
                    _currentStandings.value = list
                }
        }
    }

    /**
     * Set the active screen
     */
    fun setScreen(screen: Screen) {
        _activeScreen.value = screen
    }

    /**
     * Set the active bottom tab
     */
    fun setTab(tab: GameTab) {
        _selectedTab.value = tab
    }

    /**
     * Change viewed standings country
     */
    fun selectStandingCountry(country: String) {
        _selectedStandingCountry.value = country
    }

    /**
     * Start a fresh game career
     */
    fun createCharacter(
        name: String,
        birth: String,
        academy: String,
        preferredFoot: String = "Right",
        squadNumber: Int = 9,
        backgroundStory: String = "Street Cages"
    ) {
        viewModelScope.launch {
            _activeMatch.value = null
            _isQuickSimCompleted.value = false
            repository.startNewCareer(
                playerName = name,
                birthCountry = birth,
                academyCountry = academy,
                isSon = false,
                preferredFoot = preferredFoot,
                squadNumber = squadNumber,
                backgroundStory = backgroundStory
            )
            _selectedStandingCountry.value = academy
            _activeScreen.value = Screen.GAMEPLAY
        }
    }

    /**
     * Start son's career after retirement
     */
    fun createSon(sonName: String) {
        val father = playerFlow.value ?: return
        viewModelScope.launch {
            _activeMatch.value = null
            _isQuickSimCompleted.value = false
            val legacies = legaciesFlow.value
            val fatherLegacy = legacies.lastOrNull { it.generation == father.generation }
            val weight = fatherLegacy?.totalTrophiesWeight ?: 0

            repository.startNewCareer(
                playerName = sonName,
                birthCountry = father.academyCountry, // Son is born where father played
                academyCountry = father.academyCountry,
                isSon = true,
                fatherFinalOvr = father.ovr,
                fatherTrophiesWeight = weight,
                fatherPotentialCeiling = father.potentialCeiling
            )
            _selectedStandingCountry.value = father.academyCountry
            _activeScreen.value = Screen.GAMEPLAY
        }
    }

    /**
     * Skip scouted narrative intro to gameplay
     */
    fun skipIntro() {
        _activeScreen.value = Screen.GAMEPLAY
    }

    /**
     * Advance Time by One Month / Play Matches step-by-step
     */
    fun advanceMonth() {
        if (_isAdvancing.value) return
        viewModelScope.launch {
            _isAdvancing.value = true
            val player = playerFlow.value
            val gameState = gameStateFlow.value
            if (player != null && gameState != null) {
                if (player.careerPhase == com.example.data.PHASE_STREET || player.careerPhase == com.example.data.PHASE_YOUTH) {
                    repository.advanceMonth()
                    _isAdvancing.value = false
                    return@launch
                }

                val currentMonth = gameState.currentMonthIndex
                val playerClubId = player.currentClubId
                var nextMatch = repository.getNextPlayerMatchInMonth(currentMonth, playerClubId)
                
                while (nextMatch != null) {
                    val rotation = repository.calculateRotationForFixture(nextMatch, player, currentMonth)
                    _activeMatch.value = nextMatch
                    _playerCameOnMinute.value = if (rotation.isBenched) null else rotation.playerCameOnMinute
                    _isQuickSimCompleted.value = false
                    _activeScreen.value = Screen.MATCH_SCREEN
                    _isAdvancing.value = false
                    return@launch
                }
                
                // All player matches for this month have been simulated!
                // Run other teams' matches, progression, and monthly choice events
                repository.simulateRemainingMonth()
            }
            _isAdvancing.value = false
        }
    }

    /**
     * Quick Sim the currently active match and show simulated summary
     */
    fun quickSimCurrentMatch() {
        val fixture = _activeMatch.value ?: return
        val minute = _playerCameOnMinute.value
        viewModelScope.launch {
            if (minute == null) {
                val player = playerFlow.value
                if (player != null) {
                    val currentMonth = gameStateFlow.value?.currentMonthIndex ?: 0
                    val rotation = repository.calculateRotationForFixture(fixture, player, currentMonth)
                    repository.autoSimulateBenchedMatch(fixture, player, rotation)
                }
            } else {
                repository.quickSimPlayingMatch(fixture, minute)
            }
            // Re-assign a copy of fixture to trigger state collection & UI recomposition with new values
            _activeMatch.value = fixture.copy()
            _isQuickSimCompleted.value = true
        }
    }

    /**
     * Dismiss the quick sim summary screen and proceed to next matches
     */
    fun dismissQuickSimSummary() {
        _activeScreen.value = Screen.GAMEPLAY
        _activeMatch.value = null
        _playerCameOnMinute.value = null
        _isQuickSimCompleted.value = false
        advanceMonth()
    }

    /**
     * Resolve manually played match and return to gameplay
     */
    fun resolvePlayedMatch(
        playerGoals: Int,
        playerAssists: Int,
        finalHomeScore: Int,
        finalAwayScore: Int,
        minutesPlayed: Int,
        matchRating: Float,
        goalMinutes: String? = null,
        assistMinutes: String? = null
    ) {
        val fixture = _activeMatch.value ?: return
        val minute = _playerCameOnMinute.value
        viewModelScope.launch {
            if (minute == null) {
                val player = playerFlow.value
                if (player != null) {
                    val currentMonth = gameStateFlow.value?.currentMonthIndex ?: 0
                    val rotation = repository.calculateRotationForFixture(fixture, player, currentMonth)
                    repository.autoSimulateBenchedMatch(fixture, player, rotation)
                }
            } else {
                repository.resolvePlayedMatch(
                    fixture,
                    minute,
                    playerGoals,
                    playerAssists,
                    finalHomeScore,
                    finalAwayScore,
                    minutesPlayed,
                    matchRating,
                    goalMinutes,
                    assistMinutes
                )
            }
            _activeScreen.value = Screen.GAMEPLAY
            _activeMatch.value = null
            _playerCameOnMinute.value = null
            // Check if there are more matches in the month
            advanceMonth()
        }
    }

    /**
     * Train the player with a selected focus and grade
     */
    fun trainPlayer(focus: String, grade: String) {
        viewModelScope.launch {
            repository.trainPlayer(focus, grade)
        }
    }

    /**
     * Request transfer offers during windows (Months: 0, 5, 9)
     */
    fun checkTransferOffers() {
        val p = playerFlow.value ?: return
        viewModelScope.launch {
            val offers = repository.getTransferOffers(p)
            _transferOffers.value = offers
            _isShowingTransferDialog.value = true
        }
    }

    /**
     * Complete a club transfer
     */
    fun acceptTransfer(offer: TransferOffer) {
        val p = playerFlow.value ?: return
        viewModelScope.launch {
            repository.completeTransfer(p, offer)
            _isShowingTransferDialog.value = false
            _selectedTab.value = GameTab.HOME
        }
    }

    fun rejectTransferOffer(offer: TransferOffer) {
        _transferOffers.value = _transferOffers.value.filter { it.clubId != offer.clubId }
    }

    fun dismissTransferDialog() {
        _isShowingTransferDialog.value = false
    }

    /**
     * Show/Hide Dialogs
     */
    fun showFamilyPage(show: Boolean) {
        _isShowingFamilyPage.value = show
    }

    fun showSettings(show: Boolean) {
        _isShowingSettingsDialog.value = show
    }

    /**
     * Retire player manually
     */
    fun retirePlayer() {
        val p = playerFlow.value ?: return
        viewModelScope.launch {
            repository.retirePlayerAndSaveLegacy(p)
            _activeScreen.value = Screen.RETIRED_SUMMARY
        }
    }

    /**
     * Complete retirement review and transition to Son selection
     */
    fun proceedToLegacy() {
        _activeScreen.value = Screen.SON_SETUP
    }

    /**
     * Resolve interactive player Choice Event
     */
    fun resolveChoice(optionIndex: Int) {
        viewModelScope.launch {
            repository.resolveChoice(optionIndex)
        }
    }

    /**
     * Restart/Reset all data
     */
    fun resetGame() {
        viewModelScope.launch {
            repository.resetGame()
            _activeScreen.value = Screen.SETUP
            _selectedTab.value = GameTab.HOME
            _isShowingSettingsDialog.value = false
        }
    }

    // Developer mode controls
    fun devUpdateStats(fin: Int, pac: Int, pas: Int, phy: Int, tech: Int) {
        viewModelScope.launch {
            repository.devSetStats(fin, pac, pas, phy, tech)
        }
    }

    fun devUpdateSocialStats(morale: Int, fanRep: Int, managerTrust: Int, rivalRel: Int) {
        viewModelScope.launch {
            repository.devUpdateSocialStats(morale, fanRep, managerTrust, rivalRel)
        }
    }

    fun requestManagerTalk() {
        viewModelScope.launch {
            repository.requestManagerTalk()
        }
    }

    suspend fun getActiveManagerForClub(clubId: Int): com.example.data.NpcManagerEntity? {
        return repository.getActiveManagerForClub(clubId)
    }

    fun triggerNewManagerFirstMeeting() {
        viewModelScope.launch {
            repository.triggerNewManagerFirstMeeting()
        }
    }

    fun devSetClubRep(clubId: Int, repTier: String) {
        viewModelScope.launch {
            repository.devSetClubRep(clubId, repTier)
        }
    }

    fun toggleAutoSave(enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleAutoSave(enabled)
        }
    }

    fun setDevMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDevMode(enabled)
        }
    }

    fun activateGodMode() {
        viewModelScope.launch {
            repository.activateGodMode()
        }
    }

    fun devSimulateSeason(count: Int = 1) {
        viewModelScope.launch {
            _isAdvancing.value = true
            _isSimulatingSeason.value = true
            try {
                repository.devSimulateSeasons(count)
            } finally {
                _isAdvancing.value = false
                _isSimulatingSeason.value = false
                _isShowingSettingsDialog.value = false
            }
        }
    }
}
