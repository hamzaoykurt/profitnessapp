package com.cosmibit.profitness.data.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Profil kapağı seçimi.
 *  - [preset]: hazır kapak kimliği (bkz. presentation CoverPresets) ya da null.
 *  - [customPath]: kullanıcının kendi fotoğrafı (yerel dosya, kırpılmış ≤1080px JPEG).
 *  - [remoteUrl]: sunucudaki kapak (migration uygulanınca; şimdilik null).
 */
data class ProfileCover(val preset: String? = null, val customPath: String? = null, val remoteUrl: String? = null) {
    val isDefault get() = preset == null && customPath == null && remoteUrl == null
}

/**
 * Kapak seçimi YEREL kalıcı (SharedPreferences + dosya; kullanıcı başına). Sunucu eşitlemesi önerisi:
 * supabase/proposals/20261118_profile_cover.sql — sütunlar yoksa [loadRemote] sessizce null döner.
 */
@Singleton
class ProfileCoverStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabase: SupabaseClient
) {
    private val prefs = context.getSharedPreferences("profile_cover", Context.MODE_PRIVATE)
    private val _cover = MutableStateFlow(ProfileCover())
    val cover: StateFlow<ProfileCover> = _cover.asStateFlow()
    private var loadedFor: String? = null

    /** Kullanıcının yerel seçimini yükler (ilk çağrıda); sonra sunucu alanlarını (varsa) bir kez dener. */
    suspend fun load(userId: String) {
        if (loadedFor == userId) return
        loadedFor = userId
        val local = withContext(Dispatchers.IO) {
            ProfileCover(
                preset = prefs.getString("preset_$userId", null),
                customPath = prefs.getString("custom_$userId", null)?.takeIf { File(it).exists() }
            )
        }
        _cover.value = local
        if (local.isDefault) loadRemote(userId)?.let { remote -> _cover.value = remote }
    }

    fun selectPreset(userId: String, presetId: String?) {
        prefs.edit().putString("preset_$userId", presetId).remove("custom_$userId").apply()
        File(context.filesDir, fileName(userId)).delete()
        _cover.value = ProfileCover(preset = presetId)
    }

    /** Seçilen fotoğrafı kapak oranında ortadan kırpar, ≤1080px'e küçültür, JPEG ~80 kaydeder. */
    suspend fun saveCustom(userId: String, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Görsel okunamadı" }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = CoverImageMath.sampleSize(bounds.outWidth, bounds.outHeight, COVER_W, COVER_H)
            }
            val decoded = resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, opts) }
                ?: error("Görsel okunamadı")
            val crop = CoverImageMath.centerCrop(decoded.width, decoded.height, COVER_W.toFloat() / COVER_H)
            val cropped = Bitmap.createBitmap(decoded, crop.left, crop.top, crop.width, crop.height)
            val (tw, th) = CoverImageMath.fitWithin(crop.width, crop.height, COVER_W)
            val scaled = if (tw != crop.width) Bitmap.createScaledBitmap(cropped, tw, th, true) else cropped
            val file = File(context.filesDir, fileName(userId))
            file.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 80, it) }
            if (scaled !== cropped) scaled.recycle()
            if (cropped !== decoded) cropped.recycle()
            decoded.recycle()
            prefs.edit().putString("custom_$userId", file.absolutePath).remove("preset_$userId").apply()
            // Aynı yol: görüntü önbelleği yenilensin diye sürüm parametresi Coil tarafında dosya değişim zamanından gelir
            _cover.value = ProfileCover(customPath = file.absolutePath)
        }
    }

    private fun fileName(userId: String) = "profile_cover_$userId.jpg"

    /** Sunucu alanları (profiles.cover_url / cover_preset) — migration uygulanmadıysa hata -> null. */
    private suspend fun loadRemote(userId: String): ProfileCover? = withContext(Dispatchers.IO) {
        runCatching {
            supabase.from("profiles")
                .select(Columns.raw("cover_url,cover_preset")) { filter { eq("user_id", userId) } }
                .decodeSingleOrNull<RemoteCoverDto>()
        }.getOrNull()?.let { dto ->
            if (dto.cover_url.isNullOrBlank() && dto.cover_preset.isNullOrBlank()) null
            else ProfileCover(preset = dto.cover_preset, remoteUrl = dto.cover_url)
        }
    }

    @Serializable
    private data class RemoteCoverDto(val cover_url: String? = null, val cover_preset: String? = null)

    companion object {
        /** Kapak çıktı boyutu (5:4; profil kapağı oranına yakın, ortadan kırpılır). */
        const val COVER_W = 1080
        const val COVER_H = 864
    }
}

/** Kapak görsel hesapları (saf; birim testli). */
object CoverImageMath {
    data class Rect(val left: Int, val top: Int, val width: Int, val height: Int)

    /** Kaynağı hedef orana ortadan kırpan dikdörtgen. */
    fun centerCrop(srcW: Int, srcH: Int, targetRatio: Float): Rect {
        val srcRatio = srcW.toFloat() / srcH
        return if (srcRatio > targetRatio) {
            val w = (srcH * targetRatio).toInt().coerceIn(1, srcW)
            Rect((srcW - w) / 2, 0, w, srcH)
        } else {
            val h = (srcW / targetRatio).toInt().coerceIn(1, srcH)
            Rect(0, (srcH - h) / 2, srcW, h)
        }
    }

    /** Hedefin en az 1x'i kalacak en büyük 2'nin kuvveti örnekleme (bellek dostu çözme). */
    fun sampleSize(srcW: Int, srcH: Int, reqW: Int, reqH: Int): Int {
        var s = 1
        while (srcW / (s * 2) >= reqW && srcH / (s * 2) >= reqH) s *= 2
        return s
    }

    /** Genişlik [maxW]'yi aşmayacak şekilde oranı koruyarak küçültme boyutu. */
    fun fitWithin(w: Int, h: Int, maxW: Int): Pair<Int, Int> =
        if (w <= maxW) w to h else maxW to (h.toLong() * maxW / w).toInt().coerceAtLeast(1)
}
