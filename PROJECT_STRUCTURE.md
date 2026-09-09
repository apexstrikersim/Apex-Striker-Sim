# Apex Striker Career — Project Structure (v2.8.0)

This document outlines the modular directory layout and responsibilities of the files in `com.example.ui`.

## Directory Overview (`app/src/main/java/com/example/ui/`)

### 1. Root UI (`com.example.ui`)
- **`MainActivity.kt`**: Entry point Activity setting up edge-to-edge, view model, and root `MainGameScreen`.
- **`MainGameScreen.kt`**: Root Composable screen dispatcher handling navigation between screens, save slot picker, and trophy animations.
- **`MainMenuScreen.kt`**: First screen users see upon opening the game with "New Career", "Load Game", "Settings", and save slot selection.
- **`MatchScreen.kt`**: Interactive match engine screen with live commentary, striker decisions, authentic referee whistles, and post-match summary.
- **`SocialMediaScreen.kt`**: In-game social media feed and interactive public/press replies influencing morale, manager trust, and fan reputation.
- **`StreetFootballScreen.kt`**: Grassroots cage match simulator and narrative encounters for the early street football phase.
- **`YouthLeagueScreen.kt`**: Dedicated youth league standings, fixture results, and academy match simulator.
- **`TrophyWinAnimation.kt`**: Full-screen celebration overlay when winning cup or league trophies.
- **`SaveSlotPickerDialog.kt`**: Save slot management picker for switching between 3 distinct career save slots.
- **`CareerViewModel.kt`**: Central ViewModel managing application state, state flows, coroutines, and Room database actions.
- **`WhistlePlayer.kt`**: Audio player handling match referee whistle sound effects (kick-off, half-time, full-time).
- **`YouthTrainingDialog.kt`**: Interactive canvas-based youth training mini-games covering finishing, passing, pace, technique, and physical attributes.
- **`ClubProfileDialog.kt`**: Club details dialog showing squad, stadium, history, and rival striker info.
- **`ScoutOfferDialog.kt`**: Scout contract presentation and negotiation dialog.
- **`LegalTextDialog.kt`**: In-game terms and privacy notice dialog.

### 2. Screens (`com.example.ui.screens`)
- **`SetupScreen.kt`**: Character creation screen with name input, nationality selector, squad number, foot selection, background story, and randomized profile features.
- **`ScoutedIntroScreen.kt`**: Cinematic intro screen displaying youth scout offers and academy onboarding.
- **`GameplayScreen.kt`**: Core hub screen hosting the top player bar, main tab bar (Home, Calendar, League, Club, Trophies), and active tab contents.
- **`RetiredSummaryScreen.kt`**: End-of-career hall-of-fame summary displaying peak stats, career trophies, and option to continue lineage with a son.
- **`SonSetupScreen.kt`**: Next generation setup screen to create a son inheriting father's legacy attributes and potential ceiling.
- **`FamilyLegacyPage.kt`**: Family tree and lineage page showing all generations, historical season-by-season stats, bounded OVR progression chart, and trophy cases.
- **`YouthCareerEndedScreen.kt`**: Game over screen if a youth player reaches age 19 without signing a senior professional deal.

### 3. Tabs (`com.example.ui.tabs`)
- **`HomeTab.kt`**: Central dashboard tab displaying current match day, season progress, social meters (Morale, Fan Rep, Manager Trust, Rival Rel), next fixture preview, and quick actions.
- **`CalendarTab.kt`**: Full season fixture calendar with monthly views, match results, and simulation capabilities.
- **`FeedAndCalendarTab.kt`**: Unified tab combining monthly calendar schedule with social media newsfeed tabs.
- **`LeagueTab.kt`**: League standings table, goalscorer charts, assist leaders, and match fixtures.
- **`StreetLeagueTab.kt`**: Unaffiliated grassroots street football view before joining an official academy.
- **`ClubTab.kt`**: Senior club management tab showing team squad, contract details, and training options.
- **`StreetClubTab.kt`**: Grassroots practice drills tab and Youth Academy view.
- **`TrophiesTab.kt`**: Cabinet displaying all unlocked domestic, international, and individual awards.

### 4. Dialogs (`com.example.ui.dialogs`)
- **`TransferOffersDialog.kt`**: Incoming transfer offers dialog and official contract signing ceremony with animated signature.
- **`SettingsDialog.kt`**: Options menu for audio volume, haptics, auto-save, manual save, save slot switching, career reset, and developer tools PIN unlock.
- **`ChoiceEventDialog.kt`**: High-stakes off-pitch narrative choice prompts affecting morale, trust, and fan reputation.
- **`TrainingDialog.kt`**: Senior interactive training mini-game for attribute development and overtraining risk management.
- **`SeasonSummaryDialog.kt`**: End-of-season awards ceremony, Player of the Season trophy presentation, and season recap.

### 5. Components (`com.example.ui.components`)
- **`ButtonFeedbackAnimation.kt`**: Custom click feedback animations including `bounceClick` modifier and `AnimatedSaveButton` with press state, ripple effect, and confirmation animation.
- **`ClubCrestIcon.kt`**: Clean geometric badge renderer displaying club shapes, patterns, emblem symbols, and club-specific color palettes without text clutter.
- **`CountryFlagIcon.kt`**: Canvas vector flag renderer for domestic and international nationalities.
- **`JerseyNumberIcon.kt`**: Custom squad jersey number icon renderer.
- **`PlayerFaceIcon.kt`**: Procedural avatar generator with natural curved mustache styles, tapered beard coverage, skin tones, and hairstyles.
- **`TacticalPitchBackground.kt`**: Canvas tactical pitch background illustration with pitch markings.
- **`TrainingDrillBackgrounds.kt`**: Decorative canvas backgrounds for training drill activities.
- **`BadgeIcon.kt`**: Tier and achievement badge icon composable.
- **`LoadingTips.kt`**: Rotating gameplay tips and trivia during transitions.
- **`RepeatingStepperButton.kt`**: Hold-to-repeat numeric stepper controls for responsive attribute allocation.
- **`StatItem.kt`**: Reusable attribute progress bar and numerical stat indicator.
- **`UiHelpers.kt`**: Shared UI helper utilities such as `getMonthName`, formatting helpers, and stat bar composables.
