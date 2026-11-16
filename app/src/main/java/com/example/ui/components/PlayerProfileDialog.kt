package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Player
import com.example.data.model.TournamentAccount
import com.example.ui.theme.CricketGold
import com.example.ui.theme.CricketGreen
import com.example.ui.theme.CricketPrimary
import com.example.ui.theme.CricketRed
import com.example.ui.theme.OutfitFontFamily
import java.time.LocalDate
import java.time.Period

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerProfileDialog(
    player: Player?,
    currentUser: TournamentAccount?,
    onDismiss: () -> Unit,
    onEdit: (Player) -> Unit,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onDelete: (String) -> Unit,
    onViewCitizenshipDoc: (Player) -> Unit = {}
) {
    if (player == null) return
    val context = LocalContext.current
    val isAdmin = currentUser?.role?.trim()?.equals("admin", ignoreCase = true) == true
    val isOwnCaptain = currentUser?.role?.trim()?.equals("captain", ignoreCase = true) == true && currentUser.ward == player.ward
    val canEdit = isAdmin || isOwnCaptain

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
            title = { Text("Delete Player?", fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold) },
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 520.dp)
                .heightIn(max = 760.dp)
                .padding(vertical = 8.dp)
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
                        .size(128.dp)
                        .clip(RoundedCornerShape(16.dp))
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

                Spacer(Modifier.height(12.dp))

                Text(
                    text = player.name,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Rajdevi Mayor Cup 2083 · Ward ${player.ward} Team",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                "Ward ${player.ward}",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = CricketPrimary.copy(alpha = 0.14f),
                            labelColor = CricketPrimary
                        )
                    )

                    if (player.jersey.isNotBlank()) {
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    "#${player.jersey}",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }
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
                    if (canEdit && player.citizen.isNotBlank()) "Citizenship No" to player.citizen else null,
                    if (canEdit && player.father.isNotBlank()) "Father Name" to player.father else null,
                    if (canEdit && player.dob.isNotBlank()) "Date of Birth" to "${player.dob}$ageText" else null,
                    if (canEdit && player.mobile.isNotBlank()) "Mobile" to player.mobile else null,
                    if (canEdit && player.blood.isNotBlank()) "Blood Group" to player.blood else null,
                    if (canEdit && player.address.isNotBlank()) "Address" to player.address else null,
                    if (player.role.isNotBlank()) "Role" to player.role else null,
                    if (player.bat.isNotBlank()) "Batting" to player.bat else null,
                    if (player.bowl.isNotBlank()) "Bowling" to player.bowl else null,
                    if (player.cap.isNotBlank()) "Captaincy" to player.cap else null,
                    if (currentUser != null && player.status.isNotBlank()) "Status" to player.status.replaceFirstChar { it.uppercase() } else null
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
                                style = MaterialTheme.typography.bodyMedium,
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
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.End
                                )
                                if (label == "Mobile") {
                                    Spacer(Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$value"))
                                            context.startActivity(dialIntent)
                                        },
                                        modifier = Modifier.size(28.dp)
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

                if (canEdit && player.citizenimg.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    FilledTonalButton(
                        onClick = { onViewCitizenshipDoc(player) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .testTag("view_citizenship_button")
                    ) {
                        Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("View Citizenship Photo")
                    }
                }

                Spacer(Modifier.height(16.dp))

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
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 44.dp)
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
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 44.dp)
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
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                        ) {
                            Text("Close")
                        }

                        OutlinedButton(
                            onClick = {
                                val text = buildString {
                                    appendLine("Rajdevi Mayor Cup 2083")
                                    appendLine("Player: ${player.name} (${player.role})")
                                    appendLine("Team: Ward ${player.ward} | Jersey: #${player.jersey}")
                                    if (player.cap.isNotBlank()) appendLine("Role: ${player.cap}")
                                    appendLine("Batting: ${player.bat} | Bowling: ${player.bowl}")
                                }
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Player: ${player.name}")
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Player Profile"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Share")
                        }

                        if (canEdit) {
                            Button(
                                onClick = { onEdit(player) },
                                colors = ButtonDefaults.buttonColors(containerColor = CricketPrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
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
                            Text("Delete Player")
                        }
                    }
                }
            }
        }
    }
}
