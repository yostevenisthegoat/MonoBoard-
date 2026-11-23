# Contributing to MonoBoard

Thank you for helping improve **MonoBoard** — an open-source, 100% offline board game platform built with Kotlin and Jetpack Compose.

## Architecture Overview

- `app/src/main/java/com/example/domain/GameCatalog.kt` — Central registry of game metadata, player counts, AI/Pass & Play support, and rule summaries.
- `app/src/main/java/com/example/ui/games/*` — Self-contained game engines and Compose screens organized by genre (`chess`, `checkers`, `strategy`, `classic`, `puzzles`, `cards`, `words`, `sudoku`, `snakesladders`).
- `app/src/main/java/com/example/data/sound/SoundAndHapticsManager.kt` — Offline procedural PCM sound synthesis and haptic feedback waveforms.
- `app/src/main/java/com/example/ui/components/MotionEffects.kt` — Shared Compose animations (spring pops, bouncy presses, error shakes, 3D card flips, animated dice, confetti bursts).
- `app/src/main/java/com/example/ui/components/PrivacyCurtain.kt` — Universal Pass & Play turn-handoff privacy layer for hidden-hand games.

## Contributor Guidelines

1. **100% Offline Guarantee**: Do not add `INTERNET` permissions, analytics, ads, or remote calls.
2. **Mono Visual Identity**: Follow the minimalist black-and-grey Material 3 palette (`#080808`, `#1B1B1B`, `#242424`) across menus and chrome. Traditional board accents may appear inside active gameplay boards.
3. **Rule Accuracy & Tactile Feel**: Ensure all moves, wins, and AI turns obey official game rules and trigger appropriate `SoundCue` and motion feedback.
4. **Verify Locally**: Build before opening a pull request:
   ```bash
   gradle :app:assembleDebug
   ```

## License

By contributing to MonoBoard, you agree that your contributions will be licensed under the [MIT License](LICENSE).
