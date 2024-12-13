package com.example.androidapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.example.androidapp.R

class AudioService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "AudioServiceChannel"
        const val ACTION_PLAY_PAUSE = "action_play_pause"
        const val ACTION_STOP = "action_stop"
    }
    override fun onBind(intent: Intent): IBinder? = null
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        mediaPlayer = MediaPlayer.create(this, R.raw.song)
        mediaPlayer?.isLooping = true
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Audio Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls for audio playback"
                setSound(null, null)
                enableLights(false)
                enableVibration(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> togglePlayback()
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startPlayback()
            }
        }
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        return START_NOT_STICKY
    }
    private fun createNotification(): Notification {
        val playPauseIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, AudioService::class.java).apply { action = ACTION_PLAY_PAUSE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, AudioService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // Create media style
        val mediaStyle = MediaStyle()
            .setShowActionsInCompactView(0, 1) // Показывает обе кнопки даже в свернутом виде
        // Create notification
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Playing Audio")
            .setContentText(if (isPlaying) "Now Playing" else "Paused")
            .setAutoCancel(false)
            .setSmallIcon(R.drawable.music)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setStyle(mediaStyle)
            .addAction(
                if (isPlaying) R.drawable.pause else R.drawable.play,
                if (isPlaying) "Pause" else "Play",
                playPauseIntent
            )
            .addAction(
                R.drawable.close,
                "Stop",
                stopIntent
            )
            .build()
    }
    private fun startPlayback() {
        mediaPlayer?.start()
        isPlaying = true
        updateNotification()
    }
    private fun togglePlayback() {
        if (isPlaying) {
            mediaPlayer?.pause()
        } else {
            mediaPlayer?.start()
        }
        isPlaying = !isPlaying
        updateNotification()
    }
    private fun updateNotification() {
        val notification = createNotification()
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }
}