package com.parentcontrol.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock

class KeepAliveReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Service restart karo agar band ho gayi
        val serviceIntent = Intent(context, MonitoringService::class.java)
        context.startForegroundService(serviceIntent)

        // Token re-register karo
        FirebaseReceiver.registerTokenToServer()

        // Alarm dobara set karo
        scheduleAlarm(context)
    }

    companion object {
        fun scheduleAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, KeepAliveReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Har 5 minute mein check karo
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 5 * 60 * 1000,
                pendingIntent
            )
        }
    }
}
