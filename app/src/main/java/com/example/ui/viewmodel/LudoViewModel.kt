package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEffectManager
import com.example.data.db.LudoRepository
import com.example.logic.LudoEngine
import com.example.model.AiDifficulty
import com.example.model.GameHistoryEntity
import com.example.model.GameSettings
import com.example.model.GameState
import com.example.model.GameStatus
import com.example.model.PlayerColor
import com.example.model.PlayerType
import com.example.network.GameStateJsonAdapter
import com.example.network.LudoNetworkManager
import com.example.network.NetworkEvent
import com.example.network.NetworkRoomState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LudoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LudoRepository(application)
    private val soundManager = SoundEffectManager(application)
    val networkManager = LudoNetworkManager(application)

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _settings = MutableStateFlow(GameSettings())
    val settings: StateFlow<GameSettings> = _settings.asStateFlow()

    val savedGame: StateFlow<GameState?> = repository.savedGameState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val gameHistory: StateFlow<List<GameHistoryEntity>> = repository.historyList
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Local Wi-Fi Network states
    val networkRoomState: StateFlow<NetworkRoomState> = networkManager.roomState

    private val _isNetworkMode = MutableStateFlow(false)
    val isNetworkMode: StateFlow<Boolean> = _isNetworkMode.asStateFlow()

    private val _myLocalNetworkColor = MutableStateFlow<PlayerColor?>(null)
    val myLocalNetworkColor: StateFlow<PlayerColor?> = _myLocalNetworkColor.asStateFlow()

    private var aiTurnJob: Job? = null
    private var gameStartTime: Long = 0L

    init {
        networkManager.eventListener = { event ->
            when (event) {
                is NetworkEvent.ActionRollDice -> {
                    // Host receives roll request from client
                    if (_isNetworkMode.value && networkRoomState.value.isHost) {
                        viewModelScope.launch {
                            val active = _gameState.value.activePlayer
                            if (active != null && active.color == event.color) {
                                rollDice()
                            }
                        }
                    }
                }
                is NetworkEvent.ActionMoveToken -> {
                    // Host receives move request from client
                    if (_isNetworkMode.value && networkRoomState.value.isHost) {
                        viewModelScope.launch {
                            val active = _gameState.value.activePlayer
                            if (active != null && active.color == event.color) {
                                moveToken(event.tokenId)
                            }
                        }
                    }
                }
                is NetworkEvent.GameStarted -> {
                    val synced = GameStateJsonAdapter.fromJson(event.gameStateJson)
                    if (synced != null) {
                        _isNetworkMode.value = true
                        _gameState.value = synced
                    }
                }
                is NetworkEvent.GameSynced -> {
                    val synced = GameStateJsonAdapter.fromJson(event.gameStateJson)
                    if (synced != null) {
                        _gameState.value = synced
                    }
                }
                is NetworkEvent.RoomUpdated -> {
                    if (!event.roomState.isHost) {
                        _myLocalNetworkColor.value = event.roomState.myColor
                    }
                }
                else -> {}
            }
        }
    }

    fun startHostRoom(hostName: String) {
        _isNetworkMode.value = true
        _myLocalNetworkColor.value = PlayerColor.RED
        networkManager.startHost(hostName)
    }

    fun joinHostRoom(roomCodeOrIp: String, playerName: String, preferredColor: PlayerColor) {
        _isNetworkMode.value = true
        _myLocalNetworkColor.value = preferredColor
        networkManager.joinRoom(roomCodeOrIp, playerName, preferredColor)
    }

    fun startNetworkMatch() {
        val roomState = networkRoomState.value
        if (!roomState.isHost) return

        aiTurnJob?.cancel()
        gameStartTime = System.currentTimeMillis()

        val pTypes = mutableMapOf<PlayerColor, PlayerType>()
        val pDiffs = mutableMapOf<PlayerColor, AiDifficulty>()

        val playersList = roomState.players.take(4)
        val pCount = if (playersList.size in 2..4) playersList.size else 4

        playersList.forEach { p ->
            pTypes[p.color] = p.type
            pDiffs[p.color] = AiDifficulty.MEDIUM
        }

        val initial = LudoEngine.createInitialGame(pCount, pTypes, pDiffs).let { base ->
            // Replace default names with connected player names
            val updatedPlayers = base.players.map { player ->
                val netP = playersList.find { it.color == player.color }
                if (netP != null && netP.name.isNotBlank()) {
                    player.copy(name = netP.name)
                } else player
            }
            base.copy(players = updatedPlayers)
        }

        _gameState.value = initial
        _isNetworkMode.value = true
        _myLocalNetworkColor.value = PlayerColor.RED

        val jsonStr = GameStateJsonAdapter.toJson(initial)
        networkManager.broadcastStartGame(jsonStr)

        checkAndExecuteAiTurn()
    }

    fun exitNetworkMode() {
        _isNetworkMode.value = false
        _myLocalNetworkColor.value = null
        networkManager.stopAll()
        _gameState.value = GameState()
    }

    fun startNewGame(
        playerCount: Int,
        playerTypes: Map<PlayerColor, PlayerType>,
        aiDifficulties: Map<PlayerColor, AiDifficulty>
    ) {
        _isNetworkMode.value = false
        _myLocalNetworkColor.value = null
        aiTurnJob?.cancel()
        gameStartTime = System.currentTimeMillis()
        val initialState = LudoEngine.createInitialGame(playerCount, playerTypes, aiDifficulties)
        _gameState.value = initialState
        saveCurrentGame()

        checkAndExecuteAiTurn()
    }

    fun resumeSavedGame(saved: GameState) {
        _isNetworkMode.value = false
        _myLocalNetworkColor.value = null
        aiTurnJob?.cancel()
        _gameState.value = saved.copy(status = GameStatus.IN_PROGRESS)
        checkAndExecuteAiTurn()
    }

    fun rollDice(overrideValue: Int? = null) {
        val current = _gameState.value
        if (current.isDiceRolled || current.status != GameStatus.IN_PROGRESS || current.isAutoPlaying) return

        // If in network mode and guest client, delegate to networkManager
        if (_isNetworkMode.value && !networkRoomState.value.isHost) {
            val myColor = _myLocalNetworkColor.value ?: return
            val active = current.activePlayer
            if (active != null && active.color == myColor) {
                networkManager.sendRollDiceAction(myColor)
            }
            return
        }

        soundManager.playDiceRollSound(_settings.value)
        val nextState = LudoEngine.rollDice(current, overrideValue)
        _gameState.value = nextState
        
        if (!_isNetworkMode.value) {
            saveCurrentGame()
        } else if (networkRoomState.value.isHost) {
            networkManager.broadcastGameState(GameStateJsonAdapter.toJson(nextState))
        }

        if (nextState.status == GameStatus.FINISHED) {
            onGameFinished(nextState)
            return
        }

        if (nextState.isDiceRolled && nextState.movableTokenIds.isNotEmpty()) {
            checkAndExecuteAiTurn()
        } else if (!nextState.isDiceRolled) {
            checkAndExecuteAiTurn()
        }
    }

    fun moveToken(tokenId: Int) {
        val current = _gameState.value
        if (!current.isDiceRolled || current.status != GameStatus.IN_PROGRESS || current.isAutoPlaying) return

        // If in network mode and guest client, delegate to networkManager
        if (_isNetworkMode.value && !networkRoomState.value.isHost) {
            val myColor = _myLocalNetworkColor.value ?: return
            val active = current.activePlayer
            if (active != null && active.color == myColor) {
                networkManager.sendMoveTokenAction(myColor, tokenId)
            }
            return
        }

        soundManager.playMoveSound(_settings.value)
        val nextState = LudoEngine.moveToken(current, tokenId)
        _gameState.value = nextState

        if (!_isNetworkMode.value) {
            saveCurrentGame()
        } else if (networkRoomState.value.isHost) {
            networkManager.broadcastGameState(GameStateJsonAdapter.toJson(nextState))
        }

        if (nextState.status == GameStatus.FINISHED) {
            onGameFinished(nextState)
            return
        }

        checkAndExecuteAiTurn()
    }

    private fun checkAndExecuteAiTurn() {
        val current = _gameState.value
        if (current.status != GameStatus.IN_PROGRESS) return

        val activePlayer = current.activePlayer ?: return
        if (activePlayer.type != PlayerType.AI) return

        aiTurnJob?.cancel()
        aiTurnJob = viewModelScope.launch {
            _gameState.value = _gameState.value.copy(isAutoPlaying = true)
            delay(_settings.value.aiSpeedMs)

            if (!_gameState.value.isDiceRolled) {
                // AI rolls dice
                soundManager.playDiceRollSound(_settings.value)
                val stateAfterRoll = LudoEngine.rollDice(_gameState.value)
                _gameState.value = stateAfterRoll.copy(isAutoPlaying = true)
                
                if (_isNetworkMode.value && networkRoomState.value.isHost) {
                    networkManager.broadcastGameState(GameStateJsonAdapter.toJson(_gameState.value))
                } else if (!_isNetworkMode.value) {
                    saveCurrentGame()
                }

                if (stateAfterRoll.status == GameStatus.FINISHED) {
                    onGameFinished(stateAfterRoll)
                    return@launch
                }

                if (stateAfterRoll.isDiceRolled && stateAfterRoll.movableTokenIds.isNotEmpty()) {
                    delay(_settings.value.aiSpeedMs)
                    val bestTokenId = LudoEngine.chooseBestTokenForAi(stateAfterRoll)
                    if (bestTokenId != null) {
                        soundManager.playMoveSound(_settings.value)
                        val stateAfterMove = LudoEngine.moveToken(stateAfterRoll, bestTokenId)
                        _gameState.value = stateAfterMove.copy(isAutoPlaying = false)
                        
                        if (_isNetworkMode.value && networkRoomState.value.isHost) {
                            networkManager.broadcastGameState(GameStateJsonAdapter.toJson(_gameState.value))
                        } else if (!_isNetworkMode.value) {
                            saveCurrentGame()
                        }

                        if (stateAfterMove.status == GameStatus.FINISHED) {
                            onGameFinished(stateAfterMove)
                            return@launch
                        }

                        // Chain AI turn if AI gets extra turn
                        checkAndExecuteAiTurn()
                    } else {
                        _gameState.value = _gameState.value.copy(isAutoPlaying = false)
                    }
                } else {
                    _gameState.value = _gameState.value.copy(isAutoPlaying = false)
                    checkAndExecuteAiTurn()
                }
            } else {
                // Dice already rolled, AI picks token
                val bestTokenId = LudoEngine.chooseBestTokenForAi(current)
                if (bestTokenId != null) {
                    soundManager.playMoveSound(_settings.value)
                    val stateAfterMove = LudoEngine.moveToken(current, bestTokenId)
                    _gameState.value = stateAfterMove.copy(isAutoPlaying = false)
                    
                    if (_isNetworkMode.value && networkRoomState.value.isHost) {
                        networkManager.broadcastGameState(GameStateJsonAdapter.toJson(_gameState.value))
                    } else if (!_isNetworkMode.value) {
                        saveCurrentGame()
                    }

                    if (stateAfterMove.status == GameStatus.FINISHED) {
                        onGameFinished(stateAfterMove)
                        return@launch
                    }

                    checkAndExecuteAiTurn()
                } else {
                    _gameState.value = _gameState.value.copy(isAutoPlaying = false)
                }
            }
        }
    }

    private fun onGameFinished(finalState: GameState) {
        soundManager.playWinSound(_settings.value)
        val winnerColor = finalState.winners.firstOrNull() ?: PlayerColor.RED
        val winnerPlayer = finalState.players.find { it.color == winnerColor }

        val duration = (System.currentTimeMillis() - gameStartTime) / 1000L

        viewModelScope.launch {
            repository.recordGameWin(
                GameHistoryEntity(
                    modeText = if (_isNetworkMode.value) "Wi-Fi Multiplayer Mode" else "${finalState.players.size} Players Mode",
                    playerCount = finalState.players.size,
                    winnerColor = winnerColor.name,
                    winnerName = winnerPlayer?.name ?: "Unknown",
                    durationSeconds = if (duration > 0) duration else 120L
                )
            )
            repository.clearSavedGame()
        }
    }

    fun pauseGame() {
        aiTurnJob?.cancel()
        _gameState.value = _gameState.value.copy(status = GameStatus.PAUSED, isAutoPlaying = false)
        if (!_isNetworkMode.value) saveCurrentGame()
    }

    fun resumeGame() {
        _gameState.value = _gameState.value.copy(status = GameStatus.IN_PROGRESS)
        checkAndExecuteAiTurn()
    }

    fun restartGame() {
        val current = _gameState.value
        val pTypes = current.players.associate { it.color to it.type }
        val pDiffs = current.players.associate { it.color to it.difficulty }
        startNewGame(current.players.size, pTypes, pDiffs)
    }

    fun abandonGame() {
        aiTurnJob?.cancel()
        if (_isNetworkMode.value) {
            exitNetworkMode()
        } else {
            _gameState.value = GameState()
            viewModelScope.launch {
                repository.clearSavedGame()
            }
        }
    }

    private fun saveCurrentGame() {
        val state = _gameState.value
        if (state.status == GameStatus.IN_PROGRESS || state.status == GameStatus.PAUSED) {
            viewModelScope.launch {
                repository.saveGameState(state)
            }
        }
    }

    fun updateSettings(newSettings: GameSettings) {
        _settings.value = newSettings
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    override fun onCleared() {
        super.onCleared()
        networkManager.stopAll()
    }
}
