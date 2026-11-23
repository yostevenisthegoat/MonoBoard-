package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.db.PlayerProfileEntity
import com.example.domain.AiDifficulty
import com.example.domain.AvatarSymbol
import com.example.domain.GameDefinition
import com.example.domain.GameSessionConfig
import com.example.domain.PlayMode
import com.example.domain.PlayerSlotConfig
import com.example.domain.StartingPlayerRule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassAndPlaySetupSheet(
    game: GameDefinition,
    defaultPrivacyMode: Boolean,
    savedProfiles: List<PlayerProfileEntity>,
    preferredPlayMode: PlayMode? = null,
    hasSavedSession: Boolean = false,
    onResumeSaved: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onStartGame: (GameSessionConfig) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val availableModes = remember(game) {
        buildList {
            if (game.supportsPassAndPlay) add(PlayMode.PASS_AND_PLAY)
            if (game.supportsAi) add(PlayMode.VS_AI)
            if (game.supportsSolo) add(PlayMode.SOLO)
        }
    }
    var selectedMode by remember(game) {
        mutableStateOf(
            if (preferredPlayMode != null && preferredPlayMode in availableModes) preferredPlayMode
            else availableModes.first()
        )
    }
    var playerCount by remember(game, selectedMode) {
        mutableIntStateOf(
            when (selectedMode) {
                PlayMode.SOLO -> 1
                PlayMode.VS_AI -> game.minPlayers.coerceAtLeast(2)
                PlayMode.PASS_AND_PLAY -> game.minPlayers.coerceAtLeast(2)
            }
        )
    }
    var aiDifficulty by remember { mutableStateOf(AiDifficulty.MEDIUM) }
    var ruleVariant by remember(game) { mutableStateOf(game.ruleVariants.first()) }
    var timeLimitMinutes by remember { mutableIntStateOf(0) }
    var privacyMode by remember(game) {
        mutableStateOf(game.requiresPrivacyByDefault || (defaultPrivacyMode && game.requiresPrivacyByDefault))
    }
    var startingRule by remember { mutableStateOf(StartingPlayerRule.PLAYER_ONE) }

    val defaultAvatars = remember { AvatarSymbol.entries.filter { it != AvatarSymbol.BOT } }
    val slotNames = remember { mutableStateListOf("Player 1", "Player 2", "Player 3", "Player 4") }
    val slotAvatars = remember {
        mutableStateListOf(
            defaultAvatars[0],
            defaultAvatars[1],
            defaultAvatars[2],
            defaultAvatars[3]
        )
    }
    val slotIsAi = remember { mutableStateListOf(false, false, false, false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = game.icon,
                    contentDescription = game.title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = game.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = game.rulesSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (hasSavedSession && onResumeSaved != null) {
                OutlinedButton(
                    onClick = onResumeSaved,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("resume_saved_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Continue Saved Match")
                }
            }

            // Mode Selection
            if (availableModes.size > 1) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Game Mode", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableModes.forEach { mode ->
                            val icon = when (mode) {
                                PlayMode.PASS_AND_PLAY -> Icons.Rounded.Groups
                                PlayMode.VS_AI -> Icons.Rounded.SmartToy
                                PlayMode.SOLO -> Icons.Rounded.Person
                            }
                            FilterChip(
                                selected = selectedMode == mode,
                                onClick = {
                                    selectedMode = mode
                                    playerCount = when (mode) {
                                        PlayMode.SOLO -> 1
                                        PlayMode.VS_AI -> game.minPlayers.coerceAtLeast(2)
                                        PlayMode.PASS_AND_PLAY -> game.minPlayers.coerceAtLeast(2)
                                    }
                                },
                                label = { Text(mode.label) },
                                leadingIcon = {
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }

            // Player Count (when variable)
            if (selectedMode != PlayMode.SOLO && game.maxPlayers > game.minPlayers) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Number of Players", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (game.minPlayers.coerceAtLeast(2)..game.maxPlayers).forEach { count ->
                            FilterChip(
                                selected = playerCount == count,
                                onClick = { playerCount = count },
                                label = { Text("$count Players") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }

            // Player Configuration (Names, Avatars, AI Seats)
            if (selectedMode != PlayMode.SOLO) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Players & Seats", style = MaterialTheme.typography.titleMedium)
                    if (savedProfiles.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            savedProfiles.take(6).forEach { profile ->
                                MonoTag(
                                    text = "+ ${profile.name}",
                                    modifier = Modifier.clickable {
                                        val emptyIdx = (0 until playerCount).firstOrNull {
                                            slotNames[it].startsWith("Player ")
                                        } ?: 0
                                        slotNames[emptyIdx] = profile.name
                                        slotAvatars[emptyIdx] = runCatching {
                                            AvatarSymbol.valueOf(profile.avatarName)
                                        }.getOrDefault(defaultAvatars[emptyIdx % defaultAvatars.size])
                                    }
                                )
                            }
                        }
                    }

                    (0 until playerCount).forEach { idx ->
                        val forcedAi = selectedMode == PlayMode.VS_AI && idx > 0
                        val seatIsAi = forcedAi || (selectedMode == PlayMode.PASS_AND_PLAY && game.supportsAi && idx > 0 && slotIsAi[idx])
                        MonoCard(elevated = true) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AvatarBadge(
                                    symbol = if (seatIsAi) AvatarSymbol.BOT else slotAvatars[idx],
                                    isActive = idx == 0,
                                    size = 38.dp,
                                    modifier = Modifier.clickable {
                                        if (!seatIsAi) {
                                            val nextIdx = (defaultAvatars.indexOf(slotAvatars[idx]) + 1) % defaultAvatars.size
                                            slotAvatars[idx] = defaultAvatars[nextIdx]
                                        }
                                    }
                                )
                                OutlinedTextField(
                                    value = if (seatIsAi) "Mono AI ${if (playerCount > 2) idx else ""}".trim() else slotNames[idx],
                                    onValueChange = { if (!seatIsAi) slotNames[idx] = it.take(18) },
                                    enabled = !seatIsAi,
                                    singleLine = true,
                                    label = { Text("Seat ${idx + 1}") },
                                    modifier = Modifier.weight(1f)
                                )
                                if (selectedMode == PlayMode.PASS_AND_PLAY && game.supportsAi && idx > 0 && playerCount > 2) {
                                    FilterChip(
                                        selected = slotIsAi[idx],
                                        onClick = { slotIsAi[idx] = !slotIsAi[idx] },
                                        label = { Text(if (slotIsAi[idx]) "AI" else "Human") }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // AI Difficulty
            val anyAi = selectedMode == PlayMode.VS_AI ||
                (selectedMode == PlayMode.PASS_AND_PLAY && game.supportsAi && (1 until playerCount).any { slotIsAi[it] })
            if (anyAi) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Computer Difficulty", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(AiDifficulty.EASY, AiDifficulty.MEDIUM, AiDifficulty.HARD).forEach { diff ->
                            FilterChip(
                                selected = aiDifficulty == diff,
                                onClick = { aiDifficulty = diff },
                                label = { Text(diff.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }

            // Rule Variants
            if (game.ruleVariants.size > 1) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ruleset / Difficulty", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        game.ruleVariants.forEach { variant ->
                            FilterChip(
                                selected = ruleVariant == variant,
                                onClick = { ruleVariant = variant },
                                label = { Text(variant) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }

            // Clock Duration (where applicable)
            if (game.supportsTimers) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Match Clock", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0 to "Untimed", 3 to "3 Min", 5 to "5 Min", 10 to "10 Min", 15 to "15 Min").forEach { (mins, label) ->
                            FilterChip(
                                selected = timeLimitMinutes == mins,
                                onClick = { timeLimitMinutes = mins },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }

            // Starting Player & Privacy Mode for Pass & Play
            if (selectedMode == PlayMode.PASS_AND_PLAY) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Starting Player", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(StartingPlayerRule.PLAYER_ONE, StartingPlayerRule.RANDOM).forEach { rule ->
                            FilterChip(
                                selected = startingRule == rule,
                                onClick = { startingRule = rule },
                                label = { Text(rule.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                MonoCard(elevated = true) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Privacy Mode", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Hide private hands/boards between turns",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = privacyMode, onCheckedChange = { privacyMode = it })
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = {
                    val players = if (selectedMode == PlayMode.SOLO) {
                        listOf(PlayerSlotConfig(0, "Player 1", slotAvatars[0], isAi = false))
                    } else {
                        (0 until playerCount).map { idx ->
                            val isAi = (selectedMode == PlayMode.VS_AI && idx > 0) ||
                                (selectedMode == PlayMode.PASS_AND_PLAY && game.supportsAi && idx > 0 && slotIsAi[idx])
                            PlayerSlotConfig(
                                index = idx,
                                name = if (isAi) "Mono AI" + (if (playerCount > 2) " ${idx}" else "") else slotNames[idx].ifBlank { "Player ${idx + 1}" },
                                avatar = if (isAi) AvatarSymbol.BOT else slotAvatars[idx],
                                isAi = isAi,
                                aiDifficulty = aiDifficulty
                            )
                        }
                    }
                    onStartGame(
                        GameSessionConfig(
                            gameId = game.id,
                            playMode = selectedMode,
                            players = players,
                            aiDifficulty = aiDifficulty,
                            timeLimitMinutes = timeLimitMinutes,
                            privacyMode = privacyMode,
                            startingPlayerRule = startingRule,
                            ruleVariant = ruleVariant
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("start_game_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "Start ${game.title}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
