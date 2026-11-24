package com.tick.app.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.tick.app.MainActivity
import com.tick.app.R
import com.tick.app.data.EventType
import com.tick.app.data.StatsCalculator
import com.tick.app.data.TickEvent
import com.tick.app.data.TickRepository
import java.time.ZonedDateTime
import java.util.UUID

object TickNotifications {
    const val ACTION_DATA_CHANGED = "com.tick.app.action.DATA_CHANGED"
    const val ACTION_CHECK_IN = "com.tick.app.action.CHECK_IN"

    private const val CHANNEL_LIVE = "tick_live"
    private const val CHANNEL_REMINDER = "tick_reminder"
    private const val CHANNEL_EVENTS = "tick_events"
    private const val CHANNEL_UPDATES = "tick_updates"
    private const val LIVE_ID = 7001
    private const val REMINDER_ID = 7002
    private const val CONFIRM_ID = 7003
    private const val UPDATE_ID = 7004
    private const val REQ_OPEN = 41
    private const val REQ_CHECK_IN = 42
    private const val REQ_REMINDER = 43
    private const val ACCENT = 0xFF3482FF.toInt()

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val live = NotificationChannel(
            CHANNEL_LIVE,
            "实时活动",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "在通知栏显示已稳住计时与今日打卡状态"
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
        val reminder = NotificationChannel(
            CHANNEL_REMINDER,
            "打卡提醒",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "每天在你选择的时间提醒打卡"
            setShowBadge(true)
        }
        val events = NotificationChannel(
            CHANNEL_EVENTS,
            "打卡记录",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "打卡成功后的轻量确认"
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        val updates = NotificationChannel(
            CHANNEL_UPDATES,
            "版本更新",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "每天最多检查一次新版本"
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        manager.createNotificationChannels(listOf(live, reminder, events, updates))
    }

    fun canPost(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        if (!manager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return true
    }

    fun syncAll(context: Context) {
        ensureChannels(context)
        val repo = TickRepository(context)
        val settings = repo.loadSettings()
        if (!canPost(context)) {
            cancelLive(context)
            cancelReminder(context)
            return
        }
        if (settings.notifications.liveUpdate) {
            postLive(context, repo, settings)
        } else {
            cancelLive(context)
        }
        scheduleReminder(context, settings)
    }

    fun syncLive(context: Context) {
        ensureChannels(context)
        val repo = TickRepository(context)
        val settings = repo.loadSettings()
        if (!canPost(context) || !settings.notifications.liveUpdate) {
            cancelLive(context)
            return
        }
        postLive(context, repo, settings)
    }

    fun showUpdateAvailable(context: Context, versionName: String) {
        if (!canPost(context)) return
        ensureChannels(context)
        val builder = Notification.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("刻度 $versionName 可用")
            .setContentText("打开设置 → 关于与更新 查看并安装。")
            .setContentIntent(openIntent(context))
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_RECOMMENDATION)
            .setColor(ACCENT)
        context.getSystemService(NotificationManager::class.java)
            ?.notify(UPDATE_ID, builder.build())
    }

    fun showCheckInConfirmation(context: Context) {
        if (!canPost(context)) return
        ensureChannels(context)
        val repo = TickRepository(context)
        val settings = repo.loadSettings()
        if (!settings.notifications.checkInConfirmation) return
        val events = repo.loadEvents()
        val journeyStart = repo.loadJourneyStart()
        val stats = StatsCalculator.calculate(events, journeyStart)
        val builder = Notification.Builder(context, CHANNEL_EVENTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("今天已稳住")
            .setContentText("已累计打卡 ${stats.totalCheckIns} 天。记下这一格，不用声张。")
            .setContentIntent(openIntent(context))
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_STATUS)
            .setColor(ACCENT)
        context.getSystemService(NotificationManager::class.java)
            ?.notify(CONFIRM_ID, builder.build())
    }

    fun showReminder(context: Context) {
        if (!canPost(context)) return
        ensureChannels(context)
        val repo = TickRepository(context)
        val settings = repo.loadSettings()
        if (!settings.notifications.dailyReminder) return
        val events = repo.loadEvents()
        val now = System.currentTimeMillis()
        if (events.any { it.type == EventType.CHECK_IN && StatsCalculator.sameDay(it.at, now) }) return

        val builder = Notification.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("今天还没打卡")
            .setContentText("给自己留一格。哪怕只是稳稳地过完今天。")
            .setContentIntent(openIntent(context))
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setColor(ACCENT)
            .addAction(checkInAction(context))
        context.getSystemService(NotificationManager::class.java)
            ?.notify(REMINDER_ID, builder.build())
    }

    fun scheduleReminder(context: Context, settings: com.tick.app.data.AppSettings = TickRepository(context).loadSettings()) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = reminderIntent(context)
        alarmManager.cancel(pendingIntent)
        if (!settings.notifications.dailyReminder || !canPost(context)) return

        val now = ZonedDateTime.now()
        var next = now
            .withHour(settings.notifications.reminderHour)
            .withMinute(settings.notifications.reminderMinute)
            .withSecond(0)
            .withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        // Inexact one-shot alarms avoid the special exact-alarm permission and
        // are kind to the battery; the receiver schedules the next day.
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            next.toInstant().toEpochMilli(),
            pendingIntent,
        )
    }

