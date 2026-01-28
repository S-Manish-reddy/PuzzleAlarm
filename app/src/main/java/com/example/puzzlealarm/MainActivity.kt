package com.example.puzzlealarm

import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.puzzlealarm.ui.theme.PuzzleAlarmTheme
import java.util.Calendar

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Exact alarm permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(
                    Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                )
            }
        }

        setContent {
            PuzzleAlarmTheme {

                val context = LocalContext.current

                var showDialog by remember { mutableStateOf(false) }
                var editingAlarm by remember { mutableStateOf<AlarmModel?>(null) }

                val alarms = remember {
                    mutableStateListOf<AlarmModel>().apply {
                        addAll(AlarmStorage.load(context))
                    }
                }

                MainScreen(
                    alarms = alarms,
                    onAddAlarm = {
                        editingAlarm = null
                        showDialog = true
                    },
                    onEdit = { alarm ->
                        editingAlarm = alarm
                        showDialog = true
                    },
                    onToggle = { alarm ->
                        val i = alarms.indexOf(alarm)
                        if (i != -1) {
                            val updated = alarm.copy(isEnabled = !alarm.isEnabled)
                            alarms[i] = updated
                            AlarmStorage.save(context, alarms)

                            if (updated.isEnabled) {
                                AlarmScheduler.schedule(context, updated)
                            } else {
                                AlarmScheduler.cancel(context, updated)
                            }
                        }
                    },
                    onSkipNext = { alarm ->
                        val i = alarms.indexOf(alarm)
                        if (i != -1) {
                            alarms[i] = alarm.copy(skipNext = true)
                            AlarmStorage.save(context, alarms)
                        }
                    },
                    onDelete = { alarm ->
                        AlarmScheduler.cancel(context, alarm)
                        alarms.remove(alarm)
                        AlarmStorage.save(context, alarms)
                    }
                )

                if (showDialog) {
                    AddAlarmDialog(
                        initialAlarm = editingAlarm,
                        onDismiss = {
                            showDialog = false
                            editingAlarm = null
                        },
                        onConfirm = { h, m, days ->
                            if (editingAlarm != null) {
                                val index = alarms.indexOf(editingAlarm!!)
                                if (index != -1) {
                                    AlarmScheduler.cancel(context, editingAlarm!!)
                                    val updated = editingAlarm!!.copy(
                                        hour = h,
                                        minute = m,
                                        repeatDays = days
                                    )
                                    alarms[index] = updated
                                    AlarmStorage.save(context, alarms)
                                    AlarmScheduler.schedule(context, updated)
                                }
                            } else {
                                val newAlarm = AlarmModel(
                                    id = alarms.maxOfOrNull { it.id }?.plus(1) ?: 1,
                                    hour = h,
                                    minute = m,
                                    repeatDays = days,
                                    isEnabled = true,
                                    skipNext = false
                                )
                                alarms.add(newAlarm)
                                AlarmStorage.save(context, alarms)
                                AlarmScheduler.schedule(context, newAlarm)
                            }

                            showDialog = false
                            editingAlarm = null
                        }
                    )
                }
            }
        }
    }
}

/* ========================= MAIN SCREEN ========================= */

@Composable
fun MainScreen(
    alarms: List<AlarmModel>,
    onAddAlarm: () -> Unit,
    onEdit: (AlarmModel) -> Unit,
    onToggle: (AlarmModel) -> Unit,
    onSkipNext: (AlarmModel) -> Unit,
    onDelete: (AlarmModel) -> Unit
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddAlarm,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("+", fontSize = 26.sp)
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {

            Spacer(modifier = Modifier.height(150.dp))

            Text(
                text = getNextAlarmText(alarms),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(30.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        onEdit = { onEdit(alarm) },
                        onToggle = { onToggle(alarm) },
                        onSkipNext = { onSkipNext(alarm) },
                        onDelete = { onDelete(alarm) }
                    )
                }
            }
        }
    }
}

/* ========================= ADD / EDIT DIALOG ========================= */

@Composable
fun AddAlarmDialog(
    initialAlarm: AlarmModel?,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Set<Int>) -> Unit
) {
    val context = LocalContext.current

    var hour by remember { mutableStateOf(initialAlarm?.hour ?: 7) }
    var minute by remember { mutableStateOf(initialAlarm?.minute ?: 0) }
    var repeatDays by remember { mutableStateOf(initialAlarm?.repeatDays ?: emptySet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialAlarm == null) "Add Alarm" else "Edit Alarm") },
        confirmButton = {
            TextButton(onClick = { onConfirm(hour, minute, repeatDays) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        String.format("%02d:%02d", hour, minute),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Button(onClick = {
                        TimePickerDialog(
                            context,
                            { _, h, m ->
                                hour = h
                                minute = m
                            },
                            hour,
                            minute,
                            false
                        ).show()
                    }) {
                        Text("Change")
                    }
                }

                RepeatDaysSelector(
                    selectedDays = repeatDays,
                    onDayToggle = { day ->
                        repeatDays =
                            if (repeatDays.contains(day))
                                repeatDays - day
                            else
                                repeatDays + day
                    }
                )
            }
        }
    )
}

/* ========================= ALARM CARD ========================= */

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlarmCard(
    alarm: AlarmModel,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onSkipNext: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onEdit,
                onLongClick = onDelete
            ),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    String.format("%02d:%02d", alarm.hour, alarm.minute),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Medium
                )

                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { onToggle() }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            DaysRow(alarm.repeatDays)

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (alarm.skipNext) "Skipped for next alarm" else "Skip next",
                fontSize = 13.sp,
                color =
                    if (alarm.skipNext)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(enabled = !alarm.skipNext) {
                    onSkipNext()
                }
            )
        }
    }
}

/* ========================= DAYS ========================= */

@Composable
fun RepeatDaysSelector(
    selectedDays: Set<Int>,
    onDayToggle: (Int) -> Unit
) {
    val days = listOf("S", "M", "T", "W", "T", "F", "S")

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        days.forEachIndexed { index, label ->
            val selected = selectedDays.contains(index + 1)
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color =
                    if (selected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { onDayToggle(index + 1) }
            )
        }
    }
}

@Composable
fun DaysRow(repeatDays: Set<Int>) {
    val days = listOf("S", "M", "T", "W", "T", "F", "S")

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        days.forEachIndexed { index, label ->
            val selected = repeatDays.contains(index + 1)
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color =
                    if (selected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/* ========================= NEXT ALARM TEXT ========================= */

fun getNextAlarmText(alarms: List<AlarmModel>): String {
    val enabled = alarms.filter { it.isEnabled }
    if (enabled.isEmpty()) return "No upcoming alarms"

    val now = Calendar.getInstance()

    val next = enabled.minByOrNull { it.hour * 60 + it.minute } ?: return "No upcoming alarms"

    val alarmTime = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, next.hour)
        set(Calendar.MINUTE, next.minute)
        set(Calendar.SECOND, 0)
        if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
    }

    val diff = alarmTime.timeInMillis - now.timeInMillis
    val hours = diff / (1000 * 60 * 60)
    val minutes = (diff / (1000 * 60)) % 60

    return "Next alarm in ${hours}h ${minutes}m"
}
