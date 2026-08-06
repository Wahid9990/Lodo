package com.example.logic

import com.example.model.AiDifficulty
import com.example.model.GameState
import com.example.model.GameStatus
import com.example.model.Player
import com.example.model.PlayerColor
import com.example.model.PlayerType
import com.example.model.Token
import kotlin.random.Random

object LudoEngine {

    fun createInitialGame(
        playerCount: Int,
        playerTypes: Map<PlayerColor, PlayerType>,
        aiDifficulties: Map<PlayerColor, AiDifficulty>
    ): GameState {
        val activeColors = when (playerCount) {
            2 -> listOf(PlayerColor.RED, PlayerColor.YELLOW)
            3 -> listOf(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW)
            else -> listOf(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE)
        }

        val players = activeColors.map { color ->
            Player(
                color = color,
                name = when (playerTypes[color]) {
                    PlayerType.HUMAN -> "${color.displayName} Player"
                    else -> "${color.displayName} AI (${aiDifficulties[color] ?: AiDifficulty.MEDIUM})"
                },
                type = playerTypes[color] ?: PlayerType.HUMAN,
                difficulty = aiDifficulties[color] ?: AiDifficulty.MEDIUM,
                tokens = List(4) { id ->
                    Token(id = id, color = color, relativePosition = -1, initialYardIndex = id)
                }
            )
        }

        return GameState(
            players = players,
            activePlayerIndex = 0,
            diceValue = 1,
            isDiceRolled = false,
            consecutiveSixes = 0,
            status = GameStatus.IN_PROGRESS,
            winners = emptyList(),
            movableTokenIds = emptyList(),
            lastActionMessage = "${players.firstOrNull()?.name}'s turn to roll!"
        )
    }

    fun rollDice(currentState: GameState, overrideValue: Int? = null): GameState {
        if (currentState.status != GameStatus.IN_PROGRESS || currentState.isDiceRolled) {
            return currentState
        }

        val roll = overrideValue ?: Random.nextInt(1, 7)
        val activePlayer = currentState.activePlayer ?: return currentState

        val newSixes = if (roll == 6) currentState.consecutiveSixes + 1 else 0

        // 3 consecutive sixes rule -> lose turn immediately
        if (newSixes >= 3) {
            val nextState = passTurn(
                currentState.copy(
                    diceValue = roll,
                    isDiceRolled = true,
                    consecutiveSixes = 0,
                    lastActionMessage = "${activePlayer.name} rolled three 6s in a row! Turn forfeited."
                )
            )
            return nextState
        }

        val movable = getMovableTokenIds(activePlayer, roll)

        val updatedState = currentState.copy(
            diceValue = roll,
            isDiceRolled = true,
            consecutiveSixes = newSixes,
            movableTokenIds = movable,
            lastActionMessage = if (movable.isEmpty()) {
                "${activePlayer.name} rolled $roll. No valid moves available."
            } else {
                "${activePlayer.name} rolled $roll. Pick a token to move!"
            }
        )

        // If no valid moves, automatically pass turn
        if (movable.isEmpty()) {
            return passTurn(updatedState)
        }

        return updatedState
    }

    fun getMovableTokenIds(player: Player, diceRoll: Int): List<Int> {
        return player.tokens.filter { canMoveToken(it, diceRoll) }.map { it.id }
    }

    fun canMoveToken(token: Token, diceRoll: Int): Boolean {
        if (token.isFinished) return false
        if (token.isInYard) {
            return diceRoll == 6
        }
        val targetPos = token.relativePosition + diceRoll
        return targetPos <= 56
    }

