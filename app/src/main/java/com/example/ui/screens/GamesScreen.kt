package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.db.GameStatEntity
import com.example.data.db.SavedGameEntity
import com.example.domain.GameCatalog
import com.example.domain.GameCategory
import com.example.domain.PlayMode
import com.example.ui.components.GameCatalogCard

private enum class ModeFilter(val label: String) {
    ALL("All Modes"),
    PASS_AND_PLAY("Pass & Play"),
    VS_AI("vs Computer"),
    SOLO("Solo")
}

@Composable
fun GamesScreen(
    initialCategory: GameCategory?,
    statsMap: Map<String, GameStatEntity>,
    savedGames: List<SavedGameEntity>,
    onOpenGame: (String, PlayMode?) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember(initialCategory) { mutableStateOf(initialCategory) }
    var modeFilter by remember { mutableStateOf(ModeFilter.ALL) }

    val filteredGames = remember(searchQuery, selectedCategory, modeFilter) {
        val base = if (selectedCategory == null) GameCatalog.allGames else GameCatalog.byCategory(selectedCategory!!)
        base.filter { game ->
            val matchesSearch = searchQuery.isBlank() ||
                game.title.contains(searchQuery, ignoreCase = true) ||
                game.subtitle.contains(searchQuery, ignoreCase = true) ||
                game.category.label.contains(searchQuery, ignoreCase = true)
            val matchesMode = when (modeFilter) {
                ModeFilter.ALL -> true
                ModeFilter.PASS_AND_PLAY -> game.supportsPassAndPlay
                ModeFilter.VS_AI -> game.supportsAi
                ModeFilter.SOLO -> game.supportsSolo
            }
            matchesSearch && matchesMode
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "All Games (${filteredGames.size})",
                style = MaterialTheme.typography.headlineLarge
            )
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search chess, sudoku, solitaire, trivia...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("games_search_input")
            )

            // Category Filter Chips
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("All (${GameCatalog.allGames.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                GameCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = { Text(cat.shortLabel) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Mode Filter Chips
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModeFilter.entries.forEach { mf ->
                    FilterChip(
                        selected = modeFilter == mf,
                        onClick = { modeFilter = mf },
                        label = { Text(mf.label) }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredGames, key = { it.id }) { game ->
                val preferred = when (modeFilter) {
                    ModeFilter.PASS_AND_PLAY -> PlayMode.PASS_AND_PLAY
                    ModeFilter.VS_AI -> PlayMode.VS_AI
                    ModeFilter.SOLO -> PlayMode.SOLO
                    ModeFilter.ALL -> null
                }
                GameCatalogCard(
                    game = game,
                    isFavorite = statsMap[game.id]?.isFavorite == true,
                    hasSavedSession = savedGames.any { it.gameId == game.id },
                    onPlayClick = { onOpenGame(game.id, preferred) },
                    onToggleFavorite = { onToggleFavorite(game.id) }
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
