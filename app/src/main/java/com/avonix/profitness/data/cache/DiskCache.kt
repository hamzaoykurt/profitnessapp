package com.avonix.profitness.data.cache

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Basit dosya-tabanlı JSON cache.
 * Repository'ler Supabase'den çekilen verileri buraya yazar,
 * uygulama yeniden açıldığında önce buradan okur — network'e gitmeden anında gösterir.
 * Veri değiştiğinde (create/update/delete) ilgili cache silinir.
 *
 * CACHE_VERSION: Bu sabiti artır → bir sonraki açılışta tüm cache otomatik temizlenir.
 * Race condition fix'i (v2) ile birlikte kullanıcıların bozuk disk cache'i temizlenir.
 */
@Singleton
class DiskCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @PublishedApi internal val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Volatile
    @PublishedApi
    internal var dir: File? = null

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var initializerStarted = false

    companion object {
        private const val CACHE_VERSION = 2
        private const val KEY_VERSION   = "cache_version"
    }

    init {
        // Cache metadata and filesystem setup are initialized lazily on Dispatchers.IO.
    }

    /** Versiyon uyuşmuyorsa tüm cache dosyalarını sil (tek seferlik migrasyon). */
    private fun migrateIfNeeded(dir: File) {
        val prefs = context.getSharedPreferences("disk_cache_meta", Context.MODE_PRIVATE)
        if (prefs.getInt(KEY_VERSION, 0) != CACHE_VERSION) {
            dir.listFiles()?.forEach { it.delete() }
            prefs.edit().putInt(KEY_VERSION, CACHE_VERSION).apply()
        }
    }

    @PublishedApi
    internal fun readyDir(): File? {
        startInitializer()
        return dir
    }

    private fun startInitializer() {
        if (initializerStarted) return
        synchronized(this) {
            if (initializerStarted) return
            initializerStarted = true
            ioScope.launch {
                val cacheDir = File(context.filesDir, "data_cache").also { it.mkdirs() }
                migrateIfNeeded(cacheDir)
                dir = cacheDir
            }
        }
    }

    inline fun <reified T> get(key: String): T? {
        val file = File(readyDir() ?: return null, "$key.json")
        if (!file.exists()) return null
        return try {
            json.decodeFromString<T>(file.readText())
        } catch (_: Exception) {
            file.delete(); null
        }
    }

    inline fun <reified T> put(key: String, value: T) {
        try {
            File(readyDir() ?: return, "$key.json").writeText(json.encodeToString(value))
        } catch (_: Exception) { /* sessizce yoksay — cache opsiyonel */ }
    }

    fun remove(key: String) {
        File(readyDir() ?: return, "$key.json").delete()
    }

    fun removeByPrefix(prefix: String) {
        readyDir()?.listFiles()?.filter { it.name.startsWith(prefix) }?.forEach { it.delete() }
    }
}
