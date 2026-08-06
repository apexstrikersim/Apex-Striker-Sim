package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

const val PHASE_STREET = "STREET_FOOTBALL"
const val PHASE_YOUTH = "YOUTH_ACADEMY"
const val PHASE_SENIOR = "SENIOR"

@Entity(tableName = "youth_academies")
data class YouthAcademyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val parentClubId: Int,
    val academyName: String,
    val country: String,
    val parentReputation: String,
    val parentReputationPoints: Int,
    val minScoutOvr: Int,
    val youthRivalName: String,
    val youthRivalOvr: Int
)

@Entity(tableName = "youth_standings")
data class YouthStandingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val academyId: Int,
    var played: Int = 0,
    var wins: Int = 0,
    var draws: Int = 0,
    var losses: Int = 0,
    var goalsFor: Int = 0,
    var goalsAgainst: Int = 0,
    var points: Int = 0
)

@Entity(tableName = "youth_fixtures", indices = [Index(value = ["monthIndex"])])
data class YouthFixtureEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val monthIndex: Int,
    val homeAcademyId: Int,
    val awayAcademyId: Int,
    var homeScore: Int? = null,
    var awayScore: Int? = null,
    var isSimulated: Boolean = false,
    var playerGoals: Int = 0,
    var playerAssists: Int = 0,
    var playerMvp: Boolean = false,
    var playerCameOnMinute: Int? = null
)

@Entity(tableName = "street_football_games", indices = [Index(value = ["seasonNumber"])])
data class StreetFootballGameEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val seasonNumber: Int,
    val monthIndex: Int,
    val opponentName: String,
    var playerGoals: Int = 0,
    var playerAssists: Int = 0,
    var playerMvp: Boolean = false,
    var resultSummary: String = ""
)

@Entity(tableName = "npc_strikers", indices = [Index(value = ["currentClubId"])])
data class NpcStrikerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val country: String,           // the league/country this striker plays in
    var currentClubId: Int?,       // null = retired
    var age: Int,
    var ovr: Int,
    var potentialCeiling: Int,     // young replacements get a high ceiling so they can grow into real threats over several seasons
    var seasonsAtCurrentClub: Int = 0,
    var isRetired: Boolean = false,
    var isRisingTalent: Boolean = false  // true only for freshly-generated academy replacements
)

@Entity(tableName = "npc_managers", indices = [Index(value = ["currentClubId"])])
data class NpcManagerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val country: String,
    var currentClubId: Int?,       // null = between jobs / retired
    var reputationTier: String,    // "ELITE", "BIG", "MID", "SMALL"
    var seasonsAtCurrentClub: Int = 0,
    var seasonsAsManager: Int = 0, // career total, never resets
    var isRetired: Boolean = false
)