    fun cancelReminder(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(reminderIntent(context))
    }

    fun cancelAll(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.cancel(LIVE_ID)
        manager?.cancel(REMINDER_ID)
        manager?.cancel(CONFIRM_ID)
        cancelReminder(context)
    }

    fun addCheckInFromNotification(context: Context): Boolean {
        val repo = TickRepository(context)
        val now = System.currentTimeMillis()
        val events = repo.loadEvents()
        if (events.any { it.type == EventType.CHECK_IN && StatsCalculator.sameDay(it.at, now) }) {
            syncLive(context)
            return false
        }
        val event = TickEvent(
            id = UUID.randomUUID().toString(),
            type = EventType.CHECK_IN,
            at = now,
        )
        repo.saveEvents((events + event).sortedBy { it.at })
        syncAll(context)
        showCheckInConfirmation(context)
        context.sendBroadcast(Intent(ACTION_DATA_CHANGED).setPackage(context.packageName))
        return true
    }

    private fun postLive(
        context: Context,
        repo: TickRepository,
        settings: com.tick.app.data.AppSettings,
    ) {
        val events = repo.loadEvents()
        val journeyStart = repo.loadJourneyStart()
        val stats = StatsCalculator.calculate(events, journeyStart)
        val percent = (stats.targetProgress * 100).toInt().coerceIn(0, 100)
        val builder = Notification.Builder(context, CHANNEL_LIVE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("刻度 · 已稳住")
            .setContentText("${stats.streakDays} 天 ${stats.streakHours} 小时 ${stats.streakMinutes} 分钟")
            .setSubText(if (stats.checkedInToday) "今日已打卡" else "今天还没打卡")
            .setContentIntent(openIntent(context))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setWhen(stats.streakStart)
            .setUsesChronometer(true)
            .setChronometerCountDown(false)
            .setColor(ACCENT)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setCategory(if (Build.VERSION.SDK_INT >= 36) Notification.CATEGORY_PROGRESS else Notification.CATEGORY_STOPWATCH)

        if (!stats.checkedInToday) {
            builder.addAction(checkInAction(context))
        }
        if (Build.VERSION.SDK_INT >= 36) {
            applyLiveUpdateStyle(builder, context, percent, stats)
        } else {
            builder.setProgress(100, percent, false)
        }
        context.getSystemService(NotificationManager::class.java)
            ?.notify(LIVE_ID, builder.build())
    }

    @RequiresApi(36)
    private fun applyLiveUpdateStyle(
        builder: Notification.Builder,
        context: Context,
        percent: Int,
        stats: com.tick.app.data.TickStats,
    ) {
        val style = Notification.ProgressStyle()
            .setProgress(percent)
            .addProgressSegment(Notification.ProgressStyle.Segment(100).setColor(ACCENT))
            .setProgressTrackerIcon(Icon.createWithResource(context, R.drawable.ic_notification))
            .setStyledByProgress(false)
        builder.setStyle(style)
        builder.setShortCriticalText("${stats.streakDays}天${stats.streakHours}时")
    }

    private fun checkInAction(context: Context): Notification.Action {
        val intent = Intent(context, CheckInActionReceiver::class.java).setAction(ACTION_CHECK_IN)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQ_CHECK_IN,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Action.Builder(
            Icon.createWithResource(context, R.drawable.ic_notification),
            "打卡",
            pendingIntent,
        ).build()
    }

    private fun openIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "com.tick.app.action.OPEN"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            REQ_OPEN,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun reminderIntent(context: Context): PendingIntent {
        val intent = Intent(context, DailyReminderReceiver::class.java).setAction("com.tick.app.action.DAILY_REMINDER")
        return PendingIntent.getBroadcast(
            context,
            REQ_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun cancelLive(context: Context) {
        context.getSystemService(NotificationManager::class.java)?.cancel(LIVE_ID)
    }
}
