package com.cosmibit.profitness

import android.app.Application
import android.os.StrictMode
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

@HiltAndroidApp
class ProfitnessApplication : Application(), ImageLoaderFactory {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
        }
        prewarmSupabaseClient()
    }

    /**
     * Tek Coil ImageLoader: avatar/hero/egzersiz görselleri bellekte (%25) ve diskte (~100MB) tutulur;
     * sekmeye tekrar girişte ve uygulama yeniden açılışında ağdan beklemeden gelir.
     */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.25).build() }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizeBytes(100L * 1024 * 1024)
                .build()
        }
        .build()

    private fun prewarmSupabaseClient() {
        appScope.launch {
            runCatching {
                EntryPointAccessors
                    .fromApplication(this@ProfitnessApplication, SupabaseWarmupEntryPoint::class.java)
                    .supabaseClient()
            }
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SupabaseWarmupEntryPoint {
    fun supabaseClient(): SupabaseClient
}
