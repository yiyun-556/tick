package com.tick.app.ui.components

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val zone: ZoneId get() = ZoneId.systemDefault()

fun formatTime(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA))

fun formatDateTime(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp).atZone(zone).format(DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.CHINA))

fun formatFullDate(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp).atZone(zone).format(DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA))

fun formatDateHeader(timestamp: Long): String {
    val date = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
    val today = Instant.now().atZone(zone).toLocalDate()
    return when (date) {
        today -> "今天 · ${date.monthValue}月${date.dayOfMonth}日"
        today.minusDays(1) -> "昨天 · ${date.monthValue}月${date.dayOfMonth}日"
        else -> "${date.year}年${date.monthValue}月${date.dayOfMonth}日"
    }
}

fun formatWeekday(timestamp: Long): String {
    val date = Instant.ofEpochMilli(timestamp).atZone(zone)
    val weekdays = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    return "${date.monthValue}月${date.dayOfMonth}日 · ${weekdays[date.dayOfWeek.value - 1]}"
}

fun formatMinutesAgo(timestamp: Long, now: Long): String {
    val minutes = ((now - timestamp) / 60_000L).coerceAtLeast(0L)
    return when {
        minutes < 1 -> "刚刚"
        minutes < 60 -> "$minutes 分钟前"
        minutes < 1440 -> "${minutes / 60} 小时前"
        else -> "${minutes / 1440} 天前"
    }
}
