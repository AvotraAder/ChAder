package com.example.chader.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface ChAderRoute : NavKey

@Serializable
data object LoginRoute : ChAderRoute

@Serializable
data object HomeRoute : ChAderRoute

@Serializable
data class ChatRoute(val chatId: String) : ChAderRoute

@Serializable
data class StoryRoute(val storyId: String) : ChAderRoute
