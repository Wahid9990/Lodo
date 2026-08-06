package com.example.model

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PlayerColor(
    val displayName: String,
    val hexColor: Long,
    val accentHexColor: Long
) {
    RED("Red", 0xFFE53935, 0xFFFF8A80),
    GREEN("Green", 0xFF43A047, 0xFFB9F6CA),
    YELLOW("Yellow", 0xFFFDD835, 0xFFFFD180),
    BLUE("Blue", 0xFF1E88E5, 0xFF82B1FF)
}

enum class PlayerType {
    HUMAN,
    AI
}

enum class AiDifficulty {
    EASY,
    MEDIUM,
    HARD
}

enum class GameStatus {
    NOT_STARTED,
    IN_PROGRESS,
    PAUSED,
    FINISHED
}

enum class BoardTheme {
    CLASSIC,
    NEON,
    WOODEN,
    PASTEL
}

data class Token(
    val id: Int, // 0..3 for player tokens
    val color: PlayerColor,
    val relativePosition: Int = -1, // -1: Yard, 0..50: Common Track, 51..55: Home Stretch, 56: Home Finish
    val initialYardIndex: Int // 0..3 position inside home yard
) {
    val isInYard: Boolean get() = relativePosition == -1
    val isFinished: Boolean get() = relativePosition >= 56
    val isInHomeStretch: Boolean get() = relativePosition in 51..55
    val isOnCommonTrack: Boolean get() = relativePosition in 0..50
}

data class Player(
    val color: PlayerColor,
    val name: String,
    val type: PlayerType,
    val difficulty: AiDifficulty = AiDifficulty.MEDIUM,
    val tokens: List<Token> = List(4) { id ->
        Token(id = id, color = color, relativePosition = -1, initialYardIndex = id)
    },
    val rank: Int = 0 // 0 if not finished yet, 1 for 1st place, 2 for 2nd, etc.
) {
    val isFinished: Boolean get() = tokens.all { it.isFinished }
    val finishedTokensCount: Int get() = tokens.count { it.isFinished }
}

data class GameState(
    val players: List<Player> = emptyList(),
    val activePlayerIndex: Int = 0,
    val diceValue: Int = 1,
    val isDiceRolled: Boolean = false,
    val consecutiveSixes: Int = 0,
    val status: GameStatus = GameStatus.NOT_STARTED,
    val winners: List<PlayerColor> = emptyList(),
    val movableTokenIds: List<Int> = emptyList(),
    val lastActionMessage: String = "Welcome to Ludo Master! Roll dice to start.",
    val selectedPlayerCount: Int = 4,
    val isAutoPlaying: Boolean = false,
    val turnTimestamp: Long = System.currentTimeMillis()
) {
    val activePlayer: Player? get() = players.getOrNull(activePlayerIndex)
}

data class GameSettings(
    val soundFxEnabled: Boolean = true,
    val bgMusicEnabled: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val aiSpeedMs: Long = 600L,
    val isDarkMode: Boolean = true,
    val boardTheme: BoardTheme = BoardTheme.CLASSIC
)

@Entity(tableName = "game_save")
data class GameSaveEntity(
    @PrimaryKey val id: Int = 1,
    val gameStateJson: String,
    val updatedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "game_history")
data class GameHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val modeText: String,
    val playerCount: Int,
    val winnerColor: String,
    val winnerName: String,
    val durationSeconds: Long,
    val timestamp: Long = System.currentTimeMillis()
)