    fun moveToken(currentState: GameState, tokenId: Int): GameState {
        if (!currentState.isDiceRolled || currentState.status != GameStatus.IN_PROGRESS) {
            return currentState
        }

        val activePlayer = currentState.activePlayer ?: return currentState
        val tokenToMove = activePlayer.tokens.find { it.id == tokenId } ?: return currentState

        if (!canMoveToken(tokenToMove, currentState.diceValue)) {
            return currentState
        }

        val dice = currentState.diceValue
        val oldPos = tokenToMove.relativePosition
        val newPos = if (tokenToMove.isInYard) 0 else oldPos + dice

        val updatedToken = tokenToMove.copy(relativePosition = newPos)

        var capturedTokenMessage: String? = null
        var isCapture = false

        // Check capture on common track (0..50)
        val allPlayers = currentState.players.map { player ->
            if (player.color == activePlayer.color) {
                // Update active player's tokens
                player.copy(tokens = player.tokens.map { if (it.id == tokenId) updatedToken else it })
            } else {
                // Check if opponent token is captured
                if (newPos in 0..50) {
                    val myGlobalPos = (LudoBoardMapper.getColorStartOffset(activePlayer.color) + newPos) % 52
                    val isSafe = LudoBoardMapper.isGlobalIndexSafe(myGlobalPos)

                    if (!isSafe) {
                        val newTokens = player.tokens.map { oppToken ->
                            if (oppToken.isOnCommonTrack) {
                                val oppGlobalPos = (LudoBoardMapper.getColorStartOffset(player.color) + oppToken.relativePosition) % 52
                                if (oppGlobalPos == myGlobalPos) {
                                    isCapture = true
                                    capturedTokenMessage = "${activePlayer.name} captured ${player.name}'s token!"
                                    oppToken.copy(relativePosition = -1) // Send back home
                                } else oppToken
                            } else oppToken
                        }
                        player.copy(tokens = newTokens)
                    } else player
                } else player
            }
        }

        // Check if player finished or won
        val currentUpdatedPlayer = allPlayers.find { it.color == activePlayer.color }!!
        val isFinishedNow = currentUpdatedPlayer.tokens.all { it.isFinished }
        var updatedWinners = currentState.winners.toMutableList()

        if (isFinishedNow && activePlayer.color !in updatedWinners) {
            updatedWinners.add(activePlayer.color)
        }

        // Check extra turn conditions:
        // 1. Rolled a 6
        // 2. Captured opponent token
        // 3. Reached home finish (newPos == 56)
        val reachedFinish = (oldPos != 56 && newPos == 56)
        val getsExtraTurn = (dice == 6 || isCapture || reachedFinish) && !isFinishedNow

        val nextStatus = if (updatedWinners.size >= currentState.players.size - 1 || (currentState.players.size == 2 && updatedWinners.isNotEmpty())) {
            GameStatus.FINISHED
        } else {
            GameStatus.IN_PROGRESS
        }

        var msg = when {
            capturedTokenMessage != null -> capturedTokenMessage!!
            reachedFinish -> "${activePlayer.name} brought a token HOME!"
            dice == 6 -> "${activePlayer.name} rolled a 6 and gets an extra turn!"
            else -> "${activePlayer.name} moved token."
        }

        if (nextStatus == GameStatus.FINISHED) {
            val winnerName = allPlayers.find { it.color == updatedWinners.firstOrNull() }?.name ?: "Winner"
            msg = "🏆 $winnerName won the game!"
        }

        val postMoveState = currentState.copy(
            players = allPlayers,
            isDiceRolled = false,
            movableTokenIds = emptyList(),
            winners = updatedWinners,
            status = nextStatus,
            lastActionMessage = msg
        )

        if (nextStatus == GameStatus.FINISHED) {
            return postMoveState
        }

        if (getsExtraTurn) {
            // Keep same active player, reset dice rolled
            return postMoveState.copy(
                isDiceRolled = false,
                lastActionMessage = "$msg Roll again!"
            )
        } else {
            // Pass turn to next active player
            return passTurn(postMoveState)
        }
    }

    fun passTurn(state: GameState): GameState {
        val totalPlayers = state.players.size
        var nextIdx = (state.activePlayerIndex + 1) % totalPlayers

        // Skip finished players
        var attempts = 0
        while (attempts < totalPlayers && state.players[nextIdx].isFinished) {
            nextIdx = (nextIdx + 1) % totalPlayers
            attempts++
        }

        val nextPlayer = state.players[nextIdx]
        return state.copy(
            activePlayerIndex = nextIdx,
            isDiceRolled = false,
            consecutiveSixes = 0,
            movableTokenIds = emptyList(),
            lastActionMessage = "${nextPlayer.name}'s turn to roll!"
        )
    }

    // AI Decision Maker
    fun chooseBestTokenForAi(state: GameState): Int? {
        val activePlayer = state.activePlayer ?: return null
        val movableIds = state.movableTokenIds
        if (movableIds.isEmpty()) return null
        if (movableIds.size == 1) return movableIds.first()

        return when (activePlayer.difficulty) {
            AiDifficulty.EASY -> movableIds.random()
            AiDifficulty.MEDIUM -> chooseMediumAiMove(state, activePlayer, movableIds)
            AiDifficulty.HARD -> chooseHardAiMove(state, activePlayer, movableIds)
        }
    }

