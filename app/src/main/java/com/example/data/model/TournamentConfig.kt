package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "config")
data class TournamentConfig(
    @PrimaryKey val key: String,
    val value: String
)

data class StandingRow(
    val ward: Int,
    val played: Int,
    val won: Int,
    val lost: Int,
    val tie: Int,
    val runsFor: Int,
    val oversFor: Double,
    val runsAgainst: Int,
    val oversAgainst: Double,
    val points: Int,
    val nrr: Double
)
