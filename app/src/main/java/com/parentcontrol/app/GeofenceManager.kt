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

class GeofenceManager(private val context: Context) {

    private val SERVER_URL = "https://overflowing-perception-production-17b2.up.railway.app"
    private val DEVICE_NAME = android.os.Build.MODEL
    private var geofenceLat = 0.0
    private var geofenceLng = 0.0
    private var geofenceRadius = 500.0 // meters
    private var isActive = false
    private var wasInside = true

    fun startGeofence(lat: Double, lng: Double, radius: Double) {
        geofenceLat = lat
        geofenceLng = lng
        geofenceRadius = radius
        isActive = true

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 30000, 10f,
                object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        checkGeofence(location)
                    }
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }
            )
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun checkGeofence(location: Location) {
        if (!isActive) return
        val results = FloatArray(1)
        Location.distanceBetween(
            geofenceLat, geofenceLng,
            location.latitude, location.longitude,
            results
        )
        val distance = results[0]
        val isInside = distance <= geofenceRadius

        if (wasInside && !isInside) {
            sendAlert("EXIT", location.latitude, location.longitude, distance)
        } else if (!wasInside && isInside) {
            sendAlert("ENTER", location.latitude, location.longitude, distance)
        }
        wasInside = isInside
    }

    private fun sendAlert(type: String, lat: Double, lng: Double, distance: Float) {
        Thread {
            try {
                val json = JSONObject().apply {
                    put("device", DEVICE_NAME)
                    put("type", type)
                    put("lat", lat)
                    put("lng", lng)
                    put("distance", distance)
                    put("time", System.currentTimeMillis())
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                OkHttpClient().newCall(
                    Request.Builder().url("$SERVER_URL/geofence_alert").post(body).build()
                ).execute()
            } catch (e: Exception) { e.printStackTrace() }
        }.start()
    }
}
