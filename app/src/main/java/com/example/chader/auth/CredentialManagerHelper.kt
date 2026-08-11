package com.example.chader.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CredentialManagerHelper(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun getGoogleCredential(webClientId: String): GoogleIdTokenCredential? = withContext(Dispatchers.IO) {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false) // Force account selection picker
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val result = credentialManager.getCredential(context, request)
            handleSignIn(result)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun handleSignIn(result: GetCredentialResponse): GoogleIdTokenCredential? {
        val credential = result.credential
        return if (credential is GoogleIdTokenCredential) {
            credential
        } else if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                GoogleIdTokenCredential.createFrom(credential.data)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
}
