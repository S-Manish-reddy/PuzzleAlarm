package com.example.puzzlealarm

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import com.example.puzzlealarm.ui.theme.PuzzleAlarmTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.media.AudioManager


class PuzzleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        // ✅ Modern Android (API 27+) recommended calls
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        enableEdgeToEdge()
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Intentionally empty:
                    // User must solve the puzzle to stop alarm
                }
            }
        )
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        volumeControlStream = AudioManager.STREAM_ALARM

        setContent {
            PuzzleAlarmTheme {
                PuzzleScreen(
                    onPuzzleSolved = {
                        stopAlarm()
                        finish()
                    }
                )
            }
        }
    }

    private fun stopAlarm() {
        // Stop the foreground alarm service
        stopService(Intent(this, AlarmService::class.java))

        // Clear saved alarm so it won't re-trigger after reboot
        val prefs = getSharedPreferences("alarm_prefs", MODE_PRIVATE)
        prefs.edit().remove("alarm_time").apply()
    }

}

