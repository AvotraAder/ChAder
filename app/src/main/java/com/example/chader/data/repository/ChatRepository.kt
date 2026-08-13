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
                
                // Sort chats locally by last message timestamp (descending) to avoid requiring a composite index
                val sortedChats = chats.sortedByDescending { it.lastMessageTimestamp ?: 0L }
                
                // Decrypt last message for local storage and display
                val decryptedChats = sortedChats.map { chat ->
                    chat.copy(
                        lastMessageContent = chat.lastMessageContent?.let { 
                            EncryptionUtils.decrypt(it, chat.lastMessageEncryptionKey ?: chat.encryptionKey)
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
    fun syncMessages(chatId: String, currentChatKey: String?): Flow<List<Message>> = callbackFlow {
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
                
                // Decrypt messages content using their own key if available, otherwise fallback to current key
                val decryptedMessages = messages.map { 
                    it.copy(content = EncryptionUtils.decrypt(it.content, it.encryptionKey ?: currentChatKey))
                }

                // Update local cache (optional but recommended)
                repositoryScope.launch {
                    decryptedMessages.forEach { chatDao.insertMessage(it) }
                }

                trySend(decryptedMessages)
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(chatId: String, content: String, encryptionKey: String?) {
        val userEmail = auth.currentUser?.email ?: "unknown"
        
        // Encrypt message content for Firestore
        val encryptedContent = EncryptionUtils.encrypt(content, encryptionKey)
        
        val message = Message(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = userEmail,
            content = encryptedContent,
            timestamp = System.currentTimeMillis(),
            status = "SENT",
            encryptionKey = encryptionKey // Store the key used for this specific message
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
                "lastMessageStatus" to "SENT",
                "lastMessageEncryptionKey" to encryptionKey, // Track key in chat for preview decryption
                "unreadCount" to com.google.firebase.firestore.FieldValue.increment(1)
            ))
        }.await()

        // Cache locally as PLAIN TEXT for easier reading/searching
        chatDao.insertMessage(message.copy(content = content))
    }

    suspend fun markMessagesAsReceived(chatId: String, myEmail: String) {
        try {
            val chatRef = firestore.collection("chats").document(chatId)
            val messagesRef = chatRef.collection("messages")
            
            // Get messages that are still in "SENT" status
            val sentMessages = messagesRef
                .whereEqualTo("status", "SENT")
                .get()
                .await()
            
            if (sentMessages.isEmpty) return

            val chatDoc = chatRef.get().await()
            val lastMessageId = chatDoc.getString("lastMessageId")

            firestore.runBatch { batch ->
                var lastMessageUpdated = false
                sentMessages.documents.forEach { doc ->
                    val senderId = doc.getString("senderId")
                    // Only mark as RECEIVED if I am the receiver
                    if (senderId != myEmail) {
                        batch.update(doc.reference, "status", "RECEIVED")
                        if (doc.id == lastMessageId) {
                            lastMessageUpdated = true
                        }
                    }
                }
                
                if (lastMessageUpdated) {
                    batch.update(chatRef, "lastMessageStatus", "RECEIVED")
                }
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun markMessagesAsSeen(chatId: String, myEmail: String) {
        try {
            val chatRef = firestore.collection("chats").document(chatId)
            val messagesRef = chatRef.collection("messages")
            
            // Get messages that are not yet SEEN
            // To avoid complex indices, we get all messages and filter locally or use status
            // Usually, only SENT and RECEIVED messages need to be marked as SEEN
            val messagesToMark = messagesRef.get().await()
            
            val chatDoc = chatRef.get().await()
            val currentLastMessageStatus = chatDoc.getString("lastMessageStatus")
            val lastMessageSenderId = chatDoc.getString("lastMessageSenderId")

            firestore.runBatch { batch ->
                var updatedCount = 0
                
                messagesToMark.documents.forEach { doc ->
                    val status = doc.getString("status")
                    val senderId = doc.getString("senderId")
                    if (senderId != myEmail && status != "SEEN") {
                        batch.update(doc.reference, "status", "SEEN")
                        updatedCount++
                    }
                }
                
                val updates = mutableMapOf<String, Any>()
                
                // If I am the receiver and the last message is from the other person and not SEEN yet
                if (lastMessageSenderId != myEmail && currentLastMessageStatus != "SEEN") {
                    updates["lastMessageStatus"] = "SEEN"
                    updates["unreadCount"] = 0
                } else if (updatedCount > 0) {
                    updates["unreadCount"] = 0
                }
                
                if (updates.isNotEmpty()) {
                    batch.update(chatRef, updates)
                }
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun createOrUpdateUser(user: User) {
        val userRef = firestore.collection("users").document(user.id)
        val existingUserDoc = userRef.get().await()
        
        if (existingUserDoc.exists()) {
            // Si l'utilisateur existe, on ne met à jour QUE les champs nécessaires
            // On ne touche PAS au username ni à l'avatar s'ils existent déjà
            val updates = mutableMapOf<String, Any>()
            
            val currentUsername = existingUserDoc.getString("username")
            val currentAvatar = existingUserDoc.getString("avatarUrl")
            
            // On ne met à jour le nom et l'email que s'ils ont changé (rare)
            updates["name"] = user.name
            updates["email"] = user.email
            updates["status"] = "En ligne"
            updates["lastSeen"] = System.currentTimeMillis()
            
            // IMPORTANT: On ne remet le username par défaut QUE s'il est vide dans Firestore
            if (currentUsername.isNullOrEmpty()) {
                updates["username"] = user.username
            }
            
            // Pareil pour l'avatar
            if (currentAvatar.isNullOrEmpty() && !user.avatarUrl.isNullOrEmpty()) {
                updates["avatarUrl"] = user.avatarUrl!!
            }
            
            userRef.update(updates).await()
            
            // Mettre à jour le cache local
            val updatedUser = existingUserDoc.toObject(User::class.java)?.copy(
                status = "En ligne",
                lastSeen = System.currentTimeMillis()
            )
            updatedUser?.let { chatDao.insertUser(it) }
        } else {
            // Premier enregistrement
            userRef.set(user).await()
            chatDao.insertUser(user)
        }
    }

    suspend fun updateUserStatus(userId: String, isOnline: Boolean) {
        val status = if (isOnline) "En ligne" else "Hors ligne"
        
        try {
            firestore.collection("users").document(userId)
                .update("status", status)
                .await()
            
            // Update local cache
            chatDao.getUserById(userId)?.let { user ->
                chatDao.insertUser(user.copy(status = status))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateLastSeen(userId: String) {
        val lastSeen = System.currentTimeMillis()
        try {
            firestore.collection("users").document(userId)
                .update("lastSeen", lastSeen)
                .await()
            
            chatDao.getUserById(userId)?.let { user ->
                chatDao.insertUser(user.copy(lastSeen = lastSeen))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateUsername(userId: String, newUsername: String): Result<Unit> {
        val trimmedUsername = newUsername.trim().removePrefix("@")
        
        if (trimmedUsername.isEmpty()) {
            return Result.failure(Exception("Le pseudo ne peut pas être vide"))
        }
        
        if (!trimmedUsername.matches(Regex("^[a-zA-Z0-9_]{3,20}$"))) {
            return Result.failure(Exception("Le pseudo doit contenir entre 3 et 20 caractères (lettres, chiffres, _)"))
        }

        return try {
            // Check if username is already taken (case-insensitive check is better for uniqueness)
            val allUsers = firestore.collection("users").get().await()
            val alreadyTaken = allUsers.documents.any { 
                it.id != userId && it.getString("username")?.equals(trimmedUsername, ignoreCase = true) == true 
            }
            
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
        val trimmedQuery = query.trim()
        return try {
            // Search by email (still lowercase for email usually)
            val emailQuery = firestore.collection("users")
                .whereEqualTo("email", trimmedQuery.lowercase())
                .get()
                .await()
            
            val byEmail = emailQuery.toObjects(User::class.java).firstOrNull()
            if (byEmail != null) return byEmail

            // Search by username (case-insensitive)
            val usernameToSearch = if (trimmedQuery.startsWith("@")) trimmedQuery.substring(1) else trimmedQuery
            
            val allUsers = firestore.collection("users").get().await()
            val byUsername = allUsers.documents.find { 
                it.getString("username")?.equals(usernameToSearch, ignoreCase = true) == true 
            }?.toObject(User::class.java)
            
            return byUsername
        } catch (e: Exception) {
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

    suspend fun editMessage(chatId: String, messageId: String, newContent: String, encryptionKey: String?) {
        val encryptedContent = EncryptionUtils.encrypt(newContent, encryptionKey)
        
        val chatRef = firestore.collection("chats").document(chatId)
        val messageRef = chatRef.collection("messages").document(messageId)

        // Important: Update BOTH the message document and the chat preview document
        firestore.runBatch { batch ->
            batch.update(messageRef, mapOf(
                "content" to encryptedContent,
                "edited" to true,
                "encryptionKey" to encryptionKey
            ))
        }.await()
        
        // Secondary update for the chat preview if it's the last message
        val chatSnapshot = chatRef.get().await()
        if (chatSnapshot.getString("lastMessageId") == messageId) {
            chatRef.update(mapOf(
                "lastMessageContent" to encryptedContent,
                "lastMessageEncryptionKey" to encryptionKey
            )).await()
        }
    }

    suspend fun deleteMessage(chatId: String, messageId: String) {
        val chatRef = firestore.collection("chats").document(chatId)
        val messageRef = chatRef.collection("messages").document(messageId)

        // Get chat doc first to check if it's the last message
        val chatDoc = chatRef.get().await()
        val isLastMessage = chatDoc.getString("lastMessageId") == messageId

        firestore.runBatch { batch ->
            // Delete the message document completely
            batch.delete(messageRef)
            
            if (isLastMessage) {
                batch.update(chatRef, mapOf(
                    "lastMessageId" to null,
                    "lastMessageContent" to null,
                    "lastMessageTimestamp" to null,
                    "lastMessageSenderId" to null,
                    "lastMessageStatus" to null
                ))
            }
        }.await()
    }

    suspend fun updateChatEncryptionKey(chatId: String, newKey: String) {
        firestore.collection("chats").document(chatId)
            .update("encryptionKey", newKey)
            .await()
        
        // Update local cache
        chatDao.getChatById(chatId)?.let { chat ->
            chatDao.insertChat(chat.copy(encryptionKey = newKey))
        }
    }

    suspend fun updateAvatarUrl(userId: String, url: String) {
        try {
            firestore.collection("users").document(userId)
                .update("avatarUrl", url)
                .await()
            
            chatDao.getUserById(userId)?.let { user ->
                chatDao.insertUser(user.copy(avatarUrl = url))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
