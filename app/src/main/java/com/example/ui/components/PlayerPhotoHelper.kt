package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Decodes a `data:image/...;base64,...` URI or renders standard URLs via Coil.
 */
@Composable
fun PlayerAvatarImage(
    photo: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    fallbackEmoji: String = "👤",
    fallbackSize: TextUnit = 40.sp
) {
    if (photo.isBlank()) {
        Text(
            text = fallbackEmoji,
            fontSize = fallbackSize,
            color = Color.LightGray
        )
        return
    }

    if (photo.startsWith("data:image", ignoreCase = true)) {
        val imageBitmap: ImageBitmap? = remember(photo) {
            try {
                val base64Part = photo.substringAfter("base64,", "")
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
            Text(
                text = fallbackEmoji,
                fontSize = fallbackSize,
                color = Color.LightGray
            )
        }
    } else {
        AsyncImage(
            model = photo,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxSize()
        )
    }
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
