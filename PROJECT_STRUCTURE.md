# Apex Striker Career — Project Structure (v2.9.0)

This document outlines the modular directory layout and responsibilities of the files in `com.example`.

## Directory Overview

### 1. Data Layer (`app/src/main/java/com/example/data/`)
- **`AppDatabase.kt`**: Room database definition (Schema v2) managing isolated database files for each save slot (`apex_career_slot_<id>.db`) with migration and stale database reconciliation.
- **`CareerRepository.kt`**: Central simulation and business logic repository. Handles match simulation, 12-month calendar progression, transfer offers, national team call-up checks, quadrennial World Cup tournament engine, independent per-skill ceiling clamping, and trophy awards.
- **`NationData.kt`**: 200 global nations with tier ratings (TOP, HIGH, MID, LOW), ranking points, call-up thresholds, ISO-2 mappings, and Unicode flag emoji resolvers.
- **`PlayerEntity.kt`**: Core player data entity tracking attributes, independent skill ceilings (`finishingCeiling`, `paceCeiling`, `passingCeiling`, `physicalCeiling`, `techniqueCeiling`), form, fatigue, national team caps, and contract details.
- **`GameStateEntity.kt`**: Career state entity tracking current season, month (0–11), transfer windows, national call-up state, and World Cup historical champions.
- **`ClubEntity.kt` / `FixtureEntity.kt` / `TrophyEntity.kt`**: Data entities for domestic/European clubs, scheduled fixtures, and career accolades.
- **`FictionalData.kt`**: Fictional domestic league structures, team names, tactical profiles, and player background narratives.
- **`AudioSettings.kt` / `SaveSlotManager.kt`**: Local preference managers for audio volume, sound effects, and save slot metadata.

### 2. Root UI (`app/src/main/java/com/example/ui/`)
- **`MainActivity.kt`**: Entry point Activity setting up edge-to-edge display, view model injection, and root `MainGameScreen`.
- **`MainGameScreen.kt`**: Root Composable screen dispatcher handling navigation between screens, save slot picker, and trophy animations.
- **`MainMenuScreen.kt`**: Start screen with "New Career", "Load Game", "Settings", and save slot selector.
- **`MatchScreen.kt`**: Interactive match engine screen with live commentary, striker decisions, authentic referee whistles, and post-match summary.
- **`SocialMediaScreen.kt`**: In-game social media feed and interactive public/press replies influencing morale, manager trust, and fan reputation.
- **`StreetFootballScreen.kt`**: Grassroots cage match simulator and narrative encounters for the early street football phase.
- **`YouthLeagueScreen.kt`**: Dedicated youth league standings, fixture results, and academy match simulator.
- **`TrophyWinAnimation.kt`**: Full-screen celebration overlay when winning cup, league, or international trophies.
- **`SaveSlotPickerDialog.kt`**: Save slot management picker for switching between 3 distinct career save slots.
- **`CareerViewModel.kt`**: Central ViewModel exposing UI state flows, managing coroutines, and orchestrating Room database actions.
- **`WhistlePlayer.kt`**: Audio player handling match referee whistle sound effects (kick-off, half-time, full-time).
- **`YouthTrainingDialog.kt`**: Interactive youth training mini-games covering finishing, passing, pace, technique, and physical attributes.
- **`ClubProfileDialog.kt`**: Club details dialog showing squad, stadium, history, and rival striker info.
- **`ScoutOfferDialog.kt`**: Scout contract presentation and negotiation dialog.
- **`LegalTextDialog.kt`**: In-game terms and privacy notice dialog.

