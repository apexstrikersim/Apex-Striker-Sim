package com.example.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.yield
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

class CareerRepository(private val context: Context, val slotId: Int = 1) {

    private val db = AppDatabase.getDatabase(context, slotId)
    private val dao = db.careerDao()

    var onTrophyUnlockedListener: ((TrophyEntity) -> Unit)? = null

    // Flows for UI binding
    val playerFlow: Flow<PlayerEntity?> = dao.getPlayerFlow()
    val allClubsFlow: Flow<List<ClubEntity>> = dao.getAllClubsFlow()
    val allFixturesFlow: Flow<List<FixtureEntity>> = dao.getAllFixturesFlow()
    val allTrophiesFlow: Flow<List<TrophyEntity>> = dao.getAllTrophiesFlow()
    val allLegaciesFlow: Flow<List<LegacyEntity>> = dao.getAllLegaciesFlow()
    val allSeasonRecordsFlow: Flow<List<PlayerSeasonRecordEntity>> = dao.getAllSeasonRecordsFlow()
    val gameStateFlow: Flow<GameStateEntity?> = dao.getGameStateFlow()
    val youthAcademiesFlow: Flow<List<YouthAcademyEntity>> = dao.getAllYouthAcademiesFlow()
    val youthStandingsFlow: Flow<List<YouthStandingEntity>> = dao.getAllYouthStandingsFlow()
    val youthFixturesFlow: Flow<List<YouthFixtureEntity>> = dao.getAllYouthFixturesFlow()
    val allSocialPostsFlow: Flow<List<SocialPostEntity>> = dao.getAllSocialPostsFlow()

    private val _latestSeasonSummaryFlow = MutableStateFlow<SeasonSummaryData?>(null)
    val latestSeasonSummaryFlow: StateFlow<SeasonSummaryData?> = _latestSeasonSummaryFlow.asStateFlow()

    fun clearSeasonSummary() {
        _latestSeasonSummaryFlow.value = null
    }

    suspend fun getPlayerSync(): PlayerEntity? = dao.getPlayerSync()

    suspend fun clearAllData() {
        db.withTransaction {
            dao.clearPlayers()
            dao.clearClubs()
            dao.clearStandings()
            dao.clearFixtures()
            dao.clearTrophies()
            dao.clearLegacies()
            dao.clearGameState()
            dao.clearUsedNames()
            dao.clearClubSeasonHistories()
            dao.clearClubRecords()
            dao.clearYouthAcademies()
            dao.clearYouthStandings()
            dao.clearYouthFixtures()
            dao.clearStreetFootballGames()
            dao.clearSocialPosts()
            dao.clearNpcStrikers()
            dao.clearNpcManagers()
        }
    }

    fun getStreetFootballGamesFlow(seasonNumber: Int): Flow<List<StreetFootballGameEntity>> =
        dao.getStreetFootballGamesForSeasonFlow(seasonNumber)

    fun getStandingsFlow(country: String): Flow<List<StandingEntity>> =
        dao.getStandingsByCountryFlow(country)

    fun getFixturesForMonthFlow(monthIndex: Int): Flow<List<FixtureEntity>> =
        dao.getFixturesForMonthFlow(monthIndex)

    suspend fun acceptYouthScoutOffer(academyId: Int) {
        db.withTransaction {
            val player = dao.getPlayerSync() ?: return@withTransaction
            val gameState = dao.getGameStateSync() ?: return@withTransaction
            player.careerPhase = PHASE_YOUTH
            player.currentAcademyId = academyId
            gameState.persistedYouthOffers = null
            dao.updatePlayer(player)
            dao.updateGameState(gameState)
        }
    }

    suspend fun rejectYouthScoutOffer(academyId: Int) {
        db.withTransaction {
            val player = dao.getPlayerSync() ?: return@withTransaction
            val gameState = dao.getGameStateSync() ?: return@withTransaction
            val currentRejected = if (player.rejectedAcademyIds.isBlank()) mutableListOf() else player.rejectedAcademyIds.split(",").toMutableList()
            if (!currentRejected.contains(academyId.toString())) {
                currentRejected.add(academyId.toString())
                player.rejectedAcademyIds = currentRejected.joinToString(",")
                dao.updatePlayer(player)
            }
            val currentOffers = YouthCareerLogic.deserializeYouthOffers(gameState.persistedYouthOffers ?: "")
            val remaining = currentOffers.filter { it.academyId != academyId }
            gameState.persistedYouthOffers = if (remaining.isEmpty()) null else YouthCareerLogic.serializeYouthOffers(remaining)
            dao.updateGameState(gameState)
        }
    }

    suspend fun acceptSeniorYouthOffer(clubId: Int, contractYears: Int, targetGplusA: Int): String? {
        var resultMsg: String? = null
        db.withTransaction {
            val player = dao.getPlayerSync() ?: return@withTransaction
            val gameState = dao.getGameStateSync() ?: return@withTransaction
            player.careerPhase = PHASE_SENIOR
            player.currentClubId = clubId
            player.contractYearsRemaining = contractYears
            player.contractTargetGoalsAssists = targetGplusA
            gameState.persistedSeniorYouthOffers = null
            dao.updatePlayer(player)
            dao.updateGameState(gameState)
        }
        return resultMsg
    }

    suspend fun rejectSeniorYouthOffer(clubId: Int) {
        db.withTransaction {
            val player = dao.getPlayerSync() ?: return@withTransaction
            val gameState = dao.getGameStateSync() ?: return@withTransaction
            val currentRejected = if (player.rejectedSeniorClubIds.isBlank()) mutableListOf() else player.rejectedSeniorClubIds.split(",").toMutableList()
            if (!currentRejected.contains(clubId.toString())) {
                currentRejected.add(clubId.toString())
                player.rejectedSeniorClubIds = currentRejected.joinToString(",")
                dao.updatePlayer(player)
            }
            val currentOffers = YouthCareerLogic.deserializeSeniorYouthOffers(gameState.persistedSeniorYouthOffers ?: "")
            val remaining = currentOffers.filter { it.clubId != clubId }
            gameState.persistedSeniorYouthOffers = if (remaining.isEmpty()) null else YouthCareerLogic.serializeSeniorYouthOffers(remaining)
            dao.updateGameState(gameState)
        }
    }

    suspend fun declineAllYouthScoutOffers() {
        db.withTransaction {
            val player = dao.getPlayerSync() ?: return@withTransaction
            val gameState = dao.getGameStateSync() ?: return@withTransaction
            val currentOffers = YouthCareerLogic.deserializeYouthOffers(gameState.persistedYouthOffers ?: "")
            val currentRejected = if (player.rejectedAcademyIds.isBlank()) mutableSetOf() else player.rejectedAcademyIds.split(",").toMutableSet()
            currentOffers.forEach { currentRejected.add(it.academyId.toString()) }
            player.rejectedAcademyIds = currentRejected.joinToString(",")
            gameState.persistedYouthOffers = null
            dao.updatePlayer(player)
            dao.updateGameState(gameState)
        }
    }

    suspend fun declineAllSeniorYouthOffers() {
        db.withTransaction {
            val player = dao.getPlayerSync() ?: return@withTransaction
            val gameState = dao.getGameStateSync() ?: return@withTransaction
            val currentOffers = YouthCareerLogic.deserializeSeniorYouthOffers(gameState.persistedSeniorYouthOffers ?: "")
            val currentRejected = if (player.rejectedSeniorClubIds.isBlank()) mutableSetOf() else player.rejectedSeniorClubIds.split(",").toMutableSet()
            currentOffers.forEach { currentRejected.add(it.clubId.toString()) }
            player.rejectedSeniorClubIds = currentRejected.joinToString(",")
            gameState.persistedSeniorYouthOffers = null
            dao.updatePlayer(player)
            dao.updateGameState(gameState)
        }
    }

    suspend fun updatePlayer(player: PlayerEntity) {
        dao.updatePlayer(player)
    }

    /**
     * Set up a new career (Generation 1 or Son).
     */
    suspend fun startNewCareer(
        playerName: String,
        birthCountry: String,
        academyCountry: String,
        isSon: Boolean = false,
        fatherFinalOvr: Int = 0,
        fatherTrophiesWeight: Int = 0,
        fatherPotentialCeiling: Int = 99,
        preferredFoot: String = "Right",
        squadNumber: Int = 9,
        backgroundStory: String = "Street Cages"
    ) {
        db.withTransaction {
            // Read legacy list to preserve it if starting a son's career
            val legacyList = if (isSon) dao.getAllLegaciesSync() else emptyList()

            // Clear previous progress - only clear players unconditionally
            dao.clearPlayers()
            if (!isSon) {
                dao.clearClubs()
                dao.clearStandings()
                dao.clearFixtures()
                dao.clearTrophies()
                dao.clearGameState()
                dao.clearUsedNames()
                dao.clearClubSeasonHistories()
                dao.clearClubRecords()
                dao.clearLegacies()
                dao.clearNpcStrikers()
                dao.clearNpcManagers()
            }

            // Restore legacies
            for (legacy in legacyList) {
                dao.insertLegacy(legacy)
            }

            val dbClubs = if (!isSon) {
                // 1. Generate All Clubs across 5 countries
                val allClubs = mutableListOf<ClubEntity>()
                for (country in FictionalData.COUNTRIES) {
                    val templates = FictionalData.CLUB_TEMPLATES[country] ?: emptyList()
                    for (template in templates) {
                        val repPoints = FictionalData.getReputationPoints(template.reputation)
                        val rivalRange = FictionalData.getRivalOvrRange(template.reputation)
                        val rivalOvr = Random.nextInt(rivalRange.first, rivalRange.last + 1)
                        val rivalName = generateUniqueName(country, dao)
                        val foundedSeasonsAgo = Random.nextInt(50, 141)
                        
                        val suffix = listOf("Lions", "Warriors", "Eagles", "Tigers", "Knights", "Giants").random()
                        val nickname = "The $suffix"
                        val stadiumPart = listOf("Arena", "Park", "Stadium", "Ground", "Coliseum").random()
                        val cleanName = template.name.replace(" Rovers", "").replace(" Town", "").replace(" United", "").replace(" City", "").replace(" Athletic", "").replace(" FC", "").replace(" CD", "").replace(" AS", "").replace(" US", "").replace(" SV", "").replace(" AC", "").replace(" Real", "")
                        val stadium = "$cleanName $stadiumPart"
                        val style = listOf(
                            "Known for their high-pressing offensive philosophy and deep-rooted community pride.",
                            "Famous for their rock-solid defensive organization and clinical counter-attacking football.",
                            "Renowned for their fluid possession-based style and a rich history of nurturing world-class talents.",
                            "Distinguished by their aggressive aerial play, unmatched work rate, and passionate fanbase.",
                            "Highly regarded for tactical flexibility, high stamina levels, and a relentless winning mentality."
                        ).random()
                        val description = "Widely known as $nickname, they play their home matches at the prestigious $stadium. $style"

                        allClubs.add(
                            ClubEntity(
                                name = template.name,
                                country = country,
                                reputation = template.reputation,
                                reputationPoints = repPoints,
                                rivalStrikerName = rivalName,
                                rivalStrikerOvr = rivalOvr,
                                description = description,
                                foundedSeasonsAgo = foundedSeasonsAgo
                            )
                        )
                    }
                }
                dao.insertClubs(allClubs)

                // Re-fetch clubs to get their generated IDs and ensure club records are seeded
                val generatedClubs = dao.getAllClubsSync()
                for (dbClub in generatedClubs) {
                    ensureClubRecordsExist(dbClub.id)
                }

                // Seed NPC Strikers and Managers for each generated club
                val seededClubs = mutableListOf<ClubEntity>()
                for (dbClub in generatedClubs) {
                    val rivalRange = FictionalData.getRivalOvrRange(dbClub.reputation)
                    val strikerOvr = Random.nextInt(rivalRange.first, rivalRange.last + 1)
                    val strikerName = generateUniqueName(dbClub.country, dao)
                    val striker = NpcStrikerEntity(
                        name = strikerName,
                        country = dbClub.country,
                        currentClubId = dbClub.id,
                        age = Random.nextInt(20, 30),
                        ovr = strikerOvr,
                        potentialCeiling = strikerOvr + Random.nextInt(0, 8),
                        seasonsAtCurrentClub = Random.nextInt(0, 4),
                        isRetired = false,
                        isRisingTalent = false
                    )
                    dao.insertNpcStriker(striker)

                    val managerName = generateUniqueName(dbClub.country, dao)
                    val seasonsAtClub = Random.nextInt(0, 4)
                    val manager = NpcManagerEntity(
                        name = managerName,
                        country = dbClub.country,
                        currentClubId = dbClub.id,
                        reputationTier = dbClub.reputation,
                        seasonsAtCurrentClub = seasonsAtClub,
                        seasonsAsManager = seasonsAtClub + Random.nextInt(0, 6),
                        isRetired = false
                    )
                    val managerId = dao.insertNpcManager(manager).toInt()

                    seededClubs.add(
                        dbClub.copy(
                            rivalStrikerName = strikerName,
                            rivalStrikerOvr = strikerOvr,
                            managerName = managerName,
                            managerReputationTier = dbClub.reputation,
                            managerId = managerId
                        )
                    )
                }
                dao.updateClubs(seededClubs)
                seededClubs
            } else {
                dao.getAllClubsSync()
            }

            // 2. Identify Player's starting club (lowest reputation in academyCountry)
            val countryClubs = dbClubs.filter { it.country == academyCountry }
            val startingClub = countryClubs
                .filter { it.reputation == "SMALL" }
                .minByOrNull { it.reputationPoints }
                ?: countryClubs.minByOrNull { it.reputationPoints }
                ?: dbClubs.first()

            // 3. Generate Player Stats
            val generation = if (isSon) legacyList.size + 1 else 1

            // How much of HIS OWN potential did the father actually fulfill? (1.0 = maxed it out, lower = fell short)
            val achievementRatio = if (isSon) {
                (fatherFinalOvr.toFloat() / fatherPotentialCeiling.toFloat()).coerceIn(0.3f, 1.1f)
            } else {
                1f
            }

            // Calculate Son's boost — scaled down if the father significantly underperformed his own ceiling,
            // not just judged on his raw final OVR.
            var statBoost = 0
            if (isSon) {
                val rawBoost = (fatherFinalOvr - 50) * 0.15f + fatherTrophiesWeight * 0.2f
                statBoost = (rawBoost * achievementRatio).roundToInt().coerceIn(5, 25)
            }

            val potentialCeiling = computePotentialCeiling(generation, statBoost, achievementRatio)

            val talentSeed = 0.90f + Random.nextFloat() * 0.25f // 0.9 to 1.15
            val minStat = 20 + statBoost
            val maxStat = 38 + statBoost

            val finishing = (Random.nextInt(minStat, maxStat + 1) * talentSeed).roundToInt().coerceIn(1, 99)
            val pace = (Random.nextInt(minStat, maxStat + 1) * talentSeed).roundToInt().coerceIn(1, 99)
            val passing = (Random.nextInt(minStat, maxStat + 1) * talentSeed).roundToInt().coerceIn(1, 99)
            val physical = (Random.nextInt(minStat, maxStat + 1) * talentSeed).roundToInt().coerceIn(1, 99)
            val technique = (Random.nextInt(minStat, maxStat + 1) * talentSeed).roundToInt().coerceIn(1, 99)

            val ovr = calculateOvr(finishing, pace, passing, physical, technique).coerceAtMost(potentialCeiling)

            val player = PlayerEntity(
                name = playerName,
                age = 13,
                birthCountry = birthCountry,
                academyCountry = academyCountry,
                currentClubId = -1,
                finishing = finishing,
                pace = pace,
                passing = passing,
                physical = physical,
                technique = technique,
                talentSeed = talentSeed,
                ovr = ovr,
                form = 0,
                contractYearsRemaining = 3,
                contractTargetGoalsAssists = 0, // In academy, no goal target initially
                generation = generation,
                peakOvr = ovr,
                potentialCeiling = potentialCeiling,
                careerPhase = PHASE_STREET,
                preferredFoot = preferredFoot,
                squadNumber = squadNumber,
                backgroundStory = backgroundStory
            )
            dao.insertPlayer(player)

            // Generate Youth Academies, Standings, and Fixtures
            dao.clearYouthAcademies()
            dao.clearYouthStandings()
            dao.clearYouthFixtures()
            dao.clearStreetFootballGames()

            val youthAcademies = YouthCareerLogic.generateYouthAcademies(dbClubs)
            if (youthAcademies.isNotEmpty()) {
                dao.insertYouthAcademies(youthAcademies)
                val savedAcademies = dao.getAllYouthAcademiesSync()
                val youthStandings = savedAcademies.map { ac ->
                    YouthStandingEntity(academyId = ac.id)
                }
                dao.insertYouthStandings(youthStandings)
                val youthFixtures = YouthCareerLogic.generateYouthFixtures(savedAcademies)
                dao.insertYouthFixtures(youthFixtures)
            }

            if (!isSon) {
                // 4. Generate League Standings (all clubs initialized with 0 points)
                val standings = dbClubs.map { club ->
                    StandingEntity(
                        country = club.country,
                        clubId = club.id
                    )
                }
                dao.insertStandings(standings)

                // 5. Generate League Fixtures
                val fixtures = mutableListOf<FixtureEntity>()
                for (country in FictionalData.COUNTRIES) {
                    val countryClubsList = dbClubs.filter { it.country == country }
                    fixtures.addAll(generateLeagueFixtures(country, countryClubsList))
                }
                dao.insertFixtures(fixtures)

                // 6. Generate European qualifications for Season 1 (based on starting reputation)
                generateEuropeanCupBracketsForSeason(1, dbClubs)

                // 7. Initialize GameState
                val introText = "Welcome to your grassroots football career, $playerName!\n" +
                        "You are starting your journey playing local street football in $birthCountry at age 13.\n" +
                        "Play street football matches, complete training drills to grow your stats, and attract youth academy scout offers during transfer windows!"

                val gameState = GameStateEntity(
                    currentSeason = 1,
                    currentMonthIndex = 0,
                    narrativeLog = introText,
                    autoSave = true,
                    isDevMode = false
                )
                dao.insertGameState(gameState)
            } else {
                // For Son: Preserve existing GameState (season count & month index carry forward!)
                val existingGameState = dao.getGameStateSync()
                val introText = "--- GENERATION $generation: ${playerName}'s Career Begins ---\n" +
                        "A scout spotted you playing in the local streets of $birthCountry. " +
                        "They recognized the lineage of your father and offered you a chance in the game.\n" +
                        "You have been placed in the youth academy of ${startingClub.name} in $academyCountry at age 13. " +
                        "Work hard in training, compete for your spot, and carry forward the legacy!\n\n"
                if (existingGameState != null) {
                    existingGameState.narrativeLog = introText + existingGameState.narrativeLog
                    dao.updateGameState(existingGameState)
                } else {
                    val gameState = GameStateEntity(
                        currentSeason = 1,
                        currentMonthIndex = 0,
                        narrativeLog = introText,
                        autoSave = true,
                        isDevMode = false
                    )
                    dao.insertGameState(gameState)
                }
            }
        }
    }

    /**
     * Returns a damping multiplier between 0.03 and 1.0 based on how close a stat already is,
     * proportionally, to the player's potential ceiling. Far from the ceiling, growth is
     * essentially unaffected (damping ≈ 1.0). As the stat approaches the ceiling, growth is
     * smoothly throttled toward a small trickle instead of being cut off abruptly at a hard cap.
     * This replaces the old flat "half growth at 80, quarter growth at 90" step function with a
     * continuous curve relative to THIS player's own ceiling (which varies by generation), so it
     * works correctly whether the ceiling is 68 or 99.
     */
    fun proximityDamping(current: Int, ceiling: Int): Float {
        if (ceiling <= 0) return 0f
        val progress = (current.toFloat() / ceiling.toFloat()).coerceIn(0f, 1.05f)
        return (1f - progress.pow(2.3f)).coerceIn(0.03f, 1f)
    }

    /**
     * Computes the maximum OVR (and per-stat cap) this generation's player can ever reach.
     * - Generation 1 always caps well below 99 (68) so a single career cannot max out immediately.
     * - Each subsequent generation raises the baseline ceiling, reaching 99 around generation 5.
     * - If the previous generation fell well short of THEIR OWN potential (achievementRatio < 1),
     *   this generation's ceiling is pulled down below the generation's normal baseline — a
     *   family that squanders its potential doesn't get the full generational step-up next time.
     */
    fun computePotentialCeiling(generation: Int, statBoost: Int, achievementRatio: Float): Int {
        val baseCeiling = (68 + (generation - 1) * 8).coerceAtMost(99)
        if (generation <= 1) return baseCeiling.coerceIn(60, 99)

        val legacyNudge = (statBoost - 15) / 3f
        val shortfall = (1f - achievementRatio).coerceIn(0f, 1f) // 0 = fully maxed potential, 1 = reached almost none of it
        val achievementPenalty = shortfall * 10f // up to -10 ceiling points for badly squandering potential

        return (baseCeiling + legacyNudge - achievementPenalty).roundToInt().coerceIn(55, 99)
    }

    /**
     * OVR formula: (Finishing * 0.35) + (Pace * 0.20) + (Passing * 0.15) + (Physical * 0.15) + (Technique * 0.15)
     */
    fun calculateOvr(finishing: Int, pace: Int, passing: Int, physical: Int, technique: Int): Int {
        val calculated = (finishing * 0.35f) + (pace * 0.20f) + (passing * 0.15f) + (physical * 0.15f) + (technique * 0.15f)
        return calculated.roundToInt().coerceIn(1, 99)
    }

    /**
     * Generate double round-robin league fixtures for 16 clubs.
     * Round index range: 1 to 30.
     * Splitting 30 matchdays across 10 months: 3 matchdays per month.
     */
    private fun generateLeagueFixtures(country: String, clubs: List<ClubEntity>): List<FixtureEntity> {
        val fixtures = mutableListOf<FixtureEntity>()
        val n = clubs.size
        val list = clubs.toMutableList()

        // Circle method
        for (round in 0 until (n - 1)) {
            for (i in 0 until n / 2) {
                val homeIdx = (round + i) % (n - 1)
                val awayIdx = (n - 1 - i + round) % (n - 1)

                val home = if (i == 0) list[n - 1] else list[homeIdx]
                val away = list[awayIdx]

                // Leg 1
                val leg1Month = round / 3 // 0 to 4
                fixtures.add(
                    FixtureEntity(
                        country = country,
                        competition = "LEAGUE",
                        monthIndex = leg1Month,
                        homeClubId = home.id,
                        awayClubId = away.id,
                        leg = 1,
                        round = round + 1
                    )
                )

                // Leg 2
                val leg2Month = 5 + (round / 3) // 5 to 9
                fixtures.add(
                    FixtureEntity(
                        country = country,
                        competition = "LEAGUE",
                        monthIndex = leg2Month,
                        homeClubId = away.id,
                        awayClubId = home.id,
                        leg = 2,
                        round = round + 16
                    )
                )
            }
        }
        return fixtures
    }

    /**
     * Set up European Cup Brackets at the start of a season.
     */
    private suspend fun generateEuropeanCupBracketsForSeason(season: Int, allClubs: List<ClubEntity>) {
        // Find qualified clubs.
        // For Season 1, we qualify the top 6 clubs in each country by starting reputation points.
        // For Season >1, we read standings from the previous year. But if we just started, we can use reputation as base.
        val clTeams = mutableListOf<ClubEntity>()
        val elTeams = mutableListOf<ClubEntity>()
        val confTeams = mutableListOf<ClubEntity>()

        for (country in FictionalData.COUNTRIES) {
            val countryClubs = allClubs.filter { it.country == country }
            val sorted = countryClubs.sortedByDescending { it.reputationPoints }
            if (sorted.size >= 6) {
                clTeams.add(sorted[0])
                clTeams.add(sorted[1])
                elTeams.add(sorted[2])
                elTeams.add(sorted[3])
                confTeams.add(sorted[4])
                confTeams.add(sorted[5])
            }
        }

        scheduleKnockoutTournament("CHAMPIONS_LEAGUE", clTeams)
        scheduleKnockoutTournament("EUROPA_LEAGUE", elTeams)
        scheduleKnockoutTournament("CONFERENCE_LEAGUE", confTeams)
    }

    private fun capNarrativeLog(log: String): String {
        val entries = log.split("\n\n")
        if (entries.size <= 30) return log
        return entries.take(30).joinToString("\n\n")
    }

    /**
     * Schedule a straight knockout tournament for 10 qualified teams.
     * Round 10 (Preliminary): 4 teams play (2 matches, 2 legs). 6 teams get a bye.
     * Month indices:
     * - Preliminary Leg 1: Month 1 (September), Leg 2: Month 2 (October).
     */
    private suspend fun scheduleKnockoutTournament(comp: String, teams: List<ClubEntity>) {
        android.util.Log.d("FootballCareer", "BRACKET_GEN: Scheduling $comp tournament with ${teams.size} teams: ${teams.joinToString { it.name }}")
        if (teams.size < 10) {
            android.util.Log.w("FootballCareer", "WARNING: $comp has only ${teams.size} teams qualified (expected 10). Skipping bracket generation.")
            return
        }
        val sortedTeams = teams.sortedByDescending { it.reputationPoints }

        // Bottom 4 teams play preliminary round
        val prelimTeams = sortedTeams.subList(6, 10).shuffled()
        val fixtures = mutableListOf<FixtureEntity>()

        // Match 1
        fixtures.add(FixtureEntity(
            country = "Europe",
            competition = comp,
            monthIndex = 1,
            homeClubId = prelimTeams[0].id,
            awayClubId = prelimTeams[1].id,
            leg = 1,
            round = 10
        ))
        fixtures.add(FixtureEntity(
            country = "Europe",
            competition = comp,
            monthIndex = 2,
            homeClubId = prelimTeams[1].id,
            awayClubId = prelimTeams[0].id,
            leg = 2,
            round = 10
        ))

        // Match 2
        fixtures.add(FixtureEntity(
            country = "Europe",
            competition = comp,
            monthIndex = 1,
            homeClubId = prelimTeams[2].id,
            awayClubId = prelimTeams[3].id,
            leg = 1,
            round = 10
        ))
        fixtures.add(FixtureEntity(
            country = "Europe",
            competition = comp,
            monthIndex = 2,
            homeClubId = prelimTeams[3].id,
            awayClubId = prelimTeams[2].id,
            leg = 2,
            round = 10
        ))

        // Persist the 6 direct-entry teams (with round = 11, monthIndex = -1, isSimulated = true so they sit dormant)
        val directTeams = sortedTeams.subList(0, 6)
        for (team in directTeams) {
            fixtures.add(FixtureEntity(
                country = "Europe",
                competition = comp,
                monthIndex = -1,
                homeClubId = team.id,
                awayClubId = -1,
                leg = 1,
                round = 11,
                isSimulated = true
            ))
        }

        dao.insertFixtures(fixtures)
    }

    private suspend fun simulateWorldFixturesForMonth(currentMonth: Int, excludeClubId: Int, monthLogs: MutableList<String>? = null) {
        val fixturesToSimulate = dao.getFixturesForMonthSync(currentMonth)
        val allClubsMap = dao.getAllClubsSync().associateBy { it.id }
        val otherMatches = fixturesToSimulate.filter { it.homeClubId != excludeClubId && it.awayClubId != excludeClubId }

        for (fixture in otherMatches) {
            if (fixture.isSimulated) continue
            val homeClub = allClubsMap[fixture.homeClubId] ?: continue
            val awayClub = allClubsMap[fixture.awayClubId] ?: continue

            val result = simulateTeamMatch(homeClub, awayClub)
            fixture.homeScore = result.first
            fixture.awayScore = result.second
            fixture.isSimulated = true

            dao.updateFixture(fixture)

            if (fixture.competition == "LEAGUE") {
                updateDomesticStandings(fixture)
            }
        }

        if (currentMonth == 2) {
            progressKnockoutRound("CHAMPIONS_LEAGUE", 10, 8, 3)
            progressKnockoutRound("EUROPA_LEAGUE", 10, 8, 3)
            progressKnockoutRound("CONFERENCE_LEAGUE", 10, 8, 3)
            monthLogs?.add("🏆 European cup preliminary rounds finished. Quarter-final brackets are drawn!")
        }
        if (currentMonth == 4) {
            progressKnockoutRound("CHAMPIONS_LEAGUE", 8, 4, 5)
            progressKnockoutRound("EUROPA_LEAGUE", 8, 4, 5)
            progressKnockoutRound("CONFERENCE_LEAGUE", 8, 4, 5)
            monthLogs?.add("🏆 European cup quarter-finals finished. Semi-final brackets are drawn!")
        }
        if (currentMonth == 6) {
            progressKnockoutRound("CHAMPIONS_LEAGUE", 4, 2, 8)
            progressKnockoutRound("EUROPA_LEAGUE", 4, 2, 8)
            progressKnockoutRound("CONFERENCE_LEAGUE", 4, 2, 8)
            monthLogs?.add("🏆 European cup semi-finals finished. The finalists are locked in for April!")
        }
        if (currentMonth == 8) {
            scheduleSuperCup()
            monthLogs?.add("🏆 Champions League and Europa League winners are crowned! Super Cup scheduled for May.")
        }
    }

