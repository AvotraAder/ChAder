package com.example.chader.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.chader.data.model.Chat
import com.example.chader.ui.components.StoryReel
import com.example.chader.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ChatViewModel,
    onChatClick: (String) -> Unit,
    onStoryClick: (String) -> Unit
) {
    val chats by viewModel.chats.collectAsState(initial = emptyList())
    val stories by viewModel.stories.collectAsState(initial = emptyList())
    val users by viewModel.users.collectAsState(initial = emptyList())

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "ChAder", 
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.seedDemoData() },
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
            if (stories.isNotEmpty()) {
                StoryReel(stories = stories, onStoryClick = onStoryClick)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(chats) { chat ->
                    val otherUserId = chat.participantIds.find { it != "me" }
                    val otherUser = users.find { it.id == otherUserId }
                    
                    ListItem(
                        headlineContent = { Text(otherUser?.name ?: "Unknown User", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text(otherUser?.status ?: "No status", maxLines = 1) },
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