@Entity(tableName = "clubs")
data class ClubEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val country: String,
    val reputation: String, // "ELITE", "BIG", "MID", "SMALL"
    val reputationPoints: Int,
    var rivalStrikerName: String,
    var rivalStrikerOvr: Int,
    val description: String = "",
    val foundedSeasonsAgo: Int = 0,
    // Era-shift system: persists a multi-season rise/decline arc per club.
    val momentumBias: Float = 0f,
    val trajectorySeasonsRemaining: Int = 0,
    var managerName: String = "",
    var managerReputationTier: String = "MID",
    var managerId: Int = 0
) {
    val foundedYear: Int
        get() = CAREER_START_YEAR - foundedSeasonsAgo
}

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey val id: Int = 1,
    val name: String,
    var age: Int,
    val birthCountry: String,
    val academyCountry: String,
    var currentClubId: Int,
    var finishing: Int,
    var pace: Int,
    var passing: Int,
    var physical: Int,
    var technique: Int,
    val talentSeed: Float,
    var ovr: Int,
    var form: Int, // -5 to +5
    var gamesPlayed: Int = 0,
    var goals: Int = 0,
    var assists: Int = 0,
    var mvps: Int = 0,
    var contractYearsRemaining: Int,
    var contractTargetGoalsAssists: Int,
    var seasonGamesPlayed: Int = 0,
    var seasonGoals: Int = 0,
    var seasonAssists: Int = 0,
    var seasonMvps: Int = 0,
    var isRetired: Boolean = false,
    val generation: Int = 1,
    var peakOvr: Int,
    var potentialCeiling: Int = 99,
    var fatigue: Int = 0,
    var overtrainingRisk: Int = 0,
    var hasTrainedThisMonth: Int = 0,
    var finishingTrainingBonus: Float = 0f,
    var paceTrainingBonus: Float = 0f,
    var passingTrainingBonus: Float = 0f,
    var physicalTrainingBonus: Float = 0f,
    var techniqueTrainingBonus: Float = 0f,
    var morale: Int = 50,
    var fanReputation: Int = 50,
    var managerTrust: Int = 50,
    var rivalRelationship: Int = 50,
    var activePromiseGoalsAssists: Int? = null,
    var activePromiseGamesRemaining: Int? = null,
    var hasTransferredThisWindow: Boolean = false,
    var clubGamesPlayed: Int = 0,
    var clubGoals: Int = 0,
    var clubAssists: Int = 0,
    var careerPhase: String = PHASE_STREET,
    var currentAcademyId: Int? = null,
    var rejectedAcademyIds: String = "",
    var rejectedSeniorClubIds: String = "",
    var youthGoals: Int = 0,
    var youthAssists: Int = 0,
    var youthMvps: Int = 0,
    var youthGamesPlayed: Int = 0,
    var shootingDrillsCompleted: Int = 0,
    var passingDrillsCompleted: Int = 0,
    var paceDrillsCompleted: Int = 0,
    var technicalDrillsCompleted: Int = 0,
    var physicalDrillsCompleted: Int = 0,
    var assignedPlaystyle: String? = null,
    var assignedWeakness: String? = null,
    var playstyleAssignedAtAge: Int? = null,
    var streetFootballGamesThisSeason: Int = 0,
    var preferredFoot: String = "Right",
    var squadNumber: Int = 9,
    var backgroundStory: String = "Street Cages",
    var pendingNewManagerNotice: Boolean = false,
    var isGodMode: Boolean = false,
    var lastGoalMilestonePosted: Int = 0
)

@Entity(tableName = "standings", indices = [Index(value = ["country"])])
data class StandingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val country: String,
    val clubId: Int,
    var played: Int = 0,
    var wins: Int = 0,
    var draws: Int = 0,
    var losses: Int = 0,
    var goalsFor: Int = 0,
    var goalsAgainst: Int = 0,
    var points: Int = 0
)

@Entity(tableName = "fixtures", indices = [Index(value = ["monthIndex"])])
data class FixtureEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val country: String,
    val competition: String, // "LEAGUE", "CHAMPIONS_LEAGUE", "EUROPA_LEAGUE", "CONFERENCE_LEAGUE", "SUPER_CUP"
    val monthIndex: Int, // 0 = Aug, 9 = May
    val homeClubId: Int,
    val awayClubId: Int,
    var homeScore: Int? = null,
    var awayScore: Int? = null,
    var isSimulated: Boolean = false,
    var playerCameOnMinute: Int? = null, // null if benched, 0 if started, >0 if subbed
    var playerSubbedOffMinute: Int? = null, // null if played to end, or minute subbed off
    var playerGoals: Int = 0,
    var playerAssists: Int = 0,
    var playerMvp: Boolean = false,
    val leg: Int = 1,
    val round: Int = 0, // 0 = group/standard, 16 = Rd 16, 8 = Quarter, 4 = Semi, 2 = Final
    var playerRating: Float? = null,
    var wentToExtraTime: Boolean = false,
    var homePens: Int? = null,
    var awayPens: Int? = null,
    var goalMinutes: String? = null,
    var assistMinutes: String? = null
)

@Entity(tableName = "trophies", indices = [Index(value = ["playerName", "generation", "clubName"]), Index(value = ["clubName"])])
data class TrophyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playerName: String,
    val generation: Int,
    val clubName: String,
    val seasonYear: Int,
    val competitionName: String,
    val isMajor: Boolean
)

