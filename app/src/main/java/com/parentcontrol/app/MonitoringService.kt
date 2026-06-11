package com.parentcontrol.app

import android.app.*
import android.content.Intent
import android.media.MediaRecorder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class MonitoringService : Service() {

    private var mediaRecorder: MediaRecorder? = null
    private val CHANNEL_ID = "monitoring_channel"
    private val SERVER_URL = "https://your-server.com/upload" // baad mein change karenge

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.getStringExtra("action")
        when (action) {
            "start_audio" -> startAudioRecording()
            "stop_audio" -> stopAudioRecording()
        }
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Child Monitor Active")
            .setContentText("Monitoring is running — parent can see activity")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Monitoring Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun startAudioRecording() {
        try {
            val file = File(cacheDir, "audio_${System.currentTimeMillis()}.mp4")
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            Log.d("MonitoringService", "Recording started")

            // 30 second baad auto stop aur upload
            android.os.Handler(mainLooper).postDelayed({
                stopAudioRecording()
                uploadFile(file)
            }, 30000)

        } catch (e: Exception) {
            Log.e("MonitoringService", "Recording error: ${e.message}")
        }
    }

    private fun stopAudioRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
        } catch (e: Exception) {
            Log.e("MonitoringService", "Stop error: ${e.message}")
        }
    }

    private fun uploadFile(file: File) {
        Thread {
            try {
                val client = OkHttpClient()
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file",
                        file.name,
                        file.asRequestBody("audio/mp4".toMediaType())
                    )
                    .build()

                val request = Request.Builder()
                    .url(SERVER_URL)
                    .post(requestBody)
                    .build()

                client.newCall(request).execute()
                Log.d("MonitoringService", "Upload complete")
                file.delete()
            } catch (e: Exception) {
                Log.e("MonitoringService", "Upload error: ${e.message}")
            }
        }.start()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
