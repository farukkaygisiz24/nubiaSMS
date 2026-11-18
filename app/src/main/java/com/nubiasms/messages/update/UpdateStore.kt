package com.nubiasms.messages.update

import android.content.SharedPreferences

class UpdateStore(private val prefs: SharedPreferences) {
    var lastInfo: UpdateInfo?
        get() {
            val code = prefs.getInt(KEY_CODE, -1)
            if (code < 0) return null
            val name = prefs.getString(KEY_NAME, null) ?: return null
            val url = prefs.getString(KEY_URL, null) ?: return null
            return UpdateInfo(code, name, url)
        }
        set(value) {
            prefs.edit().apply {
                if (value == null) {
                    remove(KEY_CODE).remove(KEY_NAME).remove(KEY_URL)
                } else {
                    putInt(KEY_CODE, value.versionCode)
                    putString(KEY_NAME, value.versionName)
                    putString(KEY_URL, value.apkDownloadUrl)
                }
            }.apply()
        }

    var softDismissedForVersionCode: Int
        get() = prefs.getInt(KEY_SOFT_DISMISS, 0)
        set(value) = prefs.edit().putInt(KEY_SOFT_DISMISS, value).apply()

    private companion object {
        const val KEY_CODE = "update_last_code"
        const val KEY_NAME = "update_last_name"
        const val KEY_URL = "update_last_url"
        const val KEY_SOFT_DISMISS = "update_soft_dismiss_code"
    }
}