### 3. Screens (`app/src/main/java/com/example/ui/screens/`)
- **`SetupScreen.kt`**: Character creation screen with name input, nationality selector with authentic flag emojis and alphabetical sorting, squad number, foot selection, and background story.
- **`ScoutedIntroScreen.kt`**: Cinematic intro screen displaying youth scout offers and academy onboarding.
- **`GameplayScreen.kt`**: Core hub screen hosting the top player bar, main tab bar (Home, Calendar, League, Club, Trophies), and active tab contents.
- **`RetiredSummaryScreen.kt`**: End-of-career hall-of-fame summary displaying peak stats, career trophies, caps, and option to continue lineage with a son.
- **`SonSetupScreen.kt`**: Next generation setup screen to create a son inheriting father's legacy attributes and potential ceiling.
- **`FamilyLegacyPage.kt`**: Family tree and lineage page showing all generations, historical season-by-season stats, bounded OVR progression chart, and trophy cases.
- **`YouthCareerEndedScreen.kt`**: Game over screen if a youth player reaches age 19 without signing a senior professional deal.

### 4. Tabs (`app/src/main/java/com/example/ui/tabs/`)
- **`HomeTab.kt`**: Central dashboard tab displaying current match day, season progress, social meters (Morale, Fan Rep, Manager Trust, Rival Rel), next fixture preview, and advance action.
- **`CalendarTab.kt`**: Full 12-month season fixture calendar (August through July) with month selector tabs, swipe gestures, match results, and simulation.
- **`FeedAndCalendarTab.kt`**: Unified tab combining 12-month calendar schedule with the social media newsfeed.
- **`LeagueTab.kt`**: 3-Way competitions hub (DOMESTIC, EUROPE, WORLD CUP) displaying standings tables, goalscorer charts, European tournaments, active World Cup brackets, countdown tracker, and past World Cup winners archive modal.
- **`StreetLeagueTab.kt`**: Unaffiliated grassroots street football view before joining an official academy.
- **`ClubTab.kt`**: Senior club management tab showing team squad, contract details, and training options.
- **`StreetClubTab.kt`**: Grassroots practice drills tab and Youth Academy view.
- **`TrophiesTab.kt`**: Cabinet displaying all unlocked domestic, European, international, and individual awards.

### 5. Dialogs (`app/src/main/java/com/example/ui/dialogs/`)
- **`TransferOffersDialog.kt`**: Incoming transfer offers dialog and official contract signing ceremony with animated signature.
- **`SettingsDialog.kt`**: Options menu for audio volume, haptics, auto-save, manual save, save slot switching, career reset, and developer tools PIN unlock.
- **`ChoiceEventDialog.kt`**: High-stakes off-pitch narrative choice prompts affecting morale, trust, and fan reputation.
- **`TrainingDialog.kt`**: Senior interactive training mini-game for attribute development and overtraining risk management.
- **`SeasonSummaryDialog.kt`**: End-of-season awards ceremony, Player of the Season trophy presentation, and season recap.

### 6. Components (`app/src/main/java/com/example/ui/components/`)
- **`ButtonFeedbackAnimation.kt`**: Custom click feedback animations including `bounceClick` modifier and `AnimatedSaveButton`.
- **`ClubCrestIcon.kt`**: Clean geometric badge renderer displaying club shapes, patterns, emblem symbols, and club-specific color palettes.
- **`CountryFlagIcon.kt`**: Vector flag renderer for domestic and international nationalities.
- **`JerseyNumberIcon.kt`**: Custom squad jersey number icon renderer.
- **`PlayerFaceIcon.kt`**: Procedural avatar generator with curved mustache styles, tapered beard coverage, skin tones, and hairstyles.
- **`TacticalPitchBackground.kt`**: Canvas tactical pitch background illustration with pitch markings.
- **`TrainingDrillBackgrounds.kt`**: Decorative canvas backgrounds for training drill activities.
- **`BadgeIcon.kt`**: Tier and achievement badge icon composable.
- **`LoadingTips.kt`**: Rotating gameplay tips and trivia during transitions.
- **`RepeatingStepperButton.kt`**: Hold-to-repeat numeric stepper controls for responsive attribute allocation.
- **`StatItem.kt`**: Reusable attribute progress bar and numerical stat indicator.
- **`UiHelpers.kt`**: Shared UI helper utilities including `getMonthName` supporting all 12 calendar months, formatting helpers, and stat bar composables.

