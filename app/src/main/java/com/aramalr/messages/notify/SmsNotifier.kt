package com.aramalr.messages.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aramalr.messages.MainActivity

class SmsNotifier(private val context: Context) {
    fun notifyIncoming(
        sender: String,
        body: String,
        notificationId: Int,
    ) {
        ensureChannel()
        val launch =
            PendingIntent.getActivity(
                context,
                notificationId,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.sym_action_chat)
                .setContentTitle(sender)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(launch)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "Mesajlar",
                NotificationManager.IMPORTANCE_HIGH,
            )
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "aramalr_sms"
    }
}