    private suspend fun simulateYouthWorldForMonth(currentMonth: Int, player: PlayerEntity) {
        val monthFixtures = dao.getYouthFixturesForMonthSync(currentMonth)
        val standingsMap = dao.getAllYouthStandingsSync().associateBy { it.academyId }.toMutableMap()

        for (fix in monthFixtures) {
            if (fix.isSimulated) continue
            val homeScore = Random.nextInt(0, 4)
            val awayScore = Random.nextInt(0, 4)
            fix.homeScore = homeScore
            fix.awayScore = awayScore
            fix.isSimulated = true

            val isUserMatch = player.careerPhase == PHASE_YOUTH && (fix.homeAcademyId == player.currentAcademyId || fix.awayAcademyId == player.currentAcademyId)
            if (isUserMatch) {
                val pGoals = if (Random.nextFloat() < (player.finishing / 90f)) Random.nextInt(1, 3) else 0
                val pAssists = if (Random.nextFloat() < (player.passing / 90f)) Random.nextInt(0, 2) else 0
                val pMvp = pGoals + pAssists >= 2 && Random.nextFloat() < 0.5f

                fix.playerGoals = pGoals
                fix.playerAssists = pAssists
                fix.playerMvp = pMvp
                fix.playerCameOnMinute = 0

                player.youthGoals += pGoals
                player.youthAssists += pAssists
                if (pMvp) player.youthMvps += 1
                player.youthGamesPlayed += 1
            }

            val hStand = standingsMap[fix.homeAcademyId]
            val aStand = standingsMap[fix.awayAcademyId]
            if (hStand != null && aStand != null) {
                hStand.played += 1
                aStand.played += 1
                hStand.goalsFor += homeScore
                hStand.goalsAgainst += awayScore
                aStand.goalsFor += awayScore
                aStand.goalsAgainst += homeScore

                if (homeScore > awayScore) {
                    hStand.wins += 1; hStand.points += 3
                    aStand.losses += 1
                } else if (awayScore > homeScore) {
                    aStand.wins += 1; aStand.points += 3
                    hStand.losses += 1
                } else {
                    hStand.draws += 1; hStand.points += 1
                    aStand.draws += 1; aStand.points += 1
                }
            }
            dao.updateYouthFixture(fix)
        }
        dao.updateYouthStandings(standingsMap.values.toList())
    }

    private suspend fun resetYouthLeagueForNewSeason() {
        dao.clearYouthFixtures()
        val allAcademies = dao.getAllYouthAcademiesSync()
        dao.clearYouthStandings()
        val resetStandings = allAcademies.map { YouthStandingEntity(academyId = it.id) }
        dao.insertYouthStandings(resetStandings)
        val newYouthFixtures = YouthCareerLogic.generateYouthFixtures(allAcademies)
        dao.insertYouthFixtures(newYouthFixtures)
    }

    private suspend fun checkAndAwardYouthTrophy(player: PlayerEntity, gameState: GameStateEntity) {
        if (player.careerPhase == PHASE_YOUTH && player.currentAcademyId != null && player.currentAcademyId != -1) {
            val finalYouthStandings = dao.getAllYouthStandingsSync().sortedWith(
                compareByDescending<YouthStandingEntity> { it.points }.thenByDescending { it.goalsFor - it.goalsAgainst }
            )
            val topAcademyStanding = finalYouthStandings.firstOrNull()
            if (topAcademyStanding != null && topAcademyStanding.academyId == player.currentAcademyId) {
                val myAcademy = dao.getAllYouthAcademiesSync().find { it.id == player.currentAcademyId }
                safeInsertTrophy(TrophyEntity(
                    playerName = player.name,
                    generation = player.generation,
                    clubName = myAcademy?.academyName ?: "Academy",
                    seasonYear = gameState.currentSeason,
                    competitionName = "Youth Academy League",
                    isMajor = false
                ))
            }
        }
    }

    /**
     * Advance Time by One Month: Simulate all matches in the current month.
     */
    suspend fun advanceMonth(isAutoSim: Boolean = false): String {
        return legacy_advanceMonth(isAutoSim)
    }

    suspend fun legacy_advanceMonth(isAutoSim: Boolean = false): String {
        var narrativeReport = ""
        db.withTransaction {
            val player = dao.getPlayerSync() ?: return@withTransaction
            val gameState = dao.getGameStateSync() ?: return@withTransaction
            val currentMonth = gameState.currentMonthIndex

            if (gameState.activeChoicePrompt != null) {
                narrativeReport = "CHOICE_PENDING"
                return@withTransaction
            }

            if (player.pendingNewManagerNotice && gameState.activeChoicePrompt == null) {
                triggerNewManagerFirstMeetingInternal(player, gameState)
                narrativeReport = "CHOICE_PENDING"
                return@withTransaction
            }

            // Narrative logs for this month
            val monthLogs = mutableListOf<String>()
            val monthName = getMonthName(currentMonth)
            monthLogs.add("--- Season ${gameState.currentSeason} (${formatSeasonYear(gameState.currentSeason)}), $monthName ---")

            // Unconditionally simulate youth & senior world fixtures every month
            simulateYouthWorldForMonth(currentMonth, player)
            val seniorExcludeId = if (player.careerPhase == PHASE_SENIOR) player.currentClubId else -1
            simulateWorldFixturesForMonth(currentMonth, seniorExcludeId, if (player.careerPhase == PHASE_SENIOR) monthLogs else null)

            // Phase 1: STREET FOOTBALL
            if (player.careerPhase == PHASE_STREET) {
                val numMatches = Random.nextInt(1, 3)
                val opponents = listOf("Eastside Cage", "Dockyard FC", "Concrete Kings", "Underpass Crew", "Street Legends")
                for (i in 0 until numMatches) {
                    val opp = opponents.random()
                    val pGoals = if (Random.nextFloat() < (player.finishing / 100f)) Random.nextInt(1, 3) else 0
                    val pAssists = if (Random.nextFloat() < (player.passing / 100f)) Random.nextInt(0, 2) else 0
                    val pMvp = pGoals + pAssists >= 2 && Random.nextFloat() < 0.6f

                    player.youthGoals += pGoals
                    player.youthAssists += pAssists
                    if (pMvp) player.youthMvps += 1
                    player.streetFootballGamesThisSeason += 1

                    val summaryPrefix = when (player.backgroundStory) {
                        "School Team" -> "School team discipline: "
                        "Family Club" -> "Family tradition: "
                        else -> "Cage grit: "
                    }
                    val summary = "$summaryPrefix Scored $pGoals goals, $pAssists assists in a match vs $opp."
                    dao.insertStreetFootballGame(
                        StreetFootballGameEntity(
                            seasonNumber = gameState.currentSeason,
                            monthIndex = currentMonth,
                            opponentName = opp,
                            playerGoals = pGoals,
                            playerAssists = pAssists,
                            playerMvp = pMvp,
                            resultSummary = summary
                        )
                    )
                }

                val isTransferWindow = isTransferWindowOpen(currentMonth)
                val academies = dao.getAllYouthAcademiesSync()
                val rejectedIds = if (player.rejectedAcademyIds.isBlank()) emptySet() else player.rejectedAcademyIds.split(",").toSet()
                val eligibleAcademies = academies.filter { it.minScoutOvr <= player.ovr && !rejectedIds.contains(it.id.toString()) }

                if (isTransferWindow && eligibleAcademies.isNotEmpty() && gameState.persistedYouthOffers.isNullOrEmpty()) {
                    val offers = eligibleAcademies.take(3).mapIndexed { index, ac ->
                        val report = YouthCareerLogic.generateScoutReportText(player, ac.academyName, ac.youthRivalName, ac.youthRivalOvr, index, ac.parentReputation)
                        YouthScoutOffer(
                            academyId = ac.id,
                            academyName = ac.academyName,
                            parentClubReputation = ac.parentReputation,
                            parentClubReputationPoints = ac.parentReputationPoints,
                            youthRivalName = ac.youthRivalName,
                            youthRivalOvr = ac.youthRivalOvr,
                            minScoutOvr = ac.minScoutOvr,
                            scoutReport = report
                        )
                    }
                    gameState.persistedYouthOffers = YouthCareerLogic.serializeYouthOffers(offers)
                    monthLogs.add("📩 TRANSFER WINDOW SCOUT OFFER: ${offers.size} Youth Academy scouts have sent trial invitations!")
                }

                val nextMonth = currentMonth + 1
                if (nextMonth <= 9) {
                    gameState.currentMonthIndex = nextMonth
                } else {
                    checkAndAwardYouthTrophy(player, gameState)
                    resetYouthLeagueForNewSeason()
                    val endReport = handleEndOfSeason(player, gameState)
                    gameState.narrativeLog = capNarrativeLog(endReport + "\n\n" + gameState.narrativeLog)
                    gameState.currentSeason += 1
                    gameState.currentMonthIndex = 0
                    player.age += 1
                    player.streetFootballGamesThisSeason = 0

                    if (player.age >= 19 && player.careerPhase != PHASE_SENIOR) {
                        gameState.youthCareerEnded = true
                        monthLogs.add("⚠️ CAREER ENDED: You reached Age 19 without signing a senior professional contract!")
                    }
                }
                player.hasTrainedThisMonth = 0
                applyMonthlyFanRepFormHook(player)
                dao.updatePlayer(player)
                dao.updateGameState(gameState)
                generateSocialPostsForMonth(player, null, gameState)
                narrativeReport = monthLogs.joinToString("\n")
                return@withTransaction
            }

            // Phase 2: YOUTH ACADEMY
            if (player.careerPhase == PHASE_YOUTH) {
                val isTransferWindowYouth = isTransferWindowOpen(currentMonth)
                if (isTransferWindowYouth && player.age >= 16 && gameState.persistedSeniorYouthOffers.isNullOrEmpty()) {
                    val allClubs = dao.getAllClubsSync()
                    val rejectedIds = if (player.rejectedSeniorClubIds.isBlank()) emptySet() else player.rejectedSeniorClubIds.split(",").toSet()
                    val eligibleClubs = allClubs.filter { club ->
                        !rejectedIds.contains(club.id.toString()) &&
                        (player.ovr >= 45 || club.reputation == "SMALL" || (player.ovr >= 55 && club.reputation == "MID") || (player.ovr >= 68 && club.reputation == "BIG") || (player.ovr >= 78 && club.reputation == "ELITE"))
                    }

                    if (eligibleClubs.isNotEmpty()) {
                        val offers = eligibleClubs.shuffled().take(3).map { club ->
                            val contractYears = Random.nextInt(2, 5)
                            val targetGplusA = (player.ovr / 5).coerceIn(5, 25)
                            val report = "Professional evaluators from ${club.name} have monitored your youth performances and submitted a pro contract offer."
                            YouthToSeniorOffer(
                                clubId = club.id,
                                clubName = club.name,
                                clubReputation = club.reputation,
                                rivalStrikerName = club.rivalStrikerName,
                                rivalStrikerOvr = club.rivalStrikerOvr,
                                contractYears = contractYears,
                                targetGplusA = targetGplusA,
                                scoutReport = report
                            )
                        }
                        gameState.persistedSeniorYouthOffers = YouthCareerLogic.serializeSeniorYouthOffers(offers)
                        monthLogs.add("📩 PROFESSIONAL CONTRACT OFFER: ${offers.size} senior clubs are offering professional contracts!")
                    }
                }

                if (player.age == 25 && player.playstyleAssignedAtAge == null) {
                    YouthCareerLogic.evaluatePlaystyleAssignment(player)
                    monthLogs.add("🌟 PLAYSTYLE EVOLUTION: Your career experience has crystallized your Playstyle: ${player.assignedPlaystyle ?: "Balanced"}!")
                }

                val nextMonth = currentMonth + 1
                if (nextMonth <= 9) {
                    gameState.currentMonthIndex = nextMonth
                } else {
                    checkAndAwardYouthTrophy(player, gameState)
                    resetYouthLeagueForNewSeason()
                    val endReport = handleEndOfSeason(player, gameState)
                    gameState.narrativeLog = capNarrativeLog(endReport + "\n\n" + gameState.narrativeLog)
                    gameState.currentSeason += 1
                    gameState.currentMonthIndex = 0
                    player.age += 1

                    if (player.age >= 19 && player.careerPhase != PHASE_SENIOR) {
                        gameState.youthCareerEnded = true
                        monthLogs.add("⚠️ YOUTH CAREER ENDED: You reached Age 19 without signing a senior professional contract!")
                    }
                }
                player.hasTrainedThisMonth = 0
                applyMonthlyFanRepFormHook(player)
                dao.updatePlayer(player)
                dao.updateGameState(gameState)
                generateSocialPostsForMonth(player, null, gameState)
                narrativeReport = monthLogs.joinToString("\n")
                return@withTransaction
            }

            // 1. Fetch and simulate all matches scheduled for this month
            val fixturesToSimulate = dao.getFixturesForMonthSync(currentMonth)
            val allClubsMap = dao.getAllClubsSync().associateBy { it.id }

            // Split into player's matches vs others
            val playerClubId = player.currentClubId
            val isAcademy = player.age in 13..15
            val playerMatches = if (isAcademy) emptyList() else fixturesToSimulate.filter { it.homeClubId == playerClubId || it.awayClubId == playerClubId }
            val otherMatches = if (isAcademy) fixturesToSimulate else fixturesToSimulate.filter { it.homeClubId != playerClubId && it.awayClubId != playerClubId }

            // Track if player played any matches this month
            var playedMatchCount = 0
            var goalsThisMonth = 0
            var assistsThisMonth = 0
            var mvpsThisMonth = 0

            // Match simulation
            for (fixture in playerMatches) {
                val homeClub = allClubsMap[fixture.homeClubId] ?: continue
                val awayClub = allClubsMap[fixture.awayClubId] ?: continue

                // Simulate team score
                val result = simulateTeamMatch(homeClub, awayClub)
                fixture.homeScore = result.first
                fixture.awayScore = result.second
                fixture.isSimulated = true

                // Calculate Rotation for Player in this match
                val isPlayerHome = fixture.homeClubId == playerClubId
                val myClub = if (isPlayerHome) homeClub else awayClub
                val oppClub = if (isPlayerHome) awayClub else homeClub

                // Get current rival striker OVR
                val rivalOvr = myClub.rivalStrikerOvr

                // Rotation calculation
                val isAcademy = player.age in 13..15
                val rivalOvrBuff = if (player.rivalRelationship < 30) 4 else 0
                val rivalEffectiveOvr = (if (isAcademy) 30 else rivalOvr) + rivalOvrBuff
                val diff = (player.ovr + player.form) - rivalEffectiveOvr

                // Base start prob
                var startProb = 0.5f + (diff * 0.035f)
                val isHighStakes = fixture.competition != "LEAGUE" || (currentMonth == 9)
                if (isHighStakes) {
                    startProb = 0.5f + (diff * 0.05f)
                }

                // managerTrust effects
                if (player.managerTrust >= 70) {
                    startProb += 0.20f
                    if (player.form >= -2 && player.fatigue <= 40) {
                        startProb = startProb.coerceAtLeast(0.65f)
                    }
                } else if (player.managerTrust < 30) {
                    startProb -= 0.30f
                    startProb = startProb.coerceAtMost(0.15f)
                }

                startProb = startProb.coerceIn(if (isHighStakes) 0.02f else 0.05f, if (isHighStakes) 0.98f else 0.95f)

                val rand = Random.nextFloat()
                if (rand < startProb) {
                    // Starts!
                    fixture.playerCameOnMinute = 0
                    playedMatchCount++
                } else {
                    // Check if subbed in (60% chance)
                    if (Random.nextFloat() < 0.60f) {
                        fixture.playerCameOnMinute = Random.nextInt(60, 81)
                        playedMatchCount++
                    } else {
                        // Benched
                        fixture.playerCameOnMinute = null
                    }
                }

                // Simulate Player stats if they played
                val minutes = when {
                    fixture.playerCameOnMinute == 0 -> 90
                    fixture.playerCameOnMinute != null -> 90 - fixture.playerCameOnMinute!!
                    else -> 0
                }

                if (minutes > 0) {
                    val myTeamGoals = if (isPlayerHome) result.first else result.second
                    
                    // Modifiers based on club reps
                    val myTeamRepFactor = getRepMultiplier(myClub.reputation)
                    val oppRepFactor = getOpponentMultiplier(oppClub.reputation)
                    
                    // Personal goal probability
                    val goalProb = (player.finishing / 100.0f) * myTeamRepFactor * oppRepFactor * (1.0f + (player.form * 0.05f))
                    val assistProb = (player.passing / 100.0f) * myTeamRepFactor * oppRepFactor * (1.0f + (player.form * 0.05f))

                    var playerGoals = 0
                    var playerAssists = 0

                    // Distribute goals/assists based on how many goals the team scored
                    if (myTeamGoals > 0) {
                        for (g in 1..myTeamGoals) {
                            // Striker goal chance
                            if (Random.nextFloat() < (goalProb * 0.45f) && playerGoals < myTeamGoals) {
                                playerGoals++
                            } else if (Random.nextFloat() < (assistProb * 0.30f) && (playerGoals + playerAssists < myTeamGoals)) {
                                playerAssists++
                            }
                        }
                    }

                    fixture.playerGoals = playerGoals
                    fixture.playerAssists = playerAssists
                    val startMin = fixture.playerCameOnMinute ?: 0
                    fixture.goalMinutes = if (playerGoals > 0) List(playerGoals) { Random.nextInt(if (startMin == 0) 1 else startMin, 91) }.sorted().joinToString(",") else null
                    fixture.assistMinutes = if (playerAssists > 0) List(playerAssists) { Random.nextInt(if (startMin == 0) 1 else startMin, 91) }.sorted().joinToString(",") else null
                    goalsThisMonth += playerGoals
                    assistsThisMonth += playerAssists

                    // Calculate Match Rating and MVP
                    val matchRating = 6.0f + (playerGoals * 1.5f) + (playerAssists * 1.0f) + (player.finishing + player.pace + player.physical) * 0.005f + (Random.nextFloat() * 0.5f)
                    val ratingStr = "%.1f".format(matchRating)

                    val earnedMvp = matchRating > 8.0f && (playerGoals > 0 || playerAssists > 0) && (Random.nextFloat() < 0.65f)
                    fixture.playerMvp = earnedMvp
                    if (earnedMvp) {
                        mvpsThisMonth++
                    }

                    // Shift form: positive for goals/assists/MVP, negative for quiet games
                    var formDelta = -1
                    if (playerGoals > 0 || playerAssists > 0) formDelta = 1
                    if (playerGoals >= 2 || earnedMvp) formDelta = 2
                    player.form = (player.form + formDelta).coerceIn(-5, 5)

                    // Logs details
                    val matchTypeStr = when (fixture.competition) {
                        "LEAGUE" -> "League match"
                        "CHAMPIONS_LEAGUE" -> "Champions League"
                        "EUROPA_LEAGUE" -> "Europa League"
                        "CONFERENCE_LEAGUE" -> "Conference League"
                        "SUPER_CUP" -> "Super Cup"
                        else -> "Cup match"
                    }

                    val roleStr = if (fixture.playerCameOnMinute == 0) "Started" else "Subbed in (${fixture.playerCameOnMinute}')"
                    val contribStr = if (playerGoals > 0 || playerAssists > 0) {
                        "${playerGoals}G, ${playerAssists}A" + (if (earnedMvp) " (MVP⭐)" else "")
                    } else "No goals/assists"

                    val scoreStr = "${fixture.homeScore}-${fixture.awayScore}"
                    val outcomeChar = getOutcomeChar(isPlayerHome, fixture.homeScore!!, fixture.awayScore!!)

                    monthLogs.add(
                        "⚽ $matchTypeStr vs ${oppClub.name}: ($outcomeChar) $scoreStr. $roleStr, Rating: $ratingStr. $contribStr."
                    )
                } else {
                    // Benched
                    val outcomeChar = getOutcomeChar(isPlayerHome, fixture.homeScore!!, fixture.awayScore!!)
                    val scoreStr = "${fixture.homeScore}-${fixture.awayScore}"
                    monthLogs.add("🪑 Benched vs ${oppClub.name}: ($outcomeChar) $scoreStr.")
                    player.form = (player.form - 1).coerceIn(-5, 5) // drop in form
                }

                dao.updateFixture(fixture)

                // Update Standings if domestic league match
                if (fixture.competition == "LEAGUE") {
                    updateDomesticStandings(fixture)
                }
            }

            // Simulate all OTHER matches (not involving player)
            for (fixture in otherMatches) {
                val homeClub = allClubsMap[fixture.homeClubId] ?: continue
                val awayClub = allClubsMap[fixture.awayClubId] ?: continue

                val result = simulateTeamMatch(homeClub, awayClub)
                fixture.homeScore = result.first
                fixture.awayScore = result.second
                fixture.isSimulated = true

                dao.updateFixture(fixture)

                if (fixture.competition == "LEAGUE") {
                    updateDomesticStandings(fixture)
                }
            }

            // Update player season/career aggregates
            player.gamesPlayed += playedMatchCount
            player.goals += goalsThisMonth
            player.assists += assistsThisMonth
            player.mvps += mvpsThisMonth

            player.seasonGamesPlayed += playedMatchCount
            player.seasonGoals += goalsThisMonth
            player.seasonAssists += assistsThisMonth
            player.seasonMvps += mvpsThisMonth

            player.clubGamesPlayed += playedMatchCount
            player.clubGoals += goalsThisMonth
            player.clubAssists += assistsThisMonth

            // Narrative summary of month
            if (isAcademy) {
                monthLogs.add("\n🎓 ACADEMY PROGRESS: You played matches with the youth squad. Official stats will begin tracking once you turn 16!")
            } else if (playedMatchCount > 0) {
                val formIndicator = when {
                    player.form >= 3 -> "🔥 Hot Form"
                    player.form <= -3 -> "❄️ Cold Form"
                    else -> "⚡ Stable Form"
                }
                monthLogs.add("\n📊 Monthly Summary: Played $playedMatchCount games, scored $goalsThisMonth goals, $assistsThisMonth assists. ($formIndicator)")
            } else {
                monthLogs.add("\n📊 Monthly Summary: Did not play any matches this month. Form continues to slide.")
            }

            // 2. Perform Knockout Progression Checks
            // If month is October (2) -> prelims are complete, generate Quarterfinals
            if (currentMonth == 2) {
                progressKnockoutRound("CHAMPIONS_LEAGUE", 10, 8, 3)
                progressKnockoutRound("EUROPA_LEAGUE", 10, 8, 3)
                progressKnockoutRound("CONFERENCE_LEAGUE", 10, 8, 3)
                monthLogs.add("🏆 European cup preliminary rounds finished. Quarter-final brackets are drawn!")
            }
            // If month is December (4) -> QF finished, generate Semifinals
            if (currentMonth == 4) {
                progressKnockoutRound("CHAMPIONS_LEAGUE", 8, 4, 5)
                progressKnockoutRound("EUROPA_LEAGUE", 8, 4, 5)
                progressKnockoutRound("CONFERENCE_LEAGUE", 8, 4, 5)
                monthLogs.add("🏆 European cup quarter-finals finished. Semi-final brackets are drawn!")
            }
            // If month is February (6) -> SF finished, generate Finals (played in Month 8 / April)
            if (currentMonth == 6) {
                progressKnockoutRound("CHAMPIONS_LEAGUE", 4, 2, 8)
                progressKnockoutRound("EUROPA_LEAGUE", 4, 2, 8)
                progressKnockoutRound("CONFERENCE_LEAGUE", 4, 2, 8)
                monthLogs.add("🏆 European cup semi-finals finished. The finalists are locked in for April!")
            }
            // If month is April (8) -> Finals finished, schedule Super Cup in Month 9 / May
            if (currentMonth == 8) {
                scheduleSuperCup()
                monthLogs.add("🏆 Champions League and Europa League winners are crowned! Super Cup scheduled for May.")
            }

            // Check for mid-contract early termination
            // "if a player receives meaningful minutes (roughly 50%+) but performs drastically below target (not just a slight miss) mid-contract, the club may terminate early."
            if (player.contractYearsRemaining > 1 && player.seasonGamesPlayed >= 10) {
                val availableMinutes = 30 * 90f // roughly 30 matches
                val playedPercentage = (player.seasonGamesPlayed * 90f) / availableMinutes
                if (playedPercentage >= 0.50f) {
                    val actualTally = player.seasonGoals + player.seasonAssists
                    // If target is established and we have done 10+ games, we check if they are way under
                    val progressRatio = actualTally.toFloat() / max(1, player.contractTargetGoalsAssists)
                    // If less than 15% of progress on target, trigger early termination!
                    if (progressRatio < 0.15f && player.contractTargetGoalsAssists >= 5) {
                        player.contractYearsRemaining = 0
                        player.contractTargetGoalsAssists = 0
                        monthLogs.add("\n⚠️ CONTRACT TERMINATED: Due to poor performances (scoring only $actualTally in ${player.seasonGamesPlayed} games), your club has terminated your contract early! You are now a free agent.")
                    }
                }
            }

            // Morale monthly drift ±1 toward 50
            if (player.morale < 50) {
                player.morale += 1
            } else if (player.morale > 50) {
                player.morale -= 1
            }

            // Monthly fan rep form hook (nudge by ±1 if >=75 or <=25)
            applyMonthlyFanRepFormHook(player)

            // Save player stats
            dao.updatePlayer(player)

            // Generate monthly social feed posts based on updated stats and club
            val currentClub = if (player.currentClubId != 0) dao.getClubById(player.currentClubId) else null
            generateSocialPostsForMonth(player, currentClub, gameState)

            // Random monthly Choice Event (60% chance, skipped during auto sim)
            val triggerChoice = if (isAutoSim) false else (Random.nextFloat() < 0.60f)
            if (triggerChoice) {
                val myClub = dao.getClubById(player.currentClubId)
                val clubName = myClub?.name ?: "Club"
                val rivalName = myClub?.rivalStrikerName ?: "Rival"
                val recentIds = if (gameState.recentChoiceEventIds.isBlank()) emptyList() else gameState.recentChoiceEventIds.split(",")
                val rawEvent = generateMonthlyUnifiedChoiceEvent(player, clubName, rivalName, recentIds)
                val updatedRecent = (listOf(rawEvent.id) + recentIds).take(3)
                gameState.recentChoiceEventIds = updatedRecent.joinToString(",")
                
                gameState.activeChoicePrompt = rawEvent.prompt
                gameState.activeChoiceOption1 = rawEvent.option1
                gameState.activeChoiceOption2 = rawEvent.option2
                gameState.activeChoiceOption3 = rawEvent.option3
                gameState.activeChoiceOutcome1 = rawEvent.outcome1
                gameState.activeChoiceOutcome2 = rawEvent.outcome2
                gameState.activeChoiceOutcome3 = rawEvent.outcome3
                
                gameState.activeChoiceFormMod1 = rawEvent.formMod1
                gameState.activeChoiceFinishingMod1 = rawEvent.finishingMod1
                gameState.activeChoiceTechniqueMod1 = rawEvent.techniqueMod1
                gameState.activeChoiceMoraleMod1 = rawEvent.moraleMod1
                gameState.activeChoiceFanRepMod1 = rawEvent.fanRepMod1
                gameState.activeChoiceManagerTrustMod1 = rawEvent.managerTrustMod1
                gameState.activeChoiceRivalRelMod1 = rawEvent.rivalRelMod1
                gameState.activeChoiceFatigueMod1 = rawEvent.fatigueMod1
                
                gameState.activeChoiceFormMod2 = rawEvent.formMod2
                gameState.activeChoiceFinishingMod2 = rawEvent.finishingMod2
                gameState.activeChoiceTechniqueMod2 = rawEvent.techniqueMod2
                gameState.activeChoiceMoraleMod2 = rawEvent.moraleMod2
                gameState.activeChoiceFanRepMod2 = rawEvent.fanRepMod2
                gameState.activeChoiceManagerTrustMod2 = rawEvent.managerTrustMod2
                gameState.activeChoiceRivalRelMod2 = rawEvent.rivalRelMod2
                gameState.activeChoiceFatigueMod2 = rawEvent.fatigueMod2
                
                gameState.activeChoiceFormMod3 = rawEvent.formMod3
                gameState.activeChoiceFinishingMod3 = rawEvent.finishingMod3
                gameState.activeChoiceTechniqueMod3 = rawEvent.techniqueMod3
                gameState.activeChoiceMoraleMod3 = rawEvent.moraleMod3
                gameState.activeChoiceFanRepMod3 = rawEvent.fanRepMod3
                gameState.activeChoiceManagerTrustMod3 = rawEvent.managerTrustMod3
                gameState.activeChoiceRivalRelMod3 = rawEvent.rivalRelMod3
                gameState.activeChoiceFatigueMod3 = rawEvent.fatigueMod3
                
                gameState.activeChoicePendingMonthLogs = monthLogs.joinToString("\n")
                
                dao.updateGameState(gameState)
                narrativeReport = "CHOICE_TRIGGERED"
            } else {
                // 3. Update GameState normally
                val nextMonth = currentMonth + 1
                narrativeReport = monthLogs.joinToString("\n")
                gameState.narrativeLog = capNarrativeLog(narrativeReport + "\n\n" + gameState.narrativeLog)

                // Decrement manager talk cooldown
                if (gameState.managerTalkCooldownMonths > 0) {
                    gameState.managerTalkCooldownMonths -= 1
                }

                // Clear persisted transfer offers since we are leaving the current month
                gameState.persistedTransferOffers = null

                if (nextMonth <= 9) {
                    gameState.currentMonthIndex = nextMonth
                    if (nextMonth == 5) {
                        player.hasTransferredThisWindow = false
                        dao.updatePlayer(player)
                    }
                    dao.updateGameState(gameState)
                } else {
                    // END OF SEASON TRIGGERS!
                    // Month is May (9), advance season
                    checkAndAwardYouthTrophy(player, gameState)
                    resetYouthLeagueForNewSeason()
                    val endReport = handleEndOfSeason(player, gameState)
                    gameState.narrativeLog = capNarrativeLog(endReport + "\n\n" + gameState.narrativeLog)
                    gameState.currentSeason += 1
                    gameState.currentMonthIndex = 0
                    player.hasTransferredThisWindow = false
                    dao.updatePlayer(player)
                    dao.updateGameState(gameState)
                }
            }
        }
        return narrativeReport
    }

    /**
     * Complete the domestic league table standing updating
     */
    private fun updateStandingInMemory(homeStanding: StandingEntity, awayStanding: StandingEntity, homeScore: Int, awayScore: Int) {
        homeStanding.played += 1
        awayStanding.played += 1

        homeStanding.goalsFor += homeScore
        homeStanding.goalsAgainst += awayScore

        awayStanding.goalsFor += awayScore
        awayStanding.goalsAgainst += homeScore

        when {
            homeScore > awayScore -> {
                homeStanding.wins += 1
                homeStanding.points += 3
                awayStanding.losses += 1
            }
            homeScore < awayScore -> {
                awayStanding.wins += 1
                awayStanding.points += 3
                homeStanding.losses += 1
            }
            else -> {
                homeStanding.draws += 1
                homeStanding.points += 1
                awayStanding.draws += 1
                awayStanding.points += 1
            }
        }
    }

