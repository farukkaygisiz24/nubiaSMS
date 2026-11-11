package com.aramalr.messages.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Stub required for default SMS role; MMS out of v1 product scope. */
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Unit
}
