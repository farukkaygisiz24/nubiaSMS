package com.aramalr.messages.otp

object LatestOtpStore {
    private const val TTL_MS = 5 * 60 * 1000L

    @Volatile
    private var match: OtpMatch? = null

    @Volatile
    private var sender: String? = null

    @Volatile
    private var receivedAtMillis: Long = 0L

    @Synchronized
    fun publish(
        match: OtpMatch,
        sender: String,
        receivedAtMillis: Long,
    ) {
        this.match = match
        this.sender = sender
        this.receivedAtMillis = receivedAtMillis
    }

    @Synchronized
    fun latest(): Pair<OtpMatch, Long>? {
        val m = match ?: return null
        if (System.currentTimeMillis() - receivedAtMillis > TTL_MS) {
            clear()
            return null
        }
        return m to receivedAtMillis
    }

    @Synchronized
    fun clear() {
        match = null
        sender = null
        receivedAtMillis = 0L
    }
}
