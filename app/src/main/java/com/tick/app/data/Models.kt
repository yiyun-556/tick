package com.tick.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale
import java.util.UUID
import kotlin.math.max

enum class EventType {
    CHECK_IN,
    RELAPSE,
}

data class TickEvent(
    val id: String = UUID.randomUUID().toString(),
    val type: EventType,
    val at: Long,
    val trigger: String? = null,
    val note: String? = null,
)

enum class PaletteStyle {
    MONET,
    CLASSIC,
}

enum class DarkMode {
    SYSTEM,
    LIGHT,
    DARK,
}

data class AiConfig(
    val presetId: String = "deepseek",
    val baseUrl: String = "https://api.deepseek.com/v1",
    val model: String = "deepseek-chat",
    val apiKey: String = "",
    val aiName: String = "小刻",
    val persona: String = "",
) {
    val hasKey: Boolean get() = apiKey.isNotBlank()
    val isReady: Boolean get() = baseUrl.isNotBlank() && model.isNotBlank() && apiKey.isNotBlank()
}

data class NotificationSettings(
    val liveUpdate: Boolean = true,
    val dailyReminder: Boolean = true,
    val reminderHour: Int = 21,
    val reminderMinute: Int = 0,
    val checkInConfirmation: Boolean = true,
)

enum class DockStyle {
    GLASS,
    BLUR,
    SOLID,
}

data class DockSettings(
    val style: DockStyle = DockStyle.GLASS,
    val blurRadius: Float = 8f,
    val refractionHeight: Float = 24f,
    val refractionAmount: Float = 24f,
    val chromaticAberration: Boolean = true,
    val highlight: Boolean = true,
)

data class AppSettings(
    val palette: PaletteStyle = PaletteStyle.CLASSIC,
    val darkMode: DarkMode = DarkMode.SYSTEM,
    val ai: AiConfig = AiConfig(),
    val notifications: NotificationSettings = NotificationSettings(),
    val dock: DockSettings = DockSettings(),
    val autoUpdateCheck: Boolean = false,
)

enum class ChatRole {
    USER,
    ASSISTANT,
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val text: String,
    val at: Long = System.currentTimeMillis(),
    val local: Boolean = false,
    val error: Boolean = false,
)

data class AiPreset(
    val id: String,
    val label: String,
    val hint: String,
    val baseUrl: String,
    val model: String,
)

val aiPresets: List<AiPreset> = listOf(
    AiPreset("deepseek", "DeepSeek", "性价比高，中文自然", "https://api.deepseek.com/v1", "deepseek-chat"),
    AiPreset("qwen", "通义千问 Qwen", "阿里云百炼兼容接口", "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-plus"),
    AiPreset("kimi", "Kimi / Moonshot", "长上下文稳定", "https://api.moonshot.cn/v1", "moonshot-v1-8k"),
    AiPreset("glm", "智谱 GLM", "响应快，免费额度友好", "https://open.bigmodel.cn/api/paas/v4", "glm-4-flash"),
    AiPreset("openai", "OpenAI", "标准 Chat Completions", "https://api.openai.com/v1", "gpt-4o-mini"),
    AiPreset("custom", "自定义兼容接口", "任意 OpenAI 兼容服务", "", ""),
)

data class TickStats(
    val streakStart: Long,
    val streakMillis: Long,
    val thisMonthCount: Int,
    val averageIntervalMillis: Long?,
    val longestStreakMillis: Long,
    val checkedInToday: Boolean,
    val totalCheckIns: Int,
    val nextTargetMillis: Long,
    val previousTargetMillis: Long,
    val targetProgress: Float,
) {
    val streakDays: Long get() = streakMillis / DAY_MILLIS
    val streakHours: Long get() = (streakMillis % DAY_MILLIS) / HOUR_MILLIS
    val streakMinutes: Long get() = (streakMillis % HOUR_MILLIS) / MINUTE_MILLIS
}

const val MINUTE_MILLIS: Long = 60_000L
const val HOUR_MILLIS: Long = 60 * MINUTE_MILLIS
const val DAY_MILLIS: Long = 24 * HOUR_MILLIS

