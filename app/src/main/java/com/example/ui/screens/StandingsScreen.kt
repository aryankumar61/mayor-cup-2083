package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StandingRow
import com.example.data.model.TournamentAccount
import com.example.data.model.TournamentMatch
import com.example.ui.theme.CricketGold
import com.example.ui.theme.CricketGreen
import com.example.ui.theme.CricketPrimary
import java.util.Locale

@Composable
fun StandingsScreen(
    standings: List<StandingRow>,
    matches: List<TournamentMatch>,
    configs: Map<String, String>,
    currentUser: TournamentAccount?,
    onSaveAwardsAndConfig: (
        champion: String,
        runnerup: String,
        mom: String,
        bestbat: String,
        bestbowl: String,
        maxovers: String
    ) -> Unit
) {
    val isAdmin = currentUser?.role == "admin"

    // Derive auto winner and runner-up from Final match if present
    val finalMatch = matches.find { it.stage.equals("Final", ignoreCase = true) }
    val autoChampion = if (finalMatch != null) {
        val win = finalMatch.calculatedWinner()
        if (win > 0) "Ward $win" else ""
    } else ""
    val autoRunnerUp = if (finalMatch != null && autoChampion.isNotBlank()) {
        val win = finalMatch.calculatedWinner()
        val runner = if (finalMatch.teamA == win) finalMatch.teamB else finalMatch.teamA
        "Ward $runner"
    } else ""

    var champion by remember(configs) { mutableStateOf(configs["champion"] ?: "") }
    var runnerup by remember(configs) { mutableStateOf(configs["runnerup"] ?: "") }
    var mom by remember(configs) { mutableStateOf(configs["mom"] ?: "") }
    var bestbat by remember(configs) { mutableStateOf(configs["bestbat"] ?: "") }
    var bestbowl by remember(configs) { mutableStateOf(configs["bestbowl"] ?: "") }
    var maxovers by remember(configs) { mutableStateOf(configs["maxovers"] ?: "10") }

    val anyLeaguePlayed = remember(matches) {
        matches.any { it.stage.equals("League", ignoreCase = true) && it.calculatedResult().isNotEmpty() }
    }

    val honorCards = listOf(
        Triple("🏆 Winner (Champion)", champion, autoChampion),
        Triple("🥈 Runner-up", runnerup, autoRunnerUp),
        Triple("⭐ Man of the Tournament", mom, ""),
        Triple("🏏 Best Batsman", bestbat, ""),
        Triple("🎯 Best Bowler", bestbowl, "")
    )
    val showHonorsSection = isAdmin || honorCards.any { (_, v, auto) -> v.isNotBlank() || auto.isNotBlank() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        if (showHonorsSection) {
            Text(
                text = "🏆 Tournament Honors & Awards",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isAdmin || champion.isNotBlank() || autoChampion.isNotBlank()) {
                    HonorCard(
                        title = "🏆 Winner (Champion)",
                        value = if (isAdmin) champion else champion.ifBlank { autoChampion },
                        isAdmin = isAdmin,
                        onValueChange = { champion = it },
                        placeholder = if (autoChampion.isNotBlank()) "Auto: $autoChampion" else "Ward Team"
                    )
                }
                if (isAdmin || runnerup.isNotBlank() || autoRunnerUp.isNotBlank()) {
                    HonorCard(
                        title = "🥈 Runner-up",
                        value = if (isAdmin) runnerup else runnerup.ifBlank { autoRunnerUp },
                        isAdmin = isAdmin,
                        onValueChange = { runnerup = it },
                        placeholder = if (autoRunnerUp.isNotBlank()) "Auto: $autoRunnerUp" else "Ward Team"
                    )
                }
                if (isAdmin || mom.isNotBlank()) {
                    HonorCard(
                        title = "⭐ Man of the Tournament",
                        value = mom,
                        isAdmin = isAdmin,
                        onValueChange = { mom = it },
                        placeholder = "Player Name"
                    )
                }
                if (isAdmin || bestbat.isNotBlank()) {
                    HonorCard(
                        title = "🏏 Best Batsman",
                        value = bestbat,
                        isAdmin = isAdmin,
                        onValueChange = { bestbat = it },
                        placeholder = "Player Name"
                    )
                }
                if (isAdmin || bestbowl.isNotBlank()) {
                    HonorCard(
                        title = "🎯 Best Bowler",
                        value = bestbowl,
                        isAdmin = isAdmin,
                        onValueChange = { bestbowl = it },
                        placeholder = "Player Name"
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        // Standings Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "📊 League Points Table",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Surface(
                color = CricketGreen.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "Top 4 Qualify for Semi-Finals",
                    color = CricketGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Full Points Table with Horizontal Scroll
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    RoundedCornerShape(12.dp)
                )
        ) {
            Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TableCell(text = "#", width = 36.dp, isHeader = true)
                        TableCell(text = "Team", width = 90.dp, isHeader = true, align = TextAlign.Start)
                        TableCell(text = "P", width = 38.dp, isHeader = true)
                        TableCell(text = "W", width = 38.dp, isHeader = true)
                        TableCell(text = "L", width = 38.dp, isHeader = true)
                        TableCell(text = "T/NR", width = 45.dp, isHeader = true)
                        TableCell(text = "Runs For", width = 68.dp, isHeader = true)
                        TableCell(text = "Overs", width = 56.dp, isHeader = true)
                        TableCell(text = "Runs Ag.", width = 68.dp, isHeader = true)
                        TableCell(text = "Overs", width = 56.dp, isHeader = true)
                        TableCell(text = "Pts", width = 45.dp, isHeader = true)
                        TableCell(text = "NRR", width = 74.dp, isHeader = true)
                    }

                    Spacer(Modifier.height(4.dp))

                    // Team Rows
                    standings.forEachIndexed { index, row ->
                        val isTop4 = anyLeaguePlayed && index < 4
                        val rowBg = if (isTop4) CricketGreen.copy(alpha = 0.09f) else Color.Transparent

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(rowBg, RoundedCornerShape(4.dp))
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TableCell(text = "${index + 1}", width = 36.dp, isBold = isTop4)
                            TableCell(
                                text = "Ward ${row.ward}",
                                width = 90.dp,
                                align = TextAlign.Start,
                                isBold = true,
                                color = if (isTop4) CricketGreen else MaterialTheme.colorScheme.onSurface
                            )
                            TableCell(text = "${row.played}", width = 38.dp)
                            TableCell(text = "${row.won}", width = 38.dp)
                            TableCell(text = "${row.lost}", width = 38.dp)
                            TableCell(text = "${row.tie}", width = 45.dp)
                            TableCell(text = "${row.runsFor}", width = 68.dp)
                            TableCell(text = String.format(Locale.US, "%.1f", row.oversFor), width = 56.dp)
                            TableCell(text = "${row.runsAgainst}", width = 68.dp)
                            TableCell(text = String.format(Locale.US, "%.1f", row.oversAgainst), width = 56.dp)
                            TableCell(text = "${row.points}", width = 45.dp, isBold = true, color = CricketPrimary)
                            val nrrPrefix = if (row.nrr > 0) "+" else ""
                            TableCell(
                                text = "$nrrPrefix${String.format(Locale.US, "%.3f", row.nrr)}",
                                width = 74.dp,
                                isBold = true,
                                color = if (row.nrr >= 0) CricketGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Explanation text
        Text(
            text = "ℹ Auto-calculated from completed League matches. Win = 2 pts, Tie/No-result = 1 pt. Top 4 teams advance to Semi-Finals (SF1: 1st vs 4th, SF2: 2nd vs 3rd). NRR = (Runs For ÷ Overs Faced) − (Runs Against ÷ Overs Bowled). If a team is all out, full quota overs are counted.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
        )

        // Admin Save & Config Section
        if (isAdmin) {
            Spacer(Modifier.height(14.dp))
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("⚙ Tournament Rules & Save", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = maxovers,
                        onValueChange = { maxovers = it },
                        label = { Text("Max overs per innings (all-out NRR rule)") },
                        placeholder = { Text("e.g. 10") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(0.6f)
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            onSaveAwardsAndConfig(champion, runnerup, mom, bestbat, bestbowl, maxovers)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CricketGreen),
                        modifier = Modifier.testTag("save_standings_config_button")
                    ) {
                        Text("💾 Save Winners & Settings")
                    }
                }
            }
        }
    }
}

@Composable
fun TableCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    align: TextAlign = TextAlign.Center,
    isHeader: Boolean = false,
    isBold: Boolean = false,
    color: Color = Color.Unspecified
) {
    Text(
        text = text,
        fontSize = if (isHeader) 11.sp else 12.sp,
        fontWeight = if (isHeader || isBold) FontWeight.Bold else FontWeight.Normal,
        textAlign = align,
        color = if (color != Color.Unspecified) color else if (isHeader) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.width(width)
    )
}

@Composable
fun HonorCard(
    title: String,
    value: String,
    isAdmin: Boolean,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Surface(
        color = Color(0xFFFFF9E6),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFE082)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.width(160.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF7A5B00),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            if (isAdmin) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = { Text(placeholder, fontSize = 11.sp) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = value.ifBlank { "TBD" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E2723),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
