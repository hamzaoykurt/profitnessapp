package com.avonix.profitness.presentation.profile

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream

private const val MAX_PROFILE_PHOTO_BYTES = 5L * 1024L * 1024L

private val ALLOWED_PROFILE_PHOTO_MIME_TYPES = setOf(
    "image/jpeg",
    "image/png",
    "image/webp"
)

data class ProfilePhotoReadResult(
    val bytes: ByteArray? = null,
    val errorMessage: String? = null
)

fun readSafeProfilePhotoBytes(context: Context, uri: Uri): ProfilePhotoReadResult {
    val resolver = context.contentResolver
    val mimeType = resolver.getType(uri)?.lowercase()
    if (mimeType !in ALLOWED_PROFILE_PHOTO_MIME_TYPES) {
        return ProfilePhotoReadResult(errorMessage = "Profil fotoğrafı JPEG, PNG veya WebP olmalı.")
    }

    val declaredSize = queryOpenableSize(context, uri)
    if (declaredSize != null && declaredSize > MAX_PROFILE_PHOTO_BYTES) {
        return ProfilePhotoReadResult(errorMessage = "Profil fotoğrafı en fazla 5 MB olabilir.")
    }

    val bytes = try {
        resolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0L

            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                total += read
                if (total > MAX_PROFILE_PHOTO_BYTES) {
                    return ProfilePhotoReadResult(errorMessage = "Profil fotoğrafı en fazla 5 MB olabilir.")
                }
                output.write(buffer, 0, read)
            }

            output.toByteArray()
        }
    } catch (_: SecurityException) {
        return ProfilePhotoReadResult(errorMessage = "Profil fotoğrafı okunamadı.")
    } catch (_: IllegalArgumentException) {
        return ProfilePhotoReadResult(errorMessage = "Profil fotoğrafı okunamadı.")
    }

    return if (bytes == null || bytes.isEmpty()) {
        ProfilePhotoReadResult(errorMessage = "Profil fotoğrafı okunamadı.")
    } else {
        ProfilePhotoReadResult(bytes = bytes)
    }
}

private fun queryOpenableSize(context: Context, uri: Uri): Long? =
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex >= 0 && cursor.moveToFirst() && !cursor.isNull(sizeIndex)) {
                cursor.getLong(sizeIndex).takeIf { it >= 0L }
            } else {
                null
            }
        }
    }.getOrNull()
