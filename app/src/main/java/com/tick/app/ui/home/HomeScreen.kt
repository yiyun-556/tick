package com.tick.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tick.app.data.HOUR_MILLIS
import com.tick.app.data.StatsCalculator
import com.tick.app.data.TickEvent
import com.tick.app.data.TickStats
import com.tick.app.ui.components.SectionCard
import com.tick.app.ui.components.SoftDivider
import com.tick.app.ui.components.formatDateTime
import com.tick.app.ui.components.formatMinutesAgo
import com.tick.app.ui.components.formatWeekday
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.icon.extended.Create
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun HomeScreen(
    events: List<TickEvent>,
    journeyStart: Long,
    dayNotes: Map<Long, String>,
    onUpdateDayNote: (Long, String) -> Unit,
    topPadding: androidx.compose.ui.unit.Dp,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onCheckIn: () -> Boolean,
    onOpenRecord: () -> Unit,
    onSelectCheckInTime: () -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        // Only the minute field is shown; waking at the next minute boundary
        // avoids a 1 Hz recomposition (and is friendlier to the battery).
        while (true) {
            val current = System.currentTimeMillis()
            now = current
            delay(60_000L - (current % 60_000L) + 40L)
        }
    }
    // A newly recorded event can fall after the cached minute boundary. Refresh
    // immediately so the streak/count don't wait up to a minute to reset.
    LaunchedEffect(events) {
        now = System.currentTimeMillis()
    }
    val stats = remember(events, journeyStart, now) {
        StatsCalculator.calculate(events = events, journeyStart = journeyStart, now = now)
    }
    val lastRelapse = remember(events) {
        events.filter { it.type == com.tick.app.data.EventType.RELAPSE }.maxByOrNull { it.at }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = topPadding, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            HeroPanel(
                stats = stats,
                events = events,
                now = now,
                dayNotes = dayNotes,
                onUpdateDayNote = onUpdateDayNote,
            )
        }
        item { StatsRow(stats = stats) }
        item {
            DailyActions(
                stats = stats,
                onCheckIn = onCheckIn,
                onOpenRecord = onOpenRecord,
                onSelectCheckInTime = onSelectCheckInTime,
            )
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (lastRelapse == null) {
                        "还没有“记录一次”，但每一格稳住都算数。"
                    } else {
                        "最近一次记录：${formatMinutesAgo(lastRelapse.at, now)} · ${lastRelapse.trigger ?: "没有标签"}"
                    },
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "每一次记录，都是更了解自己一点。",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun StatsRow(stats: TickStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            value = stats.thisMonthCount.toString(),
            unit = "次",
            label = "本月次数",
            accent = MiuixTheme.colorScheme.primary,
        )
        StatCard(
            modifier = Modifier.weight(1f),
            value = StatsCalculator.averageLabel(stats),
            unit = "",
            label = "平均间隔",
            accent = MiuixTheme.colorScheme.onSurface,
        )
        StatCard(
            modifier = Modifier.weight(1f),
            value = StatsCalculator.longestLabel(stats),
            unit = "",
            label = "最长连胜",
            accent = MiuixTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun StatCard(
    value: String,
    unit: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        cornerRadius = 18.dp,
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.surfaceContainer,
            contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
        ),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MiuixTheme.textStyles.title3.copy(fontFeatureSettings = "tnum"),
                color = accent,
                fontSize = if (value.length > 5) 17.sp else 22.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    modifier = Modifier.padding(start = 1.dp, bottom = 2.dp),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 10.sp,
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            fontSize = 11.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun DailyActions(
    stats: TickStats,
    onCheckIn: () -> Boolean,
    onOpenRecord: () -> Unit,
    onSelectCheckInTime: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var showSuccess by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(3_200)
            showSuccess = false
        }
    }

    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (stats.checkedInToday) "今天，已经稳住了" else "今天也照顾好自己",
                    style = MiuixTheme.textStyles.headline2,
                    color = MiuixTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (stats.checkedInToday) {
                        "这一格已经记下。稳稳的，不用声张。"
                    } else {
                        "顺利度过的一天，也值得被看见。"
                    },
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 12.sp,
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = "已打卡 ${stats.totalCheckIns} 天",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.primary,
                fontSize = 11.sp,
            )
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = {
                if (onCheckIn()) {
                    showSuccess = true
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !stats.checkedInToday,
            minHeight = 50.dp,
            cornerRadius = 16.dp,
            colors = ButtonDefaults.buttonColorsPrimary(),
        ) {
            Icon(
                imageVector = MiuixIcons.Basic.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = if (stats.checkedInToday) "今天已稳住" else "每日稳住打卡",
                style = MiuixTheme.textStyles.button,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        AnimatedVisibility(
            visible = showSuccess,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Text(
                text = "记下了。今天这一格，属于你。",
                modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.primary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(9.dp))
        Button(
            onClick = onOpenRecord,
            modifier = Modifier.fillMaxWidth(),
            minHeight = 46.dp,
            cornerRadius = 15.dp,
            colors = ButtonDefaults.buttonColors(),
        ) {
            Icon(
                imageVector = MiuixIcons.Regular.Create,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = "记录一次",
                style = MiuixTheme.textStyles.button,
                fontSize = 14.sp,
            )
        }
        Text(
            text = "错过了打卡时间？轻点这里补一个过去的时间",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 9.dp)
                .clickable(onClick = onSelectCheckInTime),
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.primary,
            fontSize = 10.5.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "不管发生了什么，记下来就好。一次记录不会否定你。",
            modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            fontSize = 10.5.sp,
            textAlign = TextAlign.Center,
        )
    }
}
