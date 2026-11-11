package com.aramalr.messages.sms

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager

class SmsSender(private val context: Context) {
    fun send(
        address: String,
        body: String,
        subscriptionId: Int? = null,
    ): Result<Unit> =
        runCatching {
            val smsManager = smsManagerFor(subscriptionId)
            val sent =
                PendingIntent.getBroadcast(
                    context,
                    address.hashCode(),
                    Intent("com.aramalr.messages.SMS_SENT"),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            smsManager.sendTextMessage(address, null, body, sent, null)
            val values =
                android.content.ContentValues().apply {
                    put(android.provider.Telephony.Sms.ADDRESS, address)
                    put(android.provider.Telephony.Sms.BODY, body)
                    put(android.provider.Telephony.Sms.DATE, System.currentTimeMillis())
                    put(android.provider.Telephony.Sms.READ, 1)
                    put(android.provider.Telephony.Sms.TYPE, android.provider.Telephony.Sms.MESSAGE_TYPE_SENT)
                }
            context.contentResolver.insert(android.provider.Telephony.Sms.Sent.CONTENT_URI, values)
        }

    private fun smsManagerFor(subscriptionId: Int?): SmsManager {
        val subId =
            subscriptionId
                ?: SubscriptionManager.getDefaultSmsSubscriptionId().takeIf { it != SubscriptionManager.INVALID_SUBSCRIPTION_ID }
        return if (subId != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java).createForSubscriptionId(subId)
        } else if (subId != null) {
            @Suppress("DEPRECATION")
            SmsManager.getSmsManagerForSubscriptionId(subId)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
    }
}
