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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.chader.data.model.Chat
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
            title = { Text("New Chat") },
            text = {
                Column {
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { 
                            emailInput = it 
                            errorMessage = null
                        },
                        label = { Text("Email or Username") },
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
                    Text("Start Chat")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false },
                    enabled = !isSearching
                ) { Text("Cancel") }
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
                            text = "@${currentUser.username}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = "Définir un pseudo",
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
                    label = { Text("Messages") },
                    selected = true,
                    onClick = {
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("Profile") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onProfileClick()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
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
                    label = { Text("Logout", color = MaterialTheme.colorScheme.error) },
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
                            Icon(Icons.Default.Person, contentDescription = "Profile", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
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
                    Icon(Icons.Default.Chat, contentDescription = "New Chat")
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
                            otherUser?.username?.isNotEmpty() == true -> "@${otherUser.username}"
                            otherUser?.name?.isNotEmpty() == true -> otherUser.name
                            else -> otherUserId ?: "Unknown User"
                        }
                        
                        ListItem(
                            headlineContent = { Text(displayName, fontWeight = FontWeight.SemiBold) },
                            supportingContent = { 
                                val prefix = when {
                                    chat.lastMessageSenderId == null -> ""
                                    chat.lastMessageSenderId == myEmail -> "Vous : "
                                    else -> {
                                        val sender = users.find { it.email == chat.lastMessageSenderId }
                                        if (sender?.username?.isNotEmpty() == true) "${sender.username} : "
                                        else if (sender?.name?.isNotEmpty() == true) "${sender.name} : "
                                        else ""
                                    }
                                }
                                val isUnread = chat.unreadCount > 0 && chat.lastMessageSenderId != myEmail
                                Text(
                                    text = "$prefix${chat.lastMessageContent ?: otherUser?.status ?: "Pas encore de messages"}",
                                    maxLines = 1,
                                    color = if (isUnread) MaterialTheme.colorScheme.onSurface else if (chat.lastMessageContent != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
                                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                                    style = if (isUnread) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall
                                )
                            },
                            leadingContent = {
                                AsyncImage(
                                    model = otherUser?.avatarUrl ?: "https://i.pravatar.cc/150",
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                )
                            },
                            trailingContent = {
                                if (chat.unreadCount > 0) {
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
