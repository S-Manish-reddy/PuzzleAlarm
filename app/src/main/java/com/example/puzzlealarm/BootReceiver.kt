package com.example.puzzlealarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val alarms = AlarmStorage.load(context)
            alarms.filter { it.isEnabled }.forEach {
                AlarmScheduler.schedule(context, it)
            }
        }
    }
}
