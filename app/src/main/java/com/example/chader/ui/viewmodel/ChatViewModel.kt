package com.example.chader.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chader.data.model.Chat
import com.example.chader.data.model.Message
import com.example.chader.data.model.User
import com.example.chader.data.repository.ChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(private val repository: ChatRepository) : ViewModel() {

    private val _myEmail = MutableStateFlow<String?>(null)
    
    val chats: StateFlow<List<Chat>> = _myEmail.flatMapLatest { email ->
        if (email == null) repository.allChatsLocal
        else repository.syncChats(email)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val users: StateFlow<List<User>> = repository.syncUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setMyEmail(email: String) {
        _myEmail.value = email
    }
    
    fun getMessages(chatId: String, encryptionKey: String?): Flow<List<Message>> = repository.syncMessages(chatId, encryptionKey)

    fun markAsReceived(chatId: String, myEmail: String) {
        viewModelScope.launch {
            repository.markMessagesAsReceived(chatId, myEmail)
        }
    }

    fun markAsSeen(chatId: String, myEmail: String) {
        viewModelScope.launch {
            repository.markMessagesAsSeen(chatId, myEmail)
        }
    }

    fun sendMessage(chatId: String, content: String, encryptionKey: String?) {
        viewModelScope.launch {
            repository.sendMessage(chatId, content, encryptionKey)
        }
    }

    fun editMessage(chatId: String, messageId: String, newContent: String, encryptionKey: String?) {
        viewModelScope.launch {
            repository.editMessage(chatId, messageId, newContent, encryptionKey)
        }
    }

    fun deleteMessage(chatId: String, messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(chatId, messageId)
        }
    }

    fun updateEncryptionKey(chatId: String, newKey: String) {
        viewModelScope.launch {
            repository.updateChatEncryptionKey(chatId, newKey)
        }
    }

    fun setUserStatus(userId: String, isOnline: Boolean) {
        viewModelScope.launch {
            repository.updateUserStatus(userId, isOnline)
        }
    }

    fun updateLastSeen(userId: String) {
        viewModelScope.launch {
            repository.updateLastSeen(userId)
        }
    }

    fun updateUsername(userId: String, newUsername: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = repository.updateUsername(userId, newUsername)
            onResult(result)
        }
    }

    fun startChatByQuery(query: String, myEmail: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            println("DEBUG: Starting chat search for query: $query")
            try {
                val otherUser = repository.getUserByEmailOrUsername(query)
                if (otherUser != null) {
                    println("DEBUG: Found user: ${otherUser.email}")
                    if (otherUser.email.equals(myEmail, ignoreCase = true)) {
                        println("DEBUG: Cannot start chat with yourself")
                        onResult(null)
                        return@launch
                    }
                    val chatId = repository.createChat(listOf(otherUser.email, myEmail))
                    println("DEBUG: Chat created with ID: $chatId")
                    onResult(chatId)
                } else {
                    println("DEBUG: No user found for query: $query")
                    onResult(null)
                }
            } catch (e: Exception) {
                println("DEBUG: Error in startChatByQuery: ${e.message}")
                onResult(null)
            }
        }
    }

    // Demo data helper
    fun seedDemoData(myEmail: String) {
        viewModelScope.launch {
            val user1 = User("user1", "Alice", "alice@example.com", "https://i.pravatar.cc/150?u=user1", "Feeling energetic!")
            val user2 = User("user2", "Bob", "bob@example.com", "https://i.pravatar.cc/150?u=user2", "Available")
            repository.createOrUpdateUser(user1)
            repository.createOrUpdateUser(user2)
            
            repository.createChat(listOf("alice@example.com", myEmail))
        }
    }
}
