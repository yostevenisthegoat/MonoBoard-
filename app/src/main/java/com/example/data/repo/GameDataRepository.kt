package com.example.data.repo

import com.example.data.db.GameStatDao
import com.example.data.db.GameStatEntity
import com.example.data.db.MatchHistoryDao
import com.example.data.db.MatchHistoryEntity
import com.example.data.db.MonoBoardDatabase
import com.example.data.db.PlayerProfileDao
import com.example.data.db.PlayerProfileEntity
import com.example.data.db.SavedGameDao
import com.example.data.db.SavedGameEntity
import com.example.domain.AvatarSymbol
import com.example.domain.GameCatalog
import kotlinx.coroutines.flow.Flow

enum class MatchOutcome {
    WIN, LOSS, DRAW, COMPLETE
}

class GameDataRepository(db: MonoBoardDatabase) {
    private val statDao: GameStatDao = db.gameStatDao()
    private val savedDao: SavedGameDao = db.savedGameDao()
    private val historyDao: MatchHistoryDao = db.matchHistoryDao()
    private val profileDao: PlayerProfileDao = db.playerProfileDao()

    val allStats: Flow<List<GameStatEntity>> = statDao.observeAllStats()
    val favorites: Flow<List<GameStatEntity>> = statDao.observeFavorites()
    val recentlyPlayed: Flow<List<GameStatEntity>> = statDao.observeRecentlyPlayed()
    val savedGames: Flow<List<SavedGameEntity>> = savedDao.observeSavedGames()
    val matchHistory: Flow<List<MatchHistoryEntity>> = historyDao.observeHistory()
    val playerProfiles: Flow<List<PlayerProfileEntity>> = profileDao.observeProfiles()

    suspend fun toggleFavorite(gameId: String) {
        val current = statDao.getStat(gameId)
        if (current == null) {
            statDao.upsertStat(GameStatEntity(gameId = gameId, isFavorite = true))
        } else {
            statDao.setFavorite(gameId, !current.isFavorite)
        }
    }

    suspend fun recordMatchResult(
        gameId: String,
        modeLabel: String,
        outcome: MatchOutcome,
        winnerName: String,
        summary: String,
        score: Int = 0,
        durationSeconds: Int = 0
    ) {
        val now = System.currentTimeMillis()
        val existing = statDao.getStat(gameId) ?: GameStatEntity(gameId = gameId)
        val bestScore = maxOf(existing.bestScore, score)
        val bestTime = when {
            durationSeconds <= 0 -> existing.bestTimeSeconds
            outcome != MatchOutcome.WIN && outcome != MatchOutcome.COMPLETE -> existing.bestTimeSeconds
            existing.bestTimeSeconds == 0 -> durationSeconds
            else -> minOf(existing.bestTimeSeconds, durationSeconds)
        }
        val updated = existing.copy(
            gamesPlayed = existing.gamesPlayed + 1,
            wins = existing.wins + if (outcome == MatchOutcome.WIN || outcome == MatchOutcome.COMPLETE) 1 else 0,
            losses = existing.losses + if (outcome == MatchOutcome.LOSS) 1 else 0,
            draws = existing.draws + if (outcome == MatchOutcome.DRAW) 1 else 0,
            bestScore = bestScore,
            bestTimeSeconds = bestTime,
            lastPlayedAt = now
        )
        statDao.upsertStat(updated)

        val def = GameCatalog.byId(gameId)
        historyDao.insertMatch(
            MatchHistoryEntity(
                gameId = gameId,
                gameTitle = def.title,
                modeLabel = modeLabel,
                winnerName = winnerName,
                outcome = outcome.name,
                summary = summary,
                durationSeconds = durationSeconds,
                playedAt = now
            )
        )
    }

    suspend fun touchRecentlyPlayed(gameId: String) {
        val existing = statDao.getStat(gameId) ?: GameStatEntity(gameId = gameId)
        statDao.upsertStat(existing.copy(lastPlayedAt = System.currentTimeMillis()))
    }

    suspend fun saveGameState(
        gameId: String,
        modeLabel: String,
        playersSummary: String,
        serializedState: String
    ) {
        val def = GameCatalog.byId(gameId)
        savedDao.upsertSavedGame(
            SavedGameEntity(
                gameId = gameId,
                title = def.title,
                modeLabel = modeLabel,
                playersSummary = playersSummary,
                serializedState = serializedState,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun loadSavedGame(gameId: String): SavedGameEntity? = savedDao.getSavedGame(gameId)

    suspend fun clearSavedGame(gameId: String) = savedDao.deleteSavedGame(gameId)

    suspend fun addPlayerProfile(name: String, avatar: AvatarSymbol) {
        val trimmed = name.trim().take(20)
        if (trimmed.isNotEmpty()) {
            profileDao.upsertProfile(
                PlayerProfileEntity(
                    name = trimmed,
                    avatarName = avatar.name
                )
            )
        }
    }

    suspend fun removePlayerProfile(id: Long) {
        profileDao.deleteProfile(id)
    }

    suspend fun resetStatistics() {
        statDao.clearAllStats()
        historyDao.clearHistory()
    }

    suspend fun resetAllLocalData() {
        statDao.clearAllStats()
        savedDao.clearAllSavedGames()
        historyDao.clearHistory()
        profileDao.clearProfiles()
    }
}
