package com.parentcontrol.app

import android.app.usage.UsageStatsManager
import android.content.Context
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class AppUsageReader(private val context: Context) {

    private val SERVER_URL = "https://overflowing-perception-production-17b2.up.railway.app/appusage"
    private val DEVICE_NAME = android.os.Build.MODEL
    private val parentCode: String
        get() {
            val prefs = context.getSharedPreferences("parent_control", Context.MODE_PRIVATE)
            return prefs.getString("parent_code", "") ?: ""
        }

    fun readAndSend() {
        Thread {
            try {
                val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val endTime = System.currentTimeMillis()
                val startTime = endTime - 24 * 60 * 60 * 1000

                val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
                val appList = JSONArray()
                stats?.filter { it.totalTimeInForeground > 0 }
                    ?.sortedByDescending { it.totalTimeInForeground }
                    ?.take(20)
                    ?.forEach { stat ->
                        appList.put(JSONObject().apply {
                            put("package", stat.packageName)
                            put("minutes", stat.totalTimeInForeground / 60000)
                        })
                    }

                val json = JSONObject().apply {
                    put("device", DEVICE_NAME)
                    put("apps", appList)
                    put("code", parentCode)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                OkHttpClient().newCall(
                    Request.Builder().url(SERVER_URL).post(body).build()
                ).execute()
            } catch (e: Exception) { e.printStackTrace() }
        }.start()
    }
}
