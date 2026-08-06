package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logic.GridPoint
import com.example.logic.LudoBoardMapper
import com.example.model.GameState
import com.example.model.PlayerColor
import com.example.model.Token

@Composable
fun LudoBoard(
    gameState: GameState,
    onTokenClicked: (tokenId: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .shadow(12.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF10172A))
            .border(4.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
            .testTag("ludo_board_canvas")
    ) {
        val boardSizeDp = maxWidth
        val cellSizeDp = boardSizeDp / 15f

        // Draw Base Board (Grids, Quadrants, Paths, Safe Stars)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellPx = size.width / 15f

            // 1. Draw 4 Home Yards
            drawHomeYard(0, 0, cellPx, Color(0xFFE53935)) // RED - Top Left
            drawHomeYard(9, 0, cellPx, Color(0xFF43A047)) // GREEN - Top Right
            drawHomeYard(9, 9, cellPx, Color(0xFFFDD835)) // YELLOW - Bottom Right
            drawHomeYard(0, 9, cellPx, Color(0xFF1E88E5)) // BLUE - Bottom Left

            // 2. Draw 15x15 Track Grid Outline
            drawGridLines(cellPx)

            // 3. Highlight Start Cells and Home Stretches
            drawColoredPaths(cellPx)

            // 4. Center Finish Home Triangles
            drawCenterTriangles(cellPx)
        }

        // Overlay Star Icons on Safe Spots
        val safePoints = listOf(
            GridPoint(6, 1) to Color(0xFFE53935),
            GridPoint(2, 6) to Color(0xFFFFB300),
            GridPoint(1, 8) to Color(0xFF43A047),
            GridPoint(6, 12) to Color(0xFFFFB300),
            GridPoint(8, 13) to Color(0xFFFDD835),
            GridPoint(12, 8) to Color(0xFFFFB300),
            GridPoint(13, 6) to Color(0xFF1E88E5),
            GridPoint(8, 2) to Color(0xFFFFB300)
        )

        safePoints.forEach { (point, color) ->
            Box(
                modifier = Modifier
                    .offset(
                        x = cellSizeDp * point.col.toFloat(),
                        y = cellSizeDp * point.row.toFloat()
                    )
                    .size(cellSizeDp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Safe Star",
                    tint = color.copy(alpha = 0.85f),
                    modifier = Modifier.size(cellSizeDp * 0.7f)
                )
            }
        }

        // Collect Tokens & Group by (row, col) coordinates
        val activePlayer = gameState.activePlayer
        val movableIds = gameState.movableTokenIds

        val tokenLocations = mutableMapOf<GridPoint, MutableList<Pair<Token, Boolean>>>()

        gameState.players.forEach { player ->
            player.tokens.forEach { token ->
                val pt = LudoBoardMapper.getGridPoint(token.color, token.relativePosition, token.initialYardIndex)
                val isMovable = (player.color == activePlayer?.color) && (token.id in movableIds)
                tokenLocations.getOrPut(pt) { mutableListOf() }.add(token to isMovable)
            }
        }

        // Render Tokens
        tokenLocations.forEach { (gridPoint, tokensAtPoint) ->
            val count = tokensAtPoint.size
            tokensAtPoint.forEachIndexed { index, (token, isMovable) ->
                // Offset multiple tokens on same cell slightly so all remain visible!
                val offsetX = if (count > 1) cellSizeDp * ((index % 2) * 0.2f - 0.1f) else 0.dp
                val offsetY = if (count > 1) cellSizeDp * ((index / 2) * 0.2f - 0.1f) else 0.dp

                TokenComposable(
                    token = token,
                    cellSizeDp = cellSizeDp,
                    isMovable = isMovable,
                    stackCount = if (index == 0 && count > 1) count else 1,
                    onClicked = { if (isMovable) onTokenClicked(token.id) },
                    modifier = Modifier.offset(
                        x = cellSizeDp * gridPoint.col.toFloat() + offsetX,
                        y = cellSizeDp * gridPoint.row.toFloat() + offsetY
                    )
                )
            }
        }
    }
}

@Composable
private fun TokenComposable(
    token: Token,
    cellSizeDp: Dp,
    isMovable: Boolean,
    stackCount: Int,
    onClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scalePulse by animateFloatAsState(
        targetValue = if (isMovable) 1.25f else 1.0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "token_scale"
    )

    val tokenColor = when (token.color) {
        PlayerColor.RED -> Color(0xFFE53935)
        PlayerColor.GREEN -> Color(0xFF43A047)
        PlayerColor.YELLOW -> Color(0xFFFDD835)
        PlayerColor.BLUE -> Color(0xFF1E88E5)
    }

    Box(
        modifier = modifier
            .size(cellSizeDp)
            .scale(scalePulse),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(cellSizeDp * 0.78f)
                .shadow(if (isMovable) 8.dp else 4.dp, CircleShape)
                .clip(CircleShape)
                .background(tokenColor)
                .border(
                    width = if (isMovable) 3.dp else 1.5.dp,
                    color = if (isMovable) Color.White else Color(0x99FFFFFF),
                    shape = CircleShape
                )
                .clickable(enabled = isMovable, onClick = onClicked)
                .testTag("token_${token.color.name}_${token.id}"),
            contentAlignment = Alignment.Center
        ) {
            // Inner glossy token cap
            Box(
                modifier = Modifier
                    .size(cellSizeDp * 0.38f)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.6f))
            )

            // Badge for stacked tokens
            if (stackCount > 1) {
                Text(
                    text = "$stackCount",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }
    }
}

