package com.parentcontrol.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.IBinder
import android.view.KeyEvent
import androidx.core.app.NotificationCompat

class VolumeService : Service() {

    private val CHANNEL_ID = "volume_channel"
    private var volumeUpCount = 0
    private var volumeDownCount = 0
    private var lastPressTime = 0L
    private val COMBO_TIMEOUT = 3000L // 3 seconds
    private val REQUIRED_PRESSES = 3

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(2, buildNotification())
        startVolumeDetection()
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("System Service")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "System", NotificationManager.IMPORTANCE_MIN
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun startVolumeDetection() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        var lastVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        Thread {
            while (true) {
                Thread.sleep(100)
                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                val now = System.currentTimeMillis()

                if (currentVolume != lastVolume) {
                    if (now - lastPressTime > COMBO_TIMEOUT) {
                        volumeUpCount = 0
                        volumeDownCount = 0
                    }

                    if (currentVolume > lastVolume) {
                        volumeUpCount++
                    } else {
                        volumeDownCount++
                    }

                    lastPressTime = now
                    lastVolume = currentVolume

                    if (volumeUpCount >= REQUIRED_PRESSES && volumeDownCount >= REQUIRED_PRESSES) {
                        volumeUpCount = 0
                        volumeDownCount = 0
                        openApp()
                    }
                }
            }
        }.start()
    }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("secret_open", true)
        }
        startActivity(intent)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
