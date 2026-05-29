package com.avonix.profitness.service

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.avonix.profitness.core.notification.WorkoutNotificationAction
import com.avonix.profitness.core.notification.WorkoutNotificationActionBus
import com.avonix.profitness.R
import com.avonix.profitness.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Antrenman süresince çalışan ön plan servisi.
 * - Kalıcı bildirim: rest timer geri sayımı
 * - Timer bitince titreşim + yüksek öncelikli "Hazırsın!" bildirimi
 * - Uygulama arka plandayken bile çalışır
 */
@AndroidEntryPoint
class WorkoutForegroundService : Service() {

    companion object {
        const val TAG = "WorkoutFgService"
        const val CHANNEL_WORKOUT = "ch_workout_status"
        const val CHANNEL_ALERTS  = "ch_workout_alerts"
        const val NOTIF_ID_STATUS = 1001
        const val NOTIF_ID_ALERT  = 1002

        const val ACTION_START        = "com.avonix.profitness.START_WORKOUT"
        const val ACTION_TIMER_UPDATE = "com.avonix.profitness.TIMER_UPDATE"
        const val ACTION_TIMER_DONE   = "com.avonix.profitness.TIMER_DONE"
        const val ACTION_ACTIVITY_TIMER_UPDATE = "com.avonix.profitness.ACTIVITY_TIMER_UPDATE"
        const val ACTION_ACTIVITY_TIMER_SAVED  = "com.avonix.profitness.ACTIVITY_TIMER_SAVED"
        const val ACTION_STOP         = "com.avonix.profitness.STOP_WORKOUT"
        const val ACTION_PAUSE_TIMER  = "com.avonix.profitness.PAUSE_TIMER"
        const val ACTION_RESUME_TIMER = "com.avonix.profitness.RESUME_TIMER"
        const val ACTION_STOP_TIMER   = "com.avonix.profitness.STOP_TIMER"
        const val ACTION_DISMISS_TIMER = "com.avonix.profitness.DISMISS_TIMER"

        const val EXTRA_DAY_TITLE     = "day_title"
        const val EXTRA_EXERCISE_NAME = "exercise_name"
        const val EXTRA_SECONDS_LEFT  = "seconds_left"
        const val EXTRA_TOTAL_SECONDS = "total_seconds"
        const val EXTRA_ELAPSED_SECONDS = "elapsed_seconds"
        const val EXTRA_IS_STOPWATCH = "is_stopwatch"
        const val EXTRA_IS_PAUSED = "is_paused"

        const val REQUEST_TIMER_TOGGLE = 2001
        const val REQUEST_TIMER_STOP = 2002
        const val REQUEST_TIMER_DISMISS = 2003
    }

    @Inject
    lateinit var actionBus: WorkoutNotificationActionBus

    private lateinit var notifManager: NotificationManager
    private var isForeground = false

