package com.example.data.db

import androidx.room.*
import com.example.data.model.Player
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players ORDER BY ward ASC, CAST(jersey AS INTEGER) ASC")
    fun getAllPlayersFlow(): Flow<List<Player>>

    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun getPlayerById(id: String): Player?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(player: Player)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(players: List<Player>)

    @Query("DELETE FROM players")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(players: List<Player>) {
        deleteAll()
        if (players.isNotEmpty()) {
            insertAll(players)
        }
    }

    @Query("UPDATE players SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM players WHERE id = :id")
    suspend fun deletePlayer(id: String)

    @Query("SELECT COUNT(*) FROM players")
    suspend fun getCount(): Int
}
