package com.avonix.profitness

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

@HiltAndroidApp
class ProfitnessApplication : Application() {
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
