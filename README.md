# Apex Striker Career

**v2.9.0** • A single-player, text-driven soccer career simulation built with **Kotlin** and **Jetpack Compose** for Android.

You are a young striker starting in the concrete cages of your hometown and working your way up to European stadiums, trophies, international glory at the **World Cup**, and eventually a **multi-generational football legacy**. There are no real clubs, no live multiplayer, and no microtransactions — just a deep, offline loop of matches, training, transfers, national team call-ups, and dynasty story.

> **Status:** Active development. The README and `PROJECT_STRUCTURE.md` document the current architecture, game loops, and UI components.

---

## What you do in this game

Each turn you **advance a month** across a full **12-month season** (August through July). In each month, the game auto-simulates or prompts interactive fixtures your player is involved in, updates domestic and European league tables, evaluates international call-ups in June, and appends detailed entries to your **Career Timeline** log. Between months you can:

- **Manually play** interactive matches with key moments (1v1s, through balls, penalties, late defensive actions).
- **Quick-sim** matches with detailed tactical summaries.
- **Train** specific attributes through youth & senior drills against independent attribute skill ceilings.
- **Negotiate transfers** with dynamic offers (1–3 clubs) scaled to your form and OVR during transfer windows (months 0, 5, 9).
- **Represent your country** — choose from 200 nations with authentic flag emojis, earn national team call-ups, and compete in the quadrennial **World Cup**.
- **Track your dynasty** with the multi-generational family tree, best records, and bounded OVR progression charts.
- **Browse** the trophy cabinet, domestic tables, European tournaments, World Cup bracket, fixtures calendar, social media feed, and club profiles.

---

## Key Features

- **Multi-phase career arc** — Street Football → Youth Academy → Senior Professional → International Football.
- **Five core attributes & independent ceilings** — Finishing, Pace, Passing, Physical, Technique. Each attribute possesses an independent skill ceiling rolled at creation with growth/decline strictly clamped to that attribute's potential.
- **Dynamic OVR & Growth** — Weighted composite OVR capped by `potentialCeiling`, scaling generation by generation.
- **Match Engine & Simulation** — Interactive live matches with authentic referee whistles (`kick_off`, `half_time`, `full_time`), penalty timing bars, and commentary logs.
- **Competitions & Tournaments** — Domestic league, Domestic Cup, Super Cup, Champions League, Europa League, Conference League, and the quadrennial World Cup tournament.
- **3-Way Competitions Hub** — Switch seamlessly between Domestic, European, and World Cup tournament brackets, complete with past champions history and countdowns to the next tournament cycle.
- **12-Month Calendar Schedule** — Complete season schedule covering August (Month 0) through July (Month 11) in the Feed & Calendar section.
- **Dynamic Transfer System** — 1 to 3 offers based on player OVR and current form.
- **Procedural Club Crests & Face Avatars** — Geometric heraldic crests and customizable player face avatars with mustache/beard styles and form expressions.
- **Family Legacy & Dynasty** — Multi-generational lineage, OVR progression graph, generation milestones, and inherited potential pools.
- **Slot-based save system** with Room database per save slot and smooth simulation overlays.

---

## The Career Arc

### Phase 1 — Street Football (`careerPhase = STREET`)
You start at **age 13** in an informal background (choose from *Street Cages*, *School Team*, or *Family Club*). Your first years are played in back-alley 1v1/cage matches: 1–3 games per month against local crews such as *Eastside Cage* and *Concrete Kings*. These games build your early story (recorded as `StreetFootballGame` entries) and your `streetFootballGamesThisSeason` tally.

### Phase 2 — Youth Academy (`careerPhase = YOUTH`)
Aged 13–15 you're at an academy. You no longer play the matches yourself — each month the game auto-simulates your academy's league fixtures, updates youth standings, and rolls up your **goals / assists / MVPs** into `youthGoals`, `youthAssists`, `youthMvps`, and `youthGamesPlayed`. Youth scout offers (`YouthScoutOffer`) invite you to specialist academies, and later (`YouthToSeniorOffer`) your pro debut path opens.

### Phase 3 — Senior Professional (`careerPhase = SENIOR`)
You sign for a professional club with a squad number, contract, and wage:
- **League** — double round-robin tables among 16 clubs, live-updating positions and qualification zones.
- **Europe** — Champions League / Europa League / Conference League with group + knockout stages.
- **Domestic Cup + Super Cup.**
- **Manual matches** — when it's your game, advance to `MATCH_SCREEN` and play it yourself; set the final score, your goals, assists, minutes played, and match rating.

### Phase 4 — International Football & The World Cup
- Select from **200 global nations** at character setup, complete with national flag emojis.
- In **Month 10 (June)**, players who meet their nation's tier thresholds for OVR and 5-match form receive international call-ups.
- Dual-nationality or single-nationality eligibility prompts allow committing to a country.
- Every 4 seasons (Season 4, 8, 12, etc.), qualified nations compete in the **World Cup** tournament.
- The League tab's **WORLD CUP** section displays active tournament stages, past winners archive, and a season countdown when no tournament is active.

