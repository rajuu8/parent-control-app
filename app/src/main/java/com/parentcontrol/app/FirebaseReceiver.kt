package com.parentcontrol.app

import android.content.Intent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.FirebaseMessaging
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
        registerToken(token)
    }

    companion object {
        fun registerTokenToServer() {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                registerToken(token)
            }
        }

        fun registerToken(token: String) {
            val context = try {
                com.google.firebase.FirebaseApp.getInstance().applicationContext
            } catch (e: Exception) { return }

            val prefs = context.getSharedPreferences("parent_control", android.content.Context.MODE_PRIVATE)
            val code = prefs.getString("parent_code", null) ?: return
            val deviceName = android.os.Build.MODEL

            Thread {
                var retries = 3
                while (retries > 0) {
                    try {
                        val client = OkHttpClient()
                        val json = """{"token":"$token","device":"$deviceName","code":"$code"}"""
                        val body = json.toRequestBody("application/json".toMediaType())
                        val request = Request.Builder()
                            .url("https://overflowing-perception-production-17b2.up.railway.app/register")
                            .post(body)
                            .build()
                        val response = client.newCall(request).execute()
                        if (response.isSuccessful) break
                        retries--
                        Thread.sleep(2000)
                    } catch (e: Exception) {
                        retries--
                        Thread.sleep(2000)
                    }
                }
            }.start()
        }
    }
}