@Entity(tableName = "legacies")
data class LegacyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val generation: Int,
    val name: String,
    val totalGames: Int,
    val totalGoals: Int,
    val totalAssists: Int,
    val totalMvps: Int,
    val totalTrophiesWeight: Int,
    val peakOvr: Int,
    val finalOvr: Int,
    val clubsPlayed: String, // comma-separated names
    val isCompleted: Boolean = false,
    val retirementDescription: String = ""
)

@Entity(tableName = "game_state")
data class GameStateEntity(
    @PrimaryKey val id: Int = 1,
    var currentSeason: Int = 1,
    var currentMonthIndex: Int = 0, // 0 to 9 (Aug to May)
    var narrativeLog: String = "", // BitLife-style logs
    var autoSave: Boolean = true,
    var isDevMode: Boolean = false,
    var activeChoicePrompt: String? = null,
    var activeChoiceOption1: String? = null,
    var activeChoiceOption2: String? = null,
    var activeChoiceOption3: String? = null,
    var activeChoiceFormMod1: Int = 0,
    var activeChoiceFinishingMod1: Int = 0,
    var activeChoiceTechniqueMod1: Int = 0,
    var activeChoiceOutcome1: String? = null,
    var activeChoiceFormMod2: Int = 0,
    var activeChoiceFinishingMod2: Int = 0,
    var activeChoiceTechniqueMod2: Int = 0,
    var activeChoiceOutcome2: String? = null,
    var activeChoiceFormMod3: Int = 0,
    var activeChoiceFinishingMod3: Int = 0,
    var activeChoiceTechniqueMod3: Int = 0,
    var activeChoiceOutcome3: String? = null,
    
    var activeChoiceMoraleMod1: Int = 0,
    var activeChoiceFanRepMod1: Int = 0,
    var activeChoiceManagerTrustMod1: Int = 0,
    var activeChoiceRivalRelMod1: Int = 0,
    var activeChoiceFatigueMod1: Int = 0,
    
    var activeChoiceMoraleMod2: Int = 0,
    var activeChoiceFanRepMod2: Int = 0,
    var activeChoiceManagerTrustMod2: Int = 0,
    var activeChoiceRivalRelMod2: Int = 0,
    var activeChoiceFatigueMod2: Int = 0,
    
    var activeChoiceMoraleMod3: Int = 0,
    var activeChoiceFanRepMod3: Int = 0,
    var activeChoiceManagerTrustMod3: Int = 0,
    var activeChoiceRivalRelMod3: Int = 0,
    var activeChoiceFatigueMod3: Int = 0,
    
    var activeChoicePendingMonthLogs: String? = null,
    var persistedTransferOffers: String? = null,
    var managerTalkCooldownMonths: Int = 0,
    var persistedYouthOffers: String? = null,
    var persistedSeniorYouthOffers: String? = null,
    var youthCareerEnded: Boolean = false,
    var recentChoiceEventIds: String = ""
)

@Entity(tableName = "used_names", indices = [Index(value = ["name"], unique = true)])
data class UsedNameEntity(
    @PrimaryKey val name: String
)

@Entity(tableName = "club_season_history", indices = [Index(value = ["clubId"])])
data class ClubSeasonHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val seasonNumber: Int,
    val country: String,
    val clubId: Int,
    val leagueFinishPosition: Int,
    val europeanRank: String, // "NO", "Won UCL", "Final of UEL", etc.
    val superCupResult: String // "Won", "Lost", "NO"
)

@Entity(tableName = "club_records", indices = [Index(value = ["clubId"])])
data class ClubRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clubId: Int,
    val category: String, // "TOP_SCORER", "MOST_ASSISTS", "MOST_APPEARANCES", "MOST_TROPHIES"
    var holderName: String,
    var statValue: Int,
    var isActive: Boolean,
    var yearsActive: String,
    var isUserPlayer: Boolean = false,
    var generation: Int? = null,
    var previousHolderName: String? = null,
    var previousStatValue: Int? = null,
    var previousYearsActive: String? = null
)

