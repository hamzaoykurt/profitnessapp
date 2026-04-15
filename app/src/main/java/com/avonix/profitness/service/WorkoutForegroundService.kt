package com.avonix.profitness.service

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.avonix.profitness.MainActivity

/**
 * Antrenman süresince çalışan ön plan servisi.
 * - Kalıcı bildirim: rest timer geri sayımı
 * - Timer bitince titreşim + yüksek öncelikli "Hazırsın!" bildirimi
 * - Uygulama arka plandayken bile çalışır
 */
class WorkoutForegroundService : Service() {

    companion object {
        const val CHANNEL_WORKOUT = "ch_workout_status"
        const val CHANNEL_ALERTS  = "ch_workout_alerts"
        const val NOTIF_ID_STATUS = 1001
        const val NOTIF_ID_ALERT  = 1002

        const val ACTION_START        = "com.avonix.profitness.START_WORKOUT"
        const val ACTION_TIMER_UPDATE = "com.avonix.profitness.TIMER_UPDATE"
        const val ACTION_TIMER_DONE   = "com.avonix.profitness.TIMER_DONE"
        const val ACTION_STOP         = "com.avonix.profitness.STOP_WORKOUT"

        const val EXTRA_DAY_TITLE     = "day_title"
        const val EXTRA_EXERCISE_NAME = "exercise_name"
        const val EXTRA_SECONDS_LEFT  = "seconds_left"
        const val EXTRA_TOTAL_SECONDS = "total_seconds"
    }

    private lateinit var notifManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notifManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val dayTitle = intent.getStringExtra(EXTRA_DAY_TITLE) ?: "Antrenman"
                val notif = buildStatusNotification(
                    title   = "🏋️ Antrenman Devam Ediyor",
                    content = dayTitle,
                    progress = 0, maxProgress = 0
                )
                startForeground(NOTIF_ID_STATUS, notif)
            }

            ACTION_TIMER_UPDATE -> {
                val exerciseName = intent.getStringExtra(EXTRA_EXERCISE_NAME) ?: ""
                val secondsLeft  = intent.getIntExtra(EXTRA_SECONDS_LEFT, 0)
                val totalSeconds = intent.getIntExtra(EXTRA_TOTAL_SECONDS, 0)
                val min = secondsLeft / 60
                val sec = secondsLeft % 60
                val timeStr = if (min > 0) "${min}d ${sec.toString().padStart(2,'0')}s"
                              else "${secondsLeft}s"
                val notif = buildStatusNotification(
                    title       = "⏱️  $timeStr  —  Dinlenme süresi",
                    content     = exerciseName,
                    progress    = totalSeconds - secondsLeft,
                    maxProgress = totalSeconds
                )
                notifManager.notify(NOTIF_ID_STATUS, notif)
            }

            ACTION_TIMER_DONE -> {
                val exerciseName = intent.getStringExtra(EXTRA_EXERCISE_NAME) ?: ""
                // Durum bildirimini güncelle
                val statusNotif = buildStatusNotification(
                    title   = "✅ Hazırsın! Sonraki seti başlat",
                    content = exerciseName,
                    progress = 0, maxProgress = 0
                )
                notifManager.notify(NOTIF_ID_STATUS, statusNotif)

                // Yüksek öncelikli uyarı bildirimi (ses/titreşim)
                val alertNotif = buildAlertNotification(exerciseName)
                notifManager.notify(NOTIF_ID_ALERT, alertNotif)
            }

            ACTION_STOP -> {
                notifManager.cancel(NOTIF_ID_ALERT)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Notification Builders ─────────────────────────────────────────────────

    private fun buildStatusNotification(
        title: String,
        content: String,
        progress: Int,
        maxProgress: Int
    ): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_WORKOUT)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)

        if (maxProgress > 0) {
            builder.setProgress(maxProgress, progress, false)
        }
        return builder.build()
    }

    private fun buildAlertNotification(exerciseName: String): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 1,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ALERTS)
            .setContentTitle("💪 Hazırsın! Set zamanı!")
            .setContentText("$exerciseName — Sonraki seti başlat")
            .setSmallIcon(android.R.drawable.ic_media_next)
            .setContentIntent(tapIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 300, 150, 300, 150, 300))
            .setAutoCancel(true)
            .build()
    }

    // ── Channel Setup ─────────────────────────────────────────────────────────

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        // Düşük öncelik — kalıcı durum çubuğu bildirimi (ses yok)
        val workoutChannel = NotificationChannel(
            CHANNEL_WORKOUT,
            "Antrenman Durumu",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Antrenman sırasında timer ve durum göstergesi"
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }

        // Yüksek öncelik — set arası bitti uyarısı
        val alertChannel = NotificationChannel(
            CHANNEL_ALERTS,
            "Antrenman Uyarıları",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Dinlenme süresi bitişi ve set tamamlama bildirimleri"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 150, 300, 150, 300)
        }

        notifManager.createNotificationChannels(listOf(workoutChannel, alertChannel))
    }
}
