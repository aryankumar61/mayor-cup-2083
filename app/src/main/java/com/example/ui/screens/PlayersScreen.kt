package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.material.icons.filled.Shield
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
    val isAdmin = currentUser?.role?.trim()?.equals("admin", ignoreCase = true) == true
    val isCaptain = currentUser?.role?.trim()?.equals("captain", ignoreCase = true) == true

    val approvedCount = allPlayers.count { it.status.equals("approved", ignoreCase = true) }
    val pendingCount = allPlayers.count { it.status.equals("pending", ignoreCase = true) }
    val batsmanCount = allPlayers.count { it.status.equals("approved", ignoreCase = true) && it.role.equals("Batsman", true) }
    val bowlerCount = allPlayers.count { it.status.equals("approved", ignoreCase = true) && it.role.equals("Bowler", true) }
    val allRounderCount = allPlayers.count { it.status.equals("approved", ignoreCase = true) && it.role.equals("All-rounder", true) }
    val wkCount = allPlayers.count { it.status.equals("approved", ignoreCase = true) && it.role.equals("Wicket Keeper", true) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 156.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(
                start = 14.dp,
                end = 14.dp,
                top = 12.dp,
                bottom = 84.dp
            ),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 880.dp)
                .testTag("players_grid")
        ) {
            // 1. After-Login Role Dashboard Banner & Actions
            if (currentUser != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    DashboardRoleBanner(
                        currentUser = currentUser,
                        isAdmin = isAdmin,
                        isCaptain = isCaptain,
                        uploadedPaperWards = uploadedPaperWards,
                        pendingCount = pendingCount,
                        onAddPlayerClick = onAddPlayerClick,
                        onOpenSifarisClick = onOpenSifarisClick,
                        onExportCsvClick = onExportCsvClick
                    )
                }
            }

            // 2. Clean 3x2 Stats Grid (never clipped or scrolling off-screen)
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            count = approvedCount.toString(),
                            label = "Approved",
                            highlight = CricketPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        if (isAdmin || isCaptain) {
                            StatCard(
                                count = pendingCount.toString(),
                                label = "Pending",
                                highlight = CricketAccent,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            StatCard(
                                count = "9",
                                label = "Ward Teams",
                                highlight = CricketGreen,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        StatCard(
                            count = batsmanCount.toString(),
                            label = "Batsmen",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            count = bowlerCount.toString(),
                            label = "Bowlers",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            count = allRounderCount.toString(),
                            label = "All-rounders",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            count = wkCount.toString(),
                            label = "Wicket Keepers",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. Full-width Search Bar (never squished alongside buttons)
            item(span = { GridItemSpan(maxLineSpan) }) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChanged,
                    placeholder = { Text("Search name, jersey, phone, citizenship...") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChanged("") }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player_search_input")
                )
            }

            // 4. Status Filter Chips (for Admin / Captain)
            if (currentUser != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    val statuses = listOf(
                        "" to "All Status (${allPlayers.size})",
                        "pending" to "Pending ($pendingCount)",
                        "approved" to "Approved ($approvedCount)",
                        "rejected" to "Rejected (${allPlayers.count { it.status.equals("rejected", true) }})"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        statuses.forEach { (key, label) ->
                            FilterChip(
                                selected = statusFilter == key,
                                onClick = { onStatusFilterSelected(key) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CricketAccent,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // 5. Ward Filter Chips (All, Ward 1..9)
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedWard == 0,
                        onClick = { onWardSelected(0) },
                        label = { Text("All Wards (${allPlayers.size})") },
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
            }

            // 6. Players Grid or Empty State
            if (players.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🏏", fontSize = 44.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "No players found",
                                fontFamily = OutfitFontFamily,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (currentUser != null) "Adjust your filters or add a new player." else "Try clearing filters or search query.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (currentUser != null) {
                                Spacer(Modifier.height(14.dp))
                                Button(
                                    onClick = onAddPlayerClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = CricketAccent),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Add Player", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                items(players, key = { it.id }) { player ->
                    PlayerCard(
                        player = player,
                        showSensitiveStatus = currentUser != null,
                        onClick = { onPlayerClick(player) }
                    )
                }
            }
        }

        // Floating Action Button for logged-in Admin / Captain
        if (currentUser != null) {
            ExtendedFloatingActionButton(
                onClick = onAddPlayerClick,
                containerColor = CricketAccent,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Player", fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("add_player_fab")
            )
        }
    }
}

@Composable
private fun DashboardRoleBanner(
    currentUser: TournamentAccount,
    isAdmin: Boolean,
    isCaptain: Boolean,
    uploadedPaperWards: Set<Int>,
    pendingCount: Int,
    onAddPlayerClick: () -> Unit,
    onOpenSifarisClick: () -> Unit,
    onExportCsvClick: () -> Unit
) {
    val hasCaptainSifaris = isCaptain && (currentUser.ward in uploadedPaperWards)
    val bannerColor = when {
        isAdmin -> CricketPrimary.copy(alpha = 0.08f)
        hasCaptainSifaris -> CricketGreen.copy(alpha = 0.09f)
        else -> CricketGold.copy(alpha = 0.14f)
    }
    val borderColor = when {
        isAdmin -> CricketPrimary.copy(alpha = 0.25f)
        hasCaptainSifaris -> CricketGreen.copy(alpha = 0.35f)
        else -> CricketGold.copy(alpha = 0.45f)
    }

    Surface(
        color = bannerColor,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = if (isAdmin) CricketPrimary else if (hasCaptainSifaris) CricketGreen else CricketAccent,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = currentUser.displayName.ifBlank { currentUser.username },
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when {
                                isAdmin -> "Committee Admin · $pendingCount pending review"
                                hasCaptainSifaris -> "Ward ${currentUser.ward} Sifaris Verified"
                                else -> "Upload Ward ${currentUser.ward} Sifaris before adding players"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAddPlayerClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CricketAccent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Player", maxLines = 1, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = onOpenSifarisClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .testTag(if (isAdmin) "admin_sifaris_button" else "captain_sifaris_button")
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (isAdmin) "Sifaris (${uploadedPaperWards.size}/9)"
                        else if (hasCaptainSifaris) "View Sifaris"
                        else "Upload Sifaris",
                        maxLines = 1,
                        fontSize = 13.sp
                    )
                }

                if (isAdmin) {
                    OutlinedButton(
                        onClick = onExportCsvClick,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.heightIn(min = 44.dp)
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Export CSV",
                            tint = CricketGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("CSV", color = CricketGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    count: String,
    label: String,
    modifier: Modifier = Modifier,
    highlight: Color? = null
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.border(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
            RoundedCornerShape(12.dp)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = highlight ?: MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PlayerCard(
    player: Player,
    showSensitiveStatus: Boolean = false,
    onClick: () -> Unit
) {
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
                    contentDescription = player.name
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
                if (showSensitiveStatus && !player.status.equals("approved", ignoreCase = true)) {
                    Surface(
                        color = if (player.status.equals("pending", ignoreCase = true)) CricketGold else CricketRed,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = if (player.status.equals("pending", ignoreCase = true)) "Pending" else "Rejected",
                            color = if (player.status.equals("pending", ignoreCase = true)) Color.Black else Color.White,
                            fontFamily = OutfitFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Player Details Summary below image
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
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
                    text = buildString {
                        append(player.role.ifBlank { "Player" })
                        if (player.bat.isNotBlank()) append(" · ${player.bat.removeSuffix(" Hand")}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (showSensitiveStatus && player.mobile.isNotBlank()) {
                    Text(
                        text = player.mobile,
                        style = MaterialTheme.typography.labelSmall,
                        color = CricketPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
