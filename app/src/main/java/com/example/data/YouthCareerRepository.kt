package com.example.data

import kotlin.math.roundToInt
import kotlin.random.Random

const val MIN_YOUTH_THRESHOLD = 28
const val MAX_YOUTH_THRESHOLD = 52
const val MIN_SENIOR_THRESHOLD = 40
const val MAX_SENIOR_THRESHOLD = 72

data class YouthScoutOffer(
    val academyId: Int,
    val academyName: String,
    val parentClubReputation: String,
    val parentClubReputationPoints: Int,
    val youthRivalName: String,
    val youthRivalOvr: Int,
    val minScoutOvr: Int,
    val scoutReport: String
)

data class YouthToSeniorOffer(
    val clubId: Int,
    val clubName: String,
    val clubReputation: String,
    val rivalStrikerName: String,
    val rivalStrikerOvr: Int,
    val contractYears: Int,
    val targetGplusA: Int,
    val scoutReport: String
)

object YouthCareerLogic {

    fun generateYouthAcademies(allClubs: List<ClubEntity>): List<YouthAcademyEntity> {
        val top16 = allClubs.sortedWith(
            compareByDescending<ClubEntity> { it.reputationPoints }
                .thenByDescending { it.rivalStrikerOvr }
        ).take(16)

        if (top16.isEmpty()) return emptyList()

        val minRep = top16.minOf { it.reputationPoints }
        val maxRep = top16.maxOf { it.reputationPoints }
        val repSpan = (maxRep - minRep).coerceAtLeast(1)

        val minRiv = top16.minOf { it.rivalStrikerOvr }
        val maxRiv = top16.maxOf { it.rivalStrikerOvr }
        val rivSpan = (maxRiv - minRiv).coerceAtLeast(1)

        val youthSuffixes = listOf("Academy", "U19s", "Youth", "Colts")

        return top16.mapIndexed { index, parentClub ->
            val suffix = youthSuffixes[index % youthSuffixes.size]
            val academyName = "${parentClub.name} $suffix"

            val normRep = (parentClub.reputationPoints - minRep).toFloat() / repSpan
            val normRiv = (parentClub.rivalStrikerOvr - minRiv).toFloat() / rivSpan
            val combinedNorm = 0.8f * normRep + 0.2f * normRiv

            val threshold = (MIN_YOUTH_THRESHOLD + combinedNorm * (MAX_YOUTH_THRESHOLD - MIN_YOUTH_THRESHOLD)).roundToInt()

            val allFirsts = FictionalData.FIRST_NAMES[parentClub.country] ?: FictionalData.FIRST_NAMES["England"]!!
            val allLasts = FictionalData.LAST_NAMES[parentClub.country] ?: FictionalData.LAST_NAMES["England"]!!
            val rivalFirstName = allFirsts.random()
            val rivalLastName = allLasts.random()
            val youthRivalName = "$rivalFirstName $rivalLastName"
            val youthRivalOvr = (34 + combinedNorm * 22 + Random.nextInt(-3, 4)).roundToInt().coerceIn(30, 58)

            YouthAcademyEntity(
                parentClubId = parentClub.id,
                academyName = academyName,
                country = parentClub.country,
                parentReputation = parentClub.reputation,
                parentReputationPoints = parentClub.reputationPoints,
                minScoutOvr = threshold,
                youthRivalName = youthRivalName,
                youthRivalOvr = youthRivalOvr
            )
        }
    }

    fun generateYouthFixtures(academies: List<YouthAcademyEntity>): List<YouthFixtureEntity> {
        if (academies.size < 2) return emptyList()
        val fixtures = mutableListOf<YouthFixtureEntity>()

        val n = academies.size
        val academyIds = academies.map { it.id }.toMutableList()
        if (n % 2 != 0) {
            academyIds.add(-1)
        }
        val numTeams = academyIds.size
        val rounds = numTeams - 1
        val half = numTeams / 2

        // Distribute round-robin matches across months 0..9
        var matchCount = 0
        for (round in 0 until rounds) {
            val monthIndex = round % 10
            for (i in 0 until half) {
                val home = academyIds[i]
                val away = academyIds[numTeams - 1 - i]
                if (home != -1 && away != -1) {
                    val (h, a) = if (round % 2 == 0) home to away else away to home
                    fixtures.add(
                        YouthFixtureEntity(
                            monthIndex = monthIndex,
                            homeAcademyId = h,
                            awayAcademyId = a
                        )
                    )
                    matchCount++
                }
            }
            // Rotate list
            val last = academyIds.removeAt(numTeams - 1)
            academyIds.add(1, last)
        }

        return fixtures
    }

