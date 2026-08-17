package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ConnectionMode
import com.example.model.RadioConfig
import com.example.service.LoRaConnectionManager
import com.example.ui.theme.LoRaAmber
import com.example.ui.theme.LoRaBorder
import com.example.ui.theme.LoRaCyan
import com.example.ui.theme.LoRaDarkBg
import com.example.ui.theme.LoRaGreen
import com.example.ui.theme.LoRaOrange
import com.example.ui.theme.LoRaRed
import com.example.ui.theme.LoRaSurface
import com.example.ui.theme.LoRaSurfaceVariant
import com.example.ui.theme.LoRaTextMuted
import com.example.ui.theme.LoRaTextPrimary
import com.example.ui.theme.LoRaTextSecondary

@Composable
fun ConnectionModal(
    currentCallsign: String,
    currentMode: ConnectionMode,
    currentConfig: RadioConfig,
    connectionManager: LoRaConnectionManager,
    onSaveCallsign: (String) -> Unit,
    onSaveMode: (ConnectionMode) -> Unit,
    onSaveConfig: (RadioConfig) -> Unit,
    onClearChatHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    var callsignState by remember { mutableStateOf(currentCallsign) }
    var modeState by remember { mutableStateOf(currentMode) }
    var wsUrlState by remember { mutableStateOf(currentConfig.wsUrl) }
    var freqState by remember { mutableStateOf(currentConfig.frequencyMHz.toString()) }
    var sfState by remember { mutableStateOf(currentConfig.spreadingFactor.toString()) }
    var bwState by remember { mutableStateOf(currentConfig.bandwidthKHz.toString()) }
    var powerState by remember { mutableStateOf(currentConfig.txPowerDbm.toString()) }
    var selectedBtAddress by remember { mutableStateOf(currentConfig.btDeviceAddress) }
    var selectedBtName by remember { mutableStateOf(currentConfig.btDeviceName) }

    val pairedDevices = remember { connectionManager.getPairedBluetoothDevices() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, LoRaBorder, RoundedCornerShape(20.dp)),
            color = LoRaSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(LoRaOrange.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = LoRaOrange, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "RADIO & NODE SETTINGS",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = LoRaTextPrimary
                            )
                            Text(
                                text = "ESP32 Link & LoRa Hardware Config",
                                fontSize = 12.sp,
                                color = LoRaTextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_settings_modal")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = LoRaTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable settings body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Node Callsign Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = LoRaSurfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "NODE CALLSIGN",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = LoRaAmber
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = callsignState,
                                onValueChange = {
                                    val clean = it.uppercase().filter { ch -> ch.isLetterOrDigit() || ch == '-' }
                                    if (clean.length <= 15) callsignState = clean
                                },
                                modifier = Modifier.fillMaxWidth().testTag("settings_callsign_input"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = LoRaDarkBg,
                                    unfocusedContainerColor = LoRaDarkBg,
                                    focusedBorderColor = LoRaOrange,
                                    unfocusedBorderColor = LoRaBorder,
                                    focusedTextColor = LoRaTextPrimary,
                                    unfocusedTextColor = LoRaTextPrimary
                                )
                            )
                        }
                    }

                    // Connection Mode Selector
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = LoRaSurfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "ESP32 CONNECTION INTERFACE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = LoRaAmber
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                ModeButton(
                                    title = "Wi-Fi (WS)",
                                    icon = Icons.Default.Wifi,
                                    isSelected = modeState == ConnectionMode.WIFI_WEBSOCKET,
                                    modifier = Modifier.weight(1f),
                                    onClick = { modeState = ConnectionMode.WIFI_WEBSOCKET }
                                )
                                ModeButton(
                                    title = "BT SPP",
                                    icon = Icons.Default.Bluetooth,
                                    isSelected = modeState == ConnectionMode.BLUETOOTH_SPP,
                                    modifier = Modifier.weight(1f),
                                    onClick = { modeState = ConnectionMode.BLUETOOTH_SPP }
                                )
                                ModeButton(
                                    title = "Simulation",
                                    icon = Icons.Default.Sensors,
                                    isSelected = modeState == ConnectionMode.DEMO_SIMULATION,
                                    modifier = Modifier.weight(1f),
                                    onClick = { modeState = ConnectionMode.DEMO_SIMULATION }
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Mode specific settings
                            when (modeState) {
                                ConnectionMode.WIFI_WEBSOCKET -> {
                                    Text("WebSocket URL (Default: ws://192.168.4.1/ws)", fontSize = 11.sp, color = LoRaTextSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = wsUrlState,
                                        onValueChange = { wsUrlState = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = LoRaDarkBg,
                                            unfocusedContainerColor = LoRaDarkBg,
                                            focusedBorderColor = LoRaCyan,
                                            unfocusedBorderColor = LoRaBorder,
                                            focusedTextColor = LoRaTextPrimary,
                                            unfocusedTextColor = LoRaTextPrimary
                                        )
                                    )
                                }
                                ConnectionMode.BLUETOOTH_SPP -> {
                                    Text("Paired Bluetooth ESP32 Nodes", fontSize = 11.sp, color = LoRaTextSecondary)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    if (pairedDevices.isEmpty()) {
                                        Text(
                                            text = "No paired Bluetooth devices detected. Pair your ESP32 in Android Bluetooth Settings first.",
                                            fontSize = 12.sp,
                                            color = LoRaOrange
                                        )
                                    } else {
                                        pairedDevices.forEach { (name, addr) ->
                                            val isSelected = selectedBtAddress == addr
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) LoRaCyan.copy(alpha = 0.2f) else LoRaDarkBg)
                                                    .border(1.dp, if (isSelected) LoRaCyan else LoRaBorder, RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        selectedBtAddress = addr
                                                        selectedBtName = name
                                                    }
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text(text = name, fontWeight = FontWeight.Bold, color = LoRaTextPrimary, fontSize = 13.sp)
                                                    Text(text = addr, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = LoRaTextMuted)
                                                }
                                                if (isSelected) {
                                                    Text("SELECTED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LoRaCyan)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                        }
                                    }
                                }
                                ConnectionMode.DEMO_SIMULATION -> {
                                    Text(
                                        text = "Simulation active: Creates virtual nodes (BASE-STATION, ALPHA-RELAY) that automatically echo messages and transmit telemetry.",
                                        fontSize = 12.sp,
                                        color = LoRaGreen
                                    )
                                }
                            }
                        }
                    }

                    // Radio RF Parameters (SX1278 Ra-02)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = LoRaSurfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = LoRaCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "SX1278 RF PARAMETERS",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = LoRaCyan
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Frequency (MHz)", fontSize = 11.sp, color = LoRaTextSecondary)
                                    OutlinedTextField(
                                        value = freqState,
                                        onValueChange = { freqState = it },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = LoRaDarkBg,
                                            unfocusedContainerColor = LoRaDarkBg,
                                            focusedBorderColor = LoRaCyan,
                                            unfocusedBorderColor = LoRaBorder,
                                            focusedTextColor = LoRaTextPrimary,
                                            unfocusedTextColor = LoRaTextPrimary
                                        )
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Spreading Factor (SF)", fontSize = 11.sp, color = LoRaTextSecondary)
                                    OutlinedTextField(
                                        value = sfState,
                                        onValueChange = { sfState = it },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = LoRaDarkBg,
                                            unfocusedContainerColor = LoRaDarkBg,
                                            focusedBorderColor = LoRaCyan,
                                            unfocusedBorderColor = LoRaBorder,
                                            focusedTextColor = LoRaTextPrimary,
                                            unfocusedTextColor = LoRaTextPrimary
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Bandwidth (kHz)", fontSize = 11.sp, color = LoRaTextSecondary)
                                    OutlinedTextField(
                                        value = bwState,
                                        onValueChange = { bwState = it },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = LoRaDarkBg,
                                            unfocusedContainerColor = LoRaDarkBg,
                                            focusedBorderColor = LoRaCyan,
                                            unfocusedBorderColor = LoRaBorder,
                                            focusedTextColor = LoRaTextPrimary,
                                            unfocusedTextColor = LoRaTextPrimary
                                        )
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Power (dBm)", fontSize = 11.sp, color = LoRaTextSecondary)
                                    OutlinedTextField(
                                        value = powerState,
                                        onValueChange = { powerState = it },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = LoRaDarkBg,
                                            unfocusedContainerColor = LoRaDarkBg,
                                            focusedBorderColor = LoRaCyan,
                                            unfocusedBorderColor = LoRaBorder,
                                            focusedTextColor = LoRaTextPrimary,
                                            unfocusedTextColor = LoRaTextPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Reset / Clear Actions
                    Button(
                        onClick = {
                            onClearChatHistory()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LoRaSurfaceVariant, contentColor = LoRaRed)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear Local Chat & Packet History", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Save & Apply
                Button(
                    onClick = {
                        onSaveCallsign(callsignState)
                        onSaveMode(modeState)
                        onSaveConfig(
                            currentConfig.copy(
                                frequencyMHz = freqState.toDoubleOrNull() ?: 433.0,
                                spreadingFactor = sfState.toIntOrNull() ?: 7,
                                bandwidthKHz = bwState.toDoubleOrNull() ?: 125.0,
                                txPowerDbm = powerState.toIntOrNull() ?: 20,
                                wsUrl = wsUrlState,
                                btDeviceAddress = selectedBtAddress,
                                btDeviceName = selectedBtName
                            )
                        )
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_settings_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LoRaOrange, contentColor = Color.Black)
                ) {
                    Text("SAVE & RECONNECT NODE", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun ModeButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) LoRaOrange else LoRaDarkBg)
            .border(1.dp, if (isSelected) LoRaOrange else LoRaBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = null, tint = if (isSelected) Color.Black else LoRaCyan, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isSelected) Color.Black else LoRaTextPrimary
            )
        }
    }
}
