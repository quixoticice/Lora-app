package com.example.ui

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataArray
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.PacketLogEntity
import com.example.ui.components.rememberTimeFormat
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
fun PacketLogModal(
    packetLogs: List<PacketLogEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
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
                                .background(LoRaAmber.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DataArray, contentDescription = null, tint = LoRaAmber, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "LORA PACKET INSPECTOR",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = LoRaTextPrimary
                            )
                            Text(
                                text = "Raw JSON Frames (${packetLogs.size} recent frames)",
                                fontSize = 12.sp,
                                color = LoRaTextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_logs_modal")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = LoRaTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (packetLogs.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No RF packets transmitted or received yet.",
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = LoRaTextMuted
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(packetLogs, key = { it.id }) { log ->
                            PacketLogItem(
                                log = log,
                                onCopy = {
                                    clipboardManager.setText(AnnotatedString(log.rawJson))
                                    Toast.makeText(context, "Packet JSON copied", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PacketLogItem(
    log: PacketLogEntity,
    onCopy: () -> Unit
) {
    val isTx = log.direction == "TX"
    val dirColor = if (isTx) LoRaOrange else LoRaGreen

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, LoRaBorder, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = LoRaDarkBg)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(dirColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = log.direction,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = dirColor
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${log.src} -> ${log.dst}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = LoRaTextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "[${log.packetType}]",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = LoRaCyan
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = rememberTimeFormat(log.timestamp),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = LoRaTextMuted
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = LoRaTextMuted, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = log.rawJson,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = LoRaTextSecondary,
                lineHeight = 14.sp
            )
        }
    }
}
