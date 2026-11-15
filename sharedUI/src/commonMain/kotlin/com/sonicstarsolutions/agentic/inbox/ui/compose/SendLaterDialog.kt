package com.sonicstarsolutions.agentic.inbox.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sonicstarsolutions.agentic.inbox.util.ScheduleTimeFormatter
import com.sonicstarsolutions.agentic.inbox.util.SendLater
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Instant

private enum class Step { PRESETS, DATE, TIME }

/**
 * "Send later": quick picks (each showing when it lands) and a "Pick date & time" route through
 * the platform date and time pickers. A time the server would refuse is reported in the dialog
 * instead of closing it, so the user fixes it in place.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendLaterDialog(
    onDismiss: () -> Unit,
    onPick: (Instant) -> Unit,
    now: Instant = Clock.System.now(),
    zone: TimeZone = TimeZone.currentSystemDefault(),
) {
    var step by remember { mutableStateOf(Step.PRESETS) }
    var pickedDay by remember { mutableStateOf<Long?>(null) }
    var problem by remember { mutableStateOf<String?>(null) }

    fun choose(at: Instant) {
        val issue = SendLater.problemWith(at, now)
        if (issue == null) onPick(at) else problem = issue.message
    }

    when (step) {
        Step.PRESETS -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Send later") },
            text = {
                Column {
                    SendLater.presets(now, zone).forEach { option ->
                        PresetRow(
                            label = option.label,
                            detail = ScheduleTimeFormatter.format(option.at.toString(), now, zone),
                            onClick = { choose(option.at) },
                        )
                    }
                    PresetRow(label = "Pick date & time…", detail = null, onClick = { step = Step.DATE })
                    problem?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        )

        Step.DATE -> {
            val state = rememberDatePickerState(initialSelectedDateMillis = now.toEpochMilliseconds())
            DatePickerDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                    TextButton(
                        enabled = state.selectedDateMillis != null,
                        onClick = { pickedDay = state.selectedDateMillis; step = Step.TIME },
                    ) { Text("Next") }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
            ) { DatePicker(state = state) }
        }

        Step.TIME -> {
            val state = rememberTimePickerState(initialHour = 9, initialMinute = 0, is24Hour = false)
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Time") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        TimePicker(state = state)
                        problem?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { pickedDay?.let { choose(SendLater.combine(it, state.hour, state.minute, zone)) } },
                    ) { Text("Schedule") }
                },
                dismissButton = { TextButton(onClick = { step = Step.DATE }) { Text("Back") } },
            )
        }
    }
}

/** A full-width row — the whole row taps, and it is never shorter than 48dp. */
@Composable
private fun PresetRow(label: String, detail: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        if (detail != null) {
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
