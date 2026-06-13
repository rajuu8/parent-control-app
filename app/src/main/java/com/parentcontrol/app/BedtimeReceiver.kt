package com.parentcontrol.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import java.util.Calendar

class BedtimeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getStringExtra("bedtime_action")
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, AdminReceiver::class.java)

        when (action) {
            "lock" -> {
                if (dpm.isAdminActive(adminComponent)) {
                    dpm.lockNow()
                }
            }
        }
    }

    companion object {
        fun scheduleBedtime(context: Context, lockHour: Int, lockMinute: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val lockIntent = Intent(context, BedtimeReceiver::class.java).apply {
                putExtra("bedtime_action", "lock")
            }
            val lockPending = PendingIntent.getBroadcast(
                context, 100, lockIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val lockCalendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, lockHour)
                set(Calendar.MINUTE, lockMinute)
                set(Calendar.SECOND, 0)
                if (timeInMillis < System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                lockCalendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                lockPending
            )
        }

        fun cancelBedtime(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, BedtimeReceiver::class.java)
            val pending = PendingIntent.getBroadcast(
                context, 100, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pending)
        }
    }
}
