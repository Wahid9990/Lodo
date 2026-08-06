package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Player
import com.example.model.PlayerColor
import com.example.model.PlayerType

@Composable
fun PlayerBar(
    players: List<Player>,
    activePlayerColor: PlayerColor,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        players.forEach { player ->
            PlayerChip(
                player = player,
                isActive = (player.color == activePlayerColor),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun PlayerChip(
    player: Player,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val colorHex = when (player.color) {
        PlayerColor.RED -> Color(0xFFE53935)
        PlayerColor.GREEN -> Color(0xFF43A047)
        PlayerColor.YELLOW -> Color(0xFFFDD835)
        PlayerColor.BLUE -> Color(0xFF1E88E5)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) colorHex.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .border(
                width = if (isActive) 2.5.dp else 1.dp,
                color = if (isActive) colorHex else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .testTag("player_chip_${player.color.name}")
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(colorHex),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (player.type == PlayerType.HUMAN) Icons.Default.Person else Icons.Default.Android,
                    contentDescription = player.type.name,
                    tint = if (player.color == PlayerColor.YELLOW) Color.Black else Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column {
                Text(
                    text = player.color.displayName,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Home: ${player.finishedTokensCount}/4",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