    private suspend fun updateDomesticStandings(fixture: FixtureEntity) {
        val homeScore = fixture.homeScore ?: return
        val awayScore = fixture.awayScore ?: return

        val allStandings = dao.getAllStandingsSync()
        val homeStanding = allStandings.find { it.clubId == fixture.homeClubId } ?: return
        val awayStanding = allStandings.find { it.clubId == fixture.awayClubId } ?: return

        updateStandingInMemory(homeStanding, awayStanding, homeScore, awayScore)

        dao.updateStandings(listOf(homeStanding, awayStanding))
    }

    private fun resolveTiedMatchWithExtraTimeAndPens(fixture: FixtureEntity) {
        fixture.wentToExtraTime = true
        
        // Extra Time: Generate additional goals (e.g., 15% chance per team to score a goal)
        var etHomeGoals = 0
        var etAwayGoals = 0
        if (Random.nextFloat() < 0.15f) etHomeGoals++
        if (Random.nextFloat() < 0.15f) etAwayGoals++
        
        fixture.homeScore = (fixture.homeScore ?: 0) + etHomeGoals
        fixture.awayScore = (fixture.awayScore ?: 0) + etAwayGoals
        
        if (fixture.homeScore == fixture.awayScore) {
            // Still tied after extra time: Penalty shootout!
            val homeWins = Random.nextBoolean()
            if (homeWins) {
                val winPens = Random.nextInt(3, 6) // 3 to 5
                fixture.homePens = winPens
                fixture.awayPens = winPens - 1
            } else {
                val winPens = Random.nextInt(3, 6) // 3 to 5
                fixture.awayPens = winPens
                fixture.homePens = winPens - 1
            }
        } else {
            fixture.homePens = null
            fixture.awayPens = null
        }
    }

    private fun checkAndResolveFixtureTie(fixture: FixtureEntity) {
        val isKnockoutFinal = fixture.competition in listOf("CHAMPIONS_LEAGUE", "EUROPA_LEAGUE", "CONFERENCE_LEAGUE") && fixture.round == 2
        val isSuperCup = fixture.competition == "SUPER_CUP"
        if ((isKnockoutFinal || isSuperCup) && fixture.isSimulated && fixture.homeScore == fixture.awayScore && fixture.wentToExtraTime != true) {
            resolveTiedMatchWithExtraTimeAndPens(fixture)
        }
    }

    /**
     * Simulate knockout progression: Check leg scores, decide winners, draw next round.
     */
    private suspend fun progressKnockoutRound(comp: String, currentSize: Int, nextSize: Int, playMonth: Int) {
        val fixtures = dao.getAllFixturesSync().filter { it.competition == comp && it.round == currentSize && it.isSimulated }
        val allClubsMap = dao.getAllClubsSync().associateBy { it.id }

        android.util.Log.d("FootballCareer", "KNOCKOUT_SIM: Progressing $comp from round of $currentSize to round of $nextSize. Simulated fixtures found: ${fixtures.size}")

        // Find aggregate winners
        // Fixtures are in pairs: Leg 1 and Leg 2
        val matchPairs = fixtures.groupBy { if (it.homeClubId < it.awayClubId) "${it.homeClubId}_${it.awayClubId}" else "${it.awayClubId}_${it.homeClubId}" }
        val winners = mutableListOf<ClubEntity>()

        for ((pairKey, legs) in matchPairs) {
            if (legs.size < 2) {
                android.util.Log.w("FootballCareer", "KNOCKOUT_SIM WARNING: Incomplete fixture pair for matchup key $pairKey in $comp round $currentSize. Legs count: ${legs.size}")
                continue
            }
            val leg1 = legs[0]
            val leg2 = legs[1]

            // Leg 1: Team A is home, Team B is away.
            // Leg 2: Team B is home, Team A is away.
            val teamAId = leg1.homeClubId
            val teamBId = leg1.awayClubId

            val l1 = if (leg1.leg == 1) leg1 else leg2
            val l2 = if (leg2.leg == 2) leg2 else leg1

            val scoreA = (l1.homeScore ?: 0) + (l2.awayScore ?: 0)
            val scoreB = (l1.awayScore ?: 0) + (l2.homeScore ?: 0)

            val winnerId = if (scoreA > scoreB) {
                teamAId
            } else if (scoreA < scoreB) {
                teamBId
            } else {
                // Aggregate is level: simulate Extra Time on Leg 2!
                l2.wentToExtraTime = true
                
                // Extra Time goals
                var etHomeGoals = 0 // Team B is home of Leg 2
                var etAwayGoals = 0 // Team A is away of Leg 2
                if (Random.nextFloat() < 0.15f) etHomeGoals++
                if (Random.nextFloat() < 0.15f) etAwayGoals++
                
                // Add extra time goals to Leg 2 scores
                l2.homeScore = (l2.homeScore ?: 0) + etHomeGoals
                l2.awayScore = (l2.awayScore ?: 0) + etAwayGoals
                dao.updateFixture(l2)
                
                // Recalculate aggregate scores after extra time
                val newScoreA = (l1.homeScore ?: 0) + (l2.awayScore ?: 0) // Team A (away in leg 2)
                val newScoreB = (l1.awayScore ?: 0) + (l2.homeScore ?: 0) // Team B (home in leg 2)
                
                if (newScoreA > newScoreB) {
                    teamAId
                } else if (newScoreA < newScoreB) {
                    teamBId
                } else {
                    // Still level after extra time: Penalty shootout!
                    val aWinsPens = Random.nextBoolean()
                    if (aWinsPens) {
                        val winPens = Random.nextInt(3, 6)
                        l2.awayPens = winPens // Team A is away in Leg 2
                        l2.homePens = winPens - 1 // Team B is home in Leg 2
                        dao.updateFixture(l2)
                        teamAId
                    } else {
                        val winPens = Random.nextInt(3, 6)
                        l2.homePens = winPens // Team B is home in Leg 2
                        l2.awayPens = winPens - 1 // Team A is away in Leg 2
                        dao.updateFixture(l2)
                        teamBId
                    }
                }
            }

            val winnerClub = allClubsMap[winnerId]
            winnerClub?.let { winners.add(it) }

            android.util.Log.d(
                "FootballCareer",
                "  MATCHUP RESULT: ${allClubsMap[teamAId]?.name} vs ${allClubsMap[teamBId]?.name} inside $comp\n" +
                "    Leg 1: ${l1.homeScore} - ${l1.awayScore} (Home: ${allClubsMap[l1.homeClubId]?.name})\n" +
                "    Leg 2: ${l2.homeScore} - ${l2.awayScore} (Home: ${allClubsMap[l2.homeClubId]?.name})\n" +
                "    Aggregate: $scoreA - $scoreB. Winner: ${winnerClub?.name}"
            )
        }

        // Combine prelim round winners with direct-entry teams
        if (currentSize == 10) {
            val directFixtures = dao.getAllFixturesSync().filter { it.competition == comp && it.round == 11 }
            for (df in directFixtures) {
                val directClub = allClubsMap[df.homeClubId]
                if (directClub != null) {
                    winners.add(directClub)
                }
            }
        }

        // Draw next round
        val nextRoundFixtures = mutableListOf<FixtureEntity>()
        val shuffledWinners = winners.shuffled()

        android.util.Log.d("FootballCareer", "KNOCKOUT_SIM: Winners advancing to next round of $nextSize: ${shuffledWinners.joinToString { it.name }}")

        if (nextSize == 2) {
            // Final: Single Leg in playMonth (April / Month 8), round = 2
            if (shuffledWinners.size >= 2) {
                nextRoundFixtures.add(FixtureEntity(
                    country = "Europe",
                    competition = comp,
                    monthIndex = playMonth,
                    homeClubId = shuffledWinners[0].id,
                    awayClubId = shuffledWinners[1].id,
                    leg = 1,
                    round = 2
                ))
                android.util.Log.d("FootballCareer", "  DRAW FINAL: ${shuffledWinners[0].name} vs ${shuffledWinners[1].name} on month $playMonth")
            }
        } else {
            // Quarter or Semi-finals: Double legged
            for (i in 0 until shuffledWinners.size / 2) {
                val t1 = shuffledWinners[i * 2]
                val t2 = shuffledWinners[i * 2 + 1]

                // Leg 1
                nextRoundFixtures.add(FixtureEntity(
                    country = "Europe",
                    competition = comp,
                    monthIndex = playMonth,
                    homeClubId = t1.id,
                    awayClubId = t2.id,
                    leg = 1,
                    round = nextSize
                ))
                // Leg 2 (Next month)
                nextRoundFixtures.add(FixtureEntity(
                    country = "Europe",
                    competition = comp,
                    monthIndex = playMonth + 1,
                    homeClubId = t2.id,
                    awayClubId = t1.id,
                    leg = 2,
                    round = nextSize
                ))
                android.util.Log.d("FootballCareer", "  DRAW TIE: ${t1.name} vs ${t2.name} scheduled for months $playMonth (Leg 1) and ${playMonth + 1} (Leg 2)")
            }
        }
        dao.insertFixtures(nextRoundFixtures)
    }

    /**
     * Schedule single-legged Super Cup between CL Winner and EL Winner.
     * Played in Month 9 (May).
     */
    private suspend fun scheduleSuperCup() {
        val clFinal = dao.getAllFixturesSync().find { it.competition == "CHAMPIONS_LEAGUE" && it.round == 2 && it.isSimulated }
        val elFinal = dao.getAllFixturesSync().find { it.competition == "EUROPA_LEAGUE" && it.round == 2 && it.isSimulated }

        val clWinnerId = clFinal?.let {
            if ((it.homeScore ?: 0) > (it.awayScore ?: 0)) it.homeClubId
            else if ((it.homeScore ?: 0) < (it.awayScore ?: 0)) it.awayClubId
            else {
                if ((it.homePens ?: 0) > (it.awayPens ?: 0)) it.homeClubId else it.awayClubId
            }
        }
        val elWinnerId = elFinal?.let {
            if ((it.homeScore ?: 0) > (it.awayScore ?: 0)) it.homeClubId
            else if ((it.homeScore ?: 0) < (it.awayScore ?: 0)) it.awayClubId
            else {
                if ((it.homePens ?: 0) > (it.awayPens ?: 0)) it.homeClubId else it.awayClubId
            }
        }

        if (clWinnerId != null && elWinnerId != null) {
            val player = dao.getPlayerSync()
            val isPlayerClubInSuperCup = player != null && (player.currentClubId == clWinnerId || player.currentClubId == elWinnerId)

            if (isPlayerClubInSuperCup) {
                // If player is in it, don't simulate it and schedule for Month 9 (May) so player plays it manually
                val superCupFixture = FixtureEntity(
                    country = "Europe",
                    competition = "SUPER_CUP",
                    monthIndex = 9,
                    homeClubId = clWinnerId,
                    awayClubId = elWinnerId,
                    leg = 1,
                    round = 1,
                    isSimulated = false
                )
                dao.insertFixtures(listOf(superCupFixture))
            } else {
                // Otherwise, simulate it immediately for Month 9 (May)
                val superCupFixture = FixtureEntity(
                    country = "Europe",
                    competition = "SUPER_CUP",
                    monthIndex = 9,
                    homeClubId = clWinnerId,
                    awayClubId = elWinnerId,
                    leg = 1,
                    round = 1,
                    isSimulated = true
                )
                
                val homeClub = dao.getClubById(clWinnerId)
                val awayClub = dao.getClubById(elWinnerId)
                if (homeClub != null && awayClub != null) {
                    val result = simulateTeamMatch(homeClub, awayClub)
                    superCupFixture.homeScore = result.first
                    superCupFixture.awayScore = result.second
                    checkAndResolveFixtureTie(superCupFixture)
                    
                    dao.insertFixtures(listOf(superCupFixture))
                }
            }
        }
    }

    private suspend fun safeInsertTrophy(trophy: TrophyEntity) {
        val existing = dao.getAllTrophiesSync()
        val alreadyAwarded = existing.any {
            it.playerName == trophy.playerName &&
            it.generation == trophy.generation &&
            it.seasonYear == trophy.seasonYear &&
            it.competitionName == trophy.competitionName
        }
        if (!alreadyAwarded) {
            dao.insertTrophy(trophy)
            onTrophyUnlockedListener?.invoke(trophy)
        }
    }

    /**
     * End of Season Calculations.
     */
    private suspend fun handleEndOfSeason(player: PlayerEntity, gameState: GameStateEntity): String {
        // 1. Recalculate standings directly from this season's league fixtures to guarantee absolute score/points consistency
        val seasonFixtures = dao.getAllFixturesSync().filter { it.competition == "LEAGUE" && it.isSimulated }
        val allStandings = dao.getAllStandingsSync()
        
        for (st in allStandings) {
            st.played = 0
            st.wins = 0
            st.draws = 0
            st.losses = 0
            st.goalsFor = 0
            st.goalsAgainst = 0
            st.points = 0
        }
        val standingsByClub = allStandings.associateBy { it.clubId }
        
        for (fixture in seasonFixtures) {
            val homeStanding = standingsByClub[fixture.homeClubId] ?: continue
            val awayStanding = standingsByClub[fixture.awayClubId] ?: continue
            val homeScore = fixture.homeScore ?: 0
            val awayScore = fixture.awayScore ?: 0
            
            homeStanding.played += 1
            awayStanding.played += 1
            homeStanding.goalsFor += homeScore
            homeStanding.goalsAgainst += awayScore
            awayStanding.goalsFor += awayScore
            awayStanding.goalsAgainst += homeScore
            
            when {
                homeScore > awayScore -> {
                    homeStanding.wins += 1
                    homeStanding.points += 3
                    awayStanding.losses += 1
                }
                homeScore < awayScore -> {
                    awayStanding.wins += 1
                    awayStanding.points += 3
                    homeStanding.losses += 1
                }
                else -> {
                    homeStanding.draws += 1
                    homeStanding.points += 1
                    awayStanding.draws += 1
                    awayStanding.points += 1
                }
            }
        }
        dao.updateStandings(allStandings)

        val seasonLogs = mutableListOf<String>()
        seasonLogs.add("=====================================")
        seasonLogs.add("🏆 END OF SEASON ${gameState.currentSeason} SUMMARY 🏆")
        seasonLogs.add("=====================================")

        val allClubs = dao.getAllClubsSync()
        val allClubsMap = allClubs.associateBy { it.id }
        val myClub = allClubsMap[player.currentClubId]
        val trophyWinningClubIds = mutableSetOf<Int>()

        // Find League Winner for each country
        for (country in FictionalData.COUNTRIES) {
            val sortedStandings = dao.getStandingsByCountrySync(country)
            if (sortedStandings.isNotEmpty()) {
                val winnerClub = allClubsMap[sortedStandings[0].clubId]
                if (winnerClub != null) {
                    seasonLogs.add("🥇 ${country} Champion: ${winnerClub.name} (${sortedStandings[0].points} pts)")
                    trophyWinningClubIds.add(winnerClub.id)
                    
                    // If player is at this club, give league trophy
                    if (player.currentClubId == winnerClub.id) {
                        safeInsertTrophy(TrophyEntity(
                            playerName = player.name,
                            generation = player.generation,
                            clubName = winnerClub.name,
                            seasonYear = gameState.currentSeason,
                            competitionName = "${country} Domestic League",
                            isMajor = true
                        ))
                        seasonLogs.add("🏆 TROPHY UNLOCKED: You won the ${country} League with ${winnerClub.name}!")
                    }
                }

                // Adjust club reputations based on league position
                adjustClubsReputations(sortedStandings, allClubsMap)
            }
        }

        // Award European Trophies
        val allFixturesSnapshot = dao.getAllFixturesSync()
        val clFinal = allFixturesSnapshot.find { it.competition == "CHAMPIONS_LEAGUE" && it.round == 2 && it.isSimulated }
        if (clFinal != null) {
            val winnerId = if ((clFinal.homeScore ?: 0) > (clFinal.awayScore ?: 0)) {
                clFinal.homeClubId
            } else if ((clFinal.homeScore ?: 0) < (clFinal.awayScore ?: 0)) {
                clFinal.awayClubId
            } else {
                if ((clFinal.homePens ?: 0) > (clFinal.awayPens ?: 0)) clFinal.homeClubId else clFinal.awayClubId
            }
            val winnerClub = allClubsMap[winnerId]
            if (winnerClub != null) {
                trophyWinningClubIds.add(winnerClub.id)
                seasonLogs.add("🇪🇺 Champions League Winner: ${winnerClub.name}")
                if (player.currentClubId == winnerId) {
                    safeInsertTrophy(TrophyEntity(
                        playerName = player.name,
                        generation = player.generation,
                        clubName = winnerClub.name,
                        seasonYear = gameState.currentSeason,
                        competitionName = "Champions League",
                        isMajor = true
                    ))
                    seasonLogs.add("🏆 TROPHY UNLOCKED: Champions League Winner! Outstanding!")
                }
            }
        }

        val elFinal = allFixturesSnapshot.find { it.competition == "EUROPA_LEAGUE" && it.round == 2 && it.isSimulated }
        if (elFinal != null) {
            val winnerId = if ((elFinal.homeScore ?: 0) > (elFinal.awayScore ?: 0)) {
                elFinal.homeClubId
            } else if ((elFinal.homeScore ?: 0) < (elFinal.awayScore ?: 0)) {
                elFinal.awayClubId
            } else {
                if ((elFinal.homePens ?: 0) > (elFinal.awayPens ?: 0)) elFinal.homeClubId else elFinal.awayClubId
            }
            val winnerClub = allClubsMap[winnerId]
            if (winnerClub != null) {
                trophyWinningClubIds.add(winnerClub.id)
                seasonLogs.add("🇪🇺 Europa League Winner: ${winnerClub.name}")
                if (player.currentClubId == winnerId) {
                    safeInsertTrophy(TrophyEntity(
                        playerName = player.name,
                        generation = player.generation,
                        clubName = winnerClub.name,
                        seasonYear = gameState.currentSeason,
                        competitionName = "Europa League",
                        isMajor = false
                    ))
                    seasonLogs.add("🏆 TROPHY UNLOCKED: Europa League Winner!")
                }
            }
        }

        val confFinal = allFixturesSnapshot.find { it.competition == "CONFERENCE_LEAGUE" && it.round == 2 && it.isSimulated }
        if (confFinal != null) {
            val winnerId = if ((confFinal.homeScore ?: 0) > (confFinal.awayScore ?: 0)) {
                confFinal.homeClubId
            } else if ((confFinal.homeScore ?: 0) < (confFinal.awayScore ?: 0)) {
                confFinal.awayClubId
            } else {
                if ((confFinal.homePens ?: 0) > (confFinal.awayPens ?: 0)) confFinal.homeClubId else confFinal.awayClubId
            }
            val winnerClub = allClubsMap[winnerId]
            if (winnerClub != null) {
                trophyWinningClubIds.add(winnerClub.id)
                seasonLogs.add("🇪🇺 Conference League Winner: ${winnerClub.name}")
                if (player.currentClubId == winnerId) {
                    safeInsertTrophy(TrophyEntity(
                        playerName = player.name,
                        generation = player.generation,
                        clubName = winnerClub.name,
                        seasonYear = gameState.currentSeason,
                        competitionName = "Conference League",
                        isMajor = false
                    ))
                    seasonLogs.add("🏆 TROPHY UNLOCKED: Conference League Winner!")
                }
            }
        }

        val superCup = allFixturesSnapshot.find { it.competition == "SUPER_CUP" && it.isSimulated }
        if (superCup != null) {
            val winnerId = if ((superCup.homeScore ?: 0) > (superCup.awayScore ?: 0)) {
                superCup.homeClubId
            } else if ((superCup.homeScore ?: 0) < (superCup.awayScore ?: 0)) {
                superCup.awayClubId
            } else {
                if ((superCup.homePens ?: 0) > (superCup.awayPens ?: 0)) superCup.homeClubId else superCup.awayClubId
            }
            val winnerClub = allClubsMap[winnerId]
            if (winnerClub != null) {
                trophyWinningClubIds.add(winnerClub.id)
                seasonLogs.add("🇪🇺 European Super Cup Winner: ${winnerClub.name}")
                if (player.currentClubId == winnerId) {
                    safeInsertTrophy(TrophyEntity(
                        playerName = player.name,
                        generation = player.generation,
                        clubName = winnerClub.name,
                        seasonYear = gameState.currentSeason,
                        competitionName = "European Super Cup",
                        isMajor = false
                    ))
                    seasonLogs.add("🏆 TROPHY UNLOCKED: European Super Cup Winner!")
                }
            }
        }

        // European qualification for next season based on final standings we just generated/simulated prior to clearing
        val clTeams = mutableListOf<ClubEntity>()
        val elTeams = mutableListOf<ClubEntity>()
        val confTeams = mutableListOf<ClubEntity>()
        android.util.Log.d("FootballCareer", "--- European Qualification for Season ${gameState.currentSeason + 1} ---")
        for (country in FictionalData.COUNTRIES) {
            val sortedStandings = dao.getStandingsByCountrySync(country)
            android.util.Log.d("FootballCareer", "Country: $country, Standings Count: ${sortedStandings.size}")
            if (sortedStandings.size >= 6) {
                val cl1 = allClubsMap[sortedStandings[0].clubId]
                val cl2 = allClubsMap[sortedStandings[1].clubId]
                val el1 = allClubsMap[sortedStandings[2].clubId]
                val el2 = allClubsMap[sortedStandings[3].clubId]
                val conf1 = allClubsMap[sortedStandings[4].clubId]
                val conf2 = allClubsMap[sortedStandings[5].clubId]

                if (cl1 != null && cl2 != null && el1 != null && el2 != null && conf1 != null && conf2 != null) {
                    clTeams.add(cl1)
                    clTeams.add(cl2)
                    elTeams.add(el1)
                    elTeams.add(el2)
                    confTeams.add(conf1)
                    confTeams.add(conf2)

                    android.util.Log.d("FootballCareer", "  $country Qualified CL: ${cl1.name} (1st), ${cl2.name} (2nd)")
                    android.util.Log.d("FootballCareer", "  $country Qualified EL: ${el1.name} (3rd), ${el2.name} (4th)")
                    android.util.Log.d("FootballCareer", "  $country Qualified UECL: ${conf1.name} (5th), ${conf2.name} (6th)")
                }
            } else {
                android.util.Log.w("FootballCareer", "  $country has less than 6 standings entries! Size: ${sortedStandings.size}")
            }
        }

        // Apply Seasonal Stat Growth or Decline
        applySeasonStatGrowthAndDecline(player, myClub)

        // Increment age
        player.age += 1

        // Contract Evaluation
        evaluateContractEndSeason(player, myClub, seasonLogs, gameState.currentSeason)

        // --- Club Records & Season History Snapshotting ---
        val currentClub = allClubsMap[player.currentClubId]
        if (currentClub != null) {
            checkAndApplyPlayerClubRecords(player, currentClub, gameState.currentSeason)
        }

        for (club in allClubs) {
            evolveClubRecords(club, gameState.currentSeason, trophyWinningClubIds.contains(club.id))
        }

        // Determine final tournament winners and runners-up for this season
        var clWinnerId: Int? = null
        var clRunnerUpId: Int? = null
        val clFinalFx = dao.getAllFixturesSync().find { it.competition == "CHAMPIONS_LEAGUE" && it.round == 2 && it.isSimulated }
        if (clFinalFx != null) {
            val isHomeWinner = (clFinalFx.homeScore ?: 0) > (clFinalFx.awayScore ?: 0) || 
                               ((clFinalFx.homeScore ?: 0) == (clFinalFx.awayScore ?: 0) && (clFinalFx.homePens ?: 0) > (clFinalFx.awayPens ?: 0))
            if (isHomeWinner) {
                clWinnerId = clFinalFx.homeClubId
                clRunnerUpId = clFinalFx.awayClubId
            } else {
                clWinnerId = clFinalFx.awayClubId
                clRunnerUpId = clFinalFx.homeClubId
            }
        }

        var elWinnerId: Int? = null
        var elRunnerUpId: Int? = null
        val elFinalFx = dao.getAllFixturesSync().find { it.competition == "EUROPA_LEAGUE" && it.round == 2 && it.isSimulated }
        if (elFinalFx != null) {
            val isHomeWinner = (elFinalFx.homeScore ?: 0) > (elFinalFx.awayScore ?: 0) || 
                               ((elFinalFx.homeScore ?: 0) == (elFinalFx.awayScore ?: 0) && (elFinalFx.homePens ?: 0) > (elFinalFx.awayPens ?: 0))
            if (isHomeWinner) {
                elWinnerId = elFinalFx.homeClubId
                elRunnerUpId = elFinalFx.awayClubId
            } else {
                elWinnerId = elFinalFx.awayClubId
                elRunnerUpId = elFinalFx.homeClubId
            }
        }

        var confWinnerId: Int? = null
        var confRunnerUpId: Int? = null
        val confFinalFx = dao.getAllFixturesSync().find { it.competition == "CONFERENCE_LEAGUE" && it.round == 2 && it.isSimulated }
        if (confFinalFx != null) {
            val isHomeWinner = (confFinalFx.homeScore ?: 0) > (confFinalFx.awayScore ?: 0) || 
                               ((confFinalFx.homeScore ?: 0) == (confFinalFx.awayScore ?: 0) && (confFinalFx.homePens ?: 0) > (confFinalFx.awayPens ?: 0))
            if (isHomeWinner) {
                confWinnerId = confFinalFx.homeClubId
                confRunnerUpId = confFinalFx.awayClubId
            } else {
                confWinnerId = confFinalFx.awayClubId
                confRunnerUpId = confFinalFx.homeClubId
            }
        }

        var superCupWinnerId: Int? = null
        var superCupRunnerUpId: Int? = null
        val superCupFx = dao.getAllFixturesSync().find { it.competition == "SUPER_CUP" && it.isSimulated }
        if (superCupFx != null) {
            val isHomeWinner = (superCupFx.homeScore ?: 0) > (superCupFx.awayScore ?: 0) || 
                               ((superCupFx.homeScore ?: 0) == (superCupFx.awayScore ?: 0) && (superCupFx.homePens ?: 0) > (superCupFx.awayPens ?: 0))
            if (isHomeWinner) {
                superCupWinnerId = superCupFx.homeClubId
                superCupRunnerUpId = superCupFx.awayClubId
            } else {
                superCupWinnerId = superCupFx.awayClubId
                superCupRunnerUpId = superCupFx.homeClubId
            }
        }

        val histories = mutableListOf<ClubSeasonHistoryEntity>()
        for (country in FictionalData.COUNTRIES) {
            val sortedStandings = dao.getStandingsByCountrySync(country)
            for (index in sortedStandings.indices) {
                val standing = sortedStandings[index]
                val cId = standing.clubId
                
                val europeanRank = when (cId) {
                    clWinnerId -> "Won UCL"
                    clRunnerUpId -> "Runner-Up UCL"
                    elWinnerId -> "Won UEL"
                    elRunnerUpId -> "Runner-Up UEL"
                    confWinnerId -> "Won UECL"
                    confRunnerUpId -> "Runner-Up UECL"
                    else -> {
                        val fxList = dao.getAllFixturesSync()
                        val semiCL = fxList.any { it.competition == "CHAMPIONS_LEAGUE" && it.round == 4 && (it.homeClubId == cId || it.awayClubId == cId) && it.isSimulated }
                        val qfCL = fxList.any { it.competition == "CHAMPIONS_LEAGUE" && it.round == 8 && (it.homeClubId == cId || it.awayClubId == cId) && it.isSimulated }
                        val semiEL = fxList.any { it.competition == "EUROPA_LEAGUE" && it.round == 4 && (it.homeClubId == cId || it.awayClubId == cId) && it.isSimulated }
                        val semiCONF = fxList.any { it.competition == "CONFERENCE_LEAGUE" && it.round == 4 && (it.homeClubId == cId || it.awayClubId == cId) && it.isSimulated }
                        when {
                            semiCL -> "Semi-Final UCL"
                            qfCL -> "Quarter-Final UCL"
                            semiEL -> "Semi-Final UEL"
                            semiCONF -> "Semi-Final UECL"
                            else -> "NO"
                        }
                    }
                }
                val superCupResult = when (cId) {
                    superCupWinnerId -> "Won"
                    superCupRunnerUpId -> "Lost"
                    else -> "NO"
                }
                
                histories.add(
                    ClubSeasonHistoryEntity(
                        seasonNumber = gameState.currentSeason,
                        country = country,
                        clubId = cId,
                        leagueFinishPosition = index + 1,
                        europeanRank = europeanRank,
                        superCupResult = superCupResult
                    )
                )
            }
        }
        dao.insertClubSeasonHistories(histories)

        val playerSeasonFixturesSnapshot = dao.getAllFixturesSync().filter {
            it.isSimulated && (it.homeClubId == player.currentClubId || it.awayClubId == player.currentClubId)
        }

        // Clear and rebuild league standings and fixtures for next season
        dao.clearStandings()
        dao.clearFixtures()

        // Re-generate database standings
        val standings = allClubs.map { club ->
            StandingEntity(
                country = club.country,
                clubId = club.id
            )
        }
        dao.insertStandings(standings)

        // Re-generate fixtures
        val fixtures = mutableListOf<FixtureEntity>()
        for (country in FictionalData.COUNTRIES) {
            val countryClubs = allClubs.filter { it.country == country }
            fixtures.addAll(generateLeagueFixtures(country, countryClubs))
        }
        dao.insertFixtures(fixtures)

        // Run living transfer window for NPC rival strikers and managers
        runTransferWindow(allClubs, player, gameState, seasonLogs, histories)

        scheduleKnockoutTournament("CHAMPIONS_LEAGUE", clTeams)
        scheduleKnockoutTournament("EUROPA_LEAGUE", elTeams)
        scheduleKnockoutTournament("CONFERENCE_LEAGUE", confTeams)

        // Check forced retirement at 41
        if (player.age >= 41 && !player.isRetired && !player.isGodMode) {
            player.isRetired = true
            seasonLogs.add("\n👴 RETIREMENT NOTICE: You have reached age 41. Your body can no longer sustain the professional level. You have announced your retirement from professional football.")
            retirePlayerAndSaveLegacy(player)
        }

        // Capture this season's stats permanently before they are reset.
        // Only pro (senior) seasons count toward the Family Legacy season table.
        if (player.careerPhase == PHASE_SENIOR) {
            val clubNameAtSeasonEnd = allClubsMap[player.currentClubId]?.name ?: "Unknown"
            dao.insertPlayerSeasonRecord(
                PlayerSeasonRecordEntity(
                    playerName = player.name,
                    generation = player.generation,
                    seasonNumber = gameState.currentSeason,
                    clubName = clubNameAtSeasonEnd,
                    matchesPlayed = player.seasonGamesPlayed,
                    goals = player.seasonGoals,
                    assists = player.seasonAssists,
                    ovrAtSeasonEnd = player.ovr,
                    playerAge = player.age
                )
            )
        }

        // Calculate season summary and Player of the Season award before stats reset
        if (player.careerPhase == PHASE_SENIOR) {
            val ratingsList = playerSeasonFixturesSnapshot.mapNotNull { it.playerRating }
            val avgRating = if (ratingsList.isNotEmpty()) {
                ratingsList.average().toFloat()
            } else if (player.seasonGamesPlayed > 0) {
                (6.5f + (player.seasonGoals * 0.15f + player.seasonAssists * 0.1f)).coerceAtMost(9.5f)
            } else {
                0.0f
            }

            val trophiesThisSeason = dao.getAllTrophiesSync().filter {
                it.playerName == player.name && it.generation == player.generation && it.seasonYear == gameState.currentSeason
            }
            val majorCount = trophiesThisSeason.count { it.isMajor }
            val minorCount = trophiesThisSeason.count { !it.isMajor }
            val trophyNames = trophiesThisSeason.map { it.competitionName }

            val goalsPoints = player.seasonGoals * 4
            val assistsPoints = player.seasonAssists * 3
            val trophiesPoints = (majorCount * 25) + (minorCount * 12)
            val ratingPoints = ((avgRating - 6.0f).coerceAtLeast(0f) * 10f).toInt()
            val totalPoints = goalsPoints + assistsPoints + trophiesPoints + ratingPoints

            val isEligibleForPots = player.ovr >= 90
            val potsWinChance = when {
                !isEligibleForPots -> 0f
                totalPoints >= 130 -> 0.55f
                totalPoints >= 110 -> 0.35f
                totalPoints >= 90  -> 0.18f
                else -> 0f
            }
            val isWinner = isEligibleForPots && potsWinChance > 0f && kotlin.random.Random.nextFloat() < potsWinChance
            val awardTitle = when {
                isWinner -> "PLAYER OF THE SEASON"
                totalPoints >= 25 -> "SEASON TOP CONTENDER"
                else -> "SEASON PERFORMER"
            }
            val rank = when {
                isWinner -> 1
                totalPoints >= 25 -> 2
                else -> 3
            }

            if (isWinner) {
                safeInsertTrophy(
                    TrophyEntity(
                        playerName = player.name,
                        generation = player.generation,
                        clubName = allClubsMap[player.currentClubId]?.name ?: "Club",
                        seasonYear = gameState.currentSeason,
                        competitionName = "Player of the Season",
                        isMajor = true
                    )
                )
            }

            val summaryData = SeasonSummaryData(
                seasonNumber = gameState.currentSeason,
                playerName = player.name,
                generation = player.generation,
                clubName = allClubsMap[player.currentClubId]?.name ?: "Club",
                matchesPlayed = player.seasonGamesPlayed,
                goals = player.seasonGoals,
                assists = player.seasonAssists,
                averageRating = avgRating,
                trophiesWon = trophyNames,
                playerOfTheSeasonAward = PlayerOfTheSeasonAward(
                    isWinner = isWinner,
                    awardTitle = awardTitle,
                    totalPoints = totalPoints,
                    goalsPoints = goalsPoints,
                    assistsPoints = assistsPoints,
                    trophiesPoints = trophiesPoints,
                    ratingPoints = ratingPoints,
                    rank = rank
                )
            )
            _latestSeasonSummaryFlow.value = summaryData
        }

        // Reset seasonal stats, fatigue and overtraining risk
        player.seasonGamesPlayed = 0
        player.seasonGoals = 0
        player.seasonAssists = 0
        player.seasonMvps = 0
        player.fatigue = 0
        player.overtrainingRisk = 0

        dao.updatePlayer(player)

        return seasonLogs.joinToString("\n")
    }

