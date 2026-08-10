package com.example.chader.data.repository

import com.example.chader.data.local.ChatDao
import com.example.chader.data.model.Chat
import com.example.chader.data.model.Message
import com.example.chader.data.model.Story
import com.example.chader.data.model.User
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ChatRepository(private val chatDao: ChatDao) {
    val allChats: Flow<List<Chat>> = chatDao.getAllChats()
    val allUsers: Flow<List<User>> = chatDao.getAllUsers()
    
    fun getMessages(chatId: String): Flow<List<Message>> = chatDao.getMessagesForChat(chatId)
    
    fun getActiveStories(): Flow<List<Story>> = chatDao.getActiveStories(System.currentTimeMillis())

    suspend fun sendMessage(chatId: String, senderId: String, content: String) {
        val message = Message(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = senderId,
            content = content,
            timestamp = System.currentTimeMillis()
        )
        chatDao.insertMessage(message)
    }

    suspend fun insertUser(user: User) = chatDao.insertUser(user)
    suspend fun insertChat(chat: Chat) = chatDao.insertChat(chat)
    suspend fun insertStory(story: Story) = chatDao.insertStory(story)
    
    suspend fun getUserById(userId: String): User? = chatDao.getUserById(userId)
}
