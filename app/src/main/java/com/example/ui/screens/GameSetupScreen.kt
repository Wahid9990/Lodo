package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiDifficulty
import com.example.model.PlayerColor
import com.example.model.PlayerType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSetupScreen(
    onStartGame: (
        playerCount: Int,
        playerTypes: Map<PlayerColor, PlayerType>,
        aiDifficulties: Map<PlayerColor, AiDifficulty>
    ) -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var playerCount by remember { mutableIntStateOf(4) }

    var playerTypes by remember {
        mutableStateOf(
            mapOf(
                PlayerColor.RED to PlayerType.HUMAN,
                PlayerColor.GREEN to PlayerType.AI,
                PlayerColor.YELLOW to PlayerType.AI,
                PlayerColor.BLUE to PlayerType.AI
            )
        )
    }

    var aiDifficulties by remember {
        mutableStateOf(
            mapOf(
                PlayerColor.RED to AiDifficulty.MEDIUM,
                PlayerColor.GREEN to AiDifficulty.MEDIUM,
                PlayerColor.YELLOW to AiDifficulty.MEDIUM,
                PlayerColor.BLUE to AiDifficulty.MEDIUM
            )
        )
    }

    val activeColors = when (playerCount) {
        2 -> listOf(PlayerColor.RED, PlayerColor.YELLOW)
        3 -> listOf(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW)
        else -> listOf(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Game Setup", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { onStartGame(playerCount, playerTypes, aiDifficulties) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .testTag("start_match_button")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Start")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("START MATCH", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Player Count Selection
            Text("Select Number of Players", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(2, 3, 4).forEachIndexed { index, count ->
                    SegmentedButton(
                        selected = playerCount == count,
                        onClick = { playerCount = count },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                        modifier = Modifier.testTag("select_player_count_$count")
                    ) {
                        Text("$count Players")
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Player Slot Configuration Cards
            Text("Configure Players", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            activeColors.forEach { color ->
                PlayerSetupCard(
                    color = color,
                    currentType = playerTypes[color] ?: PlayerType.HUMAN,
                    currentDifficulty = aiDifficulties[color] ?: AiDifficulty.MEDIUM,
                    onTypeChanged = { newType ->
                        playerTypes = playerTypes.toMutableMap().apply { put(color, newType) }
                    },
                    onDifficultyChanged = { newDiff ->
                        aiDifficulties = aiDifficulties.toMutableMap().apply { put(color, newDiff) }
                    }
                )
            }
        }
    }
}

@Composable
private fun PlayerSetupCard(
    color: PlayerColor,
    currentType: PlayerType,
    currentDifficulty: AiDifficulty,
    onTypeChanged: (PlayerType) -> Unit,
    onDifficultyChanged: (AiDifficulty) -> Unit
) {
    val colorHex = when (color) {
        PlayerColor.RED -> Color(0xFFE53935)
        PlayerColor.GREEN -> Color(0xFF43A047)
        PlayerColor.YELLOW -> Color(0xFFFDD835)
        PlayerColor.BLUE -> Color(0xFF1E88E5)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(colorHex)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${color.displayName} Player",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                // Type Toggle (Human / AI)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = currentType == PlayerType.HUMAN,
                        onClick = { onTypeChanged(PlayerType.HUMAN) },
                        label = { Text("Human") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = currentType == PlayerType.AI,
                        onClick = { onTypeChanged(PlayerType.AI) },
                        label = { Text("AI") },
                        leadingIcon = { Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            // If AI, show difficulty selection
            if (currentType == PlayerType.AI) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("AI Difficulty:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        AiDifficulty.entries.forEach { diff ->
                            FilterChip(
                                selected = currentDifficulty == diff,
                                onClick = { onDifficultyChanged(diff) },
                                label = { Text(diff.name) }
                            )
                        }
                    }
                }
            }
        }
    }
}