    companion object {
        // Tuning constants for the club era-shift system.
        // PACING: a full bottom-to-top (or top-to-bottom) arc should take roughly
        // 8-15 seasons. Do not increase these to make changes "more visible" faster —
        // gradual, earned-feeling change is the explicit design goal.
        private const val ERA_PERFORMANCE_SCALE = 0.7   // weight of this season's raw finishing position
        private const val ERA_SURPRISE_SCALE = 0.25     // weight of over/under-performing pre-season expectation
        private const val ERA_REVERSION_RATE = 0.015    // weak pull toward the country's average points
        private const val ERA_MAX_SEASON_SWING = 6.0    // hard cap on total points change in a single season
        private const val ERA_POINTS_MIN = 5
        private const val ERA_POINTS_MAX = 140
        private const val ERA_MOMENTUM_MIN_SEASONS = 8   // trajectory arcs last 8-15 seasons
        private const val ERA_MOMENTUM_MAX_SEASONS_EXCLUSIVE = 16
        private const val ERA_MOMENTUM_RISING_DECLINING_MIN = 0.8f
        private const val ERA_MOMENTUM_RISING_DECLINING_MAX = 2.0f
        private const val ERA_MOMENTUM_STABLE_RANGE = 0.4f
        private const val ERA_ELITE_COUNT = 2  // per-country bracket sizes; verified as 2/3/6/5 across all 5 countries
        private const val ERA_BIG_COUNT = 3
        private const val ERA_MID_COUNT = 6
        // remaining clubs after ELITE+BIG+MID are SMALL
    }

/**
 * Adjust club reputation points based on final league standings, applying a
 * multi-season "era" system so clubs realistically rise and decline over
 * many years instead of staying locked in a narrow table position forever.
 *
 * Called once per country, once per season, from handleEndOfSeason.
 */
private suspend fun adjustClubsReputations(standings: List<StandingEntity>, clubsMap: Map<Int, ClubEntity>) {
    if (standings.isEmpty()) return

    val leagueSize = standings.size
    val midpoint = (leagueSize + 1) / 2.0 // e.g. 8.5 for a 16-club league

    // Snapshot each club's pre-season reputation rank (1 = strongest) BEFORE
    // any mutation this call, so we can reward/punish surprising finishes.
    val countryClubs = standings.mapNotNull { clubsMap[it.clubId] }
    if (countryClubs.isEmpty()) return

    val preSeasonRankByClubId: Map<Int, Int> = countryClubs
        .sortedByDescending { it.reputationPoints }
        .mapIndexed { idx, c -> c.id to (idx + 1) }
        .toMap()

    val countryAvgPoints = countryClubs.map { it.reputationPoints }.average()

    val updatedClubs = mutableListOf<ClubEntity>()

    for (index in standings.indices) {
        val standing = standings[index]
        val club = clubsMap[standing.clubId] ?: continue
        val actualPosition = index + 1 // 1-based league finish

        // (a) Continuous performance delta — EVERY position matters now,
        // including mid-table, unlike the old banded system.
        val performanceDelta = (midpoint - actualPosition) * ERA_PERFORMANCE_SCALE

        // (b) Reward/punish finishing far from pre-season expectation
        // (a mid-table club having a breakout top-3 season, or an elite club
        // collapsing to the bottom half, should matter more than a club
        // finishing exactly where it was expected to).
        val expectedPosition = preSeasonRankByClubId[club.id] ?: actualPosition
        val surpriseDelta = ((expectedPosition - actualPosition) * ERA_SURPRISE_SCALE)
            .coerceIn(-3.0, 3.0)

        // (c) Multi-season momentum/trajectory. Each club is on a
        // RISING / STABLE / DECLINING arc that lasts 8-15 seasons before a
        // new arc is rolled — this is what produces genuine multi-year eras
        // instead of pure season-to-season noise.
        var momentum = club.momentumBias
        var seasonsRemaining = club.trajectorySeasonsRemaining
        if (seasonsRemaining <= 0) {
            val roll = Random.nextFloat()
            momentum = when {
                roll < 0.25f -> Random.nextFloat() *
                    (ERA_MOMENTUM_RISING_DECLINING_MAX - ERA_MOMENTUM_RISING_DECLINING_MIN) +
                    ERA_MOMENTUM_RISING_DECLINING_MIN // RISING arc
                roll < 0.50f -> -(Random.nextFloat() *
                    (ERA_MOMENTUM_RISING_DECLINING_MAX - ERA_MOMENTUM_RISING_DECLINING_MIN) +
                    ERA_MOMENTUM_RISING_DECLINING_MIN) // DECLINING arc
                else -> (Random.nextFloat() * 2f - 1f) * ERA_MOMENTUM_STABLE_RANGE // STABLE arc
            }
            seasonsRemaining = Random.nextInt(ERA_MOMENTUM_MIN_SEASONS, ERA_MOMENTUM_MAX_SEASONS_EXCLUSIVE)
        }

        // (d) Small season-to-season flavor noise (off-field variance).
        val noise = Random.nextFloat() - 0.5f // -0.5..+0.5

        // (e) Weak mean reversion toward the country average, so the overall
        // spread of strong/weak clubs stays roughly stable across decades
        // instead of everything eventually piling up at the point cap/floor.
        val reversion = (countryAvgPoints - club.reputationPoints) * ERA_REVERSION_RATE

        // Combine and clamp the total single-season swing.
        val totalDelta = (performanceDelta + surpriseDelta + momentum + noise + reversion)
            .coerceIn(-ERA_MAX_SEASON_SWING, ERA_MAX_SEASON_SWING)

        val newPoints = (club.reputationPoints + totalDelta.roundToInt())
            .coerceIn(ERA_POINTS_MIN, ERA_POINTS_MAX)

        updatedClubs.add(
            club.copy(
                reputationPoints = newPoints,
                momentumBias = momentum,
                trajectorySeasonsRemaining = seasonsRemaining - 1
            )
        )
    }

    // (f) Re-derive reputation tiers by RELATIVE RANK within the country,
    // not fixed absolute thresholds. This guarantees the same tier
    // composition every season (2 ELITE / 3 BIG / 6 MID / rest SMALL) while
    // letting the actual clubs occupying each tier change over time — a
    // club climbing past a rival genuinely displaces it from ELITE/BIG.
    val reranked = updatedClubs.sortedByDescending { it.reputationPoints }
    val finalClubs = reranked.mapIndexed { idx, c ->
        val tier = when {
            idx < ERA_ELITE_COUNT -> "ELITE"
            idx < ERA_ELITE_COUNT + ERA_BIG_COUNT -> "BIG"
            idx < ERA_ELITE_COUNT + ERA_BIG_COUNT + ERA_MID_COUNT -> "MID"
            else -> "SMALL"
        }
        c.copy(reputation = tier)
    }

    dao.updateClubs(finalClubs)
}

    /**
     * Seasonal Stat Growth / Decline calculation.
     */
    private fun applySeasonStatGrowthAndDecline(player: PlayerEntity, club: ClubEntity?) {
        val totalGamesInSeason = 30 // approximately
        val playedPct = player.seasonGamesPlayed.toFloat() / totalGamesInSeason

        // 1. Minutes Played Modifiers
        val minutesMod = when {
            playedPct == 0f -> -2.5f
            playedPct < 0.20f -> -1.0f
            playedPct < 0.50f -> 0.6f
            playedPct < 0.80f -> 1.5f
            else -> 2.5f
        }

        // 2. Age Modifiers
        // youth: 13-21, prime: 22-29, decline: 30+
        val age = player.age
        val ageMultiplier = when {
            age in 13..17 -> 1.5f
            age in 18..21 -> 1.2f
            age in 22..29 -> 0.1f // very stable prime
            else -> 0.0f // decline curve handles 30+
        }

        // 3. Academy/Club Multiplier
        val repMultiplier = when (club?.reputation) {
            "ELITE" -> 1.30f
            "BIG" -> 1.15f
            "MID" -> 1.00f
            "SMALL" -> 0.85f
            else -> 1.00f
        }

        if (age < 30 || player.isGodMode) {
            // Standard Growth Curve
            val baseFinishingGrowth = (0.65f * ageMultiplier * repMultiplier) + minutesMod
            val basePaceGrowth = (0.45f * ageMultiplier * repMultiplier) + minutesMod
            val basePassingGrowth = (0.55f * ageMultiplier * repMultiplier) + minutesMod
            val basePhysicalGrowth = (0.62f * ageMultiplier * repMultiplier) + minutesMod
            val baseTechniqueGrowth = (0.58f * ageMultiplier * repMultiplier) + minutesMod

            // Stat-specific performance bonuses
            val goalBonus = player.seasonGoals / 14f
            val assistBonus = player.seasonAssists / 14f
            val mvpBonus = if (player.seasonMvps > 5) 1.0f else 0.0f

            player.finishing = applyStatGrowth(player.finishing, baseFinishingGrowth + goalBonus + mvpBonus + player.finishingTrainingBonus, player.potentialCeiling)
            player.pace = applyStatGrowth(player.pace, basePaceGrowth + mvpBonus + player.paceTrainingBonus, player.potentialCeiling)
            player.passing = applyStatGrowth(player.passing, basePassingGrowth + assistBonus + mvpBonus + player.passingTrainingBonus, player.potentialCeiling)
            player.physical = applyStatGrowth(player.physical, basePhysicalGrowth + mvpBonus + player.physicalTrainingBonus, player.potentialCeiling)
            player.technique = applyStatGrowth(player.technique, baseTechniqueGrowth + mvpBonus + player.techniqueTrainingBonus, player.potentialCeiling)
        } else {
            // Decline Curve System (30+)
            val declineBase = when {
                age in 30..32 -> -1.5f // gentle
                age in 33..36 -> -3.0f // accelerates
                else -> -5.0f // sharp crash
            }

            // Form can soften/worsen decline. High minutes played also slows it down.
            var declineOffset = 0.0f
            if (player.form >= 2) declineOffset += 1.5f
            if (playedPct >= 0.80f) declineOffset += 1.0f

            if (player.form <= -2) declineOffset -= 1.0f
            if (playedPct < 0.20f) declineOffset -= 1.5f

            val netDecline = (declineBase + declineOffset).coerceAtMost(0.0f) // always decline or flat

            player.finishing = (player.finishing + netDecline.roundToInt() + player.finishingTrainingBonus.roundToInt()).coerceIn(1, player.potentialCeiling)
            player.pace = (player.pace + netDecline.roundToInt() + player.paceTrainingBonus.roundToInt()).coerceIn(1, player.potentialCeiling)
            player.passing = (player.passing + netDecline.roundToInt() + player.passingTrainingBonus.roundToInt()).coerceIn(1, player.potentialCeiling)
            player.physical = (player.physical + netDecline.roundToInt() + player.physicalTrainingBonus.roundToInt()).coerceIn(1, player.potentialCeiling)
            player.technique = (player.technique + netDecline.roundToInt() + player.techniqueTrainingBonus.roundToInt()).coerceIn(1, player.potentialCeiling)
        }

        player.ovr = calculateOvr(player.finishing, player.pace, player.passing, player.physical, player.technique).coerceAtMost(player.potentialCeiling)
        if (player.ovr > player.peakOvr) {
            player.peakOvr = player.ovr
        }

        // Reset training fields for the next season
        player.finishingTrainingBonus = 0f
        player.paceTrainingBonus = 0f
        player.passingTrainingBonus = 0f
        player.physicalTrainingBonus = 0f
        player.techniqueTrainingBonus = 0f
        player.hasTrainedThisMonth = 0
    }

    private fun applyStatGrowth(current: Int, growth: Float, ceiling: Int = 99): Int {
        val damping = proximityDamping(current, ceiling)
        val nextStat = current + (growth * damping).roundToInt()
        return nextStat.coerceIn(1, ceiling)
    }

    /**
     * Evaluate player's contract at the end of a season.
     */
    private suspend fun evaluateContractEndSeason(player: PlayerEntity, club: ClubEntity?, logs: MutableList<String>, season: Int) {
        if (club == null || player.careerPhase != PHASE_SENIOR) return

        if (player.age == 16 && player.contractTargetGoalsAssists == 0) {
            // Just graduated! Sign first professional contract.
            player.contractYearsRemaining = 3
            player.contractTargetGoalsAssists = calculateSeasonTarget(player.ovr, club.reputation)
            logs.add("🤝 FIRST PROFESSIONAL CONTRACT: You have officially graduated to ${club.name}'s first team! You signed a 3-year professional contract. Season target: ${player.contractTargetGoalsAssists} G+A.")
            return
        }

        player.contractYearsRemaining = max(0, player.contractYearsRemaining - 1)

        val totalGamesInSeason = 30f
        val playedPct = player.seasonGamesPlayed / totalGamesInSeason

        // Professional Contract Evaluation
        val actualTally = player.seasonGoals + player.seasonAssists
        val adjustedTarget = calculateProratedTarget(player.contractTargetGoalsAssists, player.seasonGamesPlayed)

        logs.add("📋 Contract Stats: Target $adjustedTarget G+A (scaled for minutes). Achieved: $actualTally G+A.")

        if (player.contractYearsRemaining == 0) {
            // Final contract year decision
            val isRivalWayBetter = club.rivalStrikerOvr - player.ovr > 15
            
            if (isRivalWayBetter) {
                logs.add("⚠️ NOT RENEWED: Club decided not to offer a renewal since rival striker ${club.rivalStrikerName} (${club.rivalStrikerOvr} OVR) is much highly rated.")
                handleContractExpiry(player, logs)
            } else if (actualTally >= adjustedTarget) {
                // Renewal offered!
                val baseLength = if (abs(player.ovr - club.rivalStrikerOvr) <= 5) 4 else 2
                val length = if (player.age >= 30) min(2, baseLength) else baseLength
                val nextLength = length.coerceIn(1, 5)

                val nextTarget = calculateSeasonTarget(player.ovr, club.reputation)

                player.contractYearsRemaining = nextLength
                player.contractTargetGoalsAssists = nextTarget
                logs.add("✍️ CONTRACT RENEWED: You signed a new $nextLength-year contract with ${club.name}! Expectation: $nextTarget G+A per season.")
            } else {
                // Missed target, contract runs out
                logs.add("⚠️ RELEASED: Contract expired. Your performance did not satisfy club goals ($adjustedTarget target vs $actualTally scored). You are now a free agent.")
                handleContractExpiry(player, logs)
            }
        } else {
            logs.add("ℹ️ Contract: ${player.contractYearsRemaining} years remaining on your deal at ${club.name}.")
        }
    }

    private suspend fun handleContractExpiry(player: PlayerEntity, logs: MutableList<String>) {
        // Force-sign with a Small club in the current country
        val allClubs = dao.getAllClubsSync()
        val academyClubs = allClubs.filter { it.country == player.academyCountry }
        val smallClub = academyClubs.filter { it.reputation == "SMALL" }.randomOrNull() ?: academyClubs.random()

        val oldClubId = player.currentClubId
        val currentSeason = dao.getGameStateSync()?.currentSeason ?: 1
        
        // Finalize old club records
        finalizePlayerRecordsForClub(player, oldClubId, currentSeason)
        
        // Save current club-scoped stats before resetting
        dao.upsertPlayerClubStint(
            PlayerClubStintEntity(
                playerName = player.name,
                generation = player.generation,
                clubId = oldClubId,
                goals = player.clubGoals,
                assists = player.clubAssists,
                gamesPlayed = player.clubGamesPlayed
            )
        )

        // Resume or reset club-scoped stats for the new club
        val priorStint = dao.getPlayerClubStint(player.name, player.generation, smallClub.id)
        player.clubGamesPlayed = priorStint?.gamesPlayed ?: 0
        player.clubGoals = priorStint?.goals ?: 0
        player.clubAssists = priorStint?.assists ?: 0

        player.currentClubId = smallClub.id
        player.contractYearsRemaining = 1
        player.contractTargetGoalsAssists = calculateSeasonTarget(player.ovr, smallClub.reputation)
        
        // Ensure new club records exist
        ensureClubRecordsExist(smallClub.id)
        
        logs.add("🤝 EMERGENCY DEALS: As a free agent, you accepted a 1-year contract with ${smallClub.name} to rescue your career. Season target: ${player.contractTargetGoalsAssists} G+A.")
    }

    fun calculateSeasonTarget(ovr: Int, reputation: String): Int {
        val repBonus = when (reputation) {
            "ELITE" -> 16
            "BIG" -> 11
            "MID" -> 6
            "SMALL" -> 2
            else -> 2
        }
        return (ovr * 0.18f + repBonus).roundToInt().coerceIn(2, 35)
    }

    fun isTransferWindowOpen(monthIndex: Int): Boolean {
        return monthIndex == 0 || monthIndex == 5
    }

    private fun serializeOffers(offers: List<TransferOffer>): String {
        return offers.joinToString(";") { offer ->
            "${offer.clubId}|${offer.clubName}|${offer.clubReputation}|${offer.rivalStrikerOvr}|${offer.rivalStrikerName}|${offer.contractYears}|${offer.targetGplusA}"
        }
    }

    private fun deserializeOffers(serialized: String): List<TransferOffer> {
        if (serialized.isEmpty()) return emptyList()
        return serialized.split(";").mapNotNull { part ->
            val subParts = part.split("|")
            if (subParts.size >= 7) {
                TransferOffer(
                    clubId = subParts[0].toIntOrNull() ?: 0,
                    clubName = subParts[1],
                    clubReputation = subParts[2],
                    rivalStrikerOvr = subParts[3].toIntOrNull() ?: 0,
                    rivalStrikerName = subParts[4],
                    contractYears = subParts[5].toIntOrNull() ?: 0,
                    targetGplusA = subParts[6].toIntOrNull() ?: 0
                )
            } else null
        }
    }

    /**
     * Submit manual transfer request during windows. Returns true if approved, false otherwise.
     */
    suspend fun getTransferOffers(player: PlayerEntity): List<TransferOffer> {
        val gameState = dao.getGameStateSync() ?: return emptyList()
        if (!isTransferWindowOpen(gameState.currentMonthIndex) || player.hasTransferredThisWindow) {
            return emptyList()
        }

        val persisted = gameState.persistedTransferOffers
        if (!persisted.isNullOrEmpty()) {
            return deserializeOffers(persisted)
        }

        val allClubs = dao.getAllClubsSync()
        val myClub = allClubs.find { it.id == player.currentClubId } ?: return emptyList()

        val myRepTier = myClub.reputation
        val myCountry = player.academyCountry

        val eligibleTargets = allClubs.filter { club ->
            if (club.id == player.currentClubId) return@filter false
            
            // Generally restricted to 1 reputation tier of movement
            val repDiff = getTierDistance(myRepTier, club.reputation)
            
            // Standout exception: high OVR can move directly to Elite
            val isOverqualified = player.ovr >= 75 && (player.ovr - myClub.rivalStrikerOvr >= 8)
            val isEliteMove = club.reputation == "ELITE" && isOverqualified

            (repDiff <= 1) || isEliteMove
        }

        val generatedOffers = eligibleTargets.shuffled().take(3).map { club ->
            TransferOffer(
                clubId = club.id,
                clubName = club.name,
                clubReputation = club.reputation,
                rivalStrikerOvr = club.rivalStrikerOvr,
                rivalStrikerName = club.rivalStrikerName,
                contractYears = if (player.age >= 31) 1 else Random.nextInt(2, 5).coerceIn(1, 5),
                targetGplusA = calculateSeasonTarget(player.ovr, club.reputation)
            )
        }

        gameState.persistedTransferOffers = serializeOffers(generatedOffers)
        dao.updateGameState(gameState)

        return generatedOffers
    }

    suspend fun completeTransfer(player: PlayerEntity, offer: TransferOffer): String {
        val gameState = dao.getGameStateSync() ?: return ""
        if (!isTransferWindowOpen(gameState.currentMonthIndex) || player.hasTransferredThisWindow) {
            return "Transfer window is closed or you have already transferred in this window!"
        }

        var report = ""
        db.withTransaction {
            val dbPlayer = dao.getPlayerSync() ?: return@withTransaction
            val oldClubId = dbPlayer.currentClubId
            
            // Finalize records for the old club before transferring
            finalizePlayerRecordsForClub(dbPlayer, oldClubId, gameState.currentSeason)
            
            // Save current club-scoped stats before resetting
            dao.upsertPlayerClubStint(
                PlayerClubStintEntity(
                    playerName = dbPlayer.name,
                    generation = dbPlayer.generation,
                    clubId = oldClubId,
                    goals = dbPlayer.clubGoals,
                    assists = dbPlayer.clubAssists,
                    gamesPlayed = dbPlayer.clubGamesPlayed
                )
            )

            // Resume or reset club-scoped stats for the new club
            val priorStint = dao.getPlayerClubStint(dbPlayer.name, dbPlayer.generation, offer.clubId)
            dbPlayer.clubGamesPlayed = priorStint?.gamesPlayed ?: 0
            dbPlayer.clubGoals = priorStint?.goals ?: 0
            dbPlayer.clubAssists = priorStint?.assists ?: 0
            
            dbPlayer.currentClubId = offer.clubId
            dbPlayer.contractYearsRemaining = offer.contractYears
            dbPlayer.contractTargetGoalsAssists = offer.targetGplusA
            dbPlayer.form = 0 // reset form
            dbPlayer.hasTransferredThisWindow = true
            
            dao.updatePlayer(dbPlayer)

            // Ensure records exist for the new club
            ensureClubRecordsExist(offer.clubId)

            report = "🤝 TRANSFERRED: You signed for ${offer.clubName} on a ${offer.contractYears}-year contract!"
            
            val gs = dao.getGameStateSync()
            if (gs != null) {
                gs.narrativeLog = capNarrativeLog("$report\n" + gs.narrativeLog)
                dao.updateGameState(gs)
            }
        }
        return report
    }

    private fun getTierDistance(t1: String, t2: String): Int {
        val tiers = listOf("SMALL", "MID", "BIG", "ELITE")
        val idx1 = tiers.indexOf(t1)
        val idx2 = tiers.indexOf(t2)
        return abs(idx1 - idx2)
    }

    /**
     * Retires the player manually. Saves legacy data and puts game into retired state.
     */
    suspend fun retirePlayerAndSaveLegacy(player: PlayerEntity) {
        db.withTransaction {
            val trophies = dao.getAllTrophiesSync()
            val myTrophies = trophies.filter { it.playerName == player.name && it.generation == player.generation }

            val currentSeason = dao.getGameStateSync()?.currentSeason ?: 1
            val currentClub = dao.getClubById(player.currentClubId)
            if (currentClub != null) {
                // Check and update player records for current club one final time
                checkAndApplyPlayerClubRecords(player, currentClub, currentSeason)
                // Finalize active status
                finalizePlayerRecordsForClub(player, currentClub.id, currentSeason)
            }

            // Calculate total trophy weight
            var totalWeight = 0
            for (trophy in myTrophies) {
                totalWeight += when (trophy.competitionName) {
                    "Champions League" -> FictionalData.WEIGHT_CHAMPIONS_LEAGUE
                    "Europa League" -> FictionalData.WEIGHT_EUROPA_LEAGUE
                    "Conference League" -> FictionalData.WEIGHT_CONFERENCE_LEAGUE
                    "European Super Cup" -> FictionalData.WEIGHT_SUPER_CUP
                    else -> FictionalData.WEIGHT_LEAGUE // Domestic league
                }
            }

            val allClubs = dao.getAllClubsSync().associateBy { it.id }
            val currentClubName = allClubs[player.currentClubId]?.name ?: "Unknown"
            val trophyClubs = myTrophies.map { it.clubName }.filter { it.isNotBlank() }
            val clubsSet = LinkedHashSet<String>()
            clubsSet.add(currentClubName)
            clubsSet.addAll(trophyClubs)
            val clubsPlayedStr = clubsSet.joinToString(", ")

            val retirementDesc = generateRetirementDescription(
                player = player,
                myTrophies = myTrophies,
                totalWeight = totalWeight,
                peakOvr = player.peakOvr,
                finalOvr = player.ovr,
                totalGoals = player.goals,
                totalAssists = player.assists,
                totalGames = player.gamesPlayed,
                clubsPlayedStr = clubsPlayedStr
            )

            val legacy = LegacyEntity(
                generation = player.generation,
                name = player.name,
                totalGames = player.gamesPlayed,
                totalGoals = player.goals,
                totalAssists = player.assists,
                totalMvps = player.mvps,
                totalTrophiesWeight = totalWeight,
                peakOvr = player.peakOvr,
                finalOvr = player.ovr,
                clubsPlayed = clubsPlayedStr,
                isCompleted = true,
                retirementDescription = retirementDesc
            )
            dao.insertLegacy(legacy)

            if (player.careerPhase == PHASE_SENIOR) {
                val existingRecords = dao.getSeasonRecordsForGeneration(player.generation)
                if (existingRecords.none { it.seasonNumber == currentSeason }) {
                    dao.insertPlayerSeasonRecord(
                        PlayerSeasonRecordEntity(
                            playerName = player.name,
                            generation = player.generation,
                            seasonNumber = currentSeason,
                            clubName = currentClubName,
                            matchesPlayed = player.seasonGamesPlayed,
                            goals = player.seasonGoals,
                            assists = player.seasonAssists,
                            ovrAtSeasonEnd = player.ovr,
                            playerAge = player.age
                        )
                    )
                }
            }

            player.isRetired = true
            dao.updatePlayer(player)
        }
    }