@Entity(tableName = "player_club_stints", primaryKeys = ["playerName", "generation", "clubId"], indices = [Index(value = ["playerName", "generation"])])
data class PlayerClubStintEntity(
    val playerName: String,
    val generation: Int,
    val clubId: Int,
    val goals: Int,
    val assists: Int,
    val gamesPlayed: Int
)

@Entity(
    tableName = "player_season_records",
    indices = [Index(value = ["playerName", "generation"])]
)
data class PlayerSeasonRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playerName: String,
    val generation: Int,
    val seasonNumber: Int,       // matches GameStateEntity.currentSeason at the time
    val clubName: String,        // player's club at end of that season
    val matchesPlayed: Int,      // player.seasonGamesPlayed at capture time
    val goals: Int,              // player.seasonGoals at capture time
    val assists: Int,            // player.seasonAssists at capture time
    val ovrAtSeasonEnd: Int,     // player.ovr at capture time
    val playerAge: Int = 0       // player.age at capture time
)

const val CAREER_START_YEAR = 2026

fun formatSeasonYear(seasonNumber: Int): String {
    val startYear = 2025 + seasonNumber
    val endYearSuffix = (startYear + 1) % 100
    return "$startYear/${if (endYearSuffix < 10) "0$endYearSuffix" else endYearSuffix}"
}

fun calculateProratedTarget(baseTarget: Int, gamesPlayed: Int, totalGames: Float = 30f): Int {
    if (gamesPlayed <= 0) return baseTarget.coerceAtLeast(2)
    val playedPct = (gamesPlayed / totalGames).coerceIn(0f, 1f)
    return kotlin.math.round((baseTarget * playedPct)).toInt().coerceAtLeast(2)
}

data class PlayerOfTheSeasonAward(
    val isWinner: Boolean,
    val awardTitle: String,
    val totalPoints: Int,
    val goalsPoints: Int,
    val assistsPoints: Int,
    val trophiesPoints: Int,
    val ratingPoints: Int,
    val rank: Int
)

data class SeasonSummaryData(
    val seasonNumber: Int,
    val playerName: String,
    val generation: Int,
    val clubName: String,
    val matchesPlayed: Int,
    val goals: Int,
    val assists: Int,
    val averageRating: Float,
    val trophiesWon: List<String>,
    val playerOfTheSeasonAward: PlayerOfTheSeasonAward
)

@Entity(tableName = "social_posts", indices = [Index(value = ["sequenceIndex"])])
data class SocialPostEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sequenceIndex: Int,          // global monotonic counter, used for ordering + 30-cap pruning
    val seasonNumber: Int,
    val monthIndex: Int,
    val postType: String,            // "PUNDIT", "INFLUENCER", "CITIZEN", "CLUB_NEWS", "MILESTONE", "ON_THIS_DAY", "TREND", "FUN_FACT"
    val authorName: String,
    val authorHandle: String,        // e.g. "@ballon_watch"
    val authorInitials: String,      // 2-3 chars for avatar
    val content: String,
    val isAboutPlayerOrClub: Boolean, // drives the filter
    val relatedClubId: Int?,
    val likeCount: Int,              // cosmetic, rolled once at creation
    val isReplyable: Boolean = false,
    var hasReplied: Boolean = false,
    var selectedReplyIndex: Int? = null,
    val reply1Text: String? = null,
    val reply1MoraleMod: Int = 0,
    val reply1FanRepMod: Int = 0,
    val reply1ManagerTrustMod: Int = 0,
    val reply1RivalRelMod: Int = 0,
    val reply2Text: String? = null,
    val reply2MoraleMod: Int = 0,
    val reply2FanRepMod: Int = 0,
    val reply2ManagerTrustMod: Int = 0,
    val reply2RivalRelMod: Int = 0,
    val reply3Text: String? = null,
    val reply3MoraleMod: Int = 0,
    val reply3FanRepMod: Int = 0,
    val reply3ManagerTrustMod: Int = 0,
    val reply3RivalRelMod: Int = 0
)


