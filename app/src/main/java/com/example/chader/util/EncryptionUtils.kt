package com.example.chader.util

object EncryptionUtils {
    
    fun encrypt(text: String, key: String?): String {
        if (key.isNullOrEmpty()) return text
        return xor(text, key)
    }

    fun decrypt(encryptedText: String, key: String?): String {
        if (key.isNullOrEmpty()) return encryptedText
        return xor(encryptedText, key)
    }

    private fun xor(input: String, key: String): String {
        val output = StringBuilder()
        for (i in input.indices) {
            output.append((input[i].code xor key[i % key.length].code).toChar())
        }
        return output.toString()
    }
}
