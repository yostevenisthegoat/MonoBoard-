package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.GameStatEntity
import com.example.data.db.MatchHistoryEntity
import com.example.data.db.MonoBoardDatabase
import com.example.data.db.PlayerProfileEntity
import com.example.data.db.SavedGameEntity
import com.example.data.prefs.AppSettings
import com.example.data.prefs.SettingsRepository
import com.example.data.repo.GameDataRepository
import com.example.data.repo.MatchOutcome
import com.example.data.sound.SoundAndHapticsManager
import com.example.data.sound.SoundCue
import com.example.domain.AiDifficulty
import com.example.domain.AvatarSymbol
import com.example.domain.GameCatalog
import com.example.domain.GameDefinition
import com.example.domain.GameSessionConfig
import com.example.domain.PlayMode
import com.example.domain.PlayerSlotConfig
import com.example.ui.theme.BoardTheme
import com.example.ui.theme.PieceTheme
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ActiveGameSession(
    val config: GameSessionConfig,
    val initialSerializedState: String? = null
)

data class PendingSetupRequest(
    val game: GameDefinition,
    val preferredMode: PlayMode? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = MonoBoardDatabase.getInstance(application)
    private val repo = GameDataRepository(db)
    private val prefs = SettingsRepository(application)
    private val soundManager = SoundAndHapticsManager(application)

    val settings: StateFlow<AppSettings> = prefs.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val allStats: StateFlow<List<GameStatEntity>> = repo.allStats
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val savedGames: StateFlow<List<SavedGameEntity>> = repo.savedGames
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val matchHistory: StateFlow<List<MatchHistoryEntity>> = repo.matchHistory
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val playerProfiles: StateFlow<List<PlayerProfileEntity>> = repo.playerProfiles
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _pendingSetup = MutableStateFlow<PendingSetupRequest?>(null)
    val pendingSetup: StateFlow<PendingSetupRequest?> = _pendingSetup.asStateFlow()

    private val _activeSession = MutableStateFlow<ActiveGameSession?>(null)
    val activeSession: StateFlow<ActiveGameSession?> = _activeSession.asStateFlow()

    init {
        viewModelScope.launch {
            settings.collect { s ->
                soundManager.soundEnabled = s.soundEffects
                soundManager.hapticsEnabled = s.vibration
                soundManager.updateAmbientMusic(s.backgroundMusic)
            }
        }
    }

    fun openGameSetup(gameId: String, preferredMode: PlayMode? = null) {
        val game = GameCatalog.byId(gameId)
        _pendingSetup.value = PendingSetupRequest(game, preferredMode)
    }

    fun dismissSetup() {
        _pendingSetup.value = null
    }

    fun launchSession(config: GameSessionConfig, initialSerializedState: String? = null) {
        _pendingSetup.value = null
        _activeSession.value = ActiveGameSession(config, initialSerializedState)
        viewModelScope.launch {
            repo.touchRecentlyPlayed(config.gameId)
        }
    }

    fun resumeSavedGame(saved: SavedGameEntity) {
        val game = GameCatalog.byId(saved.gameId)
        val isAi = saved.modeLabel.contains("Computer", ignoreCase = true) ||
            saved.modeLabel.contains("AI", ignoreCase = true)
        val mode = when {
            isAi && game.supportsAi -> PlayMode.VS_AI
            game.supportsPassAndPlay -> PlayMode.PASS_AND_PLAY
            else -> PlayMode.SOLO
        }
        val names = saved.playersSummary.split(" vs ").filter { it.isNotBlank() }
        val players = if (names.size >= 2) {
            names.mapIndexed { idx, name ->
                PlayerSlotConfig(
                    index = idx,
                    name = name.trim(),
                    avatar = if (isAi && idx > 0) AvatarSymbol.BOT else AvatarSymbol.entries[idx % 4],
                    isAi = isAi && idx > 0,
                    aiDifficulty = AiDifficulty.MEDIUM
                )
            }
        } else {
            listOf(
                PlayerSlotConfig(0, "Player 1", AvatarSymbol.CROWN, isAi = false),
                PlayerSlotConfig(1, if (isAi) "Mono AI" else "Player 2", if (isAi) AvatarSymbol.BOT else AvatarSymbol.SHIELD, isAi = isAi)
            )
        }
        val config = GameSessionConfig(
            gameId = saved.gameId,
            playMode = mode,
            players = players,
            aiDifficulty = AiDifficulty.MEDIUM,
            privacyMode = game.requiresPrivacyByDefault && settings.value.privacyModeDefault
        )
        _pendingSetup.value = null
        _activeSession.value = ActiveGameSession(config, saved.serializedState)
    }

    fun closeActiveSession() {
        _activeSession.value = null
    }

    fun toggleFavorite(gameId: String) {
        viewModelScope.launch { repo.toggleFavorite(gameId) }
    }

    fun recordOutcome(
        gameId: String,
        modeLabel: String,
        outcome: MatchOutcome,
        winnerName: String,
        summary: String,
        score: Int,
        durationSeconds: Int
    ) {
        viewModelScope.launch {
            repo.recordMatchResult(
                gameId = gameId,
                modeLabel = modeLabel,
                outcome = outcome,
                winnerName = winnerName,
                summary = summary,
                score = score,
                durationSeconds = durationSeconds
            )
        }
    }

    fun saveGameState(
        gameId: String,
        modeLabel: String,
        playersSummary: String,
        serializedState: String
    ) {
        viewModelScope.launch {
            repo.saveGameState(gameId, modeLabel, playersSummary, serializedState)
        }
    }

    fun clearSavedGame(gameId: String) {
        viewModelScope.launch { repo.clearSavedGame(gameId) }
    }

    fun addPlayerProfile(name: String, avatar: AvatarSymbol) {
        viewModelScope.launch { repo.addPlayerProfile(name, avatar) }
    }

    fun removePlayerProfile(id: Long) {
        viewModelScope.launch { repo.removePlayerProfile(id) }
    }

    fun playCue(cue: SoundCue) {
        soundManager.playCue(cue)
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { prefs.setThemeMode(mode) }
    fun setSoundEffects(enabled: Boolean) = viewModelScope.launch { prefs.setSoundEffects(enabled) }
    fun setBackgroundMusic(enabled: Boolean) = viewModelScope.launch { prefs.setBackgroundMusic(enabled) }
    fun setVibration(enabled: Boolean) = viewModelScope.launch { prefs.setVibration(enabled) }
    fun setLanguage(lang: String) = viewModelScope.launch { prefs.setLanguage(lang) }
    fun setPrivacyModeDefault(enabled: Boolean) = viewModelScope.launch { prefs.setPrivacyModeDefault(enabled) }
    fun setBoardTheme(theme: BoardTheme) = viewModelScope.launch { prefs.setBoardTheme(theme) }
    fun setPieceTheme(theme: PieceTheme) = viewModelScope.launch { prefs.setPieceTheme(theme) }
    fun setShowLegalMoves(enabled: Boolean) = viewModelScope.launch { prefs.setShowLegalMoves(enabled) }
    fun setConfirmTurnPass(enabled: Boolean) = viewModelScope.launch { prefs.setConfirmTurnPass(enabled) }

    fun resetStatistics() = viewModelScope.launch { repo.resetStatistics() }
    fun resetAllLocalData() = viewModelScope.launch {
        repo.resetAllLocalData()
        prefs.resetAllPreferences()
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.updateAmbientMusic(false)
    }
}