    fun generateScoutReportText(
        player: PlayerEntity,
        targetName: String,
        rivalName: String,
        rivalOvr: Int,
        offerIndex: Int = 0,
        parentReputation: String = "MID"
    ): String {
        val stats = listOf(
            "finishing" to player.finishing,
            "pace" to player.pace,
            "passing" to player.passing,
            "physical" to player.physical,
            "technique" to player.technique
        ).sortedByDescending { it.second }

        val topStat = stats[0].first
        val secondStat = stats[1].first

        val primaryPhrases = mapOf(
            "finishing" to "clinical precision in front of goal",
            "pace" to "blistering acceleration on the counter-attack",
            "passing" to "surgical vision and link-up play",
            "physical" to "immense functional strength and aerial presence",
            "technique" to "sublime ball control in tight spaces"
        )

        val secondaryPhrases = mapOf(
            "finishing" to "sharp composure inside the box",
            "pace" to "relentless off-the-ball movement",
            "passing" to "crisp one-touch distribution",
            "physical" to "dominant physical stamina",
            "technique" to "effortless dribbling agility"
        )

        val p1 = primaryPhrases[topStat] ?: "natural attacking instincts"
        val p2 = secondaryPhrases[secondStat] ?: "impressive tactical awareness"

        val repHook = when (parentReputation) {
            "ELITE" -> "As a world-renowned academy setup, they demand immediate high-level performance."
            "BIG" -> "Their prestigious development program offers a high-profile pathway to senior football."
            "MID" -> "Their technical staff specializes in polishing tactical growth for first-team readiness."
            else -> "Their tight-knit environment promises a direct, uninhibited route to senior minutes."
        }

        val rivalNote = "Current top prospect $rivalName ($rivalOvr OVR) anchors their squad, setting up a competitive duel for the starting shirt."

        return when (offerIndex % 3) {
            0 -> "Evaluators from $targetName highlighted your $p1 alongside $p2. $repHook $rivalNote"
            1 -> "A glowing scouting dossier from $targetName praised your $p1. Combined with your $p2, you fit their tactical blueprint perfectly. $repHook $rivalNote"
            else -> "$targetName scouts attended your recent performances, citing $p1 as a key asset. Paired with your $p2, $repHook $rivalNote"
        }
    }

    fun serializeYouthOffers(offers: List<YouthScoutOffer>): String {
        return offers.joinToString(";") { offer ->
            "${offer.academyId}|${offer.academyName}|${offer.parentClubReputation}|${offer.parentClubReputationPoints}|${offer.youthRivalName}|${offer.youthRivalOvr}|${offer.minScoutOvr}|${offer.scoutReport.replace("|", "/").replace(";", ",")}"
        }
    }

    fun deserializeYouthOffers(serialized: String): List<YouthScoutOffer> {
        if (serialized.isBlank()) return emptyList()
        return serialized.split(";").mapNotNull { part ->
            val sub = part.split("|")
            if (sub.size >= 8) {
                YouthScoutOffer(
                    academyId = sub[0].toIntOrNull() ?: 0,
                    academyName = sub[1],
                    parentClubReputation = sub[2],
                    parentClubReputationPoints = sub[3].toIntOrNull() ?: 0,
                    youthRivalName = sub[4],
                    youthRivalOvr = sub[5].toIntOrNull() ?: 0,
                    minScoutOvr = sub[6].toIntOrNull() ?: 0,
                    scoutReport = sub[7]
                )
            } else null
        }
    }

    fun serializeSeniorYouthOffers(offers: List<YouthToSeniorOffer>): String {
        return offers.joinToString(";") { offer ->
            "${offer.clubId}|${offer.clubName}|${offer.clubReputation}|${offer.rivalStrikerName}|${offer.rivalStrikerOvr}|${offer.contractYears}|${offer.targetGplusA}|${offer.scoutReport.replace("|", "/").replace(";", ",")}"
        }
    }

    fun deserializeSeniorYouthOffers(serialized: String): List<YouthToSeniorOffer> {
        if (serialized.isBlank()) return emptyList()
        return serialized.split(";").mapNotNull { part ->
            val sub = part.split("|")
            if (sub.size >= 8) {
                YouthToSeniorOffer(
                    clubId = sub[0].toIntOrNull() ?: 0,
                    clubName = sub[1],
                    clubReputation = sub[2],
                    rivalStrikerName = sub[3],
                    rivalStrikerOvr = sub[4].toIntOrNull() ?: 0,
                    contractYears = sub[5].toIntOrNull() ?: 0,
                    targetGplusA = sub[6].toIntOrNull() ?: 0,
                    scoutReport = sub[7]
                )
            } else null
        }
    }

