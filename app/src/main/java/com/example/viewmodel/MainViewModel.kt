package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.DiscoveredNodeEntity
import com.example.data.local.MessageStatus
import com.example.data.local.PacketLogEntity
import com.example.data.local.PreferencesManager
import com.example.model.ConnectionMode
import com.example.model.ConnectionStatus
import com.example.model.GpsPayload
import com.example.model.LoRaPacket
import com.example.model.PacketType
import com.example.model.RadioConfig
import com.example.service.AudioRecorderService
import com.example.service.GpsService
import com.example.service.LoRaConnectionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val chatDao = db.chatDao()
    private val nodeDao = db.nodeDao()
    private val packetDao = db.packetDao()

    val preferencesManager = PreferencesManager(application)
    val connectionManager = LoRaConnectionManager(application)
    val gpsService = GpsService(application)
    val audioRecorderService = AudioRecorderService(application)

    // User Callsign & State
    private val _callsign = MutableStateFlow(preferencesManager.getCallsign())
    val callsign: StateFlow<String> = _callsign.asStateFlow()

    private val _selectedMode = MutableStateFlow(preferencesManager.getConnectionMode())
    val selectedMode: StateFlow<ConnectionMode> = _selectedMode.asStateFlow()

    private val _radioConfig = MutableStateFlow(preferencesManager.getRadioConfig())
    val radioConfig: StateFlow<RadioConfig> = _radioConfig.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow(preferencesManager.isOnboardingCompleted())
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    // Active Channel / Node selection ("BROADCAST" or node callsign)
    private val _selectedRecipient = MutableStateFlow("BROADCAST")
    val selectedRecipient: StateFlow<String> = _selectedRecipient.asStateFlow()

    // Connection Status
    val connectionStatus: StateFlow<ConnectionStatus> = connectionManager.connectionStatus

    // Discovered Nodes
    val discoveredNodes: StateFlow<List<DiscoveredNodeEntity>> = nodeDao.getAllNodes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Messages for selected recipient/channel
    val currentMessages: StateFlow<List<ChatMessageEntity>> = _selectedRecipient
        .flatMapLatest { recipient ->
            if (recipient == "BROADCAST") {
                chatDao.getBroadcastMessages()
            } else {
                chatDao.getDirectMessagesForNode(recipient)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Raw Packet logs
    val recentPacketLogs: StateFlow<List<PacketLogEntity>> = packetDao.getRecentPacketLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live GPS state
    val liveGpsLocation: StateFlow<GpsPayload?> = gpsService.currentLocation

    // Audio recording state
    val isRecordingAudio: StateFlow<Boolean> = audioRecorderService.isRecording
    val recordDurationSec: StateFlow<Float> = audioRecorderService.recordDurationSec
    val recordingAmplitude: StateFlow<Float> = audioRecorderService.recordingAmplitude
    val playingAudioId: StateFlow<String?> = audioRecorderService.playingAudioId

    // Emergency Siren Flashing trigger
    private val _emergencyAlert = MutableStateFlow<ChatMessageEntity?>(null)
    val emergencyAlert: StateFlow<ChatMessageEntity?> = _emergencyAlert.asStateFlow()

    init {
        gpsService.startLocationUpdates()

        // Auto-connect if already onboarded
        if (preferencesManager.isOnboardingCompleted()) {
            connectionManager.connect(_selectedMode.value, _radioConfig.value)
        }

        // Collect incoming packets
        viewModelScope.launch {
            connectionManager.incomingPackets.collect { packet ->
                handleIncomingPacket(packet)
            }
        }

        // Collect outgoing packets for log persistence
        viewModelScope.launch {
            connectionManager.outgoingPackets.collect { packet ->
                packetDao.insertPacketLog(
                    PacketLogEntity(
                        rawJson = packet.toJsonString(),
                        direction = "TX",
                        packetType = packet.type.name,
                        src = packet.src,
                        dst = packet.dst,
                        isSuccess = true
                    )
                )
            }
        }
    }

    fun completeOnboarding(callsign: String, mode: ConnectionMode) {
        val cleanCallsign = callsign.trim().uppercase().ifEmpty { "OPERATOR-01" }
        preferencesManager.setCallsign(cleanCallsign)
        preferencesManager.setConnectionMode(mode)
        preferencesManager.setOnboardingCompleted(true)

        _callsign.value = cleanCallsign
        _selectedMode.value = mode
        _isOnboardingCompleted.value = true

        connectionManager.connect(mode, _radioConfig.value)
    }

    fun setRecipient(recipient: String) {
        _selectedRecipient.value = recipient.trim().uppercase()
        if (recipient != "BROADCAST") {
            viewModelScope.launch {
                nodeDao.resetUnreadCount(recipient)
            }
        }
    }

    fun updateCallsign(newCallsign: String) {
        val clean = newCallsign.trim().uppercase()
        if (clean.isNotEmpty()) {
            preferencesManager.setCallsign(clean)
            _callsign.value = clean
        }
    }

    fun updateMode(mode: ConnectionMode) {
        preferencesManager.setConnectionMode(mode)
        _selectedMode.value = mode
        connectionManager.connect(mode, _radioConfig.value)
    }

    fun updateRadioConfig(config: RadioConfig) {
        preferencesManager.saveRadioConfig(config)
        _radioConfig.value = config
        connectionManager.connect(_selectedMode.value, config)
    }

    fun reconnect() {
        connectionManager.connect(_selectedMode.value, _radioConfig.value)
    }

    fun disconnect() {
        connectionManager.disconnect()
    }

    fun sendTextMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val packet = LoRaPacket(
            id = UUID.randomUUID().toString().take(8),
            src = _callsign.value,
            dst = _selectedRecipient.value,
            type = PacketType.TEXT,
            hop = 0,
            payload = trimmed,
            timestamp = System.currentTimeMillis() / 1000L
        )

        viewModelScope.launch {
            // Save to local Room database
            val messageEntity = ChatMessageEntity(
                packetId = packet.id,
                srcCallsign = packet.src,
                dstCallsign = packet.dst,
                type = packet.type.name,
                payload = packet.payload,
                timestamp = System.currentTimeMillis(),
                isOutgoing = true,
                status = MessageStatus.PENDING.name,
                hop = 0
            )
            val msgId = chatDao.insertMessage(messageEntity)

            val success = connectionManager.sendPacket(packet)
            chatDao.updateMessageStatus(packet.id, if (success) MessageStatus.SENT.name else MessageStatus.FAILED.name)
        }
    }

    fun sendGpsLocation(customNote: String = "") {
        val loc = gpsService.getCurrentOrFallbackLocation()
        val payloadStr = if (customNote.isNotBlank()) {
            loc.copy(note = customNote).serialize()
        } else {
            loc.serialize()
        }

        val packet = LoRaPacket(
            id = UUID.randomUUID().toString().take(8),
            src = _callsign.value,
            dst = _selectedRecipient.value,
            type = PacketType.GPS,
            hop = 0,
            payload = payloadStr,
            timestamp = System.currentTimeMillis() / 1000L
        )

        viewModelScope.launch {
            chatDao.insertMessage(
                ChatMessageEntity(
                    packetId = packet.id,
                    srcCallsign = packet.src,
                    dstCallsign = packet.dst,
                    type = PacketType.GPS.name,
                    payload = packet.payload,
                    timestamp = System.currentTimeMillis(),
                    isOutgoing = true,
                    status = MessageStatus.SENT.name
                )
            )
            connectionManager.sendPacket(packet)
        }
    }

    fun sendSosEmergency(emergencyNote: String = "EMERGENCY SOS! Off-grid beacon.") {
        val loc = gpsService.getCurrentOrFallbackLocation()
        val sosPayload = "${loc.latitude},${loc.longitude},${loc.accuracy},${loc.altitude},0,SOS: $emergencyNote"

        val packet = LoRaPacket(
            id = UUID.randomUUID().toString().take(8),
            src = _callsign.value,
            dst = "BROADCAST", // SOS is always broadcast to all nodes
            type = PacketType.SOS,
            hop = 0,
            payload = sosPayload,
            timestamp = System.currentTimeMillis() / 1000L
        )

        viewModelScope.launch {
            chatDao.insertMessage(
                ChatMessageEntity(
                    packetId = packet.id,
                    srcCallsign = packet.src,
                    dstCallsign = "BROADCAST",
                    type = PacketType.SOS.name,
                    payload = packet.payload,
                    timestamp = System.currentTimeMillis(),
                    isOutgoing = true,
                    status = MessageStatus.SENT.name
                )
            )
            connectionManager.sendPacket(packet)
        }
    }

    fun startAudioRecording() {
        audioRecorderService.startRecording { base64Data, durationSec ->
            sendAudioVoiceNote(base64Data, durationSec)
        }
    }

    fun stopAudioRecordingAndSend() {
        val (base64, durationSec) = audioRecorderService.stopRecording()
        if (base64.isNotEmpty()) {
            sendAudioVoiceNote(base64, durationSec)
        }
    }

    fun cancelAudioRecording() {
        audioRecorderService.cancelRecording()
    }

    private fun sendAudioVoiceNote(base64Data: String, durationSec: Float) {
        val packet = LoRaPacket(
            id = UUID.randomUUID().toString().take(8),
            src = _callsign.value,
            dst = _selectedRecipient.value,
            type = PacketType.AUDIO,
            hop = 0,
            payload = base64Data,
            timestamp = System.currentTimeMillis() / 1000L
        )

        viewModelScope.launch {
            chatDao.insertMessage(
                ChatMessageEntity(
                    packetId = packet.id,
                    srcCallsign = packet.src,
                    dstCallsign = packet.dst,
                    type = PacketType.AUDIO.name,
                    payload = packet.payload,
                    timestamp = System.currentTimeMillis(),
                    isOutgoing = true,
                    status = MessageStatus.SENT.name
                )
            )
            connectionManager.sendPacket(packet)
        }
    }

    fun playAudio(audioId: String, base64Data: String) {
        audioRecorderService.playAudio(audioId, base64Data)
    }

    fun dismissEmergencyAlert() {
        _emergencyAlert.value = null
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            chatDao.clearAllMessages()
            packetDao.clearLogs()
        }
    }

    private suspend fun handleIncomingPacket(packet: LoRaPacket) {
        // Ignore loopback packets from self
        if (packet.src.equals(_callsign.value, ignoreCase = true)) return

        // 1. Persist packet in message store
        val entity = ChatMessageEntity(
            packetId = packet.id,
            srcCallsign = packet.src,
            dstCallsign = packet.dst,
            type = packet.type.name,
            payload = packet.payload,
            timestamp = packet.timestamp * 1000L,
            isOutgoing = false,
            status = MessageStatus.DELIVERED.name,
            rssi = packet.rssi,
            snr = packet.snr,
            hop = packet.hop
        )
        chatDao.insertMessage(entity)

        // 2. Update discovered node registry
        val existingNode = nodeDao.getNode(packet.src)
        val unreadInc = if (_selectedRecipient.value == packet.src || _selectedRecipient.value == "BROADCAST") 0 else 1
        val updatedNode = DiscoveredNodeEntity(
            callsign = packet.src,
            lastSeen = System.currentTimeMillis(),
            rssi = packet.rssi ?: existingNode?.rssi ?: -75,
            snr = packet.snr ?: existingNode?.snr ?: 7.0f,
            lastMsgType = packet.type.name,
            lastSnippet = when (packet.type) {
                PacketType.TEXT -> packet.payload
                PacketType.GPS -> "GPS Coordinates"
                PacketType.AUDIO -> "Voice Note (PTT)"
                PacketType.SOS -> "SOS Emergency Beacon!"
            }.take(40),
            hopCount = packet.hop,
            unreadCount = (existingNode?.unreadCount ?: 0) + unreadInc
        )
        nodeDao.insertOrUpdateNode(updatedNode)

        // 3. Log raw packet
        packetDao.insertPacketLog(
            PacketLogEntity(
                rawJson = packet.toJsonString(),
                direction = "RX",
                packetType = packet.type.name,
                src = packet.src,
                dst = packet.dst,
                isSuccess = true
            )
        )

        // 4. Trigger emergency banner if SOS
        if (packet.type == PacketType.SOS) {
            _emergencyAlert.value = entity
        }
    }

    fun getEspFirmwareSnippet(): String {
        return """
/* =========================================================================
 * ESP32 + SX1278 (Ra-02 433MHz) LoRa Mesh Communicator Firmware
 * Bridges Mobile App (Wi-Fi WebSocket / Bluetooth SPP) <-> SX1278 LoRa Radio
 * =========================================================================
 * Hardware SPI Pinout (ESP32 -> Ra-02 SX1278):
 *  - NSS / CS  -> GPIO 5
 *  - RST       -> GPIO 14
 *  - DIO0 / IRQ-> GPIO 2
 *  - SCK       -> GPIO 18
 *  - MISO      -> GPIO 19
 *  - MOSI      -> GPIO 23
 * ========================================================================= */

#include <SPI.h>
#include <LoRa.h>
#include <WiFi.h>
#include <WebSocketsServer.h>
#include "BluetoothSerial.h"
#include <ArduinoJson.h>

// --- LoRa Hardware Configuration ---
#define LORA_SCK     18
#define LORA_MISO    19
#define LORA_MOSI    23
#define LORA_SS      5
#define LORA_RST     14
#define LORA_DIO0    2
#define LORA_FREQ    433E6  // 433.0 MHz (or 868E6 / 915E6)

// --- Wi-Fi SoftAP Config ---
const char* ap_ssid = "ESP32_LoRa_Node";
const char* ap_pass = "loramesh123";
WebSocketsServer webSocket = WebSocketsServer(80);

// --- Bluetooth SPP Serial ---
BluetoothSerial SerialBT;

void onWebSocketEvent(uint8_t num, WStype_t type, uint8_t * payload, size_t length) {
    if (type == WStype_TEXT) {
        String msg = String((char*)payload);
        Serial.printf("[WS -> LoRa TX] %s\n", msg.c_str());
        // Broadcast over LoRa airwaves
        LoRa.beginPacket();
        LoRa.print(msg);
        LoRa.endPacket();
    }
}

void setup() {
    Serial.begin(115200);
    delay(1000);
    Serial.println("\n[INIT] Starting ESP32 LoRa Bridge...");

    // 1. Initialize LoRa SX1278
    SPI.begin(LORA_SCK, LORA_MISO, LORA_MOSI, LORA_SS);
    LoRa.setPins(LORA_SS, LORA_RST, LORA_DIO0);
    
    if (!LoRa.begin(LORA_FREQ)) {
        Serial.println("[ERROR] SX1278 LoRa initialization failed!");
        while (1) { delay(1000); }
    }
    
    LoRa.setSpreadingFactor(7);     // SF7 for balance of range and speed
    LoRa.setSignalBandwidth(125E3);  // 125 kHz
    LoRa.setCodingRate4(5);         // CR 4/5
    LoRa.setTxPower(20);            // 20 dBm (100mW max)
    LoRa.enableCrc();
    Serial.println("[OK] SX1278 LoRa Radio Initialized @ 433MHz.");

    // 2. Initialize Wi-Fi SoftAP & WebSocket
    WiFi.softAP(ap_ssid, ap_pass);
    IPAddress IP = WiFi.softAPIP();
    Serial.printf("[OK] SoftAP SSID: %s | IP: %s\n", ap_ssid, IP.toString().c_str());
    
    webSocket.begin();
    webSocket.onEvent(onWebSocketEvent);

    // 3. Initialize Bluetooth SPP Serial
    SerialBT.begin("ESP32_LoRa_Node"); // Bluetooth device name
    Serial.println("[OK] Bluetooth SPP Serial active as 'ESP32_LoRa_Node'");
}

void loop() {
    webSocket.loop();

    // Check if Phone sent packet via Bluetooth SPP
    if (SerialBT.available()) {
        String btMsg = SerialBT.readStringUntil('\n');
        btMsg.trim();
        if (btMsg.length() > 0) {
            Serial.printf("[BT -> LoRa TX] %s\n", btMsg.c_str());
            LoRa.beginPacket();
            LoRa.print(btMsg);
            LoRa.endPacket();
        }
    }

    // Check if LoRa Radio received a packet from the airwaves
    int packetSize = LoRa.parsePacket();
    if (packetSize) {
        String incoming = "";
        while (LoRa.available()) {
            incoming += (char)LoRa.read();
        }
        
        int rssi = LoRa.packetRssi();
        float snr = LoRa.packetSnr();
        Serial.printf("[LoRa RX] RSSI: %d dBm | SNR: %.1f dB | Msg: %s\n", rssi, snr, incoming.c_str());

        // Parse JSON and inject live RF telemetry (RSSI & SNR)
        StaticJsonDocument<1024> doc;
        DeserializationError err = deserializeJson(doc, incoming);
        if (!err) {
            doc["rssi"] = rssi;
            doc["snr"] = snr;
            String enrichedJson;
            serializeJson(doc, enrichedJson);

            // Forward to connected phone via WebSocket
            webSocket.broadcastTXT(enrichedJson);

            // Forward to connected phone via Bluetooth SPP
            SerialBT.println(enrichedJson);
        } else {
            // Fallback plain forwarding
            webSocket.broadcastTXT(incoming);
            SerialBT.println(incoming);
        }
    }
}
""".trimIndent()
    }

    override fun onCleared() {
        super.onCleared()
        gpsService.stopLocationUpdates()
        audioRecorderService.stopPlayback()
        audioRecorderService.cancelRecording()
    }
}