object StatsCalculator {
    private val targetsHours = longArrayOf(24, 72, 168, 336, 720, 1440, 2160, 4320, 8760, 17520, 26280)

    fun calculate(
        events: List<TickEvent>,
        journeyStart: Long,
        now: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): TickStats {
        val relapses = events
            .asSequence()
            .filter { it.type == EventType.RELAPSE && it.at <= now }
            .sortedBy { it.at }
            .toList()

        val lastRelapse = relapses.lastOrNull()
        val streakStart = lastRelapse?.at ?: journeyStart.coerceAtMost(now)
        val streakMillis = max(0L, now - streakStart)

        val today = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()
        val currentMonth = YearMonth.from(today)
        val monthCount = relapses.count {
            val date = Instant.ofEpochMilli(it.at).atZone(zoneId).toLocalDate()
            YearMonth.from(date) == currentMonth
        }

        val intervals = relapses.zipWithNext { a, b -> b.at - a.at }.filter { it > 0 }
        val average = if (intervals.isEmpty()) null else intervals.average().toLong()
        val longest = max(intervals.maxOrNull() ?: 0L, streakMillis)

        val checkedToday = events.any {
            it.type == EventType.CHECK_IN && sameDay(it.at, now, zoneId)
        }
        val totalCheckIns = events.count { it.type == EventType.CHECK_IN }

        val streakHours = streakMillis.toDouble() / HOUR_MILLIS
        val nextIndex = targetsHours.indexOfFirst { it > streakHours }.let { if (it == -1) targetsHours.size - 1 else it }
        val nextTarget = targetsHours[nextIndex] * HOUR_MILLIS
        val previousTarget = if (nextIndex == 0) 0L else targetsHours[nextIndex - 1] * HOUR_MILLIS
        val span = (nextTarget - previousTarget).coerceAtLeast(1L)
        val progress = ((streakMillis - previousTarget).toFloat() / span).coerceIn(0f, 1f)

        return TickStats(
            streakStart = streakStart,
            streakMillis = streakMillis,
            thisMonthCount = monthCount,
            averageIntervalMillis = average,
            longestStreakMillis = longest,
            checkedInToday = checkedToday,
            totalCheckIns = totalCheckIns,
            nextTargetMillis = nextTarget,
            previousTargetMillis = previousTarget,
            targetProgress = progress,
        )
    }

    fun sameDay(a: Long, b: Long, zoneId: ZoneId = ZoneId.systemDefault()): Boolean {
        val da = Instant.ofEpochMilli(a).atZone(zoneId).toLocalDate()
        val db = Instant.ofEpochMilli(b).atZone(zoneId).toLocalDate()
        return da == db
    }

    fun nextTargetLabel(stats: TickStats): String {
        val hours = stats.nextTargetMillis / HOUR_MILLIS
        return when {
            hours % 24L == 0L -> "${hours / 24} 天"
            hours >= 24 -> String.format(Locale.CHINA, "%.1f 天", hours / 24.0)
            else -> "$hours 小时"
        }
    }

    fun progressCaption(stats: TickStats): String {
        val start = Instant.ofEpochMilli(stats.streakStart).atZone(ZoneId.systemDefault())
        return "从 ${start.monthValue} 月 ${start.dayOfMonth} 日 ${String.format(Locale.CHINA, "%02d:%02d", start.hour, start.minute)} 开始"
    }

    fun averageLabel(stats: TickStats): String {
        val millis = stats.averageIntervalMillis ?: return "—"
        return durationShort(millis)
    }

    fun longestLabel(stats: TickStats): String = durationShort(stats.longestStreakMillis)

    fun durationShort(millis: Long): String {
        if (millis <= 0L) return "0 小时"
        val days = millis / DAY_MILLIS
        val hours = millis / HOUR_MILLIS
        return when {
            days >= 10 -> "$days 天"
            days >= 1 -> String.format(Locale.CHINA, "%.1f 天", millis / DAY_MILLIS.toDouble())
            hours >= 1 -> "$hours 小时"
            else -> "${(millis / MINUTE_MILLIS).coerceAtLeast(1)} 分钟"
        }
    }
}
