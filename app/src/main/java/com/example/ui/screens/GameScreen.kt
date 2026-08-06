package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameSettings
import com.example.model.GameState
import com.example.model.GameStatus
import com.example.model.PlayerColor
import com.example.ui.components.ConfettiEffect
import com.example.ui.components.DiceRoller
import com.example.ui.components.LudoBoard
import com.example.ui.components.PlayerBar
import com.example.ui.viewmodel.LudoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: LudoViewModel,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gameState by viewModel.gameState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isNetworkMode by viewModel.isNetworkMode.collectAsState()
    val myNetworkColor by viewModel.myLocalNetworkColor.collectAsState()

    var showPauseDialog by remember { mutableStateOf(false) }

    val activePlayer = gameState.activePlayer
    val activeColor = activePlayer?.color ?: PlayerColor.RED
    val isMyTurn = !isNetworkMode || (activePlayer != null && activePlayer.color == myNetworkColor)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ludo Master",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.updateSettings(
                                settings.copy(soundFxEnabled = !settings.soundFxEnabled)
                            )
                        }
                    ) {
                        Icon(
                            imageVector = if (settings.soundFxEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Sound Toggle"
                        )
                    }
                    IconButton(onClick = { viewModel.restartGame() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restart"
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.pauseGame()
                            showPauseDialog = true
                        },
                        modifier = Modifier.testTag("pause_game_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top Player Chips Bar
                PlayerBar(
                    players = gameState.players,
                    activePlayerColor = activeColor
                )

                if (isNetworkMode) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMyTurn) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isMyTurn) "🎲 YOUR TURN (${myNetworkColor?.displayName ?: "Player"}) - Tap dice to roll!" else "⏳ Waiting for ${activePlayer?.name ?: activeColor.displayName}'s turn...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMyTurn) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                // Status Message Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = gameState.lastActionMessage,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("action_status_banner")
                    )
                }

                // Interactive 15x15 Ludo Board Canvas
                LudoBoard(
                    gameState = gameState,
                    onTokenClicked = { tokenId -> viewModel.moveToken(tokenId) },
                    isNetworkMode = isNetworkMode,
                    myNetworkColor = myNetworkColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )

                // Animated Dice Roller
                DiceRoller(
                    diceValue = gameState.diceValue,
                    isDiceRolled = gameState.isDiceRolled,
                    activeColor = activeColor,
                    isAutoPlaying = gameState.isAutoPlaying,
                    isMyTurn = isMyTurn,
                    onRollClicked = { viewModel.rollDice() }
                )
            }

            // Victory Confetti Particle Layer & Winner Dialog
            if (gameState.status == GameStatus.FINISHED) {
                ConfettiEffect(modifier = Modifier.fillMaxSize())

                val winnerColor = gameState.winners.firstOrNull() ?: PlayerColor.RED
                val winnerName = gameState.players.find { it.color == winnerColor }?.name ?: "Player"

                AlertDialog(
                    onDismissRequest = { },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Winner",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(48.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "VICTORY!",
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp
                        )
                    },
                    text = {
                        Text(
                            text = "$winnerName has conquered the board and won the match! 🏆",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.abandonGame()
                                onNavigateHome()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("victory_home_button")
                        ) {
                            Text("RETURN TO HOME")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { viewModel.restartGame() }
                        ) {
                            Text("PLAY AGAIN")
                        }
                    }
                )
            }

            // Pause Dialog
            if (showPauseDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showPauseDialog = false
                        viewModel.resumeGame()
                    },
                    title = { Text("Game Paused", fontWeight = FontWeight.Bold) },
                    text = { Text("What would you like to do?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showPauseDialog = false
                                viewModel.resumeGame()
                            }
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("RESUME")
                        }
                    },
                    dismissButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = {
                                    showPauseDialog = false
                                    viewModel.restartGame()
                                }
                            ) {
                                Text("RESTART")
                            }
                            TextButton(
                                onClick = {
                                    showPauseDialog = false
                                    viewModel.abandonGame()
                                    onNavigateHome()
                                }
                            ) {
                                Text("EXIT")
                            }
                        }
                    }
                )
            }
        }
    }
}
