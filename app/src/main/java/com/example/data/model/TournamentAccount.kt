package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class TournamentAccount(
    @PrimaryKey val username: String,
    val password: String,
    val displayName: String,
    val role: String, // "admin" or "captain"
    val ward: Int = 0 // 1..9 for captain, 0 for admin
)
