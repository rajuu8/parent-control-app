package com.parentcontrol.app

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class AdminReceiver : DeviceAdminReceiver() {
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "PIN enter karo uninstall karne ke liye"
    }

    override fun onDisabled(context: Context, intent: Intent) {
        // Device admin disabled — PIN sahi tha
    }
}
