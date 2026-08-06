# Apex Striker Career

**v2.4.0** • A single-player, text-driven soccer career simulation built with **Kotlin** and **Jetpack Compose** for Android.

You are a young striker starting in the concrete cages of your hometown and working your way up to European stadiums, trophies, and eventually a **multi-generational football legacy**. There are no real clubs, no live multiplayer, and no microtransactions — just a tight loop of matches, training, transfers, and story.

> **Status:** Early development *(repo created July 2026)*. The README is the source of truth for what the game is and how it works.

---

## What you do in this game

Each turn you **advance a month**. In that month the game auto-simulates every fixture your player is involved in, updates the league tables, and appends a line to your **Career Timeline** log. Between months you can:

- **Manually play** a match (pick score, goals, assists, rating, minutes played) instead of quick-simming it.
- **Quick-sim** the current match with a single tap.
- **Train** specific attributes through youth drills.
- **Buy / sell** your player during transfer windows (months 0, 5, 9).
- **Browse** the trophy cabinet, league tables, fixtures calendar, and club info.

It's a "manage the career arc" game, not a real-time action game.

---

## Features

- **Three-phase career arc** — Street Football → Youth Academy → Senior Professional.
- **Five core attributes** drive everything: Finishing, Pace, Passing, Physical, Technique.
- **OVR that responds to your real stats**, capped at a per-player `potentialCeiling`.
- **Manual vs. Quick-SIM match play** with goals, assists, match ratings, Man-of-the-Match, and form shifts.
- **European competitions** — Champions League, Europa League, Conference League, plus domestic league + cups, all with proper group/league and knockout-bracket tracking.
- **Transfer windows**, squad rotation, bench roles (starter / sub / benched), and manager-trust effects.
- **Youth development path** with training drills (Shooting, Passing, Pace, Technical, Physical) and scout offers.
- **Social stats** — morale, fan reputation, manager trust, rival relationship, fatigue, and form.
- **Multi-generational legacy** — retire, record your legacy, then create and guide your son's career with inherited potential.
- **Developer mode** for testing: instant stat editing, season simulation, club-rep editing.
- **Slot-based save system** with Room database per save slot.

---

## The Career Arc

### Phase 1 — Street Football (`careerPhase = STREET`)
You start at **age 13** in an informal background (choose from *Street Cages*, *School Team*, or *Family Club*). Your first years are played in back-alley 1v1/cage matches: 1–3 games per month against local crews such as *Eastside Cage* and *Concrete Kings*. These games build your early story (recorded as `StreetFootballGame` entries) and your `streetFootballGamesThisSeason` tally.

### Phase 2 — Youth Academy (`careerPhase = YOUTH`)
Aged 13–15 you're at an academy. You no longer play the matches yourself — each month the game auto-simulates your academy's league fixtures, updates youth standings, and rolls up your **goals / assists / MVPs** into `youthGoals`, `youthAssists`, `youthMvps`, and `youthGamesPlayed`.

Youth scout offers (`YouthScoutOffer`) invite you to specialist academies, and later (`YouthToSeniorOffer`) your pro debut path opens.

### Phase 3 — Senior Professional (`careerPhase = SENIOR`)
You sign for a real pro club with a squad number, a club, and a contract. This is the heart of the game:

- **League** — double round-robin tables among 16 clubs, live-updating positions and qualification zones.
- **Europe** — Champions League / Europa League / Conference League, tracked as full group + knockout brackets.
- **Domestic Cup + Super Cup.**
- **Manual matches** — when it's your game, advance to `MATCH_SCREEN` and play it yourself; set the final score, your goals, assists, minutes played, and match rating.

---

## How OVR Works

Your overall rating is a **transparent weighted formula** of the five core attributes, and it can never exceed your ceiling:

```
OVR = (Finishing × 0.35) + (Pace × 0.20) + (Passing × 0.15)
      + (Physical × 0.15) + (Technique × 0.15)
```

- `OVR` is clamped to `[1, 99]` and further clamped to your `potentialCeiling`.
- `potentialCeiling` is set at character creation and scales by **generation**: Generation 1 starts capped well below 99 (~68), and each later generation raises the ceiling, reaching 99 around Generation 5.
- Your **father's** realization of his own ceiling (`fatherFinalOvr / fatherPotentialCeiling`) feeds directly into your son's ceiling and starting boosts — a family that squandered its potential gets a lower ceiling next gen.

---

## Social & Reputation System

Beyond raw attributes, the game tracks six social/physical states visible on the Home tab:

| Stat | Range | Effect |
|------|-------|--------|
| **Form** | -5 → +5 | Shifts your effective match OVR (+form added to the win-probability diff). Good matches push it up, quiet ones drag it down. |
| **Fatigue** | 0–100% | High fatigue penalizes start probability and rating. |
| **Morale** | 0–100 | Flavor/retention effects. |
| **Fan Reputation** | 0–100 | Recognition / story flavor. |
| **Manager Trust** | 0–100 | ≥70 gives you start-probability and form bonuses; below 30 benches you or even locks you out of starts. |
| **Rival Relationship** | 0–100 | Hostile rivals boost the opponent's effective OVR against you. |

