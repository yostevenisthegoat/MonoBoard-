package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameStatDao {
    @Query("SELECT * FROM game_stats")
    fun observeAllStats(): Flow<List<GameStatEntity>>

    @Query("SELECT * FROM game_stats WHERE gameId = :gameId")
    suspend fun getStat(gameId: String): GameStatEntity?

    @Query("SELECT * FROM game_stats WHERE isFavorite = 1 ORDER BY lastPlayedAt DESC")
    fun observeFavorites(): Flow<List<GameStatEntity>>

    @Query("SELECT * FROM game_stats WHERE lastPlayedAt > 0 ORDER BY lastPlayedAt DESC LIMIT 12")
    fun observeRecentlyPlayed(): Flow<List<GameStatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStat(stat: GameStatEntity)

    @Query("UPDATE game_stats SET isFavorite = :favorite WHERE gameId = :gameId")
    suspend fun setFavorite(gameId: String, favorite: Boolean)

    @Query("DELETE FROM game_stats")
    suspend fun clearAllStats()
}

@Dao
interface SavedGameDao {
    @Query("SELECT * FROM saved_games ORDER BY updatedAt DESC")
    fun observeSavedGames(): Flow<List<SavedGameEntity>>

    @Query("SELECT * FROM saved_games WHERE gameId = :gameId")
    suspend fun getSavedGame(gameId: String): SavedGameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSavedGame(saved: SavedGameEntity)

    @Query("DELETE FROM saved_games WHERE gameId = :gameId")
    suspend fun deleteSavedGame(gameId: String)

    @Query("DELETE FROM saved_games")
    suspend fun clearAllSavedGames()
}

@Dao
interface MatchHistoryDao {
    @Query("SELECT * FROM match_history ORDER BY playedAt DESC LIMIT 100")
    fun observeHistory(): Flow<List<MatchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchHistoryEntity)

    @Query("DELETE FROM match_history")
    suspend fun clearHistory()
}

@Dao
interface PlayerProfileDao {
    @Query("SELECT * FROM player_profiles ORDER BY wins DESC, name ASC")
    fun observeProfiles(): Flow<List<PlayerProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: PlayerProfileEntity)

    @Query("DELETE FROM player_profiles WHERE id = :id")
    suspend fun deleteProfile(id: Long)

    @Query("DELETE FROM player_profiles")
    suspend fun clearProfiles()
}
