package com.example.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.example.model.ConnectionMode
import com.example.model.ConnectionStatus
import com.example.model.GpsPayload
import com.example.model.LoRaPacket
import com.example.model.PacketType
import com.example.model.RadioConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class LoRaConnectionManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO)

    // Status Flow
    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    // Incoming Packets Flow
    private val _incomingPackets = MutableSharedFlow<LoRaPacket>(extraBufferCapacity = 64)
    val incomingPackets: SharedFlow<LoRaPacket> = _incomingPackets

    // Outgoing packet broadcast for UI logs
    private val _outgoingPackets = MutableSharedFlow<LoRaPacket>(extraBufferCapacity = 64)
    val outgoingPackets: SharedFlow<LoRaPacket> = _outgoingPackets

    // Packet statistics
    private var txCount = 0
    private var rxCount = 0

    // Wi-Fi WebSocket variables
    private var okHttpClient: OkHttpClient? = null
    private var webSocket: WebSocket? = null

    // Bluetooth variables
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var bluetoothSocket: BluetoothSocket? = null
    private var bluetoothOutputStream: OutputStream? = null
    private var bluetoothReaderJob: Job? = null

    // Simulation Job
    private var simulationJob: Job? = null

    // Reconnect Job
    private var reconnectJob: Job? = null
    private var currentMode: ConnectionMode = ConnectionMode.DEMO_SIMULATION
    private var currentConfig: RadioConfig = RadioConfig()
    private var isUserInitiatedDisconnect = false

    fun connect(mode: ConnectionMode, config: RadioConfig) {
        disconnect()
        isUserInitiatedDisconnect = false
        currentMode = mode
        currentConfig = config

        when (mode) {
            ConnectionMode.WIFI_WEBSOCKET -> connectWebSocket(config.wsUrl)
            ConnectionMode.BLUETOOTH_SPP -> connectBluetooth(config.btDeviceAddress, config.btDeviceName)
            ConnectionMode.DEMO_SIMULATION -> startSimulationMode()
        }
    }

    fun disconnect() {
        isUserInitiatedDisconnect = true
        reconnectJob?.cancel()

        // Close WebSocket
        try {
            webSocket?.close(1000, "User disconnected")
        } catch (e: Exception) {
            // Ignore
        } finally {
            webSocket = null
        }

        // Close Bluetooth
        bluetoothReaderJob?.cancel()
        try {
            bluetoothOutputStream?.close()
            bluetoothSocket?.close()
        } catch (e: Exception) {
            // Ignore
        } finally {
            bluetoothOutputStream = null
            bluetoothSocket = null
        }

        // Cancel Simulation
        simulationJob?.cancel()

        _connectionStatus.value = ConnectionStatus.Disconnected
    }

    private fun connectWebSocket(url: String) {
        _connectionStatus.value = ConnectionStatus.Connecting(url)
        scope.launch {
            try {
                okHttpClient = OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(0, TimeUnit.MILLISECONDS)
                    .pingInterval(10, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder().url(url).build()

                webSocket = okHttpClient?.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(ws: WebSocket, response: Response) {
                        _connectionStatus.value = ConnectionStatus.Connected(
                            mode = ConnectionMode.WIFI_WEBSOCKET,
                            endpoint = url,
                            rssi = -55,
                            txCount = txCount,
                            rxCount = rxCount
                        )
                    }

                    override fun onMessage(ws: WebSocket, text: String) {
                        handleIncomingRawJson(text)
                    }

                    override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                        ws.close(1000, null)
                    }

                    override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                        if (!isUserInitiatedDisconnect && currentConfig.autoReconnect) {
                            scheduleReconnect()
                        } else {
                            _connectionStatus.value = ConnectionStatus.Disconnected
                        }
                    }

                    override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                        _connectionStatus.value = ConnectionStatus.Error("Wi-Fi Error: ${t.localizedMessage ?: "Failed to connect to $url"}")
                        if (!isUserInitiatedDisconnect && currentConfig.autoReconnect) {
                            scheduleReconnect()
                        }
                    }
                })
            } catch (e: Exception) {
                _connectionStatus.value = ConnectionStatus.Error("WebSocket Exception: ${e.message}")
                if (!isUserInitiatedDisconnect && currentConfig.autoReconnect) {
                    scheduleReconnect()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectBluetooth(deviceAddress: String, deviceName: String) {
        val targetName = if (deviceName.isNotEmpty()) deviceName else deviceAddress.ifEmpty { "ESP32_LoRa_Node" }
        _connectionStatus.value = ConnectionStatus.Connecting("BT: $targetName")

        scope.launch(Dispatchers.IO) {
            try {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                if (adapter == null || !adapter.isEnabled) {
                    _connectionStatus.value = ConnectionStatus.Error("Bluetooth is disabled on device")
                    return@launch
                }

                val device: BluetoothDevice? = if (deviceAddress.isNotEmpty()) {
                    adapter.getRemoteDevice(deviceAddress)
                } else {
                    // Try to find paired ESP32 device
                    adapter.bondedDevices.firstOrNull {
                        it.name?.contains("ESP32", ignoreCase = true) == true ||
                                it.name?.contains("LoRa", ignoreCase = true) == true
                    } ?: adapter.bondedDevices.firstOrNull()
                }

                if (device == null) {
                    _connectionStatus.value = ConnectionStatus.Error("No paired Bluetooth ESP32 node found. Please pair in Settings.")
                    return@launch
                }

                adapter.cancelDiscovery()

                val socket = device.createRfcommSocketToServiceRecord(sppUuid)
                socket.connect()

                bluetoothSocket = socket
                bluetoothOutputStream = socket.outputStream

                _connectionStatus.value = ConnectionStatus.Connected(
                    mode = ConnectionMode.BLUETOOTH_SPP,
                    endpoint = device.name ?: device.address,
                    rssi = -68,
                    txCount = txCount,
                    rxCount = rxCount
                )

                // Start reader loop
                val reader = BufferedReader(InputStreamReader(socket.inputStream))
                bluetoothReaderJob = launch {
                    try {
                        while (isActive) {
                            val line = reader.readLine() ?: break
                            if (line.isNotBlank()) {
                                handleIncomingRawJson(line)
                            }
                        }
                    } catch (e: Exception) {
                        if (!isUserInitiatedDisconnect) {
                            _connectionStatus.value = ConnectionStatus.Error("Bluetooth link lost")
                            if (currentConfig.autoReconnect) scheduleReconnect()
                        }
                    }
                }
            } catch (e: Exception) {
                _connectionStatus.value = ConnectionStatus.Error("Bluetooth error: ${e.message ?: "Failed to connect"}")
                if (!isUserInitiatedDisconnect && currentConfig.autoReconnect) {
                    scheduleReconnect()
                }
            }
        }
    }

    private fun startSimulationMode() {
        _connectionStatus.value = ConnectionStatus.Connected(
            mode = ConnectionMode.DEMO_SIMULATION,
            endpoint = "Virtual LoRa Mesh (433MHz SF7)",
            rssi = -72,
            txCount = txCount,
            rxCount = rxCount
        )

        // Launch periodic simulated node telemetry & beacon broadcasts
        simulationJob = scope.launch {
            delay(1500)
            // Welcome beacon from Base Station
            val beacon = LoRaPacket(
                src = "BASE-STATION",
                dst = "BROADCAST",
                type = PacketType.TEXT,
                hop = 1,
                payload = "LoRa Mesh Repeater Active on 433.000MHz SF7 BW125. All nodes operational.",
                rssi = -74,
                snr = 8.5f
            )
            handleIncomingPacket(beacon)

            while (isActive) {
                delay(35000 + Random.nextLong(15000))
                val simulatedNodes = listOf("ALPHA-RELAY", "SCOUT-02", "COMMAND-HQ", "MOUNTAIN-NODE")
                val randomNode = simulatedNodes.random()
                val randomType = if (Random.nextBoolean()) PacketType.TEXT else PacketType.GPS

                val simPacket = when (randomType) {
                    PacketType.TEXT -> LoRaPacket(
                        src = randomNode,
                        dst = "BROADCAST",
                        type = PacketType.TEXT,
                        hop = Random.nextInt(0, 3),
                        payload = listOf(
                            "Battery 88% | Temperature 22C | Solar Charging OK",
                            "Sector 4 perimeter patrol clear. RF link strong.",
                            "Check-in pulse: Node online and repeating packets.",
                            "Monitoring emergency channel 433MHz."
                        ).random(),
                        rssi = Random.nextInt(-95, -60),
                        snr = Random.nextDouble(4.0, 11.0).toFloat()
                    )
                    PacketType.GPS -> {
                        val lat = 37.7749 + Random.nextDouble(-0.04, 0.04)
                        val lon = -122.4194 + Random.nextDouble(-0.04, 0.04)
                        LoRaPacket(
                            src = randomNode,
                            dst = "BROADCAST",
                            type = PacketType.GPS,
                            hop = 1,
                            payload = GpsPayload(lat, lon, 8.0f, 120.0, 1.2f, "Field telemetry").serialize(),
                            rssi = Random.nextInt(-90, -65),
                            snr = Random.nextDouble(5.0, 9.5).toFloat()
                        )
                    }
                    else -> null
                }
                simPacket?.let { handleIncomingPacket(it) }
            }
        }
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            _connectionStatus.value = ConnectionStatus.Connecting("Auto-reconnecting in 4s...")
            delay(4000)
            if (!isUserInitiatedDisconnect) {
                connect(currentMode, currentConfig)
            }
        }
    }

    suspend fun sendPacket(packet: LoRaPacket): Boolean {
        val jsonStr = packet.toJsonString()
        var success = false

        withContext(Dispatchers.IO) {
            when (currentMode) {
                ConnectionMode.WIFI_WEBSOCKET -> {
                    webSocket?.let { ws ->
                        success = ws.send(jsonStr)
                    }
                }
                ConnectionMode.BLUETOOTH_SPP -> {
                    bluetoothOutputStream?.let { stream ->
                        try {
                            stream.write((jsonStr + "\n").toByteArray(Charsets.UTF_8))
                            stream.flush()
                            success = true
                        } catch (e: Exception) {
                            success = false
                        }
                    }
                }
                ConnectionMode.DEMO_SIMULATION -> {
                    // Simulates over-the-air transmission delay
                    delay(350)
                    success = true

                    // Simulate interactive automatic response for realism
                    scope.launch {
                        delay(1200 + Random.nextLong(800))
                        simulateEchoResponse(packet)
                    }
                }
            }

            if (success) {
                txCount++
                updateStatsInStatus()
                _outgoingPackets.emit(packet)
            }
        }
        return success
    }

    private suspend fun simulateEchoResponse(sentPacket: LoRaPacket) {
        val respNode = if (sentPacket.dst != "BROADCAST" && sentPacket.dst.isNotBlank()) sentPacket.dst else "ECHO-RELAY"
        val response = when (sentPacket.type) {
            PacketType.TEXT -> {
                LoRaPacket(
                    src = respNode,
                    dst = sentPacket.src,
                    type = PacketType.TEXT,
                    hop = 1,
                    payload = "ACK [${sentPacket.id}] - Received text: \"${sentPacket.payload.take(30)}\"",
                    rssi = Random.nextInt(-85, -60),
                    snr = Random.nextDouble(6.0, 10.5).toFloat()
                )
            }
            PacketType.GPS -> {
                LoRaPacket(
                    src = respNode,
                    dst = sentPacket.src,
                    type = PacketType.TEXT,
                    hop = 1,
                    payload = "GPS coordinates logged by $respNode. Tracking active.",
                    rssi = Random.nextInt(-80, -62),
                    snr = Random.nextDouble(7.0, 11.0).toFloat()
                )
            }
            PacketType.SOS -> {
                LoRaPacket(
                    src = "RESCUE-DISPATCH",
                    dst = "BROADCAST",
                    type = PacketType.TEXT,
                    hop = 0,
                    payload = "MAYDAY ACKNOWLEDGED! Rescue unit responding to ${sentPacket.src} emergency broadcast.",
                    rssi = -58,
                    snr = 12.0f
                )
            }
            PacketType.AUDIO -> {
                LoRaPacket(
                    src = respNode,
                    dst = sentPacket.src,
                    type = PacketType.TEXT,
                    hop = 1,
                    payload = "Audio clip packet received (${(sentPacket.payload.length * 3 / 4)} bytes)",
                    rssi = Random.nextInt(-85, -65),
                    snr = Random.nextDouble(5.5, 9.0).toFloat()
                )
            }
        }
        handleIncomingPacket(response)
    }

    private fun handleIncomingRawJson(rawJson: String) {
        val packet = LoRaPacket.fromJsonString(rawJson)
        if (packet != null) {
            handleIncomingPacket(packet)
        }
    }

    private fun handleIncomingPacket(packet: LoRaPacket) {
        rxCount++
        updateStatsInStatus()
        scope.launch {
            _incomingPackets.emit(packet)
        }
    }

    private fun updateStatsInStatus() {
        val current = _connectionStatus.value
        if (current is ConnectionStatus.Connected) {
            _connectionStatus.value = current.copy(
                txCount = txCount,
                rxCount = rxCount
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun getPairedBluetoothDevices(): List<Pair<String, String>> {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            adapter?.bondedDevices?.map { device ->
                Pair(device.name ?: "Unknown Device", device.address)
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