    override fun onCreate() {
        super.onCreate()
        notifManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        runCatching {
            when (intent?.action) {
                ACTION_START -> {
                    val dayTitle = intent.getStringExtra(EXTRA_DAY_TITLE) ?: "Antrenman"
                    val notif = buildStatusNotification(
                        title   = "🏋️ Antrenman Devam Ediyor",
                        content = dayTitle,
                        progress = 0, maxProgress = 0
                    )
                    promoteOrNotify(notif)
                }

                ACTION_TIMER_UPDATE -> {
                    val exerciseName = intent.getStringExtra(EXTRA_EXERCISE_NAME) ?: ""
                    val secondsLeft  = intent.getIntExtra(EXTRA_SECONDS_LEFT, 0)
                    val totalSeconds = intent.getIntExtra(EXTRA_TOTAL_SECONDS, 0)
                    val isPaused = intent.getBooleanExtra(EXTRA_IS_PAUSED, false)
                    val min = secondsLeft / 60
                    val sec = secondsLeft % 60
                    val timeStr = if (min > 0) "${min}d ${sec.toString().padStart(2,'0')}s"
                                  else "${secondsLeft}s"
                    val notif = buildStatusNotification(
                        title       = if (isPaused) "Dinlenme duraklatıldı" else "⏱️  $timeStr  —  Dinlenme süresi",
                        content     = if (isPaused) "$exerciseName • $timeStr kaldı" else exerciseName,
                        progress    = totalSeconds - secondsLeft,
                        maxProgress = totalSeconds,
                        controls = NotificationControls(
                            canPauseResume = true,
                            isPaused = isPaused,
                            stopLabel = "Durdur"
                        ),
                        color = if (isPaused) 0xFFFFB020.toInt() else 0xFFFF444B.toInt()
                    )
                    promoteOrNotify(notif)
                }

                ACTION_ACTIVITY_TIMER_UPDATE -> {
                    val exerciseName = intent.getStringExtra(EXTRA_EXERCISE_NAME) ?: ""
                    val elapsedSeconds = intent.getIntExtra(EXTRA_ELAPSED_SECONDS, 0)
                    val totalSeconds = intent.getIntExtra(EXTRA_TOTAL_SECONDS, 0)
                    val isStopwatch = intent.getBooleanExtra(EXTRA_IS_STOPWATCH, true)
                    val isPaused = intent.getBooleanExtra(EXTRA_IS_PAUSED, false)
                    val secondsToShow = if (isStopwatch) elapsedSeconds else (totalSeconds - elapsedSeconds).coerceAtLeast(0)
                    val timeStr = formatDuration(secondsToShow)
                    val title = exerciseName.ifBlank { "Sayaç" }
                    val content = when {
                        isPaused -> "Duraklatıldı • $timeStr"
                        isStopwatch -> "Kronometre çalışıyor"
                        else -> "Geri sayım • $timeStr kaldı"
                    }
                    val notif = buildStatusNotification(
                        title = title,
                        content = content,
                        progress = if (isStopwatch) 0 else elapsedSeconds,
                        maxProgress = if (isStopwatch) 0 else totalSeconds,
                        chronometerBaseMillis = if (isStopwatch && !isPaused)
                            System.currentTimeMillis() - elapsedSeconds * 1000L else null,
                        showChronometer = isStopwatch && !isPaused,
                        color = if (isPaused) 0xFFFFB020.toInt() else 0xFFFF444B.toInt(),
                        controls = NotificationControls(
                            canPauseResume = true,
                            isPaused = isPaused,
                            stopLabel = "Kaydet",
                            canDismiss = true
                        )
                    )
                    promoteOrNotify(notif)
                }

                ACTION_ACTIVITY_TIMER_SAVED -> {
                    val exerciseName = intent.getStringExtra(EXTRA_EXERCISE_NAME) ?: ""
                    val elapsedSeconds = intent.getIntExtra(EXTRA_ELAPSED_SECONDS, 0)
                    val notif = buildStatusNotification(
                        title = exerciseName.ifBlank { "Sayaç" },
                        content = "Süre kaydedildi • ${formatDuration(elapsedSeconds)}",
                        progress = 0,
                        maxProgress = 0,
                        color = 0xFFFF444B.toInt()
                    )
                    promoteOrNotify(notif)
                }

                ACTION_TIMER_DONE -> {
                    val exerciseName = intent.getStringExtra(EXTRA_EXERCISE_NAME) ?: ""
                    // Durum bildirimini güncelle
                    val statusNotif = buildStatusNotification(
                        title   = "Hazırsın! Sonraki seti başlat",
                        content = exerciseName,
                        progress = 0, maxProgress = 0
                    )
                    promoteOrNotify(statusNotif)

                    // Yüksek öncelikli uyarı bildirimi (ses/titreşim)
                    val alertNotif = buildAlertNotification(exerciseName)
                    notifManager.notify(NOTIF_ID_ALERT, alertNotif)
                }

                ACTION_STOP -> {
                    notifManager.cancel(NOTIF_ID_ALERT)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    isForeground = false
                    stopSelf()
                }

                ACTION_PAUSE_TIMER -> actionBus.dispatch(WorkoutNotificationAction.Pause)
                ACTION_RESUME_TIMER -> actionBus.dispatch(WorkoutNotificationAction.Resume)
                ACTION_STOP_TIMER -> actionBus.dispatch(WorkoutNotificationAction.Stop)
                ACTION_DISMISS_TIMER -> actionBus.dispatch(WorkoutNotificationAction.Dismiss)
            }
        }.onFailure { error ->
            Log.e(TAG, "Workout foreground service failed", error)
            stopForeground(STOP_FOREGROUND_REMOVE)
            isForeground = false
            stopSelf()
        }
        return START_NOT_STICKY
    }

