package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.TournamentAccount
import com.example.ui.theme.CricketAccent
import com.example.ui.theme.CricketPrimary
import com.example.ui.theme.CricketRed
import com.example.ui.theme.OutfitFontFamily

@Composable
fun AccountsScreen(
    accounts: List<TournamentAccount>,
    onAddAccountClick: () -> Unit,
    onEditAccountClick: (TournamentAccount) -> Unit,
    onDeleteAccountClick: (String) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Captain & Admin Accounts",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Role-based access control for ward captains",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onAddAccountClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CricketAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("add_account_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New Account", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(10.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Captains can upload their ward's Sifaris once and submit players with citizenship verification for their own ward. Admin can approve or reject entries, manage matches, and configure standings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(accounts, key = { it.username }) { account ->
                    AccountCard(
                        account = account,
                        onEdit = { onEditAccountClick(account) },
                        onDelete = { onDeleteAccountClick(account.username) }
                    )
                }
            }
        }
    }
}

@Composable
fun AccountCard(
    account: TournamentAccount,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isPrimaryAdmin = account.username.equals("admin", ignoreCase = true)
    var showDeleteConfirm by remember(account.username) { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account?", fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold) },
            text = { Text("Delete account ${account.username}?") },
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

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(14.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = CricketPrimary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = CricketPrimary
                        )
                    }
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = account.username,
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        Surface(
                            color = if (account.role == "admin") CricketPrimary.copy(alpha = 0.15f) else CricketAccent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (account.role == "admin") "Admin" else "Captain W${account.ward}",
                                fontFamily = OutfitFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (account.role == "admin") CricketPrimary else CricketAccent,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = account.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onEdit, modifier = Modifier.size(44.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Account",
                        tint = CricketPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                if (!isPrimaryAdmin) {
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(44.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Account",
                            tint = CricketRed,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountEditDialog(
    isOpen: Boolean,
    account: TournamentAccount?,
    onDismiss: () -> Unit,
    onSave: (TournamentAccount, Boolean) -> Unit
) {
    if (!isOpen || account == null) return

    val isEditing = account.username.isNotBlank()
    var username by remember(account) { mutableStateOf(account.username) }
    var password by remember(account) { mutableStateOf("") }
    var displayName by remember(account) { mutableStateOf(account.displayName) }
    var role by remember(account) { mutableStateOf(account.role) }
    var ward by remember(account) { mutableStateOf(if (account.ward > 0) account.ward else 1) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isEditing) "Edit Account: $username" else "Create New Account",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                )

                if (!isEditing) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (isEditing) "Password (blank = keep)" else "Password") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Role Dropdown
                var roleExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = roleExpanded,
                    onExpandedChange = { roleExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = role.replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Role") },
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = roleExpanded,
                        onDismissRequest = { roleExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Captain") },
                            onClick = {
                                role = "captain"
                                roleExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Admin") },
                            onClick = {
                                role = "admin"
                                roleExpanded = false
                            }
                        )
                    }
                }

                if (role == "captain") {
                    var wardExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = wardExpanded,
                        onExpandedChange = { wardExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = "Ward $ward",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assigned Ward") },
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = wardExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = wardExpanded,
                            onDismissRequest = { wardExpanded = false }
                        ) {
                            (1..9).forEach { w ->
                                DropdownMenuItem(
                                    text = { Text("Ward $w") },
                                    onClick = {
                                        ward = w
                                        wardExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val saved = account.copy(
                                username = username.trim(),
                                password = password.trim(),
                                displayName = displayName.trim(),
                                role = role,
                                ward = if (role == "captain") ward else 0
                            )
                            onSave(saved, !isEditing)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CricketPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
