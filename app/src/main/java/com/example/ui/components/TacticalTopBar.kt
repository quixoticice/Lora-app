package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataArray
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionMode
import com.example.model.ConnectionStatus
import com.example.ui.theme.LoRaAmber
import com.example.ui.theme.LoRaBorder
import com.example.ui.theme.LoRaCyan
import com.example.ui.theme.LoRaGreen
import com.example.ui.theme.LoRaOrange
import com.example.ui.theme.LoRaRed
import com.example.ui.theme.LoRaSurface
import com.example.ui.theme.LoRaSurfaceVariant
import com.example.ui.theme.LoRaTextMuted
import com.example.ui.theme.LoRaTextPrimary
import com.example.ui.theme.LoRaTextSecondary

@Composable
fun TacticalTopBar(
    callsign: String,
    mode: ConnectionMode,
    status: ConnectionStatus,
    onOpenNodes: () -> Unit,
    onOpenFirmware: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenSettings: () -> Unit,
    onReconnect: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LoRaSurface,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Main Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Callsign & Status LED
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    StatusLed(status = status)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = callsign,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = LoRaTextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            ModePill(mode = mode)
                        }
                        StatusText(status = status)
                    }
                }

                // Action Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenNodes,
                        modifier = Modifier.size(38.dp).testTag("open_nodes_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = "Discovered Nodes",
                            tint = LoRaCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenLogs,
                        modifier = Modifier.size(38.dp).testTag("open_logs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DataArray,
                            contentDescription = "Packet Logs",
                            tint = LoRaAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenFirmware,
                        modifier = Modifier.size(38.dp).testTag("open_firmware_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "ESP32 Firmware",
                            tint = LoRaTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(38.dp).testTag("open_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Radio Settings",
                            tint = LoRaTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Secondary RF Telemetry Strip
            if (status is ConnectionStatus.Connected) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(LoRaSurfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "433.00 MHz | SF7 BW125",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = LoRaTextMuted
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "RSSI: ${status.rssi} dBm",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = LoRaGreen
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "TX: ${status.txCount} | RX: ${status.rxCount}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = LoRaCyan
                        )
                    }
                }
            } else if (status is ConnectionStatus.Error || status is ConnectionStatus.Disconnected) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(LoRaSurfaceVariant)
                        .clickable { onReconnect() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (status is ConnectionStatus.Error) status.message else "Link offline. Tap to reconnect.",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = LoRaRed,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reconnect",
                        tint = LoRaOrange,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusLed(status: ConnectionStatus) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "led_pulse"
    )

    val (color, isPulsing) = when (status) {
        is ConnectionStatus.Connected -> Pair(LoRaGreen, true)
        is ConnectionStatus.Connecting -> Pair(LoRaAmber, true)
        is ConnectionStatus.Disconnected -> Pair(LoRaBorder, false)
        is ConnectionStatus.Error -> Pair(LoRaRed, false)
    }

    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = if (isPulsing) alpha else 1f))
            .border(2.dp, color.copy(alpha = 0.5f), CircleShape)
    )
}

@Composable
fun ModePill(mode: ConnectionMode) {
    val (label, icon) = when (mode) {
        ConnectionMode.WIFI_WEBSOCKET -> Pair("WI-FI", Icons.Default.Wifi)
        ConnectionMode.BLUETOOTH_SPP -> Pair("BT SPP", Icons.Default.Bluetooth)
        ConnectionMode.DEMO_SIMULATION -> Pair("SIM", Icons.Default.Sensors)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(LoRaBorder)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = LoRaCyan, modifier = Modifier.size(10.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = LoRaTextPrimary
        )
    }
}

@Composable
fun StatusText(status: ConnectionStatus) {
    val text = when (status) {
        is ConnectionStatus.Connected -> "ONLINE - ${status.endpoint}"
        is ConnectionStatus.Connecting -> "CONNECTING (${status.target})"
        is ConnectionStatus.Disconnected -> "STANDBY (DISCONNECTED)"
        is ConnectionStatus.Error -> "DISCONNECTED"
    }

    val color = when (status) {
        is ConnectionStatus.Connected -> LoRaGreen
        is ConnectionStatus.Connecting -> LoRaAmber
        is ConnectionStatus.Disconnected -> LoRaTextMuted
        is ConnectionStatus.Error -> LoRaRed
    }

    Text(
        text = text,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        color = color,
        maxLines = 1
    )
}
