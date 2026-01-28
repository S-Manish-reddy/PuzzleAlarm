package com.example.puzzlealarm

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object AlarmStorage {

    private const val PREF_NAME = "alarms_prefs"
    private const val KEY_ALARMS = "alarms"

    fun save(context: Context, alarms: List<AlarmModel>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(alarms)
        prefs.edit().putString(KEY_ALARMS, json).apply()
    }

    fun load(context: Context): MutableList<AlarmModel> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_ALARMS, null) ?: return mutableListOf()

        val type = object : TypeToken<List<AlarmModel>>() {}.type
        return Gson().fromJson(json, type)
    }
}
