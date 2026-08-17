package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.PacketType

enum class MessageStatus {
    PENDING,
    SENT,
    DELIVERED,
    FAILED
}

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packetId: String,
    val srcCallsign: String,
    val dstCallsign: String, // "BROADCAST" or specific callsign
    val type: String, // TEXT, GPS, AUDIO, SOS
    val payload: String,
    val timestamp: Long, // unix seconds or millis
    val isOutgoing: Boolean,
    val status: String = MessageStatus.SENT.name,
    val rssi: Int? = null,
    val snr: Float? = null,
    val hop: Int = 0
)

@Entity(tableName = "discovered_nodes")
data class DiscoveredNodeEntity(
    @PrimaryKey
    val callsign: String,
    val lastSeen: Long,
    val rssi: Int = -80,
    val snr: Float = 6.0f,
    val lastMsgType: String = "TEXT",
    val lastSnippet: String = "",
    val hopCount: Int = 0,
    val unreadCount: Int = 0
)

@Entity(tableName = "packet_logs")
data class PacketLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rawJson: String,
    val direction: String, // "TX" or "RX"
    val timestamp: Long = System.currentTimeMillis(),
    val packetType: String,
    val src: String,
    val dst: String,
    val isSuccess: Boolean = true
)
