package com.parentcontrol.app

import android.content.Intent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class FirebaseReceiver : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val action = remoteMessage.data["action"]
        val intent = Intent(this, MonitoringService::class.java)
        intent.putExtra("action", action)
        startForegroundService(intent)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Thread {
            try {
                val client = OkHttpClient()
                val json = """{"token":"$token"}"""
                val body = json.toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("https://overflowing-perception-production-17b2.up.railway.app/register")
                    .post(body)
                    .build()
                client.newCall(request).execute()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