private fun DrawScope.drawHomeYard(colStart: Int, rowStart: Int, cellPx: Float, color: Color) {
    val x = colStart * cellPx
    val y = rowStart * cellPx
    val sizePx = 6 * cellPx

    // Yard background
    drawRect(color = color, topLeft = Offset(x, y), size = Size(sizePx, sizePx))

    // Inner white container
    val innerMargin = cellPx * 0.8f
    val innerSize = sizePx - (2 * innerMargin)
    drawRect(
        color = Color.White,
        topLeft = Offset(x + innerMargin, y + innerMargin),
        size = Size(innerSize, innerSize)
    )

    // 4 Token Circles inside yard
    val circleRadius = cellPx * 0.55f
    val centers = listOf(
        Offset(x + cellPx * 1.8f, y + cellPx * 1.8f),
        Offset(x + cellPx * 4.2f, y + cellPx * 1.8f),
        Offset(x + cellPx * 1.8f, y + cellPx * 4.2f),
        Offset(x + cellPx * 4.2f, y + cellPx * 4.2f)
    )
    centers.forEach { center ->
        drawCircle(color = color, radius = circleRadius, center = center)
        drawCircle(color = Color.White.copy(alpha = 0.4f), radius = circleRadius * 0.4f, center = center)
    }
}

private fun DrawScope.drawGridLines(cellPx: Float) {
    val stroke = Stroke(width = 1.2f)
    val gridColor = Color(0xFF334155)

    for (i in 0..15) {
        // Vertical lines
        drawLine(gridColor, start = Offset(i * cellPx, 0f), end = Offset(i * cellPx, size.height), strokeWidth = stroke.width)
        // Horizontal lines
        drawLine(gridColor, start = Offset(0f, i * cellPx), end = Offset(size.width, i * cellPx), strokeWidth = stroke.width)
    }
}

private fun DrawScope.drawColoredPaths(cellPx: Float) {
    val redColor = Color(0xFFE53935)
    val greenColor = Color(0xFF43A047)
    val yellowColor = Color(0xFFFDD835)
    val blueColor = Color(0xFF1E88E5)

    // Red Start & Home Stretch (row 7, cols 1..5) & Start (6, 1)
    drawRect(redColor, topLeft = Offset(1 * cellPx, 6 * cellPx), size = Size(cellPx, cellPx))
    for (c in 1..5) drawRect(redColor, topLeft = Offset(c * cellPx, 7 * cellPx), size = Size(cellPx, cellPx))

    // Green Start (1, 8) & Home Stretch (rows 1..5, col 7)
    drawRect(greenColor, topLeft = Offset(8 * cellPx, 1 * cellPx), size = Size(cellPx, cellPx))
    for (r in 1..5) drawRect(greenColor, topLeft = Offset(7 * cellPx, r * cellPx), size = Size(cellPx, cellPx))

    // Yellow Start (8, 13) & Home Stretch (row 7, cols 9..13)
    drawRect(yellowColor, topLeft = Offset(13 * cellPx, 8 * cellPx), size = Size(cellPx, cellPx))
    for (c in 9..13) drawRect(yellowColor, topLeft = Offset(c * cellPx, 7 * cellPx), size = Size(cellPx, cellPx))

    // Blue Start (13, 6) & Home Stretch (rows 9..13, col 7)
    drawRect(blueColor, topLeft = Offset(6 * cellPx, 13 * cellPx), size = Size(cellPx, cellPx))
    for (r in 9..13) drawRect(blueColor, topLeft = Offset(7 * cellPx, r * cellPx), size = Size(cellPx, cellPx))
}

private fun DrawScope.drawCenterTriangles(cellPx: Float) {
    val centerLeft = 6 * cellPx
    val centerTop = 6 * cellPx
    val centerRight = 9 * cellPx
    val centerBottom = 9 * cellPx
    val midX = 7.5f * cellPx
    val midY = 7.5f * cellPx

    val redColor = Color(0xFFE53935)
    val greenColor = Color(0xFF43A047)
    val yellowColor = Color(0xFFFDD835)
    val blueColor = Color(0xFF1E88E5)

    // Left (Red) Triangle
    drawPath(Path().apply {
        moveTo(centerLeft, centerTop)
        lineTo(midX, midY)
        lineTo(centerLeft, centerBottom)
        close()
    }, redColor)

    // Top (Green) Triangle
    drawPath(Path().apply {
        moveTo(centerLeft, centerTop)
        lineTo(midX, midY)
        lineTo(centerRight, centerTop)
        close()
    }, greenColor)

    // Right (Yellow) Triangle
    drawPath(Path().apply {
        moveTo(centerRight, centerTop)
        lineTo(midX, midY)
        lineTo(centerRight, centerBottom)
        close()
    }, yellowColor)

    // Bottom (Blue) Triangle
    drawPath(Path().apply {
        moveTo(centerLeft, centerBottom)
        lineTo(midX, midY)
        lineTo(centerRight, centerBottom)
        close()
    }, blueColor)
}
