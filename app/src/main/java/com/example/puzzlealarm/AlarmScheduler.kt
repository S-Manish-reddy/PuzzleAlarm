package com.example.puzzlealarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object AlarmScheduler {

    fun schedule(context: Context, alarm: AlarmModel) {
        if (!alarm.isEnabled) return

        val triggerTime = computeNextTriggerTime(alarm) ?: return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) return
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }

    fun cancel(context: Context, alarm: AlarmModel) {
        val intent = Intent(context, AlarmReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager.cancel(pendingIntent)
    }

    private fun computeNextTriggerTime(alarm: AlarmModel): Long? {
        val now = Calendar.getInstance()

        for (i in 0..6) {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, i)
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
            }

            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val mappedDay =
                if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1

            if (
                alarm.repeatDays.isEmpty() ||
                alarm.repeatDays.contains(mappedDay)
            ) {
                if (cal.timeInMillis > now.timeInMillis) {
                    return cal.timeInMillis
                }
            }
        }
        return null
    }
}
