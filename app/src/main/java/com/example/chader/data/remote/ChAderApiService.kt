package com.example.chader.data.remote

import com.example.chader.data.model.Chat
import com.example.chader.data.model.Message
import com.example.chader.data.model.Story
import com.example.chader.data.model.User
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ChAderApiService {
    @POST("auth/login")
    suspend fun login(@Body credentials: LoginRequest): User

    @GET("chats")
    suspend fun getChats(): List<Chat>

    @GET("chats/{chatId}/messages")
    suspend fun getMessages(@Path("chatId") chatId: String): List<Message>

    @POST("chats/{chatId}/messages")
    suspend fun sendMessage(@Path("chatId") chatId: String, @Body message: Message): Message

    @GET("stories")
    suspend fun getStories(): List<Story>
}

data class LoginRequest(
    val email: String,
    val token: String
)
