package com.example.chader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import coil.compose.AsyncImage
import com.example.chader.R
import com.example.chader.data.datastore.UserSession
import com.example.chader.ui.viewmodel.ChatViewModel
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Person

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    session: UserSession,
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onLanguageChange: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isEditing by remember { mutableStateOf(false) }
    var usernameState by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    
    // Get current user from VM to get the most up-to-date username
    val users by viewModel.users.collectAsState()
    val currentUser = users.find { it.id == session.userId }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            scope.launch {
                isUploading = true
                try {
                    val userId = session.userId ?: throw Exception("Utilisateur non connecté")
                    val storage = FirebaseStorage.getInstance()
                    val storageRef = storage.reference.child("avatars/$userId.jpg")
                    
                    storageRef.putFile(it).await()
                    val downloadUrl = storageRef.downloadUrl.await().toString()
                    
                    viewModel.updateAvatarUrl(userId, downloadUrl)
                    Toast.makeText(context, "Photo mise à jour avec succès", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    val errorMsg = e.localizedMessage ?: "Erreur inconnue"
                    val userFriendlyMsg = when {
                        errorMsg.contains("Permission denied") -> "Accès refusé : Vérifiez les 'Rules' dans la console Firebase Storage."
                        errorMsg.contains("bucket") -> "Erreur de configuration : Le 'Storage' n'est peut-être pas activé dans la console."
                        else -> "Erreur : $errorMsg"
                    }
                    Toast.makeText(context, userFriendlyMsg, Toast.LENGTH_LONG).show()
                } finally {
                    isUploading = false
                }
            }
        }
    }
    
    LaunchedEffect(currentUser) {
        if (!isEditing) {
            usernameState = currentUser?.username ?: ""
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.profile))
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(
                            onClick = { 
                                isSaving = true
                                session.userId?.let { 
                                    viewModel.updateUsername(it, usernameState) { result ->
                                        isSaving = false
                                        result.onSuccess {
                                            isEditing = false
                                            errorMessage = null
                                        }.onFailure { e ->
                                            errorMessage = e.message
                                        }
                                    }
                                }
                            },
                            enabled = !isSaving
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Save, contentDescription = stringResource(R.string.save))
                            }
                        }
                    } else {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                        }
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = stringResource(R.string.logout), tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(enabled = !isUploading) { 
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.Center
            ) {
                if (currentUser?.avatarUrl != null) {
                    AsyncImage(
                        model = currentUser.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(120.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        strokeWidth = 4.dp
                    )
                } else {
                    // Petite icône d'édition sur l'image
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(32.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        tonalElevation = 4.dp
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.change_profile_picture),
                            modifier = Modifier.padding(6.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = currentUser?.name ?: session.userName ?: "Unknown User",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            if (isEditing) {
                OutlinedTextField(
                    value = usernameState,
                    onValueChange = { 
                        usernameState = it 
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.username)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Ce pseudo sera utilisé pour vous trouver.", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                    },
                    enabled = !isSaving
                )
            } else {
                Text(
                    text = if (currentUser?.username?.isNotEmpty() == true) currentUser.username else "Aucun pseudo défini",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (currentUser?.username?.isNotEmpty() == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            Text(
                text = session.userEmail ?: "No email provided",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.language),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    var expanded by remember { mutableStateOf(false) }
                    val languages = listOf("Français" to "fr", "English" to "en")
                    val currentLang = languages.find { it.second == (session.language ?: "fr") }?.first ?: "Français"

                    Box {
                        TextButton(onClick = { expanded = true }) {
                            Text(currentLang)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            languages.forEach { (name, code) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        onLanguageChange(code)
                                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.account_details),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow(label = stringResource(R.string.user_id), value = session.userId ?: "N/A")
                    val provider = if ((session.token?.length ?: 0) > 100) "Google" else "Email"
                    DetailRow(label = stringResource(R.string.connected_via), value = provider)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (session.isDarkMode == true) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.dark_mode),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Switch(
                        checked = session.isDarkMode ?: false,
                        onCheckedChange = onToggleDarkMode
                    )
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
