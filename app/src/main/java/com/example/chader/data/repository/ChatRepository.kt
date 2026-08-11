package com.example.chader.data.repository

import com.example.chader.data.local.ChatDao
import com.example.chader.data.model.Chat
import com.example.chader.data.model.Message
import com.example.chader.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepository(private val chatDao: ChatDao) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Local DB as cache
    val allChatsLocal: Flow<List<Chat>> = chatDao.getAllChats()
    val allUsersLocal: Flow<List<User>> = chatDao.getAllUsers()

    fun getMessagesLocal(chatId: String): Flow<List<Message>> = chatDao.getMessagesForChat(chatId)

    // Sync all chats where I am a participant
    fun syncChats(myEmail: String): Flow<List<Chat>> = callbackFlow {
        val listener = firestore.collection("chats")
            .whereArrayContains("participantIds", myEmail)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                val chats = snapshot?.toObjects(Chat::class.java) ?: emptyList()
                // Update local cache
                repositoryScope.launch {
                    chats.forEach { chatDao.insertChat(it) }
                }
                trySend(chats)
            }
        awaitClose { listener.remove() }
    }

    // Sync all users
    fun syncUsers(): Flow<List<User>> = callbackFlow {
        val listener = firestore.collection("users")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                val users = snapshot?.toObjects(User::class.java) ?: emptyList()
                repositoryScope.launch {
                    users.forEach { chatDao.insertUser(it) }
                }
                trySend(users)
            }
        awaitClose { listener.remove() }
    }

    // Remote Sync (Madagascar to the World)
    fun syncMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listener = firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                val messages = snapshot?.toObjects(Message::class.java) ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(chatId: String, content: String) {
        val userEmail = auth.currentUser?.email ?: "unknown"
        val message = Message(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = userEmail,
            content = content,
            timestamp = System.currentTimeMillis()
        )
        
        // Save message to Firestore sub-collection
        val chatRef = firestore.collection("chats").document(chatId)
        val messageRef = chatRef.collection("messages").document(message.id)
        
        firestore.runBatch { batch ->
            batch.set(messageRef, message)
            // Update the chat document with last message info for the list view
            batch.update(chatRef, mapOf(
                "lastMessageId" to message.id,
                "lastMessageContent" to message.content,
                "lastMessageTimestamp" to message.timestamp
            ))
        }.await()

        // Also cache locally
        chatDao.insertMessage(message)
    }

    suspend fun createOrUpdateUser(user: User) {
        firestore.collection("users").document(user.id).set(user).await()
        chatDao.insertUser(user)
    }

    suspend fun getUserByEmailOrUsername(query: String): User? {
        val trimmedQuery = query.trim().lowercase()
        return try {
            println("DEBUG: Firestore search start for: $trimmedQuery")
            
            // Search by email
            val emailQuery = firestore.collection("users")
                .whereEqualTo("email", trimmedQuery)
                .get()
                .await()
            
            println("DEBUG: Email search count: ${emailQuery.size()}")
            val byEmail = emailQuery.toObjects(User::class.java).firstOrNull()
            
            if (byEmail != null) {
                println("DEBUG: Found by email: ${byEmail.email}")
                return byEmail
            }

            // Search by username
            val usernameToSearch = if (trimmedQuery.startsWith("@")) trimmedQuery.substring(1) else trimmedQuery
            println("DEBUG: Searching for username: $usernameToSearch")
            
            val usernameQuery = firestore.collection("users")
                .whereEqualTo("username", usernameToSearch)
                .get()
                .await()
                
            println("DEBUG: Username search count: ${usernameQuery.size()}")
            val byUsername = usernameQuery.toObjects(User::class.java).firstOrNull()
            
            if (byUsername == null) {
                println("DEBUG: No user document found in Firestore 'users' collection for $trimmedQuery")
            } else {
                println("DEBUG: Found by username: ${byUsername.username}")
            }
            
            return byUsername
        } catch (e: Exception) {
            println("DEBUG: Firestore ERROR: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    suspend fun createChat(participantEmails: List<String>): String {
        // Simple chat ID generation based on sorted emails
        val chatId = participantEmails.sorted().joinToString("_")
        val chat = Chat(id = chatId, participantIds = participantEmails)
        firestore.collection("chats").document(chatId).set(chat).await()
        chatDao.insertChat(chat)
        return chatId
    }
}
