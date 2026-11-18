package com.nubiasms.messages.update

object UpdatePolicy {
    fun kind(installedVersionCode: Int, latestVersionCode: Int): UpdatePromptKind {
        val lag = latestVersionCode - installedVersionCode
        return when {
            lag <= 0 -> UpdatePromptKind.None
            lag == 1 -> UpdatePromptKind.Soft
            else -> UpdatePromptKind.Insistent
        }
    }

    /** Soft dismissed for this latest code → hide Soft; Insistent always shows. */
    fun displayKind(
        policyKind: UpdatePromptKind,
        latestVersionCode: Int,
        softDismissedForVersionCode: Int,
    ): UpdatePromptKind {
        if (policyKind == UpdatePromptKind.Soft && softDismissedForVersionCode == latestVersionCode) {
            return UpdatePromptKind.None
        }
        return policyKind
    }
}
