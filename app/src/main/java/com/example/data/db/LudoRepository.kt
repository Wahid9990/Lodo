package com.example.data.db

import android.content.Context
import com.example.model.GameHistoryEntity
import com.example.model.GameState
import com.example.model.GameSaveEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LudoRepository(context: Context) {
    private val dao = AppDatabase.getDatabase(context).ludoDao()
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val jsonAdapter = moshi.adapter(GameState::class.java)

    val savedGameState: Flow<GameState?> = dao.getSavedGame().map { entity ->
        entity?.let {
            try {
                jsonAdapter.fromJson(it.gameStateJson)
            } catch (e: Exception) {
                null
            }
        }
    }

    val historyList: Flow<List<GameHistoryEntity>> = dao.getGameHistory()

    suspend fun saveGameState(state: GameState) {
        try {
            val json = jsonAdapter.toJson(state)
            dao.saveGame(GameSaveEntity(id = 1, gameStateJson = json))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun clearSavedGame() {
        dao.clearSavedGame()
    }

    suspend fun recordGameWin(history: GameHistoryEntity) {
        dao.insertHistory(history)
    }

    suspend fun clearHistory() {
        dao.clearHistory()
    }
}
