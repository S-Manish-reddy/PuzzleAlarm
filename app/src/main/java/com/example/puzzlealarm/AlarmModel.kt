package com.example.puzzlealarm

data class AlarmModel(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val repeatDays: Set<Int>, // 1=Mon ... 7=Sun
    val isEnabled: Boolean = true,
    val skipNext: Boolean = false
)
