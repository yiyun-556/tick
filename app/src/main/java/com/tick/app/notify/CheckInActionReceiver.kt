package com.tick.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CheckInActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != TickNotifications.ACTION_CHECK_IN) return
        TickNotifications.addCheckInFromNotification(context)
    }
}
