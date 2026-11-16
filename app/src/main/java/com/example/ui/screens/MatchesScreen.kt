package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.TournamentAccount
import com.example.data.model.TournamentMatch
import com.example.ui.theme.CricketAccent
import com.example.ui.theme.CricketGold
import com.example.ui.theme.CricketGreen
import com.example.ui.theme.CricketPrimary
import com.example.ui.theme.CricketRed
import java.util.Locale

@Composable
fun MatchesScreen(
    matches: List<TournamentMatch>,
    currentUser: TournamentAccount?,
    onAddMatchClick: () -> Unit,
    onEditMatchClick: (TournamentMatch) -> Unit,
    onDeleteMatchClick: (Int) -> Unit,
    onAutoSemiFinalClick: () -> Unit
) {
    val isAdmin = currentUser?.role == "admin"
    var selectedStage by remember { mutableStateOf("All") }

    val filteredMatches = remember(matches, selectedStage) {
        if (selectedStage == "All") matches else matches.filter { it.stage.equals(selectedStage, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Admin top action bar
        if (isAdmin) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAddMatchClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CricketAccent),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_match_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Match")
                }

                Button(
                    onClick = onAutoSemiFinalClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CricketGreen),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("auto_brackets_button")
                ) {
                    Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Auto Semi / Final")
                }
            }
        }

        // Stage filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("All", "League", "Semi Final", "Final").forEach { stage ->
                FilterChip(
                    selected = selectedStage == stage,
                    onClick = { selectedStage = stage },
                    label = { Text(stage) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CricketPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        if (filteredMatches.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No matches scheduled yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredMatches, key = { it.no }) { match ->
                    MatchCard(
                        match = match,
                        isAdmin = isAdmin,
                        onEdit = { onEditMatchClick(match) },
                        onDelete = { onDeleteMatchClick(match.no) }
                    )
                }
            }
        }
    }
}

