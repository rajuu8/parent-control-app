package com.parentcontrol.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class AppBlocker : AccessibilityService() {

    private val blockedApps = mutableSetOf<String>()

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            if (blockedApps.contains(packageName)) {
                // Home pe bhej do
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            }
        }
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        loadBlockedApps()
    }

    private fun loadBlockedApps() {
        val prefs = getSharedPreferences("blocked_apps", MODE_PRIVATE)
        val blocked = prefs.getStringSet("apps", emptySet()) ?: emptySet()
        blockedApps.clear()
        blockedApps.addAll(blocked)
    }

    companion object {
        fun blockApp(context: android.content.Context, packageName: String) {
            val prefs = context.getSharedPreferences("blocked_apps", android.content.Context.MODE_PRIVATE)
            val current = prefs.getStringSet("apps", mutableSetOf()) ?: mutableSetOf()
            current.add(packageName)
            prefs.edit().putStringSet("apps", current).apply()
        }

        fun unblockApp(context: android.content.Context, packageName: String) {
            val prefs = context.getSharedPreferences("blocked_apps", android.content.Context.MODE_PRIVATE)
            val current = prefs.getStringSet("apps", mutableSetOf()) ?: mutableSetOf()
            current.remove(packageName)
            prefs.edit().putStringSet("apps", current).apply()
        }
    }
}
