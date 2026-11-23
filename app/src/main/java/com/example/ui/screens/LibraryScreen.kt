package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.db.GameStatEntity
import com.example.data.db.MatchHistoryEntity
import com.example.data.db.SavedGameEntity
import com.example.domain.GameCatalog
import com.example.domain.PlayMode
import com.example.ui.components.GameCatalogCard
import com.example.ui.components.MonoCard
import com.example.ui.components.MonoSectionHeader
import com.example.ui.components.MonoTag
import com.example.ui.components.StatMetricTile
import com.example.ui.theme.JetBrainsMonoFontFamily

private enum class LibraryTab(val label: String) {
    COLLECTION("Collection"),
    STATS("Statistics"),
    HISTORY("Match Log")
}

@Composable
fun LibraryScreen(
    stats: List<GameStatEntity>,
    savedGames: List<SavedGameEntity>,
    matchHistory: List<MatchHistoryEntity>,
    onOpenGame: (String, PlayMode?) -> Unit,
    onResumeSaved: (SavedGameEntity) -> Unit,
    onDeleteSaved: (String) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    var activeTab by remember { mutableStateOf(LibraryTab.COLLECTION) }
    val statsMap = remember(stats) { stats.associateBy { it.gameId } }
    val favoriteGames = remember(stats) {
        stats.filter { it.isFavorite }.map { GameCatalog.byId(it.gameId) }
    }
    val recentGames = remember(stats) {
        stats.filter { it.lastPlayedAt > 0L }
            .sortedByDescending { it.lastPlayedAt }
            .map { GameCatalog.byId(it.gameId) }
    }

    val totalPlayed = remember(stats) { stats.sumOf { it.gamesPlayed } }
    val totalWins = remember(stats) { stats.sumOf { it.wins } }
    val totalLosses = remember(stats) { stats.sumOf { it.losses } }
    val totalDraws = remember(stats) { stats.sumOf { it.draws } }
    val winRate = if (totalPlayed > 0) (totalWins * 100) / totalPlayed else 0

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Library & Records", style = MaterialTheme.typography.headlineLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LibraryTab.entries.forEach { tab ->
                    FilterChip(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        label = { Text(tab.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (activeTab) {
                LibraryTab.COLLECTION -> {
                    item {
                        MonoSectionHeader(
                            title = "Saved Matches (${savedGames.size})",
                            subtitle = "Resumable offline boards"
                        )
                    }
                    if (savedGames.isEmpty()) {
                        item {
                            MonoCard {
                                Text(
                                    "No suspended matches yet. Chess & Checkers auto-save on move.",
                                    modifier = Modifier.padding(14.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(savedGames, key = { it.gameId }) { saved ->
                            MonoCard(borderHighlight = true) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(saved.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${saved.modeLabel} • ${saved.playersSummary}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteSaved(saved.gameId) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete saved match")
                                    }
                                    Button(
                                        onClick = { onResumeSaved(saved) },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Resume")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(4.dp))
                        MonoSectionHeader(title = "Bookmarked Games (${favoriteGames.size})")
                    }
                    if (favoriteGames.isEmpty()) {
                        item {
                            MonoCard {
                                Text(
                                    "Tap the bookmark icon on any game to save it here.",
                                    modifier = Modifier.padding(14.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(favoriteGames, key = { "fav_${it.id}" }) { game ->
                            GameCatalogCard(
                                game = game,
                                isFavorite = true,
                                hasSavedSession = savedGames.any { it.gameId == game.id },
                                onPlayClick = { onOpenGame(game.id, null) },
                                onToggleFavorite = { onToggleFavorite(game.id) }
                            )
                        }
                    }

                    if (recentGames.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(4.dp))
                            MonoSectionHeader(title = "Recently Played")
                        }
                        items(recentGames, key = { "rec_${it.id}" }) { game ->
                            GameCatalogCard(
                                game = game,
                                isFavorite = statsMap[game.id]?.isFavorite == true,
                                hasSavedSession = savedGames.any { it.gameId == game.id },
                                onPlayClick = { onOpenGame(game.id, null) },
                                onToggleFavorite = { onToggleFavorite(game.id) }
                            )
                        }
                    }
                }

                LibraryTab.STATS -> {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatMetricTile("Games Played", totalPlayed.toString(), Modifier.weight(1f))
                            StatMetricTile("Win Rate", "$winRate%", Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatMetricTile("Wins", totalWins.toString(), Modifier.weight(1f))
                            StatMetricTile("Losses", totalLosses.toString(), Modifier.weight(1f))
                            StatMetricTile("Draws", totalDraws.toString(), Modifier.weight(1f))
                        }
                    }
                    item {
                        Spacer(Modifier.height(4.dp))
                        MonoSectionHeader(title = "Per-Game Records")
                    }
                    val playedStats = stats.filter { it.gamesPlayed > 0 || it.bestScore > 0 || it.bestTimeSeconds > 0 }
                        .sortedByDescending { it.gamesPlayed }
                    if (playedStats.isEmpty()) {
                        item {
                            MonoCard {
                                Text(
                                    "Complete a game to record wins, best scores, and fastest puzzle times locally.",
                                    modifier = Modifier.padding(14.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(playedStats, key = { "st_${it.gameId}" }) { st ->
                            val def = GameCatalog.byId(st.gameId)
                            MonoCard(onClick = { onOpenGame(def.id, null) }) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(def.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "Played ${st.gamesPlayed} • W ${st.wins} / L ${st.losses} / D ${st.draws}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (st.bestScore > 0) {
                                            MonoTag(text = "Best ${st.bestScore}", emphasized = true)
                                        }
                                        if (st.bestTimeSeconds > 0) {
                                            MonoTag(text = "${st.bestTimeSeconds}s")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                LibraryTab.HISTORY -> {
                    if (matchHistory.isEmpty()) {
                        item {
                            MonoCard {
                                Text(
                                    "No completed matches logged yet. Your offline match history is stored privately in Room.",
                                    modifier = Modifier.padding(14.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(matchHistory, key = { it.id }) { m ->
                            MonoCard {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(m.gameTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                            MonoTag(text = m.modeLabel)
                                        }
                                        Text(
                                            m.summary,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        MonoTag(text = m.outcome, emphasized = m.outcome == "WIN" || m.outcome == "COMPLETE")
                                        if (m.durationSeconds > 0) {
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                "${m.durationSeconds}s",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(14.dp)) }
        }
    }
}
