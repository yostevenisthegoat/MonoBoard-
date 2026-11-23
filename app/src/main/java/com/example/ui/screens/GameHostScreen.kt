package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.example.data.db.GameStatEntity
import com.example.data.prefs.AppSettings
import com.example.data.repo.MatchOutcome
import com.example.data.sound.SoundCue
import com.example.ui.ActiveGameSession
import com.example.ui.games.cards.BlackjackScreen
import com.example.ui.games.cards.CrazyEightsScreen
import com.example.ui.games.cards.FreeCellScreen
import com.example.ui.games.cards.GoFishScreen
import com.example.ui.games.cards.KlondikeScreen
import com.example.ui.games.cards.SpiderSolitaireScreen
import com.example.ui.games.cards.TrickTakingScreen
import com.example.ui.games.cards.WarScreen
import com.example.ui.games.checkers.CheckersScreen
import com.example.ui.games.chess.ChessScreen
import com.example.ui.games.classic.BattleshipScreen
import com.example.ui.games.classic.ConnectFourScreen
import com.example.ui.games.classic.DotsBoxesScreen
import com.example.ui.games.classic.HangmanScreen
import com.example.ui.games.classic.MastermindScreen
import com.example.ui.games.classic.MemoryMatchScreen
import com.example.ui.games.classic.NimScreen
import com.example.ui.games.classic.RpsScreen
import com.example.ui.games.classic.TicTacToeScreen
import com.example.ui.games.puzzles.CrosswordScreen
import com.example.ui.games.puzzles.Game2048Screen
import com.example.ui.games.puzzles.JigsawScreen
import com.example.ui.games.puzzles.KakuroScreen
import com.example.ui.games.puzzles.KenKenScreen
import com.example.ui.games.puzzles.MinesweeperScreen
import com.example.ui.games.puzzles.NonogramScreen
import com.example.ui.games.puzzles.NumberMatchScreen
import com.example.ui.games.puzzles.SlidingPuzzleScreen
import com.example.ui.games.puzzles.WordSearchScreen
import com.example.ui.games.snakesladders.SnakesLaddersScreen
import com.example.ui.games.strategy.BackgammonScreen
import com.example.ui.games.strategy.CarromScreen
import com.example.ui.games.strategy.ChineseCheckersScreen
import com.example.ui.games.strategy.DominoesScreen
import com.example.ui.games.strategy.GoScreen
import com.example.ui.games.strategy.LudoScreen
import com.example.ui.games.strategy.MahjongScreen
import com.example.ui.games.strategy.MancalaScreen
import com.example.ui.games.strategy.NineMensMorrisScreen
import com.example.ui.games.strategy.ReversiScreen
import com.example.ui.games.strategy.ShogiScreen
import com.example.ui.games.strategy.XiangqiScreen
import com.example.ui.games.sudoku.SudokuScreen
import com.example.ui.games.words.AnagramsScreen
import com.example.ui.games.words.DailyWordScreen
import com.example.ui.games.words.TriviaScreen
import com.example.ui.games.words.WordScrambleScreen

