package com.example.chader

import android.os.Bundle
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.room.Room
import com.example.chader.auth.CredentialManagerHelper
import com.example.chader.data.datastore.UserSession
import com.example.chader.data.datastore.UserSessionManager
import com.example.chader.data.local.AppDatabase
import com.example.chader.data.model.User
import com.example.chader.data.repository.ChatRepository
import com.example.chader.navigation.ChatRoute
import com.example.chader.navigation.HomeRoute
import com.example.chader.navigation.LoginRoute
import com.example.chader.navigation.ProfileRoute
import com.example.chader.ui.screens.ChatScreen
import com.example.chader.ui.screens.HomeScreen
import com.example.chader.ui.screens.LoginScreen
import com.example.chader.ui.screens.ProfileScreen
import com.example.chader.ui.theme.ChAderTheme
import com.example.chader.ui.viewmodel.ChatViewModel
import com.example.chader.util.JwtUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {
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
        val credentialManagerHelper = CredentialManagerHelper(this)

        enableEdgeToEdge()

        setContent {
            // Pre-calculate current system language to avoid "null" or "default" flash
            val currentSystemLang = remember { 
                AppCompatDelegate.getApplicationLocales().toLanguageTags().ifEmpty { "fr" } 
            }
            
            val userSessionState by userSessionManager.userSession.collectAsStateWithLifecycle(
                initialValue = UserSession(null, null, null, null, null, currentSystemLang)
            )
            
            val userSession = userSessionState!!
            val isDarkTheme = userSession.isDarkMode ?: isSystemInDarkTheme()
            val lang = userSession.language ?: currentSystemLang

            // Sync with AppCompatDelegate (system level language)
            LaunchedEffect(lang) {
                val currentAppLocales = AppCompatDelegate.getApplicationLocales().toLanguageTags()
                if (currentAppLocales != lang) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
                }
            }

            // Sync Status Bar
            DisposableEffect(isDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkTheme) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                                     else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                )
                onDispose { }
            }

            ChAderTheme(darkTheme = isDarkTheme, dynamicColor = false) {
                CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides this@MainActivity) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        ChAderApp(userSessionManager, repository, credentialManagerHelper)
                    }
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
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(repository) as T
            }
        }
    )
    
    val userSessionState by userSessionManager.userSession.collectAsStateWithLifecycle(
        initialValue = null
    )
    val userSession = userSessionState ?: return

    val backStack = rememberNavBackStack(LoginRoute)
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(userSession.userId, lifecycleOwner) {
        val userId = userSession.userId
        val observer = LifecycleEventObserver { _, event ->
            if (userId == null) return@LifecycleEventObserver
            if (event == Lifecycle.Event.ON_START) {
                chatViewModel.setUserStatus(userId, true)
            } else if (event == Lifecycle.Event.ON_STOP) {
                chatViewModel.setUserStatus(userId, false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            userId?.let { chatViewModel.setUserStatus(it, false) }
        }
    }

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
                            Toast.makeText(context, "Erreur de connexion", Toast.LENGTH_LONG).show()
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
                                    
                                    // Reload user to ensure profile data (like photoUrl) is synced from Google
                                    try { user.reload().await() } catch (e: Exception) { e.printStackTrace() }
                                    val updatedUser = FirebaseAuth.getInstance().currentUser ?: user
                                    
                                    // 4 sources pour la photo de profil
                                    val jwtPhotoUrl = JwtUtils.getProfilePictureFromToken(credential.idToken)
                                    val avatarUrl = updatedUser.photoUrl?.toString() 
                                        ?: jwtPhotoUrl
                                        ?: credential.profilePictureUri?.toString()
                                        ?: updatedUser.providerData.find { it.providerId == "google.com" }?.photoUrl?.toString()
                                    
                                    if (avatarUrl == null) {
                                        Toast.makeText(context, "Note: Photo de profil Google non détectée", Toast.LENGTH_SHORT).show()
                                    }
                                    
                                    userSessionManager.saveSession(credential.idToken, id, userName, userEmail)
                                    repository.createOrUpdateUser(
                                        User(
                                            id = id,
                                            name = userName,
                                            email = userEmail,
                                            username = username,
                                            avatarUrl = avatarUrl,
                                            status = "En ligne"
                                        )
                                    )
                                }
                            } else {
                                Toast.makeText(context, "Échec de la récupération du compte", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Erreur Google: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
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
                        userSession.userId?.let { chatViewModel.setUserStatus(it, false) }
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
                viewModel = chatViewModel,
                onBack = { backStack.removeAt(backStack.lastIndex) },
                onLogout = {
                    scope.launch {
                        userSession.userId?.let { chatViewModel.setUserStatus(it, false) }
                        FirebaseAuth.getInstance().signOut()
                        userSessionManager.clearSession()
                        backStack.clear()
                        backStack.add(LoginRoute)
                    }
                },
                onToggleDarkMode = { isDark ->
                    scope.launch {
                        userSessionManager.setDarkMode(isDark)
                    }
                },
                onLanguageChange = { lang ->
                    scope.launch {
                        userSessionManager.setLanguage(lang)
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
