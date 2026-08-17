package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE (srcCallsign = :callsign AND dstCallsign != 'BROADCAST') OR (dstCallsign = :callsign) ORDER BY timestamp ASC")
    fun getDirectMessagesForNode(callsign: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE dstCallsign = 'BROADCAST' ORDER BY timestamp ASC")
    fun getBroadcastMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("UPDATE chat_messages SET status = :status WHERE packetId = :packetId")
    suspend fun updateMessageStatus(packetId: String, status: String)

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()
}

@Dao
interface NodeDao {
    @Query("SELECT * FROM discovered_nodes ORDER BY lastSeen DESC")
    fun getAllNodes(): Flow<List<DiscoveredNodeEntity>>

    @Query("SELECT * FROM discovered_nodes WHERE callsign = :callsign LIMIT 1")
    suspend fun getNode(callsign: String): DiscoveredNodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateNode(node: DiscoveredNodeEntity)

    @Query("UPDATE discovered_nodes SET unreadCount = 0 WHERE callsign = :callsign")
    suspend fun resetUnreadCount(callsign: String)

    @Query("DELETE FROM discovered_nodes WHERE callsign = :callsign")
    suspend fun deleteNode(callsign: String)

    @Query("DELETE FROM discovered_nodes")
    suspend fun clearAllNodes()
}

@Dao
interface PacketDao {
    @Query("SELECT * FROM packet_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentPacketLogs(): Flow<List<PacketLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPacketLog(log: PacketLogEntity)

    @Query("DELETE FROM packet_logs")
    suspend fun clearLogs()
}
