package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.model.ConnectionMode
import com.example.ui.theme.LoRaAmber
import com.example.ui.theme.LoRaAmberLight
import com.example.ui.theme.LoRaBorder
import com.example.ui.theme.LoRaCyan
import com.example.ui.theme.LoRaDarkBg
import com.example.ui.theme.LoRaGreen
import com.example.ui.theme.LoRaOrange
import com.example.ui.theme.LoRaSurface
import com.example.ui.theme.LoRaSurfaceVariant
import com.example.ui.theme.LoRaTextMuted
import com.example.ui.theme.LoRaTextPrimary
import com.example.ui.theme.LoRaTextSecondary

@Composable
fun OnboardingScreen(
    initialCallsign: String,
    initialMode: ConnectionMode,
    onComplete: (callsign: String, mode: ConnectionMode) -> Unit
) {
    var callsignInput by remember { mutableStateOf(initialCallsign.ifEmpty { "NODE-01" }) }
    var selectedMode by remember { mutableStateOf(initialMode) }
    var isError by remember { mutableStateOf(false) }

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
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(LoRaOrange.copy(alpha = 0.15f))
                        .border(2.dp, LoRaOrange, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CellTower,
                        contentDescription = "LoRa Radio Antenna",
                        tint = LoRaOrange,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "LORA OFF-GRID",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    color = LoRaTextPrimary
                )

                Text(
                    text = "ESP32 + SX1278 433MHz Mesh Node",
                    fontSize = 13.sp,
                    color = LoRaCyan,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Text(
                    text = "Zero internet or SIM required. Decentralized mesh radio communicator.",
                    fontSize = 13.sp,
                    color = LoRaTextSecondary,
                    modifier = Modifier.padding(top = 8.dp, start = 12.dp, end = 12.dp),
                    lineHeight = 18.sp
                )
            }

            // Configuration Form
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            ) {
                // Callsign Input
                Text(
                    text = "STEP 1: ASSIGN NODE CALLSIGN",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = LoRaAmber,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = callsignInput,
                    onValueChange = {
                        val cleaned = it.uppercase().filter { ch -> ch.isLetterOrDigit() || ch == '-' || ch == '_' }
                        if (cleaned.length <= 15) {
                            callsignInput = cleaned
                            isError = cleaned.isBlank()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("callsign_input"),
                    placeholder = { Text("e.g. ALPHA-01, SCOUT, HQ", color = LoRaTextMuted) },
                    singleLine = true,
                    isError = isError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = LoRaSurface,
                        unfocusedContainerColor = LoRaSurface,
                        focusedBorderColor = LoRaOrange,
                        unfocusedBorderColor = LoRaBorder,
                        focusedTextColor = LoRaTextPrimary,
                        unfocusedTextColor = LoRaTextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Radio, contentDescription = null, tint = LoRaOrange)
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Connection Mode Selector
                Text(
                    text = "STEP 2: SELECT ESP32 LINK MODE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = LoRaAmber,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                ConnectionModeCard(
                    mode = ConnectionMode.WIFI_WEBSOCKET,
                    title = "Wi-Fi Mode (SoftAP)",
                    subtitle = "ESP32 creates Access Point (ws://192.168.4.1/ws)",
                    icon = Icons.Default.Wifi,
                    isSelected = selectedMode == ConnectionMode.WIFI_WEBSOCKET,
                    badgeText = "Fast / Low Latency",
                    onClick = { selectedMode = ConnectionMode.WIFI_WEBSOCKET }
                )

                Spacer(modifier = Modifier.height(10.dp))

                ConnectionModeCard(
                    mode = ConnectionMode.BLUETOOTH_SPP,
                    title = "Bluetooth Mode (SPP)",
                    subtitle = "Pairs with ESP32 Bluetooth Serial (RFCOMM)",
                    icon = Icons.Default.Bluetooth,
                    isSelected = selectedMode == ConnectionMode.BLUETOOTH_SPP,
                    badgeText = "Ultra Low Power",
                    onClick = { selectedMode = ConnectionMode.BLUETOOTH_SPP }
                )

                Spacer(modifier = Modifier.height(10.dp))

                ConnectionModeCard(
                    mode = ConnectionMode.DEMO_SIMULATION,
                    title = "Simulation Mode (Virtual)",
                    subtitle = "Test mesh protocol & echo responder without hardware",
                    icon = Icons.Default.Sensors,
                    isSelected = selectedMode == ConnectionMode.DEMO_SIMULATION,
                    badgeText = "Hardware Free",
                    onClick = { selectedMode = ConnectionMode.DEMO_SIMULATION }
                )
            }

            // Launch Button
            Button(
                onClick = {
                    if (callsignInput.isBlank()) {
                        isError = true
                    } else {
                        onComplete(callsignInput.trim().uppercase(), selectedMode)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("initialize_node_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LoRaOrange,
                    contentColor = Color.Black
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "INITIALIZE OFF-GRID NODE",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectionModeCard(
    mode: ConnectionMode,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    badgeText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) LoRaOrange else LoRaBorder,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) LoRaSurfaceVariant else LoRaSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) LoRaOrange.copy(alpha = 0.2f) else LoRaDarkBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) LoRaOrange else LoRaCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LoRaTextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(LoRaBorder)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 10.sp,
                                color = LoRaAmberLight,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = LoRaTextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = LoRaGreen,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
