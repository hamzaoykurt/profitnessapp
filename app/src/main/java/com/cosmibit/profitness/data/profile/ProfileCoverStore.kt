package com.cosmibit.profitness.data.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Profil kapağı seçimi.
 *  - [preset]: hazır kapak kimliği (bkz. presentation CoverPresets) ya da null.
 *  - [customPath]: kullanıcının kendi fotoğrafı (yerel dosya, kırpılmış ≤1080px JPEG).
 *  - [remoteUrl]: sunucudaki kapak URL'si (`profiles.cover_url`).
 */
data class ProfileCover(val preset: String? = null, val customPath: String? = null, val remoteUrl: String? = null) {
    val isDefault get() = preset == null && customPath == null && remoteUrl == null
}

/**
 * Kapak seçimi YEREL kalıcı (SharedPreferences + dosya; kullanıcı başına) ve sunucu ile eşitlenir
 * (`profiles.cover_url`, `profiles.cover_preset`, `profile-photos/covers/<uid>/cover.jpg`).
 * Çevrimdışı durumda yerel yazım her zaman anında uygulanır.
 */
@Singleton
class ProfileCoverStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabase: SupabaseClient
) {
    private val prefs = context.getSharedPreferences("profile_cover", Context.MODE_PRIVATE)
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _cover = MutableStateFlow(ProfileCover())
    val cover: StateFlow<ProfileCover> = _cover.asStateFlow()
    private var loadedFor: String? = null

    /** Çıkışta veya hesap değişiminde bellek önbelleğini sıfırlar. */
    fun resetSession() {
        loadedFor = null
        _cover.value = ProfileCover()
    }

    /** Kullanıcının yerel seçimini yükler (ilk çağrıda); sonra sunucu alanlarını (varsa) dener. */
    suspend fun load(userId: String) {
        if (loadedFor == userId) return
        loadedFor = userId
        val local = withContext(Dispatchers.IO) {
            ProfileCover(
                preset = prefs.getString("preset_$userId", null),
                customPath = prefs.getString("custom_$userId", null)?.takeIf { File(it).exists() },
                remoteUrl = prefs.getString("remote_$userId", null)?.takeIf { it.isNotBlank() }
            )
        }
        _cover.value = local
        if (local.isDefault) {
            loadRemote(userId)?.let { remote ->
                when {
                    !remote.remoteUrl.isNullOrBlank() -> {
                        withContext(Dispatchers.IO) {
                            prefs.edit().putString("remote_$userId", remote.remoteUrl).apply()
                        }
                        _cover.value = ProfileCover(remoteUrl = remote.remoteUrl)
                    }
                    !remote.preset.isNullOrBlank() -> {
                        val normalizedPreset = remote.preset.takeIf { it != "photo" }
                        withContext(Dispatchers.IO) {
                            prefs.edit().putString("preset_$userId", normalizedPreset).apply()
                        }
                        _cover.value = ProfileCover(preset = normalizedPreset)
                    }
                }
            }
        }
    }

    fun selectPreset(userId: String, presetId: String?) {
        val normalized = presetId?.takeIf { it != "photo" }
        prefs.edit().putString("preset_$userId", normalized).remove("custom_$userId").remove("remote_$userId").apply()
        File(context.filesDir, fileName(userId)).delete()
        _cover.value = ProfileCover(preset = normalized)
        syncScope.launch {
            runCatching {
                supabase.from("profiles").update(
                    {
                        set("cover_preset", normalized ?: "photo")
                        set("cover_url", null as String?)
                    }
                ) {
                    filter { eq("user_id", userId) }
                }
            }
        }
    }

    /** Seçilen fotoğrafı kapak oranında ortadan kırpar, ≤1080px'e küçültür, JPEG ~80 kaydeder ve Storage'a eşitler. */
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

            // Sunucu eşitlemesi: hata yerel özel kapağı bozmaz (çevrimdışı güvenli)
            runCatching {
                val storagePath = "covers/$userId/cover.jpg"
                val bytes = file.readBytes()
                supabase.storage.from("profile-photos").upload(storagePath, bytes, upsert = true)
                val publicUrl = "${supabase.storage.from("profile-photos").publicUrl(storagePath)}?v=${System.currentTimeMillis()}"
                supabase.from("profiles").update(
                    {
                        set("cover_url", publicUrl)
                        set("cover_preset", null as String?)
                    }
                ) {
                    filter { eq("user_id", userId) }
                }
                prefs.edit().putString("remote_$userId", publicUrl).apply()
                _cover.value = ProfileCover(customPath = file.absolutePath, remoteUrl = publicUrl)
            }
            Unit
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