@Composable
fun GameHostScreen(
    session: ActiveGameSession,
    settings: AppSettings,
    stat: GameStatEntity?,
    onToggleFavorite: (String) -> Unit,
    onSaveState: (String, String, String, String) -> Unit,
    onClearSaved: (String) -> Unit,
    onRecordOutcome: (String, String, MatchOutcome, String, String, Int, Int) -> Unit,
    onPlayCue: (SoundCue) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val cfg = session.config
    val gameId = cfg.gameId
    val isFav = stat?.isFavorite == true
    val modeLabel = cfg.playMode.label

    val toggleFav = { onToggleFavorite(gameId) }
    val record: (MatchOutcome, String, String, Int, Int) -> Unit = { outcome, winner, summary, score, duration ->
        onRecordOutcome(gameId, modeLabel, outcome, winner, summary, score, duration)
    }

    when (gameId) {
        // Strategy & Board
        "chess" -> ChessScreen(
            sessionConfig = cfg,
            settings = settings,
            initialSerializedState = session.initialSerializedState,
            isFavorite = isFav,
            onToggleFavorite = toggleFav,
            onSaveState = { summary, json -> onSaveState(gameId, modeLabel, summary, json) },
            onClearSaved = { onClearSaved(gameId) },
            onRecordOutcome = record,
            onPlayCue = onPlayCue,
            onBack = onBack
        )
        "checkers" -> CheckersScreen(
            sessionConfig = cfg,
            settings = settings,
            initialSerializedState = session.initialSerializedState,
            isFavorite = isFav,
            onToggleFavorite = toggleFav,
            onSaveState = { summary, json -> onSaveState(gameId, modeLabel, summary, json) },
            onClearSaved = { onClearSaved(gameId) },
            onRecordOutcome = record,
            onPlayCue = onPlayCue,
            onBack = onBack
        )
        "chinese_checkers" -> ChineseCheckersScreen(cfg, isFav, toggleFav, record, onPlayCue, onBack)
        "go" -> GoScreen(cfg, isFav, toggleFav, record, onPlayCue, onBack)
        "reversi" -> ReversiScreen(cfg, settings, isFav, toggleFav, record, onPlayCue, onBack)
        "backgammon" -> BackgammonScreen(cfg, isFav, toggleFav, record, onPlayCue, onBack)
        "mancala" -> MancalaScreen(cfg, isFav, toggleFav, record, onPlayCue, onBack)
        "nine_mens_morris" -> NineMensMorrisScreen(cfg, isFav, toggleFav, record, onPlayCue, onBack)
        "shogi" -> ShogiScreen(cfg, isFav, toggleFav, record, onPlayCue, onBack)
        "xiangqi" -> XiangqiScreen(cfg, isFav, toggleFav, record, onPlayCue, onBack)
        "ludo" -> LudoScreen(cfg, isFav, toggleFav, record, onPlayCue, onBack)
        "snakes_ladders" -> SnakesLaddersScreen(cfg, isFav, toggleFav, record, onPlayCue, onBack)
        "carrom" -> CarromScreen(cfg, isFav, toggleFav, record, onPlayCue, onBack)
        "dominoes" -> DominoesScreen(cfg, isFav, toggleFav, record, onPlayCue, onBack)
        "mahjong" -> MahjongScreen(isFav, toggleFav, record, onPlayCue, onBack)

        // Classic Casual
        "tic_tac_toe" -> TicTacToeScreen(cfg, isFav, toggleFav, record, onPlayCue, onBack)
        "connect_four" -> ConnectFourScreen(cfg, isFav, toggleFav, record, onPlayCue, onBack)
        "dots_boxes" -> DotsBoxesScreen(cfg, isFav, toggleFav, record, onPlayCue, onBack)
        "battleship" -> BattleshipScreen(cfg, isFav, toggleFav, record, onPlayCue, onBack)
        "rps" -> RpsScreen(cfg, isFav, toggleFav, record, onPlayCue, onBack)
        "hangman" -> HangmanScreen(cfg, isFav, toggleFav, record, onPlayCue, onBack)
        "mastermind" -> MastermindScreen(cfg, isFav, toggleFav, record, onPlayCue, onBack)
        "nim" -> NimScreen(cfg, isFav, toggleFav, record, onPlayCue, onBack)
        "memory_match" -> MemoryMatchScreen(cfg, isFav, toggleFav, record, onPlayCue, onBack)

        // Puzzle Games
        "sudoku", "killer_sudoku" -> SudokuScreen(gameId, cfg, isFav, toggleFav, record, onPlayCue, onBack)
        "word_search" -> WordSearchScreen(isFav, toggleFav, record, onPlayCue, onBack)
        "crossword" -> CrosswordScreen(isFav, toggleFav, record, onPlayCue, onBack)
        "nonogram" -> NonogramScreen(cfg, isFav, toggleFav, record, onPlayCue, onBack)
        "minesweeper" -> MinesweeperScreen(cfg, isFav, toggleFav, record, onPlayCue, onBack)
        "game_2048" -> Game2048Screen(stat?.bestScore ?: 0, isFav, toggleFav, record, onPlayCue, onBack)
        "sliding_puzzle" -> SlidingPuzzleScreen(cfg, isFav, toggleFav, record, onPlayCue, onBack)
        "kakuro" -> KakuroScreen(isFav, toggleFav, record, onPlayCue, onBack)
        "kenken" -> KenKenScreen(cfg, isFav, toggleFav, record, onPlayCue, onBack)
        "number_match" -> NumberMatchScreen(isFav, toggleFav, record, onPlayCue, onBack)
        "jigsaw" -> JigsawScreen(cfg, isFav, toggleFav, record, onPlayCue, onBack)

        // Card Games
        "solitaire" -> KlondikeScreen(cfg, isFav, toggleFav, record, onPlayCue, onBack)
        "spider_solitaire" -> SpiderSolitaireScreen(cfg, isFav, toggleFav, record, onPlayCue, onBack)
        "freecell" -> FreeCellScreen(isFav, toggleFav, record, onPlayCue, onBack)
        "hearts", "spades" -> TrickTakingScreen(gameId, cfg, isFav, toggleFav, record, onPlayCue, onBack)
        "crazy_eights" -> CrazyEightsScreen(cfg, isFav, toggleFav, record, onPlayCue, onBack)
        "go_fish" -> GoFishScreen(cfg, isFav, toggleFav, record, onPlayCue, onBack)
        "war" -> WarScreen(cfg, isFav, toggleFav, record, onPlayCue, onBack)
        "blackjack" -> BlackjackScreen(isFav, toggleFav, record, onPlayCue, onBack)

        // Word & Trivia
        "word_scramble" -> WordScrambleScreen(isFav, toggleFav, record, onPlayCue, onBack)
        "anagrams" -> AnagramsScreen(isFav, toggleFav, record, onPlayCue, onBack)
        "trivia", "quiz_challenge" -> TriviaScreen(gameId, cfg, isFav, toggleFav, record, onPlayCue, onBack)
        "daily_word" -> DailyWordScreen(cfg, isFav, toggleFav, record, onPlayCue, onBack)
    }
}
