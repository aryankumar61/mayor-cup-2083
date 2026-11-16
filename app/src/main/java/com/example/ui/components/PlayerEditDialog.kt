package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Player
import com.example.data.model.TournamentAccount
import com.example.ui.theme.CricketGold
import com.example.ui.theme.CricketGreen
import com.example.ui.theme.CricketPrimary
import com.example.ui.theme.OutfitFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerEditDialog(
    isOpen: Boolean,
    player: Player?,
    currentUser: TournamentAccount?,
    hasWardSifaris: Boolean = true,
    isSaving: Boolean = false,
    onDismiss: () -> Unit,
    onOpenSifaris: () -> Unit = {},
    onSave: (Player, String?) -> Unit
) {
    if (!isOpen || player == null) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isCaptain = currentUser?.role == "captain"

    var ward by remember(player) { mutableStateOf(if (isCaptain) (currentUser?.ward ?: player.ward) else player.ward) }
    var jersey by remember(player) { mutableStateOf(player.jersey) }
    var name by remember(player) { mutableStateOf(player.name) }
    var father by remember(player) { mutableStateOf(player.father) }
    var dob by remember(player) { mutableStateOf(player.dob) }
    var mobile by remember(player) { mutableStateOf(player.mobile) }
    var blood by remember(player) { mutableStateOf(player.blood) }
    var address by remember(player) { mutableStateOf(player.address) }
    var role by remember(player) { mutableStateOf(player.role.ifBlank { "Batsman" }) }
    var bat by remember(player) { mutableStateOf(player.bat.ifBlank { "Right Hand" }) }
    var bowl by remember(player) { mutableStateOf(player.bowl.ifBlank { "None" }) }
    var cap by remember(player) { mutableStateOf(player.cap) }
    var citizen by remember(player) { mutableStateOf(player.citizen) }
    var photoData by remember(player) { mutableStateOf(player.photo) }
    var citizenDataUrl by remember(player) { mutableStateOf<String?>(null) }
    var isEncodingPhoto by remember { mutableStateOf(false) }
    var isEncodingCitizen by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isEncodingPhoto = true
                val encoded = withContext(Dispatchers.IO) {
                    encodeImageUriToDataUrl(context, uri)
                }
                if (encoded != null) {
                    photoData = encoded
                }
                isEncodingPhoto = false
            }
        }
    }

    val citizenPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isEncodingCitizen = true
                val encoded = withContext(Dispatchers.IO) {
                    encodeImageUriToDataUrl(context, uri, maxDim = 1000, quality = 78)
                }
                if (encoded != null) {
                    citizenDataUrl = encoded
                }
                isEncodingCitizen = false
            }
        }
    }

    val roles = listOf("Batsman", "Bowler", "All-rounder", "Wicket Keeper")
    val batStyles = listOf("Right Hand", "Left Hand")
    val bowlStyles = listOf(
        "None", "Right Arm Fast", "Right Arm Medium", "Right Arm Spin",
        "Left Arm Fast", "Left Arm Medium", "Left Arm Spin"
    )
    val bloodGroups = listOf("", "A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")
    val captaincies = listOf("", "Captain", "Vice Captain")

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 600.dp)
                .fillMaxHeight(0.92f)
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    text = if (player.name.isNotBlank()) "Edit Player" else "Register Player",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (isCaptain) {
                    Spacer(Modifier.height(8.dp))
                    if (!hasWardSifaris) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Upload Ward $ward Sifaris letter before saving players.",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = onOpenSifaris) {
                                    Text("Upload", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Surface(
                            color = CricketGold.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Captain submissions are saved as Pending until approved by committee admin.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Photo Upload Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(82.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isEncodingPhoto) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else if (photoData.isNotBlank()) {
                                PlayerAvatarImage(
                                    photo = photoData,
                                    contentDescription = "Player photo"
                                )
                            } else {
                                Icon(
                                    Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Pick photo",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            ) {
                                Text(if (photoData.isBlank()) "Choose Player Photo" else "Change Photo")
                            }
                            Text(
                                "Uploaded to Drive on save",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Full Name (Mandatory)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Player Full Name *") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_name_input")
                    )

                    // Citizenship Verification Section
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = CricketPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isCaptain) "Citizenship Verification (Required)" else "Citizenship Verification",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            OutlinedTextField(
                                value = citizen,
                                onValueChange = { citizen = it },
                                label = { Text(if (isCaptain) "Citizenship No *" else "Citizenship No") },
                                placeholder = { Text("e.g. 12-01-75-01234") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("player_citizen_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            citizenPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        isEncodingCitizen -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        citizenDataUrl != null -> PlayerAvatarImage(
                                            photo = citizenDataUrl.orEmpty(),
                                            contentDescription = "Selected citizenship card"
                                        )
                                        player.citizenimg.isNotBlank() -> Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Attached on server",
                                            tint = CricketGreen
                                        )
                                        else -> Icon(
                                            Icons.Default.AddPhotoAlternate,
                                            contentDescription = "Pick citizenship photo",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    OutlinedButton(
                                        onClick = {
                                            citizenPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        enabled = !isEncodingCitizen,
                                        modifier = Modifier.testTag("pick_citizen_photo_button")
                                    ) {
                                        Text(
                                            when {
                                                citizenDataUrl != null -> "Selected (Tap to change)"
                                                player.citizenimg.isNotBlank() -> "Attached (Replace Photo)"
                                                else -> if (isCaptain) "Citizenship Photo *" else "Citizenship Photo"
                                            }
                                        )
                                    }
                                    Text(
                                        text = if (player.citizenimg.isNotBlank() && citizenDataUrl == null) {
                                            "Private citizenship card saved on server"
                                        } else {
                                            "Stored privately in Drive (Admin & Captain only)"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Ward and Jersey Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        var wardExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = wardExpanded && !isCaptain,
                            onExpandedChange = { if (!isCaptain) wardExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = "Ward $ward",
                                onValueChange = {},
                                readOnly = true,
                                enabled = !isCaptain,
                                label = { Text("Ward Team") },
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

                        OutlinedTextField(
                            value = jersey,
                            onValueChange = { jersey = it },
                            label = { Text("Jersey No") },
                            placeholder = { Text("e.g. 7") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("player_jersey_input")
                        )
                    }

                    // Father Name
                    OutlinedTextField(
                        value = father,
                        onValueChange = { father = it },
                        label = { Text("Father Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // DOB & Mobile
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = dob,
                            onValueChange = { dob = it },
                            label = { Text("Date of Birth") },
                            placeholder = { Text("YYYY-MM-DD") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = mobile,
                            onValueChange = { mobile = it },
                            label = { Text("Mobile No") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Blood Group & Captaincy
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        var bloodExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = bloodExpanded,
                            onExpandedChange = { bloodExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = blood.ifBlank { "-" },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Blood Group") },
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bloodExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = bloodExpanded,
                                onDismissRequest = { bloodExpanded = false }
                            ) {
                                bloodGroups.forEach { bg ->
                                    DropdownMenuItem(
                                        text = { Text(bg.ifBlank { "-" }) },
                                        onClick = {
                                            blood = bg
                                            bloodExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        var capExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = capExpanded,
                            onExpandedChange = { capExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = cap.ifBlank { "-" },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Captain / Vice") },
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = capExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = capExpanded,
                                onDismissRequest = { capExpanded = false }
                            ) {
                                captaincies.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text(c.ifBlank { "-" }) },
                                        onClick = {
                                            cap = c
                                            capExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Playing Role
                    var roleExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = roleExpanded,
                        onExpandedChange = { roleExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = role,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Playing Role") },
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
                            roles.forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r) },
                                    onClick = {
                                        role = r
                                        roleExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Batting & Bowling
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        var batExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = batExpanded,
                            onExpandedChange = { batExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = bat,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Batting Style") },
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = batExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = batExpanded,
                                onDismissRequest = { batExpanded = false }
                            ) {
                                batStyles.forEach { bs ->
                                    DropdownMenuItem(
                                        text = { Text(bs) },
                                        onClick = {
                                            bat = bs
                                            batExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        var bowlExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = bowlExpanded,
                            onExpandedChange = { bowlExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = bowl,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Bowling Style") },
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bowlExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = bowlExpanded,
                                onDismissRequest = { bowlExpanded = false }
                            ) {
                                bowlStyles.forEach { bws ->
                                    DropdownMenuItem(
                                        text = { Text(bws) },
                                        onClick = {
                                            bowl = bws
                                            bowlExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Address / Tole
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address / Tole") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isSaving,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val updated = player.copy(
                                ward = ward,
                                jersey = jersey.trim(),
                                name = name.trim(),
                                father = father.trim(),
                                dob = dob.trim(),
                                mobile = mobile.trim(),
                                blood = blood.trim(),
                                address = address.trim(),
                                role = role.trim(),
                                bat = bat.trim(),
                                bowl = bowl.trim(),
                                cap = cap.trim(),
                                photo = photoData,
                                citizen = citizen.trim()
                            )
                            onSave(updated, citizenDataUrl)
                        },
                        enabled = !isSaving && !isEncodingPhoto && !isEncodingCitizen,
                        colors = ButtonDefaults.buttonColors(containerColor = CricketPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("save_player_button")
                    ) {
                        Text(if (isSaving) "Saving…" else "Save Player")
                    }
                }
            }
        }
    }
}
