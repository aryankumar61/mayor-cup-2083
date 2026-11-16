package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Player
import com.example.data.model.TournamentAccount
import com.example.ui.components.PlayerAvatarImage
import com.example.ui.theme.CricketAccent
import com.example.ui.theme.CricketGold
import com.example.ui.theme.CricketGreen
import com.example.ui.theme.CricketPrimary
import com.example.ui.theme.CricketRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersScreen(
    players: List<Player>,
    allPlayers: List<Player>,
    selectedWard: Int,
    statusFilter: String,
    searchQuery: String,
    currentUser: TournamentAccount?,
    onWardSelected: (Int) -> Unit,
    onStatusFilterSelected: (String) -> Unit,
    onSearchChanged: (String) -> Unit,
    onPlayerClick: (Player) -> Unit,
    onAddPlayerClick: () -> Unit,
    onExportCsvClick: () -> Unit
) {
    val context = LocalContext.current
    val isAdmin = currentUser?.role == "admin"
    val isCaptain = currentUser?.role == "captain"

    val approvedCount = allPlayers.count { it.status == "approved" }
    val pendingCount = allPlayers.count { it.status == "pending" }
    val batsmanCount = allPlayers.count { it.status == "approved" && it.role.equals("Batsman", true) }
    val bowlerCount = allPlayers.count { it.status == "approved" && it.role.equals("Bowler", true) }
    val allRounderCount = allPlayers.count { it.status == "approved" && it.role.equals("All-rounder", true) }
    val wkCount = allPlayers.count { it.status == "approved" && it.role.equals("Wicket Keeper", true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Stats Overview Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(count = approvedCount.toString(), label = "Approved Players", highlight = CricketPrimary)
            if (isAdmin) {
                StatCard(count = pendingCount.toString(), label = "Pending", highlight = CricketAccent)
            }
            StatCard(count = batsmanCount.toString(), label = "Batsmen")
            StatCard(count = bowlerCount.toString(), label = "Bowlers")
            StatCard(count = allRounderCount.toString(), label = "All-rounders")
            StatCard(count = wkCount.toString(), label = "WKs")
        }

        Spacer(Modifier.height(10.dp))

        // Ward Filter Chips (All, Ward 1..9)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedWard == 0,
                onClick = { onWardSelected(0) },
                label = { Text("All (${allPlayers.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CricketPrimary,
                    selectedLabelColor = Color.White
                )
            )

            (1..9).forEach { w ->
                val countInWard = allPlayers.count { it.ward == w }
                FilterChip(
                    selected = selectedWard == w,
                    onClick = { onWardSelected(w) },
                    label = { Text("Ward $w ($countInWard)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CricketPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Search Bar, Status Dropdown, Add & CSV Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChanged,
                placeholder = { Text("Search name, jersey, mobile...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("player_search_input")
            )

            if (currentUser != null) {
                var statusExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { statusExpanded = true },
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            when (statusFilter) {
                                "pending" -> "⏳ Pending"
                                "approved" -> "✅ Approved"
                                "rejected" -> "❌ Rejected"
                                else -> "Status: All"
                            },
                            fontSize = 12.sp
                        )
                    }
                    DropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Statuses") },
                            onClick = {
                                onStatusFilterSelected("")
                                statusExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("⏳ Pending") },
                            onClick = {
                                onStatusFilterSelected("pending")
                                statusExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("✅ Approved") },
                            onClick = {
                                onStatusFilterSelected("approved")
                                statusExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("❌ Rejected") },
                            onClick = {
                                onStatusFilterSelected("rejected")
                                statusExpanded = false
                            }
                        )
                    }
                }
            }

            if (currentUser != null) {
                Button(
                    onClick = onAddPlayerClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CricketAccent),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.testTag("add_player_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (isAdmin) {
                IconButton(
                    onClick = onExportCsvClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Export CSV",
                        tint = CricketGreen
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Players Grid
        if (players.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏏", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "No players found",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Try clearing filters or search query",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("players_grid")
            ) {
                items(players, key = { it.id }) { player ->
                    PlayerCard(player = player, onClick = { onPlayerClick(player) })
                }
            }
        }
    }
}

@Composable
fun StatCard(count: String, label: String, highlight: Color? = null) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.widthIn(min = 100.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = highlight ?: MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PlayerCard(player: Player, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .testTag("player_card_${player.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                PlayerAvatarImage(
                    photo = player.photo,
                    contentDescription = player.name,
                    fallbackEmoji = "👤",
                    fallbackSize = 40.sp
                )

                // Ward badge top-left
                Surface(
                    color = CricketAccent,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = "W${player.ward}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Jersey badge top-right
                if (player.jersey.isNotBlank()) {
                    Surface(
                        color = CricketPrimary,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "#${player.jersey}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Captaincy badge bottom-right
                if (player.cap.isNotBlank()) {
                    Surface(
                        color = CricketGold,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = if (player.cap.equals("Captain", true)) "C" else "VC",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                // Status badge bottom-left
                if (player.status != "approved") {
                    Surface(
                        color = if (player.status == "pending") CricketGold else CricketRed,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = if (player.status == "pending") "⏳ Pending" else "Rejected",
                            color = if (player.status == "pending") Color.Black else Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // Info below photo
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = player.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = player.role,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
