package com.example.chader.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface HushRoute : NavKey

@Serializable
data object LoginRoute : HushRoute

@Serializable
data object HomeRoute : HushRoute

@Serializable
data object ProfileRoute : HushRoute

@Serializable
data object SettingsRoute : HushRoute

@Serializable
data class ChatRoute(val chatId: String) : HushRoute
