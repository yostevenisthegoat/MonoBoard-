package com.example.domain

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brightness1
import androidx.compose.material.icons.rounded.ChangeHistory
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Hexagon
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Star
import androidx.compose.ui.graphics.vector.ImageVector

enum class AvatarSymbol(val label: String, val icon: ImageVector) {
    CROWN("Apex", Icons.Rounded.AutoAwesome),
    SHIELD("Shield", Icons.Rounded.Shield),
    HEX("Hex", Icons.Rounded.Hexagon),
    DIAMOND("Diamond", Icons.Rounded.Diamond),
    STAR("Star", Icons.Rounded.Star),
    CIRCLE("Orb", Icons.Rounded.Brightness1),
    DELTA("Delta", Icons.Rounded.ChangeHistory),
    BOT("Bot", Icons.Rounded.SmartToy)
}

enum class AiDifficulty(val label: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard"),
    EXPERT("Expert")
}

enum class PlayMode(val label: String) {
    SOLO("Solo"),
    PASS_AND_PLAY("Pass & Play"),
    VS_AI("vs Computer")
}

enum class StartingPlayerRule(val label: String) {
    PLAYER_ONE("Player 1 First"),
    RANDOM("Random Starter"),
    ALTERNATE("Seat Order")
}

data class PlayerSlotConfig(
    val index: Int,
    val name: String,
    val avatar: AvatarSymbol,
    val isAi: Boolean = false,
    val aiDifficulty: AiDifficulty = AiDifficulty.MEDIUM
)

data class GameSessionConfig(
    val gameId: String,
    val playMode: PlayMode,
    val players: List<PlayerSlotConfig>,
    val aiDifficulty: AiDifficulty = AiDifficulty.MEDIUM,
    val timeLimitMinutes: Int = 0, // 0 = untimed
    val privacyMode: Boolean = false,
    val startingPlayerRule: StartingPlayerRule = StartingPlayerRule.PLAYER_ONE,
    val ruleVariant: String = "Standard"
) {
    val startingPlayerIndex: Int
        get() = when (startingPlayerRule) {
            StartingPlayerRule.RANDOM -> if (players.isNotEmpty()) (players.indices).random() else 0
            else -> 0
        }
}
