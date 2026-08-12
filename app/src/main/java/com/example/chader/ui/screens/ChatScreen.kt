package com.example.chader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.chader.R
import com.example.chader.data.model.Message
import com.example.chader.data.model.MessageStatus
import com.example.chader.ui.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    viewModel: ChatViewModel,
    myEmail: String,
    onBack: () -> Unit
) {
    val chats by viewModel.chats.collectAsState()
    val chat = remember(chats, chatId) { chats.find { it.id == chatId } }
    val encryptionKey = chat?.encryptionKey

    // Update last seen when entering the chat
    LaunchedEffect(chatId) {
        val myUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (myUserId != null) {
            viewModel.updateLastSeen(myUserId)
        }
    }

    val messages by viewModel.getMessages(chatId, encryptionKey).collectAsState(initial = emptyList())
    val users by viewModel.users.collectAsState()
    val otherUser = remember(users, chatId, myEmail) {
        val otherEmail = chatId.split("_").find { it != myEmail }
        users.find { it.email == otherEmail }
    }
    
    var showKeyDialog by remember { mutableStateOf(false) }
    var newKeyText by remember { mutableStateOf(encryptionKey ?: "") }
    
    // Mark messages as seen when entering the chat or receiving new ones
    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            val hasUnseen = messages.any { it.senderId != myEmail && it.status != "SEEN" }
            if (hasUnseen) {
                viewModel.markAsSeen(chatId, myEmail)
            }
        }
    }
    
    var textState by remember { mutableStateOf("") }
    var editingMessage by remember { mutableStateOf<Message?>(null) }
    var messageToDelete by remember { mutableStateOf<Message?>(null) }
    val listState = rememberLazyListState()

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (messageToDelete != null) {
        AlertDialog(
            onDismissRequest = { messageToDelete = null },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.delete_message_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        messageToDelete?.let { viewModel.deleteMessage(chatId, it.id) }
                        messageToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { messageToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { 
                    val displayName = when {
                        otherUser?.username?.isNotEmpty() == true -> otherUser.username
                        otherUser?.name?.isNotEmpty() == true -> otherUser.name
                        else -> chatId.split("_").find { it != myEmail } ?: "Chat"
                    }
                    Column {
                        Text(displayName, style = MaterialTheme.typography.titleMedium)
                        otherUser?.let {
                            val statusText = if (it.status == "En ligne") {
                                stringResource(R.string.online)
                            } else {
                                val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                                stringResource(R.string.last_seen) + " ${sdf.format(Date(it.lastSeen))}"
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (it.status == "En ligne") Color.Green else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        newKeyText = encryptionKey ?: ""
                        showKeyDialog = true 
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.encryption_key))
                    }
                }
            )
        },
        bottomBar = {
            Column {
                if (editingMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Settings, // Using settings as a fallback for "Edit"
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.edit_message),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = editingMessage?.content ?: "",
                                    maxLines = 1,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = { 
                                editingMessage = null 
                                textState = ""
                            }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    }
                }
                BottomAppBar(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.ime),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = textState,
                            onValueChange = { textState = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(stringResource(R.string.type_message)) },
                            shape = RoundedCornerShape(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (textState.isNotBlank()) {
                                    val currentEditing = editingMessage
                                    if (currentEditing != null) {
                                        viewModel.editMessage(chatId, currentEditing.id, textState, encryptionKey)
                                        editingMessage = null
                                    } else {
                                        viewModel.sendMessage(chatId, textState, encryptionKey)
                                    }
                                    textState = ""
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.send))
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                MessageBubble(
                    message = message, 
                    isMe = message.senderId == myEmail,
                    onEditRequest = {
                        editingMessage = message
                        textState = message.content
                    },
                    onDeleteRequest = {
                        messageToDelete = message
                    }
                )
            }
        }
    }

    if (showKeyDialog) {
        AlertDialog(
            onDismissRequest = { showKeyDialog = false },
            title = { Text(stringResource(R.string.encryption_key)) },
            text = {
                Column {
                    Text(stringResource(R.string.enter_key))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newKeyText,
                        onValueChange = { newKeyText = it },
                        label = { Text(stringResource(R.string.key)) },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateEncryptionKey(chatId, newKeyText)
                    showKeyDialog = false
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showKeyDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun MessageBubble(
    message: Message, 
    isMe: Boolean,
    onEditRequest: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
    
    val bubbleShape = if (isMe) {
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    }

    val canModify = remember(message.timestamp, isMe) {
        isMe && (System.currentTimeMillis() - message.timestamp < 30 * 60 * 1000)
    }

    var showOptions by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(message.id) {
                detectTapGestures(
                    onLongPress = {
                        if (isMe) showOptions = true
                    }
                )
            }, 
        contentAlignment = alignment
    ) {
        if (showOptions) {
            DropdownMenu(
                expanded = showOptions,
                onDismissRequest = { showOptions = false }
            ) {
                if (canModify) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit)) },
                        onClick = {
                            showOptions = false
                            onEditRequest()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        showOptions = false
                        onDeleteRequest()
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(bubbleShape)
                .background(bubbleColor)
                .padding(12.dp)
        ) {
            Text(text = message.content, color = textColor)
            
            if (message.isEdited) {
                Text(
                    text = stringResource(R.string.edited),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
            
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.7f)
                )
                if (isMe) {
                    val statusIcon = when (message.status) {
                        "SENDING" -> null
                        "SENT" -> Icons.Default.Done
                        "RECEIVED" -> Icons.Default.DoneAll
                        "SEEN" -> Icons.Default.DoneAll
                        else -> null
                    }
                    val statusTint = if (message.status == "SEEN") {
                        Color.Cyan
                    } else {
                        textColor.copy(alpha = 0.5f)
                    }

                    if (statusIcon != null) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = statusTint
                        )
                    }
                }
            }
        }
    }
}

