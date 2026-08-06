package com.example.network

import com.example.model.AiDifficulty
import com.example.model.GameState
import com.example.model.GameStatus
import com.example.model.Player
import com.example.model.PlayerColor
import com.example.model.PlayerType
import com.example.model.Token
import org.json.JSONArray
import org.json.JSONObject

object GameStateJsonAdapter {

    fun toJson(state: GameState): String {
        val root = JSONObject()

        root.put("activePlayerIndex", state.activePlayerIndex)
        root.put("diceValue", state.diceValue)
        root.put("isDiceRolled", state.isDiceRolled)
        root.put("consecutiveSixes", state.consecutiveSixes)
        root.put("status", state.status.name)
        root.put("lastActionMessage", state.lastActionMessage)
        root.put("selectedPlayerCount", state.selectedPlayerCount)
        root.put("isAutoPlaying", state.isAutoPlaying)
        root.put("turnTimestamp", state.turnTimestamp)

        // Winners
        val winnersArr = JSONArray()
        state.winners.forEach { winnersArr.put(it.name) }
        root.put("winners", winnersArr)

        // Movable token IDs
        val movableArr = JSONArray()
        state.movableTokenIds.forEach { movableArr.put(it) }
        root.put("movableTokenIds", movableArr)

        // Players
        val playersArr = JSONArray()
        state.players.forEach { p ->
            val pObj = JSONObject()
            pObj.put("color", p.color.name)
            pObj.put("name", p.name)
            pObj.put("type", p.type.name)
            pObj.put("difficulty", p.difficulty.name)
            pObj.put("rank", p.rank)

            val tokensArr = JSONArray()
            p.tokens.forEach { t ->
                val tObj = JSONObject()
                tObj.put("id", t.id)
                tObj.put("color", t.color.name)
                tObj.put("relativePosition", t.relativePosition)
                tObj.put("initialYardIndex", t.initialYardIndex)
                tokensArr.put(tObj)
            }
            pObj.put("tokens", tokensArr)
            playersArr.put(pObj)
        }
        root.put("players", playersArr)

        return root.toString()
    }

    fun fromJson(jsonStr: String): GameState? {
        try {
            val root = JSONObject(jsonStr)

            val activePlayerIndex = root.optInt("activePlayerIndex", 0)
            val diceValue = root.optInt("diceValue", 1)
            val isDiceRolled = root.optBoolean("isDiceRolled", false)
            val consecutiveSixes = root.optInt("consecutiveSixes", 0)
            val statusStr = root.optString("status", GameStatus.IN_PROGRESS.name)
            val status = GameStatus.entries.find { it.name == statusStr } ?: GameStatus.IN_PROGRESS
            val lastActionMessage = root.optString("lastActionMessage", "")
            val selectedPlayerCount = root.optInt("selectedPlayerCount", 4)
            val isAutoPlaying = root.optBoolean("isAutoPlaying", false)
            val turnTimestamp = root.optLong("turnTimestamp", System.currentTimeMillis())

            val winnersList = mutableListOf<PlayerColor>()
            val winnersArr = root.optJSONArray("winners")
            if (winnersArr != null) {
                for (i in 0 until winnersArr.length()) {
                    val wName = winnersArr.getString(i)
                    val c = PlayerColor.entries.find { it.name == wName }
                    if (c != null) winnersList.add(c)
                }
            }

            val movableList = mutableListOf<Int>()
            val movableArr = root.optJSONArray("movableTokenIds")
            if (movableArr != null) {
                for (i in 0 until movableArr.length()) {
                    movableList.add(movableArr.getInt(i))
                }
            }

            val playersList = mutableListOf<Player>()
            val playersArr = root.optJSONArray("players")
            if (playersArr != null) {
                for (i in 0 until playersArr.length()) {
                    val pObj = playersArr.getJSONObject(i)
                    val colorStr = pObj.optString("color")
                    val color = PlayerColor.entries.find { it.name == colorStr } ?: PlayerColor.RED
                    val name = pObj.optString("name", color.displayName)
                    val typeStr = pObj.optString("type", PlayerType.HUMAN.name)
                    val type = PlayerType.entries.find { it.name == typeStr } ?: PlayerType.HUMAN
                    val diffStr = pObj.optString("difficulty", AiDifficulty.MEDIUM.name)
                    val diff = AiDifficulty.entries.find { it.name == diffStr } ?: AiDifficulty.MEDIUM
                    val rank = pObj.optInt("rank", 0)

                    val tokensList = mutableListOf<Token>()
                    val tokensArr = pObj.optJSONArray("tokens")
                    if (tokensArr != null) {
                        for (j in 0 until tokensArr.length()) {
                            val tObj = tokensArr.getJSONObject(j)
                            val tId = tObj.optInt("id", j)
                            val tColorStr = tObj.optString("color", color.name)
                            val tColor = PlayerColor.entries.find { it.name == tColorStr } ?: color
                            val relPos = tObj.optInt("relativePosition", -1)
                            val yardIdx = tObj.optInt("initialYardIndex", j)

                            tokensList.add(
                                Token(
                                    id = tId,
                                    color = tColor,
                                    relativePosition = relPos,
                                    initialYardIndex = yardIdx
                                )
                            )
                        }
                    } else {
                        tokensList.addAll(List(4) { id -> Token(id, color, -1, id) })
                    }

                    playersList.add(
                        Player(
                            color = color,
                            name = name,
                            type = type,
                            difficulty = diff,
                            tokens = tokensList,
                            rank = rank
                        )
                    )
                }
            }

            return GameState(
                players = playersList,
                activePlayerIndex = activePlayerIndex,
                diceValue = diceValue,
                isDiceRolled = isDiceRolled,
                consecutiveSixes = consecutiveSixes,
                status = status,
                winners = winnersList,
                movableTokenIds = movableList,
                lastActionMessage = lastActionMessage,
                selectedPlayerCount = selectedPlayerCount,
                isAutoPlaying = isAutoPlaying,
                turnTimestamp = turnTimestamp
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
