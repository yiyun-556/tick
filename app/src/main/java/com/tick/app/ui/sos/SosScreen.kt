package com.tick.app.ui.sos

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tick.app.data.AppSettings
import com.tick.app.data.ChatMessage
import com.tick.app.data.ChatRole
import com.tick.app.ui.components.SmallActionButton
import com.tick.app.ui.components.SoftDivider
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Reset
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.min

private val quickPrompts = listOf(
    "我现在特别想破戒，有点扛不住了，陪我说说话",
    "刚才没忍住破戒了，现在心里有点自责烦躁",
    "夜里睡不着脑子很乱，帮我转移一下注意力",
)

@Composable
fun SosScreen(
    settings: AppSettings,
    messages: List<ChatMessage>,
    isStreaming: Boolean,
    streamingMessageId: String?,
    onClose: () -> Unit,
    onSend: (String) -> Unit,
    onClearChat: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    BackHandler(onBack = onClose)
    val surface = MiuixTheme.colorScheme.surface
    val background = Brush.verticalGradient(
        listOf(
            surface.copy(alpha = 0.64f),
            surface.copy(alpha = 0.80f),
            surface.copy(alpha = 0.91f),
        ),
    )
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    var draft by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current

    var remaining by rememberSaveable { mutableIntStateOf(60) }
    var running by rememberSaveable { mutableStateOf(true) }
    var runId by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(running, runId) {
        while (running && remaining > 0) {
            delay(1_000)
            if (!running) break
            remaining -= 1
        }
        if (remaining <= 0 && running) {
            running = false
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        }
    }

    val elapsed = (60 - remaining).coerceIn(0, 60)
    val cyclePosition = if (elapsed >= 60) 13 else elapsed % 14
    val cycleNumber = elapsed / 14
    val phaseIndex = when {
        elapsed >= 60 -> 2
        cyclePosition < 4 -> 0
        cyclePosition < 8 -> 1
        else -> 2
    }
    val phase = when (phaseIndex) {
        0 -> "吸气"
        1 -> "屏息"
        else -> "呼气"
    }
    val phaseSeconds = when (phaseIndex) {
        0 -> 4 - cyclePosition
        1 -> 8 - cyclePosition
        else -> 14 - cyclePosition
    }.coerceAtLeast(1)

    LaunchedEffect(cycleNumber, phaseIndex) {
        if (elapsed > 0 && remaining > 0) {
            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
        }
    }

    val targetScale = when {
        remaining <= 0 -> 0.92f
        phaseIndex == 0 -> 1f
        phaseIndex == 1 -> 1f
        else -> 0.72f
    }
    val ringScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(
            durationMillis = when (phaseIndex) {
                0 -> 4_000
                1 -> 400
                else -> 6_000
            },
            easing = LinearEasing,
        ),
        label = "breathScale",
    )

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    val lastLength = messages.lastOrNull()?.text?.length ?: 0
    LaunchedEffect(lastLength, isStreaming) {
        if (messages.isNotEmpty() && isStreaming) {
            listState.scrollToItem(messages.lastIndex)
        }
    }
    LaunchedEffect(imeVisible) {
        if (imeVisible && messages.isNotEmpty()) {
            listState.scrollToItem(messages.lastIndex)
        }
    }

    fun sendNow(text: String) {
        val value = text.trim()
        if (value.isEmpty() || isStreaming) return
        draft = TextFieldValue("")
        onSend(value)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { } },
        )
        Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "先慢一点",
                    style = MiuixTheme.textStyles.title4,
                    color = MiuixTheme.colorScheme.onSurface,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (settings.ai.isReady) "不用做决定，我陪你过这一阵" else "本地陪伴模式 · 先陪你过这一阵",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 11.sp,
                )
            }
            SmallActionButton(text = "重开", onClick = onClearChat, icon = MiuixIcons.Regular.Reset)
            Spacer(Modifier.width(4.dp))
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(40.dp),
                minWidth = 40.dp,
                minHeight = 40.dp,
                backgroundColor = Color.Transparent,
            ) {
                Icon(
                    imageVector = MiuixIcons.Regular.Close,
                    contentDescription = "返回",
                    modifier = Modifier.size(22.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }

        if (!settings.ai.isReady) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                    .clickable(onClick = onOpenSettings)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = MiuixIcons.Regular.Info,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MiuixTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "接入自己的 AI，回应会更贴近你。点这里去设置。",
                    modifier = Modifier.weight(1f),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onPrimaryContainer,
                    fontSize = 10.5.sp,
                )
                Text(
                    text = "去设置",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.primary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        if (!imeVisible) {
            BreathingCard(
                remaining = remaining,
                running = running,
                completed = remaining <= 0,
                phase = phase,
                phaseSeconds = phaseSeconds,
                ringScale = ringScale,
                onToggle = { running = !running },
                onRestart = {
                    remaining = 60
                    running = true
                    runId += 1
                },
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "呼吸引导 · ${if (remaining > 0) "还剩 $remaining 秒" else "已完成"}",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.primary,
                    fontSize = 10.5.sp,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = phase,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 10.5.sp,
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(messages, key = { it.id }) { message ->
                ChatBubble(
                    message = message,
                    aiName = settings.ai.aiName.ifBlank { "小刻" },
                    isStreamingThis = message.id == streamingMessageId && isStreaming,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .background(surface.copy(alpha = 0.96f)),
        ) {
            SoftDivider()
            if (!imeVisible) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    quickPrompts.forEach { prompt ->
                        QuickPrompt(
                            text = prompt,
                            enabled = !isStreaming,
                            onClick = { sendNow(prompt) },
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 10.dp, top = 2.dp, bottom = 10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                TextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    label = "想说什么都可以…",
                    useLabelAsPlaceholder = true,
                    minLines = 1,
                    maxLines = 4,
                    cornerRadius = 20.dp,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { sendNow(draft.text) }),
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { sendNow(draft.text) },
                    enabled = !isStreaming && draft.text.isNotBlank(),
                    modifier = Modifier.size(46.dp),
                    minWidth = 46.dp,
                    minHeight = 46.dp,
                    backgroundColor = if (isStreaming || draft.text.isBlank()) {
                        MiuixTheme.colorScheme.surfaceContainerHigh
                    } else {
                        MiuixTheme.colorScheme.primary
                    },
                ) {
                    Icon(
                        imageVector = MiuixIcons.Regular.Send,
                        contentDescription = "发送",
                        modifier = Modifier.size(21.dp),
                        tint = if (isStreaming || draft.text.isBlank()) {
                            MiuixTheme.colorScheme.onSurfaceVariantSummary
                        } else {
                            MiuixTheme.colorScheme.onPrimary
                        },
                    )
                }
            }
        }
    }
}
}

@Composable
private fun BreathingCard(
    remaining: Int,
    running: Boolean,
    completed: Boolean,
    phase: String,
    phaseSeconds: Int,
    ringScale: Float,
    onToggle: () -> Unit,
    onRestart: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(94.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(94.dp)
                    .clip(CircleShape)
                    .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.09f)),
            )
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .graphicsLayer {
                        scaleX = ringScale
                        scaleY = ringScale
                    }
                    .clip(CircleShape)
                    .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.16f)),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (completed) "完成" else phase,
                    style = MiuixTheme.textStyles.headline2,
                    color = MiuixTheme.colorScheme.primary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (completed) "60 秒" else "$phaseSeconds 秒",
                    style = MiuixTheme.textStyles.footnote2.copy(fontFeatureSettings = "tnum"),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 10.sp,
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (completed) "这一阵，已经过去了" else "60 秒呼吸引导",
                style = MiuixTheme.textStyles.headline2,
                color = MiuixTheme.colorScheme.onSurface,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (completed) {
                    "不管有没有完全平静，你都陪自己待了一会儿。"
                } else {
                    "吸气 4 秒 · 屏息 4 秒 · 呼气 6 秒"
                },
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 10.5.sp,
                maxLines = 2,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = if (remaining > 0) "还剩 $remaining 秒" else "这一轮结束",
                style = MiuixTheme.textStyles.footnote1.copy(fontFeatureSettings = "tnum"),
                color = MiuixTheme.colorScheme.primary,
                fontSize = 11.sp,
            )
            Spacer(Modifier.height(5.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                if (!completed) {
                    CircleTextButton(
                        text = if (running) "暂停" else "继续",
                        onClick = onToggle,
                    )
                }
                CircleTextButton(text = if (completed) "再来一轮" else "重来", onClick = onRestart)
            }
        }
    }
}