    /**
     * Kullanıcı uygulamayı son uygulamalar listesinden kapattığında
     * servisi ve bildirimi temizle. Böylece process serbest kalır ve
     * uygulama bir sonraki açılışta takılmaz.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        notifManager.cancel(NOTIF_ID_ALERT)
        stopForeground(STOP_FOREGROUND_REMOVE)
        isForeground = false
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        runCatching { notifManager.cancel(NOTIF_ID_ALERT) }
        isForeground = false
        super.onDestroy()
    }

    // ── Notification Builders ─────────────────────────────────────────────────

    private fun buildStatusNotification(
        title: String,
        content: String,
        progress: Int,
        maxProgress: Int,
        chronometerBaseMillis: Long? = null,
        showChronometer: Boolean = false,
        color: Int = 0xFFFF444B.toInt(),
        controls: NotificationControls = NotificationControls()
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
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setSubText("Forge Sayaç")
            .setColor(color)

        addTimerActions(builder, controls)

        if (maxProgress > 0) {
            builder.setProgress(maxProgress, progress, false)
        }
        if (showChronometer && chronometerBaseMillis != null) {
            builder
                .setWhen(chronometerBaseMillis)
                .setShowWhen(true)
                .setUsesChronometer(true)
        } else {
            builder.setShowWhen(false)
        }
        return builder.build()
    }

    private fun addTimerActions(
        builder: NotificationCompat.Builder,
        controls: NotificationControls
    ) {
        if (controls.canPauseResume) {
            val action = if (controls.isPaused) ACTION_RESUME_TIMER else ACTION_PAUSE_TIMER
            val icon = if (controls.isPaused) R.drawable.ic_notification_play else R.drawable.ic_notification_pause
            val label = if (controls.isPaused) "Sürdür" else "Duraklat"
            builder.addAction(icon, label, servicePendingIntent(action, REQUEST_TIMER_TOGGLE))
        }
        controls.stopLabel?.let { label ->
            builder.addAction(
                R.drawable.ic_notification_done,
                label,
                servicePendingIntent(ACTION_STOP_TIMER, REQUEST_TIMER_STOP)
            )
        }
        if (controls.canDismiss) {
            builder.addAction(
                R.drawable.ic_notification_stop,
                "Kapat",
                servicePendingIntent(ACTION_DISMISS_TIMER, REQUEST_TIMER_DISMISS)
            )
        }
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this,
            requestCode,
            Intent(this, WorkoutForegroundService::class.java).apply { this.action = action },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun buildAlertNotification(exerciseName: String): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 1,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ALERTS)
            .setContentTitle("Hazırsın! Set zamanı!")
            .setContentText("$exerciseName — Sonraki seti başlat")
            .setSmallIcon(R.drawable.ic_stat_timer)
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
            description = "Antrenman sırasında sayaç, kronometre ve durum göstergesi"
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

    private fun promoteOrNotify(notification: Notification) {
        runCatching {
            if (!isForeground) {
                startForeground(NOTIF_ID_STATUS, notification)
                isForeground = true
            } else {
                notifManager.notify(NOTIF_ID_STATUS, notification)
            }
        }.onFailure { error ->
            Log.e(TAG, "Timer notification could not be shown", error)
            throw error
        }
    }

    private fun formatDuration(totalSeconds: Int): String {
        val safe = totalSeconds.coerceAtLeast(0)
        val hours = safe / 3600
        val minutes = (safe % 3600) / 60
        val seconds = safe % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }

    private data class NotificationControls(
        val canPauseResume: Boolean = false,
        val isPaused: Boolean = false,
        val stopLabel: String? = null,
        val canDismiss: Boolean = false
    )
}