    private fun generateRetirementDescription(
        player: PlayerEntity,
        myTrophies: List<TrophyEntity>,
        totalWeight: Int,
        peakOvr: Int,
        finalOvr: Int,
        totalGoals: Int,
        totalAssists: Int,
        totalGames: Int,
        clubsPlayedStr: String
    ): String {
        val rng = kotlin.random.Random(player.generation.toLong())
        val name = player.name

        // 1. PEAK OVR TIER
        val openingLines = when {
            peakOvr >= 90 -> listOf(
                "$name retires as one of the true greats of the era, a player defenses feared and fans idolized wherever they played.",
                "$name closes the chapter on a legendary career, cementing a legacy amongst footballing royalty.",
                "$name leaves the game at the very pinnacle of world football, revered as an unstoppable attacking force.",
                "$name hangs up their boots as an iconic generational talent whose extraordinary quality thrilled supporters worldwide."
            )
            peakOvr in 80..89 -> listOf(
                "$name built a career among the finest strikers of their generation, consistently delivering when it mattered most.",
                "$name steps away from professional football having established a formidable reputation at the highest levels of the game.",
                "$name completes a stellar professional journey, recognized as a dangerous and highly reliable attacking focal point.",
                "$name retires after years of high-level performances, earning widespread acclaim for composure in front of goal."
            )
            peakOvr in 65..79 -> listOf(
                "$name carved out a respected, dependable career as a professional footballer, earning the trust of teammates and coaches alike.",
                "$name brings an honest and hardworking career to an end, leaving behind a legacy of determination and discipline.",
                "$name closes a solid tenure in professional football, having consistently put in relentless efforts for every club served.",
                "$name concludes their professional journey with pride, respected across the league as a committed professional."
            )
            else -> listOf(
                "$name may not have reached the very top, but gave everything to the game across a long and honest career.",
                "$name retires with head held high, having poured every ounce of passion into a gritty professional journey.",
                "$name completes a career defined by persistence and heart, battling through every challenge on the pitch.",
                "$name hangs up their boots after a dedicated career, always displaying immense pride whenever stepping onto the pitch."
            )
        }
        val openingLine = openingLines[rng.nextInt(openingLines.size)]

        // 2. LONGEVITY / STAT-LINE MIDDLE
        val statLines = listOf(
            "Across $totalGames appearances, they found the net $totalGoals times and set up $totalAssists more, turning out for $clubsPlayedStr along the way.",
            "$totalGoals goals and $totalAssists assists over $totalGames matches tell only part of the story of a career spent at $clubsPlayedStr.",
            "In $totalGames senior matches across spells with $clubsPlayedStr, they amassed $totalGoals goals and $totalAssists assists.",
            "Representing $clubsPlayedStr, they compiled a total stat line of $totalGoals goals and $totalAssists assists in $totalGames matches."
        )
        val statLine = statLines[rng.nextInt(statLines.size)]

        // 3. TROPHY / LEGACY CLOSING LINE
        val majorTrophies = myTrophies.filter { it.isMajor }
        val topTrophyName = majorTrophies.firstOrNull()?.competitionName
        val closingLines = when {
            topTrophyName != null || totalWeight >= 50 -> listOf(
                "Silverware including ${topTrophyName ?: "major titles"} will forever mark this as a career of substance.",
                "A glittering trophy cabinet headlined by ${topTrophyName ?: "major trophies"} stands as permanent testament to their success.",
                "Crowned with honors such as ${topTrophyName ?: "championship titles"}, their mantelpiece reflects a career defined by winning."
            )
            myTrophies.isNotEmpty() -> listOf(
                "With key domestic honors secured along the way, their contribution to team silverware remains memorable.",
                "Honored with cup triumphs, they ensured their efforts on the pitch translated into tangible silverware.",
                "Lifting silverware during their career provided memorable highlights for supporters to cherish."
            )
            else -> listOf(
                "Though major silverware proved elusive, their grit, loyalty, and sheer dedication on the pitch earned enduring respect.",
                "While trophy cabinets don't tell the whole story, their passion and commitment left an indelible mark on every club.",
                "Beyond trophies and medals, the journey itself and the respect earned from peers defined a career of true integrity."
            )
        }
        val closingLine = closingLines[rng.nextInt(closingLines.size)]

        return "$openingLine $statLine $closingLine"
    }

    // Dev Mode tools
    suspend fun devSetStats(fin: Int, pac: Int, pas: Int, phy: Int, tech: Int) {
        val player = dao.getPlayerSync() ?: return
        player.finishing = fin.coerceIn(1, 99)
        player.pace = pac.coerceIn(1, 99)
        player.passing = pas.coerceIn(1, 99)
        player.physical = phy.coerceIn(1, 99)
        player.technique = tech.coerceIn(1, 99)
        player.ovr = calculateOvr(player.finishing, player.pace, player.passing, player.physical, player.technique)
        dao.updatePlayer(player)
    }

    suspend fun devUpdateSocialStats(morale: Int, fanRep: Int, managerTrust: Int, rivalRel: Int) {
        val player = dao.getPlayerSync() ?: return
        player.morale = morale.coerceIn(0, 100)
        player.fanReputation = fanRep.coerceIn(0, 100)
        player.managerTrust = managerTrust.coerceIn(0, 100)
        player.rivalRelationship = rivalRel.coerceIn(0, 100)
        dao.updatePlayer(player)
    }

    suspend fun requestManagerTalk() {
        db.withTransaction {
            val player = dao.getPlayerSync() ?: return@withTransaction
            val gameState = dao.getGameStateSync() ?: return@withTransaction
            
            if (gameState.managerTalkCooldownMonths > 0) return@withTransaction
            
            gameState.activeChoicePrompt = "👔 Manager Talk: You enter the manager's office to discuss your squad role. He looks up. 'What's on your mind?'"
            
            gameState.activeChoiceOption1 = "Demand: 'I should be in the starting lineup. I'm ready.'"
            if (player.form >= 3) {
                gameState.activeChoiceOutcome1 = "'You've got a point. Your form in training is excellent.' Manager Trust +10."
                gameState.activeChoiceManagerTrustMod1 = 10
            } else {
                gameState.activeChoiceOutcome1 = "'Your current form doesn't warrant a start. Work harder.' Manager Trust -15."
                gameState.activeChoiceManagerTrustMod1 = -15
            }
            
            gameState.activeChoiceOption2 = "Ask: 'What do I need to improve to earn my place back?'"
            gameState.activeChoiceOutcome2 = "'I want to see more technical sharp focus in shooting drills.' Manager Trust +5, Finishing +2."
            gameState.activeChoiceManagerTrustMod2 = 5
            gameState.activeChoiceFinishingMod2 = 2
            
            gameState.activeChoiceOption3 = "Promise: 'Give me a chance. I promise to deliver 2 goals/assists in the next 3 matches.'"
            gameState.activeChoiceOutcome3 = "'Fine. You have 3 matches to deliver 2 G/A. Don't let me down.' Morale +8, Promise Started!"
            gameState.activeChoiceMoraleMod3 = 8
            
            gameState.activeChoicePendingMonthLogs = null // Tells resolveChoice NOT to advance month
            
            gameState.managerTalkCooldownMonths = 3
            
            dao.updateGameState(gameState)
        }
    }

    suspend fun getActiveManagerForClub(clubId: Int): NpcManagerEntity? {
        return dao.getActiveManagerForClub(clubId)
    }

    suspend fun triggerNewManagerFirstMeeting() {
        db.withTransaction {
            val player = dao.getPlayerSync() ?: return@withTransaction
            val gameState = dao.getGameStateSync() ?: return@withTransaction
            if (!player.pendingNewManagerNotice) return@withTransaction
            if (gameState.activeChoicePrompt != null) return@withTransaction
            triggerNewManagerFirstMeetingInternal(player, gameState)
        }
    }

    private suspend fun triggerNewManagerFirstMeetingInternal(player: PlayerEntity, gameState: GameStateEntity) {
        val myClub = dao.getClubById(player.currentClubId)
        val rivalOvr = myClub?.rivalStrikerOvr ?: 65
        val managerName = if (!myClub?.managerName.isNullOrEmpty()) myClub!!.managerName else "The new manager"

        gameState.activeChoicePrompt = "👔 New Management: $managerName calls you in for a first conversation. 'I've heard about you, but I don't know you yet. Let's see what you're about.'"

        gameState.activeChoiceOption1 = "Tell him you're the club's best striker and expect to start."
        if (player.ovr >= rivalOvr) {
            gameState.activeChoiceOutcome1 = "He respects the confidence — the numbers back it up. Manager Trust +8."
            gameState.activeChoiceManagerTrustMod1 = 8
        } else {
            gameState.activeChoiceOutcome1 = "He's skeptical — you'll need to prove it on the pitch first. Manager Trust -10."
            gameState.activeChoiceManagerTrustMod1 = -10
        }

        gameState.activeChoiceOption2 = "Say you're ready to learn his system and earn your place."
        gameState.activeChoiceOutcome2 = "'I like that attitude. Work hard in training.' Manager Trust +5, Morale +3."
        gameState.activeChoiceManagerTrustMod2 = 5
        gameState.activeChoiceMoraleMod2 = 3

        gameState.activeChoiceOption3 = "Say little, let your performances speak for themselves."
        gameState.activeChoiceOutcome3 = "'We'll see on matchdays.' Fans respect your quiet confidence. Fan Rep +2."
        gameState.activeChoiceManagerTrustMod3 = 0
        gameState.activeChoiceFanRepMod3 = 2

        gameState.activeChoicePendingMonthLogs = null

        player.pendingNewManagerNotice = false
        dao.updatePlayer(player)
        dao.updateGameState(gameState)
    }

    suspend fun devSetClubRep(clubId: Int, repTier: String) {
        val club = dao.getClubById(clubId) ?: return
        val pts = FictionalData.getReputationPoints(repTier)
        val updated = club.copy(reputation = repTier, reputationPoints = pts)
        dao.updateClub(updated)
    }

    suspend fun toggleAutoSave(enabled: Boolean) {
        val gameState = dao.getGameStateSync() ?: return
        gameState.autoSave = enabled
        dao.updateGameState(gameState)
    }

    suspend fun devSimulateFullSeason() {
        val startSeason = dao.getGameStateSync()?.currentSeason ?: return
        var currentSeason = startSeason
        var safetyCounter = 0

        while (currentSeason == startSeason && safetyCounter < 40) {
            safetyCounter++
            val player = dao.getPlayerSync() ?: break
            if (player.isRetired || (player.age >= 41 && !player.isGodMode)) break

            var gs = dao.getGameStateSync() ?: break
            var resolveAttempts = 0
            while (gs.activeChoicePrompt != null && resolveAttempts < 5) {
                resolveChoice(1)
                gs = dao.getGameStateSync() ?: break
                resolveAttempts++
            }

            if (player.careerPhase == PHASE_STREET) {
                val offers = YouthCareerLogic.deserializeYouthOffers(gs?.persistedYouthOffers ?: "")
                if (offers.isNotEmpty()) {
                    acceptYouthScoutOffer(offers.first().academyId)
                }
            } else if (player.careerPhase == PHASE_YOUTH && player.age >= 16) {
                val offers = YouthCareerLogic.deserializeSeniorYouthOffers(gs?.persistedSeniorYouthOffers ?: "")
                if (offers.isNotEmpty()) {
                    val offer = offers.first()
                    acceptSeniorYouthOffer(offer.clubId, offer.contractYears, offer.targetGplusA)
                }
            }

            legacy_advanceMonth(isAutoSim = true)
            yield()

            val updatedGs = dao.getGameStateSync() ?: break
            currentSeason = updatedGs.currentSeason
        }
    }

    suspend fun devSimulateSeasons(seasonsToSimulate: Int) {
        val player0 = dao.getPlayerSync()
        val maxAllowed = if (player0?.isGodMode == true) 100 else 10
        val count = seasonsToSimulate.coerceIn(1, maxAllowed)
        for (i in 0 until count) {
            val player = dao.getPlayerSync() ?: break
            if (player.isRetired || (player.age >= 41 && !player.isGodMode)) break

            devSimulateFullSeason()

            val updatedPlayer = dao.getPlayerSync() ?: break
            if (updatedPlayer.isRetired || (updatedPlayer.age >= 41 && !updatedPlayer.isGodMode)) break
        }
    }

    suspend fun resetGame() {
        db.withTransaction {
            dao.clearPlayers()
            dao.clearClubs()
            dao.clearStandings()
            dao.clearFixtures()
            dao.clearTrophies()
            dao.clearLegacies()
            dao.clearGameState()
            dao.clearUsedNames()
            dao.clearClubSeasonHistories()
            dao.clearClubRecords()
            dao.clearNpcStrikers()
            dao.clearNpcManagers()
        }
    }

    private suspend fun runTransferWindow(
        allClubs: List<ClubEntity>,
        player: PlayerEntity,
        gameState: GameStateEntity,
        seasonLogs: MutableList<String>,
        histories: List<ClubSeasonHistoryEntity>
    ) {
        val clubsMap = allClubs.associateBy { it.id }
        val finishPositionByClubId = histories.associate { it.clubId to it.leagueFinishPosition }
        val narrativeLogs = mutableListOf<String>()

        // -------------------------------------------------------------
        // 5a. STRIKER MOVEMENT PASS
        // -------------------------------------------------------------
        val activeStrikers = dao.getAllNpcStrikersSync().filter { !it.isRetired }.toMutableList()

        for (striker in activeStrikers) {
            striker.age += 1
            striker.seasonsAtCurrentClub += 1

            val currentClub = striker.currentClubId?.let { clubsMap[it] }

            // Organic OVR drift
            if (currentClub != null) {
                val range = FictionalData.getRivalOvrRange(currentClub.reputation)
                striker.ovr = (striker.ovr + Random.nextInt(-2, 3)).coerceIn(range.first, range.last + 1)
            }

            // Rising talent extra boost
            if (striker.isRisingTalent && striker.age < 24 && striker.ovr < striker.potentialCeiling) {
                striker.ovr = (striker.ovr + Random.nextInt(0, 3)).coerceAtMost(striker.potentialCeiling)
            }

            // Retirement check
            val retirementAgeThreshold = Random.nextInt(32, 38)
            if (striker.age >= retirementAgeThreshold) {
                striker.isRetired = true
                val oldClubId = striker.currentClubId
                striker.currentClubId = null

                if (oldClubId != null && clubsMap.containsKey(oldClubId)) {
                    val oldClub = clubsMap[oldClubId]!!
                    val newAge = Random.nextInt(17, 20)
                    val newOvr = Random.nextInt(48, 62)
                    val newCeiling = newOvr + Random.nextInt(15, 30)
                    val newName = generateUniqueName(oldClub.country, dao)

                    val newStriker = NpcStrikerEntity(
                        name = newName,
                        country = oldClub.country,
                        currentClubId = oldClub.id,
                        age = newAge,
                        ovr = newOvr,
                        potentialCeiling = newCeiling,
                        seasonsAtCurrentClub = 0,
                        isRetired = false,
                        isRisingTalent = true
                    )
                    dao.insertNpcStriker(newStriker)

                    val logText = "📰 ${striker.name} has retired from professional football. ${oldClub.name} promote $newName ($newAge) from their academy."
                    seasonLogs.add(logText)
                    narrativeLogs.add(logText)
                }
                continue
            }

            // Transfer check (only if not retired this pass)
            val transferRoll = Random.nextFloat()
            if (transferRoll < 0.18f && currentClub != null) {
                val range = FictionalData.getRivalOvrRange(currentClub.reputation)
                val countryClubs = allClubs.filter { it.country == currentClub.country }

                val rangeSpread = (range.last - range.first).coerceAtLeast(1)
                val isTopPortion = striker.ovr >= (range.first + rangeSpread * 0.6f)
                val isBottomPortion = striker.ovr <= (range.first + rangeSpread * 0.4f)

                var targetClub: ClubEntity? = null

                if (isTopPortion && Random.nextFloat() < 0.65f) {
                    val higherTier = getNextHigherReputationTier(currentClub.reputation)
                    val candidates = countryClubs.filter { it.reputation == higherTier && it.id != currentClub.id }
                    if (candidates.isNotEmpty()) {
                        targetClub = candidates.minByOrNull { it.rivalStrikerOvr }
                    }
                    if (targetClub == null) {
                        targetClub = countryClubs.filter { it.reputation == currentClub.reputation && it.id != currentClub.id }.randomOrNull()
                    }
                } else if (isBottomPortion && Random.nextFloat() < 0.50f) {
                    val lowerTier = getNextLowerReputationTier(currentClub.reputation)
                    targetClub = countryClubs.filter { it.reputation == lowerTier && it.id != currentClub.id }.randomOrNull()
                    if (targetClub == null) {
                        targetClub = countryClubs.filter { it.reputation == currentClub.reputation && it.id != currentClub.id }.randomOrNull()
                    }
                } else {
                    targetClub = countryClubs.filter { it.reputation == currentClub.reputation && it.id != currentClub.id }.randomOrNull()
                }

                if (targetClub != null) {
                    val oldClub = currentClub
                    val newClub = targetClub

                    striker.currentClubId = newClub.id
                    striker.seasonsAtCurrentClub = 0

                    // The club they LEFT must get a new academy striker
                    val vacAge = Random.nextInt(17, 20)
                    val vacOvr = Random.nextInt(48, 62)
                    val vacCeiling = vacOvr + Random.nextInt(15, 30)
                    val vacName = generateUniqueName(oldClub.country, dao)
                    val vacStriker = NpcStrikerEntity(
                        name = vacName,
                        country = oldClub.country,
                        currentClubId = oldClub.id,
                        age = vacAge,
                        ovr = vacOvr,
                        potentialCeiling = vacCeiling,
                        seasonsAtCurrentClub = 0,
                        isRetired = false,
                        isRisingTalent = true
                    )
                    dao.insertNpcStriker(vacStriker)

                    if (newClub.id == player.currentClubId) {
                        val logText = "🔄 ${striker.name} (${striker.ovr} OVR) has joined ${newClub.name} — expect real competition for minutes."
                        seasonLogs.add(logText)
                        narrativeLogs.add(logText)
                        player.rivalRelationship = 50

                        val maxSeq = (dao.getMaxSocialPostSequenceIndex() ?: 0) + 1
                        dao.insertSocialPost(
                            SocialPostEntity(
                                sequenceIndex = maxSeq,
                                seasonNumber = gameState.currentSeason,
                                monthIndex = gameState.currentMonthIndex,
                                postType = "CLUB_NEWS",
                                authorName = "Transfer Deadline",
                                authorHandle = "@TransferNews",
                                authorInitials = "TN",
                                content = "🔄 TRANSFER OFFICIAL: ${striker.name} (${striker.ovr} OVR) has joined ${newClub.name}!",
                                isAboutPlayerOrClub = true,
                                relatedClubId = player.currentClubId,
                                likeCount = Random.nextInt(1000, 25000)
                            )
                        )
                    } else if (oldClub.id == player.currentClubId) {
                        val logText = "🔄 ${striker.name} (${striker.ovr} OVR) has departed ${oldClub.name} for ${newClub.name}."
                        seasonLogs.add(logText)
                        narrativeLogs.add(logText)

                        val maxSeq = (dao.getMaxSocialPostSequenceIndex() ?: 0) + 1
                        dao.insertSocialPost(
                            SocialPostEntity(
                                sequenceIndex = maxSeq,
                                seasonNumber = gameState.currentSeason,
                                monthIndex = gameState.currentMonthIndex,
                                postType = "CLUB_NEWS",
                                authorName = "Transfer Deadline",
                                authorHandle = "@TransferNews",
                                authorInitials = "TN",
                                content = "🔄 TRANSFER OFFICIAL: ${striker.name} (${striker.ovr} OVR) has departed ${oldClub.name} for ${newClub.name}.",
                                isAboutPlayerOrClub = true,
                                relatedClubId = player.currentClubId,
                                likeCount = Random.nextInt(1000, 25000)
                            )
                        )
                    }
                }
            }
        }

        dao.updateNpcStrikers(activeStrikers)

        // Unconditional cache sync for strikers
        val latestActiveStrikers = dao.getAllNpcStrikersSync().filter { !it.isRetired }
        val strikerByClubId = latestActiveStrikers.filter { it.currentClubId != null }.associateBy { it.currentClubId!! }

        for (club in allClubs) {
            val activeStriker = strikerByClubId[club.id]
            if (activeStriker != null) {
                club.rivalStrikerName = activeStriker.name
                club.rivalStrikerOvr = activeStriker.ovr
            } else {
                val fName = generateUniqueName(club.country, dao)
                val fRange = FictionalData.getRivalOvrRange(club.reputation)
                val fOvr = Random.nextInt(fRange.first, fRange.last + 1)
                val fStriker = NpcStrikerEntity(
                    name = fName,
                    country = club.country,
                    currentClubId = club.id,
                    age = Random.nextInt(17, 20),
                    ovr = fOvr,
                    potentialCeiling = fOvr + 15,
                    seasonsAtCurrentClub = 0,
                    isRetired = false,
                    isRisingTalent = true
                )
                dao.insertNpcStriker(fStriker)
                club.rivalStrikerName = fName
                club.rivalStrikerOvr = fOvr
            }
        }

        // -------------------------------------------------------------
        // 5b. MANAGER MOVEMENT PASS
        // -------------------------------------------------------------
        val activeManagers = dao.getAllNpcManagersSync().filter { !it.isRetired }.toMutableList()
        for (m in activeManagers) {
            m.seasonsAtCurrentClub += 1
            m.seasonsAsManager += 1
        }

        var playerManagerChanged = false
        var oldPlayerManagerName: String? = null
        var newPlayerManagerName: String? = null

        val managersByClubId = activeManagers.filter { it.currentClubId != null }.associateBy { it.currentClubId!! }.toMutableMap()

        for (club in allClubs) {
            val mgr = managersByClubId[club.id] ?: continue
            val finishPos = finishPositionByClubId[club.id] ?: 1

            val isUnderperforming = (club.reputation == "ELITE" && finishPos > 3) ||
                                    (club.reputation == "BIG" && finishPos > 6)

            val sackChance = when {
                isUnderperforming -> 0.35f
                mgr.seasonsAtCurrentClub >= 5 -> 0.20f
                else -> 0.08f
            }

            if (Random.nextFloat() < sackChance) {
                val oldName = mgr.name
                mgr.currentClubId = null // Sacked / departing

                if (club.id == player.currentClubId) {
                    playerManagerChanged = true
                    oldPlayerManagerName = oldName
                }

                var appointedName = ""
                var promoted = false
                if (Random.nextFloat() < 0.50f) {
                    val candidate = activeManagers.filter {
                        it.currentClubId != null && it.currentClubId != club.id &&
                        clubsMap[it.currentClubId]?.country == club.country &&
                        isTierLowerThan(it.reputationTier, club.reputation)
                    }.maxByOrNull { getTierRank(it.reputationTier) }

                    if (candidate != null) {
                        val prevClubId = candidate.currentClubId!!
                        val prevClub = clubsMap[prevClubId]

                        candidate.currentClubId = club.id
                        candidate.seasonsAtCurrentClub = 0
                        candidate.reputationTier = getNextHigherReputationTier(candidate.reputationTier)
                        appointedName = candidate.name
                        promoted = true
                        managersByClubId[club.id] = candidate
                        managersByClubId.remove(prevClubId)

                        // Old club needs replacement manager
                        if (prevClub != null) {
                            val replacementName = generateUniqueName(prevClub.country, dao)
                            val repTier = if (Random.nextFloat() < 0.70f) prevClub.reputation else getNextLowerReputationTier(prevClub.reputation)
                            val replacementMgr = NpcManagerEntity(
                                name = replacementName,
                                country = prevClub.country,
                                currentClubId = prevClub.id,
                                reputationTier = repTier,
                                seasonsAtCurrentClub = 0,
                                seasonsAsManager = Random.nextInt(0, 3)
                            )
                            val newId = dao.insertNpcManager(replacementMgr).toInt()
                            prevClub.managerName = replacementName
                            prevClub.managerReputationTier = repTier
                            prevClub.managerId = newId
                            managersByClubId[prevClub.id] = replacementMgr
                        }
                    }
                }

                if (!promoted) {
                    appointedName = generateUniqueName(club.country, dao)
                    val repTier = if (Random.nextFloat() < 0.70f) club.reputation else getNextLowerReputationTier(club.reputation)
                    val freshMgr = NpcManagerEntity(
                        name = appointedName,
                        country = club.country,
                        currentClubId = club.id,
                        reputationTier = repTier,
                        seasonsAtCurrentClub = 0,
                        seasonsAsManager = Random.nextInt(0, 3)
                    )
                    val newId = dao.insertNpcManager(freshMgr).toInt()
                    managersByClubId[club.id] = freshMgr
                }

                if (club.id == player.currentClubId) {
                    newPlayerManagerName = appointedName
                }
            }
        }

        dao.updateNpcManagers(activeManagers)

        // Handle player club manager change special case
        if (playerManagerChanged) {
            player.pendingNewManagerNotice = true
            val newTrust = Random.nextInt(20, 50)
            player.managerTrust = newTrust
            val playerClubName = clubsMap[player.currentClubId]?.name ?: "your club"
            val mgrLog = "👔 NEW MANAGER: ${oldPlayerManagerName ?: "The manager"} has left $playerClubName. ${newPlayerManagerName ?: "A new manager"} has been appointed as the new manager. Manager Trust has reset to $newTrust/100 — you'll need to prove yourself again."
            seasonLogs.add(mgrLog)
            narrativeLogs.add(mgrLog)

            val maxSeq = (dao.getMaxSocialPostSequenceIndex() ?: 0) + 1
            dao.insertSocialPost(
                SocialPostEntity(
                    sequenceIndex = maxSeq,
                    seasonNumber = gameState.currentSeason,
                    monthIndex = gameState.currentMonthIndex,
                    postType = "CLUB_NEWS",
                    authorName = "Breaking Football",
                    authorHandle = "@SkyFootballNews",
                    authorInitials = "SF",
                    content = "👔 NEW MANAGER APPOINTED: ${newPlayerManagerName ?: "A new manager"} has officially taken charge of $playerClubName following the departure of ${oldPlayerManagerName ?: "the former manager"}.",
                    isAboutPlayerOrClub = true,
                    relatedClubId = player.currentClubId,
                    likeCount = Random.nextInt(5000, 35000)
                )
            )
        }

        // Unconditional cache sync for managers
        val latestActiveManagers = dao.getAllNpcManagersSync().filter { !it.isRetired }
        val managerByClubId = latestActiveManagers.filter { it.currentClubId != null }.associateBy { it.currentClubId!! }

        for (club in allClubs) {
            val activeMgr = managerByClubId[club.id]
            if (activeMgr != null) {
                club.managerName = activeMgr.name
                club.managerReputationTier = activeMgr.reputationTier
                club.managerId = activeMgr.id
            } else {
                val mName = generateUniqueName(club.country, dao)
                val mMgr = NpcManagerEntity(
                    name = mName,
                    country = club.country,
                    currentClubId = club.id,
                    reputationTier = club.reputation,
                    seasonsAtCurrentClub = 0,
                    seasonsAsManager = Random.nextInt(0, 3)
                )
                val mId = dao.insertNpcManager(mMgr).toInt()
                club.managerName = mName
                club.managerReputationTier = club.reputation
                club.managerId = mId
            }
        }

        // Persist updated clubs & player
        dao.updateClubs(allClubs)
        dao.updatePlayer(player)

        if (narrativeLogs.isNotEmpty()) {
            gameState.narrativeLog = capNarrativeLog(narrativeLogs.joinToString("\n\n") + "\n\n" + gameState.narrativeLog)
            dao.updateGameState(gameState)
        }
    }

    private fun getNextHigherReputationTier(tier: String): String {
        return when (tier) {
            "SMALL" -> "MID"
            "MID" -> "BIG"
            "BIG" -> "ELITE"
            else -> "ELITE"
        }
    }

    private fun getNextLowerReputationTier(tier: String): String {
        return when (tier) {
            "ELITE" -> "BIG"
            "BIG" -> "MID"
            "MID" -> "SMALL"
            else -> "SMALL"
        }
    }

    private fun getTierRank(tier: String): Int {
        return when (tier) {
            "ELITE" -> 4
            "BIG" -> 3
            "MID" -> 2
            "SMALL" -> 1
            else -> 1
        }
    }

    private fun isTierLowerThan(tier1: String, tier2: String): Boolean {
        return getTierRank(tier1) < getTierRank(tier2)
    }

    // Helper functions
    private fun getMonthName(monthIndex: Int): String {
        return listOf("August", "September", "October", "November", "December", "January", "February", "March", "April", "May")[monthIndex]
    }

    private fun getRepMultiplier(rep: String): Float {
        return when (rep) {
            "ELITE" -> 1.20f
            "BIG" -> 1.10f
            "MID" -> 1.00f
            "SMALL" -> 0.80f
            else -> 1.00f
        }
    }

    private fun getOpponentMultiplier(rep: String): Float {
        return when (rep) {
            "ELITE" -> 0.70f
            "BIG" -> 0.80f
            "MID" -> 1.00f
            "SMALL" -> 1.20f
            else -> 1.00f
        }
    }

    private fun getOutcomeChar(isHome: Boolean, homeScore: Int, awayScore: Int): String {
        return when {
            homeScore == awayScore -> "D"
            isHome && homeScore > awayScore -> "W"
            !isHome && awayScore > homeScore -> "W"
            else -> "L"
        }
    }

    private fun simulateTeamMatch(homeClub: ClubEntity, awayClub: ClubEntity): Pair<Int, Int> {
        val homeWeight = homeClub.reputationPoints + 10 // Home advantage
        val awayWeight = awayClub.reputationPoints

        // Goal ranges
        val homeBase = (Random.nextInt(0, 4) + (homeWeight - awayWeight) / 25f).coerceIn(0f, 6f).roundToInt()
        val awayBase = (Random.nextInt(0, 4) + (awayWeight - homeWeight) / 25f).coerceIn(0f, 6f).roundToInt()

        return Pair(homeBase, awayBase)
    }

