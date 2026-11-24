package com.tick.app.ui.history

import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tick.app.data.EventType
import com.tick.app.data.TickEvent
import com.tick.app.ui.components.SectionCard
import com.tick.app.ui.components.formatDateHeader
import com.tick.app.ui.components.formatTime
import java.time.Instant
import java.time.ZoneId
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Recording
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun HistoryScreen(
    events: List<TickEvent>,
    topPadding: androidx.compose.ui.unit.Dp,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onBackfill: () -> Unit,
    onDelete: (TickEvent) -> Unit,
) {
    val groups = remember(events) {
        events
            .sortedByDescending { it.at }
            .groupBy { Instant.ofEpochMilli(it.at).atZone(ZoneId.systemDefault()).toLocalDate() }
            .entries
            .toList()
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = topPadding, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { HistorySummary(events = events, onBackfill = onBackfill) }

        if (groups.isEmpty()) {
            item { EmptyHistory(onBackfill = onBackfill) }
        } else {
            groups.forEach { (date, dayEvents) ->
                item(key = "header-${date}") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp, start = 4.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = formatDateHeader(dayEvents.first().at),
                            style = MiuixTheme.textStyles.subtitle,
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                            fontSize = 12.sp,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "${dayEvents.size} 条",
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            fontSize = 10.sp,
                        )
                    }
                }
                dayEvents.forEach { event ->
                    item(key = event.id) {
                        EventCard(event = event, onDelete = { onDelete(event) })
                    }
                }
            }
        }

        item {
            Text(
                text = "所有记录都只保存在这台手机上。",
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 10.5.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HistorySummary(events: List<TickEvent>, onBackfill: () -> Unit) {
    val checkIns = events.count { it.type == EventType.CHECK_IN }
    val relapses = events.count { it.type == EventType.RELAPSE }
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "你的时间轴",
                    style = MiuixTheme.textStyles.headline2,
                    color = MiuixTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "认真记录的人，才能看见自己的节奏。",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 12.sp,
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onBackfill,
                minHeight = 38.dp,
                cornerRadius = 12.dp,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(text = "补录", style = MiuixTheme.textStyles.footnote1, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CountPill(label = "全部", value = events.size, modifier = Modifier.weight(1f))
            CountPill(
                label = "稳住",
                value = checkIns,
                modifier = Modifier.weight(1f),
                accent = MiuixTheme.colorScheme.primary,
            )
            CountPill(
                label = "记录一次",
                value = relapses,
                modifier = Modifier.weight(1f),
                accent = MiuixTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun CountPill(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    accent: androidx.compose.ui.graphics.Color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
) {
    Column(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            text = value.toString(),
            style = MiuixTheme.textStyles.title4.copy(fontFeatureSettings = "tnum"),
            color = accent,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            fontSize = 10.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun EventCard(event: TickEvent, onDelete: () -> Unit) {
    val isCheckIn = event.type == EventType.CHECK_IN
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        insideMargin = PaddingValues(0.dp),
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.surfaceContainer,
            contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, top = 13.dp, bottom = 13.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCheckIn) {
                            MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.34f)
                        } else {
                            MiuixTheme.colorScheme.surfaceContainerHigh
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isCheckIn) MiuixIcons.Basic.Check else MiuixIcons.Regular.Recording,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isCheckIn) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isCheckIn) "稳住打卡" else "一次记录",
                        style = MiuixTheme.textStyles.headline2,
                        color = MiuixTheme.colorScheme.onSurface,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = formatTime(event.at),
                        style = MiuixTheme.textStyles.footnote2.copy(fontFeatureSettings = "tnum"),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontSize = 11.sp,
                    )
                }
                if (!isCheckIn && !event.trigger.isNullOrBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MiuixTheme.colorScheme.secondaryVariant)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = event.trigger,
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSecondaryVariant,
                            fontSize = 10.sp,
                        )
                    }
                }
                if (!event.note.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = event.note,
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontSize = 11.sp,
                        maxLines = 2,
                    )
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(38.dp),
                minWidth = 38.dp,
                minHeight = 38.dp,
                backgroundColor = androidx.compose.ui.graphics.Color.Transparent,
            ) {
                Icon(
                    imageVector = MiuixIcons.Regular.Delete,
                    contentDescription = "删除这条记录",
                    modifier = Modifier.size(18.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.65f),
                )
            }
        }
    }
}

@Composable
private fun EmptyHistory(onBackfill: () -> Unit) {
    SectionCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MiuixTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = MiuixIcons.Regular.Recording,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "时间轴还是空的",
                style = MiuixTheme.textStyles.headline2,
                color = MiuixTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "第一格，从今天开始就好。",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onBackfill,
                minHeight = 42.dp,
                cornerRadius = 13.dp,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(text = "补录一条", style = MiuixTheme.textStyles.button, fontSize = 13.sp)
            }
        }
    }
}
