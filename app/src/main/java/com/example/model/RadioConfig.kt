package com.example.model

data class LoRaNode(
    val callsign: String,
    val lastSeen: Long = System.currentTimeMillis(),
    val rssi: Int = -80,
    val snr: Float = 6.0f,
    val lastMessageType: PacketType = PacketType.TEXT,
    val lastSnippet: String = "",
    val hopCount: Int = 0,
    val isFavorite: Boolean = false
)

data class RadioConfig(
    val frequencyMHz: Double = 433.0,
    val spreadingFactor: Int = 7, // SF7 - SF12
    val bandwidthKHz: Double = 125.0, // 125, 250, 500
    val codingRate: String = "4/5",
    val txPowerDbm: Int = 20,
    val wsUrl: String = "ws://192.168.4.1/ws",
    val btDeviceAddress: String = "",
    val btDeviceName: String = "",
    val autoReconnect: Boolean = true,
    val hopLimit: Int = 3
)