---

## How OVR & Skill Ceilings Work

### Core Formula
Your overall rating is a **transparent weighted formula** of the five core attributes:

```
OVR = (Finishing × 0.35) + (Pace × 0.20) + (Passing × 0.15)
      + (Physical × 0.15) + (Technique × 0.15)
```

- `OVR` is clamped to `[1, 99]` and further clamped to your composite `potentialCeiling`.
- `potentialCeiling` is set at character creation and scales by **generation**: Generation 1 starts capped around ~68, and each later generation raises the ceiling, reaching 99 around Generation 5.
- Your **father's** realization of his own ceiling (`fatherFinalOvr / fatherPotentialCeiling`) feeds directly into your son's ceiling and starting boosts.

### Independent Per-Skill Ceilings
Each player possesses individual attribute ceilings:
- `finishingCeiling`, `paceCeiling`, `passingCeiling`, `physicalCeiling`, `techniqueCeiling`
- Rolled at character creation with independent $\pm(0\text{–}6)$ deviations around the composite potential ceiling.
- Training and match growth clamp each attribute to its individual ceiling, allowing realistic player profiles (e.g., a lightning-fast winger who caps out in passing earlier, or a pure clinical poacher).

---

## Social & Reputation System

Beyond raw attributes, the game tracks six social/physical states visible on the Home tab:

| Stat | Range | Effect |
|------|-------|--------|
| **Form** | -5 → +5 | Shifts your effective match OVR (+form added to the win-probability diff). Good matches push it up, quiet ones drag it down. |
| **Fatigue** | 0–100% | High fatigue penalizes start probability and rating. |
| **Morale** | 0–100 | Flavor and retention effects. |
| **Fan Reputation** | 0–100 | Recognition and sponsorship flavor. |
| **Manager Trust** | 0–100 | ≥70 gives you start-probability and form bonuses; below 30 benches you or locks you out of starts. |
| **Rival Relationship** | 0–100 | Hostile rivals boost the opponent's effective OVR against you. |

Match **rating** is computed from your goals, assists, and base attributes, with Man-of-the-Match awarded for ratings > 8.0 with goal/assist contributions.

---

## Transfers

- Windows open in **months 0, 5, and 9** (August, January, and May).
- The game generates interest from other clubs based on your form, OVR, and performance; you accept/decline via the Transfer Offers dialog.
- Completing a transfer resets the current monthly choice-flow and advances you to your new club.

---

## Training

- **Youth Phase**: Pick one drill per month (Shooting, Passing, Pace, Technical, Physical) to accumulate attribute bonuses.
- **Senior Phase**: Balanced training drills with risk management for injury and fatigue.
- Stat growth respects each attribute's individual ceiling.

---

## Multi-Generational Legacy

1. **Retire** your player at any time — the game records your final OVR, trophies, international caps, and legacy weight.
2. **Legacy summary** screen shows your career; then you **create your son**.
3. The son inherits:
   - A boosted starting stat pool derived from father's final OVR and trophy success, scaled down if potential was squandered.
   - A higher `potentialCeiling` by generation, along with newly rolled skill ceilings.
4. Play as the son — **Generation 2**, then optionally a son of *that* player, and so on.

---

## How to Play (Quickstart)

1. Create a striker — choose your name, birth country (with flag emoji), preferred foot, squad number, and background story.
2. Tap **`ADVANCE`** each month to simulate all matches in the current month.
3. On matchday, choose **Play Manually** (interactive moments) or **Quick-Sim**.
4. Watch the Career Timeline log narrate your season across 12 months.
5. In June (Month 10), watch for national team call-up invitations and World Cup tournaments.
6. Reach the end of a season, retire when ready, and build a generational legacy.

---

## Save System & Device Storage

- Each career is its own **Room database file**, keyed by slot: `apex_career_slot_<slotId>.db`.
- Database version: `2` (Room schema v2).
- `audio_settings` and `save_slots` are stored in **SharedPreferences**.
- All data is strictly local with no external cloud servers or tracking.

---

## Technical Details

| Item | Value |
|------|-------|
| **Platform** | Android (API 24+, Android 10+ recommended) |
| **Language** | Kotlin |
| **UI** | Jetpack Compose (Material 3) |
| **Data** | Room (SQLite) + StateFlow + SharedPreferences |
| **App ID** | `com.example` |
| **Min SDK** | 24 |
| **Target SDK** | 36 |
| **Database Version** | 2 |

---

## Contact & Support

For questions, feedback, or support inquiries, contact **Apexstrikersim@gmail.com**.

---

*Apex Striker Career is an amateur, offline passion-project simulator. No real player or club data is used; all club names, leagues, and competitions are fictitious.*

