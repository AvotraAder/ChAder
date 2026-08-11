package com.example.chader.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.chader.data.model.Chat
import com.example.chader.data.model.Message
import com.example.chader.data.model.User

@Database(entities = [User::class, Chat::class, Message::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}
