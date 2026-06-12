package com.parentcontrol.app

import android.app.*
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.net.URI

class MonitoringService : Service() {

    private var audioRecord: AudioRecord? = null
    private val CHANNEL_ID = "monitoring_channel"
    private val SERVER_URL = "https://overflowing-perception-production-17b2.up.railway.app/upload"
    private val WS_URL = "wss://overflowing-perception-production-17b2.up.railway.app?type=child"
    private var isRecording = false
    private var isLive = false
    private var wsClient: okhttp3.WebSocket? = null
    private val SAMPLE_RATE = 16000
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(
        SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ) * 4

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.getStringExtra("action")) {
            "start_audio" -> startAudioRecording()
            "stop_audio" -> stopAudioRecording()
            "start_live" -> startLiveStream()
            "stop_live" -> stopLiveStream()
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
            CHANNEL_ID, "Monitoring Service", NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    // --- RECORDING ---
    private fun startAudioRecording() {
        try {
            val file = File(cacheDir, "audio_${System.currentTimeMillis()}.mp4")
            val recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            android.os.Handler(mainLooper).postDelayed({
                recorder.stop()
                recorder.release()
                uploadFile(file)
            }, 30000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAudioRecording() {
        isRecording = false
    }

    private fun uploadFile(file: File) {
        Thread {
            try {
                val client = OkHttpClient()
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.name, file.asRequestBody("audio/mp4".toMediaType()))
                    .build()
                val request = Request.Builder().url(SERVER_URL).post(body).build()
                client.newCall(request).execute()
                file.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    // --- LIVE STREAM ---
    private fun startLiveStream() {
        isLive = true
        val client = OkHttpClient()
        val request = Request.Builder().url(WS_URL).build()
        wsClient = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: okhttp3.WebSocket, response: Response) {
                streamMicToWebSocket(webSocket)
            }
            override fun onFailure(webSocket: okhttp3.WebSocket, t: Throwable, response: Response?) {
                isLive = false
            }
        })
    }

    private fun streamMicToWebSocket(webSocket: okhttp3.WebSocket) {
        Thread {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                BUFFER_SIZE
            )
            audioRecord?.startRecording()
            val buffer = ByteArray(BUFFER_SIZE)
            while (isLive) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    webSocket.send(okio.ByteString.of(*buffer.copyOf(read)))
                }
            }
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        }.start()
    }

    private fun stopLiveStream() {
        isLive = false
        wsClient?.close(1000, "Stop")
        wsClient = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
