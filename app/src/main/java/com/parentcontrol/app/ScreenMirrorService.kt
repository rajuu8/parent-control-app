package com.parentcontrol.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import okhttp3.*
import okio.ByteString.Companion.toByteString
import java.io.ByteArrayOutputStream

class ScreenMirrorService : Service() {

    private val CHANNEL_ID = "screen_mirror_channel"
    private val WS_URL = "wss://overflowing-perception-production-17b2.up.railway.app"
    private val DEVICE_NAME = android.os.Build.MODEL
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var wsScreen: okhttp3.WebSocket? = null
    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        var resultCode = 0
        var resultData: Intent? = null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(3, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.getStringExtra("action")) {
            "start_screen" -> startScreenMirror()
            "stop_screen" -> stopScreenMirror()
        }
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Mirror Active")
            .setContentText("Screen sharing is running")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Screen Mirror", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun startScreenMirror() {
        isRunning = true
        val client = OkHttpClient.Builder()
            .pingInterval(20, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()

        wsScreen = client.newWebSocket(
            Request.Builder().url("$WS_URL?type=screen&device=$DEVICE_NAME").build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: okhttp3.WebSocket, response: Response) {
                    startCapture(webSocket)
                }
                override fun onFailure(webSocket: okhttp3.WebSocket, t: Throwable, response: Response?) {
                    isRunning = false
                }
            }
        )
    }

    private fun startCapture(webSocket: okhttp3.WebSocket) {
        try {
            val metrics = DisplayMetrics()
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.defaultDisplay.getMetrics(metrics)

            val width = metrics.widthPixels / 2
            val height = metrics.heightPixels / 2
            val density = metrics.densityDpi

            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData!!)

            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenCapture", width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface, null, null
            )

            imageReader?.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                try {
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * width

                    val bitmap = android.graphics.Bitmap.createBitmap(
                        width + rowPadding / pixelStride, height,
                        android.graphics.Bitmap.Config.ARGB_8888
                    )
                    bitmap.copyPixelsFromBuffer(buffer)

                    val stream = ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 40, stream)
                    val bytes = stream.toByteArray()
                    bitmap.recycle()

                    if (isRunning) webSocket.send(bytes.toByteString())
                } finally {
                    image.close()
                }
            }, handler)

        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun stopScreenMirror() {
        isRunning = false
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        wsScreen?.close(1000, "Stop")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
