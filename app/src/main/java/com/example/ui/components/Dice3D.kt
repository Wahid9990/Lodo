package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PlayerColor

@Composable
fun DiceRoller(
    diceValue: Int,
    isDiceRolled: Boolean,
    activeColor: PlayerColor,
    isAutoPlaying: Boolean,
    onRollClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isDiceRolled) 360f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "dice_rotation"
    )

    val activeColorHex = when (activeColor) {
        PlayerColor.RED -> Color(0xFFE53935)
        PlayerColor.GREEN -> Color(0xFF43A047)
        PlayerColor.YELLOW -> Color(0xFFFDD835)
        PlayerColor.BLUE -> Color(0xFF1E88E5)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dice Box Display
        Box(
            modifier = Modifier
                .size(64.dp)
                .rotate(rotationAngle)
                .shadow(8.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(3.dp, activeColorHex, RoundedCornerShape(12.dp))
                .clickable(enabled = !isDiceRolled && !isAutoPlaying, onClick = onRollClicked)
                .testTag("dice_view"),
            contentAlignment = Alignment.Center
        ) {
            DiceDots(diceValue = diceValue)
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Roll Button
        Button(
            onClick = onRollClicked,
            enabled = !isDiceRolled && !isAutoPlaying,
            colors = ButtonDefaults.buttonColors(
                containerColor = activeColorHex,
                contentColor = if (activeColor == PlayerColor.YELLOW) Color.Black else Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .height(54.dp)
                .weight(1f)
                .shadow(6.dp, RoundedCornerShape(16.dp))
                .testTag("roll_dice_button")
        ) {
            Icon(
                imageVector = Icons.Default.Casino,
                contentDescription = "Roll Dice",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isDiceRolled) "Dice: $diceValue" else "ROLL DICE",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun DiceDots(diceValue: Int) {
    Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
        val radius = size.width * 0.1f
        val dotColor = Color(0xFF1E293B)

        val left = size.width * 0.25f
        val center = size.width * 0.5f
        val right = size.width * 0.75f

        val top = size.height * 0.25f
        val middle = size.height * 0.5f
        val bottom = size.height * 0.75f

        val points = when (diceValue) {
            1 -> listOf(Offset(center, middle))
            2 -> listOf(Offset(left, top), Offset(right, bottom))
            3 -> listOf(Offset(left, top), Offset(center, middle), Offset(right, bottom))
            4 -> listOf(Offset(left, top), Offset(right, top), Offset(left, bottom), Offset(right, bottom))
            5 -> listOf(Offset(left, top), Offset(right, top), Offset(center, middle), Offset(left, bottom), Offset(right, bottom))
            6 -> listOf(Offset(left, top), Offset(right, top), Offset(left, middle), Offset(right, middle), Offset(left, bottom), Offset(right, bottom))
            else -> listOf(Offset(center, middle))
        }

        points.forEach { point ->
            drawCircle(color = dotColor, radius = radius, center = point)
        }
    }
}
