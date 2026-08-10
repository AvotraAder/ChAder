package com.example.chader.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chader.data.model.Chat
import com.example.chader.data.model.Message
import com.example.chader.data.model.Story
import com.example.chader.data.model.User
import com.example.chader.data.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(private val repository: ChatRepository) : ViewModel() {

    val chats: Flow<List<Chat>> = repository.allChats
    val users: Flow<List<User>> = repository.allUsers
    val stories: Flow<List<Story>> = repository.getActiveStories()

    fun getMessages(chatId: String): Flow<List<Message>> = repository.getMessages(chatId)

    fun sendMessage(chatId: String, senderId: String, content: String) {
        viewModelScope.launch {
            repository.sendMessage(chatId, senderId, content)
        }
    }

    // Demo data helper
    fun seedDemoData() {
        viewModelScope.launch {
            val user1 = User("user1", "Alice", "https://i.pravatar.cc/150?u=user1", "Feeling energetic!")
            val user2 = User("user2", "Bob", "https://i.pravatar.cc/150?u=user2", "Available")
            repository.insertUser(user1)
            repository.insertUser(user2)

            val chat1 = Chat("chat1", listOf("user1", "me"))
            val chat2 = Chat("chat2", listOf("user2", "me"))
            repository.insertChat(chat1)
            repository.insertChat(chat2)

            repository.insertStory(Story("s1", "user1", "https://picsum.photos/seed/s1/400/800", System.currentTimeMillis(), System.currentTimeMillis() + 86400000))
            repository.insertStory(Story("s2", "user2", "https://picsum.photos/seed/s2/400/800", System.currentTimeMillis(), System.currentTimeMillis() + 86400000))
        }
    }
}