Match **rating** is computed from your goals, assists, and base attributes, with Man-of-the-Match awarded for ratings > 8.0 with goal/assist contributions.

---

## Transfers

- Windows open in **months 0, 5, and 9** (roughly August, January, and May).
- The game generates interest from other clubs based on your form, OVR, and performance; you accept/decline via the Transfer Offers dialog.
- Completing a transfer resets the current monthly choice-flow and advances you to your new club.

---

## Training (Youth)

While in the youth phase, you can complete one training drill per month, picking a focus:

- **Shooting** → `finishingTrainingBonus` → grows Finishing (capped at ceiling).
- **Passing** → grows Passing.
- **Pace** → grows Pace.
- **Technical** → grows Technique.
- **Physical** → grows Physical.

When a bonus meter reaches 1.0+, the corresponding attribute ticks up by `1` (clamped to your potential ceiling). Training is the main way to grow stats while you're too young for senior minutes.

---

## Multi-Generational Legacy

This is the long game:

1. **Retire** your player at any time — the game records your final OVR, trophies, and a `totalTrophiesWeight`.
2. **Legacy summary** screen shows your career; then you **create your son**.
3. The son inherits:
   - A boosted starting stat pool derived from `(fatherFinalOvr - 50) × 0.15 + fatherTrophiesWeight × 0.20`, *scaled down* if the father under-achieved his own ceiling.
   - A `potentialCeiling` nudged up by generation, with a penalty if potential was squandered.
4. Play as the son — **Generation 2**, then optionally a son of *that* player, and so on.

So your worst season can literally drag down your heir's starting ceiling. Plan accordingly.

---

## How to Play (Quickstart)

1. Create a striker — pick a name, nationality, preferred foot, squad number, and background story.
2. Tap **`ADVANCE`** each month to simulate all matches in the current month.
3. On matchday, choose **Play Manually** (set score/lineup) or **Quick-Sim**.
4. Watch the Career Timeline log narrate your season.
5. Between months, train, scout offers, and negotiate transfers.
6. Reach the end of a season, retire when ready, and start a son's legacy.

Keyboard shortcuts / device: the game is fully touch-composed (Jetpack Compose). The `testTag` markers on every screen (`setup_screen`, `home_tab`, `advance_button`, `league_tab`, etc.) are useful if you automate testing.

---

## Save System & Device Backup

- Each career is its own **Room database file**, keyed by slot: `apex_career_slot_<slotId>.db`.
- `audio_settings` and `save_slots` are stored in **SharedPreferences**.
- A one-time **legacy migration** (`migrateLegacyDatabaseIfNeeded`) runs on first open.
- ⚠️ Android **Auto Backup** is currently enabled (`android:allowBackup="true"`). To prevent stale saves from being silently restored after an uninstall/reinstall during testing, consider setting `allowBackup="false"` while debugging, or add an `<exclude>` rule for `save_slots.xml` and the `apex_career_slot_*.db` files in `data_extraction_rules.xml` / `backup_rules.xml`.

---

## Technical Details

| Item | Value |
|------|-------|
| **Platform** | Android (API 24+, Android 10+ recommended) |
| **Language** | Kotlin |
| **UI** | Jetpack Compose (Material 3) |
| **Data** | Room (SQLite) + StateFlow + DataStore/SharedPreferences |
| **App ID** | `com.aistudio.footballcareersim.rkypws` |
| **Min SDK** | 24 |
| **Target SDK** | 36 |
| **Version** | `2.3.3` (codeVersion 9) |

### Project layout (relevant parts)
```
app/src/main/java/com/example/
├── data/              # Room entities (Player, Club, Fixture, GameState, Trophies...), DAO, CareerRepository
│   └── AppDatabase.kt # slot-keyed databases + legacy migration
├── ui/                # Jetpack Compose screens + CareerViewModel
│   ├── CareerViewModel.kt
│   ├── MainGameScreen.kt      # single screen, switches by `Screen` enum
│   ├── SetupScreen            # character creation
│   ├── GameplayScreen         # Home/Calendar/League/Club/Trophies tabs
│   └── SettingsDialog         # audio + game settings
└── data/FictionalData.kt       # country names, club data
```

The app is architecturally a **single-activity / single-compose-root**: `MainGameScreen` switches between screens via a `Screen` enum (`SETUP`, `INTRO`, `GAMEPLAY`, `RETIRED_SUMMARY`, `SON_SETUP`, `MATCH_SCREEN`, `YOUTH_CAREER_ENDED`) driven by `CareerViewModel._activeScreen`.

---

## Contributing / Notes for Editors

- Keep the OVR formula in sync between `CareerRepository.calculateOvr()` and any UI that displays it.
- The youth-vs-senior growth asymmetry above is **intentional behavior to be aware of** — do not "fix" the youth match recording to also bump attributes unless the design calls for it, otherwise Generation 1 youth OVR can blow past its deliberate low ceiling.
- All numeric balance (rating thresholds, form deltas, rep multipliers) lives in `CareerRepository.kt` — the single place to tune match difficulty and growth speed.

---

*Apex Striker Career is an amateur, passion-project simulator. No real player or club data is used; all names and leagues are fictitious.*