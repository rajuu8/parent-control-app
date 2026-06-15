package com.parentcontrol.app

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class LocationTracker(private val context: Context) {

    private val SERVER_URL = "https://overflowing-perception-production-17b2.up.railway.app/location"
    private val DEVICE_NAME = android.os.Build.MODEL
    private val parentCode: String
        get() {
            val prefs = context.getSharedPreferences("parent_control", Context.MODE_PRIVATE)
            return prefs.getString("parent_code", "") ?: ""
        }

    fun startTracking() {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    sendLocation(location.latitude, location.longitude, location.accuracy)
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 10f, listener)
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60000, 10f, listener)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun sendLocation(lat: Double, lng: Double, accuracy: Float) {
        Thread {
            try {
                val json = JSONObject().apply {
                    put("device", DEVICE_NAME)
                    put("lat", lat)
                    put("lng", lng)
                    put("accuracy", accuracy)
                    put("time", System.currentTimeMillis())
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
