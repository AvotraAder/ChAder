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
    val type: MessageType = MessageType.TEXT
)

@Serializable
enum class MessageType {
    TEXT, IMAGE, VIDEO, VOICE
}
