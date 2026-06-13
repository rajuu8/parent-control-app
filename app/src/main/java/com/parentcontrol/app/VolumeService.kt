package com.parentcontrol.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class VolumeService : AccessibilityService() {

    private var pressCount = 0
    private var lastPressTime = 0L
    private val TIMEOUT = 5000L
    private val REQUIRED = 5 // 5 baar Volume Down press karo

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo()
        info.flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
        serviceInfo = info
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN &&
            event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {

            val now = System.currentTimeMillis()

            if (now - lastPressTime > TIMEOUT) {
                pressCount = 0
            }

            pressCount++
            lastPressTime = now

            if (pressCount >= REQUIRED) {
                pressCount = 0
                openApp()
            }
        }
        return false
    }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("secret_open", true)
        }
        startActivity(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {}
    override fun onInterrupt() {}
}
