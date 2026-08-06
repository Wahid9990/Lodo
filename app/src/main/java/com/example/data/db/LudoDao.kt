package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.GameHistoryEntity
import com.example.model.GameSaveEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LudoDao {
    @Query("SELECT * FROM game_save WHERE id = 1 LIMIT 1")
    fun getSavedGame(): Flow<GameSaveEntity?>

    @Query("SELECT * FROM game_save WHERE id = 1 LIMIT 1")
    suspend fun getSavedGameDirect(): GameSaveEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGame(gameSave: GameSaveEntity)

    @Query("DELETE FROM game_save WHERE id = 1")
    suspend fun clearSavedGame()

    @Query("SELECT * FROM game_history ORDER BY timestamp DESC")
    fun getGameHistory(): Flow<List<GameHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: GameHistoryEntity)

    @Query("DELETE FROM game_history")
    suspend fun clearHistory()
}
