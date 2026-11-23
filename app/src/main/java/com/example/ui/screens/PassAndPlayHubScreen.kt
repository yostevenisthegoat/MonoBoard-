package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.db.GameStatEntity
import com.example.data.db.PlayerProfileEntity
import com.example.data.db.SavedGameEntity
import com.example.domain.AvatarSymbol
import com.example.domain.GameCatalog
import com.example.domain.PlayMode
import com.example.ui.components.AvatarBadge
import com.example.ui.components.GameCatalogCard
import com.example.ui.components.MonoCard
import com.example.ui.components.MonoSectionHeader
import com.example.ui.components.MonoTag

private enum class PassFilter(val label: String) {
    ALL("All Pass & Play"),
    MULTI_SEAT("2–4 Players"),
    PRIVACY("Privacy Mode")
}

@Composable
fun PassAndPlayHubScreen(
    statsMap: Map<String, GameStatEntity>,
    savedGames: List<SavedGameEntity>,
    playerProfiles: List<PlayerProfileEntity>,
    onOpenGame: (String, PlayMode?) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onAddProfile: (String, AvatarSymbol) -> Unit,
    onRemoveProfile: (Long) -> Unit
) {
    var filter by remember { mutableStateOf(PassFilter.ALL) }
    var showAddProfile by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newAvatar by remember { mutableStateOf(AvatarSymbol.CROWN) }

    val passGames = remember(filter) {
        GameCatalog.passAndPlayGames.filter { game ->
            when (filter) {
                PassFilter.ALL -> true
                PassFilter.MULTI_SEAT -> game.maxPlayers >= 3
                PassFilter.PRIVACY -> game.requiresPrivacyByDefault
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            MonoCard(elevated = true) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Pass & Play Engine", style = MaterialTheme.typography.headlineMedium)
                    }
                    Text(
                        "One phone, zero internet. Configure seats, avatars, turn timers, starting order, and automatic Privacy Curtains for hidden-hand games.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MonoTag(text = "${GameCatalog.passAndPlayGames.size} Local Games", emphasized = true)
                        MonoTag(text = "Privacy Shield", icon = Icons.Rounded.Lock)
                    }
                }
            }
        }

        // Local Saved Player Profiles
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MonoSectionHeader(
                    title = "Local Player Roster",
                    subtitle = "Quick-fill names & avatars during game setup",
                    actionLabel = "+ Add",
                    onActionClick = { showAddProfile = true }
                )
                if (playerProfiles.isEmpty()) {
                    MonoCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Save regular players locally for 1-tap seating.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = { showAddProfile = true },
                                modifier = Modifier.testTag("add_profile_button")
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add Player")
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        playerProfiles.forEach { profile ->
                            val avatar = runCatching { AvatarSymbol.valueOf(profile.avatarName) }
                                .getOrDefault(AvatarSymbol.CROWN)
                            MonoCard(elevated = true) {
                                Row(
                                    modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AvatarBadge(symbol = avatar, isActive = true, size = 30.dp)
                                    Text(profile.name, style = MaterialTheme.typography.labelLarge)
                                    IconButton(
                                        onClick = { onRemoveProfile(profile.id) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete profile",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Filter Chips
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PassFilter.entries.forEach { pf ->
                    FilterChip(
                        selected = filter == pf,
                        onClick = { filter = pf },
                        label = { Text(pf.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        items(passGames, key = { it.id }) { game ->
            GameCatalogCard(
                game = game,
                isFavorite = statsMap[game.id]?.isFavorite == true,
                hasSavedSession = savedGames.any { it.gameId == game.id },
                onPlayClick = { onOpenGame(game.id, PlayMode.PASS_AND_PLAY) },
                onToggleFavorite = { onToggleFavorite(game.id) }
            )
        }
        item { Spacer(Modifier.height(14.dp)) }
    }

    if (showAddProfile) {
        val selectableAvatars = AvatarSymbol.entries.filter { it != AvatarSymbol.BOT }
        AlertDialog(
            onDismissRequest = { showAddProfile = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("New Local Player", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it.take(18) },
                        label = { Text("Player Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Select Avatar", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectableAvatars.forEach { sym ->
                            AvatarBadge(
                                symbol = sym,
                                isActive = newAvatar == sym,
                                size = 42.dp,
                                modifier = Modifier.clickable { newAvatar = sym }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onAddProfile(newName.trim(), newAvatar)
                            newName = ""
                            showAddProfile = false
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Save Profile", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showAddProfile = false }) { Text("Cancel") }
            }
        )
    }
}
