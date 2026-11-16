package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val DRIVE_ID_REGEX = Regex("""(?:/d/|id=)([a-zA-Z0-9_-]{15,})""")

private fun buildImageCandidates(rawUrl: String): List<String> {
    val clean = rawUrl.trim()
    if (clean.isBlank() || clean.equals("null", ignoreCase = true)) return emptyList()
    val driveId = if (clean.contains("drive.google.com", ignoreCase = true) ||
        clean.contains("docs.google.com", ignoreCase = true)
    ) {
        DRIVE_ID_REGEX.find(clean)?.groupValues?.getOrNull(1)
    } else null

    return if (driveId != null) {
        listOf(
            "https://drive.google.com/thumbnail?id=$driveId&sz=w600",
            "https://lh3.googleusercontent.com/d/$driveId=w600",
            "https://drive.google.com/uc?export=view&id=$driveId"
        ).distinct()
    } else {
        listOf(clean)
    }
}

@Composable
private fun AvatarPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

/**
 * Decodes a `data:image/...;base64,...` URI or loads standard / Google Drive URLs via Coil
 * with automatic Drive endpoint fallback, loading indicator, and avatar placeholder.
 */
@Composable
fun PlayerAvatarImage(
    photo: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") fallbackEmoji: String = "👤",
    @Suppress("UNUSED_PARAMETER") fallbackSize: TextUnit = 40.sp
) {
    val trimmed = photo.trim()
    if (trimmed.isBlank() || trimmed.equals("null", ignoreCase = true)) {
        AvatarPlaceholder(modifier)
        return
    }

    if (trimmed.startsWith("data:image", ignoreCase = true)) {
        val imageBitmap: ImageBitmap? = remember(trimmed) {
            try {
                val base64Part = trimmed.substringAfter("base64,", "")
                if (base64Part.isBlank()) null
                else {
                    val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                }
            } catch (_: Exception) {
                null
            }
        }
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = modifier.fillMaxSize()
            )
        } else {
            AvatarPlaceholder(modifier)
        }
        return
    }

    val candidates = remember(trimmed) { buildImageCandidates(trimmed) }
    var candidateIndex by remember(trimmed) { mutableIntStateOf(0) }
    val activeUrl = candidates.getOrNull(candidateIndex)

    if (activeUrl == null) {
        AvatarPlaceholder(modifier)
        return
    }

    val context = LocalContext.current
    val request = remember(activeUrl) {
        ImageRequest.Builder(context)
            .data(activeUrl)
            .crossfade(true)
            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
            .build()
    }

    SubcomposeAsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxSize(),
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        error = {
            if (candidateIndex + 1 < candidates.size) {
                LaunchedEffect(activeUrl) {
                    candidateIndex++
                }
            }
            AvatarPlaceholder()
        }
    )
}

/**
 * Resizes an image Uri to maxDim and encodes it as JPEG Data URL.
 */
fun encodeImageUriToDataUrl(context: Context, uri: Uri, maxDim: Int = 420, quality: Int = 72): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val original = inputStream.use { BitmapFactory.decodeStream(it) } ?: return null
        val scale = min(1f, maxDim.toFloat() / max(original.width, original.height).toFloat())
        val targetW = (original.width * scale).roundToInt().coerceAtLeast(1)
        val targetH = (original.height * scale).roundToInt().coerceAtLeast(1)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(original, targetW, targetH, true)
        } else {
            original
        }

        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        if (scaled !== original) scaled.recycle()
        original.recycle()

        val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        "data:image/jpeg;base64,$base64"
    } catch (_: Exception) {
        null
    }
}

/**
 * Encodes any image or PDF Uri (e.g. Ward Sifaris or Citizenship photo) into a `data:<mime>;base64,...` string.
 * Images are resized to max 1000px for crisp readability while keeping upload size fast.
 */
fun encodeDocumentUriToDataUrl(context: Context, uri: Uri): String? {
    return try {
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        if (mime.startsWith("image/", ignoreCase = true)) {
            encodeImageUriToDataUrl(context, uri, maxDim = 1000, quality = 78)
        } else {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            if (bytes.size > 4 * 1024 * 1024) return null // Keep under Apps Script payload limits
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:$mime;base64,$base64"
        }
    } catch (_: Exception) {
        null
    }
}
