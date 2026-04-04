package com.avonix.profitness.data.cache

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
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
 */
@Singleton
class DiskCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @PublishedApi internal val json = Json { ignoreUnknownKeys = true; isLenient = true }
    @PublishedApi internal val dir by lazy { File(context.filesDir, "data_cache").also { it.mkdirs() } }

    inline fun <reified T> get(key: String): T? {
        val file = File(dir, "$key.json")
        if (!file.exists()) return null
        return try {
            json.decodeFromString<T>(file.readText())
        } catch (_: Exception) {
            file.delete(); null
        }
    }

    inline fun <reified T> put(key: String, value: T) {
        try {
            File(dir, "$key.json").writeText(json.encodeToString(value))
        } catch (_: Exception) { /* sessizce yoksay — cache opsiyonel */ }
    }

    fun remove(key: String) {
        File(dir, "$key.json").delete()
    }

    fun removeByPrefix(prefix: String) {
        dir.listFiles()?.filter { it.name.startsWith(prefix) }?.forEach { it.delete() }
    }
}
