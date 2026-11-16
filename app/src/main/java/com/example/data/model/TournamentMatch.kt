package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "matches")
data class TournamentMatch(
    @PrimaryKey val no: Int,
    val stage: String = "League", // "League", "Semi Final", "Final"
    val teamA: Int = 1,
    val teamB: Int = 2,
    val runsA: Int = 0,
    val wicketsA: Int = 0,
    val oversA: Double = 0.0,
    val runsB: Int = 0,
    val wicketsB: Int = 0,
    val oversB: Double = 0.0,
    val result: String = "", // "", "A", "B", "tie", "nr"
    val date: String = "",
    val note: String = ""
) {
    fun calculatedWinner(): Int {
        val r = calculatedResult()
        return when (r) {
            "A" -> teamA
            "B" -> teamB
            else -> 0
        }
    }

    fun calculatedResult(): String {
        if (result in listOf("A", "B", "tie", "nr")) return result
        if (oversA <= 0.0 || oversB <= 0.0) return ""
        return when {
            runsA > runsB -> "A"
            runsB > runsA -> "B"
            else -> "tie"
        }
    }
}
