package com.example.data.db

import androidx.room.*
import com.example.data.model.TournamentAccount
import com.example.data.model.TournamentConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY username ASC")
    fun getAllAccountsFlow(): Flow<List<TournamentAccount>>

    @Query("SELECT * FROM accounts WHERE username = :username LIMIT 1")
    suspend fun getAccount(username: String): TournamentAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(account: TournamentAccount)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<TournamentAccount>)

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(accounts: List<TournamentAccount>) {
        deleteAll()
        if (accounts.isNotEmpty()) {
            insertAll(accounts)
        }
    }

    @Query("DELETE FROM accounts WHERE username = :username")
    suspend fun deleteAccount(username: String)

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getCount(): Int
}

@Dao
interface ConfigDao {
    @Query("SELECT * FROM config")
    fun getAllConfigsFlow(): Flow<List<TournamentConfig>>

    @Query("SELECT value FROM config WHERE `key` = :key LIMIT 1")
    suspend fun getConfigValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setConfig(config: TournamentConfig)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setAll(configs: List<TournamentConfig>)
}
