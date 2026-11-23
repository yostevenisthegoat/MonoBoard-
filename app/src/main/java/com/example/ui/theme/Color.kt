package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Mono Design System — Obsidian Dark Palette (Default)
val MonoBlack = Color(0xFF080808)
val MonoCharcoal = Color(0xFF1B1B1B)
val MonoElevated = Color(0xFF242424)
val MonoElevatedHigh = Color(0xFF2C2C2C)
val MonoBorderDark = Color(0xFF2F2F2F)
val MonoWhite = Color(0xFFF5F5F5)
val MonoPureWhite = Color(0xFFFFFFFF)
val MonoGreySecondary = Color(0xFF9E9E9E)
val MonoGreyMuted = Color(0xFF6B6B6B)

// Mono Design System — Paper Light Palette (Optional)
val MonoPaper = Color(0xFFF7F7F7)
val MonoLightCard = Color(0xFFFFFFFF)
val MonoLightElevated = Color(0xFFECECEC)
val MonoBorderLight = Color(0xFFDCDCDC)
val MonoInk = Color(0xFF0A0A0A)
val MonoLightSecondary = Color(0xFF5F5F5F)

// Subtle semantic tones for gameplay clarity while preserving Mono discipline
val MonoSuccess = Color(0xFF4CAF50)
val MonoWarning = Color(0xFFE0A82E)
val MonoError = Color(0xFFCF6679)

// Traditional board game palettes (used exclusively inside active gameplay boards)
enum class BoardTheme(
    val label: String,
    val lightSquare: Color,
    val darkSquare: Color,
    val highlight: Color,
    val boardFrame: Color
) {
    MONO_CLASSIC(
        label = "Mono Slate",
        lightSquare = Color(0xFFDCDCDC),
        darkSquare = Color(0xFF3A3A3A),
        highlight = Color(0xFF8C8C8C),
        boardFrame = Color(0xFF1B1B1B)
    ),
    WARM_WOOD(
        label = "Classic Wood",
        lightSquare = Color(0xFFE8D0AA),
        darkSquare = Color(0xFF8D5B3C),
        highlight = Color(0xFFC8A951),
        boardFrame = Color(0xFF4E3120)
    ),
    MIDNIGHT_BLUE(
        label = "Midnight Steel",
        lightSquare = Color(0xFFD5DCE4),
        darkSquare = Color(0xFF3C536B),
        highlight = Color(0xFF6B8FAD),
        boardFrame = Color(0xFF1C2733)
    ),
    TOURNAMENT_GREEN(
        label = "Tournament Felt",
        lightSquare = Color(0xFFEEEED2),
        darkSquare = Color(0xFF587D4B),
        highlight = Color(0xFFBACA44),
        boardFrame = Color(0xFF283B22)
    )
}

enum class PieceTheme(val label: String) {
    CLASSIC("Classic Serif"),
    GEOMETRIC("Geometric"),
    HIGH_CONTRAST("High Contrast")
}
