package com.example.chader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.chader.navigation.ChatRoute
import com.example.chader.navigation.HomeRoute
import com.example.chader.navigation.LoginRoute
import com.example.chader.navigation.StoryRoute
import com.example.chader.ui.screens.ChatScreen
import com.example.chader.ui.screens.HomeScreen
import com.example.chader.ui.screens.LoginScreen
import com.example.chader.ui.screens.StoryScreen
import com.example.chader.ui.theme.ChAderTheme

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chader.data.datastore.UserSessionManager
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.room.Room
import com.example.chader.data.local.AppDatabase
import com.example.chader.data.repository.ChatRepository
import com.example.chader.ui.viewmodel.ChatViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "chader_db"
        ).fallbackToDestructiveMigration().build()
        
        val repository = ChatRepository(database.chatDao())
        val userSessionManager = UserSessionManager(applicationContext)

        enableEdgeToEdge()
        setContent {
            ChAderTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    ChAderApp(userSessionManager, repository)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ChAderApp(userSessionManager: UserSessionManager, repository: ChatRepository) {
    val chatViewModel: ChatViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatViewModel(repository) as T
            }
        }
    )
    
    val userToken by userSessionManager.userToken.collectAsStateWithLifecycle(initialValue = null)
    val backStack = rememberNavBackStack(LoginRoute)
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()
    val scope = rememberCoroutineScope()

    LaunchedEffect(userToken) {
        if (userToken != null && backStack.lastOrNull() == LoginRoute) {
            backStack.clear()
            backStack.add(HomeRoute)
        }
    }

    val entryProvider = entryProvider<NavKey> {
        entry(LoginRoute) {
            LoginScreen(
                onLoginSuccess = {
                    scope.launch {
                        userSessionManager.saveSession("dummy_token", "user_123", "John Doe")
                    }
                }
            )
        }
        entry<HomeRoute>(
            metadata = ListDetailSceneStrategy.listPane()
        ) {
            HomeScreen(
                viewModel = chatViewModel,
                onChatClick = { chatId -> backStack.add(ChatRoute(chatId)) },
                onStoryClick = { storyId -> backStack.add(StoryRoute(storyId)) }
            )
        }
        entry<ChatRoute>(
            metadata = ListDetailSceneStrategy.detailPane()
        ) { route ->
            ChatScreen(
                chatId = route.chatId,
                viewModel = chatViewModel,
                onBack = { backStack.removeAt(backStack.lastIndex) }
            )
        }
        entry<StoryRoute> { route ->
            StoryScreen(
                storyId = route.storyId,
                viewModel = chatViewModel,
                onClose = { backStack.removeAt(backStack.lastIndex) }
            )
        }
    }

    NavDisplay(
        backStack = backStack,
        sceneStrategy = listDetailStrategy,
        entryProvider = entryProvider
    )
}
