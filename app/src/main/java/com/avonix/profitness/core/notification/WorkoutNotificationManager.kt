package com.avonix.profitness.core.notification

import android.content.Context
import android.content.Intent
import android.util.Log
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import com.avonix.profitness.service.WorkoutForegroundService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Antrenman bildirimlerini ve seslerini yöneten singleton yönetici.
 *
 * WorkoutViewModel bu sınıfı inject eder:
 *  - startWorkoutSession()  → ön plan servisi başlatır (bildirim çubuğunda görünür)
 *  - updateRestTimer()      → her saniye geri sayımı günceller
 *  - notifyTimerDone()      → ses çalar + "Hazırsın!" bildirimi gönderir
 *  - stopWorkoutSession()   → servisi durdurur
 */
@Singleton
class WorkoutNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var toneGenerator: ToneGenerator? = null
    private var sessionActive = false
    private var lastTimerUpdateAt = 0L
    private var lastTimerSecondsLeft = Int.MIN_VALUE
    private var lastActivityUpdateAt = 0L
    private var lastActivitySeconds = Int.MIN_VALUE
    private val soundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ── Oturum Kontrolü ──────────────────────────────────────────────────────

    fun startWorkoutSession(dayTitle: String) {
        sessionActive = true
        lastTimerUpdateAt = 0L
        lastTimerSecondsLeft = Int.MIN_VALUE
        lastActivityUpdateAt = 0L
        lastActivitySeconds = Int.MIN_VALUE
        val intent = Intent(context, WorkoutForegroundService::class.java).apply {
            action = WorkoutForegroundService.ACTION_START
            putExtra(WorkoutForegroundService.EXTRA_DAY_TITLE, dayTitle)
        }
        startService(intent)
    }

    fun stopWorkoutSession() {
        sessionActive = false
        lastTimerUpdateAt = 0L
        lastTimerSecondsLeft = Int.MIN_VALUE
        lastActivityUpdateAt = 0L
        lastActivitySeconds = Int.MIN_VALUE
        val intent = Intent(context, WorkoutForegroundService::class.java)
        runCatching { context.stopService(intent) }
            .onFailure { Log.w(TAG, "Workout timer notification service could not stop", it) }
        releaseTone()
    }

    // ── Timer Güncellemeleri ──────────────────────────────────────────────────

    /**
     * UI timer'ı saniyede bir akar; bildirim ise daha seyrek güncellenir.
     * Servis başlamamışsa otomatik başlatır.
     */
    fun updateRestTimer(
        exerciseName: String,
        secondsLeft: Int,
        totalSeconds: Int,
        isPaused: Boolean = false,
        force: Boolean = false
    ) {
        val now = android.os.SystemClock.elapsedRealtime()
        val shouldUpdate = force ||
            isPaused ||
            secondsLeft == totalSeconds ||
            secondsLeft <= 5 ||
            secondsLeft == 0 ||
            secondsLeft % 5 == 0 ||
            now - lastTimerUpdateAt >= TIMER_NOTIFICATION_THROTTLE_MS

        if (!shouldUpdate && secondsLeft == lastTimerSecondsLeft) return
        if (!shouldUpdate) return

        lastTimerUpdateAt = now
        lastTimerSecondsLeft = secondsLeft
        val intent = Intent(context, WorkoutForegroundService::class.java).apply {
            action = WorkoutForegroundService.ACTION_TIMER_UPDATE
            putExtra(WorkoutForegroundService.EXTRA_EXERCISE_NAME, exerciseName)
            putExtra(WorkoutForegroundService.EXTRA_SECONDS_LEFT,  secondsLeft)
            putExtra(WorkoutForegroundService.EXTRA_TOTAL_SECONDS, totalSeconds)
            putExtra(WorkoutForegroundService.EXTRA_IS_PAUSED, isPaused)
        }
        startService(intent)
    }

    fun updateActivityTimer(
        exerciseName: String,
        elapsedSeconds: Int,
        totalSeconds: Int,
        isStopwatch: Boolean,
        isPaused: Boolean = false,
        force: Boolean = false
    ) {
        val now = android.os.SystemClock.elapsedRealtime()
        val displaySeconds = if (isStopwatch) elapsedSeconds else (totalSeconds - elapsedSeconds).coerceAtLeast(0)
        val shouldUpdate = force ||
            isPaused ||
            elapsedSeconds == 0 ||
            displaySeconds <= 5 ||
            displaySeconds % 5 == 0 ||
            now - lastActivityUpdateAt >= TIMER_NOTIFICATION_THROTTLE_MS

        if (!shouldUpdate && displaySeconds == lastActivitySeconds) return
        if (!shouldUpdate) return

        sessionActive = true
        lastActivityUpdateAt = now
        lastActivitySeconds = displaySeconds
        val intent = Intent(context, WorkoutForegroundService::class.java).apply {
            action = WorkoutForegroundService.ACTION_ACTIVITY_TIMER_UPDATE
            putExtra(WorkoutForegroundService.EXTRA_EXERCISE_NAME, exerciseName)
            putExtra(WorkoutForegroundService.EXTRA_ELAPSED_SECONDS, elapsedSeconds)
            putExtra(WorkoutForegroundService.EXTRA_TOTAL_SECONDS, totalSeconds)
            putExtra(WorkoutForegroundService.EXTRA_IS_STOPWATCH, isStopwatch)
            putExtra(WorkoutForegroundService.EXTRA_IS_PAUSED, isPaused)
        }
        startService(intent)
    }

    fun notifyActivityTimerSaved(exerciseName: String, elapsedSeconds: Int) {
        lastActivityUpdateAt = 0L
        lastActivitySeconds = Int.MIN_VALUE
        val intent = Intent(context, WorkoutForegroundService::class.java).apply {
            action = WorkoutForegroundService.ACTION_ACTIVITY_TIMER_SAVED
            putExtra(WorkoutForegroundService.EXTRA_EXERCISE_NAME, exerciseName)
            putExtra(WorkoutForegroundService.EXTRA_ELAPSED_SECONDS, elapsedSeconds)
        }
        startService(intent)
    }

    /**
     * Timer sıfıra ulaştığında çağrılır:
     * 1. Bildirim sesi / titreşim
     * 2. "Hazırsın!" yüksek öncelikli bildirimi
     */
    fun notifyTimerDone(exerciseName: String) {
        lastTimerUpdateAt = 0L
        lastTimerSecondsLeft = Int.MIN_VALUE
        playTimerEndSound()
        val intent = Intent(context, WorkoutForegroundService::class.java).apply {
            action = WorkoutForegroundService.ACTION_TIMER_DONE
            putExtra(WorkoutForegroundService.EXTRA_EXERCISE_NAME, exerciseName)
        }
        startService(intent)
    }

    // ── Ses ──────────────────────────────────────────────────────────────────

    /**
     * Üç kısa bip sesi çalar. ToneGenerator sistem kütüphanesini kullanır —
     * harici ses dosyası gerekmez.
     */
    fun playTimerEndSound() {
        soundScope.launch {
            try {
                val tone = synchronized(this@WorkoutNotificationManager) {
                    if (toneGenerator == null) {
                        toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
                    }
                    toneGenerator
                } ?: return@launch
                // Three short beeps without blocking the caller thread.
                tone.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                delay(320)
                tone.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                delay(320)
                tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
            } catch (_: Exception) {
                // Ses cihazı meşgulse sessizce devam et
            }
        }
    }

    private fun releaseTone() {
        synchronized(this) {
            runCatching { toneGenerator?.release() }
            toneGenerator = null
        }
    }

    // ── Yardımcı ─────────────────────────────────────────────────────────────

    private fun startService(intent: Intent) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }.onFailure { error ->
            Log.w(TAG, "Workout timer notification service could not start", error)
        }
    }

    private companion object {
        const val TAG = "WorkoutNotifManager"
        const val TIMER_NOTIFICATION_THROTTLE_MS = 5_000L
    }
}
