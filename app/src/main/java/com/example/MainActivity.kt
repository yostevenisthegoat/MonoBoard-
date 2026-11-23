package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.GameCategory
import com.example.ui.MainViewModel
import com.example.ui.components.PassAndPlaySetupSheet
import com.example.ui.screens.GameHostScreen
import com.example.ui.screens.GamesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PassAndPlayHubScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MonoBoardTheme

enum class MonoTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Rounded.Home, Icons.Outlined.Home),
    GAMES("Games", Icons.Rounded.GridView, Icons.Outlined.GridView),
    PASS_AND_PLAY("Pass & Play", Icons.Rounded.Groups, Icons.Outlined.Groups),
    LIBRARY("Library", Icons.Rounded.Bookmarks, Icons.Outlined.Bookmarks),
    SETTINGS("Settings", Icons.Rounded.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MonoBoardApp()
        }
    }
}

@Composable
fun MonoBoardApp(viewModel: MainViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val allStats by viewModel.allStats.collectAsStateWithLifecycle()
    val savedGames by viewModel.savedGames.collectAsStateWithLifecycle()
    val matchHistory by viewModel.matchHistory.collectAsStateWithLifecycle()
    val playerProfiles by viewModel.playerProfiles.collectAsStateWithLifecycle()
    val pendingSetup by viewModel.pendingSetup.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()

    val statsMap = remember(allStats) { allStats.associateBy { it.gameId } }
    var selectedTab by rememberSaveable { mutableStateOf(MonoTab.HOME) }
    var gamesInitialCategory by remember { mutableStateOf<GameCategory?>(null) }

    val useRail = LocalConfiguration.current.screenWidthDp >= 720

    MonoBoardTheme(themeMode = settings.themeMode) {
        if (activeSession != null) {
            val session = activeSession!!
            GameHostScreen(
                session = session,
                settings = settings,
                stat = statsMap[session.config.gameId],
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onSaveState = { id, mode, players, json -> viewModel.saveGameState(id, mode, players, json) },
                onClearSaved = { viewModel.clearSavedGame(it) },
                onRecordOutcome = { id, mode, outcome, winner, summary, score, dur ->
                    viewModel.recordOutcome(id, mode, outcome, winner, summary, score, dur)
                },
                onPlayCue = { viewModel.playCue(it) },
                onBack = { viewModel.closeActiveSession() }
            )
        } else {
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            MonoTab.entries.forEach { tab ->
                                val selected = selectedTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { selectedTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label, maxLines = 1) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                            MonoTab.entries.forEach { tab ->
                                val selected = selectedTab == tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { selectedTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) }
                                )
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        when (selectedTab) {
                            MonoTab.HOME -> HomeScreen(
                                statsMap = statsMap,
                                savedGames = savedGames,
                                onOpenGame = { id, mode -> viewModel.openGameSetup(id, mode) },
                                onResumeSaved = { viewModel.resumeSavedGame(it) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onNavigateToGames = { cat ->
                                    gamesInitialCategory = cat
                                    selectedTab = MonoTab.GAMES
                                },
                                onNavigateToPassAndPlay = { selectedTab = MonoTab.PASS_AND_PLAY }
                            )
                            MonoTab.GAMES -> GamesScreen(
                                initialCategory = gamesInitialCategory,
                                statsMap = statsMap,
                                savedGames = savedGames,
                                onOpenGame = { id, mode -> viewModel.openGameSetup(id, mode) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) }
                            )
                            MonoTab.PASS_AND_PLAY -> PassAndPlayHubScreen(
                                statsMap = statsMap,
                                savedGames = savedGames,
                                playerProfiles = playerProfiles,
                                onOpenGame = { id, mode -> viewModel.openGameSetup(id, mode) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onAddProfile = { name, avatar -> viewModel.addPlayerProfile(name, avatar) },
                                onRemoveProfile = { viewModel.removePlayerProfile(it) }
                            )
                            MonoTab.LIBRARY -> LibraryScreen(
                                stats = allStats,
                                savedGames = savedGames,
                                matchHistory = matchHistory,
                                onOpenGame = { id, mode -> viewModel.openGameSetup(id, mode) },
                                onResumeSaved = { viewModel.resumeSavedGame(it) },
                                onDeleteSaved = { viewModel.clearSavedGame(it) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) }
                            )
                            MonoTab.SETTINGS -> SettingsScreen(
                                settings = settings,
                                onSetThemeMode = { viewModel.setThemeMode(it) },
                                onSetSoundEffects = { viewModel.setSoundEffects(it) },
                                onSetBackgroundMusic = { viewModel.setBackgroundMusic(it) },
                                onSetVibration = { viewModel.setVibration(it) },
                                onSetLanguage = { viewModel.setLanguage(it) },
                                onSetPrivacyModeDefault = { viewModel.setPrivacyModeDefault(it) },
                                onSetBoardTheme = { viewModel.setBoardTheme(it) },
                                onSetPieceTheme = { viewModel.setPieceTheme(it) },
                                onSetShowLegalMoves = { viewModel.setShowLegalMoves(it) },
                                onSetConfirmTurnPass = { viewModel.setConfirmTurnPass(it) },
                                onResetStats = { viewModel.resetStatistics() },
                                onResetAllData = { viewModel.resetAllLocalData() }
                            )
                        }
                    }
                }
            }
        }

        pendingSetup?.let { request ->
            val saved = savedGames.firstOrNull { it.gameId == request.game.id }
            PassAndPlaySetupSheet(
                game = request.game,
                defaultPrivacyMode = settings.privacyModeDefault,
                savedProfiles = playerProfiles,
                preferredPlayMode = request.preferredMode,
                hasSavedSession = saved != null,
                onResumeSaved = if (saved != null) {
                    { viewModel.resumeSavedGame(saved) }
                } else null,
                onDismiss = { viewModel.dismissSetup() },
                onStartGame = { config -> viewModel.launchSession(config) }
            )
        }
    }
}
