package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Player
import com.example.data.model.TournamentAccount
import com.example.ui.theme.CricketGold
import com.example.ui.theme.CricketGreen
import com.example.ui.theme.CricketPrimary
import com.example.ui.theme.CricketRed
import java.time.LocalDate
import java.time.Period

@Composable
fun PlayerProfileDialog(
    player: Player?,
    currentUser: TournamentAccount?,
    onDismiss: () -> Unit,
    onEdit: (Player) -> Unit,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    if (player == null) return
    val context = LocalContext.current
    val isAdmin = currentUser?.role == "admin"
    val isCaptain = currentUser?.role == "captain" && currentUser.ward == player.ward
    val canEdit = isAdmin || isCaptain

    var showDeleteConfirm by remember(player.id) { mutableStateOf(false) }

    val ageText = try {
        if (player.dob.isNotBlank() && player.dob.length >= 10) {
            val birthDate = LocalDate.parse(player.dob.take(10))
            val years = Period.between(birthDate, LocalDate.now()).years
            if (years in 5..80) " ($years yrs)" else ""
        } else ""
    } catch (_: Exception) {
        ""
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Player?") },
            text = { Text("Delete ${player.name} permanently?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(player.id)
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

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    PlayerAvatarImage(
                        photo = player.photo,
                        contentDescription = player.name,
                        fallbackEmoji = "👤",
                        fallbackSize = 48.sp
                    )
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = player.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Rajdevi Mayor Cup 2083 · Ward ${player.ward} Team",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Ward ${player.ward}", fontWeight = FontWeight.Bold) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = CricketPrimary.copy(alpha = 0.15f),
                            labelColor = CricketPrimary
                        )
                    )

                    if (player.jersey.isNotBlank()) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("#${player.jersey}", fontWeight = FontWeight.Bold) }
                        )
                    }

                    if (player.cap.isNotBlank()) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(player.cap, fontWeight = FontWeight.Bold) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = CricketGold.copy(alpha = 0.2f),
                                labelColor = CricketGold
                            )
                        )
                    }

                    if (currentUser != null && player.status != "approved") {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(player.status.uppercase(), fontWeight = FontWeight.Bold) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (player.status == "pending") CricketGold.copy(alpha = 0.2f) else CricketRed.copy(alpha = 0.2f),
                                labelColor = if (player.status == "pending") CricketGold else CricketRed
                            )
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(Modifier.height(12.dp))

                val detailRows = listOfNotNull(
                    "Ward Team" to "Ward ${player.ward}",
                    if (player.jersey.isNotBlank()) "Jersey No" to player.jersey else null,
                    if (player.father.isNotBlank()) "Father Name" to player.father else null,
                    if (player.dob.isNotBlank()) "Date of Birth" to "${player.dob}$ageText" else null,
                    if (player.mobile.isNotBlank()) "Mobile" to player.mobile else null,
                    if (player.blood.isNotBlank()) "Blood Group" to player.blood else null,
                    if (player.address.isNotBlank()) "Address" to player.address else null,
                    if (player.role.isNotBlank()) "Role" to player.role else null,
                    if (player.bat.isNotBlank()) "Batting" to player.bat else null,
                    if (player.bowl.isNotBlank()) "Bowling" to player.bowl else null,
                    if (player.cap.isNotBlank()) "Captaincy" to player.cap else null,
                    if (currentUser != null && player.status.isNotBlank()) "Status" to player.status else null
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    detailRows.forEach { (label, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Row(
                                modifier = Modifier.weight(1.3f),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = value,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (label == "Mobile") {
                                    Spacer(Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$value"))
                                            context.startActivity(dialIntent)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Phone,
                                            contentDescription = "Call $value",
                                            tint = CricketGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isAdmin) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (player.status != "approved") {
                                Button(
                                    onClick = { onApprove(player.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CricketGreen),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Approve")
                                }
                            }
                            if (player.status != "rejected") {
                                Button(
                                    onClick = { onReject(player.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CricketRed),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Reject")
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Close")
                        }

                        OutlinedButton(
                            onClick = {
                                val text = buildString {
                                    appendLine("🏏 Rajdevi Mayor Cup 2083")
                                    appendLine("Player: ${player.name} (${player.role})")
                                    appendLine("Team: Ward ${player.ward} | Jersey: #${player.jersey}")
                                    if (player.cap.isNotBlank()) appendLine("Role: ${player.cap}")
                                    if (player.mobile.isNotBlank()) appendLine("Mobile: ${player.mobile}")
                                    appendLine("Batting: ${player.bat} | Bowling: ${player.bowl}")
                                }
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Player: ${player.name}")
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Player Profile"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Share")
                        }

                        if (canEdit) {
                            Button(
                                onClick = { onEdit(player) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("edit_player_button")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Edit")
                            }
                        }
                    }

                    if (isAdmin) {
                        TextButton(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = CricketRed),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}
