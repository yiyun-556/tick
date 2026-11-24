package com.tick.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tick.app.data.TickRepository

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != "com.tick.app.action.DAILY_REMINDER") return
        val settings = TickRepository(context).loadSettings()
        TickNotifications.showReminder(context)
        TickNotifications.scheduleReminder(context, settings)
    }
}
