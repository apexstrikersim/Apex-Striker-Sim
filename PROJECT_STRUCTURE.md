# Apex Striker Career — Project Structure (v2.4.0)

This document outlines the modular directory layout and responsibilities of the files in `com.example.ui`.

## Directory Overview (`app/src/main/java/com/example/ui/`)

### 1. Root UI (`com.example.ui`)
- **`MainActivity.kt`**: Entry point Activity setting up edge-to-edge, view model, and root `MainGameScreen`.
- **`MainGameScreen.kt`**: Root Composable screen dispatcher handling navigation between screens, save slot picker, and trophy animations.
- **`MainMenuScreen.kt`**: First screen users see upon opening the game with "New Career", "Load Game", "Settings", and save slot selection.
- **`MatchScreen.kt`**: Interactive match engine screen with live commentary, striker decisions, and post-match summary.
- **`TrophyWinAnimation.kt`**: Full-screen celebration overlay when winning cup or league trophies.
- **`SaveSlotPickerDialog.kt`**: Save slot management picker for switching between 3 distinct career save slots.
- **`CareerViewModel.kt`**: Central ViewModel managing application state, state flows, coroutines, and Room database actions.

### 2. Screens (`com.example.ui.screens`)
- **`SetupScreen.kt`**: Character creation screen with name input, country selector, squad number, foot selection, background story, and back arrow navigation.
- **`ScoutedIntroScreen.kt`**: Cinematic intro screen displaying youth scout offers and academy onboarding.
- **`GameplayScreen.kt`**: Core hub screen hosting the top player bar, main tab bar (Home, Calendar, League, Club, Trophies), and active tab contents.
- **`RetiredSummaryScreen.kt`**: End-of-career hall-of-fame summary displaying peak stats, career trophies, and option to continue lineage with a son.
- **`SonSetupScreen.kt`**: Next generation setup screen to create a son inheriting father's legacy attributes.
- **`FamilyLegacyPage.kt`**: Family tree and lineage page showing all generations, historical season-by-season stats, OVR growth graph, and trophy cases.
- **`YouthCareerEndedScreen.kt`**: Game over screen if a youth player reaches age 19 without signing a senior professional deal.

### 3. Tabs (`com.example.ui.tabs`)
- **`HomeTab.kt`**: Central dashboard tab displaying current match day, season progress, social meters (Morale, Fan Rep, Manager Trust, Rival Rel), and quick actions.
- **`CalendarTab.kt`**: Full season fixture calendar with monthly views, match results, and simulation capabilities.
- **`LeagueTab.kt`**: League standings table, goalscorer charts, assist leaders, and match fixtures.
- **`StreetLeagueTab.kt`**: Unaffiliated grassroots street football view before joining an official academy.
- **`ClubTab.kt`**: Senior club management tab showing team squad, contract details, and training options.
- **`StreetClubTab.kt`**: Grassroots practice drills tab and Youth Academy view.
- **`TrophiesTab.kt`**: Cabinet displaying all unlocked domestic, international, and individual awards.

### 4. Dialogs (`com.example.ui.dialogs`)
- **`TransferOffersDialog.kt`**: Incoming transfer offers dialog and official contract signing ceremony with animated signature.
- **`SettingsDialog.kt`**: Options menu for audio volume, haptics, auto-save, manual save, save slot switching, career reset, and developer tools PIN unlock.
- **`ChoiceEventDialog.kt`**: High-stakes off-pitch narrative choice prompts affecting morale, trust, and fan reputation.
- **`TrainingDialog.kt`**: Interactive timing mini-game for attribute development and overtraining risk management.
- **`SeasonSummaryDialog.kt`**: End-of-season awards ceremony, Player of the Season trophy presentation, and season recap.

### 5. Components (`com.example.ui.components`)
- **`ButtonFeedbackAnimation.kt`**: Custom click feedback animations including `bounceClick` modifier and `AnimatedSaveButton` with press state, ripple effect, and confirmation animation.
- **`CountryFlagIcon.kt`**: Canvas vector flag renderer for England, Spain, France, Germany, Italy, and standard country badges.
- **`UiHelpers.kt`**: Shared UI helper utilities such as `getMonthName`, formatting helpers, and stat bar composables.
