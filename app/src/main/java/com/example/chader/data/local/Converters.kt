package com.example.chader.data.local

import androidx.room.TypeConverter
import com.example.chader.data.model.MessageStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return Json.decodeFromString(value)
    }

    @TypeConverter
    fun fromMessageStatus(status: MessageStatus?): String? {
        return status?.name
    }

    @TypeConverter
    fun toMessageStatus(value: String?): MessageStatus? {
        return value?.let { enumValueOf<MessageStatus>(it) }
    }
}
