package com.example.model

import org.json.JSONObject
import java.util.UUID

enum class PacketType {
    TEXT,
    GPS,
    AUDIO,
    SOS;

    companion object {
        fun fromString(value: String?): PacketType {
            return try {
                valueOf(value?.uppercase() ?: "TEXT")
            } catch (e: Exception) {
                TEXT
            }
        }
    }
}

data class LoRaPacket(
    val id: String = UUID.randomUUID().toString().take(8),
    val src: String,
    val dst: String = "BROADCAST",
    val type: PacketType = PacketType.TEXT,
    val hop: Int = 0,
    val payload: String,
    val timestamp: Long = System.currentTimeMillis() / 1000L,
    // Optional telemetry attached by bridge / receiver node
    val rssi: Int? = null,
    val snr: Float? = null
) {
    fun toJsonString(): String {
        val json = JSONObject()
        json.put("id", id)
        json.put("src", src)
        json.put("dst", dst)
        json.put("type", type.name)
        json.put("hop", hop)
        json.put("payload", payload)
        json.put("timestamp", timestamp)
        if (rssi != null) json.put("rssi", rssi)
        if (snr != null) json.put("snr", snr.toDouble())
        return json.toString()
    }

    companion object {
        fun fromJsonString(jsonStr: String): LoRaPacket? {
            return try {
                val json = JSONObject(jsonStr.trim())
                LoRaPacket(
                    id = json.optString("id", UUID.randomUUID().toString().take(8)),
                    src = json.optString("src", "UNKNOWN"),
                    dst = json.optString("dst", "BROADCAST"),
                    type = PacketType.fromString(json.optString("type", "TEXT")),
                    hop = json.optInt("hop", 0),
                    payload = json.optString("payload", ""),
                    timestamp = json.optLong("timestamp", System.currentTimeMillis() / 1000L),
                    rssi = if (json.has("rssi")) json.optInt("rssi") else null,
                    snr = if (json.has("snr")) json.optDouble("snr").toFloat() else null
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class GpsPayload(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 0f,
    val altitude: Double = 0.0,
    val speed: Float = 0f,
    val note: String = ""
) {
    fun serialize(): String {
        return "$latitude,$longitude,$accuracy,$altitude,$speed,$note"
    }

    companion object {
        fun deserialize(raw: String): GpsPayload? {
            return try {
                val parts = raw.split(",")
                if (parts.size >= 2) {
                    val lat = parts[0].toDoubleOrNull() ?: 0.0
                    val lon = parts[1].toDoubleOrNull() ?: 0.0
                    val acc = parts.getOrNull(2)?.toFloatOrNull() ?: 0f
                    val alt = parts.getOrNull(3)?.toDoubleOrNull() ?: 0.0
                    val spd = parts.getOrNull(4)?.toFloatOrNull() ?: 0f
                    val note = parts.drop(5).joinToString(",")
                    GpsPayload(lat, lon, acc, alt, spd, note)
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
}