    fun evaluatePlaystyleAssignment(player: PlayerEntity) {
        if (player.playstyleAssignedAtAge != null) return

        val shootingCount = player.shootingDrillsCompleted
        val passingCount = player.passingDrillsCompleted
        val paceCount = player.paceDrillsCompleted
        val techCount = player.technicalDrillsCompleted
        val physCount = player.physicalDrillsCompleted

        val counts = listOf(
            "Shooting" to shootingCount,
            "Passing" to passingCount,
            "Pace" to paceCount,
            "Technical" to techCount,
            "Physical" to physCount
        )

        val qualifyingGood = counts.filter { it.second >= 25 }

        var goodPlaystyle: String? = null

        val comboMap = mapOf(
            setOf("Shooting", "Passing") to "The Playmaker Finisher",
            setOf("Shooting", "Pace") to "The Poacher",
            setOf("Shooting", "Technical") to "The Virtuoso",
            setOf("Shooting", "Physical") to "The Battering Ram",
            setOf("Passing", "Pace") to "The Conductor",
            setOf("Passing", "Technical") to "The Maestro",
            setOf("Passing", "Physical") to "The Anchor",
            setOf("Pace", "Technical") to "The Wizard",
            setOf("Pace", "Physical") to "The Freight Train",
            setOf("Technical", "Physical") to "The Enforcer"
        )

        val soloMap = mapOf(
            "Shooting" to "The Marksman",
            "Passing" to "The Visionary",
            "Pace" to "Speed Demon",
            "Technical" to "Dribble Master",
            "Physical" to "Powerhouse"
        )

        when {
            qualifyingGood.size == 1 -> {
                goodPlaystyle = soloMap[qualifyingGood[0].first]
            }
            qualifyingGood.size == 2 -> {
                val set = setOf(qualifyingGood[0].first, qualifyingGood[1].first)
                goodPlaystyle = comboMap[set]
            }
            qualifyingGood.size >= 3 -> {
                val sortedTop2 = qualifyingGood.sortedByDescending { it.second }.take(2)
                val set = setOf(sortedTop2[0].first, sortedTop2[1].first)
                goodPlaystyle = comboMap[set]
            }
        }

        // Bad playstyle check for <= 5 drills
        val qualifyingBad = counts.filter { it.second <= 5 }
        var badPlaystyle: String? = null

        if (qualifyingBad.isNotEmpty()) {
            val lowest = qualifyingBad.minWithOrNull(
                compareBy<Pair<String, Int>> { it.second }.thenBy { it.first }
            )
            val badMap = mapOf(
                "Shooting" to "Wasteful",
                "Passing" to "Sloppy",
                "Pace" to "Lead-Footed",
                "Technical" to "Heavy Touch",
                "Physical" to "Fragile"
            )
            if (lowest != null) {
                badPlaystyle = badMap[lowest.first]
            }
        }

        player.assignedPlaystyle = goodPlaystyle
        player.assignedWeakness = badPlaystyle
        player.playstyleAssignedAtAge = 25

        // Apply stat bonuses/penalties (+3 for good, -3 for bad)
        if (goodPlaystyle != null) {
            applyPlaystyleStatEffect(player, goodPlaystyle, delta = 3)
        }
        if (badPlaystyle != null) {
            applyPlaystyleStatEffect(player, badPlaystyle, delta = -3)
        }
    }

    private fun applyPlaystyleStatEffect(player: PlayerEntity, playstyle: String, delta: Int) {
        when (playstyle) {
            "The Marksman", "Wasteful" -> player.finishing = (player.finishing + delta).coerceIn(30, 99)
            "The Visionary", "Sloppy" -> player.passing = (player.passing + delta).coerceIn(30, 99)
            "Speed Demon", "Lead-Footed" -> player.pace = (player.pace + delta).coerceIn(30, 99)
            "Dribble Master", "Heavy Touch" -> player.technique = (player.technique + delta).coerceIn(30, 99)
            "Powerhouse", "Fragile" -> player.physical = (player.physical + delta).coerceIn(30, 99)
            "The Playmaker Finisher" -> {
                player.finishing = (player.finishing + delta).coerceIn(30, 99)
                player.passing = (player.passing + delta).coerceIn(30, 99)
            }
            "The Poacher" -> {
                player.finishing = (player.finishing + delta).coerceIn(30, 99)
                player.pace = (player.pace + delta).coerceIn(30, 99)
            }
            "The Virtuoso" -> {
                player.finishing = (player.finishing + delta).coerceIn(30, 99)
                player.technique = (player.technique + delta).coerceIn(30, 99)
            }
            "The Battering Ram" -> {
                player.finishing = (player.finishing + delta).coerceIn(30, 99)
                player.physical = (player.physical + delta).coerceIn(30, 99)
            }
            "The Conductor" -> {
                player.passing = (player.passing + delta).coerceIn(30, 99)
                player.pace = (player.pace + delta).coerceIn(30, 99)
            }
            "The Maestro" -> {
                player.passing = (player.passing + delta).coerceIn(30, 99)
                player.technique = (player.technique + delta).coerceIn(30, 99)
            }
            "The Anchor" -> {
                player.passing = (player.passing + delta).coerceIn(30, 99)
                player.physical = (player.physical + delta).coerceIn(30, 99)
            }
            "The Wizard" -> {
                player.pace = (player.pace + delta).coerceIn(30, 99)
                player.technique = (player.technique + delta).coerceIn(30, 99)
            }
            "The Freight Train" -> {
                player.pace = (player.pace + delta).coerceIn(30, 99)
                player.physical = (player.physical + delta).coerceIn(30, 99)
            }
            "The Enforcer" -> {
                player.technique = (player.technique + delta).coerceIn(30, 99)
                player.physical = (player.physical + delta).coerceIn(30, 99)
            }
        }
        player.ovr = ((player.finishing + player.pace + player.passing + player.physical + player.technique) / 5.0f).roundToInt()
    }
}
