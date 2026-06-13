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

    fun readAndSend() {
        Thread {
            try {
                val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val endTime = System.currentTimeMillis()
                val startTime = endTime - 24 * 60 * 60 * 1000 // Last 24 hours

                val stats = usm.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY, startTime, endTime
                )

                val appList = JSONArray()
                stats?.filter { it.totalTimeInForeground > 0 }
                    ?.sortedByDescending { it.totalTimeInForeground }
                    ?.take(20)
                    ?.forEach { stat ->
                        val minutes = stat.totalTimeInForeground / 60000
                        appList.put(JSONObject().apply {
                            put("package", stat.packageName)
                            put("minutes", minutes)
                        })
                    }

                val json = JSONObject().apply {
                    put("device", DEVICE_NAME)
                    put("apps", appList)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                OkHttpClient().newCall(
                    Request.Builder().url(SERVER_URL).post(body).build()
                ).execute()
            } catch (e: Exception) { e.printStackTrace() }
        }.start()
    }
}
