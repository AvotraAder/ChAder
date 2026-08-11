package com.example.chader.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "chats")
data class Chat(
    @PrimaryKey val id: String = "",
    val participantIds: List<String> = emptyList(),
    val lastMessageId: String? = null,
    val lastMessageContent: String? = null,
    val lastMessageTimestamp: Long? = null,
    val unreadCount: Int = 0
)
