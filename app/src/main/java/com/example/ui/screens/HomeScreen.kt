package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.db.GameStatEntity
import com.example.data.db.SavedGameEntity
import com.example.domain.GameCatalog
import com.example.domain.GameCategory
import com.example.domain.GameDefinition
import com.example.domain.PlayMode
import com.example.ui.components.GameCatalogCard
import com.example.ui.components.MonoCard
import com.example.ui.components.MonoSectionHeader
import com.example.ui.components.MonoTag

@Composable
fun HomeScreen(
    statsMap: Map<String, GameStatEntity>,
    savedGames: List<SavedGameEntity>,
    onOpenGame: (String, PlayMode?) -> Unit,
    onResumeSaved: (SavedGameEntity) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onNavigateToGames: (GameCategory?) -> Unit,
    onNavigateToPassAndPlay: () -> Unit
) {
    val recentlyPlayedGames = statsMap.values
        .filter { it.lastPlayedAt > 0L }
        .sortedByDescending { it.lastPlayedAt }
        .take(5)
        .map { GameCatalog.byId(it.gameId) }

    val popularGames = GameCatalog.allGames.filter { it.isPopular }.take(6)
    val featuredGames = GameCatalog.allGames.filter { it.isFeatured }.take(5)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Mono Hero Banner
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_mono_hero),
                        contentDescription = "Mono Board Hero Artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0x33080808),
                                        Color(0xD9080808)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MonoTag(text = "OPEN SOURCE • 100% OFFLINE", emphasized = true)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Welcome to Mono Board",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White
                        )
                        Text(
                            text = "${GameCatalog.allGames.size} classic board, card, puzzle & word games on one device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFD0D0D0),
                            maxLines = 2
                        )
                    }
                }
            }
        }

        // Primary Mode Shortcuts (Pass & Play / vs Computer)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MonoCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_pass_play_shortcut"),
                    onClick = onNavigateToPassAndPlay,
                    elevated = true
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Groups,
                            contentDescription = "Pass & Play",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("Pass & Play", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Share one phone with 2–4 local players & Privacy Mode",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
                MonoCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_vs_ai_shortcut"),
                    onClick = { onOpenGame("chess", PlayMode.VS_AI) },
                    elevated = true
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SmartToy,
                            contentDescription = "Play vs Computer",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("vs Computer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Challenge offline AI in Chess, Checkers, Go, Reversi & more",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
            }
        }

        // Continue Playing (Saved Resumable Matches)
        if (savedGames.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MonoSectionHeader(title = "Continue Playing", subtitle = "Resume unfinished offline matches")
                    savedGames.forEach { saved ->
                        MonoCard(
                            onClick = { onResumeSaved(saved) },
                            borderHighlight = true
                        ) {
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
                                Button(
                                    onClick = { onResumeSaved(saved) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Resume")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recently Played
        if (recentlyPlayedGames.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MonoSectionHeader(title = "Recently Played")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(recentlyPlayedGames, key = { it.id }) { g ->
                            CompactGameTile(game = g, onClick = { onOpenGame(g.id, null) })
                        }
                    }
                }
            }
        }

        // Featured Games
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MonoSectionHeader(
                    title = "Featured Boards",
                    actionLabel = "See All",
                    onActionClick = { onNavigateToGames(null) }
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(featuredGames, key = { it.id }) { g ->
                        CompactGameTile(game = g, onClick = { onOpenGame(g.id, null) })
                    }
                }
            }
        }

        // Game Categories
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MonoSectionHeader(title = "Game Categories")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(GameCategory.entries) { cat ->
                        val count = GameCatalog.byCategory(cat).size
                        MonoCard(
                            onClick = { onNavigateToGames(cat) },
                            elevated = true
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                                Text(cat.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "$count games",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Popular Games
        item {
            MonoSectionHeader(
                title = "Popular Classics",
                actionLabel = "Browse ${GameCatalog.allGames.size}",
                onActionClick = { onNavigateToGames(null) }
            )
        }
        items(popularGames, key = { it.id }) { g ->
            GameCatalogCard(
                game = g,
                isFavorite = statsMap[g.id]?.isFavorite == true,
                hasSavedSession = savedGames.any { it.gameId == g.id },
                onPlayClick = { onOpenGame(g.id, null) },
                onToggleFavorite = { onToggleFavorite(g.id) }
            )
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun CompactGameTile(
    game: GameDefinition,
    onClick: () -> Unit
) {
    MonoCard(
        modifier = Modifier
            .width(156.dp)
            .clickable(onClick = onClick),
        elevated = true
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = game.icon,
                contentDescription = game.title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = game.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = game.category.shortLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
