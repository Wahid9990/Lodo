package com.example.logic

import com.example.model.PlayerColor

data class GridPoint(val row: Int, val col: Int)

object LudoBoardMapper {
    // 52 cells on outer path starting at RED start (row 6, col 1)
    val OUTER_TRACK: List<GridPoint> = listOf(
        // Left arm top row (0..4)
        GridPoint(6, 1), GridPoint(6, 2), GridPoint(6, 3), GridPoint(6, 4), GridPoint(6, 5),
        // Top arm left column going UP (5..10)
        GridPoint(5, 6), GridPoint(4, 6), GridPoint(3, 6), GridPoint(2, 6), GridPoint(1, 6), GridPoint(0, 6),
        // Top arm top cell (11)
        GridPoint(0, 7),
        // Top arm right column going DOWN (12..17)
        GridPoint(0, 8), GridPoint(1, 8), GridPoint(2, 8), GridPoint(3, 8), GridPoint(4, 8), GridPoint(5, 8),
        // Right arm top row going RIGHT (18..23)
        GridPoint(6, 9), GridPoint(6, 10), GridPoint(6, 11), GridPoint(6, 12), GridPoint(6, 13), GridPoint(6, 14),
        // Right arm rightmost cell (24)
        GridPoint(7, 14),
        // Right arm bottom row going LEFT (25..30)
        GridPoint(8, 14), GridPoint(8, 13), GridPoint(8, 12), GridPoint(8, 11), GridPoint(8, 10), GridPoint(8, 9),
        // Bottom arm right column going DOWN (31..36)
        GridPoint(9, 8), GridPoint(10, 8), GridPoint(11, 8), GridPoint(12, 8), GridPoint(13, 8), GridPoint(14, 8),
        // Bottom arm bottom cell (37)
        GridPoint(14, 7),
        // Bottom arm left column going UP (38..43)
        GridPoint(14, 6), GridPoint(13, 6), GridPoint(12, 6), GridPoint(11, 6), GridPoint(10, 6), GridPoint(9, 6),
        // Left arm bottom row going LEFT (44..49)
        GridPoint(8, 5), GridPoint(8, 4), GridPoint(8, 3), GridPoint(8, 2), GridPoint(8, 1), GridPoint(8, 0),
        // Left arm leftmost cell (50)
        GridPoint(7, 0),
        // Left arm entrance cell (51) - Note: Red start is index 0
        GridPoint(6, 0)
    )

    fun getColorStartOffset(color: PlayerColor): Int = when (color) {
        PlayerColor.RED -> 0
        PlayerColor.GREEN -> 13
        PlayerColor.YELLOW -> 26
        PlayerColor.BLUE -> 39
    }

    val SAFE_GLOBAL_INDICES: Set<Int> = setOf(
        0,  // Red Start
        8,  // Red Star
        13, // Green Start
        21, // Green Star
        26, // Yellow Start
        34, // Yellow Star
        39, // Blue Start
        47  // Blue Star
    )

    fun isGlobalIndexSafe(globalIndex: Int): Boolean {
        return globalIndex in SAFE_GLOBAL_INDICES
    }

    fun isRelativePositionSafe(color: PlayerColor, relativePosition: Int): Boolean {
        if (relativePosition < 0 || relativePosition > 50) return true // Yard, Home stretch & Finish are safe
        val globalIdx = (getColorStartOffset(color) + relativePosition) % 52
        return isGlobalIndexSafe(globalIdx)
    }

    fun getGridPoint(color: PlayerColor, relativePosition: Int, yardIndex: Int): GridPoint {
        if (relativePosition == -1) {
            // Yard position
            return getYardGridPoint(color, yardIndex)
        }

        if (relativePosition in 0..50) {
            val globalIdx = (getColorStartOffset(color) + relativePosition) % 52
            return OUTER_TRACK[globalIdx]
        }

        if (relativePosition in 51..55) {
            val step = relativePosition - 51
            return when (color) {
                PlayerColor.RED -> GridPoint(7, 1 + step)
                PlayerColor.GREEN -> GridPoint(1 + step, 7)
                PlayerColor.YELLOW -> GridPoint(7, 13 - step)
                PlayerColor.BLUE -> GridPoint(13 - step, 7)
            }
        }

        // Finish 56
        return when (color) {
            PlayerColor.RED -> GridPoint(7, 6)
            PlayerColor.GREEN -> GridPoint(6, 7)
            PlayerColor.YELLOW -> GridPoint(7, 8)
            PlayerColor.BLUE -> GridPoint(8, 7)
        }
    }

    private fun getYardGridPoint(color: PlayerColor, yardIndex: Int): GridPoint {
        val offsets = listOf(
            GridPoint(0, 0),
            GridPoint(0, 2),
            GridPoint(2, 0),
            GridPoint(2, 2)
        )
        val offset = offsets[yardIndex % 4]
        return when (color) {
            PlayerColor.RED -> GridPoint(1 + offset.row, 1 + offset.col)
            PlayerColor.GREEN -> GridPoint(1 + offset.row, 10 + offset.col)
            PlayerColor.YELLOW -> GridPoint(10 + offset.row, 10 + offset.col)
            PlayerColor.BLUE -> GridPoint(10 + offset.row, 1 + offset.col)
        }
    }
}
