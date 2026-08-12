package com.example.chader.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import com.example.chader.R
import com.example.chader.data.model.Chat
import com.example.chader.data.model.MessageStatus
import com.example.chader.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ChatViewModel,
    myEmail: String,
    onChatClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val chats by viewModel.chats.collectAsStateWithLifecycle()
    val users by viewModel.users.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Mark incoming messages as RECEIVED when the chat list is viewed
    LaunchedEffect(chats) {
        chats.forEach { chat ->
            if (chat.lastMessageSenderId != myEmail && chat.lastMessageStatus == "SENT") {
                viewModel.markAsReceived(chat.id, myEmail)
            }
        }
    }
    
    val currentUser = users.find { it.email == myEmail }
    
    LaunchedEffect(myEmail) {
        viewModel.setMyEmail(myEmail)
    }

    var showDialog by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }

    if (showDialog) {
        var isSearching by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isSearching) showDialog = false },
            title = { Text(stringResource(R.string.new_chat)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { 
                            emailInput = it 
                            errorMessage = null
                        },
                        label = { Text(stringResource(R.string.email_or_username)) },
                        placeholder = { Text("friend@example.com or @pseudo") },
                        isError = errorMessage != null,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSearching
                    )
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                    }
                    if (isSearching) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (emailInput.isBlank()) {
                            errorMessage = "Please enter an email or username"
                            return@Button
                        }
                        isSearching = true
                        viewModel.startChatByQuery(emailInput, myEmail) { chatId ->
                            isSearching = false
                            if (chatId != null) {
                                onChatClick(chatId)
                                showDialog = false
                                emailInput = ""
                            } else {
                                errorMessage = "User not found or connection error"
                            }
                        }
                    },
                    enabled = !isSearching
                ) {
                    Text(stringResource(R.string.start_chat))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false },
                    enabled = !isSearching
                ) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                // Drawer Header with User Info
                Column(
                    modifier = Modifier
                        .padding(horizontal = 28.dp, vertical = 16.dp)
                        .fillMaxWidth()
                ) {
                    AsyncImage(
                        model = currentUser?.avatarUrl ?: "https://i.pravatar.cc/150",
                        contentDescription = null,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = currentUser?.name ?: "User",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (currentUser?.username?.isNotEmpty() == true) {
                        Text(
                            text = currentUser.username,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.define_username),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.clickable { 
                                scope.launch { drawerState.close() }
                                onProfileClick() 
                            }
                        )
                    }
                    Text(
                        text = myEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Chat, contentDescription = null) },
                    label = { Text(stringResource(R.string.messages_tab)) },
                    selected = true,
                    onClick = {
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text(stringResource(R.string.profile)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onProfileClick()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.settings)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        // Future settings logic
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                
                Spacer(Modifier.weight(1f))
                
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    label = { Text(stringResource(R.string.logout), color = MaterialTheme.colorScheme.error) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onLogoutClick()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    title = { 
                        Text(
                            "ChAder", 
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        ) 
                    },
                    actions = {
                        IconButton(onClick = onProfileClick) {
                            Icon(Icons.Default.Person, contentDescription = stringResource(R.string.profile), tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showDialog = true },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Icon(Icons.Default.Chat, contentDescription = stringResource(R.string.new_chat))
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(chats) { chat ->
                        val otherUserId = chat.participantIds.find { it != myEmail }
                        val otherUser = users.find { it.email == otherUserId }
                        val displayName = when {
                            otherUser?.username?.isNotEmpty() == true -> otherUser.username
                            otherUser?.name?.isNotEmpty() == true -> otherUser.name
                            else -> otherUserId ?: "Unknown User"
                        }
                        
                        ListItem(
                            headlineContent = { Text(displayName, fontWeight = FontWeight.SemiBold) },
                            supportingContent = { 
                                val prefix = when {
                                    chat.lastMessageSenderId == null -> ""
                                    chat.lastMessageSenderId == myEmail -> stringResource(R.string.you) + " "
                                    else -> {
                                        val sender = users.find { it.email == chat.lastMessageSenderId }
                                        if (sender?.username?.isNotEmpty() == true) "${sender.username} : "
                                        else if (sender?.name?.isNotEmpty() == true) "${sender.name} : "
                                        else ""
                                    }
                                }
                                val isUnread = chat.unreadCount > 0 && chat.lastMessageSenderId != myEmail
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (chat.lastMessageSenderId == myEmail) {
                                        val statusIcon = when (chat.lastMessageStatus) {
                                            "SENT" -> Icons.Default.Done
                                            "RECEIVED", "SEEN" -> Icons.Default.DoneAll
                                            else -> null
                                        }
                                        val statusTint = if (chat.lastMessageStatus == "SEEN") {
                                            Color(0xFF00B2FF) // Blue for seen
                                        } else {
                                            MaterialTheme.colorScheme.outline
                                        }

                                        if (statusIcon != null) {
                                            Icon(
                                                imageVector = statusIcon,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp).padding(end = 4.dp),
                                                tint = statusTint
                                            )
                                        }
                                    }
                                    val messageToShow = if (chat.lastMessageContent == "Message supprimé") "" else chat.lastMessageContent
                                    Text(
                                        text = "$prefix${messageToShow ?: otherUser?.status ?: stringResource(R.string.no_messages)}",
                                        maxLines = 1,
                                        color = if (isUnread) MaterialTheme.colorScheme.onSurface else if (chat.lastMessageContent != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
                                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                                        style = if (isUnread) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall
                                    )
                                }
                            },
                            leadingContent = {
                                Box {
                                    AsyncImage(
                                        model = otherUser?.avatarUrl ?: "https://i.pravatar.cc/150",
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                    )
                                    if (otherUser?.status == "En ligne") {
                                        Surface(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .align(Alignment.BottomEnd)
                                                .offset(x = (-2).dp, y = (-2).dp),
                                            shape = CircleShape,
                                            color = Color.Green,
                                            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
                                        ) {}
                                    }
                                }
                            },
                            trailingContent = {
                                if (chat.unreadCount > 0 && chat.lastMessageSenderId != myEmail) {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Text(chat.unreadCount.toString())
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onChatClick(chat.id) }
                        )
                    }
                }
            }
        }
    }
}
