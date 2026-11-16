package com.example.data.db

import androidx.room.*
import com.example.data.model.TournamentMatch
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches ORDER BY no ASC")
    fun getAllMatchesFlow(): Flow<List<TournamentMatch>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(match: TournamentMatch)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(matches: List<TournamentMatch>)

    @Query("DELETE FROM matches")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(matches: List<TournamentMatch>) {
        deleteAll()
        if (matches.isNotEmpty()) {
            insertAll(matches)
        }
    }

    @Query("DELETE FROM matches WHERE no = :no")
    suspend fun deleteMatch(no: Int)

    @Query("SELECT COUNT(*) FROM matches")
    suspend fun getCount(): Int
}
