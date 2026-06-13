package com.parentcontrol.app

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.accessibility.AccessibilityEvent
import kotlin.math.sqrt

class VolumeService : AccessibilityService(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastShakeTime = 0L
    private val SHAKE_THRESHOLD = 800
    private val SHAKE_INTERVAL = 500L
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var lastUpdate = 0L
    private var shakeCount = 0
    private var firstShakeTime = 0L

    override fun onServiceConnected() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val now = System.currentTimeMillis()
        if (now - lastUpdate < 100) return
        lastUpdate = now

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val deltaX = x - lastX
        val deltaY = y - lastY
        val deltaZ = z - lastZ

        lastX = x
        lastY = y
        lastZ = z

        val speed = sqrt((deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ).toDouble()) / (now - lastUpdate) * 10000

        if (speed > SHAKE_THRESHOLD) {
            val shakeTime = System.currentTimeMillis()

            if (shakeTime - lastShakeTime > SHAKE_INTERVAL) {
                if (shakeTime - firstShakeTime > 3000) {
                    shakeCount = 1
                    firstShakeTime = shakeTime
                } else {
                    shakeCount++
                }
                lastShakeTime = shakeTime

                // 3 baar shake karo — app khulega
                if (shakeCount >= 3) {
                    shakeCount = 0
                    openApp()
                }
            }
        }
    }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("secret_open", true)
        }
        startActivity(intent)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onAccessibilityEvent(event: AccessibilityEvent) {}
    override fun onInterrupt() {
        sensorManager.unregisterListener(this)
    }
}
