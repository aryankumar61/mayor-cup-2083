package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class Player(
    @PrimaryKey val id: String,
    val ward: Int,
    val jersey: String = "",
    val name: String,
    val father: String = "",
    val dob: String = "",
    val mobile: String = "",
    val blood: String = "",
    val address: String = "",
    val role: String = "Batsman",
    val bat: String = "Right Hand",
    val bowl: String = "None",
    val cap: String = "",
    val photo: String = "",
    val status: String = "approved" // "approved", "pending", "rejected"
)
