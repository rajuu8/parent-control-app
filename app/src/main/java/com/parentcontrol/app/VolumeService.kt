package com.parentcontrol.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class VolumeService : AccessibilityService() {

    private var volumeUpCount = 0
    private var volumeDownCount = 0
    private var lastPressTime = 0L
    private val COMBO_TIMEOUT = 4000L
    private val REQUIRED_PRESSES = 3

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        serviceInfo = info
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false

        val now = System.currentTimeMillis()

        if (now - lastPressTime > COMBO_TIMEOUT) {
            volumeUpCount = 0
            volumeDownCount = 0
        }

        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                volumeUpCount++
                lastPressTime = now
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                volumeDownCount++
                lastPressTime = now
            }
        }

        if (volumeUpCount >= REQUIRED_PRESSES && volumeDownCount >= REQUIRED_PRESSES) {
            volumeUpCount = 0
            volumeDownCount = 0
            openApp()
        }

        return false // Volume normally kaam karta rahe
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
