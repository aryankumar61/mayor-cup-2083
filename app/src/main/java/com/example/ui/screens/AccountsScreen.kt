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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.TournamentAccount
import com.example.ui.theme.CricketAccent
import com.example.ui.theme.CricketPrimary
import com.example.ui.theme.CricketRed

@Composable
fun AccountsScreen(
    accounts: List<TournamentAccount>,
    onAddAccountClick: () -> Unit,
    onEditAccountClick: (TournamentAccount) -> Unit,
    onDeleteAccountClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🔑 Captain & Admin Accounts",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    text = "Role-based access control for ward captains",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onAddAccountClick,
                colors = ButtonDefaults.buttonColors(containerColor = CricketAccent),
                modifier = Modifier.testTag("add_account_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("New Account")
            }
        }

        Spacer(Modifier.height(10.dp))

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "ℹ Captains can add and edit only their own ward's players. Admin can approve or reject entries, manage matches, and configure standings.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(10.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
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
            title = { Text("Delete Account?") },
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = CricketPrimary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(40.dp)
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = account.username,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        Surface(
                            color = if (account.role == "admin") CricketPrimary.copy(alpha = 0.15f) else CricketAccent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (account.role == "admin") "Admin" else "Captain W${account.ward}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (account.role == "admin") CricketPrimary else CricketAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = account.displayName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Account",
                        tint = CricketPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (!isPrimaryAdmin) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Account",
                            tint = CricketRed,
                            modifier = Modifier.size(18.dp)
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
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isEditing) "Edit Account: $username" else "Create New Account",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                if (!isEditing) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (isEditing) "Password (blank = keep)" else "Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
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

                Spacer(Modifier.height(8.dp))

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
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
