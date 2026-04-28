package com.avonix.profitness.core.notification

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import com.avonix.profitness.service.WorkoutForegroundService
import dagger.hilt.android.qualifiers.ApplicationContext
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

    // ── Oturum Kontrolü ──────────────────────────────────────────────────────

    fun startWorkoutSession(dayTitle: String) {
        sessionActive = true
        lastTimerUpdateAt = 0L
        lastTimerSecondsLeft = Int.MIN_VALUE
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
        val intent = Intent(context, WorkoutForegroundService::class.java).apply {
            action = WorkoutForegroundService.ACTION_STOP
        }
        context.startService(intent)
        releaseTone()
    }

    // ── Timer Güncellemeleri ──────────────────────────────────────────────────

    /**
     * UI timer'ı saniyede bir akar; bildirim ise daha seyrek güncellenir.
     * Servis başlamamışsa otomatik başlatır.
     */
    fun updateRestTimer(exerciseName: String, secondsLeft: Int, totalSeconds: Int) {
        val now = android.os.SystemClock.elapsedRealtime()
        val shouldUpdate = secondsLeft == totalSeconds ||
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
        try {
            if (toneGenerator == null) {
                toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
            }
            val tone = toneGenerator ?: return
            // Üç ardışık bip: 200ms bip → 120ms sessiz → 200ms bip → 120ms sessiz → 400ms bip
            tone.startTone(ToneGenerator.TONE_PROP_BEEP,  200)
            Thread.sleep(320)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP,  200)
            Thread.sleep(320)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
        } catch (_: Exception) {
            // Ses cihazı meşgulse sessizce devam et
        }
    }

    private fun releaseTone() {
        runCatching { toneGenerator?.release() }
        toneGenerator = null
    }

    // ── Yardımcı ─────────────────────────────────────────────────────────────

    private fun startService(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private companion object {
        const val TIMER_NOTIFICATION_THROTTLE_MS = 5_000L
    }
}
