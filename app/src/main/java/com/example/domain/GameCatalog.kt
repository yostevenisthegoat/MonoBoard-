package com.example.domain

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BorderAll
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Dangerous
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Filter4
import androidx.compose.material.icons.rounded.Grid3x3
import androidx.compose.material.icons.rounded.Grid4x4
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LinearScale
import androidx.compose.material.icons.rounded.LooksOne
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.Pattern
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.Spellcheck
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SquareFoot
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.ViewComfy
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.ui.graphics.vector.ImageVector

enum class GameCategory(val label: String, val shortLabel: String) {
    STRATEGY("Strategy & Board", "Strategy"),
    CASUAL("Classic Casual", "Casual"),
    PUZZLE("Puzzle Games", "Puzzles"),
    CARDS("Card Games", "Cards"),
    WORD_TRIVIA("Word & Trivia", "Word & Trivia")
}

data class GameDefinition(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: GameCategory,
    val icon: ImageVector,
    val minPlayers: Int,
    val maxPlayers: Int,
    val supportsPassAndPlay: Boolean,
    val supportsAi: Boolean,
    val supportsSolo: Boolean,
    val requiresPrivacyByDefault: Boolean = false,
    val supportsTimers: Boolean = false,
    val ruleVariants: List<String> = listOf("Standard"),
    val isPopular: Boolean = false,
    val isFeatured: Boolean = false,
    val estimatedMinutes: Int = 10,
    val rulesSummary: String
)