@Composable
fun MatchCard(
    match: TournamentMatch,
    isAdmin: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val result = match.calculatedResult()
    val winner = match.calculatedWinner()
    var showDeleteConfirm by remember(match.no) { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Match?") },
            text = { Text("Delete match ${match.no}?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CricketRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val stageBgColor = when (match.stage) {
        "Final" -> Color(0xFFFFECB3)
        "Semi Final" -> Color(0xFFFFE0B2)
        else -> Color(0xFFE3F2FD)
    }

    val stageTextColor = when (match.stage) {
        "Final" -> Color(0xFF7A5B00)
        "Semi Final" -> Color(0xFFE65100)
        else -> CricketPrimary
    }

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
            .testTag("match_card_${match.no}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Match No, Stage, Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Match ${match.no}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    Surface(
                        color = stageBgColor,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = match.stage,
                            color = stageTextColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (match.date.isNotBlank()) {
                    Text(
                        text = match.date,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Team A line
            TeamScoreLine(
                teamWard = match.teamA,
                runs = match.runsA,
                wickets = match.wicketsA,
                overs = match.oversA,
                isWinner = result == "A"
            )

            Spacer(Modifier.height(6.dp))

            // Team B line
            TeamScoreLine(
                teamWard = match.teamB,
                runs = match.runsB,
                wickets = match.wicketsB,
                overs = match.oversB,
                isWinner = result == "B"
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(Modifier.height(6.dp))

            // Result and Venue
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val resultText = when (result) {
                    "A", "B" -> "🏆 Ward $winner won"
                    "tie" -> "Match Tied"
                    "nr" -> "No Result"
                    else -> "Upcoming Match"
                }

                val resultColor = when (result) {
                    "A", "B" -> CricketGreen
                    "tie" -> CricketGold
                    "nr" -> CricketRed
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Text(
                    text = resultText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = resultColor
                )

                if (match.note.isNotBlank()) {
                    Text(
                        text = match.note,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Admin action buttons
            if (isAdmin) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onEdit,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Edit", fontSize = 11.sp)
                    }

                    Spacer(Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CricketRed),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun TeamScoreLine(
    teamWard: Int,
    runs: Int,
    wickets: Int,
    overs: Double,
    isWinner: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isWinner) CricketGreen.copy(alpha = 0.08f) else Color.Transparent,
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Ward $teamWard",
                fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp,
                color = if (isWinner) CricketGreen else MaterialTheme.colorScheme.onSurface
            )
            if (isWinner) {
                Spacer(Modifier.width(4.dp))
                Text("🏆", fontSize = 12.sp)
            }
        }

        if (overs > 0.0) {
            val runRate = if (overs > 0.0) {
                val fullOvers = kotlin.math.floor(overs)
                val balls = kotlin.math.round((overs % 1.0) * 10)
                val oversFraction = fullOvers + (balls / 6.0)
                if (oversFraction > 0) String.format(Locale.US, "%.2f", runs / oversFraction) else "-"
            } else "-"

            Text(
                text = "$runs/$wickets ($overs ov · RR $runRate)",
                fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp,
                color = if (isWinner) CricketGreen else MaterialTheme.colorScheme.onSurface
            )
        } else {
            Text(
                text = "-",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchEditDialog(
    isOpen: Boolean,
    match: TournamentMatch?,
    isEditingExisting: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (TournamentMatch) -> Unit
) {
    if (!isOpen || match == null) return

    var no by remember(match) { mutableStateOf(match.no.toString()) }
    var stage by remember(match) { mutableStateOf(match.stage) }
    var teamA by remember(match) { mutableStateOf(match.teamA) }
    var teamB by remember(match) { mutableStateOf(match.teamB) }
    var runsA by remember(match) { mutableStateOf(if (match.runsA > 0) match.runsA.toString() else "") }
    var wicketsA by remember(match) { mutableStateOf(if (match.wicketsA > 0) match.wicketsA.toString() else "") }
    var oversA by remember(match) { mutableStateOf(if (match.oversA > 0.0) match.oversA.toString() else "") }
    var runsB by remember(match) { mutableStateOf(if (match.runsB > 0) match.runsB.toString() else "") }
    var wicketsB by remember(match) { mutableStateOf(if (match.wicketsB > 0) match.wicketsB.toString() else "") }
    var oversB by remember(match) { mutableStateOf(if (match.oversB > 0.0) match.oversB.toString() else "") }
    var result by remember(match) { mutableStateOf(match.result) }
    var date by remember(match) { mutableStateOf(match.date) }
    var note by remember(match) { mutableStateOf(match.note) }

    val stages = listOf("League", "Semi Final", "Final")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Match Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = no,
                            onValueChange = { no = it },
                            label = { Text("Match No") },
                            enabled = !isEditingExisting,
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        var stageExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = stageExpanded,
                            onExpandedChange = { stageExpanded = it },
                            modifier = Modifier.weight(1.5f)
                        ) {
                            OutlinedTextField(
                                value = stage,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Stage") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stageExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = stageExpanded,
                                onDismissRequest = { stageExpanded = false }
                            ) {
                                stages.forEach { st ->
                                    DropdownMenuItem(
                                        text = { Text(st) },
                                        onClick = {
                                            stage = st
                                            stageExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Teams dropdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        var aExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = aExpanded,
                            onExpandedChange = { aExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = "Ward $teamA",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Team A") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = aExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = aExpanded,
                                onDismissRequest = { aExpanded = false }
                            ) {
                                (1..9).forEach { w ->
                                    DropdownMenuItem(
                                        text = { Text("Ward $w") },
                                        onClick = {
                                            teamA = w
                                            aExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        var bExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = bExpanded,
                            onExpandedChange = { bExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = "Ward $teamB",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Team B") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = bExpanded,
                                onDismissRequest = { bExpanded = false }
                            ) {
                                (1..9).forEach { w ->
                                    DropdownMenuItem(
                                        text = { Text("Ward $w") },
                                        onClick = {
                                            teamB = w
                                            bExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Team A score
                    Text("Team A Score (Ward $teamA)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = runsA,
                            onValueChange = { runsA = it },
                            label = { Text("Runs") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = wicketsA,
                            onValueChange = { wicketsA = it },
                            label = { Text("Wkts") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = oversA,
                            onValueChange = { oversA = it },
                            label = { Text("Overs") },
                            placeholder = { Text("e.g. 10.0") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Team B score
                    Text("Team B Score (Ward $teamB)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = runsB,
                            onValueChange = { runsB = it },
                            label = { Text("Runs") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = wicketsB,
                            onValueChange = { wicketsB = it },
                            label = { Text("Wkts") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = oversB,
                            onValueChange = { oversB = it },
                            label = { Text("Overs") },
                            placeholder = { Text("e.g. 9.4") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Result dropdown
                    var resultExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = resultExpanded,
                        onExpandedChange = { resultExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val resultLabel = when (result) {
                            "A" -> "Team A won (Ward $teamA)"
                            "B" -> "Team B won (Ward $teamB)"
                            "tie" -> "Tie"
                            "nr" -> "No Result"
                            else -> "Auto (Calculated from runs)"
                        }
                        OutlinedTextField(
                            value = resultLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Result") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = resultExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = resultExpanded,
                            onDismissRequest = { resultExpanded = false }
                        ) {
                            DropdownMenuItem(text = { Text("Auto (Higher runs wins)") }, onClick = { result = ""; resultExpanded = false })
                            DropdownMenuItem(text = { Text("Team A won (Ward $teamA)") }, onClick = { result = "A"; resultExpanded = false })
                            DropdownMenuItem(text = { Text("Team B won (Ward $teamB)") }, onClick = { result = "B"; resultExpanded = false })
                            DropdownMenuItem(text = { Text("Tie") }, onClick = { result = "tie"; resultExpanded = false })
                            DropdownMenuItem(text = { Text("No Result") }, onClick = { result = "nr"; resultExpanded = false })
                        }
                    }

                    // Date & Venue note
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Venue / Note / Man of the Match") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val saved = match.copy(
                                no = no.toIntOrNull() ?: match.no,
                                stage = stage,
                                teamA = teamA,
                                teamB = teamB,
                                runsA = runsA.toIntOrNull() ?: 0,
                                wicketsA = wicketsA.toIntOrNull() ?: 0,
                                oversA = oversA.toDoubleOrNull() ?: 0.0,
                                runsB = runsB.toIntOrNull() ?: 0,
                                wicketsB = wicketsB.toIntOrNull() ?: 0,
                                oversB = oversB.toDoubleOrNull() ?: 0.0,
                                result = result,
                                date = date.trim(),
                                note = note.trim()
                            )
                            onSave(saved)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CricketPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_match_button")
                    ) {
                        Text("Save Match")
                    }
                }
            }
        }
    }
}