@Composable
private fun CircleTextButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(MiuixTheme.colorScheme.secondaryVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceSecondary,
            fontSize = 10.5.sp,
        )
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    aiName: String,
    isStreamingThis: Boolean,
) {
    val isUser = message.role == ChatRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = aiName.take(1).ifBlank { "刻" },
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(7.dp))
        }
        Column(
            modifier = Modifier.fillMaxWidth(0.84f),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
            if (!isUser && (message.local || message.error)) {
                Text(
                    text = when {
                        message.error -> "连接提示"
                        message.local -> "本地陪伴"
                        else -> aiName
                    },
                    modifier = Modifier.padding(start = 3.dp, bottom = 3.dp),
                    style = MiuixTheme.textStyles.footnote2,
                    color = if (message.error) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 9.5.sp,
                )
            }
            Box(
                modifier = Modifier
                    .clip(
                        if (isUser) {
                            RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 5.dp)
                        } else {
                            RoundedCornerShape(topStart = 5.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
                        },
                    )
                    .background(
                        when {
                            isUser -> MiuixTheme.colorScheme.primary
                            message.error -> MiuixTheme.colorScheme.errorContainer
                            else -> MiuixTheme.colorScheme.surfaceContainerHigh
                        },
                    )
                    .padding(horizontal = 13.dp, vertical = 10.dp),
            ) {
                if (!isUser && message.text.isEmpty() && isStreamingThis) {
                    TypingDots()
                } else {
                    SelectionContainer {
                        AssistantAwareText(
                            message = message,
                            isUser = isUser,
                            isStreamingThis = isStreamingThis,
                        )
                    }
                }
            }
        }
        if (isUser) Spacer(Modifier.width(2.dp))
    }
}

