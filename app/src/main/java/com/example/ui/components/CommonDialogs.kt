package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.TournamentAccount
import com.example.data.repository.PrivateDocument
import com.example.ui.theme.CricketGold
import com.example.ui.theme.CricketGreen
import com.example.ui.theme.CricketPrimary
import com.example.ui.theme.OutfitFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun LoginDialog(
    isOpen: Boolean,
    isLoggingIn: Boolean = false,
    onDismiss: () -> Unit,
    onLogin: (username: String, password: String) -> Unit
) {
    if (!isOpen) return

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isLoggingIn) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = CricketPrimary)
                Text(
                    "Account Login",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Sign in as Committee Admin or Ward Captain (Ward 1–9).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    placeholder = { Text("e.g. admin or ward1") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_username_input")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (!isLoggingIn) onLogin(username, password) }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_password_input")
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            "Default Committee & Captain Accounts",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Admin: admin / admin@2083\nCaptains: ward1..ward9 / ward1@2083..ward9@2083",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onLogin(username, password) },
                enabled = !isLoggingIn,
                colors = ButtonDefaults.buttonColors(containerColor = CricketPrimary),
                modifier = Modifier.testTag("login_submit_button")
            ) {
                if (isLoggingIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(if (isLoggingIn) "Signing in…" else "Sign In")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isLoggingIn
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ChangePasswordDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (newPassword: String) -> Unit
) {
    if (!isOpen) return

    var newPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Change Password",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Enter a new password (minimum 6 characters):",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(newPassword) },
                colors = ButtonDefaults.buttonColors(containerColor = CricketPrimary)
            ) {
                Text("Update Password")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SifarisDialog(
    isOpen: Boolean,
    currentUser: TournamentAccount?,
    uploadedWards: Set<Int>,
    isUploading: Boolean,
    onDismiss: () -> Unit,
    onUploadSifaris: (ward: Int, paperDataUrl: String) -> Unit,
    onViewSifaris: (ward: Int) -> Unit
) {
    if (!isOpen || currentUser == null) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isAdmin = currentUser.role == "admin"
    val isCaptain = currentUser.role == "captain"

    var selectedWard by remember(currentUser) {
        mutableStateOf(if (isCaptain && currentUser.ward in 1..9) currentUser.ward else 1)
    }
    var pickedDataUrl by remember(selectedWard) { mutableStateOf<String?>(null) }
    var pickedLabel by remember(selectedWard) { mutableStateOf("") }
    var isEncoding by remember { mutableStateOf(false) }

    val alreadyUploadedForSelected = selectedWard in uploadedWards
    val canUpload = isAdmin || (isCaptain && !alreadyUploadedForSelected)

    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isEncoding = true
                val encoded = withContext(Dispatchers.IO) {
                    encodeDocumentUriToDataUrl(context, uri)
                }
                if (encoded != null) {
                    pickedDataUrl = encoded
                    pickedLabel = uri.lastPathSegment?.substringAfterLast('/') ?: "Selected file"
                }
                isEncoding = false
            }
        }
    }

    Dialog(onDismissRequest = { if (!isUploading) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = CricketPrimary)
                    Text(
                        text = "Ward Sifaris Letter",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }

                Text(
                    text = if (isCaptain) {
                        "One-time official ward recommendation letter required before adding Ward ${currentUser.ward} players."
                    } else {
                        "Inspect or update the official Sifaris letter (image or PDF) for each ward."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isAdmin) {
                    var wardExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = wardExpanded,
                        onExpandedChange = { wardExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val statusTag = if (selectedWard in uploadedWards) "Uploaded" else "Not uploaded"
                        OutlinedTextField(
                            value = "Ward $selectedWard ($statusTag)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Ward") },
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
                                val hasPaper = w in uploadedWards
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Ward $w")
                                            Text(
                                                text = if (hasPaper) "Uploaded" else "Missing",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (hasPaper) CricketGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedWard = w
                                        wardExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Current status card
                Surface(
                    color = if (alreadyUploadedForSelected) CricketGreen.copy(alpha = 0.1f) else CricketGold.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (alreadyUploadedForSelected) CricketGreen.copy(alpha = 0.35f) else CricketGold.copy(alpha = 0.35f),
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (alreadyUploadedForSelected) {
                                    "Ward $selectedWard Sifaris is on file"
                                } else {
                                    "Ward $selectedWard Sifaris not uploaded"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = if (alreadyUploadedForSelected) CricketGreen else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isCaptain && alreadyUploadedForSelected) {
                                    "Already verified. Ask committee admin to replace it."
                                } else if (alreadyUploadedForSelected) {
                                    "Stored privately in committee Drive"
                                } else {
                                    "Upload clear photo or PDF (max 4 MB)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (alreadyUploadedForSelected) {
                            FilledTonalButton(
                                onClick = { onViewSifaris(selectedWard) },
                                modifier = Modifier.heightIn(min = 40.dp)
                            ) {
                                Text("View")
                            }
                        }
                    }
                }

                if (canUpload) {
                    OutlinedButton(
                        onClick = {
                            docPickerLauncher.launch(arrayOf("image/*", "application/pdf"))
                        },
                        enabled = !isEncoding && !isUploading,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("pick_sifaris_button")
                    ) {
                        if (isEncoding) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Preparing file…")
                        } else {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (pickedDataUrl != null) "Selected: $pickedLabel"
                                else if (alreadyUploadedForSelected) "Choose Replacement Image / PDF"
                                else "Choose Sifaris Image / PDF"
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isUploading,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text("Close")
                    }

                    if (canUpload) {
                        Button(
                            onClick = {
                                val data = pickedDataUrl
                                if (data != null) {
                                    onUploadSifaris(selectedWard, data)
                                    pickedDataUrl = null
                                    pickedLabel = ""
                                }
                            },
                            enabled = pickedDataUrl != null && !isUploading && !isEncoding,
                            colors = ButtonDefaults.buttonColors(containerColor = CricketPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("upload_sifaris_button")
                        ) {
                            if (isUploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(if (isUploading) "Uploading…" else "Upload")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrivateDocumentDialog(
    document: PrivateDocument?,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    if (isLoading) {
        Dialog(onDismissRequest = {}) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), color = CricketPrimary)
                    Text(
                        "Loading private document…",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        return
    }

    if (document == null) return

    val context = LocalContext.current
    val isPdf = document.mime.contains("pdf", ignoreCase = true)

    val renderedBitmaps: List<Bitmap> by produceState<List<Bitmap>>(initialValue = emptyList(), document) {
        value = withContext(Dispatchers.IO) {
            try {
                val bytes = Base64.decode(document.base64Data, Base64.DEFAULT)
                if (isPdf) {
                    val tempFile = File.createTempFile("rmc_doc_", ".pdf", context.cacheDir)
                    tempFile.writeBytes(bytes)
                    val fd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(fd)
                    val pages = mutableListOf<Bitmap>()
                    val maxPages = renderer.pageCount.coerceAtMost(8)
                    for (i in 0 until maxPages) {
                        val page = renderer.openPage(i)
                        val scale = (1080f / page.width.coerceAtLeast(1)).coerceIn(1f, 2.5f)
                        val bmp = Bitmap.createBitmap(
                            (page.width * scale).toInt(),
                            (page.height * scale).toInt(),
                            Bitmap.Config.ARGB_8888
                        )
                        bmp.eraseColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        pages.add(bmp)
                    }
                    renderer.close()
                    fd.close()
                    tempFile.delete()
                    pages
                } else {
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null) listOf(bmp) else emptyList()
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
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
                .fillMaxHeight(0.86f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = document.title,
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (isPdf) "PDF Document (${renderedBitmaps.size} page preview)" else "Verified Private Image",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close document")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                if (renderedBitmaps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                "Rendering document preview…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        renderedBitmaps.forEach { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = document.title,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }
        }
    }
}
