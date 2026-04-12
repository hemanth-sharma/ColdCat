package com.example.coldcat.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coldcat.data.BlockSchedule
import com.example.coldcat.ui.MainViewModel
import com.example.coldcat.ui.components.*
import com.example.coldcat.ui.theme.*
import com.example.coldcat.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(viewModel: MainViewModel) {
    val schedules by viewModel.schedules.collectAsState()
    val isBlockActive by viewModel.isBlockActive.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CatBlack)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Block Schedule",
            color = CatWhite,
            fontWeight = FontWeight.Black,
            fontSize = 26.sp,
            letterSpacing = (-0.5).sp
        )

        // Lock banner
        if (isBlockActive) {
            ActiveBlockBanner()
        }

        // Info card
        InfoCard()

        // Add button
        Button(
            onClick = { showAddDialog = true },
            enabled = !isBlockActive,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CatRed,
                disabledContainerColor = CatGrayDim
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Schedule", fontWeight = FontWeight.Bold)
        }

        // Schedule list
        SectionHeader(title = "Schedules", count = schedules.size)

        if (schedules.isEmpty()) {
            EmptyState(
                emoji = "⏰",
                message = "No schedules set",
                subtitle = "Add a time range to automatically start blocking apps and websites."
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                schedules.forEach { schedule ->
                    ScheduleCard(
                        schedule = schedule,
                        onToggle = { viewModel.toggleSchedule(schedule) },
                        onDelete = { viewModel.deleteSchedule(schedule) },
                        locked = isBlockActive
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    // Add schedule dialog
    if (showAddDialog) {
        AddScheduleDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { startMin, endMin ->
                viewModel.addSchedule(startMin, endMin)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun InfoCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CatCard)
            .border(1.dp, CatBorder, RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = "💡", fontSize = 18.sp)
        Column {
            Text(
                text = "1-Hour Buffer Rule",
                color = CatWhite,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Text(
                text = "Schedules cannot span 24 hours. A minimum 1-hour window is kept open so you can always adjust your settings.",
                color = CatGray,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun ScheduleCard(
    schedule: BlockSchedule,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    locked: Boolean
) {
    val isNowActive = TimeUtils.isWithinSchedule(schedule)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                when {
                    isNowActive -> CatRedDim.copy(alpha = 0.6f)
                    !schedule.isEnabled -> CatCard.copy(alpha = 0.5f)
                    else -> CatCard
                }
            )
            .border(
                1.dp,
                when {
                    isNowActive -> CatRed.copy(alpha = 0.5f)
                    else -> CatBorder
                },
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Status dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isNowActive -> CatRed
                                    schedule.isEnabled -> CatGreen
                                    else -> CatGrayDim
                                }
                            )
                    )
                    Text(
                        text = TimeUtils.formatSchedule(schedule),
                        color = CatWhite,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp
                    )
                }

                if (isNowActive) {
                    Text(
                        text = "ACTIVE",
                        color = CatRed,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Duration info
            val startStr = TimeUtils.minuteToTimeString(schedule.startMinute)
            val endStr = TimeUtils.minuteToTimeString(schedule.endMinute)
            val durationMins = (schedule.endMinute - schedule.startMinute + 1440) % 1440
            val durationText = if (durationMins >= 60) {
                "${durationMins / 60}h ${durationMins % 60}m"
            } else {
                "${durationMins}m"
            }

            Text(
                text = "Duration: $durationText  •  $startStr to $endStr",
                color = CatGray,
                fontSize = 12.sp
            )

            if (!locked) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Toggle enabled/disabled
                    OutlinedButton(
                        onClick = onToggle,
                        modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(CatBorder)
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (schedule.isEnabled) "Disable" else "Enable",
                            color = CatGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Delete
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(CatRed.copy(alpha = 0.4f))
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Delete",
                            color = CatRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = CatGrayDim, modifier = Modifier.size(12.dp))
                    Text(text = "Locked during active block", color = CatGrayDim, fontSize = 11.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddScheduleDialog(
    onDismiss: () -> Unit,
    onConfirm: (startMinute: Int, endMinute: Int) -> Unit
) {
    var startHour by remember { mutableStateOf(9) }
    var startMinute by remember { mutableStateOf(0) }
    var endHour by remember { mutableStateOf(17) }
    var endMinute by remember { mutableStateOf(0) }
    var bufferWarning by remember { mutableStateOf(false) }

    val startMin = startHour * 60 + startMinute
    val endMin = endHour * 60 + endMinute
    val adjustedEnd = TimeUtils.applyBufferRule(startMin, endMin)
    bufferWarning = adjustedEnd != endMin

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CatDarkSurface,
        title = {
            Text("Add Schedule", color = CatWhite, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Set the time range when apps and websites will be blocked.",
                    color = CatGray,
                    fontSize = 13.sp
                )

                // Start time picker
                TimePickerRow(
                    label = "Start Time",
                    hour = startHour,
                    minute = startMinute,
                    onHourChange = { startHour = it },
                    onMinuteChange = { startMinute = it }
                )

                // End time picker
                TimePickerRow(
                    label = "End Time",
                    hour = endHour,
                    minute = endMinute,
                    onHourChange = { endHour = it },
                    onMinuteChange = { endMinute = it }
                )

                // Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CatCard)
                        .border(1.dp, CatBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "${TimeUtils.minuteToTimeString(startMin)} → ${TimeUtils.minuteToTimeString(adjustedEnd)}",
                            color = CatWhite,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (bufferWarning) {
                            Text(
                                text = "⚠️ End time adjusted (1-hour buffer rule applies)",
                                color = CatOrange,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(startMin, endMin) },
                colors = ButtonDefaults.buttonColors(containerColor = CatRed)
            ) {
                Text("Add", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = CatGray)
            }
        }
    )
}

@Composable
private fun TimePickerRow(
    label: String,
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, color = CatGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hour picker
            NumberPickerField(
                value = hour,
                range = 0..23,
                label = "HH",
                modifier = Modifier.weight(1f),
                onChange = onHourChange
            )
            Text(":", color = CatWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            // Minute picker (steps of 5)
            NumberPickerField(
                value = minute,
                range = 0..55,
                step = 5,
                label = "MM",
                modifier = Modifier.weight(1f),
                onChange = onMinuteChange
            )
        }
    }
}

@Composable
private fun NumberPickerField(
    value: Int,
    range: IntRange,
    step: Int = 1,
    label: String,
    modifier: Modifier = Modifier,
    onChange: (Int) -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CatCard)
            .border(1.dp, CatBorder, RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                val prev = if (value - step < range.first) range.last - (range.last % step) else value - step
                onChange(prev)
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = CatGray, modifier = Modifier.size(18.dp))
        }
        Text(
            text = "%02d".format(value),
            color = CatWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = {
                val next = if (value + step > range.last) range.first else value + step
                onChange(next)
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = CatGray, modifier = Modifier.size(18.dp))
        }
    }
}