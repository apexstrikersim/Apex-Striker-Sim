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

    @Query("SELECT * FROM fixtures WHERE competition = 'LEAGUE' AND isSimulated = 1")
    suspend fun getSeasonLeagueFixturesSync(): List<FixtureEntity>

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

    @Query("SELECT * FROM trophies WHERE playerName = :playerName AND generation = :generation AND seasonYear = :seasonYear AND competitionName = :competitionName LIMIT 1")
    suspend fun findTrophy(playerName: String, generation: Int, seasonYear: Int, competitionName: String): TrophyEntity?

    @Query("DELETE FROM trophies")
    suspend fun clearTrophies()

    // Legacies (Family lineage)
    @Query("SELECT * FROM legacies ORDER BY generation ASC")
    fun getAllLegaciesFlow(): Flow<List<LegacyEntity>>

    @Query("SELECT * FROM legacies ORDER BY generation ASC")
    suspend fun getAllLegaciesSync(): List<LegacyEntity>

    @Query("SELECT * FROM legacies WHERE generation = :generation LIMIT 1")
    suspend fun getLegacyByGeneration(generation: Int): LegacyEntity?

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

    @Query("SELECT * FROM club_season_history WHERE seasonNumber >= :minSeason AND leagueFinishPosition = 1")
    suspend fun getRecentLeagueWinnersSync(minSeason: Int): List<ClubSeasonHistoryEntity>

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

    // Nation Call-Up State
    @Query("SELECT * FROM nation_call_up_state WHERE playerName = :playerName AND generation = :generation")
    suspend fun getCallUpStateForPlayer(playerName: String, generation: Int): List<NationCallUpState>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCallUpState(state: NationCallUpState)

    // Nation Ranking State
    @Query("SELECT * FROM nation_ranking_state WHERE nationCode IN (:codes)")
    suspend fun getRankingStateForCodes(codes: List<String>): List<NationRankingState>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRankingState(state: NationRankingState)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRankingStates(states: List<NationRankingState>)

    @Query("SELECT * FROM nation_ranking_state ORDER BY currentRankingPoints DESC")
    suspend fun getAllRankingStateSync(): List<NationRankingState>
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
        NpcManagerEntity::class,
        NationCallUpState::class,
        NationRankingState::class
    ],
    version = AppVersion.CURRENT,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun careerDao(): CareerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val instances = mutableMapOf<Int, AppDatabase>()

        fun getDatabase(context: Context, slotId: Int = 1): AppDatabase {
            return synchronized(this) {
                instances[slotId]?.let { return it }

                SaveSlotManager(context).migrateLegacyDatabaseIfNeeded(context)

                val db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "apex_career_slot_$slotId.db"
                )
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .fallbackToDestructiveMigration()
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()

                instances[slotId] = db
                db
            }
        }

        fun closeDatabase(slotId: Int) {
            synchronized(this) {
                try {
                    instances.remove(slotId)?.close()
                } catch (_: Exception) {}
            }
        }
    }
}
