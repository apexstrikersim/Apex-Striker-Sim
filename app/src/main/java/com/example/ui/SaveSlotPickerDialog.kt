package com.example.ui

import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.SaveSlotManager
import com.example.data.SlotMetadata
import com.example.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SaveSlotPickerDialog(
    mode: String, // "NEW", "LOAD", "SAVE"
    saveSlotManager: SaveSlotManager,
    onSlotSelected: (Int) -> Unit,
    onDeleteSlot: (Int) -> Unit = {},
    onDismiss: () -> Unit
) {
    var overwriteConfirmSlot by remember { mutableStateOf<Int?>(null) }
    var deleteConfirmSlot by remember { mutableStateOf<Int?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SportsCardBg),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .border(1.dp, PitchGreen, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = PitchGreen
                        )
                        Column {
                            Text(
                                text = when (mode) {
                                    "NEW" -> "SELECT SAVE SLOT"
                                    "SAVE" -> "SELECT SLOT TO SAVE"
                                    else -> "SAVED CAREERS"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = PitchGreen
                            )
                            if (mode == "LOAD") {
                                Text(
                                    text = "Tap to load • Long-press to delete",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                HorizontalDivider(color = BorderColor)

                key(refreshKey) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(5) { index ->
                            val slotId = index + 1
                            val metadata = remember(slotId, refreshKey) { saveSlotManager.getSlotMetadata(slotId) }
                            val isEnabled = when (mode) {
                                "LOAD" -> metadata.hasData
                                else -> true
                            }

                            SlotCard(
                                slotId = slotId,
                                metadata = metadata,
                                isEnabled = isEnabled,
                                onClick = {
                                    if (mode == "NEW" && metadata.hasData) {
                                        overwriteConfirmSlot = slotId
                                    } else {
                                        onSlotSelected(slotId)
                                    }
                                },
                                onLongClick = if (metadata.hasData) {
                                    { deleteConfirmSlot = slotId }
                                } else null
                            )
                        }
                    }
                }
            }
        }
    }

    overwriteConfirmSlot?.let { slotId ->
        val metadata = saveSlotManager.getSlotMetadata(slotId)
        AlertDialog(
            onDismissRequest = { overwriteConfirmSlot = null },
            title = { Text("Overwrite Save Slot $slotId?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("This will overwrite ${metadata.playerName}'s save data. Are you sure you want to continue?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        val selected = slotId
                        overwriteConfirmSlot = null
                        onSlotSelected(selected)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MutedRed)
                ) {
                    Text("OVERWRITE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { overwriteConfirmSlot = null }) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            containerColor = DarkSlate
        )
    }

    deleteConfirmSlot?.let { slotId ->
        val metadata = saveSlotManager.getSlotMetadata(slotId)
        AlertDialog(
            onDismissRequest = { deleteConfirmSlot = null },
            icon = {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MutedRed)
            },
            title = { Text("Delete Slot $slotId?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("Are you sure you want to permanently delete ${metadata.playerName}'s career save? This cannot be undone.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSlot(slotId)
                        deleteConfirmSlot = null
                        refreshKey++
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MutedRed)
                ) {
                    Text("DELETE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmSlot = null }) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            containerColor = DarkSlate
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SlotCard(
    slotId: Int,
    metadata: SlotMetadata,
    isEnabled: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) DarkSlate else DarkSlate.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                enabled = isEnabled,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .border(
                width = 1.dp,
                color = if (metadata.hasData) TrophyGold.copy(alpha = 0.6f) else BorderColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "SLOT $slotId",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isEnabled) PitchGreen else TextSecondary.copy(alpha = 0.5f),
                        letterSpacing = 1.sp
                    )
                    if (metadata.hasData) {
                        Surface(
                            color = TrophyGold.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "GEN ${metadata.generation}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TrophyGold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (metadata.hasData) {
                    Text(
                        text = metadata.playerName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isEnabled) TextPrimary else TextSecondary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (metadata.clubName.isNotBlank()) metadata.clubName else "Free Agent",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "${metadata.ovr} OVR",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TrophyGold
                        )
                    }

                    if (metadata.lastPlayedTimestamp > 0L) {
                        val timeString = DateUtils.getRelativeTimeSpanString(
                            metadata.lastPlayedTimestamp,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS
                        ).toString()
                        Text(
                            text = "Last played: $timeString",
                            fontSize = 10.sp,
                            color = TextSecondary.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    Text(
                        text = "Empty Slot",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = TextSecondary.copy(alpha = 0.6f)
                    )
                }
            }

            if (!metadata.hasData) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SportsDarkBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Empty",
                        tint = TextSecondary.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
