package com.example.puzzlealarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var audioManager: AudioManager

    private var maxAlarmVolume: Int = 0
    private var targetAlarmVolume: Int = 0
    private val ALARM_VOLUME_PERCENT = 0.6f

    private val volumeHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val volumeRunnable = object : Runnable {
        override fun run() {
            if (AlarmState.isAlarmRunning) {
                audioManager.setStreamVolume(
                    AudioManager.STREAM_ALARM,
                    targetAlarmVolume,
                    AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
                )
                volumeHandler.postDelayed(this, 300)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        AlarmState.isAlarmRunning = true

        val fullScreenIntent = Intent(this, PuzzleActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification =
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Alarm ringing")
                .setContentText("Solve the puzzle to stop the alarm")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // 🔊 Alarm volume control (60%)
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        maxAlarmVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        targetAlarmVolume =
            (maxAlarmVolume * ALARM_VOLUME_PERCENT).toInt().coerceAtLeast(1)

        audioManager.setStreamVolume(
            AudioManager.STREAM_ALARM,
            targetAlarmVolume,
            AudioManager.FLAG_PLAY_SOUND
        )

        volumeHandler.post(volumeRunnable)

        // 🔔 Play alarm sound (ALARM stream)
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(
                    this@AlarmService,
                    android.net.Uri.parse("android.resource://${packageName}/${R.raw.alarm}")
                )
                isLooping = true
                prepare()
                start()
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        AlarmState.isAlarmRunning = false
        volumeHandler.removeCallbacks(volumeRunnable)
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm",
                NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "alarm_channel"
        private const val NOTIFICATION_ID = 1
    }
}
