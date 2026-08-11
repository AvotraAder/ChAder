package com.example.chader

import android.os.Bundle
import android.widget.Toast
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
import com.example.chader.ui.screens.ChatScreen
import com.example.chader.ui.screens.HomeScreen
import com.example.chader.ui.screens.LoginScreen
import com.example.chader.ui.theme.ChAderTheme
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chader.data.datastore.UserSessionManager
import com.example.chader.data.datastore.UserSession
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
import com.example.chader.data.model.User
import com.example.chader.auth.CredentialManagerHelper
import com.example.chader.navigation.ProfileRoute
import com.example.chader.ui.screens.ProfileScreen
import androidx.compose.ui.platform.LocalContext

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

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
        val credentialManagerHelper = CredentialManagerHelper(applicationContext)

        enableEdgeToEdge()
        setContent {
            ChAderTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    ChAderApp(userSessionManager, repository, credentialManagerHelper)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ChAderApp(
    userSessionManager: UserSessionManager, 
    repository: ChatRepository,
    credentialManagerHelper: CredentialManagerHelper
) {
    val context = LocalContext.current
    val chatViewModel: ChatViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatViewModel(repository) as T
            }
        }
    )
    
    val userSession by userSessionManager.userSession.collectAsStateWithLifecycle(
        initialValue = UserSession(null, null, null, null)
    )
    val backStack = rememberNavBackStack(LoginRoute)
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()
    val scope = rememberCoroutineScope()

    LaunchedEffect(userSession.token) {
        if (userSession.token != null && backStack.lastOrNull() == LoginRoute) {
            backStack.clear()
            backStack.add(HomeRoute)
        }
    }

    val entryProvider = entryProvider<NavKey> {
        entry(LoginRoute) {
            LoginScreen(
                onLoginSuccess = { email, _, name ->
                    scope.launch {
                        try {
                            if (email.isBlank()) {
                                Toast.makeText(context, "Veuillez entrer un email", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            // Connexion automatique simplifiée (Plan Spark)
                            val authResult = try {
                                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, "mot_de_passe_par_defaut").await()
                            } catch (e: Exception) {
                                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, "mot_de_passe_par_defaut").await()
                            }
                            
                                val user = authResult.user
                            if (user != null) {
                                val id = user.uid
                                val username = email.split("@")[0].lowercase()
                                userSessionManager.saveSession("token_spark", id, name, email)
                                repository.createOrUpdateUser(
                                    User(
                                        id = id,
                                        name = name,
                                        email = email,
                                        username = username,
                                        avatarUrl = null,
                                        status = "En ligne"
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Erreur de connexion. Vérifiez votre Internet et le fichier google-services.json", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onGoogleSignInClick = {
                    scope.launch {
                        try {
                            val webClientId = "202231401807-opev3stqhgdnvd66570pcrakv5060ijr.apps.googleusercontent.com"
                            val credential = credentialManagerHelper.getGoogleCredential(webClientId)
                            if (credential != null) {
                                val firebaseCredential = GoogleAuthProvider.getCredential(credential.idToken, null)
                                val authResult = FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).await()
                                
                                val user = authResult.user
                                if (user != null) {
                                    val id = user.uid
                                    val userName = credential.displayName ?: user.displayName ?: "Utilisateur"
                                    val userEmail = user.email ?: credential.id
                                    val username = userEmail.split("@")[0].lowercase()
                                    
                                    userSessionManager.saveSession(credential.idToken, id, userName, userEmail)
                                    repository.createOrUpdateUser(
                                        User(
                                            id = id,
                                            name = userName,
                                            email = userEmail,
                                            username = username,
                                            avatarUrl = credential.profilePictureUri?.toString(),
                                            status = "En ligne"
                                        )
                                    )
                                }
                            } else {
                                Toast.makeText(context, "Annulé ou échec Google.", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Erreur Google : ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }
        entry<HomeRoute>(
            metadata = ListDetailSceneStrategy.listPane()
        ) {
            HomeScreen(
                viewModel = chatViewModel,
                myEmail = userSession.userEmail ?: "",
                onChatClick = { chatId -> backStack.add(ChatRoute(chatId)) },
                onProfileClick = { backStack.add(ProfileRoute) },
                onLogoutClick = {
                    scope.launch {
                        FirebaseAuth.getInstance().signOut()
                        userSessionManager.clearSession()
                        backStack.clear()
                        backStack.add(LoginRoute)
                    }
                }
            )
        }
        entry(ProfileRoute) {
            ProfileScreen(
                session = userSession,
                onBack = { backStack.removeAt(backStack.lastIndex) },
                onLogout = {
                    scope.launch {
                        FirebaseAuth.getInstance().signOut()
                        userSessionManager.clearSession()
                        backStack.clear()
                        backStack.add(LoginRoute)
                    }
                }
            )
        }
        entry<ChatRoute>(
            metadata = ListDetailSceneStrategy.detailPane()
        ) { route ->
            ChatScreen(
                chatId = route.chatId,
                viewModel = chatViewModel,
                myEmail = userSession.userEmail ?: "",
                onBack = { backStack.removeAt(backStack.lastIndex) }
            )
        }
    }

    NavDisplay(
        backStack = backStack,
        sceneStrategy = listDetailStrategy,
        entryProvider = entryProvider
    )
}
