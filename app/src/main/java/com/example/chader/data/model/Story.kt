package com.example.chader.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "stories")
data class Story(
    @PrimaryKey val id: String,
    val userId: String,
    val imageUrl: String,
    val timestamp: Long,
    val expiresAt: Long
)
