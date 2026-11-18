package com.nubiasms.messages.update

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkDownloadUrl: String,
)

enum class UpdatePromptKind {
    None,
    Soft,
    Insistent,
}
