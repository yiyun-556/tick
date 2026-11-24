package com.tick.app.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tick.app.data.EventType
import com.tick.app.data.HOUR_MILLIS
import com.tick.app.data.StatsCalculator
import com.tick.app.data.TickEvent
import com.tick.app.data.TickStats
import com.tick.app.ui.components.SoftDivider
import com.tick.app.ui.components.formatDateTime
import com.tick.app.ui.components.formatWeekday
import com.tick.app.ui.theme.TickMotion
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Months
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val weekdayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

@Composable
fun HeroPanel(
    stats: TickStats,
    events: List<TickEvent>,
    now: Long,
    dayNotes: Map<Long, String>,
    onUpdateDayNote: (Long, String) -> Unit,
) {
    var showCalendar by rememberSaveable { mutableStateOf(false) }
    var showMonthPicker by rememberSaveable { mutableStateOf(false) }
    var showNoteEditor by rememberSaveable { mutableStateOf(false) }
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    var selectedEpochDay by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    val zone = remember { ZoneId.systemDefault() }
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val month = YearMonth.now(zone).plusMonths(monthOffset.toLong())
    val checkInDays = remember(events) {
        events.asSequence()
            .filter { it.type == EventType.CHECK_IN }
            .map { Instant.ofEpochMilli(it.at).atZone(zone).toLocalDate() }
            .toSet()
    }
    val relapseDays = remember(events) {
        events.asSequence()
            .filter { it.type == EventType.RELAPSE }
            .map { Instant.ofEpochMilli(it.at).atZone(zone).toLocalDate() }
            .toSet()
    }
    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)

    fun toggleCalendar() {
        if (!showCalendar) {
            monthOffset = 0
            selectedEpochDay = today.toEpochDay()
        }
        showCalendar = !showCalendar
    }

    fun moveMonth(delta: Int) {
        monthOffset += delta
        selectedEpochDay = YearMonth.now(zone)
            .plusMonths(monthOffset.toLong())
            .atDay(1)
            .toEpochDay()
    }

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(animationSpec = TickMotion.expand()),
        cornerRadius = 26.dp,
        insideMargin = PaddingValues(0.dp),
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.surfaceContainer,
            contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { toggleCalendar() }
                .padding(22.dp),
        ) {
            if (!showCalendar) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(MiuixTheme.colorScheme.primary),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = "此刻的刻度",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        imageVector = MiuixIcons.Regular.Months,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = formatWeekday(now),
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontSize = 11.sp,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "已稳住",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    NumberText(stats.streakDays.toString(), 46)
                    UnitText("天")
                    Spacer(Modifier.width(9.dp))
                    NumberText(stats.streakHours.toString(), 27)
                    UnitText("小时")
                    Spacer(Modifier.width(7.dp))
                    NumberText(stats.streakMinutes.toString(), 27)
                    UnitText("分钟")
                }
                Spacer(Modifier.height(13.dp))
                SoftDivider()
                Spacer(Modifier.height(14.dp))
                ProgressBlock(stats)
                Spacer(Modifier.height(9.dp))
                Text(
                    text = if (stats.streakMillis < HOUR_MILLIS) {
                        "新的刻度，从这一刻重新开始。"
                    } else {
                        "从 ${formatDateTime(stats.streakStart)} 开始，已经走过 ${StatsCalculator.durationShort(stats.streakMillis)}。"
                    },
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.height(10.dp))
                ToggleHint(text = "轻点卡片，看看这个月的刻度")
            } else {
                CalendarHeader(
                    month = month,
                    onPrevious = { moveMonth(-1) },
                    onNext = { moveMonth(1) },
                    onPickMonth = { showMonthPicker = true },
                )
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    weekdayLabels.forEach { label ->
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                CalendarGrid(
                    month = month,
                    today = today,
                    selectedDate = selectedDate,
                    checkInDays = checkInDays,
                    relapseDays = relapseDays,
                    onSelect = { selectedEpochDay = it.toEpochDay() },
                )
                Spacer(Modifier.height(10.dp))
                SelectedDaySummary(
                    date = selectedDate,
                    checkedIn = selectedDate in checkInDays,
                    relapse = selectedDate in relapseDays,
                    trigger = events.firstOrNull {
                        it.type == EventType.RELAPSE &&
                            Instant.ofEpochMilli(it.at).atZone(zone).toLocalDate() == selectedDate
                    }?.trigger,
                )
                Spacer(Modifier.height(8.dp))
                DayNoteRow(
                    note = dayNotes[selectedEpochDay],
                    onClick = { showNoteEditor = true },
                )
                Spacer(Modifier.height(8.dp))
                if (month != YearMonth.now(zone)) {
                    Spacer(Modifier.height(7.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MiuixTheme.colorScheme.secondaryVariant)
                            .clickable {
                                monthOffset = 0
                                selectedEpochDay = today.toEpochDay()
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(
                            text = "回到本月",
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSecondaryVariant,
                            fontSize = 10.sp,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                ToggleHint(text = "轻点卡片空白处，回到此刻")
            }
        }
    }

    if (showMonthPicker) {
        MonthPickerDialog(
            current = month,
            onDismiss = { showMonthPicker = false },
            onSelect = { target ->
                val nowMonth = YearMonth.now(zone)
                monthOffset = (target.year - nowMonth.year) * 12 +
                    (target.monthValue - nowMonth.monthValue)
                selectedEpochDay = target.atDay(1).toEpochDay()
                showMonthPicker = false
            },
        )
    }

    if (showNoteEditor) {
        DayNoteDialog(
            date = selectedDate,
            initial = dayNotes[selectedEpochDay].orEmpty(),
            onDismiss = { showNoteEditor = false },
            onSave = { text ->
                onUpdateDayNote(selectedEpochDay, text)
                showNoteEditor = false
            },
        )
    }
}

@Composable
private fun DayNoteRow(note: String?, onClick: () -> Unit) {
    val hasNote = !note.isNullOrBlank()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (hasNote) MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
                else MiuixTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (hasNote) MiuixIcons.Regular.Edit else MiuixIcons.Regular.Notes,
            contentDescription = if (hasNote) "编辑备注" else "添加备注",
            modifier = Modifier.size(14.dp),
            tint = MiuixTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(7.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (hasNote) "这一天的备注" else "给这一天留一句备注",
                style = MiuixTheme.textStyles.footnote2,
                color = if (hasNote) MiuixTheme.colorScheme.primary
                else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 10.sp,
            )
            if (hasNote) {
                Text(
                    text = note!!,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurface,
                    fontSize = 11.5.sp,
                    maxLines = 2,
                )
            }
        }
        Icon(
            imageVector = MiuixIcons.Regular.ChevronForward,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun MonthPickerDialog(
    current: YearMonth,
    onDismiss: () -> Unit,
    onSelect: (YearMonth) -> Unit,
) {
    var year by remember { mutableIntStateOf(current.year) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                insideMargin = PaddingValues(18.dp),
                colors = CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
                ),
            ) {
                Text(
                    text = "选择年月份",
                    style = MiuixTheme.textStyles.title4,
                    color = MiuixTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { year -= 1 },
                        modifier = Modifier.size(32.dp),
                        minWidth = 32.dp,
                        minHeight = 32.dp,
                        backgroundColor = Color.Transparent,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Regular.ChevronBackward,
                            contentDescription = "上一年",
                            modifier = Modifier.size(18.dp),
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                    Text(
                        text = "$year 年",
                        modifier = Modifier.weight(1f),
                        style = MiuixTheme.textStyles.title4,
                        color = MiuixTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    IconButton(
                        onClick = { year += 1 },
                        modifier = Modifier.size(32.dp),
                        minWidth = 32.dp,
                        minHeight = 32.dp,
                        backgroundColor = Color.Transparent,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Regular.ChevronForward,
                            contentDescription = "下一年",
                            modifier = Modifier.size(18.dp),
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                for (row in 0 until 4) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (column in 0 until 3) {
                            val monthValue = row * 3 + column + 1
                            val selected = year == current.year && monthValue == current.monthValue
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(3.dp)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (selected) MiuixTheme.colorScheme.primary
                                        else MiuixTheme.colorScheme.surfaceContainerHigh,
                                    )
                                    .clickable { onSelect(YearMonth.of(year, monthValue)) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "$monthValue 月",
                                    style = MiuixTheme.textStyles.footnote1,
                                    color = if (selected) MiuixTheme.colorScheme.onPrimary
                                    else MiuixTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onSelect(YearMonth.now()) },
                    modifier = Modifier.fillMaxWidth(),
                    minHeight = 40.dp,
                    cornerRadius = 12.dp,
                    colors = ButtonDefaults.buttonColors(
                        color = MiuixTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MiuixTheme.colorScheme.primary,
                    ),
                ) {
                    Text(text = "回到本月", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun DayNoteDialog(
    date: LocalDate,
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var value by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initial))
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                insideMargin = PaddingValues(18.dp),
                colors = CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
                ),
            ) {
                Text(
                    text = "${date.monthValue} 月 ${date.dayOfMonth} 日",
                    style = MiuixTheme.textStyles.title4,
                    color = MiuixTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(10.dp))
                TextField(
                    value = value,
                    onValueChange = { if (it.text.length <= 280) value = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "给这一天留一句备注",
                    useLabelAsPlaceholder = true,
                    minLines = 3,
                    maxLines = 6,
                    cornerRadius = 14.dp,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        minHeight = 42.dp,
                        cornerRadius = 12.dp,
                        colors = ButtonDefaults.buttonColors(
                            color = MiuixTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MiuixTheme.colorScheme.onSurface,
                        ),
                    ) {
                        Text(text = "取消", fontSize = 12.sp)
                    }
                    Button(
                        onClick = { onSave(value.text) },
                        modifier = Modifier.weight(1f),
                        minHeight = 42.dp,
                        cornerRadius = 12.dp,
                        colors = ButtonDefaults.buttonColors(
                            color = MiuixTheme.colorScheme.primary,
                            contentColor = MiuixTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(text = "保存", fontSize = 12.sp)
                    }
                }
                if (initial.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "清空并删除这条备注",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSave("") }
                            .padding(vertical = 8.dp),
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.error,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressBlock(stats: TickStats) {
    val animatedProgress by animateFloatAsState(
        targetValue = stats.targetProgress,
        animationSpec = tween(700),
        label = "goalProgress",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "下一阶段 · ${StatsCalculator.nextTargetLabel(stats)}",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "${(stats.targetProgress * 100).toInt()}%",
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
    Spacer(Modifier.height(8.dp))
    LinearProgressIndicator(
        progress = animatedProgress,
        modifier = Modifier.fillMaxWidth(),
        height = 8.dp,
    )
}

@Composable
private fun CalendarHeader(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPickMonth: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onPickMonth)
                .padding(vertical = 2.dp),
        ) {
            Text(
                text = "这一个月",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
            )
            Text(
                text = "${month.year} 年 ${month.monthValue} 月",
                style = MiuixTheme.textStyles.title4,
                color = MiuixTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.size(32.dp),
            minWidth = 32.dp,
            minHeight = 32.dp,
            backgroundColor = Color.Transparent,
        ) {
            Icon(
                imageVector = MiuixIcons.Regular.ChevronBackward,
                contentDescription = "上个月",
                modifier = Modifier.size(18.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        IconButton(
            onClick = onNext,
            modifier = Modifier.size(32.dp),
            minWidth = 32.dp,
            minHeight = 32.dp,
            backgroundColor = Color.Transparent,
        ) {
            Icon(
                imageVector = MiuixIcons.Regular.ChevronForward,
                contentDescription = "下个月",
                modifier = Modifier.size(18.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    month: YearMonth,
    today: LocalDate,
    selectedDate: LocalDate,
    checkInDays: Set<LocalDate>,
    relapseDays: Set<LocalDate>,
    onSelect: (LocalDate) -> Unit,
) {
    val leading = month.atDay(1).dayOfWeek.value - 1
    val cells = leading + month.lengthOfMonth()
    val rows = (cells + 6) / 7
    for (row in 0 until rows) {
        Row(modifier = Modifier.fillMaxWidth()) {
            for (column in 0 until 7) {
                val day = row * 7 + column - leading + 1
                if (day in 1..month.lengthOfMonth()) {
                    val date = month.atDay(day)
                    CalendarDay(
                        date = date,
                        isToday = date == today,
                        isSelected = date == selectedDate,
                        checkedIn = date in checkInDays,
                        relapse = date in relapseDays,
                        onClick = { onSelect(date) },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(Modifier.weight(1f).height(36.dp))
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    checkedIn: Boolean,
    relapse: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = when {
        isSelected -> MiuixTheme.colorScheme.primary
        isToday -> MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        else -> Color.Transparent
    }
    val numberColor = when {
        isSelected -> MiuixTheme.colorScheme.onPrimary
        isToday -> MiuixTheme.colorScheme.onPrimaryContainer
        else -> MiuixTheme.colorScheme.onSurfaceSecondary
    }
    Column(
        modifier = modifier
            .height(36.dp)
            .padding(2.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(background)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MiuixTheme.textStyles.footnote1.copy(fontFeatureSettings = "tnum"),
            color = numberColor,
            fontSize = 12.sp,
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
        )
        Row(
            modifier = Modifier.height(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (checkedIn) {
                Box(Modifier.size(3.dp).clip(CircleShape).background(if (isSelected) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.primary))
            }
            if (relapse) {
                Box(Modifier.size(3.dp).clip(CircleShape).background(if (isSelected) MiuixTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.7f)))
            }
        }
    }
}

@Composable
private fun SelectedDaySummary(
    date: LocalDate,
    checkedIn: Boolean,
    relapse: Boolean,
    trigger: String?,
) {
    val text = when {
        checkedIn && relapse -> "稳住打卡 · 一次记录${trigger?.let { " · $it" } ?: ""}"
        checkedIn -> "稳住打卡 · 这一格属于你"
        relapse -> "一次记录${trigger?.let { " · $it" } ?: ""}"
        else -> "这一天还没有记录"
    }
    Text(
        text = "${date.monthValue} 月 ${date.dayOfMonth} 日 · $text",
        style = MiuixTheme.textStyles.footnote2,
        color = if (checkedIn) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
        fontSize = 10.5.sp,
        maxLines = 2,
    )
}

@Composable
private fun ToggleHint(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = MiuixIcons.Regular.Months,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.7f),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = text,
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.8f),
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun NumberText(value: String, size: Int) {
    Text(
        text = value,
        style = MiuixTheme.textStyles.title1.copy(fontFeatureSettings = "tnum"),
        color = MiuixTheme.colorScheme.onSurface,
        fontSize = size.sp,
        fontWeight = FontWeight.Light,
        maxLines = 1,
        softWrap = false,
    )
}

@Composable
private fun UnitText(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 3.dp, bottom = 5.dp),
        style = MiuixTheme.textStyles.footnote1,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        fontSize = 12.sp,
    )
}
