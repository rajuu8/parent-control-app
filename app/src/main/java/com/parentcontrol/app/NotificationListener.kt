package com.parentcontrol.app

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class NotificationListener : NotificationListenerService() {

    private val SERVER_URL = "https://overflowing-perception-production-17b2.up.railway.app/notification"
    private val DEVICE_NAME = android.os.Build.MODEL
    private val parentCode: String
        get() {
            val prefs = getSharedPreferences("parent_control", Context.MODE_PRIVATE)
            return prefs.getString("parent_code", "") ?: ""
        }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val extras = sbn.notification.extras
            val title = extras.getString("android.title") ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            val appPackage = sbn.packageName
            val time = System.currentTimeMillis()

            if (title.isEmpty() && text.isEmpty()) return

            val skipPackages = listOf("android", "com.android.systemui", "com.android.settings")
            if (skipPackages.contains(appPackage)) return

            val json = JSONObject().apply {
                put("app", appPackage)
                put("title", title)
                put("text", text)
                put("time", time)
                put("device", DEVICE_NAME)
                put("code", parentCode)
            }

            Thread {
                try {
                    val body = json.toString().toRequestBody("application/json".toMediaType())
                    OkHttpClient().newCall(
                        Request.Builder().url(SERVER_URL).post(body).build()
                    ).execute()
                } catch (e: Exception) { e.printStackTrace() }
            }.start()
        } catch (e: Exception) { e.printStackTrace() }
    }
}