    private fun chooseMediumAiMove(state: GameState, player: Player, movableIds: List<Int>): Int {
        val dice = state.diceValue

        // 1. Capture move
        for (id in movableIds) {
            val token = player.tokens.find { it.id == id } ?: continue
            if (causesCapture(state, player, token, dice)) return id
        }

        // 2. Reach Home
        for (id in movableIds) {
            val token = player.tokens.find { it.id == id } ?: continue
            if (token.relativePosition + dice == 56) return id
        }

        // 3. Leave yard if rolled 6
        if (dice == 6) {
            val yardToken = player.tokens.find { it.id in movableIds && it.isInYard }
            if (yardToken != null) return yardToken.id
        }

        // 4. Move to safe spot
        for (id in movableIds) {
            val token = player.tokens.find { it.id == id } ?: continue
            val newPos = token.relativePosition + dice
            if (LudoBoardMapper.isRelativePositionSafe(player.color, newPos)) return id
        }

        // 5. Advance furthest token
        return movableIds.maxByOrNull { id ->
            player.tokens.find { it.id == id }?.relativePosition ?: -1
        } ?: movableIds.first()
    }

    private fun chooseHardAiMove(state: GameState, player: Player, movableIds: List<Int>): Int {
        val dice = state.diceValue

        var bestId = movableIds.first()
        var maxScore = Double.NEGATIVE_INFINITY

        for (id in movableIds) {
            val token = player.tokens.find { it.id == id } ?: continue
            var score = 0.0

            val oldPos = token.relativePosition
            val newPos = if (token.isInYard) 0 else oldPos + dice

            // Exit yard bonus
            if (token.isInYard && dice == 6) score += 500.0

            // Reach Home Finish
            if (newPos == 56) score += 900.0

            // Capture opponent
            if (causesCapture(state, player, token, dice)) score += 1200.0

            // Reach Safe Tile
            if (LudoBoardMapper.isRelativePositionSafe(player.color, newPos)) score += 400.0

            // Distance bonus (favor tokens closer to goal)
            score += newPos * 10.0

            // Check if escaping danger
            if (!token.isInYard && !LudoBoardMapper.isRelativePositionSafe(player.color, oldPos)) {
                if (isUnderThreat(state, player, oldPos)) {
                    score += 350.0 // Great to escape!
                }
            }

            // Check if moving into new danger
            if (newPos in 0..50 && !LudoBoardMapper.isRelativePositionSafe(player.color, newPos)) {
                if (isUnderThreat(state, player, newPos)) {
                    score -= 300.0 // Avoid!
                }
            }

            if (score > maxScore) {
                maxScore = score
                bestId = id
            }
        }

        return bestId
    }

    private fun causesCapture(state: GameState, player: Player, token: Token, dice: Int): Boolean {
        val newPos = if (token.isInYard) 0 else token.relativePosition + dice
        if (newPos !in 0..50) return false

        val myGlobalPos = (LudoBoardMapper.getColorStartOffset(player.color) + newPos) % 52
        if (LudoBoardMapper.isGlobalIndexSafe(myGlobalPos)) return false

        return state.players.any { opp ->
            opp.color != player.color && opp.tokens.any { oppToken ->
                if (oppToken.isOnCommonTrack) {
                    val oppGlobal = (LudoBoardMapper.getColorStartOffset(opp.color) + oppToken.relativePosition) % 52
                    oppGlobal == myGlobalPos
                } else false
            }
        }
    }

    private fun isUnderThreat(state: GameState, player: Player, relativePos: Int): Boolean {
        if (relativePos !in 0..50) return false
        val myGlobalPos = (LudoBoardMapper.getColorStartOffset(player.color) + relativePos) % 52

        return state.players.any { opp ->
            opp.color != player.color && opp.tokens.any { oppToken ->
                if (oppToken.isOnCommonTrack) {
                    val oppGlobal = (LudoBoardMapper.getColorStartOffset(opp.color) + oppToken.relativePosition) % 52
                    val diff = (myGlobalPos - oppGlobal + 52) % 52
                    diff in 1..6
                } else false
            }
        }
    }
}
