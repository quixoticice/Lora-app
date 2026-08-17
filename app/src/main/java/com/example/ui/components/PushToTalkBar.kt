package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LoRaAmber
import com.example.ui.theme.LoRaAmberLight
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
import java.util.Locale

@Composable
fun PushToTalkBar(
    recipient: String,
    isRecording: Boolean,
    recordDurationSec: Float,
    amplitude: Float,
    onSendText: (String) -> Unit,
    onSendGps: () -> Unit,
    onSendSos: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecordingAndSend: () -> Unit,
    onCancelRecording: () -> Unit
) {
    var textState by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = LoRaSurface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Live Recording Active Banner
            AnimatedVisibility(visible = isRecording) {
                RecordingActiveBanner(
                    durationSec = recordDurationSec,
                    amplitude = amplitude,
                    onCancel = onCancelRecording
                )
            }

            if (!isRecording) {
                // Quick Action Bar (GPS & SOS shortcuts)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 1-Tap GPS Share
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(LoRaSurfaceVariant)
                                .border(1.dp, LoRaGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .pointerInput(Unit) {
                                    detectTapGestures(onTap = { onSendGps() })
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("send_gps_button"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Send GPS",
                                tint = LoRaGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SHARE GPS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = LoRaGreen
                            )
                        }

                        // SOS Broadcast Button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(LoRaRed.copy(alpha = 0.15f))
                                .border(1.dp, LoRaRed.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .pointerInput(Unit) {
                                    detectTapGestures(onTap = { onSendSos() })
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("send_sos_button"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "SOS Emergency",
                                tint = LoRaRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SOS BROADCAST",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = LoRaRed
                            )
                        }
                    }

                    Text(
                        text = if (recipient == "BROADCAST") "TO: #ALL" else "TO: @$recipient",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = LoRaAmber
                    )
                }

                // Main Input Row: Text Field + PTT Hold Button + Send Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = textState,
                        onValueChange = { textState = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("message_text_input"),
                        placeholder = {
                            Text(
                                text = if (recipient == "BROADCAST") "Broadcast to mesh..." else "Direct message to $recipient...",
                                color = LoRaTextMuted,
                                fontSize = 14.sp
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = LoRaDarkBg,
                            unfocusedContainerColor = LoRaDarkBg,
                            focusedBorderColor = LoRaOrange,
                            unfocusedBorderColor = LoRaBorder,
                            focusedTextColor = LoRaTextPrimary,
                            unfocusedTextColor = LoRaTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Push-to-Talk (PTT) Mic Button
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(LoRaCyan.copy(alpha = 0.15f))
                            .border(1.5.dp, LoRaCyan, CircleShape)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        onStartRecording()
                                        tryAwaitRelease()
                                        onStopRecordingAndSend()
                                    }
                                )
                            }
                            .testTag("ptt_mic_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Hold for PTT Voice Note",
                            tint = LoRaCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Send Button
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (textState.isNotBlank()) LoRaOrange else LoRaSurfaceVariant)
                            .border(
                                1.5.dp,
                                if (textState.isNotBlank()) LoRaOrange else LoRaBorder,
                                CircleShape
                            )
                            .pointerInput(textState) {
                                detectTapGestures(
                                    onTap = {
                                        if (textState.isNotBlank()) {
                                            onSendText(textState)
                                            textState = ""
                                        }
                                    }
                                )
                            }
                            .testTag("send_message_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (textState.isNotBlank()) Color.Black else LoRaTextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecordingActiveBanner(
    durationSec: Float,
    amplitude: Float,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rec_blink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(400), repeatMode = RepeatMode.Reverse),
        label = "rec_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LoRaRed.copy(alpha = 0.2f))
            .border(1.5.dp, LoRaRed, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(LoRaRed.copy(alpha = alpha))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "RECORDING AUDIO (PTT)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = LoRaRed
                    )
                    Text(
                        text = "Release button to send over LoRa (Max 4.0s)",
                        fontSize = 11.sp,
                        color = LoRaTextPrimary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Circular Progress countdown
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { durationSec / 4.0f },
                        modifier = Modifier.size(34.dp),
                        color = LoRaOrange,
                        trackColor = LoRaBorder,
                        strokeWidth = 3.dp
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f", durationSec),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = LoRaTextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel Audio",
                        tint = LoRaTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