object GameCatalog {
    val allGames: List<GameDefinition> = listOf(
        // ==================== STRATEGY & BOARD GAMES ====================
        GameDefinition(
            id = "chess",
            title = "Chess",
            subtitle = "Full ruleset, clocks & 3-tier AI",
            category = GameCategory.STRATEGY,
            icon = Icons.Rounded.EmojiEvents,
            minPlayers = 2, maxPlayers = 2,
            supportsPassAndPlay = true, supportsAi = true, supportsSolo = false,
            supportsTimers = true,
            ruleVariants = listOf("Standard", "No Clock", "Blitz 3m", "Rapid 10m"),
            isPopular = true, isFeatured = true, estimatedMinutes = 15,
            rulesSummary = "Checkmate the opponent's king. Includes castling, en passant, promotion, threefold repetition, and 50-move draw rules."
        ),
        GameDefinition(
            id = "checkers",
            title = "Checkers / Draughts",
            subtitle = "Multi-jumps, kings & forced capture",
            category = GameCategory.STRATEGY,
            icon = Icons.Rounded.GridOn,
            minPlayers = 2, maxPlayers = 2,
            supportsPassAndPlay = true, supportsAi = true, supportsSolo = false,
            ruleVariants = listOf("Forced Capture", "Casual Capture"),
            isPopular = true, isFeatured = true, estimatedMinutes = 10,
            rulesSummary = "Move diagonally on dark squares, jump opponent pieces in chains, and crown kings at the far rank."
        ),
        GameDefinition(
            id = "chinese_checkers",
            title = "Chinese Checkers",
            subtitle = "Star board chain-hopping race",
            category = GameCategory.STRATEGY,
            icon = Icons.Rounded.Pattern,
            minPlayers = 2, maxPlayers = 4,
            supportsPassAndPlay = true, supportsAi = true, supportsSolo = false,
            estimatedMinutes = 15,
            rulesSummary = "Move or chain-hop marbles across the 121-point hex star to fill your opposite home triangle first."
        ),
        GameDefinition(
            id = "go",
            title = "Go (9×9)",
            subtitle = "Territory, captures & Ko rule",
            category = GameCategory.STRATEGY,
            icon = Icons.Rounded.BorderAll,
            minPlayers = 2, maxPlayers = 2,
            supportsPassAndPlay = true, supportsAi = true, supportsSolo = false,
            estimatedMinutes = 15,
            rulesSummary = "Place black and white stones on intersections to surround territory and capture stone groups with zero liberties."
        ),
        GameDefinition(
            id = "reversi",
            title = "Reversi / Othello",
            subtitle = "Outflank & flip opponent discs",
            category = GameCategory.STRATEGY,
            icon = Icons.Rounded.CompareArrows,
            minPlayers = 2, maxPlayers = 2,
            supportsPassAndPlay = true, supportsAi = true, supportsSolo = false,
            isPopular = true, estimatedMinutes = 10,
            rulesSummary = "Sandwich opponent discs horizontally, vertically, or diagonally to flip them to your side."
        ),
        GameDefinition(
            id = "backgammon",
            title = "Backgammon",
            subtitle = "24-point race, blots & bearing off",
            category = GameCategory.STRATEGY,
            icon = Icons.Rounded.LinearScale,
            minPlayers = 2, maxPlayers = 2,
            supportsPassAndPlay = true, supportsAi = true, supportsSolo = false,
            estimatedMinutes = 15,
            rulesSummary = "Roll two dice to advance all 15 checkers into your home quadrant and bear them off before your opponent."
        ),
        GameDefinition(
            id = "mancala",
            title = "Mancala",
            subtitle = "Sowing seeds & extra turn captures",
            category = GameCategory.STRATEGY,
            icon = Icons.Rounded.ViewComfy,
            minPlayers = 2, maxPlayers = 2,
            supportsPassAndPlay = true, supportsAi = true, supportsSolo = false,
            isPopular = true, estimatedMinutes = 8,
            rulesSummary = "Sow seeds counter-clockwise. Land in your store for an extra turn, or in an empty pit to capture across."
        ),
        GameDefinition(
            id = "nine_mens_morris",
            title = "Nine Men's Morris",
            subtitle = "Form mills & fly with 3 pieces",
            category = GameCategory.STRATEGY,
            icon = Icons.Rounded.Grid3x3,
            minPlayers = 2, maxPlayers = 2,
            supportsPassAndPlay = true, supportsAi = true, supportsSolo = false,
            estimatedMinutes = 10,
            rulesSummary = "Place 9 men, slide along lines, and align 3-in-a-row mills to remove opponent pieces."
        ),
        GameDefinition(
            id = "shogi",
            title = "Shogi",
            subtitle = "Japanese chess with piece drops",
            category = GameCategory.STRATEGY,
            icon = Icons.Rounded.Shield,
            minPlayers = 2, maxPlayers = 2,
            supportsPassAndPlay = true, supportsAi = true, supportsSolo = false,
            estimatedMinutes = 20,
            rulesSummary = "Capture opponent pieces and drop them back onto the 9×9 board as your own allies. Promote pieces in the enemy zone."
        ),
        GameDefinition(
            id = "xiangqi",
            title = "Xiangqi",
            subtitle = "Chinese chess across the river",
            category = GameCategory.STRATEGY,
            icon = Icons.Rounded.TableChart,
            minPlayers = 2, maxPlayers = 2,
            supportsPassAndPlay = true, supportsAi = true, supportsSolo = false,
            estimatedMinutes = 20,
            rulesSummary = "Command Generals inside the 3×3 palace, jump Cannons over screens, and block Horses on the 9×10 grid."
        ),
        GameDefinition(
            id = "ludo",
            title = "Ludo",
            subtitle = "2–4 players, safe stars & captures",
            category = GameCategory.STRATEGY,
            icon = Icons.Rounded.Casino,
            minPlayers = 2, maxPlayers = 4,
            supportsPassAndPlay = true, supportsAi = true, supportsSolo = false,
            isPopular = true, isFeatured = true, estimatedMinutes = 15,
            rulesSummary = "Roll a 6 to deploy tokens, race clockwise around the 52-square track, capture rivals, and reach home."
        ),
        GameDefinition(
            id = "snakes_ladders",
            title = "Snakes & Ladders",
            subtitle = "Animated dice, chutes & 2–4 seats",
            category = GameCategory.STRATEGY,
            icon = Icons.Rounded.LinearScale,
            minPlayers = 2, maxPlayers = 4,
            supportsPassAndPlay = true, supportsAi = true, supportsSolo = false,
            ruleVariants = listOf("Exact 100", "Bounce Back"),
            isPopular = true, isFeatured = true, estimatedMinutes = 10,
            rulesSummary = "Roll to climb 1–100. Ladders boost you upward while snakes slide you down. First to square 100 wins."
        ),
        GameDefinition(
            id = "carrom",
            title = "Carrom",
            subtitle = "Interactive striker & pocket physics",
            category = GameCategory.STRATEGY,
            icon = Icons.Rounded.SportsEsports,
            minPlayers = 2, maxPlayers = 2,
            supportsPassAndPlay = true, supportsAi = true, supportsSolo = false,
            estimatedMinutes = 10,
            rulesSummary = "Aim and flick the striker to pocket your assigned coins and cover the red Queen."
        ),
        GameDefinition(
            id = "dominoes",
            title = "Dominoes",
            subtitle = "Double-six tiles & Privacy hands",
            category = GameCategory.STRATEGY,
            icon = Icons.Rounded.ViewModule,
            minPlayers = 2, maxPlayers = 4,
            supportsPassAndPlay = true, supportsAi = true, supportsSolo = false,
            requiresPrivacyByDefault = true,
            estimatedMinutes = 10,
            rulesSummary = "Match open pip ends on the board. Draw from the boneyard if blocked. Empty your hand first to score."
        ),
        GameDefinition(
            id = "mahjong",
            title = "Mahjong",
            subtitle = "Classic layered tile-matching",
            category = GameCategory.STRATEGY,
            icon = Icons.Rounded.Layers,
            minPlayers = 1, maxPlayers = 1,
            supportsPassAndPlay = false, supportsAi = false, supportsSolo = true,
            estimatedMinutes = 12,
            rulesSummary = "Clear pairs of matching exposed tiles that have no tile above and at least one free left or right side."
        ),

        // ==================== CLASSIC CASUAL GAMES ====================
        GameDefinition(
            id = "tic_tac_toe",
            title = "Tic-Tac-Toe",
            subtitle = "3×3 classic & unbeatable AI",
            category = GameCategory.CASUAL,
            icon = Icons.Rounded.Grid3x3,
            minPlayers = 2, maxPlayers = 2,
            supportsPassAndPlay = true, supportsAi = true, supportsSolo = false,
            isPopular = true, estimatedMinutes = 2,
            rulesSummary = "Align three X or O marks horizontally, vertically, or diagonally."
        ),
        GameDefinition(
            id = "connect_four",
            title = "Connect Four",
            subtitle = "7×6 gravity drop & smart AI",
            category = GameCategory.CASUAL,
            icon = Icons.Rounded.Filter4,
            minPlayers = 2, maxPlayers = 2,
            supportsPassAndPlay = true, supportsAi = true, supportsSolo = false,
            isPopular = true, isFeatured = true, estimatedMinutes = 5,
            rulesSummary = "Drop discs into columns and connect four in any direction before your opponent."
        ),
        GameDefinition(
            id = "dots_boxes",
            title = "Dots & Boxes",
            subtitle = "Claim squares & chain bonus turns",
            category = GameCategory.CASUAL,
            icon = Icons.Rounded.CheckBox,
            minPlayers = 2, maxPlayers = 4,
            supportsPassAndPlay = true, supportsAi = true, supportsSolo = false,
            ruleVariants = listOf("4×4 Grid", "5×5 Grid"),
            estimatedMinutes = 8,
            rulesSummary = "Draw edges between dots. Closing the fourth side of a box scores a point and grants another turn."
        ),
        GameDefinition(
            id = "battleship",
            title = "Battleship",
            subtitle = "Fleet placement & Privacy Radar",
            category = GameCategory.CASUAL,
            icon = Icons.Rounded.RocketLaunch,
            minPlayers = 2, maxPlayers = 2,
            supportsPassAndPlay = true, supportsAi = true, supportsSolo = false,
            requiresPrivacyByDefault = true,
            isPopular = true, estimatedMinutes = 12,
            rulesSummary = "Position 5 warships secretly, then call radar strikes to sink every opponent vessel."
        ),
        GameDefinition(
            id = "rps",
            title = "Rock Paper Scissors",
            subtitle = "Secret picks & best-of rounds",
            category = GameCategory.CASUAL,
            icon = Icons.Rounded.PanTool,
            minPlayers = 2, maxPlayers = 2,
            supportsPassAndPlay = true, supportsAi = true, supportsSolo = false,
            requiresPrivacyByDefault = true,
            ruleVariants = listOf("Best of 3", "Best of 5", "Best of 7"),
            estimatedMinutes = 2,
            rulesSummary = "Rock crushes Scissors, Scissors cuts Paper, Paper covers Rock. Privacy curtain hides Player 1's choice."
        ),
        GameDefinition(
            id = "hangman",
            title = "Hangman",
            subtitle = "Offline word categories & 2P mode",
            category = GameCategory.CASUAL,
            icon = Icons.Rounded.Spellcheck,
            minPlayers = 1, maxPlayers = 2,
            supportsPassAndPlay = true, supportsAi = false, supportsSolo = true,
            requiresPrivacyByDefault = true,
            estimatedMinutes = 4,
            rulesSummary = "Guess the hidden word letter by letter before 6 wrong guesses complete the figure."
        ),
        GameDefinition(
            id = "mastermind",
            title = "Mastermind",
            subtitle = "Crack the 4-peg hidden code",
            category = GameCategory.CASUAL,
            icon = Icons.Rounded.Psychology,
            minPlayers = 1, maxPlayers = 2,
            supportsPassAndPlay = true, supportsAi = false, supportsSolo = true,
            requiresPrivacyByDefault = true,
            estimatedMinutes = 6,
            rulesSummary = "Deduce the secret 4-symbol code in 10 tries using exact and partial match clues."
        ),
        GameDefinition(
            id = "nim",
            title = "Nim",
            subtitle = "Mathematical heap strategy",
            category = GameCategory.CASUAL,
            icon = Icons.Rounded.SortByAlpha,
            minPlayers = 2, maxPlayers = 2,
            supportsPassAndPlay = true, supportsAi = true, supportsSolo = false,
            ruleVariants = listOf("Normal Play", "Misère"),
            estimatedMinutes = 4,
            rulesSummary = "Take 1 or more tokens from a single heap per turn. Master XOR parity to outsmart the AI."
        ),
        GameDefinition(
            id = "memory_match",
            title = "Memory Match",
            subtitle = "Solo best moves or 2–4 player duel",
            category = GameCategory.CASUAL,
            icon = Icons.Rounded.Memory,
            minPlayers = 1, maxPlayers = 4,
            supportsPassAndPlay = true, supportsAi = false, supportsSolo = true,
            ruleVariants = listOf("4×4 (8 Pairs)", "4×6 (12 Pairs)"),
            estimatedMinutes = 5,
            rulesSummary = "Flip two cards per turn to locate matching Mono symbol pairs. Matches grant a streak turn."
        ),

        // ==================== PUZZLE GAMES ====================
        GameDefinition(
            id = "sudoku",
            title = "Sudoku",
            subtitle = "4 difficulties, notes, hints & records",
            category = GameCategory.PUZZLE,
            icon = Icons.Rounded.Grid4x4,
            minPlayers = 1, maxPlayers = 1,
            supportsPassAndPlay = false, supportsAi = false, supportsSolo = true,
            ruleVariants = listOf("Easy", "Medium", "Hard", "Expert"),
            isPopular = true, isFeatured = true, estimatedMinutes = 12,
            rulesSummary = "Fill the 9×9 grid so every row, column, and 3×3 box contains digits 1–9 without repetition."
        ),
        GameDefinition(
            id = "killer_sudoku",
            title = "Killer Sudoku",
            subtitle = "Sum cages meet 9×9 Sudoku",
            category = GameCategory.PUZZLE,
            icon = Icons.Rounded.Calculate,
            minPlayers = 1, maxPlayers = 1,
            supportsPassAndPlay = false, supportsAi = false, supportsSolo = true,
            ruleVariants = listOf("Standard", "Challenge"),
            estimatedMinutes = 18,
            rulesSummary = "Follow classic Sudoku rules while ensuring digits inside every dashed cage sum to its corner target."
        ),
        GameDefinition(
            id = "word_search",
            title = "Word Search",
            subtitle = "8-direction themed offline grids",
            category = GameCategory.PUZZLE,
            icon = Icons.Rounded.Search,
            minPlayers = 1, maxPlayers = 1,
            supportsPassAndPlay = false, supportsAi = false, supportsSolo = true,
            estimatedMinutes = 7,
            rulesSummary = "Locate all hidden words forward, backward, or diagonally inside the 10×10 letter matrix."
        ),
        GameDefinition(
            id = "crossword",
            title = "Crossword",
            subtitle = "Curated offline mini & standard grids",
            category = GameCategory.PUZZLE,
            icon = Icons.Rounded.EditNote,
            minPlayers = 1, maxPlayers = 1,
            supportsPassAndPlay = false, supportsAi = false, supportsSolo = true,
            estimatedMinutes = 10,
            rulesSummary = "Solve Across and Down clues to complete every intersecting word in the monochrome crossword."
        ),
        GameDefinition(
            id = "nonogram",
            title = "Nonogram",
            subtitle = "Logic picture grid picross",
            category = GameCategory.PUZZLE,
            icon = Icons.Rounded.Apps,
            minPlayers = 1, maxPlayers = 1,
            supportsPassAndPlay = false, supportsAi = false, supportsSolo = true,
            ruleVariants = listOf("5×5", "8×8", "10×10"),
            estimatedMinutes = 8,
            rulesSummary = "Use numeric run-length clues on rows and columns to reveal the hidden monochrome pixel pattern."
        ),
        GameDefinition(
            id = "minesweeper",
            title = "Minesweeper",
            subtitle = "Safe first tap, flags & chord reveal",
            category = GameCategory.PUZZLE,
            icon = Icons.Rounded.Dangerous,
            minPlayers = 1, maxPlayers = 1,
            supportsPassAndPlay = false, supportsAi = false, supportsSolo = true,
            ruleVariants = listOf("9×9 (10 Mines)", "12×12 (22 Mines)", "14×14 (32 Mines)"),
            isPopular = true, estimatedMinutes = 6,
            rulesSummary = "Clear all safe cells using adjacent mine counts without detonating a hidden mine."
        ),
        GameDefinition(
            id = "game_2048",
            title = "2048",
            subtitle = "Swipe merges, undo & high score",
            category = GameCategory.PUZZLE,
            icon = Icons.Rounded.LooksOne,
            minPlayers = 1, maxPlayers = 1,
            supportsPassAndPlay = false, supportsAi = false, supportsSolo = true,
            isPopular = true, isFeatured = true, estimatedMinutes = 8,
            rulesSummary = "Slide tiles in four directions to combine matching powers of two until you forge the 2048 tile."
        ),
        GameDefinition(
            id = "sliding_puzzle",
            title = "Sliding Puzzle",
            subtitle = "3×3 & 4×4 solvable tile shuffles",
            category = GameCategory.PUZZLE,
            icon = Icons.Rounded.Extension,
            minPlayers = 1, maxPlayers = 1,
            supportsPassAndPlay = false, supportsAi = false, supportsSolo = true,
            ruleVariants = listOf("3×3 (8-Puzzle)", "4×4 (15-Puzzle)"),
            estimatedMinutes = 5,
            rulesSummary = "Slide numbered tiles into the blank slot until they align in ascending numerical order."
        ),
        GameDefinition(
            id = "kakuro",
            title = "Kakuro",
            subtitle = "Cross-sum numeric logic grid",
            category = GameCategory.PUZZLE,
            icon = Icons.Rounded.SquareFoot,
            minPlayers = 1, maxPlayers = 1,
            supportsPassAndPlay = false, supportsAi = false, supportsSolo = true,
            estimatedMinutes = 12,
            rulesSummary = "Fill runs with unique digits 1–9 that sum to each horizontal and vertical clue."
        ),
        GameDefinition(
            id = "kenken",
            title = "KenKen",
            subtitle = "Arithmetic Latin square cages",
            category = GameCategory.PUZZLE,
            icon = Icons.Rounded.Numbers,
            minPlayers = 1, maxPlayers = 1,
            supportsPassAndPlay = false, supportsAi = false, supportsSolo = true,
            ruleVariants = listOf("4×4", "5×5"),
            estimatedMinutes = 9,
            rulesSummary = "Fill each row and column with unique digits while satisfying every cage's +, −, ×, or ÷ target."
        ),
        GameDefinition(
            id = "number_match",
            title = "Number Match",
            subtitle = "Clear equal & sum-to-10 pairs",
            category = GameCategory.PUZZLE,
            icon = Icons.Rounded.Schedule,
            minPlayers = 1, maxPlayers = 1,
            supportsPassAndPlay = false, supportsAi = false, supportsSolo = true,
            estimatedMinutes = 7,
            rulesSummary = "Match pairs of identical digits or digits summing to 10 across open lines to clear the board."
        ),
        GameDefinition(
            id = "jigsaw",
            title = "Jigsaw Puzzle",
            subtitle = "Geometric monochrome tile assembly",
            category = GameCategory.PUZZLE,
            icon = Icons.Rounded.Extension,
            minPlayers = 1, maxPlayers = 1,
            supportsPassAndPlay = false, supportsAi = false, supportsSolo = true,
            ruleVariants = listOf("4×4", "5×5"),
            estimatedMinutes = 6,
            rulesSummary = "Swap interlocking geometric artwork tiles until the full monochrome composition locks into place."
        ),

        // ==================== CARD GAMES ====================
        GameDefinition(
            id = "solitaire",
            title = "Solitaire (Klondike)",
            subtitle = "Draw 1/3, foundations & undo",
            category = GameCategory.CARDS,
            icon = Icons.Rounded.Style,
            minPlayers = 1, maxPlayers = 1,
            supportsPassAndPlay = false, supportsAi = false, supportsSolo = true,
            ruleVariants = listOf("Draw 1", "Draw 3"),
            isPopular = true, estimatedMinutes = 10,
            rulesSummary = "Build four suited foundations Ace-to-King by organizing alternating-color descending tableau columns."
        ),
        GameDefinition(
            id = "spider_solitaire",
            title = "Spider Solitaire",
            subtitle = "1-suit & 2-suit 10-column runs",
            category = GameCategory.CARDS,
            icon = Icons.Rounded.Layers,
            minPlayers = 1, maxPlayers = 1,
            supportsPassAndPlay = false, supportsAi = false, supportsSolo = true,
            ruleVariants = listOf("1 Suit", "2 Suits"),
            estimatedMinutes = 15,
            rulesSummary = "Assemble complete King-down-to-Ace suited runs across 10 columns to remove all 8 sets."
        ),
        GameDefinition(
            id = "freecell",
            title = "FreeCell",
            subtitle = "4 open cells & pure skill deals",
            category = GameCategory.CARDS,
            icon = Icons.Rounded.ViewModule,
            minPlayers = 1, maxPlayers = 1,
            supportsPassAndPlay = false, supportsAi = false, supportsSolo = true,
            estimatedMinutes = 12,
            rulesSummary = "Use the four temporary free cells strategically to move all 52 face-up cards to the foundations."
        ),
        GameDefinition(
            id = "hearts",
            title = "Hearts",
            subtitle = "Avoid Hearts & Q♠ in 4-seat play",
            category = GameCategory.CARDS,
            icon = Icons.Rounded.Style,
            minPlayers = 4, maxPlayers = 4,
            supportsPassAndPlay = true, supportsAi = true, supportsSolo = false,
            requiresPrivacyByDefault = true,
            estimatedMinutes = 15,
            rulesSummary = "Avoid taking Hearts (1 pt) and the Queen of Spades (13 pts), or shoot the moon by taking all 26."
        ),
        GameDefinition(
            id = "spades",
            title = "Spades",
            subtitle = "Bidding, Spades trump & partnerships",
            category = GameCategory.CARDS,
            icon = Icons.Rounded.Style,
            minPlayers = 4, maxPlayers = 4,
            supportsPassAndPlay = true, supportsAi = true, supportsSolo = false,
            requiresPrivacyByDefault = true,
            estimatedMinutes = 15,
            rulesSummary = "Bid tricks before each round, follow suit, and trump with Spades to meet your contract."
        ),
        GameDefinition(
            id = "crazy_eights",
            title = "Crazy Eights",
            subtitle = "Wild 8s, suit shifts & Privacy hands",
            category = GameCategory.CARDS,
            icon = Icons.Rounded.Shuffle,
            minPlayers = 2, maxPlayers = 4,
            supportsPassAndPlay = true, supportsAi = true, supportsSolo = false,
            requiresPrivacyByDefault = true,
            estimatedMinutes = 8,
            rulesSummary = "Match the discard pile's suit or rank. Play an 8 anytime to declare the next active suit."
        ),
        GameDefinition(
            id = "go_fish",
            title = "Go Fish",
            subtitle = "Ask rivals for ranks & form books",
            category = GameCategory.CARDS,
            icon = Icons.Rounded.HelpOutline,
            minPlayers = 2, maxPlayers = 4,
            supportsPassAndPlay = true, supportsAi = true, supportsSolo = false,
            requiresPrivacyByDefault = true,
            estimatedMinutes = 8,
            rulesSummary = "Request card ranks from opponents to complete 4-of-a-kind books. Draw from the pond if they say Go Fish."
        ),
        GameDefinition(
            id = "war",
            title = "War",
            subtitle = "Rapid deck clashes & 3-card wars",
            category = GameCategory.CARDS,
            icon = Icons.Rounded.CompareArrows,
            minPlayers = 2, maxPlayers = 2,
            supportsPassAndPlay = true, supportsAi = true, supportsSolo = false,
            estimatedMinutes = 6,
            rulesSummary = "Flip top cards simultaneously; higher rank wins the pair. Ties ignite a 3-card War."
        ),
        GameDefinition(
            id = "blackjack",
            title = "Blackjack",
            subtitle = "Hit, Stand, Double vs Dealer",
            category = GameCategory.CARDS,
            icon = Icons.Rounded.Casino,
            minPlayers = 1, maxPlayers = 1,
            supportsPassAndPlay = false, supportsAi = false, supportsSolo = true,
            estimatedMinutes = 5,
            rulesSummary = "Beat the dealer's hand without exceeding 21 using offline chip wagers, Hit, Stand, and Double Down."
        ),

        // ==================== WORD & TRIVIA GAMES ====================
        GameDefinition(
            id = "word_scramble",
            title = "Word Scramble",
            subtitle = "Unscramble offline words & streaks",
            category = GameCategory.WORD_TRIVIA,
            icon = Icons.Rounded.Shuffle,
            minPlayers = 1, maxPlayers = 1,
            supportsPassAndPlay = false, supportsAi = false, supportsSolo = true,
            estimatedMinutes = 5,
            rulesSummary = "Rearrange scrambled letters to uncover the hidden word and extend your streak."
        ),
        GameDefinition(
            id = "anagrams",
            title = "Anagrams",
            subtitle = "Build sub-words from 7-letter racks",
            category = GameCategory.WORD_TRIVIA,
            icon = Icons.Rounded.AutoStories,
            minPlayers = 1, maxPlayers = 1,
            supportsPassAndPlay = false, supportsAi = false, supportsSolo = true,
            estimatedMinutes = 5,
            rulesSummary = "Discover as many valid offline dictionary words as possible from a 7-letter rack."
        ),
        GameDefinition(
            id = "trivia",
            title = "General Trivia",
            subtitle = "100+ offline questions, Solo & 2–4P",
            category = GameCategory.WORD_TRIVIA,
            icon = Icons.Rounded.Quiz,
            minPlayers = 1, maxPlayers = 4,
            supportsPassAndPlay = true, supportsAi = false, supportsSolo = true,
            isPopular = true, estimatedMinutes = 8,
            rulesSummary = "Test knowledge across Science, History, Geography, Arts, and Logic solo or in Pass & Play."
        ),
        GameDefinition(
            id = "quiz_challenge",
            title = "Quiz Challenge",
            subtitle = "Timed 10-round multiplier blitz",
            category = GameCategory.WORD_TRIVIA,
            icon = Icons.Rounded.Lightbulb,
            minPlayers = 1, maxPlayers = 1,
            supportsPassAndPlay = false, supportsAi = false, supportsSolo = true,
            estimatedMinutes = 4,
            rulesSummary = "Answer 10 rapid-fire questions before the timer expires to stack streak multipliers."
        ),
        GameDefinition(
            id = "daily_word",
            title = "Daily Word Challenge",
            subtitle = "Deterministic daily 5-letter deduction",
            category = GameCategory.WORD_TRIVIA,
            icon = Icons.Rounded.Spellcheck,
            minPlayers = 1, maxPlayers = 1,
            supportsPassAndPlay = false, supportsAi = false, supportsSolo = true,
            ruleVariants = listOf("Daily Puzzle", "Endless Practice"),
            isFeatured = true, estimatedMinutes = 4,
            rulesSummary = "Deduce the 5-letter word in 6 attempts using exact-position and letter-present clues."
        )
    )

    fun byId(id: String): GameDefinition =
        allGames.firstOrNull { it.id == id } ?: allGames.first()

    fun byCategory(category: GameCategory): List<GameDefinition> =
        if (category == GameCategory.WORD_TRIVIA) {
            // Also surface Crossword under Word & Trivia as listed in spec
            allGames.filter { it.category == GameCategory.WORD_TRIVIA || it.id == "crossword" }
        } else {
            allGames.filter { it.category == category }
        }

    val passAndPlayGames: List<GameDefinition>
        get() = allGames.filter { it.supportsPassAndPlay }

    val aiGames: List<GameDefinition>
        get() = allGames.filter { it.supportsAi }
}

private object TrendingUpIconHolder {
    val icon: ImageVector = Icons.Rounded.LinearScale
}