    // Choice event generator
    fun generateMonthlyUnifiedChoiceEvent(
        player: PlayerEntity,
        clubName: String,
        rivalName: String,
        recentIds: List<String> = emptyList()
    ): UnifiedChoiceEvent {
        val events = mutableListOf<UnifiedChoiceEvent>()

        // 1. Rival form training press/media prompt
        events.add(
            UnifiedChoiceEvent(
                id = "rival_form_press",
                prompt = "A reporter asks for your comments on rival striker $rivalName's recent hot form in training.",
                option1 = "Praise them: 'He's a great player, we challenge each other.'",
                outcome1 = "Teammates and manager respect your class. Technique +1, Form +1, Rival Relationship +10.",
                techniqueMod1 = 1, formMod1 = 1, rivalRelMod1 = 10,
                
                option2 = "Confident: 'He's in great form, but I believe in my own quality.'",
                outcome2 = "Focused and self-assured. Finishing +1, Morale +5.",
                finishingMod2 = 1, moraleMod2 = 5,
                
                option3 = "Arrogant: 'He is not even in my league. The stats don't lie.'",
                outcome3 = if (player.form >= 2) {
                    "Heated! Fans love your swagger. Fan Rep +8, Morale +8, Rival Rel -15."
                } else {
                    "Reporters mock your arrogant claim paired with inconsistent form. Fan Rep -8, Rival Rel -15, Morale -5."
                },
                fanRepMod3 = if (player.form >= 2) 8 else -8,
                moraleMod3 = if (player.form >= 2) 8 else -5,
                rivalRelMod3 = -15
            )
        )

        // 2. Coach shooting vs Gym vs Rest
        events.add(
            UnifiedChoiceEvent(
                id = "training_focus_weekend",
                prompt = "The head coach suggests you spend the weekend practicing shooting accuracy or physical stamina.",
                option1 = "Practice Shooting (Technical Focus).",
                outcome1 = "Your finishing gets sharper. Finishing +1, Technique +1, Form +1.",
                finishingMod1 = 1, techniqueMod1 = 1, formMod1 = 1,
                
                option2 = "Hit the Gym (Physical Focus).",
                outcome2 = "Your physical stamina increases. Form +1, Fatigue -5.",
                formMod2 = 1, fatigueMod2 = -5,
                
                option3 = "Rest and recover (Mental Focus).",
                outcome3 = "Fully rested and stress-free. Fatigue -15, Morale +10.",
                fatigueMod3 = -15, moraleMod3 = 10
            )
        )

        // 3. Sponsor Offer
        events.add(
            UnifiedChoiceEvent(
                id = "sponsor_neon_boots",
                prompt = "A flashy athletic wear brand offers you an energy drink sponsorship, but it requires wearing loud neon boots.",
                option1 = "Accept: Rock the loud neon boots.",
                outcome1 = "Huge public buzz! Fan Reputation +12, Morale +6.",
                fanRepMod1 = 12, moraleMod1 = 6,
                
                option2 = "Decline: Prefer a classic black styling.",
                outcome2 = "Classy and pure. Manager Trust +8.",
                managerTrustMod2 = 8
            )
        )

        // 4. Hometown Visit
        events.add(
            UnifiedChoiceEvent(
                id = "hometown_visit",
                prompt = "During a gap in fixtures, you have a chance to visit your hometown family, but it requires a long flight.",
                option1 = "Go home: Reconnect with family and old friends.",
                outcome1 = "Emotionally refreshing, but physically exhausting. Morale +15, Fatigue +12.",
                moraleMod1 = 15, fatigueMod1 = 12,
                
                option2 = "Stay recover: Do extra ice baths and recovery.",
                outcome2 = "Fully rested and recovered. Fatigue -10, Manager Trust +4.",
                fatigueMod2 = -10, managerTrustMod2 = 4
            )
        )

        // 5. Minor Knock
        events.add(
            UnifiedChoiceEvent(
                id = "minor_knock",
                prompt = "You feel a slight twinge (minor knock) in your calf before a big fixture. Rest or play?",
                option1 = "Play through: 'Put me in, Coach!'",
                outcome1 = "Gritty determination. Manager Trust +10, Form +2, Fatigue +15.",
                managerTrustMod1 = 10, formMod1 = 2, fatigueMod1 = 15,
                
                option2 = "Rest: Avoid risking a worse tear.",
                outcome2 = "Smart recovery. Fatigue -15, Morale -5.",
                fatigueMod2 = -15, moraleMod2 = -5
            )
        )

        // 6. Rival talk struggling
        events.add(
            UnifiedChoiceEvent(
                id = "rival_struggling_talk",
                prompt = "Your rival striker $rivalName is struggling with form and approaches you for a talk after training.",
                option1 = "Mentor: Offer tips on his shooting posture.",
                outcome1 = "He appreciates your leadership. Rival Relationship +15.",
                rivalRelMod1 = 15,
                
                option2 = "Ignore: 'Not my problem. Figure it out yourself.'",
                outcome2 = "A cold brush-off. No effect.",
                
                option3 = "Trash-talk: 'Maybe you're just not cut out for this level.'",
                outcome3 = "He storms off furious! Rival Relationship -20, Morale +5.",
                rivalRelMod3 = -20, moraleMod3 = 5
            )
        )

        // 7. Rival Social Media Tag
        events.add(
            UnifiedChoiceEvent(
                id = "rival_social_tag",
                prompt = "$rivalName posts a social media video bragging about his training stats and tagging you. 'Ready for the next game?'",
                option1 = "Support him: 'Iron sharpens iron. Let's put on a show!'",
                outcome1 = "Mutual respect. Rival Relationship +12.",
                rivalRelMod1 = 12,
                
                option2 = "Ignore the tag entirely.",
                outcome2 = "Silence. No effect.",
                
                option3 = "Trash-talk: 'You do your talking on social media, I do mine on the pitch.'",
                outcome3 = "Fans love the spice! Fan Reputation +8, Rival Relationship -15.",
                fanRepMod3 = 8, rivalRelMod3 = -15
            )
        )

        // 8. Penalty Controversy
        events.add(
            UnifiedChoiceEvent(
                id = "penalty_controversy",
                prompt = "In the 88th minute of a tight match, your team earns a penalty. The designated taker reaches for the ball, but you want to take it to boost your goal tally.",
                option1 = "Demand the ball and take the spot-kick yourself.",
                outcome1 = "You score, but teammates view it as selfish behavior. Finishing +1, Form +1, Manager Trust -8, Rival Relationship -5.",
                finishingMod1 = 1, formMod1 = 1, managerTrustMod1 = -8, rivalRelMod1 = -5,

                option2 = "Defer to the designated penalty taker.",
                outcome2 = "Good sportsmanship builds team trust. Manager Trust +6, Morale +4.",
                managerTrustMod2 = 6, moraleMod2 = 4,

                option3 = "Step in to calm tension and cheer him on.",
                outcome3 = "Demonstrated maturity and squad leadership. Fan Reputation +6, Manager Trust +8.",
                fanRepMod3 = 6, managerTrustMod3 = 8
            )
        )

        // 9. Fixture Congestion
        events.add(
            UnifiedChoiceEvent(
                id = "fixture_congestion",
                prompt = "A heavy midweek cup match and weekend league fixture create severe fixture congestion. Your body feels completely drained.",
                option1 = "Push through both matches with extra caffeine and pain relief.",
                outcome1 = "Showed grit, but took a heavy physical toll. Form +1, Fatigue +25, Morale -5.",
                formMod1 = 1, fatigueMod1 = 25, moraleMod1 = -5,

                option2 = "Ask the manager for squad rotation in the midweek cup match.",
                outcome2 = "Preserved energy for league competition. Fatigue -15, Form +1, Manager Trust -4.",
                fatigueMod2 = -15, formMod2 = 1, managerTrustMod2 = -4,

                option3 = "Work with the physio on aggressive cryotherapy and massage.",
                outcome3 = "Balanced recovery protocol. Fatigue -10, Technique +1.",
                fatigueMod3 = -10, techniqueMod3 = 1
            )
        )

        // 10. Tactical Dispute
        events.add(
            UnifiedChoiceEvent(
                id = "tactical_dispute",
                prompt = "The manager instructs you to drop deeper as a false-9 to link play, limiting your inside-box scoring opportunities.",
                option1 = "Follow instructions strictly and focus on playmaking.",
                outcome1 = "Excellent tactical discipline. Technique +2, Manager Trust +10, Finishing -1.",
                techniqueMod1 = 2, managerTrustMod1 = 10, finishingMod1 = -1,

                option2 = "Ignore instructions and stay high in the penalty box looking for goals.",
                outcome2 = "Selfish positioning creates friction with management. Finishing +1, Manager Trust -12, Form -1.",
                finishingMod2 = 1, managerTrustMod2 = -12, formMod2 = -1,

                option3 = "Negotiate a middle ground: drift wide on counterattacks.",
                outcome3 = "Adaptable tactical compromise. Form +1, Manager Trust +4.",
                formMod3 = 1, managerTrustMod3 = 4
            )
        )

        // 11. Media Transfer Rumors
        events.add(
            UnifiedChoiceEvent(
                id = "media_transfer_rumors",
                prompt = "Journalists publish speculative rumors linking you to a lucrative transfer move away from $clubName.",
                option1 = "Pledge full loyalty to $clubName in the post-match interview.",
                outcome1 = "Local supporters adore your loyalty! Fan Reputation +15, Morale +6.",
                fanRepMod1 = 15, moraleMod1 = 6,

                option2 = "Give a cryptic answer: 'In football, you never know what the future holds.'",
                outcome2 = "Heightened speculation creates distraction. Fan Reputation -10, Rival Relationship -8, Morale -4.",
                fanRepMod2 = -10, rivalRelMod2 = -8, moraleMod2 = -4,

                option3 = "Refuse to comment on transfer speculation.",
                outcome3 = "Professional composure under media pressure. Manager Trust +6.",
                managerTrustMod3 = 6
            )
        )

        // 12. Aggressive Defender
        events.add(
            UnifiedChoiceEvent(
                id = "aggressive_defender",
                prompt = "An opposing center-back targets you with harsh late tackles and physical intimidation throughout the first half.",
                option1 = "Match his aggression and fight back physically.",
                outcome1 = "FIERY REACTION! Form +2, Fatigue +10, Manager Trust -6, Rival Relationship -10.",
                formMod1 = 2, fatigueMod1 = 10, managerTrustMod1 = -6, rivalRelMod1 = -10,

                option2 = "Keep your cool and exploit his over-aggression with quick passing.",
                outcome2 = "Composed performance under pressure. Technique +2, Finishing +1, Morale +8.",
                techniqueMod2 = 2, finishingMod2 = 1, moraleMod2 = 8,

                option3 = "Complain repeatedly to the head referee for protection.",
                outcome3 = "Opponents mock your frustration. Morale -6, Fan Reputation -5.",
                moraleMod3 = -6, fanRepMod3 = -5
            )
        )

        // 13. Decisive Late Match
        events.add(
            UnifiedChoiceEvent(
                id = "decisive_late_match",
                prompt = "Tied 1-1 in stoppage time, you find yourself 1-on-1 against the keeper from a tight angle.",
                option1 = "Blast a powerful shot at the near post.",
                outcome1 = "Powerful strike! Finishing +2, Form +1.",
                finishingMod1 = 2, formMod1 = 1,

                option2 = "Attempt an unselfish squared pass to a teammate in the center.",
                outcome2 = "Unselfish vision! Technique +2, Manager Trust +8, Morale +5.",
                techniqueMod2 = 2, managerTrustMod2 = 8, moraleMod2 = 5,

                option3 = "Try an audacious chip over the rushing goalkeeper.",
                outcome3 = "Audacious skill execution! Technique +2, Fan Reputation +10, Morale +10.",
                techniqueMod3 = 2, fanRepMod3 = 10, moraleMod3 = 10
            )
        )

        // 14. Contract Talk Pressure
        events.add(
            UnifiedChoiceEvent(
                id = "contract_talk_pressure",
                prompt = "Your agent informs you that club management wants to open early contract extension talks before season end.",
                option1 = "Instruct your agent to negotiate immediately.",
                outcome1 = "Security and stability boost your mindset. Morale +10, Manager Trust +6.",
                moraleMod1 = 10, managerTrustMod1 = 6,

                option2 = "Postpone talks until the end of the season to stay focused.",
                outcome2 = "Sharp focus maintained on match performance. Form +2, Manager Trust +4.",
                formMod2 = 2, managerTrustMod2 = 4,

                option3 = "Demand significantly higher wages through the press.",
                outcome3 = "Greedy demands anger the club hierarchy. Fan Reputation -12, Manager Trust -15.",
                fanRepMod3 = -12, managerTrustMod3 = -15
            )
        )

        val eligible = events.filter { !recentIds.contains(it.id) }
        return eligible.randomOrNull() ?: events.random()
    }

    suspend fun resolveChoice(optionIndex: Int) {
        db.withTransaction {
            val player = dao.getPlayerSync() ?: return@withTransaction
            val gameState = dao.getGameStateSync() ?: return@withTransaction
            
            val prompt = gameState.activeChoicePrompt ?: return@withTransaction
            val option = when (optionIndex) {
                1 -> gameState.activeChoiceOption1
                2 -> gameState.activeChoiceOption2
                3 -> gameState.activeChoiceOption3
                else -> gameState.activeChoiceOption1
            } ?: "Selected Option"
            
            val outcomeText = when (optionIndex) {
                1 -> gameState.activeChoiceOutcome1
                2 -> gameState.activeChoiceOutcome2
                3 -> gameState.activeChoiceOutcome3
                else -> ""
            } ?: ""
            
            val formMod = when (optionIndex) {
                1 -> gameState.activeChoiceFormMod1
                2 -> gameState.activeChoiceFormMod2
                3 -> gameState.activeChoiceFormMod3
                else -> 0
            }
            val finishingMod = when (optionIndex) {
                1 -> gameState.activeChoiceFinishingMod1
                2 -> gameState.activeChoiceFinishingMod2
                3 -> gameState.activeChoiceFinishingMod3
                else -> 0
            }
            val techniqueMod = when (optionIndex) {
                1 -> gameState.activeChoiceTechniqueMod1
                2 -> gameState.activeChoiceTechniqueMod2
                3 -> gameState.activeChoiceTechniqueMod3
                else -> 0
            }
            
            val moraleMod = when (optionIndex) {
                1 -> gameState.activeChoiceMoraleMod1
                2 -> gameState.activeChoiceMoraleMod2
                3 -> gameState.activeChoiceMoraleMod3
                else -> 0
            }
            val fanRepMod = when (optionIndex) {
                1 -> gameState.activeChoiceFanRepMod1
                2 -> gameState.activeChoiceFanRepMod2
                3 -> gameState.activeChoiceFanRepMod3
                else -> 0
            }
            val managerTrustMod = when (optionIndex) {
                1 -> gameState.activeChoiceManagerTrustMod1
                2 -> gameState.activeChoiceManagerTrustMod2
                3 -> gameState.activeChoiceManagerTrustMod3
                else -> 0
            }
            val rivalRelMod = when (optionIndex) {
                1 -> gameState.activeChoiceRivalRelMod1
                2 -> gameState.activeChoiceRivalRelMod2
                3 -> gameState.activeChoiceRivalRelMod3
                else -> 0
            }
            val fatigueMod = when (optionIndex) {
                1 -> gameState.activeChoiceFatigueMod1
                2 -> gameState.activeChoiceFatigueMod2
                3 -> gameState.activeChoiceFatigueMod3
                else -> 0
            }
            
            // Apply Choice outcome modifiers
            player.form = (player.form + formMod).coerceIn(-5, 5)
            player.finishing = applyStatGrowth(player.finishing, finishingMod.toFloat(), player.potentialCeiling)
            player.technique = applyStatGrowth(player.technique, techniqueMod.toFloat(), player.potentialCeiling)
            player.morale = (player.morale + moraleMod).coerceIn(0, 100)
            player.fanReputation = (player.fanReputation + fanRepMod).coerceIn(0, 100)
            player.managerTrust = (player.managerTrust + managerTrustMod).coerceIn(0, 100)
            player.rivalRelationship = (player.rivalRelationship + rivalRelMod).coerceIn(0, 100)
            player.fatigue = (player.fatigue + fatigueMod).coerceIn(0, 100)
            
            player.ovr = calculateOvr(player.finishing, player.pace, player.passing, player.physical, player.technique).coerceAtMost(player.potentialCeiling)
            
            val isManagerTalkPromise = prompt.contains("Manager Talk") && optionIndex == 3
            if (isManagerTalkPromise) {
                player.activePromiseGoalsAssists = 2
                player.activePromiseGamesRemaining = 3
            }
            
            dao.updatePlayer(player)
            
            val pendingLogs = gameState.activeChoicePendingMonthLogs
            val choiceLog = "\n💡 Choice Event:\n$prompt\n(Your Choice: $option -> $outcomeText)"
            
            if (pendingLogs != null) {
                // This was an end-of-month choice event, so we append the choice log and ADVANCE the month/season
                val finalMonthReport = pendingLogs + choiceLog
                gameState.narrativeLog = capNarrativeLog(finalMonthReport + "\n\n" + gameState.narrativeLog)
                
                // Clear choice state
                gameState.activeChoicePrompt = null
                gameState.activeChoiceOption1 = null
                gameState.activeChoiceOption2 = null
                gameState.activeChoiceOption3 = null
                gameState.activeChoiceFormMod1 = 0
                gameState.activeChoiceFinishingMod1 = 0
                gameState.activeChoiceTechniqueMod1 = 0
                gameState.activeChoiceOutcome1 = null
                gameState.activeChoiceFormMod2 = 0
                gameState.activeChoiceFinishingMod2 = 0
                gameState.activeChoiceTechniqueMod2 = 0
                gameState.activeChoiceOutcome2 = null
                gameState.activeChoiceFormMod3 = 0
                gameState.activeChoiceFinishingMod3 = 0
                gameState.activeChoiceTechniqueMod3 = 0
                gameState.activeChoiceOutcome3 = null
                gameState.activeChoiceMoraleMod1 = 0
                gameState.activeChoiceFanRepMod1 = 0
                gameState.activeChoiceManagerTrustMod1 = 0
                gameState.activeChoiceRivalRelMod1 = 0
                gameState.activeChoiceFatigueMod1 = 0
                gameState.activeChoiceMoraleMod2 = 0
                gameState.activeChoiceFanRepMod2 = 0
                gameState.activeChoiceManagerTrustMod2 = 0
                gameState.activeChoiceRivalRelMod2 = 0
                gameState.activeChoiceFatigueMod2 = 0
                gameState.activeChoiceMoraleMod3 = 0
                gameState.activeChoiceFanRepMod3 = 0
                gameState.activeChoiceManagerTrustMod3 = 0
                gameState.activeChoiceRivalRelMod3 = 0
                gameState.activeChoiceFatigueMod3 = 0
                gameState.activeChoicePendingMonthLogs = null
                
                // Decrement manager talk cooldown
                if (gameState.managerTalkCooldownMonths > 0) {
                    gameState.managerTalkCooldownMonths -= 1
                }

                // Clear persisted transfer offers since we are leaving the current month
                gameState.persistedTransferOffers = null

                // Advance Month / Season
                val currentMonth = gameState.currentMonthIndex
                val nextMonth = currentMonth + 1
                if (nextMonth <= 9) {
                    gameState.currentMonthIndex = nextMonth
                    if (nextMonth == 5) {
                        player.hasTransferredThisWindow = false
                        dao.updatePlayer(player)
                    }
                    dao.updateGameState(gameState)
                } else {
                    val endReport = handleEndOfSeason(player, gameState)
                    gameState.narrativeLog = capNarrativeLog(endReport + "\n\n" + gameState.narrativeLog)
                    gameState.currentSeason += 1
                    gameState.currentMonthIndex = 0
                    player.hasTransferredThisWindow = false
                    dao.updatePlayer(player)
                    dao.updateGameState(gameState)
                }
            } else {
                // This is a mid-month/mid-match event (e.g. post-match interview, rival dynamic, life event), so we DO NOT advance the month
                gameState.narrativeLog = capNarrativeLog("💡 Event Result:\n$prompt\n(Your Choice: $option -> $outcomeText)\n\n" + gameState.narrativeLog)
                
                // Clear choice state
                gameState.activeChoicePrompt = null
                gameState.activeChoiceOption1 = null
                gameState.activeChoiceOption2 = null
                gameState.activeChoiceOption3 = null
                gameState.activeChoiceFormMod1 = 0
                gameState.activeChoiceFinishingMod1 = 0
                gameState.activeChoiceTechniqueMod1 = 0
                gameState.activeChoiceOutcome1 = null
                gameState.activeChoiceFormMod2 = 0
                gameState.activeChoiceFinishingMod2 = 0
                gameState.activeChoiceTechniqueMod2 = 0
                gameState.activeChoiceOutcome2 = null
                gameState.activeChoiceFormMod3 = 0
                gameState.activeChoiceFinishingMod3 = 0
                gameState.activeChoiceTechniqueMod3 = 0
                gameState.activeChoiceOutcome3 = null
                gameState.activeChoiceMoraleMod1 = 0
                gameState.activeChoiceFanRepMod1 = 0
                gameState.activeChoiceManagerTrustMod1 = 0
                gameState.activeChoiceRivalRelMod1 = 0
                gameState.activeChoiceFatigueMod1 = 0
                gameState.activeChoiceMoraleMod2 = 0
                gameState.activeChoiceFanRepMod2 = 0
                gameState.activeChoiceManagerTrustMod2 = 0
                gameState.activeChoiceRivalRelMod2 = 0
                gameState.activeChoiceFatigueMod2 = 0
                gameState.activeChoiceMoraleMod3 = 0
                gameState.activeChoiceFanRepMod3 = 0
                gameState.activeChoiceManagerTrustMod3 = 0
                gameState.activeChoiceRivalRelMod3 = 0
                gameState.activeChoiceFatigueMod3 = 0
                gameState.activeChoicePendingMonthLogs = null
                
                dao.updateGameState(gameState)
            }
        }
    }

    suspend fun getNextPlayerMatchInMonth(monthIndex: Int, playerClubId: Int): FixtureEntity? {
        if (playerClubId <= 0) return null
        val fixtures = dao.getFixturesForMonthSync(monthIndex)
        return fixtures.firstOrNull { 
            (it.homeClubId == playerClubId || it.awayClubId == playerClubId) && !it.isSimulated 
        }
    }

    suspend fun calculateRotationForFixture(fixture: FixtureEntity, player: PlayerEntity, monthIndex: Int): RotationResult {
        val playerClubId = player.currentClubId
        val homeClub = dao.getClubById(fixture.homeClubId) ?: return RotationResult(null, true)
        val awayClub = dao.getClubById(fixture.awayClubId) ?: return RotationResult(null, true)

        val isPlayerHome = fixture.homeClubId == playerClubId
        val myClub = if (isPlayerHome) homeClub else awayClub

        val rivalOvr = myClub.rivalStrikerOvr
        val isAcademy = player.age in 13..15
        val rivalOvrBuff = if (player.rivalRelationship < 30) 4 else 0
        val rivalEffectiveOvr = (if (isAcademy) 30 else rivalOvr) + rivalOvrBuff
        val diff = (player.ovr + player.form) - rivalEffectiveOvr

        var startProb = 0.5f + (diff * 0.035f)
        val isHighStakes = fixture.competition != "LEAGUE" || (monthIndex == 9)
        if (isHighStakes) {
            startProb = 0.5f + (diff * 0.05f)
        }

        // managerTrust effects
        if (player.managerTrust >= 70) {
            startProb += 0.20f
            if (player.form >= -2 && player.fatigue <= 40) {
                startProb = startProb.coerceAtLeast(0.65f)
            }
        } else if (player.managerTrust < 30) {
            startProb -= 0.30f
            startProb = startProb.coerceAtMost(0.15f)
        }

        startProb = startProb.coerceIn(if (isHighStakes) 0.02f else 0.05f, if (isHighStakes) 0.98f else 0.95f)

        val rand = Random.nextFloat()
        return if (rand < startProb) {
            RotationResult(0, false)
        } else {
            if (Random.nextFloat() < 0.60f) {
                RotationResult(Random.nextInt(60, 81), false)
            } else {
                RotationResult(null, true)
            }
        }
    }

    suspend fun trainPlayer(focus: String, grade: String): String {
        var logMessage = ""
        db.withTransaction {
            val player = dao.getPlayerSync() ?: return@withTransaction
            val gameState = dao.getGameStateSync() ?: return@withTransaction

            // 1. Fatigue cost of training session
            val baseFatigueCost = 10
            // High overtraining risk can increase fatigue accumulation rate
            val overtrainingFactor = if (player.overtrainingRisk > 50) 1.5f else 1.0f
            val fatigueAdded = (baseFatigueCost * overtrainingFactor).toInt()
            player.fatigue = (player.fatigue + fatigueAdded).coerceIn(0, 100)

            // 2. Increase overtraining risk (frequency)
            player.overtrainingRisk = (player.overtrainingRisk + 25).coerceIn(0, 100)

            // 3. Optional minor knock if overtraining risk is too high
            var hadKnock = false
            if (player.overtrainingRisk > 70) {
                // 30% chance of minor knock
                if (Random.nextFloat() < 0.30f) {
                    hadKnock = true
                    // a short-term fatigue spike
                    player.fatigue = (player.fatigue + 20).coerceIn(0, 100)
                    logMessage = "⚠️ Overtraining Alert: You pushed yourself too hard and suffered a minor knock during training! Fatigue spiked by +20."
                }
            }

            // 4. Calculate growth multiplier based on grade
            val multiplier = when (grade) {
                "S" -> 1.5f
                "A" -> 1.2f
                "B" -> 0.8f
                "C" -> 0.4f
                else -> 0.0f
            }

            // Standard monthly contribution
            val baseContribution = 0.25f
            val bonus = baseContribution * multiplier

            // Apply bonus based on chosen focus
            when (focus.uppercase()) {
                "FINISHING" -> player.finishingTrainingBonus += bonus
                "PACE" -> player.paceTrainingBonus += bonus
                "PASSING" -> player.passingTrainingBonus += bonus
                "PHYSICAL" -> player.physicalTrainingBonus += bonus
                "TECHNIQUE" -> player.techniqueTrainingBonus += bonus
            }

            // Mark as trained this month
            player.hasTrainedThisMonth = 1

            dao.updatePlayer(player)

            if (!hadKnock) {
                logMessage = "💪 Training Complete: Focus on $focus (Grade $grade). You earned a +${"%.2f".format(bonus)} bonus to season-end growth."
            }

            // Append to narrative log
            gameState.narrativeLog = capNarrativeLog("🏋️ $logMessage\n" + gameState.narrativeLog)
            dao.updateGameState(gameState)
        }
        return logMessage
    }

    suspend fun autoSimulateBenchedMatch(fixture: FixtureEntity, player: PlayerEntity, rotation: RotationResult) {
        db.withTransaction {
            val homeClub = dao.getClubById(fixture.homeClubId) ?: return@withTransaction
            val awayClub = dao.getClubById(fixture.awayClubId) ?: return@withTransaction

            val result = simulateTeamMatch(homeClub, awayClub)
            fixture.homeScore = result.first
            fixture.awayScore = result.second
            fixture.isSimulated = true
            fixture.playerCameOnMinute = null
            fixture.playerGoals = 0
            fixture.playerAssists = 0
            fixture.playerMvp = false

            val isPlayerHome = fixture.homeClubId == player.currentClubId
            val myClub = if (isPlayerHome) homeClub else awayClub
            val oppClub = if (isPlayerHome) awayClub else homeClub

            val outcomeChar = getOutcomeChar(isPlayerHome, fixture.homeScore!!, fixture.awayScore!!)
            val scoreStr = "${fixture.homeScore}-${fixture.awayScore}"
            
            // Append log to narrative Log
            val logLine = "🪑 Benched vs ${oppClub.name}: ($outcomeChar) $scoreStr."
            val gameState = dao.getGameStateSync()
            if (gameState != null) {
                gameState.narrativeLog = capNarrativeLog("Season ${gameState.currentSeason} (${formatSeasonYear(gameState.currentSeason)}), ${getMonthName(gameState.currentMonthIndex)}:\n$logLine\n\n" + gameState.narrativeLog)
                checkAndUpdateManagerPromise(player, fixture, gameState)
                dao.updateGameState(gameState)
            }

            // Drop form by 1
            player.form = (player.form - 1).coerceIn(-5, 5)
            dao.updatePlayer(player)

            checkAndResolveFixtureTie(fixture)
            dao.updateFixture(fixture)

            if (fixture.competition == "LEAGUE") {
                updateDomesticStandings(fixture)
            }
        }
    }

