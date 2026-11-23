package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_stats")
data class GameStatEntity(
    @PrimaryKey val gameId: String,
    val gamesPlayed: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val bestScore: Int = 0,
    val bestTimeSeconds: Int = 0, // 0 means not set
    val lastPlayedAt: Long = 0L,
    val isFavorite: Boolean = false
)

@Entity(tableName = "saved_games")
data class SavedGameEntity(
    @PrimaryKey val gameId: String,
    val title: String,
    val modeLabel: String,
    val playersSummary: String,
    val serializedState: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "match_history")
data class MatchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: String,
    val gameTitle: String,
    val modeLabel: String,
    val winnerName: String,
    val outcome: String, // WIN, LOSS, DRAW, COMPLETE
    val summary: String,
    val durationSeconds: Int,
    val playedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "player_profiles")
data class PlayerProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val avatarName: String,
    val gamesPlayed: Int = 0,
    val wins: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
