package com.example.chader.util

import android.util.Base64
import org.json.JSONObject

object JwtUtils {
    fun getProfilePictureFromToken(idToken: String): String? {
        return try {
            val parts = idToken.split(".")
            if (parts.size < 2) return null
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
            val json = JSONObject(payload)
            json.optString("picture", null)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
