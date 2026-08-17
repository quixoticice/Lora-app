package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.local.ChatMessageEntity
import com.example.model.GpsPayload
import com.example.model.PacketType
import com.example.ui.theme.BubbleGps
import com.example.ui.theme.BubbleIncoming
import com.example.ui.theme.BubbleOutgoing
import com.example.ui.theme.BubbleSos
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageItem(
    message: ChatMessageEntity,
    playingAudioId: String?,
    onPlayAudio: (id: String, base64: String) -> Unit
) {
    val isOutgoing = message.isOutgoing
    val isSos = message.type == PacketType.SOS.name
    val isGps = message.type == PacketType.GPS.name
    val isAudio = message.type == PacketType.AUDIO.name

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val timeFormatted = rememberTimeFormat(message.timestamp)

    val bubbleColor = when {
        isSos -> BubbleSos
        isGps -> BubbleGps
        isOutgoing -> BubbleOutgoing
        else -> BubbleIncoming
    }

    val borderColor = when {
        isSos -> LoRaRed
        isGps -> LoRaGreen.copy(alpha = 0.5f)
        isOutgoing -> LoRaOrange.copy(alpha = 0.4f)
        else -> LoRaBorder
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        // Sender info & Channel tag
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (isOutgoing) "YOU (${message.srcCallsign})" else message.srcCallsign,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isSos) LoRaRedBright else if (isOutgoing) LoRaOrange else LoRaCyan
            )

            if (message.dstCallsign != "BROADCAST") {
                Text(
                    text = " -> @${message.dstCallsign}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = LoRaAmber
                )
            } else {
                Text(
                    text = " [BROADCAST]",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = LoRaTextMuted
                )
            }

            if (message.hop > 0) {
                Text(
                    text = " • hop ${message.hop}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = LoRaTextMuted
                )
            }
        }

        // Bubble Content Card
        Card(
            modifier = Modifier
                .fillMaxWidth(if (isSos) 1f else 0.88f)
                .border(if (isSos) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = bubbleColor)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                when {
                    isSos -> {
                        SosMessageContent(
                            message = message,
                            onOpenMap = { lat, lon -> openMapIntent(context, lat, lon, message.srcCallsign) }
                        )
                    }
                    isGps -> {
                        GpsMessageContent(
                            message = message,
                            onOpenMap = { lat, lon -> openMapIntent(context, lat, lon, message.srcCallsign) },
                            onCopy = { coords ->
                                clipboardManager.setText(AnnotatedString(coords))
                                Toast.makeText(context, "Coordinates copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    isAudio -> {
                        AudioMessageContent(
                            message = message,
                            isPlaying = playingAudioId == message.packetId,
                            onPlay = { onPlayAudio(message.packetId, message.payload) }
                        )
                    }
                    else -> {
                        TextMessageContent(message = message)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Timestamp & Telemetry Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // RSSI / SNR if received from airwaves
                    if (message.rssi != null) {
                        Text(
                            text = "${message.rssi} dBm" + if (message.snr != null) " (SNR ${message.snr})" else "",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = LoRaCyan
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // Time & Status
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = timeFormatted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = LoRaTextMuted
                        )
                        if (isOutgoing) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = message.status,
                                tint = if (message.status == "DELIVERED" || message.status == "SENT") LoRaGreen else LoRaAmber,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TextMessageContent(message: ChatMessageEntity) {
    Text(
        text = message.payload,
        fontSize = 15.sp,
        color = LoRaTextPrimary,
        lineHeight = 20.sp
    )
}

@Composable
fun GpsMessageContent(
    message: ChatMessageEntity,
    onOpenMap: (lat: Double, lon: Double) -> Unit,
    onCopy: (coords: String) -> Unit
) {
    val gps = GpsPayload.deserialize(message.payload)

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "GPS Coordinates",
                tint = LoRaGreen,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "GPS LOCATION BROADCAST",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = LoRaGreen
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (gps != null) {
            val coordsStr = "${String.format(Locale.US, "%.5f", gps.latitude)}, ${String.format(Locale.US, "%.5f", gps.longitude)}"

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(LoRaDarkBg.copy(alpha = 0.6f))
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        text = coordsStr,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = LoRaAmberLight
                    )
                    Text(
                        text = "Accuracy: ±${gps.accuracy.toInt()}m | Altitude: ${gps.altitude.toInt()}m",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = LoRaTextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    if (gps.note.isNotBlank()) {
                        Text(
                            text = "Note: ${gps.note}",
                            fontSize = 12.sp,
                            color = LoRaTextPrimary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onOpenMap(gps.latitude, gps.longitude) },
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LoRaGreen, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open in Maps", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = { onCopy(coordsStr) },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(LoRaSurfaceVariant)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Coordinates", tint = LoRaCyan, modifier = Modifier.size(16.dp))
                }
            }
        } else {
            Text(text = message.payload, color = LoRaTextPrimary, fontSize = 14.sp)
        }
    }
}

@Composable
fun AudioMessageContent(
    message: ChatMessageEntity,
    isPlaying: Boolean,
    onPlay: () -> Unit
) {
    val estimatedBytes = (message.payload.length * 3) / 4
    val estimatedDur = (estimatedBytes / 600f).coerceIn(1.0f, 4.0f)

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice Note",
                tint = LoRaCyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "PUSH-TO-TALK VOICE NOTE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = LoRaCyan
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(LoRaDarkBg.copy(alpha = 0.7f))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Play Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) LoRaOrange else LoRaCyan)
                    .clickable { onPlay() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Waveform Graphic Simulation
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val heights = listOf(8, 14, 22, 10, 18, 26, 16, 12, 20, 24, 15, 9, 18, 11)
                heights.forEachIndexed { index, h ->
                    val barColor = if (isPlaying) {
                        if (index % 2 == 0) LoRaOrange else LoRaAmberLight
                    } else {
                        LoRaCyan.copy(alpha = 0.7f)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(h.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(barColor)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Audio telemetry
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format(Locale.US, "%.1fs", estimatedDur),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = LoRaTextPrimary
                )
                Text(
                    text = "${estimatedBytes}B (AMR)",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = LoRaTextMuted
                )
            }
        }
    }
}

@Composable
fun SosMessageContent(
    message: ChatMessageEntity,
    onOpenMap: (lat: Double, lon: Double) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sos_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(600), repeatMode = RepeatMode.Reverse),
        label = "sos_alpha"
    )

    val gps = GpsPayload.deserialize(message.payload)

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(LoRaRed.copy(alpha = alpha * 0.3f))
                .padding(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "EMERGENCY SOS",
                tint = LoRaRedBright,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "EMERGENCY DISTRESS BEACON (MAYDAY)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
                color = LoRaRedBright
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (gps != null) {
            val coordsStr = "${String.format(Locale.US, "%.5f", gps.latitude)}, ${String.format(Locale.US, "%.5f", gps.longitude)}"
            Text(
                text = "Node coordinates: $coordsStr (±${gps.accuracy.toInt()}m)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = LoRaTextPrimary
            )
            if (gps.note.isNotBlank()) {
                Text(
                    text = gps.note,
                    fontSize = 13.sp,
                    color = LoRaAmberLight,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onOpenMap(gps.latitude, gps.longitude) },
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LoRaRed, contentColor = Color.White)
            ) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("LAUNCH RESCUE MAP INTENT", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        } else {
            Text(text = message.payload, color = LoRaTextPrimary, fontSize = 14.sp)
        }
    }
}

fun openMapIntent(context: Context, lat: Double, lon: Double, label: String) {
    try {
        val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon($label)")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
        mapIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(mapIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "Lat: $lat, Lon: $lon", Toast.LENGTH_LONG).show()
    }
}

fun rememberTimeFormat(timestamp: Long): String {
    val date = Date(if (timestamp < 10000000000L) timestamp * 1000L else timestamp)
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return formatter.format(date)
}
