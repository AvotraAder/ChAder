package com.example.chader.util

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionUtils {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    
    // In a real app, this key should be derived securely or fetched from a KeyStore
    // For this example, we use a consistent key based on the chatId to simulate "room encryption"
    private fun getSecretKey(chatId: String): SecretKeySpec {
        val keyBytes = chatId.padEnd(32, '0').substring(0, 32).toByteArray()
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun getIv(chatId: String): IvParameterSpec {
        val ivBytes = chatId.reversed().padEnd(16, '0').substring(0, 16).toByteArray()
        return IvParameterSpec(ivBytes)
    }

    fun encrypt(text: String, chatId: String): String {
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(chatId), getIv(chatId))
            val encrypted = cipher.doFinal(text.toByteArray())
            Base64.encodeToString(encrypted, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            text // Fallback to plain text if encryption fails
        }
    }

    fun decrypt(encryptedText: String, chatId: String): String {
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(chatId), getIv(chatId))
            val decoded = Base64.decode(encryptedText, Base64.DEFAULT)
            val decrypted = cipher.doFinal(decoded)
            String(decrypted)
        } catch (e: Exception) {
            // If decryption fails, it might be that the message wasn't encrypted
            encryptedText
        }
    }
}
