package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ChatMessageEntity
import com.example.data.local.DiscoveredNodeEntity
import com.example.model.ConnectionMode
import com.example.model.ConnectionStatus
import com.example.model.GpsPayload
import com.example.ui.components.MessageItem
import com.example.ui.components.PushToTalkBar
import com.example.ui.components.TacticalTopBar
import com.example.ui.theme.LoRaAmber
import com.example.ui.theme.LoRaAmberLight
import com.example.ui.theme.LoRaBorder
import com.example.ui.theme.LoRaCyan
import com.example.ui.theme.LoRaDarkBg
import com.example.ui.theme.LoRaGreen
import com.example.ui.theme.LoRaOrange
import com.example.ui.theme.LoRaRed
import com.example.ui.theme.LoRaRedBright
import com.example.ui.theme.LoRaSurface
import com.example.ui.theme.LoRaSurfaceVariant
import com.example.ui.theme.LoRaTextMuted
import com.example.ui.theme.LoRaTextPrimary
import com.example.ui.theme.LoRaTextSecondary
import com.example.viewmodel.MainViewModel

@Composable
fun MainChatScreen(
    viewModel: MainViewModel,
    onOpenNodesModal: () -> Unit,
    onOpenFirmwareModal: () -> Unit,
    onOpenLogsModal: () -> Unit,
    onOpenSettingsModal: () -> Unit
) {
    val callsign by viewModel.callsign.collectAsStateWithLifecycle()
    val mode by viewModel.selectedMode.collectAsStateWithLifecycle()
    val status by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val selectedRecipient by viewModel.selectedRecipient.collectAsStateWithLifecycle()
    val messages by viewModel.currentMessages.collectAsStateWithLifecycle()
    val discoveredNodes by viewModel.discoveredNodes.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecordingAudio.collectAsStateWithLifecycle()
    val recordDurationSec by viewModel.recordDurationSec.collectAsStateWithLifecycle()
    val recordingAmplitude by viewModel.recordingAmplitude.collectAsStateWithLifecycle()
    val playingAudioId by viewModel.playingAudioId.collectAsStateWithLifecycle()
    val emergencyAlert by viewModel.emergencyAlert.collectAsStateWithLifecycle()

    var showSosConfirmDialog by remember { mutableStateOf(false) }
    var showGpsNoteDialog by remember { mutableStateOf(false) }
    var customGpsNote by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(LoRaDarkBg),
        color = LoRaDarkBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Tactical RF Header
            TacticalTopBar(
                callsign = callsign,
                mode = mode,
                status = status,
                onOpenNodes = onOpenNodesModal,
                onOpenFirmware = onOpenFirmwareModal,
                onOpenLogs = onOpenLogsModal,
                onOpenSettings = onOpenSettingsModal,
                onReconnect = { viewModel.reconnect() }
            )

            // Emergency Alert Banner
            emergencyAlert?.let { alert ->
                EmergencyDistressBanner(
                    alert = alert,
                    onDismiss = { viewModel.dismissEmergencyAlert() }
                )
            }

            // Channel & Node Switcher Bar
            ChannelSwitcherRow(
                selectedRecipient = selectedRecipient,
                discoveredNodes = discoveredNodes,
                onSelectRecipient = { viewModel.setRecipient(it) }
            )

            // Chat Messages Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty()) {
                    EmptyChatPlaceholder(
                        recipient = selectedRecipient,
                        callsign = callsign,
                        mode = mode
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            MessageItem(
                                message = msg,
                                playingAudioId = playingAudioId,
                                onPlayAudio = { id, base64 -> viewModel.playAudio(id, base64) }
                            )
                        }
                    }
                }
            }

            // Bottom Input & Push-To-Talk Controls
            PushToTalkBar(
                recipient = selectedRecipient,
                isRecording = isRecording,
                recordDurationSec = recordDurationSec,
                amplitude = recordingAmplitude,
                onSendText = { viewModel.sendTextMessage(it) },
                onSendGps = { showGpsNoteDialog = true },
                onSendSos = { showSosConfirmDialog = true },
                onStartRecording = { viewModel.startAudioRecording() },
                onStopRecordingAndSend = { viewModel.stopAudioRecordingAndSend() },
                onCancelRecording = { viewModel.cancelAudioRecording() }
            )
        }
    }

    // SOS Emergency Confirmation Dialog
    if (showSosConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSosConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = LoRaRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BROADCAST SOS ALERT?",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = LoRaRed
                    )
                }
            },
            text = {
                Text(
                    text = "This will transmit high-priority emergency distress packets with your live GPS coordinates across all LoRa frequencies and repeaters.",
                    color = LoRaTextPrimary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.sendSosEmergency("EMERGENCY DISTRESS BEACON ACTIVATED!")
                        showSosConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LoRaRed, contentColor = Color.White)
                ) {
                    Text("TRANSMIT SOS", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSosConfirmDialog = false }) {
                    Text("Cancel", color = LoRaTextSecondary)
                }
            },
            containerColor = LoRaSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // GPS Note Dialog
    if (showGpsNoteDialog) {
        AlertDialog(
            onDismissRequest = { showGpsNoteDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = LoRaGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SHARE GPS POSITION",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = LoRaGreen
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Transmit your exact coordinates to ${if (selectedRecipient == "BROADCAST") "all mesh nodes" else selectedRecipient}.",
                        color = LoRaTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customGpsNote,
                        onValueChange = { customGpsNote = it },
                        placeholder = { Text("Optional note (e.g. Camp 1, Checkpoint)", color = LoRaTextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = LoRaDarkBg,
                            unfocusedContainerColor = LoRaDarkBg,
                            focusedBorderColor = LoRaGreen,
                            unfocusedBorderColor = LoRaBorder,
                            focusedTextColor = LoRaTextPrimary,
                            unfocusedTextColor = LoRaTextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.sendGpsLocation(customGpsNote)
                        customGpsNote = ""
                        showGpsNoteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LoRaGreen, contentColor = Color.Black)
                ) {
                    Text("SEND GPS", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGpsNoteDialog = false }) {
                    Text("Cancel", color = LoRaTextSecondary)
                }
            },
            containerColor = LoRaSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun ChannelSwitcherRow(
    selectedRecipient: String,
    discoveredNodes: List<DiscoveredNodeEntity>,
    onSelectRecipient: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LoRaSurface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Broadcast Channel Chip
        val isBroadcast = selectedRecipient == "BROADCAST"
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isBroadcast) LoRaOrange else LoRaSurfaceVariant)
                .clickable { onSelectRecipient("BROADCAST") }
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("channel_broadcast_chip"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Forum,
                contentDescription = null,
                tint = if (isBroadcast) Color.Black else LoRaAmber,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "#BROADCAST",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isBroadcast) Color.Black else LoRaTextPrimary
            )
        }

        // Direct Node Chips
        discoveredNodes.forEach { node ->
            val isSelected = selectedRecipient.equals(node.callsign, ignoreCase = true)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) LoRaCyan else LoRaSurfaceVariant)
                    .clickable { onSelectRecipient(node.callsign) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = if (isSelected) Color.Black else LoRaCyan,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "@${node.callsign}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isSelected) Color.Black else LoRaTextPrimary
                )

                if (node.unreadCount > 0 && !isSelected) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(LoRaOrange),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${node.unreadCount}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmergencyDistressBanner(
    alert: ChatMessageEntity,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(LoRaRed)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "EMERGENCY SOS FROM ${alert.srcCallsign}!",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                    Text(
                        text = alert.payload.take(60),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 1
                    )
                }
            }

            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyChatPlaceholder(
    recipient: String,
    callsign: String,
    mode: ConnectionMode
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(LoRaSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CellTower,
                contentDescription = null,
                tint = LoRaCyan,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (recipient == "BROADCAST") "BROADCAST CHANNEL READY" else "DIRECT CHAT WITH $recipient",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = LoRaTextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your callsign: $callsign\nMode: ${mode.title}\nPackets will be transmitted over SX1278 433MHz LoRa airwaves.",
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = LoRaTextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(LoRaSurfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = LoRaGreen, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("One-Tap GPS", fontSize = 11.sp, color = LoRaTextPrimary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = LoRaCyan, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Hold PTT Voice", fontSize = 11.sp, color = LoRaTextPrimary)
            }
        }
    }
}
