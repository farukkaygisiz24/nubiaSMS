package com.aramalr.messages.sms

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.aramalr.messages.evidence.OtpEvidenceEvent
import com.aramalr.messages.evidence.OtpEvidenceLog
import com.aramalr.messages.notify.SmsNotifier
import com.aramalr.messages.otp.LatestOtpStore
import com.aramalr.messages.otp.OtpDetector
import java.io.File

class IncomingSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (parts.isEmpty()) return

        val address = parts.first().displayOriginatingAddress ?: return
        val body = parts.joinToString(separator = "") { it.displayMessageBody ?: "" }
        val timestamp = parts.first().timestampMillis

        val values =
            ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, timestamp)
                put(Telephony.Sms.READ, 0)
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
            }
        context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)

        val match = OtpDetector.detect(body)
        if (match != null) {
            LatestOtpStore.publish(match, address, timestamp)
        }

        OtpEvidenceLog(File(context.filesDir, "evidence")).append(
            OtpEvidenceEvent(
                timestampMillis = timestamp,
                sender = address,
                body = body,
                detectedCode = match?.code,
                confidence = match?.confidence,
                assistPath = "pending",
            ),
        )

        SmsNotifier(context).notifyIncoming(
            sender = address,
            body = body,
            notificationId = (timestamp % Int.MAX_VALUE).toInt(),
        )
    }
}