@Composable
private fun AssistantAwareText(
    message: ChatMessage,
    isUser: Boolean,
    isStreamingThis: Boolean,
) {
    var visible by remember(message.id) {
        mutableIntStateOf(if (isStreamingThis) 0 else message.text.length)
    }
    LaunchedEffect(message.text, isStreamingThis) {
        if (!isStreamingThis) {
            visible = message.text.length
            return@LaunchedEffect
        }
        while (visible < message.text.length) {
            visible = min(message.text.length, visible + 2)
            delay(16)
        }
    }
    val shown = message.text.take(visible.coerceIn(0, message.text.length))
    val cursor = if (isStreamingThis && visible < message.text.length) "▍" else ""
    Text(
        text = shown + cursor,
        style = MiuixTheme.textStyles.paragraph,
        color = if (isUser) {
            MiuixTheme.colorScheme.onPrimary
        } else if (message.error) {
            MiuixTheme.colorScheme.onErrorContainer
        } else {
            MiuixTheme.colorScheme.onSurface
        },
        fontSize = 14.sp,
        lineHeight = 21.sp,
    )
}

@Composable
private fun TypingDots() {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 900
                        0.25f at 0
                        1f at 300 + index * 120
                        0.25f at 700
                    },
                    repeatMode = RepeatMode.Restart,
                ),
                label = "dot$index",
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(6.dp)
                    .alpha(alpha)
                    .clip(CircleShape)
                    .background(MiuixTheme.colorScheme.onSurfaceVariantSummary),
            )
        }
    }
}

@Composable
private fun QuickPrompt(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(13.dp))
            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceSecondary,
            fontSize = 10.5.sp,
            maxLines = 2,
        )
    }
}
