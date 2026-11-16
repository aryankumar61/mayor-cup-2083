package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.OutfitFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersScreen(
    players: List<Player>,
    allPlayers: List<Player>,
    selectedWard: Int,
    statusFilter: String,
    searchQuery: String,
    currentUser: TournamentAccount?,
    uploadedPaperWards: Set<Int>,
    onWardSelected: (Int) -> Unit,
    onStatusFilterSelected: (String) -> Unit,
    onSearchChanged: (String) -> Unit,
    onPlayerClick: (Player) -> Unit,
    onAddPlayerClick: () -> Unit,
    onOpenSifarisClick: () -> Unit,
    onExportCsvClick: () -> Unit
) {
    val isAdmin = currentUser?.role == "admin"
    val isCaptain = currentUser?.role == "captain"

    val approvedCount = allPlayers.count { it.status == "approved" }
    val pendingCount = allPlayers.count { it.status == "pending" }
    val batsmanCount = allPlayers.count { it.status == "approved" && it.role.equals("Batsman", true) }
    val bowlerCount = allPlayers.count { it.status == "approved" && it.role.equals("Bowler", true) }
    val allRounderCount = allPlayers.count { it.status == "approved" && it.role.equals("All-rounder", true) }
    val wkCount = allPlayers.count { it.status == "approved" && it.role.equals("Wicket Keeper", true) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 880.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Stats Overview Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(count = approvedCount.toString(), label = "Approved", highlight = CricketPrimary)
                if (isAdmin || isCaptain) {
                    StatCard(count = pendingCount.toString(), label = "Pending", highlight = CricketAccent)
                }
                StatCard(count = batsmanCount.toString(), label = "Batsmen")
                StatCard(count = bowlerCount.toString(), label = "Bowlers")
                StatCard(count = allRounderCount.toString(), label = "All-rounders")
                StatCard(count = wkCount.toString(), label = "WKs")
            }

            // Captain or Admin Sifaris Verification Banner
            if (isCaptain && currentUser != null) {
                Spacer(Modifier.height(10.dp))
                val hasSifaris = currentUser.ward in uploadedPaperWards
                Surface(
                    color = if (hasSifaris) CricketGreen.copy(alpha = 0.1f) else CricketGold.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (hasSifaris) CricketGreen.copy(alpha = 0.35f) else CricketGold.copy(alpha = 0.45f),
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = if (hasSifaris) {
                                    "Ward ${currentUser.ward} Sifaris Verified"
                                } else {
                                    "Upload Ward ${currentUser.ward} Sifaris First"
                                },
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (hasSifaris) CricketGreen else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (hasSifaris) {
                                    "One-time ward recommendation letter uploaded"
                                } else {
                                    "Required one time before adding Ward ${currentUser.ward} players"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(
                            onClick = onOpenSifarisClick,
                            modifier = Modifier.testTag("captain_sifaris_button")
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (hasSifaris) "View" else "Upload")
                        }
                    }
                }
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

            // Search Bar, Status Dropdown, Sifaris/Add/CSV Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChanged,
                    placeholder = { Text("Search name, jersey, phone...") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChanged("") }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("player_search_input")
                )

                if (currentUser != null) {
                    var statusExpanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { statusExpanded = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(
                                when (statusFilter) {
                                    "pending" -> "Pending"
                                    "approved" -> "Approved"
                                    "rejected" -> "Rejected"
                                    else -> "Status: All"
                                },
                                style = MaterialTheme.typography.labelMedium
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
                                text = { Text("Pending") },
                                onClick = {
                                    onStatusFilterSelected("pending")
                                    statusExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Approved") },
                                onClick = {
                                    onStatusFilterSelected("approved")
                                    statusExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Rejected") },
                                onClick = {
                                    onStatusFilterSelected("rejected")
                                    statusExpanded = false
                                }
                            )
                        }
                    }

                    Button(
                        onClick = onAddPlayerClick,
                        colors = ButtonDefaults.buttonColors(containerColor = CricketAccent),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("add_player_fab")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (isAdmin) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onOpenSifarisClick,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_sifaris_button")
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ward Sifaris (${uploadedPaperWards.size}/9)")
                    }

                    OutlinedButton(
                        onClick = onExportCsvClick,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Export CSV",
                            tint = CricketGreen,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Export CSV", color = CricketGreen)
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
                            fontFamily = OutfitFontFamily,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Try clearing filters or search query",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 152.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
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
}

@Composable
fun StatCard(count: String, label: String, highlight: Color? = null) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .widthIn(min = 96.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp,
                color = highlight ?: MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PlayerCard(player: Player, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(14.dp)
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
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "W${player.ward}",
                        color = Color.White,
                        fontFamily = OutfitFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }

                // Jersey badge top-right
                if (player.jersey.isNotBlank()) {
                    Surface(
                        color = CricketPrimary,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "#${player.jersey}",
                            color = Color.White,
                            fontFamily = OutfitFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                // Captaincy badge bottom-right
                if (player.cap.isNotBlank()) {
                    Surface(
                        color = CricketGold,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = if (player.cap.equals("Captain", true)) "C" else "VC",
                            color = Color.Black,
                            fontFamily = OutfitFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Status badge bottom-left
                if (player.status != "approved") {
                    Surface(
                        color = if (player.status == "pending") CricketGold else CricketRed,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = if (player.status == "pending") "Pending" else "Rejected",
                            color = if (player.status == "pending") Color.Black else Color.White,
                            fontFamily = OutfitFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Info below photo
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 9.dp)
            ) {
                Text(
                    text = player.name,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = player.role,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
