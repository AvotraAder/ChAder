package com.example.chader.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val type: MessageType = MessageType.TEXT,
    val status: String = "SENT",
    val encryptionKey: String? = null,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false
) {
    val messageStatus: MessageStatus
        get() = try { enumValueOf<MessageStatus>(status) } catch (e: Exception) { MessageStatus.SENT }
}

@Serializable
enum class MessageType {
    TEXT, IMAGE, VIDEO, VOICE
}

@Serializable
enum class MessageStatus {
    SENDING, SENT, RECEIVED, SEEN
}
