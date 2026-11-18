package com.nubiasms.messages.update

class UpdateChecker(
    private val client: GithubReleaseClient,
    private val store: UpdateStore,
    private val installedVersionCode: Int,
) {
    suspend fun check(): UpdatePromptKind {
        val fetched = client.fetchLatest()
        if (fetched != null) {
            store.lastInfo = fetched
            return UpdatePolicy.kind(installedVersionCode, fetched.versionCode)
        }
        val cached = store.lastInfo ?: return UpdatePromptKind.None
        return UpdatePolicy.kind(installedVersionCode, cached.versionCode)
    }
}
