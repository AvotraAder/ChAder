package com.example.chader.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val name: String,
    val avatarUrl: String?,
    val status: String? = null,
    val lastSeen: Long = System.currentTimeMillis()
)