    suspend fun quickSimPlayingMatch(fixture: FixtureEntity, playerCameOnMinute: Int) {
        db.withTransaction {
            val player = dao.getPlayerSync() ?: return@withTransaction
            val homeClub = dao.getClubById(fixture.homeClubId) ?: return@withTransaction
            val awayClub = dao.getClubById(fixture.awayClubId) ?: return@withTransaction

            val result = simulateTeamMatch(homeClub, awayClub)
            fixture.homeScore = result.first
            fixture.awayScore = result.second
            fixture.isSimulated = true
            fixture.playerCameOnMinute = playerCameOnMinute

            val isPlayerHome = fixture.homeClubId == player.currentClubId
            val myClub = if (isPlayerHome) homeClub else awayClub
            val oppClub = if (isPlayerHome) awayClub else homeClub

            val myTeamGoals = if (isPlayerHome) result.first else result.second
            val myTeamRepFactor = getRepMultiplier(myClub.reputation)
            val oppRepFactor = getOpponentMultiplier(oppClub.reputation)

            // Personal goal probability with Quick Sim Penalty (-15% odds, or multiply by 0.85)
            val penalty = 0.85f
            val goalProb = (player.finishing / 100.0f) * myTeamRepFactor * oppRepFactor * (1.0f + (player.form * 0.05f)) * penalty
            val assistProb = (player.passing / 100.0f) * myTeamRepFactor * oppRepFactor * (1.0f + (player.form * 0.05f)) * penalty

            var playerGoals = 0
            var playerAssists = 0

            if (myTeamGoals > 0) {
                for (g in 1..myTeamGoals) {
                    if (Random.nextFloat() < (goalProb * 0.45f) && playerGoals < myTeamGoals) {
                        playerGoals++
                    } else if (Random.nextFloat() < (assistProb * 0.30f) && (playerGoals + playerAssists < myTeamGoals)) {
                        playerAssists++
                    }
                }
            }

            fixture.playerGoals = playerGoals
            fixture.playerAssists = playerAssists
            val startMin = playerCameOnMinute
            fixture.goalMinutes = if (playerGoals > 0) List(playerGoals) { Random.nextInt(if (startMin == 0) 1 else startMin, 91) }.sorted().joinToString(",") else null
            fixture.assistMinutes = if (playerAssists > 0) List(playerAssists) { Random.nextInt(if (startMin == 0) 1 else startMin, 91) }.sorted().joinToString(",") else null

            // Calculate match rating and MVP
            val matchRating = (6.0f + (playerGoals * 1.5f) + (playerAssists * 1.0f) + (player.finishing + player.pace + player.physical) * 0.005f + (Random.nextFloat() * 0.5f)).coerceIn(1.0f, 10.0f)
            fixture.playerRating = matchRating
            val ratingStr = "%.1f".format(matchRating)

            val earnedMvp = matchRating > 8.0f && (playerGoals > 0 || playerAssists > 0) && (Random.nextFloat() < 0.65f)
            fixture.playerMvp = earnedMvp

            // Update player's form shift
            var formDelta = -1
            if (playerGoals > 0 || playerAssists > 0) formDelta = 1
            if (playerGoals >= 2 || earnedMvp) formDelta = 2
            player.form = (player.form + formDelta).coerceIn(-5, 5)

            // Update player fatigue with overtraining multiplier
            val subOff = fixture.playerSubbedOffMinute ?: 90
            val cameOn = fixture.playerCameOnMinute ?: 0
            val minutesPlayed = (subOff - cameOn).coerceAtLeast(0)
            val fatigueMultiplier = if (player.overtrainingRisk > 50) 1.5f else 1.0f
            val fatigueCost = kotlin.math.ceil(15f * minutesPlayed / 90f).toInt()
            val finalFatigueCost = kotlin.math.ceil(fatigueCost * fatigueMultiplier).toInt()
            player.fatigue = (player.fatigue + finalFatigueCost).coerceIn(0, 100)

            // Update player season/career aggregates
            player.gamesPlayed += 1
            player.goals += playerGoals
            player.assists += playerAssists
            player.mvps += if (earnedMvp) 1 else 0

            player.seasonGamesPlayed += 1
            player.seasonGoals += playerGoals
            player.seasonAssists += playerAssists
            player.seasonMvps += if (earnedMvp) 1 else 0

            player.clubGamesPlayed += 1
            player.clubGoals += playerGoals
            player.clubAssists += playerAssists

            // Apply morale and reputation passive post-match effects
            val isWin = if (isPlayerHome) result.first > result.second else result.second > result.first
            val isLoss = if (isPlayerHome) result.first < result.second else result.second < result.first
            if (isWin && playerGoals > 0) {
                player.morale = (player.morale + 3).coerceIn(0, 100)
            }
            if (isLoss && matchRating < 6.0f) {
                player.morale = (player.morale - 4).coerceIn(0, 100)
            }
            if (!isPlayerHome && player.fanReputation < 30) {
                player.morale = (player.morale - 2).coerceIn(0, 100)
            }

            dao.updatePlayer(player)

            // Save fixture
            checkAndResolveFixtureTie(fixture)
            dao.updateFixture(fixture)

            if (fixture.competition == "LEAGUE") {
                updateDomesticStandings(fixture)
            }

            // Write to narrative feed
            val matchTypeStr = when (fixture.competition) {
                "LEAGUE" -> "League match"
                "CHAMPIONS_LEAGUE" -> "Champions League"
                "EUROPA_LEAGUE" -> "Europa League"
                "CONFERENCE_LEAGUE" -> "Conference League"
                "SUPER_CUP" -> "Super Cup"
                else -> "Cup match"
            }
            val roleStr = if (playerCameOnMinute == 0) "Started" else "Subbed in (${playerCameOnMinute}')"
            val contribStr = if (playerGoals > 0 || playerAssists > 0) {
                "${playerGoals}G, ${playerAssists}A" + (if (earnedMvp) " (MVP⭐)" else "")
            } else "No goals/assists"

            val outcomeChar = getOutcomeChar(isPlayerHome, fixture.homeScore!!, fixture.awayScore!!)
            val scoreStr = "${fixture.homeScore}-${fixture.awayScore}"

            val logLine = "⚡ Quick Sim: ⚽ $matchTypeStr vs ${oppClub.name}: ($outcomeChar) $scoreStr. $roleStr, Rating: $ratingStr. $contribStr."
            
            val gameState = dao.getGameStateSync()
            if (gameState != null) {
                gameState.narrativeLog = capNarrativeLog("Season ${gameState.currentSeason} (${formatSeasonYear(gameState.currentSeason)}), ${getMonthName(gameState.currentMonthIndex)}:\n$logLine\n\n" + gameState.narrativeLog)
                checkAndUpdateManagerPromise(player, fixture, gameState)
                checkAndTriggerPostMatchInterview(player, fixture, gameState)
                dao.updatePlayer(player)
                dao.updateGameState(gameState)
            }
        }
    }

    private suspend fun checkAndUpdateManagerPromise(player: PlayerEntity, fixture: FixtureEntity, gameState: GameStateEntity) {
        val gamesRemaining = player.activePromiseGamesRemaining
        if (gamesRemaining != null && gamesRemaining > 0) {
            val goalsAssistsMade = fixture.playerGoals + fixture.playerAssists
            val targetLeft = player.activePromiseGoalsAssists ?: 0
            val newTargetLeft = maxOf(0, targetLeft - goalsAssistsMade)
            player.activePromiseGoalsAssists = newTargetLeft
            
            val newGamesRemaining = gamesRemaining - 1
            player.activePromiseGamesRemaining = newGamesRemaining
            
            if (newGamesRemaining == 0) {
                if (newTargetLeft == 0) {
                    player.managerTrust = (player.managerTrust + 20).coerceIn(0, 100)
                    gameState.narrativeLog = capNarrativeLog("👔 PROMISE KEPT: You fulfilled your promise to the manager! Manager Trust +20 (Now: ${player.managerTrust}).\n\n" + gameState.narrativeLog)
                } else {
                    player.managerTrust = (player.managerTrust - 30).coerceIn(0, 100)
                    gameState.narrativeLog = capNarrativeLog("👔 PROMISE BROKEN: You failed to fulfill your promise of $targetLeft G/A. Manager Trust -30 (Now: ${player.managerTrust}).\n\n" + gameState.narrativeLog)
                }
                player.activePromiseGamesRemaining = null
                player.activePromiseGoalsAssists = null
            } else {
                gameState.narrativeLog = capNarrativeLog("👔 PROMISE STATUS: $newTargetLeft G/A remaining in $newGamesRemaining games to fulfill your manager promise.\n\n" + gameState.narrativeLog)
            }
        }
    }

    private suspend fun checkAndTriggerPostMatchInterview(player: PlayerEntity, fixture: FixtureEntity, gameState: GameStateEntity) {
        if (gameState.activeChoicePrompt != null) return

        val isMvp = fixture.playerMvp
        val isHatTrick = fixture.playerGoals >= 3
        val isDerby = (fixture.competition == "LEAGUE" && Random.nextFloat() < 0.25f)
        val isTitleDecider = (fixture.competition == "SUPER_CUP" || (fixture.competition != "LEAGUE" && Random.nextFloat() < 0.15f))
        
        if (isMvp || isHatTrick || isDerby || isTitleDecider) {
            if (Random.nextFloat() < 0.40f) {
                val myClub = dao.getClubById(player.currentClubId)
                val rivalStriker = myClub?.rivalStrikerName ?: "Rival"
                val interviewType = Random.nextInt(1, 4)
                
                if (interviewType == 1) {
                    gameState.activeChoicePrompt = "🎙️ Post-Match Press Conference: Journalists crowd around you. 'How do you feel about your personal performance out there today?'"
                    gameState.activeChoiceOption1 = "Humble: 'It was a team effort. My teammates put it on a plate for me.'"
                    gameState.activeChoiceOutcome1 = "Teammates and manager appreciate your modesty. Fan Rep +3, Manager Trust +5, Morale +2."
                    gameState.activeChoiceFanRepMod1 = 3
                    gameState.activeChoiceManagerTrustMod1 = 5
                    gameState.activeChoiceMoraleMod1 = 2
                    
                    gameState.activeChoiceOption2 = "Confident: 'I've been training hard. I knew I had this performance in me.'"
                    gameState.activeChoiceOutcome2 = "Solid, self-assured answer. Fan Rep +5, Morale +4, Manager Trust +2."
                    gameState.activeChoiceFanRepMod2 = 5
                    gameState.activeChoiceMoraleMod2 = 4
                    gameState.activeChoiceManagerTrustMod2 = 2
                    
                    gameState.activeChoiceOption3 = "Arrogant: 'Nobody in this league can stop me when I'm in this zone.'"
                    if (player.form < 2) {
                        gameState.activeChoiceOutcome3 = "Reporters mock your arrogance paired with poor form. Media criticism: 'Delusional.' Fan Rep -10, Manager Trust -8, Morale -5."
                        gameState.activeChoiceFanRepMod3 = -10
                        gameState.activeChoiceManagerTrustMod3 = -8
                        gameState.activeChoiceMoraleMod3 = -5
                    } else {
                        gameState.activeChoiceOutcome3 = "The media laps up your swagger! Fan Rep +10, Morale +8, Manager Trust -5."
                        gameState.activeChoiceFanRepMod3 = 10
                        gameState.activeChoiceMoraleMod3 = 8
                        gameState.activeChoiceManagerTrustMod3 = -5
                    }
                } else if (interviewType == 2) {
                    gameState.activeChoicePrompt = "🎙️ Derby Special Interview: A journalist asks, 'Your rivals played a very physical game today. What is your take on their tactics?'"
                    gameState.activeChoiceOption1 = "Humble: 'They are tough competitors. We respect their play style.'"
                    gameState.activeChoiceOutcome1 = "Professional and respectful. Manager Trust +6, Rival Relationship +8."
                    gameState.activeChoiceManagerTrustMod1 = 6
                    gameState.activeChoiceRivalRelMod1 = 8
                    
                    gameState.activeChoiceOption2 = "Confident: 'They tried to throw us off, but we focused on our football.'"
                    gameState.activeChoiceOutcome2 = "Strong, positive response. Morale +6, Fan Rep +5."
                    gameState.activeChoiceMoraleMod2 = 6
                    gameState.activeChoiceFanRepMod2 = 5
                    
                    gameState.activeChoiceOption3 = "Arrogant: 'They tried to kick us because they can't play football. It was pathetic.'"
                    gameState.activeChoiceOutcome3 = "Spicy headline drama! Fans love the heat, but rival striker $rivalStriker is furious. Fan Rep +10, Rival Rel -15, Manager Trust -6."
                    gameState.activeChoiceFanRepMod3 = 10
                    gameState.activeChoiceRivalRelMod3 = -15
                    gameState.activeChoiceManagerTrustMod3 = -6
                } else {
                    gameState.activeChoicePrompt = "🎙️ Press Interview: silverware and ambitions. 'Are you ready to lead this club to glory?'"
                    gameState.activeChoiceOption1 = "Humble: 'I just want to help the team in any way I can. It's about the badge.'"
                    gameState.activeChoiceOutcome1 = "The club is thrilled by your selflessness. Manager Trust +8, Fan Rep +4."
                    gameState.activeChoiceManagerTrustMod1 = 8
                    gameState.activeChoiceFanRepMod1 = 4
                    
                    gameState.activeChoiceOption2 = "Confident: 'We've worked hard all season. We are fully prepared to win.'"
                    gameState.activeChoiceOutcome2 = "Fans and teammates are highly motivated. Morale +6, Fan Rep +6."
                    gameState.activeChoiceMoraleMod2 = 6
                    gameState.activeChoiceFanRepMod2 = 6
                    
                    gameState.activeChoiceOption3 = "Arrogant: 'This club was nothing before I got here. I will win this single-handedly.'"
                    if (player.form < 2) {
                        gameState.activeChoiceOutcome3 = "The media slams your arrogance. Headline: 'Single-Handed Failure.' Fan Rep -12, Manager Trust -12, Morale -8."
                        gameState.activeChoiceFanRepMod3 = -12
                        gameState.activeChoiceManagerTrustMod3 = -12
                        gameState.activeChoiceMoraleMod3 = -8
                    } else {
                        gameState.activeChoiceOutcome3 = "Arrogant, but backed up on the pitch. Fan Rep +12, Morale +10, Manager Trust -8."
                        gameState.activeChoiceFanRepMod3 = 12
                        gameState.activeChoiceMoraleMod3 = 10
                        gameState.activeChoiceManagerTrustMod3 = -8
                    }
                }
            }
        }
    }

    suspend fun resolvePlayedMatch(
        fixture: FixtureEntity,
        playerCameOnMinute: Int,
        playerGoals: Int,
        playerAssists: Int,
        finalHomeScore: Int,
        finalAwayScore: Int,
        minutesPlayed: Int,
        matchRating: Float,
        goalMinutes: String? = null,
        assistMinutes: String? = null
    ) {
        db.withTransaction {
            val player = dao.getPlayerSync() ?: return@withTransaction
            val homeClub = dao.getClubById(fixture.homeClubId) ?: return@withTransaction
            val awayClub = dao.getClubById(fixture.awayClubId) ?: return@withTransaction

            fixture.homeScore = finalHomeScore
            fixture.awayScore = finalAwayScore
            fixture.isSimulated = true
            fixture.playerCameOnMinute = playerCameOnMinute
            fixture.playerSubbedOffMinute = (playerCameOnMinute + minutesPlayed).coerceAtMost(90)
            fixture.playerGoals = playerGoals
            fixture.playerAssists = playerAssists
            fixture.playerRating = matchRating.coerceIn(1.0f, 10.0f)
            fixture.goalMinutes = goalMinutes
            fixture.assistMinutes = assistMinutes

            val isPlayerHome = fixture.homeClubId == player.currentClubId
            val myClub = if (isPlayerHome) homeClub else awayClub
            val oppClub = if (isPlayerHome) awayClub else homeClub

            // Store the rating passed from MatchScreen
            val ratingStr = "%.1f".format(matchRating)

            val earnedMvp = matchRating > 8.0f && (playerGoals > 0 || playerAssists > 0) && (Random.nextFloat() < 0.65f)
            fixture.playerMvp = earnedMvp

            // Update player's form shift
            var formDelta = -1
            if (playerGoals > 0 || playerAssists > 0) formDelta = 1
            if (playerGoals >= 2 || earnedMvp) formDelta = 2
            player.form = (player.form + formDelta).coerceIn(-5, 5)

            // Update player fatigue dynamically based on minutes played with overtraining multiplier
            val fatigueMultiplier = if (player.overtrainingRisk > 50) 1.5f else 1.0f
            val fatigueCost = kotlin.math.ceil(15f * minutesPlayed / 90f).toInt().coerceIn(0, 15)
            val finalFatigueCost = kotlin.math.ceil(fatigueCost * fatigueMultiplier).toInt()
            player.fatigue = (player.fatigue + finalFatigueCost).coerceIn(0, 100)

            // Update player season/career aggregates
            player.gamesPlayed += 1
            player.goals += playerGoals
            player.assists += playerAssists
            player.mvps += if (earnedMvp) 1 else 0

            player.seasonGamesPlayed += 1
            player.seasonGoals += playerGoals
            player.seasonAssists += playerAssists
            player.seasonMvps += if (earnedMvp) 1 else 0

            player.clubGamesPlayed += 1
            player.clubGoals += playerGoals
            player.clubAssists += playerAssists

            // Apply morale and reputation passive post-match effects
            val isWin = if (isPlayerHome) finalHomeScore > finalAwayScore else finalAwayScore > finalHomeScore
            val isLoss = if (isPlayerHome) finalHomeScore < finalAwayScore else finalAwayScore < finalHomeScore
            if (isWin && playerGoals > 0) {
                player.morale = (player.morale + 3).coerceIn(0, 100)
            }
            if (isLoss && matchRating < 6.0f) {
                player.morale = (player.morale - 4).coerceIn(0, 100)
            }
            if (!isPlayerHome && player.fanReputation < 30) {
                player.morale = (player.morale - 2).coerceIn(0, 100)
            }

            dao.updatePlayer(player)

            // Save fixture
            checkAndResolveFixtureTie(fixture)
            dao.updateFixture(fixture)

            if (fixture.competition == "LEAGUE") {
                updateDomesticStandings(fixture)
            }

            // Write to narrative feed
            val matchTypeStr = when (fixture.competition) {
                "LEAGUE" -> "League match"
                "CHAMPIONS_LEAGUE" -> "Champions League"
                "EUROPA_LEAGUE" -> "Europa League"
                "CONFERENCE_LEAGUE" -> "Conference League"
                "SUPER_CUP" -> "Super Cup"
                else -> "Cup match"
            }
            val roleStr = if (playerCameOnMinute == 0) "Started" else "Subbed in (${playerCameOnMinute}')"
            val contribStr = if (playerGoals > 0 || playerAssists > 0) {
                "${playerGoals}G, ${playerAssists}A" + (if (earnedMvp) " (MVP⭐)" else "")
            } else "No goals/assists"

            val outcomeChar = getOutcomeChar(isPlayerHome, finalHomeScore, finalAwayScore)
            val scoreStr = "$finalHomeScore-$finalAwayScore"

            val logLine = "🎮 Match Played: ⚽ $matchTypeStr vs ${oppClub.name}: ($outcomeChar) $scoreStr. $roleStr, Rating: $ratingStr. $contribStr."
            
            val gameState = dao.getGameStateSync()
            if (gameState != null) {
                gameState.narrativeLog = capNarrativeLog("Season ${gameState.currentSeason} (${formatSeasonYear(gameState.currentSeason)}), ${getMonthName(gameState.currentMonthIndex)}:\n$logLine\n\n" + gameState.narrativeLog)
                checkAndUpdateManagerPromise(player, fixture, gameState)
                checkAndTriggerPostMatchInterview(player, fixture, gameState)
                dao.updatePlayer(player)
                dao.updateGameState(gameState)
            }
        }
    }

    suspend fun simulateRemainingMonth(isAutoSim: Boolean = false): String {
        var narrativeReport = ""
        db.withTransaction {
            val player = dao.getPlayerSync() ?: return@withTransaction
            val gameState = dao.getGameStateSync() ?: return@withTransaction
            val currentMonth = gameState.currentMonthIndex

            // Narrative logs for this month
            val monthLogs = mutableListOf<String>()

            // Fetch and simulate all remaining matches scheduled for this month
            val fixturesToSimulate = dao.getFixturesForMonthSync(currentMonth)
            val allClubsMap = dao.getAllClubsSync().associateBy { it.id }

            val playerClubId = player.currentClubId
            val isAcademy = player.age in 13..15
            val otherMatches = if (isAcademy) {
                fixturesToSimulate.filter { !it.isSimulated }
            } else {
                fixturesToSimulate.filter { it.homeClubId != playerClubId && it.awayClubId != playerClubId && !it.isSimulated }
            }

            if (otherMatches.isNotEmpty()) {
                val allStandingsMap = dao.getAllStandingsSync().associateBy { it.clubId }
                // Simulate all OTHER matches (not involving player)
                for (fixture in otherMatches) {
                    val homeClub = allClubsMap[fixture.homeClubId] ?: continue
                    val awayClub = allClubsMap[fixture.awayClubId] ?: continue

                    val result = simulateTeamMatch(homeClub, awayClub)
                    fixture.homeScore = result.first
                    fixture.awayScore = result.second
                    fixture.isSimulated = true

                    checkAndResolveFixtureTie(fixture)

                    if (fixture.competition == "LEAGUE") {
                        val hStanding = allStandingsMap[fixture.homeClubId]
                        val aStanding = allStandingsMap[fixture.awayClubId]
                        if (hStanding != null && aStanding != null) {
                            updateStandingInMemory(hStanding, aStanding, result.first, result.second)
                        }
                    }
                }
                dao.updateFixtures(otherMatches)
                dao.updateStandings(allStandingsMap.values.toList())
            }

            // Recover fatigue at end of month, affected by overtraining risk:
            val baseRecovery = 20
            val recoveryAmount = when {
                player.overtrainingRisk > 70 -> 8
                player.overtrainingRisk > 40 -> 14
                else -> baseRecovery
            }
            player.fatigue = (player.fatigue - recoveryAmount).coerceAtLeast(0)

            // Decay overtraining risk:
            val decayAmount = if (player.hasTrainedThisMonth == 1) 10 else 30
            player.overtrainingRisk = (player.overtrainingRisk - decayAmount).coerceAtLeast(0)

            // Reset trained this month flag
            player.hasTrainedThisMonth = 0

            dao.updatePlayer(player)

            // Monthly Summary from player matches in this month
            val playerMatchesThisMonth = fixturesToSimulate.filter {
                (it.homeClubId == playerClubId || it.awayClubId == playerClubId) && it.isSimulated
            }
            val playedMatchCount = playerMatchesThisMonth.count { it.playerCameOnMinute != null }
            val goalsThisMonth = playerMatchesThisMonth.sumOf { it.playerGoals }
            val assistsThisMonth = playerMatchesThisMonth.sumOf { it.playerAssists }

            if (playedMatchCount > 0) {
                val formIndicator = when {
                    player.form >= 3 -> "🔥 Hot Form"
                    player.form <= -3 -> "❄️ Cold Form"
                    else -> "⚡ Stable Form"
                }
                monthLogs.add("📊 Monthly Summary: Played $playedMatchCount games, scored $goalsThisMonth goals, $assistsThisMonth assists. ($formIndicator)")
            } else {
                monthLogs.add("📊 Monthly Summary: Did not play any matches this month. Form continues to slide.")
            }

            // Perform Knockout Progression Checks
            if (currentMonth == 2) {
                progressKnockoutRound("CHAMPIONS_LEAGUE", 10, 8, 3)
                progressKnockoutRound("EUROPA_LEAGUE", 10, 8, 3)
                progressKnockoutRound("CONFERENCE_LEAGUE", 10, 8, 3)
                monthLogs.add("🏆 European cup preliminary rounds finished. Quarter-final brackets are drawn!")
            }
            if (currentMonth == 4) {
                progressKnockoutRound("CHAMPIONS_LEAGUE", 8, 4, 5)
                progressKnockoutRound("EUROPA_LEAGUE", 8, 4, 5)
                progressKnockoutRound("CONFERENCE_LEAGUE", 8, 4, 5)
                monthLogs.add("🏆 European cup quarter-finals finished. Semi-final brackets are drawn!")
            }
            if (currentMonth == 6) {
                progressKnockoutRound("CHAMPIONS_LEAGUE", 4, 2, 8)
                progressKnockoutRound("EUROPA_LEAGUE", 4, 2, 8)
                progressKnockoutRound("CONFERENCE_LEAGUE", 4, 2, 8)
                monthLogs.add("🏆 European cup semi-finals finished. The finalists are locked in for April!")
            }
            if (currentMonth == 8) {
                scheduleSuperCup()
                monthLogs.add("🏆 Champions League and Europa League winners are crowned! Super Cup scheduled for May.")
            }

            // Check for mid-contract early termination
            if (player.contractYearsRemaining > 1 && player.seasonGamesPlayed >= 10) {
                val availableMinutes = 30 * 90f
                val playedPercentage = (player.seasonGamesPlayed * 90f) / availableMinutes
                if (playedPercentage >= 0.50f) {
                    val actualTally = player.seasonGoals + player.seasonAssists
                    val progressRatio = actualTally.toFloat() / max(1, player.contractTargetGoalsAssists)
                    if (progressRatio < 0.15f && player.contractTargetGoalsAssists >= 5) {
                        player.contractYearsRemaining = 0
                        player.contractTargetGoalsAssists = 0
                        monthLogs.add("\n⚠️ CONTRACT TERMINATED: Due to poor performances (scoring only $actualTally in ${player.seasonGamesPlayed} games), your club has terminated your contract early! You are now a free agent.")
                    }
                }
            }

            // Save player stats
            dao.updatePlayer(player)

            // Random monthly Choice Event (60% chance, skipped during auto sim)
            val triggerChoice = if (isAutoSim) false else (Random.nextFloat() < 0.60f)
            if (triggerChoice) {
                val myClub = dao.getClubById(player.currentClubId)
                val clubName = myClub?.name ?: "Club"
                val rivalName = myClub?.rivalStrikerName ?: "Rival"
                val recentIds = if (gameState.recentChoiceEventIds.isBlank()) emptyList() else gameState.recentChoiceEventIds.split(",")
                val rawEvent = generateMonthlyUnifiedChoiceEvent(player, clubName, rivalName, recentIds)
                val updatedRecent = (listOf(rawEvent.id) + recentIds).take(3)
                gameState.recentChoiceEventIds = updatedRecent.joinToString(",")
                
                gameState.activeChoicePrompt = rawEvent.prompt
                gameState.activeChoiceOption1 = rawEvent.option1
                gameState.activeChoiceOption2 = rawEvent.option2
                gameState.activeChoiceOption3 = rawEvent.option3
                gameState.activeChoiceOutcome1 = rawEvent.outcome1
                gameState.activeChoiceOutcome2 = rawEvent.outcome2
                gameState.activeChoiceOutcome3 = rawEvent.outcome3
                
                gameState.activeChoiceFormMod1 = rawEvent.formMod1
                gameState.activeChoiceFinishingMod1 = rawEvent.finishingMod1
                gameState.activeChoiceTechniqueMod1 = rawEvent.techniqueMod1
                gameState.activeChoiceMoraleMod1 = rawEvent.moraleMod1
                gameState.activeChoiceFanRepMod1 = rawEvent.fanRepMod1
                gameState.activeChoiceManagerTrustMod1 = rawEvent.managerTrustMod1
                gameState.activeChoiceRivalRelMod1 = rawEvent.rivalRelMod1
                gameState.activeChoiceFatigueMod1 = rawEvent.fatigueMod1
                
                gameState.activeChoiceFormMod2 = rawEvent.formMod2
                gameState.activeChoiceFinishingMod2 = rawEvent.finishingMod2
                gameState.activeChoiceTechniqueMod2 = rawEvent.techniqueMod2
                gameState.activeChoiceMoraleMod2 = rawEvent.moraleMod2
                gameState.activeChoiceFanRepMod2 = rawEvent.fanRepMod2
                gameState.activeChoiceManagerTrustMod2 = rawEvent.managerTrustMod2
                gameState.activeChoiceRivalRelMod2 = rawEvent.rivalRelMod2
                gameState.activeChoiceFatigueMod2 = rawEvent.fatigueMod2
                
                gameState.activeChoiceFormMod3 = rawEvent.formMod3
                gameState.activeChoiceFinishingMod3 = rawEvent.finishingMod3
                gameState.activeChoiceTechniqueMod3 = rawEvent.techniqueMod3
                gameState.activeChoiceMoraleMod3 = rawEvent.moraleMod3
                gameState.activeChoiceFanRepMod3 = rawEvent.fanRepMod3
                gameState.activeChoiceManagerTrustMod3 = rawEvent.managerTrustMod3
                gameState.activeChoiceRivalRelMod3 = rawEvent.rivalRelMod3
                gameState.activeChoiceFatigueMod3 = rawEvent.fatigueMod3
                
                gameState.activeChoicePendingMonthLogs = monthLogs.joinToString("\n")
                
                dao.updateGameState(gameState)
                narrativeReport = "CHOICE_TRIGGERED"
            } else {
                val nextMonth = currentMonth + 1
                narrativeReport = monthLogs.joinToString("\n")
                gameState.narrativeLog = capNarrativeLog("Season ${gameState.currentSeason} (${formatSeasonYear(gameState.currentSeason)}), ${getMonthName(currentMonth)}:\n" + narrativeReport + "\n\n" + gameState.narrativeLog)

                // Decrement manager talk cooldown
                if (gameState.managerTalkCooldownMonths > 0) {
                    gameState.managerTalkCooldownMonths -= 1
                }

                // Clear persisted transfer offers since we are leaving the current month
                gameState.persistedTransferOffers = null

                if (nextMonth <= 9) {
                    gameState.currentMonthIndex = nextMonth
                    if (nextMonth == 5) {
                        player.hasTransferredThisWindow = false
                        dao.updatePlayer(player)
                    }
                    dao.updateGameState(gameState)
                } else {
                    val endReport = handleEndOfSeason(player, gameState)
                    gameState.narrativeLog = capNarrativeLog(endReport + "\n\n" + gameState.narrativeLog)
                    gameState.currentSeason += 1
                    gameState.currentMonthIndex = 0
                    player.hasTransferredThisWindow = false
                    dao.updatePlayer(player)
                    dao.updateGameState(gameState)
                }
            }
        }
        return narrativeReport
    }

    // Unique name generation
    suspend fun generateUniqueName(country: String, dao: CareerDao): String {
        val blocklist = setOf(
            "Harry Kane", "Jude Bellingham", "Declan Rice",
            "Lamine Yamal", "Alvaro Morata", "Daniel Olmo", "Ferran Torres",
            "Kylian Mbappe", "Antoine Griezmann", "Olivier Giroud", "Ousmane Dembele", "Bradley Barcola",
            "Joshua Kimmich", "Jamal Musiala", "Florian Wirtz", "Kai Havertz", "Thomas Müller", "Thomas Muller",
            "Gianluigi Donnarumma", "Ciro Immobile", "Nicolo Barella", "Federico Chiesa", "Mateo Retegui"
        )
        var attempts = 0
        while (attempts < 200) {
            val candidate = FictionalData.generateRandomName(country)
            if (candidate !in blocklist) {
                val count = dao.checkUsedNameCount(candidate)
                if (count == 0) {
                    dao.insertUsedName(UsedNameEntity(candidate))
                    return candidate
                }
            }
            attempts++
        }
        var fallbackSuffix = 1
        while (true) {
            val candidate = "${FictionalData.generateRandomName(country)} $fallbackSuffix"
            if (candidate !in blocklist) {
                val count = dao.checkUsedNameCount(candidate)
                if (count == 0) {
                    dao.insertUsedName(UsedNameEntity(candidate))
                    return candidate
                }
            }
            fallbackSuffix++
        }
    }

    // One-time migration for club records with old "-N seasons ago" text
    suspend fun migrateClubRecordsIfNeeded() {
        val allRecords = dao.getAllClubRecordsSync()
        for (record in allRecords) {
            if (record.yearsActive.contains("season", ignoreCase = true) || record.yearsActive.contains("ago", ignoreCase = true)) {
                val numMatch = Regex("\\d+").find(record.yearsActive)?.value?.toIntOrNull() ?: 5
                val endYr = CAREER_START_YEAR - numMatch.coerceIn(1, 30)
                val startYr = endYr - 12
                record.yearsActive = "$startYr – $endYr"
                dao.updateClubRecord(record)
            }
        }
    }

