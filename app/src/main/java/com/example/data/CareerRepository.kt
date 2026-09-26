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

    suspend fun clearSeasonSummary() {
        _latestSeasonSummaryFlow.value = null
        db.withTransaction {
            val gameState = dao.getGameStateSync() ?: return@withTransaction
            if (gameState.pendingSeasonSummaryJson != null) {
                gameState.pendingSeasonSummaryJson = null
                dao.updateGameState(gameState)
            }
        }
    }

    suspend fun restorePendingSeasonSummaryIfNeeded() {
        val gameState = dao.getGameStateSync() ?: return
        val json = gameState.pendingSeasonSummaryJson
        if (!json.isNullOrBlank() && _latestSeasonSummaryFlow.value == null) {
            _latestSeasonSummaryFlow.value = SeasonSummaryData.deserialize(json)
        }
    }

    suspend fun getPlayerSync(): PlayerEntity? = dao.getPlayerSync()
    suspend fun getGameStateSync(): GameStateEntity? = dao.getGameStateSync()

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
        backgroundStory: String = "Street Cages",
        faceDescriptor: String = "",
        fatherFaceDescriptor: String? = null,
        firstName: String = "",
        lastName: String = ""
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
                        val openerVariant = listOf(
                            "Widely known as $nickname, they",
                            "Playing under the $nickname banner, the club",
                            "$nickname, as they're known locally,",
                            "Carrying the $nickname nickname, this side"
                        ).random()
                        val tacticalIdentity = listOf(
                            "Known for their high-pressing offensive philosophy and deep-rooted community pride.",
                            "Famous for their rock-solid defensive organization and clinical counter-attacking football.",
                            "Renowned for their fluid possession-based style and a rich history of nurturing world-class talents.",
                            "Distinguished by their aggressive aerial play, unmatched work rate, and passionate fanbase.",
                            "Highly regarded for tactical flexibility, high stamina levels, and a relentless winning mentality.",
                            "Known for lightning-fast wing play, incisive transition breaks, and unrelenting forward energy.",
                            "Famous for a pragmatic low block combined with lethal efficiency on attacking set pieces.",
                            "Renowned for patient midfield buildup, pinpoint short passing, and suffocating territory control."
                        ).random()
                        val cultureNote = listOf(
                            "The academy has a reputation for producing hard-working locals rather than marquee signings.",
                            "Boardroom stability has been rare, with managerial turnover a running theme.",
                            "Supporters are known throughout the league for a raucous atmosphere on European nights.",
                            "The club's youth setup punches well above its weight for a side of this size.",
                            "Deep community roots anchor the club, with generations of local families filling the stands.",
                            "A fiercely proud fan culture creates an intimidating environment for any traveling opponent."
                        ).random()
                        val description = "$openerVariant play their home matches at the prestigious $stadium. $tacticalIdentity $cultureNote"

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
            val skillCeilings = computeSkillCeilings(potentialCeiling)

            val talentSeed = 0.90f + Random.nextFloat() * 0.25f // 0.9 to 1.15
            val minStat = 20 + statBoost
            val maxStat = 38 + statBoost

            val finishing = (Random.nextInt(minStat, maxStat + 1) * talentSeed).roundToInt().coerceIn(1, 99)
            val pace = (Random.nextInt(minStat, maxStat + 1) * talentSeed).roundToInt().coerceIn(1, 99)
            val passing = (Random.nextInt(minStat, maxStat + 1) * talentSeed).roundToInt().coerceIn(1, 99)
            val physical = (Random.nextInt(minStat, maxStat + 1) * talentSeed).roundToInt().coerceIn(1, 99)
            val technique = (Random.nextInt(minStat, maxStat + 1) * talentSeed).roundToInt().coerceIn(1, 99)

            val ovr = calculateOvr(finishing, pace, passing, physical, technique).coerceAtMost(potentialCeiling)

            val resolvedFaceDescriptor = if (isSon && !fatherFaceDescriptor.isNullOrBlank()) {
                val fatherDesc = FaceDescriptor.deserialize(fatherFaceDescriptor)
                FaceDescriptor.inheritedFrom(fatherDesc, academyCountry).serialize()
            } else {
                faceDescriptor.ifBlank { FaceDescriptor.random(academyCountry).serialize() }
            }

            val resolvedFirstName = if (firstName.isNotBlank()) firstName else playerName.split(" ").firstOrNull() ?: playerName
            val resolvedLastName = if (lastName.isNotBlank()) lastName else playerName.split(" ").drop(1).joinToString(" ")
            val resolvedFullName = if (playerName.isNotBlank()) playerName else "$resolvedFirstName $resolvedLastName".trim()

            val player = PlayerEntity(
                name = resolvedFullName,
                firstName = resolvedFirstName,
                lastName = resolvedLastName,
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
                finishingCeiling = skillCeilings.getValue("finishing"),
                paceCeiling = skillCeilings.getValue("pace"),
                passingCeiling = skillCeilings.getValue("passing"),
                physicalCeiling = skillCeilings.getValue("physical"),
                techniqueCeiling = skillCeilings.getValue("technique"),
                careerPhase = PHASE_STREET,
                preferredFoot = preferredFoot,
                squadNumber = squadNumber,
                backgroundStory = backgroundStory,
                faceDescriptor = resolvedFaceDescriptor
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
        val progress = current.toFloat() / ceiling.toFloat()
        return if (progress < 0.85f) {
            1.0f
        } else if (progress <= 1.0f) {
            (1.0f - (progress - 0.85f) / 0.15f * 0.6f).coerceIn(0.25f, 1.0f)
        } else {
            // Soft cap past ceiling: slow down by 80% to allow specialist stats
            (0.20f / (1f + (progress - 1.0f) * 4f)).coerceIn(0.05f, 0.20f)
        }
    }

    /**
     * Computes the maximum OVR (and per-stat cap) this generation's player can ever reach.
     * - Gen 1 starts with randomized ceiling across a broad range (72..89, centered ~80) for variance.
     * - Gen 2+ incorporates the father's legacy boost on top of a randomized base.
     */
    fun computePotentialCeiling(generation: Int, statBoost: Int, achievementRatio: Float): Int {
        val randomizedBase = if (generation <= 1) {
            kotlin.random.Random.nextInt(72, 90)
        } else {
            kotlin.random.Random.nextInt(76, 91) + ((generation - 1) * 3).coerceAtMost(10)
        }
        val baseCeiling = randomizedBase.coerceAtMost(99)
        if (generation <= 1) return baseCeiling.coerceIn(68, 96)

        val legacyNudge = (statBoost - 15) / 3f
        val shortfall = (1f - achievementRatio).coerceIn(0f, 1f)
        val achievementPenalty = shortfall * 8f

        return (baseCeiling + legacyNudge - achievementPenalty).roundToInt().coerceIn(60, 99)
    }

    fun computeSkillCeilings(potentialCeiling: Int): Map<String, Int> {
        // Each skill's ceiling varies ±(0-6) around the composite potentialCeiling,
        // independently rolled per skill, so skills don't all cap identically.
        fun rolled() = (potentialCeiling + Random.nextInt(-6, 7)).coerceIn(30, 99)
        return mapOf(
            "finishing" to rolled(),
            "pace" to rolled(),
            "passing" to rolled(),
            "physical" to rolled(),
            "technique" to rolled()
        )
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

                applyYouthMatchPerformanceProgression(player, pGoals, pAssists, pMvp)
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

    private fun applyYouthMatchPerformanceProgression(player: PlayerEntity, goals: Int, assists: Int, isMvp: Boolean) {
        if (goals <= 0 && assists <= 0 && !isMvp) return

        // Chance to gain finishing or technique per goal (up to attribute cap 85 for youth stage)
        for (g in 0 until goals) {
            if (Random.nextFloat() < 0.35f) {
                if (Random.nextBoolean()) {
                    if (player.finishing < 85) player.finishing += 1
                } else {
                    if (player.technique < 85) player.technique += 1
                }
            }
        }

        // Chance to gain passing or technique per assist
        for (a in 0 until assists) {
            if (Random.nextFloat() < 0.35f) {
                if (Random.nextBoolean()) {
                    if (player.passing < 85) player.passing += 1
                } else {
                    if (player.technique < 85) player.technique += 1
                }
            }
        }

        // MVP performance gains
        if (isMvp) {
            player.form = (player.form + 1).coerceAtMost(5)
            player.morale = (player.morale + 3).coerceAtMost(100)
            if (Random.nextFloat() < 0.30f) {
                if (Random.nextBoolean()) {
                    if (player.pace < 85) player.pace += 1
                } else {
                    if (player.physical < 85) player.physical += 1
                }
            }
        }

        // Recalculate OVR dynamically, clamped to this player's generation ceiling
        player.ovr = calculateOvr(
            finishing = player.finishing,
            pace = player.pace,
            passing = player.passing,
            physical = player.physical,
            technique = player.technique
        ).coerceAtMost(player.potentialCeiling)
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

                    applyYouthMatchPerformanceProgression(player, pGoals, pAssists, pMvp)

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
                        val offerRng = kotlin.random.Random(
                            ac.id.toLong() * 1_000_003L +
                            player.ovr.toLong() * 97L +
                            player.finishing.toLong() * 31L +
                            player.pace.toLong() +
                            index.toLong() * 13L
                        )
                        val report = YouthCareerLogic.generateScoutReportText(
                            player = player,
                            targetName = ac.academyName,
                            rivalName = ac.youthRivalName,
                            rivalOvr = ac.youthRivalOvr,
                            offerIndex = index,
                            parentReputation = ac.parentReputation,
                            rng = offerRng
                        )
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
                if (nextMonth <= 11) {
                    gameState.currentMonthIndex = nextMonth
                } else {
                    checkAndAwardYouthTrophy(player, gameState)
                    resetYouthLeagueForNewSeason()
                    player.age += 1
                    val endReport = handleEndOfSeason(player, gameState)
                    gameState.narrativeLog = capNarrativeLog(endReport + "\n\n" + gameState.narrativeLog)
                    gameState.currentSeason += 1
                    gameState.currentMonthIndex = 0
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
                if (nextMonth <= 11) {
                    gameState.currentMonthIndex = nextMonth
                } else {
                    checkAndAwardYouthTrophy(player, gameState)
                    resetYouthLeagueForNewSeason()
                    player.age += 1
                    val endReport = handleEndOfSeason(player, gameState)
                    gameState.narrativeLog = capNarrativeLog(endReport + "\n\n" + gameState.narrativeLog)
                    gameState.currentSeason += 1
                    gameState.currentMonthIndex = 0

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

                if (nextMonth <= 11) {
                    gameState.currentMonthIndex = nextMonth
                    if (nextMonth == 0 || nextMonth == 5 || nextMonth == 10 || nextMonth == 11) {
                        player.hasTransferredThisWindow = false
                        dao.updatePlayer(player)
                    }

                    // In Month 10 (June): International Duty / World Cup / Call-up evaluation
                    if (nextMonth == 10 && player.careerPhase == PHASE_SENIOR) {
                        if (player.nationalTeamCode != null) {
                            val myNation = nationByCode(player.nationalTeamCode!!)
                            val isWC = isWorldCupSeason(gameState.currentSeason)
                            val thresholds = myNation?.let { thresholdsForTier(it.tier) }
                            val avgRating = recentRatingAverage(player.recentMatchRatings)
                            val requiredRating = if (isWC) thresholds?.worldCupRating else thresholds?.baseRating
                            val selected = thresholds != null && avgRating != null && requiredRating != null &&
                                    player.ovr >= thresholds.baseOvr && avgRating >= requiredRating
                            if (selected) {
                                val capsEarned = if (isWC) Random.nextInt(3, 6) else Random.nextInt(2, 4)
                                player.nationalTeamCaps += capsEarned
                                player.lastNationalCallUpSeason = gameState.currentSeason
                                dao.updatePlayer(player)
                                val dutyName = if (isWC) "FIFA World Cup" else "International fixtures"
                                monthLogs.add("🌍 International Duty: Represented ${myNation.name} in $dutyName (+$capsEarned caps). Total caps: ${player.nationalTeamCaps}.")
                            } else {
                                monthLogs.add("🌍 International Duty: Left out of ${myNation?.name ?: player.nationalTeamCode}'s squad this cycle — form/OVR below the required standard.")
                            }
                        } else {
                            val callUpOffer = evaluateNationalCallUp(player, gameState)
                            if (callUpOffer != null) {
                                gameState.pendingCallUpNationCode = callUpOffer.code
                                gameState.pendingCallUpIsWorldCup = isWorldCupSeason(gameState.currentSeason)
                                monthLogs.add("🌍 NATIONAL CALL-UP: You have received a senior call-up to represent ${callUpOffer.name}!")
                            }
                        }
                    }
                    dao.updateGameState(gameState)
                } else {
                    // END OF SEASON TRIGGERS!
                    // Month is July (11), advance season to August (0)
                    checkAndAwardYouthTrophy(player, gameState)
                    resetYouthLeagueForNewSeason()
                    player.age += 1
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
        val existing = dao.findTrophy(trophy.playerName, trophy.generation, trophy.seasonYear, trophy.competitionName)
        if (existing == null) {
            dao.insertTrophy(trophy)
            onTrophyUnlockedListener?.invoke(trophy)
        }
    }

    /**
     * End of Season Calculations.
     */
    private suspend fun handleEndOfSeason(player: PlayerEntity, gameState: GameStateEntity): String {
        val usedNamesCache = dao.getAllUsedNamesSync().toMutableSet()
        // 1. Recalculate standings directly from this season's league fixtures to guarantee absolute score/points consistency
        val seasonFixtures = dao.getSeasonLeagueFixturesSync()
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

        // Compute recent league title counts (last 5 years + this season) for downfall risk
        val minSeason = (gameState.currentSeason - 5).coerceAtLeast(1)
        val recentWinnersHistory = dao.getRecentLeagueWinnersSync(minSeason)
        val recentTitleCounts = mutableMapOf<Int, Int>()
        for (h in recentWinnersHistory) {
            recentTitleCounts[h.clubId] = (recentTitleCounts[h.clubId] ?: 0) + 1
        }

        // Find League Winner for each country
        for (country in FictionalData.COUNTRIES) {
            val sortedStandings = dao.getStandingsByCountrySync(country)
            if (sortedStandings.isNotEmpty()) {
                val winnerClub = allClubsMap[sortedStandings[0].clubId]
                if (winnerClub != null) {
                    seasonLogs.add("🥇 ${country} Champion: ${winnerClub.name} (${sortedStandings[0].points} pts)")
                    trophyWinningClubIds.add(winnerClub.id)
                    recentTitleCounts[winnerClub.id] = (recentTitleCounts[winnerClub.id] ?: 0) + 1
                    
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
                adjustClubsReputations(sortedStandings, allClubsMap, recentTitleCounts)
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

        // Contract Evaluation
        evaluateContractEndSeason(player, myClub, seasonLogs, gameState.currentSeason)

        // --- Club Records & Season History Snapshotting ---
        if (player.careerPhase == PHASE_SENIOR) {
            val currentClub = allClubsMap[player.currentClubId]
            if (currentClub != null) {
                checkAndApplyPlayerClubRecords(player, currentClub, gameState.currentSeason)
                val clubNationCode = nationCodeForCountryName(currentClub.country)
                if (clubNationCode != null) {
                    val residencyMap = parseResidencyMap(player.residencyDaysByCountry).toMutableMap()
                    residencyMap[clubNationCode] = (residencyMap[clubNationCode] ?: 0) + 365
                    player.residencyDaysByCountry = serializeResidencyMap(residencyMap)
                }
            }
        } else if (player.careerPhase == PHASE_YOUTH && player.currentAcademyId != null) {
            val academy = dao.getYouthAcademyById(player.currentAcademyId!!)
            if (academy != null) {
                val academyNationCode = nationCodeForCountryName(academy.country)
                if (academyNationCode != null) {
                    val residencyMap = parseResidencyMap(player.residencyDaysByCountry).toMutableMap()
                    residencyMap[academyNationCode] = (residencyMap[academyNationCode] ?: 0) + 365
                    player.residencyDaysByCountry = serializeResidencyMap(residencyMap)
                }
            }
        }

        // Simulate global national team ranking fluctuations once per season
        updateNationRankings(player, gameState.currentSeason)

        if (isWorldCupSeason(gameState.currentSeason)) {
            val existing = gameState.worldCupWinnersHistory.split(";").filter { it.isNotBlank() }
            val alreadyRecorded = existing.any { it.startsWith("${gameState.currentSeason}:") }
            if (!alreadyRecorded) {
                val rankings = dao.getAllRankingStateSync()
                val winnerCode = rankings.maxByOrNull { it.currentRankingPoints }?.nationCode
                    ?: ALL_NATIONS.maxByOrNull { it.baseRankingPoints }?.code
                    ?: "BRA"
                val newEntry = "${gameState.currentSeason}:$winnerCode"
                gameState.worldCupWinnersHistory = if (gameState.worldCupWinnersHistory.isBlank()) newEntry else "${gameState.worldCupWinnersHistory};$newEntry"
                dao.updateGameState(gameState)
            }
        }

        val allClubRecords = dao.getAllClubRecordsSync().groupBy { it.clubId }
        val modifiedClubRecords = mutableListOf<ClubRecordEntity>()
        for (club in allClubs) {
            val clubRecs = allClubRecords[club.id] ?: emptyList()
            modifiedClubRecords.addAll(evolveClubRecords(club, gameState.currentSeason, trophyWinningClubIds.contains(club.id), clubRecs, usedNamesCache))
        }
        if (modifiedClubRecords.isNotEmpty()) {
            dao.updateClubRecords(modifiedClubRecords)
        }

        // Determine final tournament winners and runners-up for this season
        var clWinnerId: Int? = null
        var clRunnerUpId: Int? = null
        val clFinalFx = allFixturesSnapshot.find { it.competition == "CHAMPIONS_LEAGUE" && it.round == 2 && it.isSimulated }
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
        val elFinalFx = allFixturesSnapshot.find { it.competition == "EUROPA_LEAGUE" && it.round == 2 && it.isSimulated }
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
        val confFinalFx = allFixturesSnapshot.find { it.competition == "CONFERENCE_LEAGUE" && it.round == 2 && it.isSimulated }
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
        val superCupFx = allFixturesSnapshot.find { it.competition == "SUPER_CUP" && it.isSimulated }
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
                        val fxList = allFixturesSnapshot
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

        val playerSeasonFixturesSnapshot = allFixturesSnapshot.filter {
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
        runTransferWindow(allClubs, player, gameState, seasonLogs, histories, usedNamesCache)

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
            gameState.pendingSeasonSummaryJson = summaryData.serialize()
            dao.updateGameState(gameState)
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
private suspend fun adjustClubsReputations(
    standings: List<StandingEntity>,
    clubsMap: Map<Int, ClubEntity>,
    recentTitleCounts: Map<Int, Int> = emptyMap()
) {
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
            val titleCount = recentTitleCounts[club.id] ?: 0
            val isDominant = titleCount >= 3
            val roll = Random.nextFloat()
            momentum = if (isDominant) {
                // Shift odds for dominant clubs (>=3 titles in 5 yrs): 45% DECLINING, 20% RISING, 35% STABLE
                when {
                    roll < 0.20f -> Random.nextFloat() *
                        (ERA_MOMENTUM_RISING_DECLINING_MAX - ERA_MOMENTUM_RISING_DECLINING_MIN) +
                        ERA_MOMENTUM_RISING_DECLINING_MIN // RISING arc (20%)
                    roll < 0.65f -> -(Random.nextFloat() *
                        (ERA_MOMENTUM_RISING_DECLINING_MAX - ERA_MOMENTUM_RISING_DECLINING_MIN) +
                        ERA_MOMENTUM_RISING_DECLINING_MIN) // DECLINING arc (45%)
                    else -> (Random.nextFloat() * 2f - 1f) * ERA_MOMENTUM_STABLE_RANGE // STABLE arc (35%)
                }
            } else {
                when {
                    roll < 0.25f -> Random.nextFloat() *
                        (ERA_MOMENTUM_RISING_DECLINING_MAX - ERA_MOMENTUM_RISING_DECLINING_MIN) +
                        ERA_MOMENTUM_RISING_DECLINING_MIN // RISING arc (25%)
                    roll < 0.50f -> -(Random.nextFloat() *
                        (ERA_MOMENTUM_RISING_DECLINING_MAX - ERA_MOMENTUM_RISING_DECLINING_MIN) +
                        ERA_MOMENTUM_RISING_DECLINING_MIN) // DECLINING arc (25%)
                    else -> (Random.nextFloat() * 2f - 1f) * ERA_MOMENTUM_STABLE_RANGE // STABLE arc (50%)
                }
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

            player.finishing = applyStatGrowth(player.finishing, baseFinishingGrowth + goalBonus + mvpBonus + player.finishingTrainingBonus, player.finishingCeiling)
            player.pace = applyStatGrowth(player.pace, basePaceGrowth + mvpBonus + player.paceTrainingBonus, player.paceCeiling)
            player.passing = applyStatGrowth(player.passing, basePassingGrowth + assistBonus + mvpBonus + player.passingTrainingBonus, player.passingCeiling)
            player.physical = applyStatGrowth(player.physical, basePhysicalGrowth + mvpBonus + player.physicalTrainingBonus, player.physicalCeiling)
            player.technique = applyStatGrowth(player.technique, baseTechniqueGrowth + mvpBonus + player.techniqueTrainingBonus, player.techniqueCeiling)
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

            player.finishing = (player.finishing + netDecline.roundToInt() + player.finishingTrainingBonus.roundToInt()).coerceIn(1, player.finishingCeiling)
            player.pace = (player.pace + netDecline.roundToInt() + player.paceTrainingBonus.roundToInt()).coerceIn(1, player.paceCeiling)
            player.passing = (player.passing + netDecline.roundToInt() + player.passingTrainingBonus.roundToInt()).coerceIn(1, player.passingCeiling)
            player.physical = (player.physical + netDecline.roundToInt() + player.physicalTrainingBonus.roundToInt()).coerceIn(1, player.physicalCeiling)
            player.technique = (player.technique + netDecline.roundToInt() + player.techniqueTrainingBonus.roundToInt()).coerceIn(1, player.techniqueCeiling)
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
        // Individual stats can grow up to 99 even if player's composite ceiling is lower;
        // composite OVR is clamped to potentialCeiling separately.
        return nextStat.coerceIn(1, 99)
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
        // 0=Aug, 5=Jan (existing windows, unchanged) — 10=Jun, 11=Jul (new windows)
        return monthIndex == 0 || monthIndex == 5 || monthIndex == 10 || monthIndex == 11
    }

    private fun pushRecentRating(current: String, newRating: Float): String {
        val list = if (current.isBlank()) emptyList() else current.split(";").mapNotNull { it.toFloatOrNull() }
        val updated = (list + newRating).takeLast(5)
        return updated.joinToString(";") { "%.2f".format(it) }
    }

    fun recentRatingAverage(recentMatchRatings: String): Float? {
        val list = if (recentMatchRatings.isBlank()) emptyList() else recentMatchRatings.split(";").mapNotNull { it.toFloatOrNull() }
        return if (list.isEmpty()) null else list.average().toFloat()
    }

    fun getEligibleNationCodes(player: PlayerEntity): List<String> {
        val result = mutableSetOf<String>()
        nationCodeForCountryName(player.birthCountry)?.let { result.add(it) }
        val residencyMap = parseResidencyMap(player.residencyDaysByCountry)
        val fiveYearsInDays = 365 * 5
        residencyMap.forEach { (code, days) ->
            if (days >= fiveYearsInDays && player.age < 21) result.add(code)
            else if (days >= fiveYearsInDays && result.contains(code)) {
                // already eligible via birth country; residency simply reconfirms it
            }
        }
        return result.toList()
    }

    fun getEligibleNationCodesIncludingInheritance(player: PlayerEntity, fatherNationalTeamCode: String?): List<String> {
        val own = getEligibleNationCodes(player)
        return if (fatherNationalTeamCode != null) (own + fatherNationalTeamCode).distinct() else own
    }

    suspend fun updateNationRankings(player: PlayerEntity, seasonNumber: Int) {
        val existingState = dao.getAllRankingStateSync().associateBy { it.nationCode }

        fun currentPointsFor(code: String): Int =
            existingState[code]?.currentRankingPoints ?: (nationByCode(code)?.baseRankingPoints ?: 0)

        val sortedByCurrent = ALL_NATIONS.sortedByDescending { currentPointsFor(it.code) }
        val topSimSet = sortedByCurrent.take(18).map { it.code }.toMutableSet()
        val committedNationCode = player.nationalTeamCode
        if (committedNationCode != null) topSimSet.add(committedNationCode)

        val updates = mutableListOf<NationRankingState>()

        for (nation in ALL_NATIONS) {
            val current = currentPointsFor(nation.code)
            if (nation.code in topSimSet) {
                val baseComponent = nation.baseRankingPoints * 0.7f
                val playerComponent = if (nation.code == committedNationCode) {
                    val avgRating = recentRatingAverage(player.recentMatchRatings)
                    val ovrContribution = (player.ovr - 60).coerceIn(0, 40) * 1.5f
                    val ratingContribution = if (avgRating != null) (avgRating - 5.0f).coerceIn(0f, 5f) * 12f else 0f
                    (ovrContribution + ratingContribution).coerceAtMost(120f) // hard cap: one player, max +120 pts total, not per season unbounded
                } else 0f
                val drift = (-15..15).random()
                val newPoints = (baseComponent + current * 0.3f + playerComponent + drift).toInt().coerceIn(200, 2200)
                updates.add(NationRankingState(nation.code, newPoints, seasonNumber))
            } else {
                val drift = (-40..40).random()
                val newPoints = (current + drift).coerceIn(nation.baseRankingPoints - 200, nation.baseRankingPoints + 200).coerceIn(200, 2200)
                updates.add(NationRankingState(nation.code, newPoints, existingState[nation.code]?.lastFullSimSeason ?: 0))
            }
        }

        dao.upsertRankingStates(updates)
    }

    private fun parseResidencyMap(raw: String): Map<String, Int> {
        if (raw.isBlank()) return emptyMap()
        return raw.split(";").mapNotNull {
            val parts = it.split(":")
            if (parts.size == 2) parts[0] to (parts[1].toIntOrNull() ?: 0) else null
        }.toMap()
    }

    private fun serializeResidencyMap(map: Map<String, Int>): String {
        return map.entries.joinToString(";") { "${it.key}:${it.value}" }
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

        val count = when {
            player.ovr > 75 && player.form >= 1 -> Random.nextInt(2, 4)
            player.ovr > 70 && player.form >= 0 -> Random.nextInt(1, 3)
            else -> 1
        }.coerceIn(1, 3)

        val candidateClubs = if (eligibleTargets.isNotEmpty()) eligibleTargets else allClubs.filter { it.id != player.currentClubId }
        val generatedOffers = candidateClubs.shuffled().take(count).map { club ->
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
                retirementDescription = retirementDesc,
                faceDescriptor = player.faceDescriptor,
                finalAge = player.age,
                nationalTeamCode = player.nationalTeamCode
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
        val rng = kotlin.random.Random(
            player.generation.toLong() * 1_000_003L +
            totalGoals.toLong() * 97L +
            totalGames.toLong() * 31L +
            peakOvr.toLong()
        )
        val name = player.name

        // 1. PEAK OVR TIER - Verb phrase + Legacy phrase (6 options each)
        val openingLine = when {
            peakOvr >= 90 -> {
                val verbPhrases90 = listOf(
                    "retires as one of the true greats of the era",
                    "closes the chapter on a legendary career",
                    "leaves the game at the very pinnacle of world football",
                    "hangs up their boots as an iconic generational talent",
                    "steps away having reached the summit of the sport",
                    "bows out after redefining what it means to lead the line"
                )
                val legacyPhrases90 = listOf(
                    "a player defenses feared and fans idolized wherever they played",
                    "cementing a legacy amongst footballing royalty",
                    "revered as an unstoppable attacking force",
                    "whose extraordinary quality thrilled supporters worldwide",
                    "remembered as one of the sport's defining number 9s",
                    "leaving behind highlight reels that will be watched for decades"
                )
                "$name ${verbPhrases90[rng.nextInt(verbPhrases90.size)]}, ${legacyPhrases90[rng.nextInt(legacyPhrases90.size)]}."
            }
            peakOvr in 80..89 -> {
                val verbPhrases80 = listOf(
                    "built a career among the finest strikers of their generation",
                    "steps away from professional football having established a formidable reputation",
                    "completes a stellar professional journey at the senior level",
                    "retires after years of consistently high-level performances",
                    "calls time on an admirable and distinguished attacking career",
                    "walks away with the respect of the highest tiers of the sport"
                )
                val legacyPhrases80 = listOf(
                    "consistently delivering when it mattered most",
                    "recognized as a dangerous and highly reliable attacking focal point",
                    "earning widespread acclaim for composure in front of goal",
                    "celebrated as a clinical finisher capable of deciding any match",
                    "leaving an indelible mark with crucial goals on the biggest stages",
                    "remembered as a nightmare for opposing center-backs week in and week out"
                )
                "$name ${verbPhrases80[rng.nextInt(verbPhrases80.size)]}, ${legacyPhrases80[rng.nextInt(legacyPhrases80.size)]}."
            }
            peakOvr in 65..79 -> {
                val verbPhrases65 = listOf(
                    "carved out a respected, dependable career as a professional footballer",
                    "brings an honest and hardworking career to an end",
                    "closes a solid and committed tenure in professional football",
                    "concludes their playing journey with immense personal pride",
                    "hangs up the boots after a steadfast and dedicated career",
                    "bows out after years of honest graft leading the frontline"
                )
                val legacyPhrases65 = listOf(
                    "earning the deep trust of teammates and coaches alike",
                    "leaving behind a legacy of determination and discipline",
                    "having consistently put in relentless efforts for every club served",
                    "respected across the league as a consummate team-first professional",
                    "remembered for tireless work rate and steadfast commitment to the shirt",
                    "valued everywhere for holding the line with grit and resilience"
                )
                "$name ${verbPhrases65[rng.nextInt(verbPhrases65.size)]}, ${legacyPhrases65[rng.nextInt(legacyPhrases65.size)]}."
            }
            else -> {
                val verbPhrasesElse = listOf(
                    "may not have reached the very top, but gave everything to the game",
                    "retires with head held high from a gritty professional journey",
                    "completes a career defined by persistence, resilience, and heart",
                    "hangs up their boots after a fiercely dedicated playing journey",
                    "steps away having fought relentlessly through every level of the game",
                    "closes their chapter on the pitch having emptied the tank completely"
                )
                val legacyPhrasesElse = listOf(
                    "pouring every ounce of passion into each minute played",
                    "battling through every setback and challenge on the pitch",
                    "always displaying immense pride whenever stepping across the white lines",
                    "embodying the spirit of an honest professional who never backed down",
                    "earning the genuine admiration of supporters for sheer determination",
                    "leaving behind an enduring example of heart and perseverance"
                )
                "$name ${verbPhrasesElse[rng.nextInt(verbPhrasesElse.size)]}, ${legacyPhrasesElse[rng.nextInt(legacyPhrasesElse.size)]}."
            }
        }

        // 2. LONGEVITY / STAT-LINE MIDDLE - Connector + stats + club connector
        val statOpeners = listOf(
            "Across",
            "Over the course of",
            "Through",
            "In total across",
            "Spanning",
            "Racking up figures across"
        )
        val clubConnectors = listOf(
            "while turning out for",
            "in spells with",
            "representing",
            "across stops at",
            "wearing the shirts of",
            "over stints with"
        )
        val statOpener = statOpeners[rng.nextInt(statOpeners.size)]
        val clubConn = clubConnectors[rng.nextInt(clubConnectors.size)]
        val statLine = "$statOpener $totalGames appearances, they found the net $totalGoals times and set up $totalAssists more, $clubConn $clubsPlayedStr."

        // 3. TROPHY / LEGACY CLOSING LINE - Framing phrase + sentiment phrase (6 options each)
        val majorTrophies = myTrophies.filter { it.isMajor }
        val topTrophyName = majorTrophies.firstOrNull()?.competitionName
        val closingLine = when {
            topTrophyName != null || totalWeight >= 50 -> {
                val framingPhrases = listOf(
                    "Silverware including ${topTrophyName ?: "major titles"}",
                    "A glittering trophy cabinet headlined by ${topTrophyName ?: "major trophies"}",
                    "Crowned with prestigious honors such as ${topTrophyName ?: "championship titles"}",
                    "With marquee triumphs featuring ${topTrophyName ?: "elite silverware"}",
                    "A decorated medal collection highlighted by ${topTrophyName ?: "trophy wins"}",
                    "The defining glory of lifting ${topTrophyName ?: "championship trophies"}"
                )
                val sentimentPhrases = listOf(
                    "will forever mark this as a career of immense substance.",
                    "stands as permanent testament to their winning pedigree.",
                    "reflects a career defined by triumph on the biggest stages.",
                    "cements their reputation as a born champion.",
                    "ensures their legacy will be celebrated in club lore forever.",
                    "provides the ultimate crowning achievement to their time on the pitch."
                )
                "${framingPhrases[rng.nextInt(framingPhrases.size)]} ${sentimentPhrases[rng.nextInt(sentimentPhrases.size)]}"
            }
            myTrophies.isNotEmpty() -> {
                val framingPhrases = listOf(
                    "With key domestic honors secured along the way,",
                    "Honored with hard-fought cup triumphs,",
                    "Lifting silverware during their career",
                    "Having guided their side to memorable cup success,",
                    "Proudly collecting medals over competitive campaigns,",
                    "Having celebrated triumph on cup final afternoons,"
                )
                val sentimentPhrases = listOf(
                    "their contribution to team silverware remains truly memorable.",
                    "they ensured their efforts on the pitch translated into tangible glory.",
                    "provided cherished moments that supporters will long remember.",
                    "added tangible silverware to validate years of tireless dedication.",
                    "proved their ability to make an impact when medals were on the line.",
                    "ensured their name remains etched into club history books."
                )
                "${framingPhrases[rng.nextInt(framingPhrases.size)]} ${sentimentPhrases[rng.nextInt(sentimentPhrases.size)]}"
            }
            else -> {
                val framingPhrases = listOf(
                    "Though major silverware proved elusive,",
                    "While trophy cabinets don't tell the whole story,",
                    "Beyond medals and championship titles,",
                    "Even without the fortune of team silverware,",
                    "Though fate kept the biggest trophies just out of reach,",
                    "Looking past the absence of shiny silverware,"
                )
                val sentimentPhrases = listOf(
                    "their grit, loyalty, and sheer dedication on the pitch earned enduring respect.",
                    "their passion and commitment left an indelible mark on every club.",
                    "the journey itself and the respect earned from peers defined a career of true integrity.",
                    "their relentless spirit and devotion to the badge resonated with every supporter.",
                    "the sheer heart they brought to every single fixture remains unquestioned.",
                    "they leave the pitch knowing they gave every ounce of themselves to the craft."
                )
                "${framingPhrases[rng.nextInt(framingPhrases.size)]} ${sentimentPhrases[rng.nextInt(sentimentPhrases.size)]}"
            }
        }

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
            val gs0 = dao.getGameStateSync() ?: break
            if (player.isRetired || (player.age >= 41 && !player.isGodMode) || gs0.youthCareerEnded || gs0.pendingCallUpNationCode != null) break

            var gs = gs0
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
            if (updatedGs.youthCareerEnded || updatedGs.pendingCallUpNationCode != null) break
            currentSeason = updatedGs.currentSeason
        }
    }

    suspend fun devSimulateSeasons(seasonsToSimulate: Int) {
        val player0 = dao.getPlayerSync()
        val maxAllowed = if (player0?.isGodMode == true) 100 else 10
        val count = seasonsToSimulate.coerceIn(1, maxAllowed)
        for (i in 0 until count) {
            val player = dao.getPlayerSync() ?: break
            val gs = dao.getGameStateSync()
            if (player.isRetired || (player.age >= 41 && !player.isGodMode) || gs?.youthCareerEnded == true || gs?.pendingCallUpNationCode != null) break

            devSimulateFullSeason()

            val updatedPlayer = dao.getPlayerSync() ?: break
            val updatedGs = dao.getGameStateSync()
            if (updatedPlayer.isRetired || (updatedPlayer.age >= 41 && !updatedPlayer.isGodMode) || updatedGs?.youthCareerEnded == true || updatedGs?.pendingCallUpNationCode != null) break
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
        histories: List<ClubSeasonHistoryEntity>,
        usedNamesCache: MutableSet<String>? = null
    ) {
        val clubsMap = allClubs.associateBy { it.id }
        val finishPositionByClubId = histories.associate { it.clubId to it.leagueFinishPosition }
        val narrativeLogs = mutableListOf<String>()

        // -------------------------------------------------------------
        // 5a. STRIKER MOVEMENT PASS
        // -------------------------------------------------------------
        val activeStrikers = dao.getAllNpcStrikersSync().filter { !it.isRetired }.toMutableList()
        val playerClubArrivals = mutableListOf<String>()
        val playerClubDepartures = mutableListOf<String>()
        val keyLeagueMoves = mutableListOf<String>()

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
                    val newName = generateUniqueName(oldClub.country, dao, usedNamesCache)

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
                    val vacName = generateUniqueName(oldClub.country, dao, usedNamesCache)
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
                        playerClubArrivals.add("${striker.name} (${striker.ovr} OVR)")
                        player.rivalRelationship = 50
                    } else if (oldClub.id == player.currentClubId) {
                        playerClubDepartures.add("${striker.name} (${striker.ovr} OVR, to ${newClub.name})")
                    } else if (striker.ovr >= 78 || newClub.reputation == "ELITE") {
                        keyLeagueMoves.add("${striker.name} (${striker.ovr} OVR) ➔ ${newClub.name}")
                    }
                }
            }
        }

        if (playerClubArrivals.isNotEmpty()) {
            val playerClubName = clubsMap[player.currentClubId]?.name ?: "your club"
            val arrivalsStr = playerClubArrivals.joinToString(", ")
            val logText = "🔄 SQUAD ARRIVALS: $arrivalsStr joined $playerClubName — expect real competition for minutes."
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
                    content = "🔄 TRANSFER ROUNDUP: New attacking arrival(s) at $playerClubName: $arrivalsStr!",
                    isAboutPlayerOrClub = true,
                    relatedClubId = player.currentClubId,
                    likeCount = Random.nextInt(5000, 30000)
                )
            )
        }

        if (playerClubDepartures.isNotEmpty()) {
            val playerClubName = clubsMap[player.currentClubId]?.name ?: "your club"
            val departuresStr = playerClubDepartures.joinToString(", ")
            val logText = "🔄 SQUAD DEPARTURES: $departuresStr departed from $playerClubName."
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
                    content = "🔄 SQUAD UPDATE: Outgoing transfers confirmed at $playerClubName: $departuresStr.",
                    isAboutPlayerOrClub = true,
                    relatedClubId = player.currentClubId,
                    likeCount = Random.nextInt(3000, 25000)
                )
            )
        }

        if (keyLeagueMoves.isNotEmpty() && playerClubArrivals.isEmpty()) {
            val maxSeq = (dao.getMaxSocialPostSequenceIndex() ?: 0) + 1
            val sampleMoves = keyLeagueMoves.take(3).joinToString("; ")
            dao.insertSocialPost(
                SocialPostEntity(
                    sequenceIndex = maxSeq,
                    seasonNumber = gameState.currentSeason,
                    monthIndex = gameState.currentMonthIndex,
                    postType = "CLUB_NEWS",
                    authorName = "Transfer Deadline",
                    authorHandle = "@TransferNews",
                    authorInitials = "TN",
                    content = "🔄 DEADLINE DAY WRAP: Notable moves across the division: $sampleMoves.",
                    isAboutPlayerOrClub = false,
                    relatedClubId = null,
                    likeCount = Random.nextInt(4000, 20000)
                )
            )
        }

        dao.updateNpcStrikers(activeStrikers)

        // Unconditional cache sync for strikers
        val latestActiveStrikers = dao.getAllNpcStrikersSync().filter { !it.isRetired }
        val strikerByClubId = latestActiveStrikers
            .filter { it.currentClubId != null }
            .groupBy { it.currentClubId!! }
            .mapValues { (_, strikers) -> strikers.maxByOrNull { it.ovr } }

        for (club in allClubs) {
            val activeStriker = strikerByClubId[club.id]
            if (activeStriker != null) {
                club.rivalStrikerName = activeStriker.name
                club.rivalStrikerOvr = activeStriker.ovr
            } else {
                val fName = generateUniqueName(club.country, dao, usedNamesCache)
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
                            val replacementName = generateUniqueName(prevClub.country, dao, usedNamesCache)
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
                    appointedName = generateUniqueName(club.country, dao, usedNamesCache)
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
                val mName = generateUniqueName(club.country, dao, usedNamesCache)
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

        dao.pruneOldSocialPosts()
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
        return listOf("August", "September", "October", "November", "December", "January", "February", "March", "April", "May", "June", "July")[monthIndex]
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
        android.util.Log.d("ChoiceDebug", "resolveChoice entered with optionIndex=$optionIndex")
        var isEndOfSeason = false
        db.withTransaction {
            val player = dao.getPlayerSync() ?: run {
                android.util.Log.e("ChoiceDebug", "resolveChoice: player is null")
                return@withTransaction
            }
            val gameState = dao.getGameStateSync() ?: run {
                android.util.Log.e("ChoiceDebug", "resolveChoice: gameState is null")
                return@withTransaction
            }
            
            val prompt = gameState.activeChoicePrompt ?: run {
                android.util.Log.w("ChoiceDebug", "resolveChoice: activeChoicePrompt is null, nothing to resolve")
                return@withTransaction
            }
            android.util.Log.d("ChoiceDebug", "resolveChoice: resolving prompt='$prompt', pendingLogs=${gameState.activeChoicePendingMonthLogs != null}")
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
            player.finishing = applyStatGrowth(player.finishing, finishingMod.toFloat(), player.finishingCeiling)
            player.technique = applyStatGrowth(player.technique, techniqueMod.toFloat(), player.techniqueCeiling)
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
                if (nextMonth <= 11) {
                    gameState.currentMonthIndex = nextMonth
                    if (nextMonth == 0 || nextMonth == 5 || nextMonth == 10 || nextMonth == 11) {
                        player.hasTransferredThisWindow = false
                        dao.updatePlayer(player)
                    }
                    dao.updateGameState(gameState)
                } else {
                    isEndOfSeason = true
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
        android.util.Log.d("ChoiceDebug", "resolveChoice: choice resolution committed successfully")

        // Handle end of season transition outside choice transaction to avoid stuck save-lock
        val latestPlayer = dao.getPlayerSync()
        val latestGameState = dao.getGameStateSync()
        if (isEndOfSeason && latestPlayer != null && latestGameState != null) {
            try {
                checkAndAwardYouthTrophy(latestPlayer, latestGameState)
                resetYouthLeagueForNewSeason()
                latestPlayer.age += 1
                val endReport = handleEndOfSeason(latestPlayer, latestGameState)
                latestGameState.narrativeLog = capNarrativeLog(endReport + "\n\n" + latestGameState.narrativeLog)
                latestGameState.currentSeason += 1
                latestGameState.currentMonthIndex = 0
                latestPlayer.hasTransferredThisWindow = false
                dao.updatePlayer(latestPlayer)
                dao.updateGameState(latestGameState)
                android.util.Log.d("ChoiceDebug", "resolveChoice: end of season rollover completed")
            } catch (e: Exception) {
                android.util.Log.e("ChoiceDebug", "resolveChoice: error during end of season rollover", e)
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
            player.recentMatchRatings = pushRecentRating(player.recentMatchRatings, matchRating)
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
            player.recentMatchRatings = pushRecentRating(player.recentMatchRatings, fixture.playerRating ?: matchRating)
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

                if (nextMonth <= 11) {
                    gameState.currentMonthIndex = nextMonth
                    if (nextMonth == 0 || nextMonth == 5 || nextMonth == 10 || nextMonth == 11) {
                        player.hasTransferredThisWindow = false
                        dao.updatePlayer(player)
                    }
                    dao.updateGameState(gameState)
                } else {
                    checkAndAwardYouthTrophy(player, gameState)
                    resetYouthLeagueForNewSeason()
                    player.age += 1
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
    suspend fun generateUniqueName(country: String, dao: CareerDao, usedNamesCache: MutableSet<String>? = null): String {
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
                val isUsed = if (usedNamesCache != null) {
                    candidate in usedNamesCache
                } else {
                    dao.checkUsedNameCount(candidate) > 0
                }
                if (!isUsed) {
                    usedNamesCache?.add(candidate)
                    dao.insertUsedName(UsedNameEntity(candidate))
                    return candidate
                }
            }
            attempts++
        }
        // Fix the base name once instead of re-rolling every iteration — re-rolling against
        // an already-exhausted pool was causing thousands of DB round-trips per name during
        // long God Mode simulations. Suffixing a fixed base guarantees a unique result in a
        // small, bounded number of iterations.
        val baseName = FictionalData.generateRandomName(country)
        var fallbackSuffix = 1
        while (fallbackSuffix <= 500) {
            val candidate = "$baseName $fallbackSuffix"
            if (candidate !in blocklist) {
                val isUsed = if (usedNamesCache != null) {
                    candidate in usedNamesCache
                } else {
                    dao.checkUsedNameCount(candidate) > 0
                }
                if (!isUsed) {
                    usedNamesCache?.add(candidate)
                    dao.insertUsedName(UsedNameEntity(candidate))
                    return candidate
                }
            }
            fallbackSuffix++
        }
        // Absolute last resort — guarantees termination even in a pathological case.
        val fallbackCandidate = "$baseName ${System.currentTimeMillis()}"
        usedNamesCache?.add(fallbackCandidate)
        dao.insertUsedName(UsedNameEntity(fallbackCandidate))
        return fallbackCandidate
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
    suspend fun evolveClubRecords(
        club: ClubEntity,
        currentSeason: Int,
        wonTrophyThisSeason: Boolean = false,
        clubRecords: List<ClubRecordEntity>? = null,
        usedNamesCache: MutableSet<String>? = null
    ): List<ClubRecordEntity> {
        val chance = when (club.reputation) {
            "ELITE" -> 0.03f
            "BIG" -> 0.06f
            "MID" -> 0.10f
            "SMALL" -> 0.15f
            else -> 0.10f
        }

        val records = clubRecords ?: dao.getRecordsForClubSync(club.id)
        val modifiedRecords = mutableListOf<ClubRecordEntity>()
        for (record in records) {
            if (record.category == "MOST_TROPHIES") {
                // Gate MOST_TROPHIES evolution so it only fires if the club won a trophy this season
                if (wonTrophyThisSeason && kotlin.random.Random.nextFloat() < 0.5f) {
                    record.previousHolderName = record.holderName
                    record.previousStatValue = record.statValue
                    record.previousYearsActive = record.yearsActive

                    val npcName = generateUniqueName(club.country, dao, usedNamesCache)
                    record.holderName = npcName
                    record.statValue = record.statValue + 1
                    record.isActive = true
                    record.yearsActive = "${formatSeasonYear(currentSeason)} – present"
                    record.isUserPlayer = false
                    record.generation = null

                    if (clubRecords == null) {
                        dao.updateClubRecord(record)
                    } else {
                        modifiedRecords.add(record)
                    }
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

                val npcName = generateUniqueName(club.country, dao, usedNamesCache)
                record.holderName = npcName
                record.statValue = record.statValue + increment
                record.isActive = true
                record.yearsActive = "${formatSeasonYear(currentSeason)} – present"
                record.isUserPlayer = false
                record.generation = null

                if (clubRecords == null) {
                    dao.updateClubRecord(record)
                } else {
                    modifiedRecords.add(record)
                }
            }
        }
        return modifiedRecords
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
            (gameState.currentSeason.toLong() * 397 + gameState.currentMonthIndex)
        } else {
            var h = player.name.hashCode().toLong()
            h = h * 31 + player.generation
            h = h * 31 + player.age
            h = h * 31 + player.ovr
            h = h * 31 + gameState.currentSeason
            h = h * 31 + gameState.currentMonthIndex
            h
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
            "FUN FACT 💡 The original World Cup trophy, the Jules Rimet Trophy, was made of gold-plated sterling silver and lapis lazuli.",
            "FUN FACT 💡 The oldest professional football club in the world is Sheffield FC, founded back in 1857!",
            "FUN FACT 💡 Penalty shootouts were only officially adopted into the Laws of the Game in 1970; before that, tied games were often decided by coin tosses!",
            "FUN FACT 💡 King Pelé remains the youngest player ever to score in a World Cup final, doing so at just 17 years and 249 days old in 1958.",
            "FUN FACT 💡 The iconic 32-panel black and white football design (the Telstar) was created in 1970 so television viewers could easily track it on black-and-white screens.",
            "FUN FACT 💡 A goalkeeper cannot score an own goal directly from a throw-in or goal kick under the official Laws of the Game.",
            "FUN FACT 💡 The first ever international football match was played between Scotland and England in 1872, finishing in a 0-0 draw.",
            "FUN FACT 💡 Kazuyoshi Miura, known as 'King Kazu', holds the record as the oldest professional footballer to score in a competitive match, scoring past age 50.",
            "FUN FACT 💡 The fastest recorded red card in professional football occurred just 2 seconds after kickoff for a reckless challenge.",
            "FUN FACT 💡 A regulation football pitch is not fixed to one single size; standard dimensions can range between 100 to 110 meters in length and 64 to 75 meters in width.",
            "FUN FACT 💡 The highest scoring professional match in history finished 149-0 in Madagascar in 2002, when AS Adema's opponents repeatedly scored intentional own goals in protest.",
            "FUN FACT 💡 Arsenal's 2003-04 'Invincibles' went an entire 38-game Premier League season without a single defeat (26 wins, 12 draws).",
            "FUN FACT 💡 Red and yellow cards were invented by referee Ken Aston after he was inspired by traffic lights at a London intersection.",
            "FUN FACT 💡 Over one billion people watched the 2022 FIFA World Cup Final between Argentina and France across the globe.",
            "FUN FACT 💡 Lionel Messi holds the Guinness World Record for the most official goals scored in a single calendar year, netting 91 goals in 2012.",
            "FUN FACT 💡 The quickest hat-trick in Premier League history was scored by Sadio Mané in just 2 minutes and 56 seconds in 2015.",
            "FUN FACT 💡 Real Madrid won the first five editions of the European Cup consecutively between 1956 and 1960.",
            "FUN FACT 💡 Before modern crossbars were made mandatory in 1882, the top of a football goal was simply marked by a piece of tape or rope between two wooden posts.",
            "FUN FACT 💡 Goalkeepers were not required to wear a jersey color distinct from their teammates until 1909."
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
                allowPlayerPost && (roll < 0.65f || i == 0 && maxPlayerPosts > 0) -> {
                    playerPostsAdded++
                    val isInteractiveReply = rng.nextFloat() < 0.55f || (playerPostsAdded == 1)
                    when (phase) {
                        PHASE_STREET -> {
                            val (name, handle, init) = citizenAuthors[rng.nextInt(citizenAuthors.size)]
                            if (isInteractiveReply) {
                                val replyVariant = rng.nextInt(3)
                                when (replyVariant) {
                                    0 -> {
                                        SocialPostEntity(
                                            sequenceIndex = currentSeq,
                                            seasonNumber = gameState.currentSeason,
                                            monthIndex = gameState.currentMonthIndex,
                                            postType = "CITIZEN",
                                            authorName = name,
                                            authorHandle = handle,
                                            authorInitials = init,
                                            content = "Cage talk: Rival crew says ${player.name} is all flash and no end product. Can he back it up on Friday?",
                                            isAboutPlayerOrClub = true,
                                            relatedClubId = null,
                                            likeCount = scaledLikeCount(rng, 100, 1200, 0.4f, player),
                                            isReplyable = true,
                                            reply1Text = "Fiery: 'Drop the location. I'll drop you again.'",
                                            reply1FanRepMod = 5,
                                            reply1MoraleMod = 3,
                                            reply1RivalRelMod = -4,
                                            reply2Text = "Confident: 'Game speaks for itself. See you on the asphalt.'",
                                            reply2FanRepMod = 3,
                                            reply2MoraleMod = 2,
                                            reply2ManagerTrustMod = 2,
                                            reply3Text = "Dismissive: 'Who even are you?'",
                                            reply3FanRepMod = 2,
                                            reply3RivalRelMod = -2
                                        )
                                    }
                                    1 -> {
                                        SocialPostEntity(
                                            sequenceIndex = currentSeq,
                                            seasonNumber = gameState.currentSeason,
                                            monthIndex = gameState.currentMonthIndex,
                                            postType = "INFLUENCER",
                                            authorName = "Street Scout",
                                            authorHandle = "@StreetScoutDaily",
                                            authorInitials = "SS",
                                            content = "Word on the concrete: @${player.name} ($age) has academy scouts watching the cages. Is he ready for pro football?",
                                            isAboutPlayerOrClub = true,
                                            relatedClubId = null,
                                            likeCount = scaledLikeCount(rng, 300, 2500, 0.6f, player),
                                            isReplyable = true,
                                            reply1Text = "Ambitious: 'Ready and hungry. Time to prove it.'",
                                            reply1MoraleMod = 4,
                                            reply1FanRepMod = 4,
                                            reply1ManagerTrustMod = 3,
                                            reply2Text = "Grounded: 'One step at a time, working in the shadows.'",
                                            reply2ManagerTrustMod = 5,
                                            reply2MoraleMod = 2,
                                            reply3Text = "Team First: 'Credit to the crew holding it down with me.'",
                                            reply3FanRepMod = 4,
                                            reply3MoraleMod = 3
                                        )
                                    }
                                    else -> {
                                        SocialPostEntity(
                                            sequenceIndex = currentSeq,
                                            seasonNumber = gameState.currentSeason,
                                            monthIndex = gameState.currentMonthIndex,
                                            postType = "CITIZEN",
                                            authorName = name,
                                            authorHandle = handle,
                                            authorInitials = init,
                                            content = "That nutmeg from ${player.name} in the 3v3 semifinal broke the local internet 🔥",
                                            isAboutPlayerOrClub = true,
                                            relatedClubId = null,
                                            likeCount = scaledLikeCount(rng, 150, 1800, 0.5f, player),
                                            isReplyable = true,
                                            reply1Text = "Humble: 'Just having fun out there with the squad!'",
                                            reply1FanRepMod = 4,
                                            reply1MoraleMod = 3,
                                            reply2Text = "Bold: 'Just getting warmed up.'",
                                            reply2FanRepMod = 5,
                                            reply2MoraleMod = 4,
                                            reply2RivalRelMod = -2,
                                            reply3Text = "Laughs: 'Had to do it to him 😂'",
                                            reply3FanRepMod = 3,
                                            reply3MoraleMod = 2
                                        )
                                    }
                                }
                            } else {
                                val streetOpeners = listOf(
                                    "Street report:",
                                    "Local cage rat ${player.name}",
                                    "Cage highlight:",
                                    "@${player.name} from the cages",
                                    "Concrete jungle dispatch:",
                                    "Asphalt whispers:",
                                    "Underground tape:"
                                )
                                val streetObservations = listOf(
                                    "putting in serious work on the tarmac 👊",
                                    "bagging braces again — someone's getting scouted soon.",
                                    "($age) running riot in the back alleys with effortless style.",
                                    "showing footwork in the 3v3 tournament that was absolutely ridiculous 🔥",
                                    "humbling defenders with filthy nutmegs under the floodlights.",
                                    "leaving local keepers guessing all night long.",
                                    "turning heads across the neighborhood with unreal street flair."
                                )
                                val streetContent = "${streetOpeners[rng.nextInt(streetOpeners.size)]} ${streetObservations[rng.nextInt(streetObservations.size)]}"
                                SocialPostEntity(
                                    sequenceIndex = currentSeq,
                                    seasonNumber = gameState.currentSeason,
                                    monthIndex = gameState.currentMonthIndex,
                                    postType = "CITIZEN",
                                    authorName = name,
                                    authorHandle = handle,
                                    authorInitials = init,
                                    content = streetContent,
                                    isAboutPlayerOrClub = true,
                                    relatedClubId = null,
                                    likeCount = scaledLikeCount(rng, 50, 800, 0.3f, player)
                                )
                            }
                        }

                        PHASE_YOUTH -> {
                            val (name, handle, init) = punditAuthors[rng.nextInt(punditAuthors.size)]
                            if (isInteractiveReply) {
                                val youthVar = rng.nextInt(3)
                                when (youthVar) {
                                    0 -> {
                                        SocialPostEntity(
                                            sequenceIndex = currentSeq,
                                            seasonNumber = gameState.currentSeason,
                                            monthIndex = gameState.currentMonthIndex,
                                            postType = "PUNDIT",
                                            authorName = name,
                                            authorHandle = handle,
                                            authorInitials = init,
                                            content = "Youth Scout Analysis: ${player.name} shows immense striking instinct, but academy staff want to see more tactical maturity.",
                                            isAboutPlayerOrClub = true,
                                            relatedClubId = club?.id,
                                            likeCount = scaledLikeCount(rng, 500, 4500, 0.8f, player),
                                            isReplyable = true,
                                            reply1Text = "Professional: 'Studying film and working with coaches every day.'",
                                            reply1ManagerTrustMod = 6,
                                            reply1MoraleMod = 2,
                                            reply2Text = "Defiant: 'Goals win games. That is what I bring.'",
                                            reply2FanRepMod = 4,
                                            reply2MoraleMod = 3,
                                            reply2ManagerTrustMod = -2,
                                            reply3Text = "Humble: 'Always learning, always improving.'",
                                            reply3ManagerTrustMod = 4,
                                            reply3FanRepMod = 3,
                                            reply3MoraleMod = 2
                                        )
                                    }
                                    1 -> {
                                        SocialPostEntity(
                                            sequenceIndex = currentSeq,
                                            seasonNumber = gameState.currentSeason,
                                            monthIndex = gameState.currentMonthIndex,
                                            postType = "INFLUENCER",
                                            authorName = "Academy Tracker",
                                            authorHandle = "@AcademyTracker",
                                            authorInitials = "AT",
                                            content = "Big Youth Derby this week! Will ${player.name} handle the pressure against their biggest rivals?",
                                            isAboutPlayerOrClub = true,
                                            relatedClubId = club?.id,
                                            likeCount = scaledLikeCount(rng, 800, 6000, 0.9f, player),
                                            isReplyable = true,
                                            reply1Text = "Fired up: 'Derby days are made for big moments. Let's get it!'",
                                            reply1FanRepMod = 6,
                                            reply1MoraleMod = 4,
                                            reply1RivalRelMod = -4,
                                            reply2Text = "Composed: 'Preparation is done. Time to execute on the pitch.'",
                                            reply2ManagerTrustMod = 4,
                                            reply2MoraleMod = 3,
                                            reply2FanRepMod = 2,
                                            reply3Text = "Team First: 'We fight together for the crest.'",
                                            reply3ManagerTrustMod = 5,
                                            reply3FanRepMod = 4,
                                            reply3MoraleMod = 3
                                        )
                                    }
                                    else -> {
                                        SocialPostEntity(
                                            sequenceIndex = currentSeq,
                                            seasonNumber = gameState.currentSeason,
                                            monthIndex = gameState.currentMonthIndex,
                                            postType = "CITIZEN",
                                            authorName = "YouthFan_10",
                                            authorHandle = "@YouthFan_10",
                                            authorInitials = "YF",
                                            content = "Is ${player.name} the most exciting striker in the academy right now? Thoughts?",
                                            isAboutPlayerOrClub = true,
                                            relatedClubId = club?.id,
                                            likeCount = scaledLikeCount(rng, 400, 3500, 0.7f, player),
                                            isReplyable = true,
                                            reply1Text = "Grateful: 'Thank you for the support, means the world!'",
                                            reply1FanRepMod = 5,
                                            reply1MoraleMod = 3,
                                            reply2Text = "Hungry: 'This is just the foundation. More to come.'",
                                            reply2MoraleMod = 4,
                                            reply2FanRepMod = 4,
                                            reply2ManagerTrustMod = 2,
                                            reply3Text = "Modest: 'We have so many talents here pushing each other.'",
                                            reply3ManagerTrustMod = 4,
                                            reply3FanRepMod = 3
                                        )
                                    }
                                }
                            } else {
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
                        }

                        PHASE_SENIOR -> {
                            when {
                                age < 24 && ovr < 75 -> {
                                    val (name, handle, init) = influencerAuthors[rng.nextInt(influencerAuthors.size)]
                                    if (isInteractiveReply) {
                                        data class MinutesTakeVariant(
                                            val content: String,
                                            val reply1: String, val reply2: String, val reply3: String
                                        )
                                        val minutesTakeVariants = listOf(
                                            MinutesTakeVariant(
                                                content = "Pundit Take: Young ${player.name} ($age) has huge promise at $clubName, but should the manager start him regularly or manage his minutes?",
                                                reply1 = "Ambitious: 'I am ready whenever the gaffer calls my name.'",
                                                reply2 = "Patient: 'Trusting the manager's development process completely.'",
                                                reply3 = "Bold: 'Put me on the pitch and I will deliver.'"
                                            ),
                                            MinutesTakeVariant(
                                                content = "Debate Corner: ${player.name} ($age) keeps flashing potential at $clubName — is it time to make him a starter, or is patience still the right call?",
                                                reply1 = "Confident: 'I back myself to take that chance every time.'",
                                                reply2 = "Team First: 'Whatever the manager needs, I'll deliver.'",
                                                reply3 = "Blunt: 'Minutes are earned in training. I'm earning them.'"
                                            ),
                                            MinutesTakeVariant(
                                                content = "Analyst Notebook: the numbers say ${player.name} ($age) is ready for more minutes at $clubName. Does the manager agree?",
                                                reply1 = "Composed: 'The numbers will keep coming if I get the chance.'",
                                                reply2 = "Respectful: 'That call belongs to the manager, not me.'",
                                                reply3 = "Hungry: 'Every training session, I'm making it harder to leave me out.'"
                                            )
                                        )
                                        val variant = minutesTakeVariants.random(rng)
                                        SocialPostEntity(
                                            sequenceIndex = currentSeq,
                                            seasonNumber = gameState.currentSeason,
                                            monthIndex = gameState.currentMonthIndex,
                                            postType = "INFLUENCER",
                                            authorName = name,
                                            authorHandle = handle,
                                            authorInitials = init,
                                            content = variant.content,
                                            isAboutPlayerOrClub = true,
                                            relatedClubId = club?.id,
                                            likeCount = scaledLikeCount(rng, 1500, 14000, 1.1f, player),
                                            isReplyable = true,
                                            reply1Text = variant.reply1,
                                            reply1MoraleMod = 4,
                                            reply1FanRepMod = 4,
                                            reply1ManagerTrustMod = 3,
                                            reply2Text = variant.reply2,
                                            reply2ManagerTrustMod = 6,
                                            reply2MoraleMod = 2,
                                            reply3Text = variant.reply3,
                                            reply3FanRepMod = 5,
                                            reply3MoraleMod = 3,
                                            reply3ManagerTrustMod = -1
                                        )
                                    } else {
                                        val youngOpeners = listOf(
                                            "Young ${player.name}",
                                            "Developing talent: ${player.name} ($age)",
                                            "Prospect alert: ${player.name}",
                                            "Academy graduate ${player.name}",
                                            "Rising star ${player.name}",
                                            "Fresh face ${player.name}",
                                            "One to watch: ${player.name}"
                                        )
                                        val youngObservations = listOf(
                                            "making notable strides at $clubName.",
                                            "earning valuable late substitute minutes for the first team.",
                                            "showing encouraging glimpses of raw attacking potential.",
                                            "impressing the coaching staff during recent training sessions.",
                                            "turning heads with lively cameos off the bench.",
                                            "gradually adapting to the pace and physicality of senior football.",
                                            "proving they belong in the senior squad rotation at $clubName."
                                        )
                                        val youngContent = "${youngOpeners[rng.nextInt(youngOpeners.size)]} ${youngObservations[rng.nextInt(youngObservations.size)]}"
                                        SocialPostEntity(
                                            sequenceIndex = currentSeq,
                                            seasonNumber = gameState.currentSeason,
                                            monthIndex = gameState.currentMonthIndex,
                                            postType = "INFLUENCER",
                                            authorName = name,
                                            authorHandle = handle,
                                            authorInitials = init,
                                            content = youngContent,
                                            isAboutPlayerOrClub = true,
                                            relatedClubId = club?.id,
                                            likeCount = scaledLikeCount(rng, 1000, 12000, 1.0f, player)
                                        )
                                    }
                                }

                                (form <= -2 || managerTrust < 35) -> {
                                    val (name, handle, init) = citizenAuthors[rng.nextInt(citizenAuthors.size)]
                                    val critContent = listOf(
                                        "Frustrations mounting around ${player.name} after a dip in form for $clubName.",
                                        "Tough questions for ${player.name} after missing key opportunities in front of goal.",
                                        "Can ${player.name} bounce back and silence the doubters this week?"
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
                                        likeCount = scaledLikeCount(rng, 1000, 18000, 1.1f, player),
                                        isReplyable = true,
                                        reply1Text = "Accountability: 'I hold myself to the highest standard. Working hard to make it right.'",
                                        reply1ManagerTrustMod = 5,
                                        reply1FanRepMod = 4,
                                        reply1MoraleMod = 2,
                                        reply2Text = "Defiant: 'Form is temporary, class is permanent. Keep watching.'",
                                        reply2FanRepMod = 5,
                                        reply2MoraleMod = 3,
                                        reply2ManagerTrustMod = -2,
                                        reply3Text = "Team Unity: 'We stay together through the storm. Big response coming.'",
                                        reply3ManagerTrustMod = 4,
                                        reply3MoraleMod = 3,
                                        reply3FanRepMod = 3
                                    )
                                }

                                age >= 35 -> {
                                    val (name, handle, init) = punditAuthors[rng.nextInt(punditAuthors.size)]
                                    if (isInteractiveReply) {
                                        SocialPostEntity(
                                            sequenceIndex = currentSeq,
                                            seasonNumber = gameState.currentSeason,
                                            monthIndex = gameState.currentMonthIndex,
                                            postType = "PUNDIT",
                                            authorName = name,
                                            authorHandle = handle,
                                            authorInitials = init,
                                            content = "Legend Status: ${player.name} ($age) continues to lead $clubName. How will history remember this career?",
                                            isAboutPlayerOrClub = true,
                                            relatedClubId = club?.id,
                                            likeCount = scaledLikeCount(rng, 10000, 65000, 1.6f, player),
                                            isReplyable = true,
                                            reply1Text = "Heartfelt: 'Privileged to wear this shirt and give everything for the fans.'",
                                            reply1FanRepMod = 6,
                                            reply1MoraleMod = 4,
                                            reply2Text = "Relentless: 'Not done yet. We still have trophies to fight for.'",
                                            reply2MoraleMod = 5,
                                            reply2ManagerTrustMod = 4,
                                            reply2FanRepMod = 4,
                                            reply3Text = "Legacy: 'Passing wisdom to the next generation of players.'",
                                            reply3ManagerTrustMod = 5,
                                            reply3FanRepMod = 4,
                                            reply3MoraleMod = 3
                                        )
                                    } else {
                                        val veteranOpeners = listOf(
                                            "The end of an era:",
                                            "Pundit retrospective:",
                                            "Pure class:",
                                            "Legend status:",
                                            "Veteran leadership:",
                                            "Respect across the league:",
                                            "Golden years:"
                                        )
                                        val veteranObservations = listOf(
                                            "${player.name} ($age) contemplating the final chapter of a storied career.",
                                            "${player.name}'s lasting impact on the pitch over the years has been immense.",
                                            "${player.name} receiving warm ovations from opposing fans wherever they play.",
                                            "${player.name} ($age) still demonstrating timeless instincts inside the penalty box.",
                                            "${player.name} mentoring the next generation while delivering when called upon.",
                                            "${player.name} showing that experience and football IQ never fade.",
                                            "supporters cherish every remaining minute of ${player.name}'s masterclasses."
                                        )
                                        val veteranContent = "${veteranOpeners[rng.nextInt(veteranOpeners.size)]} ${veteranObservations[rng.nextInt(veteranObservations.size)]}"
                                        SocialPostEntity(
                                            sequenceIndex = currentSeq,
                                            seasonNumber = gameState.currentSeason,
                                            monthIndex = gameState.currentMonthIndex,
                                            postType = "PUNDIT",
                                            authorName = name,
                                            authorHandle = handle,
                                            authorInitials = init,
                                            content = veteranContent,
                                            isAboutPlayerOrClub = true,
                                            relatedClubId = club?.id,
                                            likeCount = scaledLikeCount(rng, 5000, 45000, 1.4f, player)
                                        )
                                    }
                                }

                                else -> {
                                    // Prime Senior player (age 24-34, OVR >= 75)
                                    val (name, handle, init) = punditAuthors[rng.nextInt(punditAuthors.size)]
                                    if (isInteractiveReply) {
                                        val primeVar = rng.nextInt(3)
                                        when (primeVar) {
                                            0 -> {
                                                SocialPostEntity(
                                                    sequenceIndex = currentSeq,
                                                    seasonNumber = gameState.currentSeason,
                                                    monthIndex = gameState.currentMonthIndex,
                                                    postType = "PUNDIT",
                                                    authorName = name,
                                                    authorHandle = handle,
                                                    authorInitials = init,
                                                    content = "Debate Night: Is ${player.name} the most clinical striker in European football right now?",
                                                    isAboutPlayerOrClub = true,
                                                    relatedClubId = club?.id,
                                                    likeCount = scaledLikeCount(rng, 8000, 75000, 1.8f, player),
                                                    isReplyable = true,
                                                    reply1Text = "Humble Star: 'Team creates the chances, I just finish them.'",
                                                    reply1ManagerTrustMod = 5,
                                                    reply1FanRepMod = 4,
                                                    reply1MoraleMod = 3,
                                                    reply2Text = "Confident: 'I back myself against any defense in the world.'",
                                                    reply2MoraleMod = 5,
                                                    reply2FanRepMod = 5,
                                                    reply2RivalRelMod = -3,
                                                    reply3Text = "Silverware Focused: 'Stats are nice, but only trophies matter.'",
                                                    reply3ManagerTrustMod = 6,
                                                    reply3FanRepMod = 5,
                                                    reply3MoraleMod = 4
                                                )
                                            }
                                            1 -> {
                                                SocialPostEntity(
                                                    sequenceIndex = currentSeq,
                                                    seasonNumber = gameState.currentSeason,
                                                    monthIndex = gameState.currentMonthIndex,
                                                    postType = "CLUB_NEWS",
                                                    authorName = "Club Insider",
                                                    authorHandle = "@ClubInsiderNews",
                                                    authorInitials = "CI",
                                                    content = "Transfer buzz: European giants reportedly scouting ${player.name} ahead of the upcoming transfer window.",
                                                    isAboutPlayerOrClub = true,
                                                    relatedClubId = club?.id,
                                                    likeCount = scaledLikeCount(rng, 12000, 85000, 1.9f, player),
                                                    isReplyable = true,
                                                    reply1Text = "Loyal: 'My heart is at $clubName. Fully dedicated to our badge.'",
                                                    reply1ManagerTrustMod = 7,
                                                    reply1FanRepMod = 6,
                                                    reply1MoraleMod = 3,
                                                    reply2Text = "Diplomatic: 'Focused purely on the next match. Let the agent handle rumors.'",
                                                    reply2ManagerTrustMod = 3,
                                                    reply2MoraleMod = 2,
                                                    reply3Text = "Ambitious: 'Always want to compete at the absolute highest level.'",
                                                    reply3MoraleMod = 4,
                                                    reply3FanRepMod = 2,
                                                    reply3ManagerTrustMod = -3
                                                )
                                            }
                                            else -> {
                                                SocialPostEntity(
                                                    sequenceIndex = currentSeq,
                                                    seasonNumber = gameState.currentSeason,
                                                    monthIndex = gameState.currentMonthIndex,
                                                    postType = "CITIZEN",
                                                    authorName = "RivalBanter_FC",
                                                    authorHandle = "@RivalBanterFC",
                                                    authorInitials = "RB",
                                                    content = "Big rivalry clash coming up. ${player.name} won't have it easy against our center-backs!",
                                                    isAboutPlayerOrClub = true,
                                                    relatedClubId = club?.id,
                                                    likeCount = scaledLikeCount(rng, 4000, 35000, 1.3f, player),
                                                    isReplyable = true,
                                                    reply1Text = "Sharp Banter: 'Tell them to wear running shoes 😉'",
                                                    reply1FanRepMod = 6,
                                                    reply1MoraleMod = 4,
                                                    reply1RivalRelMod = -5,
                                                    reply2Text = "Respectful: 'Rivalry matches are always intense. Looking forward to it.'",
                                                    reply2ManagerTrustMod = 4,
                                                    reply2RivalRelMod = 4,
                                                    reply2FanRepMod = 2,
                                                    reply3Text = "Laser Focused: 'Talking happens before the match. Winning happens during it.'",
                                                    reply3ManagerTrustMod = 5,
                                                    reply3MoraleMod = 4,
                                                    reply3FanRepMod = 4
                                                )
                                            }
                                        }
                                    } else {
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

    suspend fun evaluateNationalCallUp(player: PlayerEntity, gameState: GameStateEntity): Nation? {
        if (player.careerPhase != PHASE_SENIOR) return null
        if (player.nationalTeamCode != null) return null

        val fatherNationalTeamCode: String? = if (player.generation > 1) {
            dao.getLegacyByGeneration(player.generation - 1)?.nationalTeamCode
        } else null

        val eligibleCodes = getEligibleNationCodesIncludingInheritance(player, fatherNationalTeamCode)
        if (eligibleCodes.isEmpty()) return null

        val states = dao.getCallUpStateForPlayer(player.name, player.generation).associateBy { it.nationCode }
        val isWC = isWorldCupSeason(gameState.currentSeason)
        val avgRating = recentRatingAverage(player.recentMatchRatings) ?: return null

        val qualifyingNations = mutableListOf<Nation>()

        for (code in eligibleCodes) {
            val nation = nationByCode(code) ?: continue
            val st = states[code]
            if (st?.permanentlyStopped == true) continue
            if (st != null && st.cooldownUntilSeason > gameState.currentSeason) continue

            val thresholds = thresholdsForTier(nation.tier)
            val passesOvr = player.ovr >= thresholds.baseOvr
            val ratingReq = if (isWC) thresholds.worldCupRating else thresholds.baseRating
            val passesRating = avgRating >= ratingReq

            if (passesOvr && passesRating) {
                qualifyingNations.add(nation)
            }
        }

        if (qualifyingNations.isEmpty()) return null

        return qualifyingNations.sortedWith(
            compareByDescending<Nation> { it.baseRankingPoints }.thenBy { it.code }
        ).firstOrNull()
    }

    suspend fun acceptNationalCallUp(nationCode: String) {
        val player = dao.getPlayerSync() ?: return
        val gameState = dao.getGameStateSync() ?: return

        player.nationalTeamCode = nationCode
        dao.updatePlayer(player)

        gameState.pendingCallUpNationCode = null
        gameState.pendingCallUpIsWorldCup = false

        val nationName = nationByCode(nationCode)?.name ?: nationCode
        val logMsg = "🌍 Accepted call-up to represent $nationName!"
        gameState.narrativeLog = capNarrativeLog(logMsg + "\n\n" + gameState.narrativeLog)
        dao.updateGameState(gameState)
    }

    suspend fun declineNationalCallUp(nationCode: String) {
        val player = dao.getPlayerSync() ?: return
        val gameState = dao.getGameStateSync() ?: return

        val existingStates = dao.getCallUpStateForPlayer(player.name, player.generation)
        val state = existingStates.find { it.nationCode == nationCode } ?: NationCallUpState(
            playerName = player.name,
            generation = player.generation,
            nationCode = nationCode,
            declineCount = 0,
            cooldownUntilSeason = 0,
            permanentlyStopped = false
        )

        state.declineCount += 1
        when (state.declineCount) {
            1 -> state.cooldownUntilSeason = gameState.currentSeason + 1
            2 -> state.cooldownUntilSeason = gameState.currentSeason + 1
            else -> state.permanentlyStopped = true
        }

        dao.upsertCallUpState(state)

        gameState.pendingCallUpNationCode = null
        gameState.pendingCallUpIsWorldCup = false

        val nationName = nationByCode(nationCode)?.name ?: nationCode
        val logMsg = "🚫 Declined call-up from $nationName."
        gameState.narrativeLog = capNarrativeLog(logMsg + "\n\n" + gameState.narrativeLog)
        dao.updateGameState(gameState)
    }

    suspend fun markStreetTutorialSeen() {
        val gameState = dao.getGameStateSync() ?: return
        gameState.hasSeenStreetTutorial = true
        dao.updateGameState(gameState)
    }

    suspend fun markProTutorialSeen() {
        val gameState = dao.getGameStateSync() ?: return
        gameState.hasSeenProTutorial = true
        dao.updateGameState(gameState)
    }

    suspend fun setHasSeenStreetTutorial(seen: Boolean) {
        val gs = dao.getGameStateSync() ?: return
        gs.hasSeenStreetTutorial = seen
        dao.updateGameState(gs)
    }

    suspend fun setHasSeenProTutorial(seen: Boolean) {
        val gs = dao.getGameStateSync() ?: return
        gs.hasSeenProTutorial = seen
        dao.updateGameState(gs)
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
