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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.prefs.AppSettings
import com.example.ui.components.MonoCard
import com.example.ui.components.MonoSectionHeader
import com.example.ui.components.MonoTag
import com.example.ui.theme.BoardTheme
import com.example.ui.theme.PieceTheme
import com.example.ui.theme.ThemeMode

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetSoundEffects: (Boolean) -> Unit,
    onSetBackgroundMusic: (Boolean) -> Unit,
    onSetVibration: (Boolean) -> Unit,
    onSetLanguage: (String) -> Unit,
    onSetPrivacyModeDefault: (Boolean) -> Unit,
    onSetBoardTheme: (BoardTheme) -> Unit,
    onSetPieceTheme: (PieceTheme) -> Unit,
    onSetShowLegalMoves: (Boolean) -> Unit,
    onSetConfirmTurnPass: (Boolean) -> Unit,
    onResetStats: () -> Unit,
    onResetAllData: () -> Unit
) {
    var confirmResetStats by remember { mutableStateOf(false) }
    var confirmWipeAll by remember { mutableStateOf(false) }
    var showLicenseDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val languages = listOf("English", "Español", "Français", "Deutsch", "日本語")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Settings", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "Mono Ecosystem • 100% Offline Preferences",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Appearance
        item {
            MonoSectionHeader(title = "Appearance")
            Spacer(Modifier.height(8.dp))
            MonoCard {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Theme Mode", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { onSetThemeMode(mode) },
                                label = { Text(mode.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.testTag("theme_${mode.name}")
                            )
                        }
                    }
                }
            }
        }

        // Board & Piece Customization
        item {
            MonoSectionHeader(title = "Board & Gameplay")
            Spacer(Modifier.height(8.dp))
            MonoCard {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Board Theme", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BoardTheme.entries.forEach { bt ->
                            FilterChip(
                                selected = settings.boardTheme == bt,
                                onClick = { onSetBoardTheme(bt) },
                                label = { Text(bt.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Text("Piece Style", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PieceTheme.entries.forEach { pt ->
                            FilterChip(
                                selected = settings.pieceTheme == pt,
                                onClick = { onSetPieceTheme(pt) },
                                label = { Text(pt.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    ToggleSettingRow(
                        title = "Show Legal Moves",
                        subtitle = "Highlight valid squares on Chess, Checkers & Reversi",
                        checked = settings.showLegalMoves,
                        onCheckedChange = onSetShowLegalMoves
                    )
                    ToggleSettingRow(
                        title = "Confirm Turn Pass",
                        subtitle = "Show clear pass-device cues between Pass & Play turns",
                        checked = settings.confirmTurnPass,
                        onCheckedChange = onSetConfirmTurnPass
                    )
                    ToggleSettingRow(
                        title = "Privacy Mode Default",
                        subtitle = "Hide hands/fleets in Battleship, Dominoes & Card Games",
                        checked = settings.privacyModeDefault,
                        onCheckedChange = onSetPrivacyModeDefault
                    )
                }
            }
        }

        // Audio & Haptics
        item {
            MonoSectionHeader(title = "Audio & Haptics")
            Spacer(Modifier.height(8.dp))
            MonoCard {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ToggleSettingRow(
                        title = "Sound Effects",
                        subtitle = "Synthesized offline move, capture & win cues",
                        checked = settings.soundEffects,
                        onCheckedChange = onSetSoundEffects
                    )
                    ToggleSettingRow(
                        title = "Background Ambient Tone",
                        subtitle = "Minimalist low-volume offline harmonic pulse",
                        checked = settings.backgroundMusic,
                        onCheckedChange = onSetBackgroundMusic
                    )
                    ToggleSettingRow(
                        title = "Vibration Feedback",
                        subtitle = "Tactile haptic response on valid moves and alerts",
                        checked = settings.vibration,
                        onCheckedChange = onSetVibration
                    )
                }
            }
        }

        // Language
        item {
            MonoSectionHeader(title = "Language")
            Spacer(Modifier.height(8.dp))
            MonoCard {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Interface Language", style = MaterialTheme.typography.titleMedium)
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        languages.forEach { lang ->
                            FilterChip(
                                selected = settings.language == lang,
                                onClick = { onSetLanguage(lang) },
                                label = { Text(lang) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }
        }

        // Local Data Management
        item {
            MonoSectionHeader(title = "Local Storage & Reset")
            Spacer(Modifier.height(8.dp))
            MonoCard {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { confirmResetStats = true }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Reset Game Statistics", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Clears win/loss tallies & match log while keeping favorites",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { confirmWipeAll = true }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Erase All Local Data", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                            Text(
                                "Deletes saved games, player profiles, stats & preferences",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // About & Open Source License
        item {
            MonoSectionHeader(title = "Open Source & About")
            Spacer(Modifier.height(8.dp))
            MonoCard {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAboutDialog = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("About Mono Board", style = MaterialTheme.typography.titleMedium)
                            Text("v1.0 • Mono Ecosystem • Zero Telemetry", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        MonoTag(text = "MIT", emphasized = true)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLicenseDialog = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Open Source License & Architecture", style = MaterialTheme.typography.titleMedium)
                            Text("Modular engines, MIT License & contributor structure", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(14.dp)) }
    }

    if (confirmResetStats) {
        AlertDialog(
            onDismissRequest = { confirmResetStats = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Reset Statistics?") },
            text = { Text("This clears all match history and per-game win/loss records stored on this device.") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetStats()
                        confirmResetStats = false
                    }
                ) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { confirmResetStats = false }) { Text("Cancel") }
            }
        )
    }

    if (confirmWipeAll) {
        AlertDialog(
            onDismissRequest = { confirmWipeAll = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Erase All Local Data?") },
            text = { Text("Saved matches, local player profiles, statistics, favorites, and settings will be permanently cleared.") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetAllData()
                        confirmWipeAll = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Erase All") }
            },
            dismissButton = {
                TextButton(onClick = { confirmWipeAll = false }) { Text("Cancel") }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Mono Board — Open Source", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Built under the Mono ecosystem: open source, minimalist, ad-free, and 100% offline.\n\n" +
                        "Every board game, card deck, puzzle generator, and AI engine executes locally on your device without internet permissions, tracking, or servers."
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("Close") }
            }
        )
    }

    if (showLicenseDialog) {
        AlertDialog(
            onDismissRequest = { showLicenseDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("MIT License & Modular Architecture", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Copyright (c) Mono Ecosystem Contributors\n\n" +
                        "Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files, to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies.\n\n" +
                        "Modular Contributor Architecture:\n" +
                        "• domain/GameCatalog.kt — Self-contained game metadata registry\n" +
                        "• ui/games/* — Independent per-game engines & Compose UI modules\n" +
                        "• ui/components/PrivacyCurtain.kt — Universal Pass & Play + Privacy Layer"
                )
            },
            confirmButton = {
                TextButton(onClick = { showLicenseDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun ToggleSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
