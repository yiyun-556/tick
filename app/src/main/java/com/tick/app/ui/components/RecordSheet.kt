package com.tick.app.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tick.app.data.EventType
import com.tick.app.data.MINUTE_MILLIS
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val triggers = listOf("熬夜", "无聊", "压力大", "刷短视频", "情绪低落", "其他")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordSheet(
    initialType: EventType,
    allowTypeSwitch: Boolean,
    hasCheckInOn: (Long) -> Boolean,
    onDismiss: () -> Unit,
    onConfirm: (type: EventType, at: Long, trigger: String?, note: String?) -> Unit,
) {
    val context = LocalContext.current
    var type by rememberSaveable { mutableStateOf(initialType) }
    var selectedAt by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedTrigger by rememberSaveable { mutableStateOf<String?>(null) }
    var otherText by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var otherValue by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var noteValue by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val isFuture = selectedAt > now
    val duplicateCheckIn = type == EventType.CHECK_IN && !isFuture && hasCheckInOn(selectedAt)
    val canConfirm = !isFuture && !duplicateCheckIn
    val zoned = Instant.ofEpochMilli(selectedAt).atZone(ZoneId.systemDefault())

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.38f))
                .clickable(onClick = onDismiss),
        )
        BoxWithConstraints(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
        ) {
            val sheetMax = maxHeight * 0.92f
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = sheetMax)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                    .pointerInput(Unit) { detectTapGestures { } }
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.35f)),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 10.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (allowTypeSwitch) "补录一条" else "记下这一刻",
                            style = MiuixTheme.textStyles.title4,
                            color = MiuixTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "不用评判，只是如实记下。",
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            fontSize = 11.sp,
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp),
                        minWidth = 40.dp,
                        minHeight = 40.dp,
                        backgroundColor = Color.Transparent,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Regular.Close,
                            contentDescription = "关闭",
                            modifier = Modifier.size(20.dp),
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                ) {
                    if (allowTypeSwitch) {
                        Spacer(Modifier.height(14.dp))
                        SegmentedOptions(
                            options = listOf(
                                EventType.CHECK_IN to "稳住打卡",
                                EventType.RELAPSE to "记录一次",
                            ),
                            selected = type,
                            onSelect = { type = it },
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "发生时间",
                        style = MiuixTheme.textStyles.subtitle,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(7.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MiuixTheme.colorScheme.surfaceContainer)
                            .padding(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatFullDate(selectedAt),
                                style = MiuixTheme.textStyles.body1,
                                color = MiuixTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = formatTime(selectedAt),
                                style = MiuixTheme.textStyles.title4.copy(fontFeatureSettings = "tnum"),
                                color = MiuixTheme.colorScheme.primary,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            TagChip(
                                text = "现在",
                                selected = false,
                                onClick = { selectedAt = System.currentTimeMillis() },
                            )
                            TagChip(
                                text = "-10 分钟",
                                selected = false,
                                onClick = { selectedAt = (selectedAt - 10 * MINUTE_MILLIS).coerceAtLeast(0L) },
                            )
                            TagChip(
                                text = "-1 小时",
                                selected = false,
                                onClick = { selectedAt = (selectedAt - 60 * MINUTE_MILLIS).coerceAtLeast(0L) },
                            )
                            TagChip(
                                text = "选日期",
                                selected = false,
                                onClick = {
                                    val dialog = DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                            val base = Instant.ofEpochMilli(selectedAt).atZone(ZoneId.systemDefault())
                                            val safeDay = day.coerceAtMost(
                                                LocalDate.of(year, month + 1, 1).lengthOfMonth(),
                                            )
                                            val date = LocalDate.of(year, month + 1, safeDay)
                                            selectedAt = date.atTime(base.toLocalTime())
                                                .atZone(base.zone)
                                                .toInstant()
                                                .toEpochMilli()
                                        },
                                        zoned.year,
                                        zoned.monthValue - 1,
                                        zoned.dayOfMonth,
                                    )
                                    dialog.datePicker.maxDate = System.currentTimeMillis()
                                    dialog.show()
                                },
                            )
                            TagChip(
                                text = "选时间",
                                selected = false,
                                onClick = {
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            val base = Instant.ofEpochMilli(selectedAt).atZone(ZoneId.systemDefault())
                                            selectedAt = base
                                                .withHour(hour)
                                                .withMinute(minute)
                                                .withSecond(0)
                                                .withNano(0)
                                                .toInstant()
                                                .toEpochMilli()
                                        },
                                        zoned.hour,
                                        zoned.minute,
                                        true,
                                    ).show()
                                },
                            )
                        }
                        if (isFuture) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "未来的时间还没发生，选一个现在或过去的时刻吧。",
                                style = MiuixTheme.textStyles.footnote2,
                                color = MiuixTheme.colorScheme.error,
                                fontSize = 10.5.sp,
                            )
                        }
                    }

                    if (type == EventType.RELAPSE) {
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "当时的诱因",
                                style = MiuixTheme.textStyles.subtitle,
                                color = MiuixTheme.colorScheme.onBackgroundVariant,
                                fontSize = 12.sp,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "可选",
                                style = MiuixTheme.textStyles.footnote2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                fontSize = 10.sp,
                            )
                        }
                        Spacer(Modifier.height(7.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            triggers.forEach { trigger ->
                                TagChip(
                                    text = trigger,
                                    selected = selectedTrigger == trigger,
                                    onClick = {
                                        selectedTrigger = if (selectedTrigger == trigger) null else trigger
                                    },
                                )
                            }
                        }
                        if (selectedTrigger == "其他") {
                            Spacer(Modifier.height(9.dp))
                            TextField(
                                value = otherValue,
                                onValueChange = {
                                    otherValue = it
                                    otherText = it.text
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = "写一下当时的情况",
                                useLabelAsPlaceholder = true,
                                singleLine = true,
                                cornerRadius = 14.dp,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = ImeAction.Next,
                                ),
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    TextField(
                        value = noteValue,
                        onValueChange = {
                            noteValue = it
                            note = it.text
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = "想补充一句（可选）",
                        useLabelAsPlaceholder = true,
                        maxLines = 3,
                        minLines = 1,
                        cornerRadius = 14.dp,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done,
                        ),
                    )
                    if (duplicateCheckIn) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "这一天已经打过卡了，换一天试试。",
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.error,
                            fontSize = 10.5.sp,
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        minHeight = 48.dp,
                        cornerRadius = 15.dp,
                        colors = ButtonDefaults.buttonColors(),
                    ) {
                        Text(text = "先不记", style = MiuixTheme.textStyles.button, fontSize = 14.sp)
                    }
                    Button(
                        onClick = {
                            val safeAt = selectedAt.coerceAtMost(System.currentTimeMillis())
                            val triggerValue = when {
                                type == EventType.CHECK_IN -> null
                                selectedTrigger == "其他" && otherText.isNotBlank() -> "其他 · ${otherText.trim()}"
                                selectedTrigger == "其他" -> "其他"
                                selectedTrigger != null -> selectedTrigger
                                else -> null
                            }
                            onConfirm(type, safeAt, triggerValue, note.trim().takeIf { it.isNotEmpty() })
                        },
                        modifier = Modifier.weight(1.4f),
                        enabled = canConfirm,
                        minHeight = 48.dp,
                        cornerRadius = 15.dp,
                        colors = ButtonDefaults.buttonColorsPrimary(),
                    ) {
                        Text(
                            text = if (type == EventType.CHECK_IN) "记下这次稳住" else "记下这一刻",
                            style = MiuixTheme.textStyles.button,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}
