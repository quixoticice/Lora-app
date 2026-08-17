package com.example.model

enum class ConnectionMode(val title: String, val description: String) {
    WIFI_WEBSOCKET(
        "ESP32 Wi-Fi SoftAP",
        "Direct WebSocket connection (ws://192.168.4.1/ws)"
    ),
    BLUETOOTH_SPP(
        "Bluetooth Serial (SPP)",
        "RFCOMM stream to ESP32 Bluetooth Classic node"
    ),
    DEMO_SIMULATION(
        "Simulation Mode",
        "Virtual mesh network with interactive echo responder"
    )
}

sealed class ConnectionStatus {
    object Disconnected : ConnectionStatus()
    data class Connecting(val target: String) : ConnectionStatus()
    data class Connected(
        val mode: ConnectionMode,
        val endpoint: String,
        val rssi: Int = -65,
        val txCount: Int = 0,
        val rxCount: Int = 0
    ) : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
}
