package com.example.chader.data.repository

import com.example.chader.data.local.ChatDao
import com.example.chader.data.model.Chat
import com.example.chader.data.model.Message
import com.example.chader.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.chader.util.EncryptionUtils
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
                
                // Decrypt last message for local storage and display
                val decryptedChats = chats.map { chat ->
                    chat.copy(
                        lastMessageContent = chat.lastMessageContent?.let { 
                            EncryptionUtils.decrypt(it, chat.id) 
                        }
                    )
                }

                // Update local cache (stores decrypted content)
                repositoryScope.launch {
                    decryptedChats.forEach { chatDao.insertChat(it) }
                }
                trySend(decryptedChats)
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

    // Remote Sync
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
                
                // Decrypt messages content
                val decryptedMessages = messages.map { 
                    it.copy(content = EncryptionUtils.decrypt(it.content, chatId)) 
                }

                // Update local cache (optional but recommended)
                repositoryScope.launch {
                    decryptedMessages.forEach { chatDao.insertMessage(it) }
                }

                trySend(decryptedMessages)
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(chatId: String, content: String) {
        val userEmail = auth.currentUser?.email ?: "unknown"
        
        // Encrypt message content for Firestore
        val encryptedContent = EncryptionUtils.encrypt(content, chatId)
        
        val message = Message(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = userEmail,
            content = encryptedContent,
            timestamp = System.currentTimeMillis(),
            status = com.example.chader.data.model.MessageStatus.SENT
        )
        
        // Save message to Firestore sub-collection
        val chatRef = firestore.collection("chats").document(chatId)
        val messageRef = chatRef.collection("messages").document(message.id)
        
        firestore.runBatch { batch ->
            batch.set(messageRef, message)
            // Update the chat document with last message info and increment unread count
            batch.update(chatRef, mapOf(
                "lastMessageId" to message.id,
                "lastMessageContent" to encryptedContent,
                "lastMessageTimestamp" to message.timestamp,
                "lastMessageSenderId" to message.senderId,
                "unreadCount" to com.google.firebase.firestore.FieldValue.increment(1)
            ))
        }.await()

        // Cache locally as PLAIN TEXT for easier reading/searching
        chatDao.insertMessage(message.copy(content = content))
    }

    suspend fun markMessagesAsSeen(chatId: String, myEmail: String) {
        try {
            val messagesRef = firestore.collection("chats").document(chatId).collection("messages")
            val unseenMessages = messagesRef
                .whereNotEqualTo("senderId", myEmail)
                .get()
                .await()
            
            firestore.runBatch { batch ->
                var updatedCount = 0
                unseenMessages.documents.forEach { doc ->
                    val status = doc.getString("status")
                    if (status != "SEEN") {
                        batch.update(doc.reference, "status", "SEEN")
                        updatedCount++
                    }
                }
                
                // Reset unread count in the main chat document if we found unseen messages
                if (updatedCount > 0) {
                    batch.update(firestore.collection("chats").document(chatId), "unreadCount", 0)
                }
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun createOrUpdateUser(user: User) {
        firestore.collection("users").document(user.id).set(user).await()
        chatDao.insertUser(user)
    }

    suspend fun updateUsername(userId: String, newUsername: String): Result<Unit> {
        val trimmedUsername = newUsername.trim().lowercase().removePrefix("@")
        
        if (trimmedUsername.isEmpty()) {
            return Result.failure(Exception("Le pseudo ne peut pas être vide"))
        }
        
        if (!trimmedUsername.matches(Regex("^[a-z0-9_]{3,20}$"))) {
            return Result.failure(Exception("Le pseudo doit contenir entre 3 et 20 caractères (lettres, chiffres, _)"))
        }

        return try {
            // Check if username is already taken
            val existing = firestore.collection("users")
                .whereEqualTo("username", trimmedUsername)
                .get()
                .await()
            
            val alreadyTaken = existing.documents.any { it.id != userId }
            if (alreadyTaken) {
                return Result.failure(Exception("Ce pseudo est déjà utilisé par un autre utilisateur"))
            }

            firestore.collection("users").document(userId)
                .update("username", trimmedUsername)
                .await()
            
            // Update local cache for immediate feedback
            chatDao.getUserById(userId)?.let { user ->
                chatDao.insertUser(user.copy(username = trimmedUsername))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
