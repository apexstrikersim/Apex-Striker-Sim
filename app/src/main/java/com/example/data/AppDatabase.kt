package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface CareerDao {

    // Player
    @Query("SELECT * FROM players WHERE id = 1 LIMIT 1")
    fun getPlayerFlow(): Flow<PlayerEntity?>

    @Query("SELECT * FROM players WHERE id = 1 LIMIT 1")
    suspend fun getPlayerSync(): PlayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: PlayerEntity)

    @Update
    suspend fun updatePlayer(player: PlayerEntity)

    @Query("DELETE FROM players")
    suspend fun clearPlayers()

    // Clubs
    @Query("SELECT * FROM clubs ORDER BY id ASC")
    fun getAllClubsFlow(): Flow<List<ClubEntity>>

    @Query("SELECT * FROM clubs ORDER BY id ASC")
    suspend fun getAllClubsSync(): List<ClubEntity>

    @Query("SELECT * FROM clubs WHERE id = :id LIMIT 1")
    suspend fun getClubById(id: Int): ClubEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClubs(clubs: List<ClubEntity>)

    @Update
    suspend fun updateClub(club: ClubEntity)

    @Update
    suspend fun updateClubs(clubs: List<ClubEntity>)

    @Query("DELETE FROM clubs")
    suspend fun clearClubs()

    // Standings
    @Query("SELECT * FROM standings WHERE country = :country ORDER BY points DESC, (goalsFor - goalsAgainst) DESC, goalsFor DESC")
    fun getStandingsByCountryFlow(country: String): Flow<List<StandingEntity>>

    @Query("SELECT * FROM standings ORDER BY points DESC")
    suspend fun getAllStandingsSync(): List<StandingEntity>

    @Query("SELECT * FROM standings WHERE country = :country ORDER BY points DESC, (goalsFor - goalsAgainst) DESC, goalsFor DESC")
    suspend fun getStandingsByCountrySync(country: String): List<StandingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStandings(standings: List<StandingEntity>)

    @Update
    suspend fun updateStandings(standings: List<StandingEntity>)

    @Query("DELETE FROM standings")
    suspend fun clearStandings()

    // Fixtures
    @Query("SELECT * FROM fixtures ORDER BY id ASC")
    fun getAllFixturesFlow(): Flow<List<FixtureEntity>>

    @Query("SELECT * FROM fixtures WHERE monthIndex = :monthIndex ORDER BY round ASC, id ASC")
    fun getFixturesForMonthFlow(monthIndex: Int): Flow<List<FixtureEntity>>

    @Query("SELECT * FROM fixtures WHERE monthIndex = :monthIndex ORDER BY round ASC, id ASC")
    suspend fun getFixturesForMonthSync(monthIndex: Int): List<FixtureEntity>

    @Query("SELECT * FROM fixtures")
    suspend fun getAllFixturesSync(): List<FixtureEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFixtures(fixtures: List<FixtureEntity>)

    @Update
    suspend fun updateFixture(fixture: FixtureEntity)

    @Update
    suspend fun updateFixtures(fixtures: List<FixtureEntity>)

    @Query("DELETE FROM fixtures")
    suspend fun clearFixtures()

    // Trophies
    @Query("SELECT * FROM trophies ORDER BY id DESC")
    fun getAllTrophiesFlow(): Flow<List<TrophyEntity>>

    @Query("SELECT * FROM trophies ORDER BY id DESC")
    suspend fun getAllTrophiesSync(): List<TrophyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrophy(trophy: TrophyEntity)

    @Query("DELETE FROM trophies")
    suspend fun clearTrophies()

    // Legacies (Family lineage)
    @Query("SELECT * FROM legacies ORDER BY generation ASC")
    fun getAllLegaciesFlow(): Flow<List<LegacyEntity>>

    @Query("SELECT * FROM legacies ORDER BY generation ASC")
    suspend fun getAllLegaciesSync(): List<LegacyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLegacy(legacy: LegacyEntity)

    @Query("DELETE FROM legacies")
    suspend fun clearLegacies()

    // GameState
    @Query("SELECT * FROM game_state WHERE id = 1 LIMIT 1")
    fun getGameStateFlow(): Flow<GameStateEntity?>

    @Query("SELECT * FROM game_state WHERE id = 1 LIMIT 1")
    suspend fun getGameStateSync(): GameStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameState(gameState: GameStateEntity)

    @Update
    suspend fun updateGameState(gameState: GameStateEntity)

    @Query("DELETE FROM game_state")
    suspend fun clearGameState()

    // Used Names
    @Query("SELECT COUNT(*) FROM used_names WHERE name = :name")
    suspend fun checkUsedNameCount(name: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUsedName(usedName: UsedNameEntity)

    @Query("DELETE FROM used_names")
    suspend fun clearUsedNames()

    // Club Season History
    @Query("SELECT * FROM club_season_history WHERE clubId = :clubId ORDER BY seasonNumber DESC")
    fun getHistoryForClubFlow(clubId: Int): Flow<List<ClubSeasonHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClubSeasonHistories(histories: List<ClubSeasonHistoryEntity>)

    @Query("DELETE FROM club_season_history")
    suspend fun clearClubSeasonHistories()

    // Club Records
    @Query("SELECT * FROM club_records WHERE clubId = :clubId")
    fun getRecordsForClubFlow(clubId: Int): Flow<List<ClubRecordEntity>>

    @Query("SELECT * FROM club_records WHERE clubId = :clubId")
    suspend fun getRecordsForClubSync(clubId: Int): List<ClubRecordEntity>

    @Query("SELECT * FROM club_records")
    suspend fun getAllClubRecordsSync(): List<ClubRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClubRecords(records: List<ClubRecordEntity>)

    @Update
    suspend fun updateClubRecord(record: ClubRecordEntity)

    @Query("DELETE FROM club_records")
    suspend fun clearClubRecords()

    // Trophies / Player scoped trophies
    @Query("SELECT COUNT(*) FROM trophies WHERE playerName = :playerName AND generation = :generation AND clubName = :clubName")
    suspend fun getPlayerTrophiesCountForClub(playerName: String, generation: Int, clubName: String): Int

    @Query("SELECT * FROM trophies WHERE clubName = :clubName ORDER BY seasonYear DESC")
    fun getTrophiesByClubFlow(clubName: String): Flow<List<TrophyEntity>>

    @Query("SELECT * FROM player_club_stints WHERE playerName = :playerName AND generation = :generation AND clubId = :clubId LIMIT 1")
    suspend fun getPlayerClubStint(playerName: String, generation: Int, clubId: Int): PlayerClubStintEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlayerClubStint(stint: PlayerClubStintEntity)

    // Youth Academies
    @Query("SELECT * FROM youth_academies ORDER BY id ASC")
    fun getAllYouthAcademiesFlow(): Flow<List<YouthAcademyEntity>>

    @Query("SELECT * FROM youth_academies ORDER BY id ASC")
    suspend fun getAllYouthAcademiesSync(): List<YouthAcademyEntity>

    @Query("SELECT * FROM youth_academies WHERE id = :id LIMIT 1")
    suspend fun getYouthAcademyById(id: Int): YouthAcademyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertYouthAcademies(academies: List<YouthAcademyEntity>)

    @Query("DELETE FROM youth_academies")
    suspend fun clearYouthAcademies()

    // Youth Standings
    @Query("SELECT * FROM youth_standings ORDER BY points DESC, (goalsFor - goalsAgainst) DESC, goalsFor DESC")
    fun getAllYouthStandingsFlow(): Flow<List<YouthStandingEntity>>

    @Query("SELECT * FROM youth_standings ORDER BY points DESC")
    suspend fun getAllYouthStandingsSync(): List<YouthStandingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertYouthStandings(standings: List<YouthStandingEntity>)

    @Update
    suspend fun updateYouthStandings(standings: List<YouthStandingEntity>)

    @Query("DELETE FROM youth_standings")
    suspend fun clearYouthStandings()

    // Youth Fixtures
    @Query("SELECT * FROM youth_fixtures ORDER BY id ASC")
    fun getAllYouthFixturesFlow(): Flow<List<YouthFixtureEntity>>

    @Query("SELECT * FROM youth_fixtures WHERE monthIndex = :monthIndex ORDER BY id ASC")
    suspend fun getYouthFixturesForMonthSync(monthIndex: Int): List<YouthFixtureEntity>

    @Query("SELECT * FROM youth_fixtures")
    suspend fun getAllYouthFixturesSync(): List<YouthFixtureEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertYouthFixtures(fixtures: List<YouthFixtureEntity>)

    @Update
    suspend fun updateYouthFixture(fixture: YouthFixtureEntity)

    @Update
    suspend fun updateYouthFixtures(fixtures: List<YouthFixtureEntity>)

    @Query("DELETE FROM youth_fixtures")
    suspend fun clearYouthFixtures()

    // Street Football Games
    @Query("SELECT * FROM street_football_games WHERE seasonNumber = :seasonNumber ORDER BY id ASC")
    fun getStreetFootballGamesForSeasonFlow(seasonNumber: Int): Flow<List<StreetFootballGameEntity>>

    @Query("SELECT * FROM street_football_games WHERE seasonNumber = :seasonNumber ORDER BY id ASC")
    suspend fun getStreetFootballGamesForSeasonSync(seasonNumber: Int): List<StreetFootballGameEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreetFootballGame(game: StreetFootballGameEntity)

    @Query("DELETE FROM street_football_games WHERE seasonNumber = :seasonNumber")
    suspend fun clearStreetFootballGamesForSeason(seasonNumber: Int)

    @Query("DELETE FROM street_football_games")
    suspend fun clearStreetFootballGames()

    // Season Records
    @Insert
    suspend fun insertPlayerSeasonRecord(record: PlayerSeasonRecordEntity)

    @Query("SELECT * FROM player_season_records WHERE generation = :generation ORDER BY seasonNumber ASC")
    suspend fun getSeasonRecordsForGeneration(generation: Int): List<PlayerSeasonRecordEntity>

    @Query("SELECT * FROM player_season_records ORDER BY generation ASC, seasonNumber ASC")
    fun getAllSeasonRecordsFlow(): Flow<List<PlayerSeasonRecordEntity>>

    // Social Posts
    @Query("SELECT * FROM social_posts ORDER BY sequenceIndex DESC")
    fun getAllSocialPostsFlow(): Flow<List<SocialPostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSocialPost(post: SocialPostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSocialPosts(posts: List<SocialPostEntity>)

    @Update
    suspend fun updateSocialPost(post: SocialPostEntity)

    @Query("SELECT MAX(sequenceIndex) FROM social_posts")
    suspend fun getMaxSocialPostSequenceIndex(): Int?

    @Query("SELECT COUNT(*) FROM social_posts WHERE postType = 'FUN_FACT'")
    suspend fun getFunFactPostCount(): Int

    @Query("DELETE FROM social_posts WHERE id NOT IN (SELECT id FROM social_posts ORDER BY sequenceIndex DESC LIMIT 30)")
    suspend fun pruneOldSocialPosts()

    @Query("DELETE FROM social_posts")
    suspend fun clearSocialPosts()

    // NPC Strikers
    @Query("SELECT * FROM npc_strikers")
    suspend fun getAllNpcStrikersSync(): List<NpcStrikerEntity>

    @Query("SELECT * FROM npc_strikers WHERE currentClubId = :clubId AND isRetired = 0 LIMIT 1")
    suspend fun getActiveStrikerForClub(clubId: Int): NpcStrikerEntity?

    @Insert
    suspend fun insertNpcStriker(striker: NpcStrikerEntity): Long

    @Insert
    suspend fun insertNpcStrikers(strikers: List<NpcStrikerEntity>)

    @Update
    suspend fun updateNpcStriker(striker: NpcStrikerEntity)

    @Update
    suspend fun updateNpcStrikers(strikers: List<NpcStrikerEntity>)

    @Query("DELETE FROM npc_strikers")
    suspend fun clearNpcStrikers()

    // NPC Managers
    @Query("SELECT * FROM npc_managers")
    suspend fun getAllNpcManagersSync(): List<NpcManagerEntity>

    @Query("SELECT * FROM npc_managers WHERE currentClubId = :clubId AND isRetired = 0 LIMIT 1")
    suspend fun getActiveManagerForClub(clubId: Int): NpcManagerEntity?

    @Insert
    suspend fun insertNpcManager(manager: NpcManagerEntity): Long

    @Insert
    suspend fun insertNpcManagers(managers: List<NpcManagerEntity>)

    @Update
    suspend fun updateNpcManager(manager: NpcManagerEntity)

    @Update
    suspend fun updateNpcManagers(managers: List<NpcManagerEntity>)

    @Query("DELETE FROM npc_managers")
    suspend fun clearNpcManagers()
}

@Database(
    entities = [
        ClubEntity::class,
        PlayerEntity::class,
        StandingEntity::class,
        FixtureEntity::class,
        TrophyEntity::class,
        LegacyEntity::class,
        GameStateEntity::class,
        UsedNameEntity::class,
        ClubSeasonHistoryEntity::class,
        ClubRecordEntity::class,
        PlayerClubStintEntity::class,
        YouthAcademyEntity::class,
        YouthStandingEntity::class,
        YouthFixtureEntity::class,
        StreetFootballGameEntity::class,
        PlayerSeasonRecordEntity::class,
        SocialPostEntity::class,
        NpcStrikerEntity::class,
        NpcManagerEntity::class
    ],
    version = 25,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun careerDao(): CareerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE players ADD COLUMN fatigue INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Check if the column already exists (pre-existing schema vs new migration)
                try {
                    db.execSQL("ALTER TABLE fixtures ADD COLUMN playerRating REAL")
                } catch (e: Exception) {
                    // Ignored if column already exists
                }
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE players ADD COLUMN overtrainingRisk INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE players ADD COLUMN hasTrainedThisMonth INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE players ADD COLUMN finishingTrainingBonus REAL NOT NULL DEFAULT 0.0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE players ADD COLUMN paceTrainingBonus REAL NOT NULL DEFAULT 0.0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE players ADD COLUMN passingTrainingBonus REAL NOT NULL DEFAULT 0.0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE players ADD COLUMN physicalTrainingBonus REAL NOT NULL DEFAULT 0.0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE players ADD COLUMN techniqueTrainingBonus REAL NOT NULL DEFAULT 0.0")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE players ADD COLUMN morale INTEGER NOT NULL DEFAULT 50")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE players ADD COLUMN fanReputation INTEGER NOT NULL DEFAULT 50")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE players ADD COLUMN managerTrust INTEGER NOT NULL DEFAULT 50")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE players ADD COLUMN rivalRelationship INTEGER NOT NULL DEFAULT 50")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE players ADD COLUMN activePromiseGoalsAssists INTEGER DEFAULT NULL")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE players ADD COLUMN activePromiseGamesRemaining INTEGER DEFAULT NULL")
                } catch (e: Exception) {}
                
                val gameStateAlters = listOf(
                    "ALTER TABLE game_state ADD COLUMN activeChoiceOption3 TEXT DEFAULT NULL",
                    "ALTER TABLE game_state ADD COLUMN activeChoiceFormMod3 INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE game_state ADD COLUMN activeChoiceFinishingMod3 INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE game_state ADD COLUMN activeChoiceTechniqueMod3 INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE game_state ADD COLUMN activeChoiceOutcome3 TEXT DEFAULT NULL",
                    
                    "ALTER TABLE game_state ADD COLUMN activeChoiceMoraleMod1 INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE game_state ADD COLUMN activeChoiceFanRepMod1 INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE game_state ADD COLUMN activeChoiceManagerTrustMod1 INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE game_state ADD COLUMN activeChoiceRivalRelMod1 INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE game_state ADD COLUMN activeChoiceFatigueMod1 INTEGER NOT NULL DEFAULT 0",
                    
                    "ALTER TABLE game_state ADD COLUMN activeChoiceMoraleMod2 INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE game_state ADD COLUMN activeChoiceFanRepMod2 INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE game_state ADD COLUMN activeChoiceManagerTrustMod2 INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE game_state ADD COLUMN activeChoiceRivalRelMod2 INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE game_state ADD COLUMN activeChoiceFatigueMod2 INTEGER NOT NULL DEFAULT 0",
                    
                    "ALTER TABLE game_state ADD COLUMN activeChoiceMoraleMod3 INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE game_state ADD COLUMN activeChoiceFanRepMod3 INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE game_state ADD COLUMN activeChoiceManagerTrustMod3 INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE game_state ADD COLUMN activeChoiceRivalRelMod3 INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE game_state ADD COLUMN activeChoiceFatigueMod3 INTEGER NOT NULL DEFAULT 0"
                )
                for (alter in gameStateAlters) {
                    try {
                        db.execSQL(alter)
                    } catch (e: Exception) {}
                }
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE players ADD COLUMN hasTransferredThisWindow INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE game_state ADD COLUMN persistedTransferOffers TEXT DEFAULT NULL")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE game_state ADD COLUMN managerTalkCooldownMonths INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE fixtures ADD COLUMN wentToExtraTime INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE fixtures ADD COLUMN homePens INTEGER DEFAULT NULL")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE fixtures ADD COLUMN awayPens INTEGER DEFAULT NULL")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE fixtures ADD COLUMN goalMinutes TEXT DEFAULT NULL")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE fixtures ADD COLUMN assistMinutes TEXT DEFAULT NULL")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // UsedNameEntity
                db.execSQL("CREATE TABLE IF NOT EXISTS used_names (name TEXT NOT NULL, PRIMARY KEY(name))")
                
                // ClubEntity modifications
                try {
                    db.execSQL("ALTER TABLE clubs ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE clubs ADD COLUMN foundedSeasonsAgo INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
                
                // ClubSeasonHistoryEntity
                db.execSQL("CREATE TABLE IF NOT EXISTS club_season_history (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, seasonNumber INTEGER NOT NULL, country TEXT NOT NULL, clubId INTEGER NOT NULL, leagueFinishPosition INTEGER NOT NULL, europeanRank TEXT NOT NULL, superCupResult TEXT NOT NULL)")
                
                // PlayerEntity modifications
                try {
                    db.execSQL("ALTER TABLE players ADD COLUMN clubGamesPlayed INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE players ADD COLUMN clubGoals INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE players ADD COLUMN clubAssists INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
                
                // ClubRecordEntity
                db.execSQL("CREATE TABLE IF NOT EXISTS club_records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, clubId INTEGER NOT NULL, category TEXT NOT NULL, holderName TEXT NOT NULL, statValue INTEGER NOT NULL, isActive INTEGER NOT NULL, yearsActive TEXT NOT NULL, isUserPlayer INTEGER NOT NULL, generation INTEGER, previousHolderName TEXT, previousStatValue INTEGER, previousYearsActive TEXT)")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS player_club_stints (playerName TEXT NOT NULL, generation INTEGER NOT NULL, clubId INTEGER NOT NULL, goals INTEGER NOT NULL, assists INTEGER NOT NULL, gamesPlayed INTEGER NOT NULL, PRIMARY KEY(playerName, generation, clubId))")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE fixtures ADD COLUMN playerSubbedOffMinute INTEGER DEFAULT NULL")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS youth_academies (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, parentClubId INTEGER NOT NULL, academyName TEXT NOT NULL, country TEXT NOT NULL, parentReputation TEXT NOT NULL, parentReputationPoints INTEGER NOT NULL, minScoutOvr INTEGER NOT NULL, youthRivalName TEXT NOT NULL, youthRivalOvr INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS youth_standings (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, academyId INTEGER NOT NULL, played INTEGER NOT NULL DEFAULT 0, wins INTEGER NOT NULL DEFAULT 0, draws INTEGER NOT NULL DEFAULT 0, losses INTEGER NOT NULL DEFAULT 0, goalsFor INTEGER NOT NULL DEFAULT 0, goalsAgainst INTEGER NOT NULL DEFAULT 0, points INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("CREATE TABLE IF NOT EXISTS youth_fixtures (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, monthIndex INTEGER NOT NULL, homeAcademyId INTEGER NOT NULL, awayAcademyId INTEGER NOT NULL, homeScore INTEGER DEFAULT NULL, awayScore INTEGER DEFAULT NULL, isSimulated INTEGER NOT NULL DEFAULT 0, playerGoals INTEGER NOT NULL DEFAULT 0, playerAssists INTEGER NOT NULL DEFAULT 0, playerMvp INTEGER NOT NULL DEFAULT 0, playerCameOnMinute INTEGER DEFAULT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS street_football_games (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, seasonNumber INTEGER NOT NULL, monthIndex INTEGER NOT NULL, opponentName TEXT NOT NULL, playerGoals INTEGER NOT NULL DEFAULT 0, playerAssists INTEGER NOT NULL DEFAULT 0, playerMvp INTEGER NOT NULL DEFAULT 0, resultSummary TEXT NOT NULL DEFAULT '')")

                val playerCols = listOf(
                    "ALTER TABLE players ADD COLUMN careerPhase TEXT NOT NULL DEFAULT 'STREET_FOOTBALL'",
                    "ALTER TABLE players ADD COLUMN currentAcademyId INTEGER DEFAULT NULL",
                    "ALTER TABLE players ADD COLUMN rejectedAcademyIds TEXT NOT NULL DEFAULT ''",
                    "ALTER TABLE players ADD COLUMN rejectedSeniorClubIds TEXT NOT NULL DEFAULT ''",
                    "ALTER TABLE players ADD COLUMN youthGoals INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE players ADD COLUMN youthAssists INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE players ADD COLUMN youthMvps INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE players ADD COLUMN youthGamesPlayed INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE players ADD COLUMN shootingDrillsCompleted INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE players ADD COLUMN passingDrillsCompleted INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE players ADD COLUMN paceDrillsCompleted INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE players ADD COLUMN technicalDrillsCompleted INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE players ADD COLUMN physicalDrillsCompleted INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE players ADD COLUMN assignedPlaystyle TEXT DEFAULT NULL",
                    "ALTER TABLE players ADD COLUMN assignedWeakness TEXT DEFAULT NULL",
                    "ALTER TABLE players ADD COLUMN playstyleAssignedAtAge INTEGER DEFAULT NULL",
                    "ALTER TABLE players ADD COLUMN streetFootballGamesThisSeason INTEGER NOT NULL DEFAULT 0"
                )
                for (colSql in playerCols) {
                    try { db.execSQL(colSql) } catch (e: Exception) {}
                }

                val gameCols = listOf(
                    "ALTER TABLE game_state ADD COLUMN persistedYouthOffers TEXT DEFAULT NULL",
                    "ALTER TABLE game_state ADD COLUMN persistedSeniorYouthOffers TEXT DEFAULT NULL",
                    "ALTER TABLE game_state ADD COLUMN youthCareerEnded INTEGER NOT NULL DEFAULT 0"
                )
                for (colSql in gameCols) {
                    try { db.execSQL(colSql) } catch (e: Exception) {}
                }
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val playerCols = listOf(
                    "ALTER TABLE players ADD COLUMN preferredFoot TEXT NOT NULL DEFAULT 'Right'",
                    "ALTER TABLE players ADD COLUMN squadNumber INTEGER NOT NULL DEFAULT 9",
                    "ALTER TABLE players ADD COLUMN backgroundStory TEXT NOT NULL DEFAULT 'Street Cages'"
                )
                for (colSql in playerCols) {
                    try { db.execSQL(colSql) } catch (e: Exception) {}
                }
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE players ADD COLUMN potentialCeiling INTEGER NOT NULL DEFAULT 99")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE game_state ADD COLUMN recentChoiceEventIds TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try { db.execSQL("CREATE INDEX IF NOT EXISTS index_fixtures_monthIndex ON fixtures(monthIndex)") } catch (e: Exception) {}
                try { db.execSQL("CREATE INDEX IF NOT EXISTS index_standings_country ON standings(country)") } catch (e: Exception) {}
                try { db.execSQL("CREATE INDEX IF NOT EXISTS index_youth_fixtures_monthIndex ON youth_fixtures(monthIndex)") } catch (e: Exception) {}
                try { db.execSQL("CREATE INDEX IF NOT EXISTS index_club_season_history_clubId ON club_season_history(clubId)") } catch (e: Exception) {}
                try { db.execSQL("CREATE INDEX IF NOT EXISTS index_club_records_clubId ON club_records(clubId)") } catch (e: Exception) {}
                try { db.execSQL("CREATE INDEX IF NOT EXISTS index_trophies_playerName_generation_clubName ON trophies(playerName, generation, clubName)") } catch (e: Exception) {}
                try { db.execSQL("CREATE INDEX IF NOT EXISTS index_trophies_clubName ON trophies(clubName)") } catch (e: Exception) {}
                try { db.execSQL("CREATE INDEX IF NOT EXISTS index_player_club_stints_playerName_generation ON player_club_stints(playerName, generation)") } catch (e: Exception) {}
                try { db.execSQL("CREATE INDEX IF NOT EXISTS index_street_football_games_seasonNumber ON street_football_games(seasonNumber)") } catch (e: Exception) {}
                try { db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_used_names_name ON used_names(name)") } catch (e: Exception) {}
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `player_season_records` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `playerName` TEXT NOT NULL,
                        `generation` INTEGER NOT NULL,
                        `seasonNumber` INTEGER NOT NULL,
                        `clubName` TEXT NOT NULL,
                        `matchesPlayed` INTEGER NOT NULL,
                        `goals` INTEGER NOT NULL,
                        `assists` INTEGER NOT NULL,
                        `ovrAtSeasonEnd` INTEGER NOT NULL
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_player_season_records_playerName_generation` ON `player_season_records` (`playerName`, `generation`)")
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE player_season_records ADD COLUMN playerAge INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE legacies ADD COLUMN retirementDescription TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS social_posts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sequenceIndex INTEGER NOT NULL,
                        seasonNumber INTEGER NOT NULL,
                        monthIndex INTEGER NOT NULL,
                        postType TEXT NOT NULL,
                        authorName TEXT NOT NULL,
                        authorHandle TEXT NOT NULL,
                        authorInitials TEXT NOT NULL,
                        content TEXT NOT NULL,
                        isAboutPlayerOrClub INTEGER NOT NULL,
                        relatedClubId INTEGER,
                        likeCount INTEGER NOT NULL,
                        isReplyable INTEGER NOT NULL DEFAULT 0,
                        hasReplied INTEGER NOT NULL DEFAULT 0,
                        selectedReplyIndex INTEGER,
                        reply1Text TEXT, reply1MoraleMod INTEGER NOT NULL DEFAULT 0, reply1FanRepMod INTEGER NOT NULL DEFAULT 0, reply1ManagerTrustMod INTEGER NOT NULL DEFAULT 0, reply1RivalRelMod INTEGER NOT NULL DEFAULT 0,
                        reply2Text TEXT, reply2MoraleMod INTEGER NOT NULL DEFAULT 0, reply2FanRepMod INTEGER NOT NULL DEFAULT 0, reply2ManagerTrustMod INTEGER NOT NULL DEFAULT 0, reply2RivalRelMod INTEGER NOT NULL DEFAULT 0,
                        reply3Text TEXT, reply3MoraleMod INTEGER NOT NULL DEFAULT 0, reply3FanRepMod INTEGER NOT NULL DEFAULT 0, reply3ManagerTrustMod INTEGER NOT NULL DEFAULT 0, reply3RivalRelMod INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_social_posts_sequenceIndex ON social_posts(sequenceIndex)")
            }
        }

        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE clubs ADD COLUMN momentumBias REAL NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE clubs ADD COLUMN trajectorySeasonsRemaining INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS npc_strikers (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            name TEXT NOT NULL,
                            country TEXT NOT NULL,
                            currentClubId INTEGER,
                            age INTEGER NOT NULL,
                            ovr INTEGER NOT NULL,
                            potentialCeiling INTEGER NOT NULL,
                            seasonsAtCurrentClub INTEGER NOT NULL DEFAULT 0,
                            isRetired INTEGER NOT NULL DEFAULT 0,
                            isRisingTalent INTEGER NOT NULL DEFAULT 0
                        )
                    """)
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_npc_strikers_currentClubId ON npc_strikers(currentClubId)")
                } catch (e: Exception) {}

                try {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS npc_managers (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            name TEXT NOT NULL,
                            country TEXT NOT NULL,
                            currentClubId INTEGER,
                            reputationTier TEXT NOT NULL,
                            seasonsAtCurrentClub INTEGER NOT NULL DEFAULT 0,
                            seasonsAsManager INTEGER NOT NULL DEFAULT 0,
                            isRetired INTEGER NOT NULL DEFAULT 0
                        )
                    """)
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_npc_managers_currentClubId ON npc_managers(currentClubId)")
                } catch (e: Exception) {}

                try {
                    db.execSQL("ALTER TABLE clubs ADD COLUMN managerName TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {}

                try {
                    db.execSQL("ALTER TABLE clubs ADD COLUMN managerReputationTier TEXT NOT NULL DEFAULT 'MID'")
                } catch (e: Exception) {}

                try {
                    db.execSQL("ALTER TABLE clubs ADD COLUMN managerId INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}

                try {
                    db.execSQL("ALTER TABLE players ADD COLUMN pendingNewManagerNotice INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE players ADD COLUMN isGodMode INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE players ADD COLUMN lastGoalMilestonePosted INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {}
            }
        }

        private val instances = mutableMapOf<Int, AppDatabase>()

        fun getDatabase(context: Context, slotId: Int = 1): AppDatabase {
            SaveSlotManager(context).migrateLegacyDatabaseIfNeeded(context)
            return synchronized(this) {
                instances.getOrPut(slotId) {
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "apex_career_slot_$slotId.db"
                    )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25)
                    .fallbackToDestructiveMigration()
                    .build()
                }
            }
        }
    }
}
