package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.BoardTheme
import com.example.ui.theme.PieceTheme
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.monoSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mono_board_settings"
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val soundEffects: Boolean = true,
    val backgroundMusic: Boolean = false,
    val vibration: Boolean = true,
    val language: String = "English",
    val privacyModeDefault: Boolean = true,
    val boardTheme: BoardTheme = BoardTheme.MONO_CLASSIC,
    val pieceTheme: PieceTheme = PieceTheme.CLASSIC,
    val showLegalMoves: Boolean = true,
    val confirmTurnPass: Boolean = true
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SOUND_EFFECTS = booleanPreferencesKey("sound_effects")
        val BACKGROUND_MUSIC = booleanPreferencesKey("background_music")
        val VIBRATION = booleanPreferencesKey("vibration")
        val LANGUAGE = stringPreferencesKey("language")
        val PRIVACY_MODE_DEFAULT = booleanPreferencesKey("privacy_mode_default")
        val BOARD_THEME = stringPreferencesKey("board_theme")
        val PIECE_THEME = stringPreferencesKey("piece_theme")
        val SHOW_LEGAL_MOVES = booleanPreferencesKey("show_legal_moves")
        val CONFIRM_TURN_PASS = booleanPreferencesKey("confirm_turn_pass")
    }

    val settings: Flow<AppSettings> = context.monoSettingsDataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.DARK,
            soundEffects = prefs[Keys.SOUND_EFFECTS] ?: true,
            backgroundMusic = prefs[Keys.BACKGROUND_MUSIC] ?: false,
            vibration = prefs[Keys.VIBRATION] ?: true,
            language = prefs[Keys.LANGUAGE] ?: "English",
            privacyModeDefault = prefs[Keys.PRIVACY_MODE_DEFAULT] ?: true,
            boardTheme = prefs[Keys.BOARD_THEME]?.let { runCatching { BoardTheme.valueOf(it) }.getOrNull() } ?: BoardTheme.MONO_CLASSIC,
            pieceTheme = prefs[Keys.PIECE_THEME]?.let { runCatching { PieceTheme.valueOf(it) }.getOrNull() } ?: PieceTheme.CLASSIC,
            showLegalMoves = prefs[Keys.SHOW_LEGAL_MOVES] ?: true,
            confirmTurnPass = prefs[Keys.CONFIRM_TURN_PASS] ?: true
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.monoSettingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setSoundEffects(enabled: Boolean) {
        context.monoSettingsDataStore.edit { it[Keys.SOUND_EFFECTS] = enabled }
    }

    suspend fun setBackgroundMusic(enabled: Boolean) {
        context.monoSettingsDataStore.edit { it[Keys.BACKGROUND_MUSIC] = enabled }
    }

    suspend fun setVibration(enabled: Boolean) {
        context.monoSettingsDataStore.edit { it[Keys.VIBRATION] = enabled }
    }

    suspend fun setLanguage(language: String) {
        context.monoSettingsDataStore.edit { it[Keys.LANGUAGE] = language }
    }

    suspend fun setPrivacyModeDefault(enabled: Boolean) {
        context.monoSettingsDataStore.edit { it[Keys.PRIVACY_MODE_DEFAULT] = enabled }
    }

    suspend fun setBoardTheme(theme: BoardTheme) {
        context.monoSettingsDataStore.edit { it[Keys.BOARD_THEME] = theme.name }
    }

    suspend fun setPieceTheme(theme: PieceTheme) {
        context.monoSettingsDataStore.edit { it[Keys.PIECE_THEME] = theme.name }
    }

    suspend fun setShowLegalMoves(enabled: Boolean) {
        context.monoSettingsDataStore.edit { it[Keys.SHOW_LEGAL_MOVES] = enabled }
    }

    suspend fun setConfirmTurnPass(enabled: Boolean) {
        context.monoSettingsDataStore.edit { it[Keys.CONFIRM_TURN_PASS] = enabled }
    }

    suspend fun resetAllPreferences() {
        context.monoSettingsDataStore.edit { it.clear() }
    }
}
