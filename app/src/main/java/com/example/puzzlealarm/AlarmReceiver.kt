package com.example.puzzlealarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        if (alarmId == -1) return

        val alarms = AlarmStorage.load(context)
        val alarm = alarms.find { it.id == alarmId } ?: return

        if (!alarm.isEnabled) return

        // Skip only once
        if (alarm.skipNext) {
            val index = alarms.indexOf(alarm)
            alarms[index] = alarm.copy(skipNext = false)
            AlarmStorage.save(context, alarms)

            // Schedule next occurrence
            AlarmScheduler.schedule(context, alarms[index])
            return
        }

        // 🔔 Start alarm sound
        val serviceIntent = Intent(context, AlarmService::class.java)
        context.startForegroundService(serviceIntent)

        // 🔁 Schedule next occurrence
        AlarmScheduler.schedule(context, alarm)
    }
}
