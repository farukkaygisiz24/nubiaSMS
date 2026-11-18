package com.nubiasms.messages.update

import org.json.JSONObject

object UpdateJsonParser {
    fun parse(json: String): Pair<Int, String>? {
        return try {
            val obj = JSONObject(json)
            if (!obj.has("versionCode") || !obj.has("versionName")) return null
            val code = obj.getInt("versionCode")
            val name = obj.getString("versionName")
            if (name.isBlank()) null else code to name
        } catch (_: Exception) {
            null
        }
    }
}