    // Seeding records
    suspend fun ensureClubRecordsExist(clubId: Int) {
        val existing = dao.getRecordsForClubSync(clubId)
        if (existing.isNotEmpty()) return

        val club = dao.getClubById(clubId) ?: return
        val repMultiplier = when (club.reputation) {
            "ELITE" -> 3.5f
            "BIG" -> 2.5f
            "MID" -> 1.5f
            "SMALL" -> 0.8f
            else -> 1.0f
        }
        val age = club.foundedSeasonsAgo

        val categories = listOf("TOP_SCORER", "MOST_ASSISTS", "MOST_APPEARANCES", "MOST_TROPHIES")
        val records = mutableListOf<ClubRecordEntity>()

        for (cat in categories) {
            val statValue = when (cat) {
                "TOP_SCORER" -> {
                    (age * (1.2f + kotlin.random.Random.nextFloat() * 0.8f) * repMultiplier).toInt().coerceIn(30, 450)
                }
                "MOST_ASSISTS" -> {
                    (age * (0.8f + kotlin.random.Random.nextFloat() * 0.6f) * repMultiplier).toInt().coerceIn(20, 300)
                }
                "MOST_APPEARANCES" -> {
                    (age * (2.5f + kotlin.random.Random.nextFloat() * 1.5f) * repMultiplier).toInt().coerceIn(80, 800)
                }
                "MOST_TROPHIES" -> {
                    when (club.reputation) {
                        "ELITE" -> kotlin.random.Random.nextInt(25, 46)
                        "BIG" -> kotlin.random.Random.nextInt(12, 25)
                        "MID" -> kotlin.random.Random.nextInt(4, 12)
                        "SMALL" -> kotlin.random.Random.nextInt(0, 4)
                        else -> 1
                    }
                }
                else -> 0
            }

            val holderName = generateUniqueName(club.country, dao)
            val careerLength = kotlin.random.Random.nextInt(10, 18)
            val endSeasonOffset = kotlin.random.Random.nextInt(2, 15)
            val endYr = CAREER_START_YEAR - endSeasonOffset
            val startYr = endYr - careerLength
            val yearsActiveStr = "$startYr – $endYr"

            records.add(
                ClubRecordEntity(
                    clubId = clubId,
                    category = cat,
                    holderName = holderName,
                    statValue = statValue,
                    isActive = false,
                    yearsActive = yearsActiveStr,
                    isUserPlayer = false,
                    generation = null
                )
            )
        }
        dao.insertClubRecords(records)
    }

    // Record evolution
    suspend fun evolveClubRecords(club: ClubEntity, currentSeason: Int, wonTrophyThisSeason: Boolean = false) {
        val chance = when (club.reputation) {
            "ELITE" -> 0.03f
            "BIG" -> 0.06f
            "MID" -> 0.10f
            "SMALL" -> 0.15f
            else -> 0.10f
        }

        val records = dao.getRecordsForClubSync(club.id)
        for (record in records) {
            if (record.category == "MOST_TROPHIES") {
                // Gate MOST_TROPHIES evolution so it only fires if the club won a trophy this season
                if (wonTrophyThisSeason && kotlin.random.Random.nextFloat() < 0.5f) {
                    record.previousHolderName = record.holderName
                    record.previousStatValue = record.statValue
                    record.previousYearsActive = record.yearsActive

                    val npcName = generateUniqueName(club.country, dao)
                    record.holderName = npcName
                    record.statValue = record.statValue + 1
                    record.isActive = true
                    record.yearsActive = "${formatSeasonYear(currentSeason)} – present"
                    record.isUserPlayer = false
                    record.generation = null

                    dao.updateClubRecord(record)
                }
            } else if (kotlin.random.Random.nextFloat() < chance) {
                val increment = when (record.category) {
                    "TOP_SCORER" -> kotlin.random.Random.nextInt(5, 15)
                    "MOST_ASSISTS" -> kotlin.random.Random.nextInt(5, 15)
                    "MOST_APPEARANCES" -> kotlin.random.Random.nextInt(15, 45)
                    else -> 1
                }

                record.previousHolderName = record.holderName
                record.previousStatValue = record.statValue
                record.previousYearsActive = record.yearsActive

                val npcName = generateUniqueName(club.country, dao)
                record.holderName = npcName
                record.statValue = record.statValue + increment
                record.isActive = true
                record.yearsActive = "${formatSeasonYear(currentSeason)} – present"
                record.isUserPlayer = false
                record.generation = null

                dao.updateClubRecord(record)
            }
        }
    }

    // Compare player stats against club records
    suspend fun checkAndApplyPlayerClubRecords(player: PlayerEntity, club: ClubEntity, currentSeason: Int) {
        val playerGoals = player.clubGoals
        val playerAssists = player.clubAssists
        val playerGames = player.clubGamesPlayed
        val playerTrophies = dao.getPlayerTrophiesCountForClub(player.name, player.generation, club.name)

        val records = dao.getRecordsForClubSync(club.id)
        for (record in records) {
            val playerValue = when (record.category) {
                "TOP_SCORER" -> playerGoals
                "MOST_ASSISTS" -> playerAssists
                "MOST_APPEARANCES" -> playerGames
                "MOST_TROPHIES" -> playerTrophies
                else -> 0
            }

            if (playerValue > record.statValue) {
                if (record.isUserPlayer && record.generation == player.generation) {
                    record.statValue = playerValue
                    dao.updateClubRecord(record)
                } else {
                    record.previousHolderName = record.holderName
                    record.previousStatValue = record.statValue
                    record.previousYearsActive = record.yearsActive
                    
                    record.holderName = player.name
                    record.statValue = playerValue
                    record.isActive = true
                    record.yearsActive = "${formatSeasonYear(currentSeason)} – present"
                    record.isUserPlayer = true
                    record.generation = player.generation
                    dao.updateClubRecord(record)
                }
            }
        }
    }

    // Finalize player active records when leaving
    suspend fun finalizePlayerRecordsForClub(player: PlayerEntity, clubId: Int, currentSeason: Int) {
        val records = dao.getRecordsForClubSync(clubId)
        for (record in records) {
            if (record.isUserPlayer && record.generation == player.generation && record.isActive) {
                record.isActive = false
                record.yearsActive = record.yearsActive.replace("present", formatSeasonYear(currentSeason))
                dao.updateClubRecord(record)
            }
        }
    }

    suspend fun setDevMode(enabled: Boolean) {
        val gs = dao.getGameStateSync() ?: return
        gs.isDevMode = enabled
        dao.updateGameState(gs)
    }

    suspend fun activateGodMode() {
        val player = dao.getPlayerSync() ?: return
        player.isGodMode = true
        dao.updatePlayer(player)
    }

    // Club history flow & trophies flow
    fun getHistoryForClubFlow(clubId: Int) = dao.getHistoryForClubFlow(clubId)
    fun getTrophiesByClubFlow(clubName: String) = dao.getTrophiesByClubFlow(clubName)
    fun getRecordsForClubFlow(clubId: Int) = dao.getRecordsForClubFlow(clubId)

    suspend fun submitSocialPostReply(post: SocialPostEntity, replyIndex: Int): String {
        if (post.hasReplied) return ""
        var statMessage = ""
        db.withTransaction {
            val player = dao.getPlayerSync() ?: return@withTransaction
            val moraleMod = when (replyIndex) { 1 -> post.reply1MoraleMod; 2 -> post.reply2MoraleMod; 3 -> post.reply3MoraleMod; else -> 0 }
            val fanRepMod = when (replyIndex) { 1 -> post.reply1FanRepMod; 2 -> post.reply2FanRepMod; 3 -> post.reply3FanRepMod; else -> 0 }
            val managerTrustMod = when (replyIndex) { 1 -> post.reply1ManagerTrustMod; 2 -> post.reply2ManagerTrustMod; 3 -> post.reply3ManagerTrustMod; else -> 0 }
            val rivalRelMod = when (replyIndex) { 1 -> post.reply1RivalRelMod; 2 -> post.reply2RivalRelMod; 3 -> post.reply3RivalRelMod; else -> 0 }

            player.morale = (player.morale + moraleMod).coerceIn(0, 100)
            player.fanReputation = (player.fanReputation + fanRepMod).coerceIn(0, 100)
            player.managerTrust = (player.managerTrust + managerTrustMod).coerceIn(0, 100)
            player.rivalRelationship = (player.rivalRelationship + rivalRelMod).coerceIn(0, 100)
            dao.updatePlayer(player)

            post.hasReplied = true
            post.selectedReplyIndex = replyIndex
            dao.updateSocialPost(post)

            val changes = mutableListOf<String>()
            if (fanRepMod != 0) changes.add("Fan Reputation ${if (fanRepMod > 0) "+$fanRepMod" else "$fanRepMod"}")
            if (moraleMod != 0) changes.add("Morale ${if (moraleMod > 0) "+$moraleMod" else "$moraleMod"}")
            if (managerTrustMod != 0) changes.add("Manager Trust ${if (managerTrustMod > 0) "+$managerTrustMod" else "$managerTrustMod"}")
            if (rivalRelMod != 0) changes.add("Rival Relationship ${if (rivalRelMod > 0) "+$rivalRelMod" else "$rivalRelMod"}")
            statMessage = if (changes.isNotEmpty()) changes.joinToString(", ") else "Reply submitted"
        }
        return statMessage
    }

    private fun fameMultiplier(player: PlayerEntity?): Float {
        if (player == null) return 1.0f
        val ovrFactor = ((player.ovr - 40).coerceAtLeast(0) / 60f)
        val fanRepFactor = player.fanReputation / 100f
        return (0.4f + ovrFactor * 1.8f + fanRepFactor * 1.0f).coerceIn(0.4f, 3.5f)
    }

    private fun scaledLikeCount(rng: Random, baseMin: Int, baseMax: Int, significance: Float, player: PlayerEntity?): Int {
        val mult = fameMultiplier(player) * significance
        val min = (baseMin * mult).toInt().coerceAtLeast(baseMin / 4)
        val max = (baseMax * mult).toInt().coerceAtLeast(min + 1)
        return rng.nextInt(min, max)
    }

    private fun applyMonthlyFanRepFormHook(player: PlayerEntity) {
        if (player.fanReputation >= 75) {
            player.form = (player.form + 1).coerceIn(-5, 5)
        } else if (player.fanReputation <= 25) {
            player.form = (player.form - 1).coerceIn(-5, 5)
        }
    }

    private suspend fun generateSocialPostsForMonth(player: PlayerEntity?, club: ClubEntity?, gameState: GameStateEntity) {
        val seed = if (player == null) {
            (gameState.currentSeason * 31 + gameState.currentMonthIndex).toLong()
        } else {
            (player.age.toLong() * 31 + player.ovr * 7 + gameState.currentSeason * 100 + gameState.currentMonthIndex).toLong()
        }
        val rng = Random(seed)

        val totalPosts = rng.nextInt(5, 8) // Exactly 5 to 7 posts per month
        val maxSeq = dao.getMaxSocialPostSequenceIndex() ?: 0
        var currentSeq = maxSeq

        val phase = player?.careerPhase
        val age = player?.age ?: 0
        val ovr = player?.ovr ?: 0
        val form = player?.form ?: 0
        val managerTrust = player?.managerTrust ?: 0
        val clubName = club?.name ?: when (phase) {
            PHASE_STREET -> "Street Cages"
            PHASE_YOUTH -> "Youth Academy"
            PHASE_SENIOR -> "Free Agent"
            else -> "Free Agent"
        }

        val existingFunFactCount = dao.getFunFactPostCount()
        var addedFunFact = false

        val punditAuthors = listOf(
            Triple("Gareth Thorne", "@GarethThorne", "GT"),
            Triple("Arthur Sterling", "@ArthurSterling", "AS"),
            Triple("Marcus Vance", "@MarcusVance", "MV"),
            Triple("Marco Bellini", "@MarcoBellini_", "MB"),
            Triple("James Vance", "@JamesVance23", "JV"),
            Triple("Raymond Kane", "@RayKaneOfficial", "RK"),
            Triple("Ronnie Fox", "@RonnieFox5", "RF"),
            Triple("Ian Wrightson", "@IanWrightson", "IW")
        )

        val influencerAuthors = listOf(
            Triple("Tactics Guru", "@TacticsGuru", "TG"),
            Triple("Futbol Hub", "@FutbolHub", "FH"),
            Triple("Ballon Watch", "@BallonWatch", "BW"),
            Triple("PitchSide", "@PitchSideMeme", "PS"),
            Triple("Wonderkid Scout", "@WonderkidScout", "WS")
        )

        val citizenAuthors = listOf(
            Triple("Dave_FC", "@Dave_FC99", "DF"),
            Triple("MatchdayMike", "@MatchdayMike", "MM"),
            Triple("FootballFanatic", "@FootyFanatic", "FF"),
            Triple("Sarah_LFC", "@Sarah_LFC", "SL"),
            Triple("Liam_Utch", "@LiamUtch", "LU"),
            Triple("Chris_B", "@Chris_B_77", "CB")
        )

        val fictionalClubs = listOf("Eastside Cage", "Dockyard FC", "Metro United", "Riverfront FC", "Apex Academy", "City Titans", "Harbor Rovers", "St. Jude Youth", "Northern Athletic", "Highland FC")
        val fictionalPlayers = listOf("Marcus Vance", "Leo Sterling", "Kai Tanaka", "Mateo Rossi", "Viktor Novak", "Antoine Dupont", "Julian Drax", "Santi Cazorla")

        val triviaTemplates = listOf(
            "FUN FACT 💡 Did you know? The fastest goal in professional football history was scored in just 2.1 seconds!",
            "FUN FACT 💡 The total distance covered by a professional outfield player during a 90-minute match averages between 10 to 13 kilometers.",
            "FUN FACT 💡 Only three players in football history have won the Champions League with three different clubs.",
            "FUN FACT 💡 The original World Cup trophy, the Jules Rimet Trophy, was made of gold-plated sterling silver and lapis lazuli."
        )

        // Calculate maximum allowed player posts for this month based on context tier
        val maxPlayerPosts = when {
            player == null -> 0
            phase == PHASE_STREET -> {
                // ≤ 1 player post every 2-3 months (~15% chance of 1, else 0)
                if (rng.nextFloat() < 0.15f) 1 else 0
            }
            phase == PHASE_YOUTH -> rng.nextInt(1, 3).coerceAtMost(2) // 1 to 2
            phase == PHASE_SENIOR && age < 24 && ovr < 75 -> rng.nextInt(1, 3).coerceAtMost(2)
            phase == PHASE_SENIOR && age in 24..29 && ovr >= 75 && form >= 0 -> rng.nextInt(2, 5).coerceAtMost(4)
            phase == PHASE_SENIOR && age in 27..35 && (form <= -3 || managerTrust < 30) -> rng.nextInt(2, 5).coerceAtMost(4)
            phase == PHASE_SENIOR && age >= 36 -> rng.nextInt(1, 3).coerceAtMost(2)
            else -> 2
        }

        val newPosts = mutableListOf<SocialPostEntity>()

        // Check for Milestone post
        var addedMilestone = false
        val milestoneThresholds = listOf(10, 25, 50, 100, 150, 200, 250, 300, 350, 400)
        val nextMilestone = if (player != null) milestoneThresholds.firstOrNull { it > player.lastGoalMilestonePosted && player.goals >= it } else null
        if (player != null && nextMilestone != null) {
            currentSeq++
            newPosts.add(
                SocialPostEntity(
                    sequenceIndex = currentSeq,
                    seasonNumber = gameState.currentSeason,
                    monthIndex = gameState.currentMonthIndex,
                    postType = "MILESTONE",
                    authorName = "Stats Foot",
                    authorHandle = "@StatsFoot",
                    authorInitials = "SF",
                    content = "MILESTONE 🌟 ${player.name} reaches $nextMilestone career goals! A landmark achievement in an extraordinary career.",
                    isAboutPlayerOrClub = true,
                    relatedClubId = club?.id,
                    likeCount = scaledLikeCount(rng, 15000, 85000, 1.0f + (nextMilestone / 100f), player)
                )
            )
            player.lastGoalMilestonePosted = nextMilestone
            dao.updatePlayer(player)
            addedMilestone = true
        }

        var playerPostsAdded = if (addedMilestone) 1 else 0
        val totalToGen = if (addedMilestone) totalPosts - 1 else totalPosts

        for (i in 0 until totalToGen) {
            val roll = rng.nextFloat()
            val allowPlayerPost = player != null && playerPostsAdded < maxPlayerPosts
            currentSeq++

            val post: SocialPostEntity = when {
                // Fun Fact check (max 1 visible, 10% chance if count == 0)
                existingFunFactCount == 0 && !addedFunFact && roll < 0.10f -> {
                    addedFunFact = true
                    SocialPostEntity(
                        sequenceIndex = currentSeq,
                        seasonNumber = gameState.currentSeason,
                        monthIndex = gameState.currentMonthIndex,
                        postType = "FUN_FACT",
                        authorName = "Football Trivia",
                        authorHandle = "@TriviaFC",
                        authorInitials = "TF",
                        content = triviaTemplates[rng.nextInt(triviaTemplates.size)],
                        isAboutPlayerOrClub = false,
                        relatedClubId = null,
                        likeCount = scaledLikeCount(rng, 200, 5000, 1.0f, null)
                    )
                }

                // Player specific post if allowed
                allowPlayerPost && (roll < 0.60f || i == 0 && maxPlayerPosts > 0) -> {
                    playerPostsAdded++
                    when (phase) {
                        PHASE_STREET -> {
                            val streetTemplates = listOf(
                                "@${player.name} from the cages putting in work 👊",
                                "Local cage rat ${player.name} bagging braces again — someone's getting scouted soon.",
                                "Street report: ${player.name} ($age) running riot in the back alleys.",
                                "Cage highlight: ${player.name}'s footwork in the 3v3 tournament was ridiculous 🔥"
                            )
                            val (name, handle, init) = citizenAuthors[rng.nextInt(citizenAuthors.size)]
                            SocialPostEntity(
                                sequenceIndex = currentSeq,
                                seasonNumber = gameState.currentSeason,
                                monthIndex = gameState.currentMonthIndex,
                                postType = "CITIZEN",
                                authorName = name,
                                authorHandle = handle,
                                authorInitials = init,
                                content = streetTemplates[rng.nextInt(streetTemplates.size)],
                                isAboutPlayerOrClub = true,
                                relatedClubId = null,
                                likeCount = scaledLikeCount(rng, 50, 800, 0.3f, player)
                            )
                        }

                        PHASE_YOUTH -> {
                            val (name, handle, init) = punditAuthors[rng.nextInt(punditAuthors.size)]
                            val youthContent = when {
                                form >= 3 -> listOf(
                                    "⚡ ${player.name} on fire! Youth PL Player of the Month nominee.",
                                    "${player.name} is the standout name in the U18s right now — scouts are noticing.",
                                    "Can't stop, won't stop: ${player.name} is tearing up the youth league this month."
                                ).random(rng)
                                form <= -3 -> listOf(
                                    "${player.name} benched — academy coaches demand answers.",
                                    "Rough patch for ${player.name}, who's lost his place in the U18s starting XI.",
                                    "Whispers from the academy: is ${player.name} losing his edge?"
                                ).random(rng)
                                else -> listOf(
                                    "${player.name} holding down a starting spot in U18s.",
                                    "Steady week for ${player.name} in the academy setup — no fireworks, no drama.",
                                    "${player.name} putting in the reps at U18 level, building toward a breakthrough.",
                                    "Academy report: ${player.name} remains a fixture in the U18s XI.",
                                    "Nothing flashy, just consistent minutes for ${player.name} in the youth ranks."
                                ).random(rng)
                            }
                            SocialPostEntity(
                                sequenceIndex = currentSeq,
                                seasonNumber = gameState.currentSeason,
                                monthIndex = gameState.currentMonthIndex,
                                postType = "PUNDIT",
                                authorName = name,
                                authorHandle = handle,
                                authorInitials = init,
                                content = youthContent,
                                isAboutPlayerOrClub = true,
                                relatedClubId = club?.id,
                                likeCount = scaledLikeCount(rng, 200, 3500, 0.5f, player)
                            )
                        }

                        PHASE_SENIOR -> {
                            when {
                                age < 24 && ovr < 75 -> {
                                    val youngTemplates = listOf(
                                        "Young ${player.name} making strides at $clubName.",
                                        "${player.name} ($age) earning late substitute minutes.",
                                        "Developing talent: ${player.name} showing encouraging glimpses of potential."
                                    )
                                    val (name, handle, init) = influencerAuthors[rng.nextInt(influencerAuthors.size)]
                                    SocialPostEntity(
                                        sequenceIndex = currentSeq,
                                        seasonNumber = gameState.currentSeason,
                                        monthIndex = gameState.currentMonthIndex,
                                        postType = "INFLUENCER",
                                        authorName = name,
                                        authorHandle = handle,
                                        authorInitials = init,
                                        content = youngTemplates[rng.nextInt(youngTemplates.size)],
                                        isAboutPlayerOrClub = true,
                                        relatedClubId = club?.id,
                                        likeCount = scaledLikeCount(rng, 1000, 12000, 1.0f, player)
                                    )
                                }

                                age in 27..35 && (form <= -3 || managerTrust < 30) -> {
                                    val isCritical = rng.nextFloat() < 0.35f
                                    val (name, handle, init) = citizenAuthors[rng.nextInt(citizenAuthors.size)]
                                    if (isCritical) {
                                        val critContent = listOf(
                                            "Fans turning on ${player.name} after that performance.",
                                            "${player.name}'s miss costs $clubName a point.",
                                            "Past his prime? ${player.name} ($age) benched again."
                                        )[rng.nextInt(3)]
                                        SocialPostEntity(
                                            sequenceIndex = currentSeq,
                                            seasonNumber = gameState.currentSeason,
                                            monthIndex = gameState.currentMonthIndex,
                                            postType = "CITIZEN",
                                            authorName = name,
                                            authorHandle = handle,
                                            authorInitials = init,
                                            content = critContent,
                                            isAboutPlayerOrClub = true,
                                            relatedClubId = club?.id,
                                            likeCount = scaledLikeCount(rng, 500, 8000, 1.0f, player),
                                            isReplyable = true,
                                            reply1Text = "Claps back: 'Keep watching, working hard every day.'",
                                            reply1FanRepMod = 4,
                                            reply1MoraleMod = 2,
                                            reply1ManagerTrustMod = -1,
                                            reply2Text = "Stays classy: 'We win and learn together as a team.'",
                                            reply2MoraleMod = 3,
                                            reply2FanRepMod = 1,
                                            reply2ManagerTrustMod = 3,
                                            reply3Text = "Ignores it",
                                            reply3MoraleMod = 0
                                        )
                                    } else {
                                        SocialPostEntity(
                                            sequenceIndex = currentSeq,
                                            seasonNumber = gameState.currentSeason,
                                            monthIndex = gameState.currentMonthIndex,
                                            postType = "PUNDIT",
                                            authorName = name,
                                            authorHandle = handle,
                                            authorInitials = init,
                                            content = "${player.name} under pressure to deliver for $clubName next match.",
                                            isAboutPlayerOrClub = true,
                                            relatedClubId = club?.id,
                                            likeCount = scaledLikeCount(rng, 1500, 15000, 1.1f, player)
                                        )
                                    }
                                }

                                age >= 36 -> {
                                    val veteranTemplates = listOf(
                                        "The end of an era: ${player.name} ($age) contemplating the final chapter of a legendary career.",
                                        "Pundit retrospective: ${player.name}'s impact over the years has been immense.",
                                        "Respect: ${player.name} receiving warm applause from opposing fans across the league."
                                    )
                                    val (name, handle, init) = punditAuthors[rng.nextInt(punditAuthors.size)]
                                    SocialPostEntity(
                                        sequenceIndex = currentSeq,
                                        seasonNumber = gameState.currentSeason,
                                        monthIndex = gameState.currentMonthIndex,
                                        postType = "PUNDIT",
                                        authorName = name,
                                        authorHandle = handle,
                                        authorInitials = init,
                                        content = veteranTemplates[rng.nextInt(veteranTemplates.size)],
                                        isAboutPlayerOrClub = true,
                                        relatedClubId = club?.id,
                                        likeCount = scaledLikeCount(rng, 5000, 45000, 1.4f, player)
                                    )
                                }

                                else -> {
                                    // Prime Senior player (age 24-29, OVR >= 75)
                                    val (name, handle, init) = punditAuthors[rng.nextInt(punditAuthors.size)]
                                    val isUnpopularOpinion = rng.nextFloat() < 0.12f
                                    val primeContent = if (isUnpopularOpinion) {
                                        "Unpopular opinion: ${player.name} is currently a top 3 striker in the league."
                                    } else {
                                        listOf(
                                            "${player.name} in the conversation for national team call-up!",
                                            "${player.name} climbing the top scorer charts at $clubName.",
                                            "Player of the Month candidate: ${player.name} on a remarkable streak."
                                        )[rng.nextInt(3)]
                                    }
                                    SocialPostEntity(
                                        sequenceIndex = currentSeq,
                                        seasonNumber = gameState.currentSeason,
                                        monthIndex = gameState.currentMonthIndex,
                                        postType = "PUNDIT",
                                        authorName = name,
                                        authorHandle = handle,
                                        authorInitials = init,
                                        content = primeContent,
                                        isAboutPlayerOrClub = true,
                                        relatedClubId = club?.id,
                                        likeCount = scaledLikeCount(rng, 4000, 50000, 1.5f, player)
                                    )
                                }
                            }
                        }

                        else -> {
                            val (name, handle, init) = citizenAuthors[rng.nextInt(citizenAuthors.size)]
                            SocialPostEntity(
                                sequenceIndex = currentSeq,
                                seasonNumber = gameState.currentSeason,
                                monthIndex = gameState.currentMonthIndex,
                                postType = "CITIZEN",
                                authorName = name,
                                authorHandle = handle,
                                authorInitials = init,
                                content = "Matchday buzz: fans excited to see how team lines up this week!",
                                isAboutPlayerOrClub = false,
                                relatedClubId = club?.id,
                                likeCount = scaledLikeCount(rng, 100, 1000, 1.0f, null)
                            )
                        }
                    }
                }

                // General Club & League Posts
                else -> {
                    val genType = rng.nextInt(6)
                    val clubA = fictionalClubs[rng.nextInt(fictionalClubs.size)]
                    var clubB = fictionalClubs[rng.nextInt(fictionalClubs.size)]
                    while (clubB == clubA) clubB = fictionalClubs[rng.nextInt(fictionalClubs.size)]
                    val playerX = fictionalPlayers[rng.nextInt(fictionalPlayers.size)]

                    val (name, handle, init) = influencerAuthors[rng.nextInt(influencerAuthors.size)]

                    val isNatCallup = genType == 5 && player != null && ovr >= 80 && age <= 30
                    val content = when (genType) {
                        0 -> "Breaking: $clubA sign $playerX from $clubB."
                        1 -> "$clubB 3-1 $clubA — late winner stuns the stadium."
                        2 -> "Golden Boot race: $playerX now on ${rng.nextInt(12, 28)} goals."
                        3 -> "$clubA U18s lift the youth trophy."
                        4 -> "Market buzz: $clubB eyeing a January move."
                        else -> if (isNatCallup) {
                            "National team squad announced — ${player.name} earns first senior cap! 🌐"
                        } else {
                            "National team squad announced — $playerX earns senior cap!"
                        }
                    }

                    val postType = if (genType == 0 || genType == 1) "CLUB_NEWS" else "INFLUENCER"
                    SocialPostEntity(
                        sequenceIndex = currentSeq,
                        seasonNumber = gameState.currentSeason,
                        monthIndex = gameState.currentMonthIndex,
                        postType = postType,
                        authorName = name,
                        authorHandle = handle,
                        authorInitials = init,
                        content = content,
                        isAboutPlayerOrClub = isNatCallup,
                        relatedClubId = if (isNatCallup) club?.id else null,
                        likeCount = scaledLikeCount(rng, 500, 25000, 1.0f, if (isNatCallup) player else null)
                    )
                }
            }
            newPosts.add(post)
        }

        dao.insertSocialPosts(newPosts)
        dao.pruneOldSocialPosts()
    }
}

data class RotationResult(
    val playerCameOnMinute: Int?,
    val isBenched: Boolean
)

data class TransferOffer(
    val clubId: Int,
    val clubName: String,
    val clubReputation: String,
    val rivalStrikerOvr: Int,
    val rivalStrikerName: String,
    val contractYears: Int,
    val targetGplusA: Int
)

data class ChoiceEvent(
    val prompt: String,
    val option1: String,
    val option2: String,
    val formModOption1: Int,
    val finishingModOption1: Int,
    val techniqueModOption1: Int,
    val outcome1: String,
    val formModOption2: Int,
    val finishingModOption2: Int,
    val techniqueModOption2: Int,
    val outcome2: String
)

data class UnifiedChoiceEvent(
    val id: String = "",
    val prompt: String,
    val option1: String,
    val outcome1: String,
    val formMod1: Int = 0,
    val finishingMod1: Int = 0,
    val techniqueMod1: Int = 0,
    val moraleMod1: Int = 0,
    val fanRepMod1: Int = 0,
    val managerTrustMod1: Int = 0,
    val rivalRelMod1: Int = 0,
    val fatigueMod1: Int = 0,

    val option2: String,
    val outcome2: String,
    val formMod2: Int = 0,
    val finishingMod2: Int = 0,
    val techniqueMod2: Int = 0,
    val moraleMod2: Int = 0,
    val fanRepMod2: Int = 0,
    val managerTrustMod2: Int = 0,
    val rivalRelMod2: Int = 0,
    val fatigueMod2: Int = 0,

    val option3: String? = null,
    val outcome3: String? = null,
    val formMod3: Int = 0,
    val finishingMod3: Int = 0,
    val techniqueMod3: Int = 0,
    val moraleMod3: Int = 0,
    val fanRepMod3: Int = 0,
    val managerTrustMod3: Int = 0,
    val rivalRelMod3: Int = 0,
    val fatigueMod3: Int = 0
)
