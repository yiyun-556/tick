package com.tick.app.ui.settings

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tick.app.data.AiConfig
import com.tick.app.data.AiTestState
import com.tick.app.data.AppSettings
import com.tick.app.data.DarkMode
import com.tick.app.data.DockSettings
import com.tick.app.data.DockStyle
import com.tick.app.data.NotificationSettings
import com.tick.app.data.PaletteStyle
import com.tick.app.data.aiPresets
import com.tick.app.update.UpdateChecker
import com.tick.app.update.UpdateConfig
import com.tick.app.update.UpdateInfo
import com.tick.app.ui.theme.TickMotion
import com.tick.app.ui.components.SectionCard
import com.tick.app.ui.components.SectionTitle
import com.tick.app.ui.components.SegmentedOptions
import com.tick.app.ui.liquid.LiquidSlider
import com.tick.app.ui.liquid.LiquidToggle
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.tick.app.ui.components.SoftDivider
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Hide
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Show
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.Locale
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: AppSettings,
    testState: AiTestState,
    topPadding: androidx.compose.ui.unit.Dp,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onPaletteChange: (PaletteStyle) -> Unit,
    onDarkModeChange: (DarkMode) -> Unit,
    onDockChange: (DockSettings) -> Unit,
    onAutoUpdateCheckChange: (Boolean) -> Unit,
    onSaveAi: (AiConfig) -> Unit,
    onTestAi: (AiConfig) -> Unit,
    onResetTest: () -> Unit,
    onClearData: () -> Unit,
    notificationsAllowed: Boolean,
    onUpdateNotifications: (NotificationSettings) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    var presetId by rememberSaveable { mutableStateOf(settings.ai.presetId) }
    var baseUrl by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(settings.ai.baseUrl)) }
    var model by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(settings.ai.model)) }
    var apiKey by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(settings.ai.apiKey)) }
    var aiName by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(settings.ai.aiName)) }
    var persona by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(settings.ai.persona)) }
    var lastSavedAi by remember { mutableStateOf(settings.ai) }
    var showKey by rememberSaveable { mutableStateOf(false) }
    var showPresetDialog by rememberSaveable { mutableStateOf(false) }
    var justSaved by rememberSaveable { mutableStateOf(false) }
    var showPrivacyPolicy by rememberSaveable { mutableStateOf(false) }
    val glassPrimary = MiuixTheme.colorScheme.primary
    val glassSurface = MiuixTheme.colorScheme.surface
    val drawControlBackdrop: DrawScope.() -> Unit = remember(glassPrimary, glassSurface) {
        {
            drawRect(
                Brush.linearGradient(
                    colors = listOf(
                        glassPrimary.copy(alpha = 0.55f),
                        Color(0xFF7FB3FF).copy(alpha = 0.30f),
                        glassSurface.copy(alpha = 0.96f),
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height),
                ),
            )
        }
    }
    val controlBackdrop = rememberCanvasBackdrop(drawControlBackdrop)

    LaunchedEffect(settings.ai) {
        if (settings.ai != lastSavedAi) {
            lastSavedAi = settings.ai
            presetId = settings.ai.presetId
            baseUrl = TextFieldValue(settings.ai.baseUrl)
            model = TextFieldValue(settings.ai.model)
            apiKey = TextFieldValue(settings.ai.apiKey)
            aiName = TextFieldValue(settings.ai.aiName)
            persona = TextFieldValue(settings.ai.persona)
        }
    }
    LaunchedEffect(justSaved) {
        if (justSaved) {
            kotlinx.coroutines.delay(2_000)
            justSaved = false
        }
    }

    fun draft(): AiConfig = AiConfig(
        presetId = presetId,
        baseUrl = baseUrl.text,
        model = model.text,
        apiKey = apiKey.text,
        aiName = aiName.text,
        persona = persona.text,
    )

    LazyColumn(
        modifier = Modifier.fillMaxWidth().imePadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = topPadding, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { SectionTitle(text = "外观") }
        item {
            AppearanceCard(
                settings = settings,
                onPaletteChange = onPaletteChange,
                onDarkModeChange = onDarkModeChange,
                onDockChange = onDockChange,
                controlBackdrop = controlBackdrop,
            )
        }

        item { SectionTitle(text = "通知与打卡") }
        item {
            NotificationsCard(
                notifications = settings.notifications,
                allowed = notificationsAllowed,
                onUpdate = onUpdateNotifications,
                onRequestPermission = onRequestNotificationPermission,
                onOpenSettings = onOpenNotificationSettings,
                controlBackdrop = controlBackdrop,
            )
        }

        item { SectionTitle(text = "AI 伴侣") }
        item {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.34f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Regular.Theme,
                            contentDescription = null,
                            modifier = Modifier.size(21.dp),
                            tint = MiuixTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "接入一个温柔的模型",
                            style = MiuixTheme.textStyles.headline2,
                            color = MiuixTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "不配置也能用本地陪伴模式。",
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            fontSize = 11.sp,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "平台预设",
                    style = MiuixTheme.textStyles.subtitle,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(7.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                        .clickable { showPresetDialog = true }
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = aiPresets.firstOrNull { it.id == presetId }?.label ?: "自定义兼容接口",
                        modifier = Modifier.weight(1f),
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                    )
                    Icon(
                        imageVector = MiuixIcons.Regular.ChevronForward,
                        contentDescription = "选择平台",
                        modifier = Modifier.size(18.dp),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
                Spacer(Modifier.height(12.dp))
                TextField(
                    value = baseUrl,
                    onValueChange = {
                        baseUrl = it
                        onResetTest()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = "Base URL",
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    cornerRadius = 14.dp,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next,
                    ),
                )
                if (baseUrl.text.startsWith("http://", ignoreCase = true)) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "这是未加密的 http 连接，API Key 可能被同一网络看到；建议只用于局域网自建服务。",
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.error,
                        fontSize = 10.sp,
                    )
                }
                Spacer(Modifier.height(9.dp))
                TextField(
                    value = model,
                    onValueChange = {
                        model = it
                        onResetTest()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = "模型名称",
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    cornerRadius = 14.dp,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Next),
                )
                Spacer(Modifier.height(9.dp))
                TextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        onResetTest()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = "API Key",
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    cornerRadius = 14.dp,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Next),
                    trailingIcon = {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { showKey = !showKey }
                                .padding(8.dp),
                        ) {
                            Icon(
                                imageVector = if (showKey) MiuixIcons.Regular.Hide else MiuixIcons.Regular.Show,
                                contentDescription = if (showKey) "隐藏 Key" else "显示 Key",
                                modifier = Modifier.size(19.dp),
                                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    },
                )
                Spacer(Modifier.height(11.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Button(
                        onClick = {
                            onTestAi(draft())
                        },
                        modifier = Modifier.weight(1f),
                        enabled = testState !is AiTestState.Testing,
                        minHeight = 44.dp,
                        cornerRadius = 13.dp,
                        colors = ButtonDefaults.buttonColors(),
                    ) {
                        if (testState is AiTestState.Testing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                size = 16.dp,
                            )
                            Spacer(Modifier.width(7.dp))
                        }
                        Text(
                            text = if (testState is AiTestState.Testing) "连接中" else "测试连接",
                            style = MiuixTheme.textStyles.button,
                            fontSize = 12.sp,
                        )
                    }
                    Button(
                        onClick = {
                            onSaveAi(draft())
                            justSaved = true
                        },
                        modifier = Modifier.weight(1f),
                        minHeight = 44.dp,
                        cornerRadius = 13.dp,
                        colors = ButtonDefaults.buttonColorsPrimary(),
                    ) {
                        Text(text = "保存配置", style = MiuixTheme.textStyles.button, fontSize = 12.sp)
                    }
                }
                AnimatedVisibility(visible = justSaved, enter = fadeIn(), exit = fadeOut()) {
                    Text(
                        text = "已保存，只在本机生效。",
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.primary,
                        fontSize = 10.5.sp,
                    )
                }
                when (testState) {
                    is AiTestState.Idle -> Unit
                    is AiTestState.Testing -> Unit
                    is AiTestState.Success -> TestLine(text = testState.message, success = true)
                    is AiTestState.Failure -> TestLine(text = testState.message, success = false)
                }
                Spacer(Modifier.height(12.dp))
                SoftDivider()
                Spacer(Modifier.height(12.dp))
                TextField(
                    value = aiName,
                    onValueChange = { aiName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "AI 称呼",
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    cornerRadius = 14.dp,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Next),
                )
                Spacer(Modifier.height(9.dp))
                TextField(
                    value = persona,
                    onValueChange = { persona = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "性格补充（可选）",
                    useLabelAsPlaceholder = true,
                    minLines = 2,
                    maxLines = 4,
                    cornerRadius = 14.dp,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = MiuixIcons.Regular.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Key 使用 Android Keystore 加密，只保存在这台手机上。",
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontSize = 10.sp,
                    )
                }
            }
        }

        item { SectionTitle(text = "关于与更新") }
        item {
            UpdateCard(
                settings = settings,
                onAutoUpdateCheckChange = onAutoUpdateCheckChange,
                controlBackdrop = controlBackdrop,
            )
        }

        item { SectionTitle(text = "隐私") }
        item {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Regular.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MiuixTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "所有数据只留在这台手机",
                            style = MiuixTheme.textStyles.headline2,
                            color = MiuixTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "打卡、对话和 API Key 都不会上传。",
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            fontSize = 11.sp,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = MiuixIcons.Regular.Info,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp).padding(top = 2.dp),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "AI 对话会发送给你配置的模型服务商，请在选择服务时自行确认其隐私政策。刻度本身不保存任何云端副本。",
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontSize = 10.5.sp,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { showPrivacyPolicy = true },
                    modifier = Modifier.fillMaxWidth(),
                    minHeight = 44.dp,
                    cornerRadius = 13.dp,
                    colors = ButtonDefaults.buttonColors(
                        color = MiuixTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MiuixTheme.colorScheme.primary,
                    ),
                ) {
                    Text(text = "查看隐私政策", style = MiuixTheme.textStyles.button, fontSize = 13.sp)
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onClearData,
                    modifier = Modifier.fillMaxWidth(),
                    minHeight = 44.dp,
                    cornerRadius = 13.dp,
                    colors = ButtonDefaults.buttonColors(
                        color = MiuixTheme.colorScheme.errorContainer,
                        contentColor = MiuixTheme.colorScheme.error,
                        disabledColor = MiuixTheme.colorScheme.errorContainer,
                        disabledContentColor = MiuixTheme.colorScheme.error,
                    ),
                ) {
                    Text(text = "清空全部本地数据", style = MiuixTheme.textStyles.button, fontSize = 13.sp)
                }
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "刻度 1.0.0",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 10.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "不是医疗建议 · 需要时请寻求专业帮助",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 10.sp,
                )
            }
        }
    }

    if (showPresetDialog) {
        PresetDialog(
            selectedId = presetId,
            onDismiss = { showPresetDialog = false },
            onSelect = { preset ->
                presetId = preset.id
                baseUrl = TextFieldValue(preset.baseUrl)
                model = TextFieldValue(preset.model)
                onResetTest()
                showPresetDialog = false
            },
        )
    }

    if (showPrivacyPolicy) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyPolicy = false })
    }
}

@Composable
private fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
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
                    text = "隐私政策",
                    style = MiuixTheme.textStyles.title4,
                    color = MiuixTheme.colorScheme.onSurface,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "版本 1.0.0 · 生效日期 2026-11-25",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 10.5.sp,
                )
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    privacySections.forEach { (title, body) ->
                        Text(
                            text = title,
                            style = MiuixTheme.textStyles.subtitle,
                            color = MiuixTheme.colorScheme.onSurface,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = body,
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            fontSize = 11.sp,
                            lineHeight = 17.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    minHeight = 42.dp,
                    cornerRadius = 13.dp,
                    colors = ButtonDefaults.buttonColors(
                        color = MiuixTheme.colorScheme.primary,
                        contentColor = MiuixTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(text = "我知道了", style = MiuixTheme.textStyles.button, fontSize = 13.sp)
                }
            }
        }
    }
}

internal val privacySections = listOf(
    "1. 总则与同意" to
        "本政策适用于「刻度」（包名 com.tick.app）。开始使用即表示你已阅读并同意本政策。若不同意，请停止使用并卸载本应用。",
    "2. 数据只保存在本机" to
        "所有打卡记录、复发记录、备注、日期、设置、通知设置和 AI 配置都只保存在这台设备的应用私有存储中。API Key 使用 Android Keystore 加密保存。刻度没有账号系统，没有云端数据库，也没有开发者服务器。",
    "3. 不收集、不上传、不共享" to
        "刻度不收集个人身份信息，不接入广告、统计或推送 SDK，不读取通讯录、位置、相册、麦克风或相机，不向维护者或任何第三方出售、出租或共享你的本地数据。",
    "4. 网络、第三方 AI 与更新检查" to
        "只有当你主动配置并使用 AI 伴侣时，应用才会从这台设备直接向你填写的模型服务地址发送对话内容、人格提示和必要上下文。数据不经过维护者。第三方模型服务商按其自身隐私政策处理数据，其可用性、内容和数据处理行为由你和服务商负责。更新检查默认关闭；开启后每天最多请求一次 GitHub Releases 的版本信息，不会上传打卡、备注或 AI 对话。GitHub 会按其政策记录请求 IP 和访问日志。",
    "5. 权限" to
        "通知权限用于实时活动、每日提醒和打卡确认；开机接收器用于在重启后恢复提醒。拒绝通知权限不影响本地记录。应用不申请位置、通讯录、相机、麦克风等敏感权限。",
    "6. 删除、备份与迁移" to
        "你可以在应用内删除单条记录或清空全部本地数据；卸载应用会删除应用私有数据。系统备份、换机迁移或云备份由你和你使用的系统控制，维护者无法访问、恢复或导出这些数据。请自行备份重要内容。",
    "7. 安全" to
        "刻度采取应用私有存储和 Android Keystore 等合理措施保护数据，但任何设备或系统都无法保证绝对安全。设备被 root、越狱、感染恶意软件或被他人接触，可能导致数据泄露。请使用系统锁屏和可信设备。",
    "8. 健康与医疗免责" to
        "刻度是自我管理与记录工具，不是医疗器械，不提供诊断、治疗、急救或心理治疗，不能替代专业医疗或心理帮助。打卡、统计、提醒和 AI 内容仅供参考。如果你或他人存在伤害自己或他人的紧急风险，请立即联系当地急救、危机热线或可信任的人。一次复发不代表失败。",
    "9. AI 内容免责" to
        "AI 回复由第三方模型生成，可能错误、不完整、有偏见、过时或不适用于你的情况，不构成医疗、法律、财务或其他专业建议。不要仅凭 AI 输出做重大决定。维护者不对第三方模型的输出、延迟或不可用负责。",
    "10. 服务与数据风险" to
        "本应用按现状提供，不保证不中断、无错误或适合特定目的。通知和提醒依赖系统调度，可能延迟或不响；本地数据可能因卸载、清除、设备故障或系统策略而丢失。你理解并自行承担使用本应用的风险。",
    "11. 责任限制" to
        "在法律允许的最大范围内，维护者不对任何间接、附带、特殊、惩罚性或后果性损害承担责任，包括但不限于数据丢失、设备损坏、利润或机会损失、情绪或健康相关决定，以及第三方服务的行为。某些司法辖区不允许排除默示保证或限制责任，本政策不排除依法不可排除的责任。",
    "12. 未成年人" to
        "本应用不面向未满 14 周岁的儿童。未成年人应在监护人同意和指导下使用。",
    "13. 政策更新与联系" to
        "本政策可能随版本更新。重大变化会在应用内提示，继续使用视为接受更新。如有疑问，可通过应用项目页或应用内反馈渠道联系维护者。",
    "14. 适用法律" to
        "本政策不排除你所在地区强制性消费者保护或隐私法律赋予你的权利。因本政策产生的争议，双方应先友好协商解决。",
    "15. 版本更新" to
        "更新检查默认关闭。你可以手动检查更新，或主动开启每日自动检查；自动检查每天最多一次，只请求 GitHub Releases 的版本信息，不上传打卡记录、备注、AI 对话或设备标识。GitHub 的服务器会看到请求 IP 和时间，并按其隐私政策处理访问日志。下载的安装包必须通过 SHA-256 校验和应用签名校验后才会交给系统安装器；最终是否安装由你确认。更新检查可以随时在设置中关闭。",
)

@Composable
private fun AppearanceCard(
    settings: AppSettings,
    onPaletteChange: (PaletteStyle) -> Unit,
    onDarkModeChange: (DarkMode) -> Unit,
    onDockChange: (DockSettings) -> Unit,
    controlBackdrop: Backdrop,
) {
    var showGlassParams by rememberSaveable { mutableStateOf(false) }
    val glassArrowRotation by animateFloatAsState(
        targetValue = if (showGlassParams) -90f else 90f,
        animationSpec = TickMotion.expand(),
        label = "glassParamsArrow",
    )
    SectionCard {
        Text(
            text = "配色风格",
            style = MiuixTheme.textStyles.subtitle,
            color = MiuixTheme.colorScheme.onSurface,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = if (settings.palette == PaletteStyle.MONET) {
                "跟随系统壁纸取色，每台设备都有一点不同。"
            } else {
                "经典 HyperOS 蓝，克制、清晰。"
            },
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(10.dp))
        SegmentedOptions(
            options = listOf(
                PaletteStyle.MONET to "莫奈动态取色",
                PaletteStyle.CLASSIC to "经典 HyperOS",
            ),
            selected = settings.palette,
            onSelect = onPaletteChange,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "深浅模式",
            style = MiuixTheme.textStyles.subtitle,
            color = MiuixTheme.colorScheme.onSurface,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(10.dp))
        SegmentedOptions(
            options = listOf(
                DarkMode.SYSTEM to "跟随系统",
                DarkMode.LIGHT to "浅色",
                DarkMode.DARK to "深色",
            ),
            selected = settings.darkMode,
            onSelect = onDarkModeChange,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "导航质感",
            style = MiuixTheme.textStyles.subtitle,
            color = MiuixTheme.colorScheme.onSurface,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = when (settings.dock.style) {
                DockStyle.GLASS -> "完整液态玻璃：模糊、镜头折射、高光与内阴影"
                DockStyle.BLUR -> "只保留背景模糊，最快也最清爽"
                DockStyle.SOLID -> "纯色 Dock，省电模式下的稳定回退"
            },
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            fontSize = 10.5.sp,
        )
        Spacer(Modifier.height(10.dp))
        SegmentedOptions(
            options = listOf(
                DockStyle.GLASS to "玻璃",
                DockStyle.BLUR to "模糊",
                DockStyle.SOLID to "纯色",
            ),
            selected = settings.dock.style,
            onSelect = { onDockChange(settings.dock.copy(style = it)) },
        )

        if (settings.dock.style != DockStyle.SOLID) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showGlassParams = !showGlassParams }
                    .padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (settings.dock.style == DockStyle.BLUR) "模糊参数" else "玻璃参数",
                    modifier = Modifier.weight(1f),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurface,
                    fontSize = 12.5.sp,
                )
                Text(
                    text = if (showGlassParams) "收起" else "展开",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 10.5.sp,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = MiuixIcons.Regular.ChevronForward,
                    contentDescription = if (showGlassParams) "收起参数" else "展开参数",
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(glassArrowRotation),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }

        AnimatedVisibility(
            visible = showGlassParams && settings.dock.style != DockStyle.SOLID,
            enter = expandVertically(
                animationSpec = TickMotion.expand(),
                expandFrom = Alignment.Top,
            ),
            exit = shrinkVertically(
                animationSpec = TickMotion.expand(),
                shrinkTowards = Alignment.Top,
            ),
        ) {
            Column {
            when (settings.dock.style) {
            DockStyle.GLASS -> {
                Spacer(Modifier.height(14.dp))
                DockSlider(
                    label = "模糊半径",
                    value = settings.dock.blurRadius,
                    range = 0f..20f,
                    unit = "dp",
                    controlBackdrop = controlBackdrop,
                ) { onDockChange(settings.dock.copy(blurRadius = it)) }
                DockSlider(
                    label = "折射高度",
                    value = settings.dock.refractionHeight,
                    range = 0f..32f,
                    unit = "dp",
                    controlBackdrop = controlBackdrop,
                ) { onDockChange(settings.dock.copy(refractionHeight = it)) }
                DockSlider(
                    label = "折射强度",
                    value = settings.dock.refractionAmount,
                    range = 0f..32f,
                    unit = "dp",
                    controlBackdrop = controlBackdrop,
                ) { onDockChange(settings.dock.copy(refractionAmount = it)) }
                SwitchRow(
                    title = "色散",
                    subtitle = "按下玻璃时出现轻微彩虹边缘",
                    checked = settings.dock.chromaticAberration,
                    onCheckedChange = { onDockChange(settings.dock.copy(chromaticAberration = it)) },
                    controlBackdrop = controlBackdrop,
                )
                SwitchRow(
                    title = "高光",
                    subtitle = "玻璃边缘的亮线",
                    checked = settings.dock.highlight,
                    onCheckedChange = { onDockChange(settings.dock.copy(highlight = it)) },
                    controlBackdrop = controlBackdrop,
                )
            }

            DockStyle.BLUR -> {
                Spacer(Modifier.height(14.dp))
                DockSlider(
                    label = "模糊半径",
                    value = settings.dock.blurRadius,
                    range = 0f..20f,
                    unit = "dp",
                    controlBackdrop = controlBackdrop,
                ) { onDockChange(settings.dock.copy(blurRadius = it)) }
            }

            DockStyle.SOLID -> Unit
            }
            }
        }
    }
}

@Composable
private fun UpdateCard(
    settings: AppSettings,
    onAutoUpdateCheckChange: (Boolean) -> Unit,
    controlBackdrop: Backdrop,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isLight = MiuixTheme.colorScheme.surface.luminance() >= 0.5f
    val currentVersion = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        }.getOrDefault("")
    }
    var checking by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    SectionCard {
        Text(
            text = "刻度 $currentVersion",
            style = MiuixTheme.textStyles.headline2,
            color = MiuixTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = "更新只从 GitHub Releases 获取，不上传任何打卡或备注。",
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            fontSize = 10.5.sp,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "自动检查更新",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurface,
                    fontSize = 12.5.sp,
                )
                Text(
                    text = "每天最多一次，仅请求版本信息",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 10.sp,
                )
            }
            LiquidToggle(
                selected = { settings.autoUpdateCheck },
                onSelect = onAutoUpdateCheckChange,
                backdrop = controlBackdrop,
                isLightTheme = isLight,
            )
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                if (checking || !UpdateConfig.isConfigured) return@Button
                checking = true
                message = null
                updateInfo = null
                scope.launch {
                    val result = UpdateChecker.check()
                    checking = false
                    result.onSuccess { info ->
                        if (info == null) {
                            message = "已经是最新版。"
                        } else {
                            updateInfo = info
                        }
                    }.onFailure {
                        message = "检查失败：${it.message ?: "网络错误"}"
                    }
                }
            },
            enabled = !checking && !downloading && UpdateConfig.isConfigured,
            modifier = Modifier.fillMaxWidth(),
            minHeight = 42.dp,
            cornerRadius = 12.dp,
            colors = ButtonDefaults.buttonColors(
                color = MiuixTheme.colorScheme.surfaceContainerHigh,
                contentColor = MiuixTheme.colorScheme.primary,
            ),
        ) {
            if (checking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    size = 16.dp,
                )
                Spacer(Modifier.width(7.dp))
                Text(text = "检查中…", fontSize = 12.sp)
            } else {
                Text(
                    text = if (UpdateConfig.isConfigured) "检查更新" else "更新源未配置",
                    fontSize = 12.sp,
                )
            }
        }
        if (downloading) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth(),
                height = 6.dp,
            )
        }
        message?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                text = it,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 10.5.sp,
            )
        }
        updateInfo?.let { info ->
            Spacer(Modifier.height(10.dp))
            Text(
                text = "新版本 ${info.versionName}",
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurface,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
            )
            if (info.notes.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = info.notes,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 10.5.sp,
                    maxLines = 4,
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (downloading) return@Button
                    downloading = true
                    progress = 0f
                    message = null
                    scope.launch {
                        UpdateChecker.download(context, info) { progress = it }
                            .onSuccess { file ->
                                downloading = false
                                runCatching { UpdateChecker.install(context, file) }
                                    .onFailure { message = "无法打开安装器：${it.message}" }
                            }
                            .onFailure {
                                downloading = false
                                message = "下载失败：${it.message ?: "网络错误"}"
                            }
                    }
                },
                enabled = !downloading,
                modifier = Modifier.fillMaxWidth(),
                minHeight = 42.dp,
                cornerRadius = 12.dp,
                colors = ButtonDefaults.buttonColors(
                    color = MiuixTheme.colorScheme.primary,
                    contentColor = MiuixTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(text = if (downloading) "下载中… ${(progress * 100).toInt()}%" else "下载并安装", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun NotificationsCard(
    notifications: NotificationSettings,
    allowed: Boolean,
    onUpdate: (NotificationSettings) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    controlBackdrop: Backdrop,
) {
    val context = LocalContext.current
    val isLight = MiuixTheme.colorScheme.surface.luminance() >= 0.5f
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "实时活动",
                    style = MiuixTheme.textStyles.headline2,
                    color = MiuixTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "通知栏显示已稳住计时与今日打卡状态",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 10.5.sp,
                )
            }
            LiquidToggle(
                selected = { notifications.liveUpdate },
                onSelect = {
                    onUpdate(notifications.copy(liveUpdate = it))
                    if (it && !allowed) onRequestPermission()
                },
                backdrop = controlBackdrop,
                isLightTheme = isLight,
            )
        }
        Spacer(Modifier.height(12.dp))
        SoftDivider()
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "每日打卡提醒",
                    style = MiuixTheme.textStyles.headline2,
                    color = MiuixTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "到点提醒，不会替你做假打卡",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 10.5.sp,
                )
            }
            LiquidToggle(
                selected = { notifications.dailyReminder },
                onSelect = {
                    onUpdate(notifications.copy(dailyReminder = it))
                    if (it && !allowed) onRequestPermission()
                },
                backdrop = controlBackdrop,
                isLightTheme = isLight,
            )
        }
        if (notifications.dailyReminder) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                    .clickable {
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                onUpdate(
                                    notifications.copy(
                                        reminderHour = hour,
                                        reminderMinute = minute,
                                    ),
                                )
                            },
                            notifications.reminderHour,
                            notifications.reminderMinute,
                            true,
                        ).show()
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "提醒时间",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = String.format(
                        Locale.CHINA,
                        "%02d:%02d",
                        notifications.reminderHour,
                        notifications.reminderMinute,
                    ),
                    style = MiuixTheme.textStyles.footnote1.copy(fontFeatureSettings = "tnum"),
                    color = MiuixTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        SoftDivider()
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "打卡成功通知",
                    style = MiuixTheme.textStyles.headline2,
                    color = MiuixTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "打卡后发一条轻量确认",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 10.5.sp,
                )
            }
            LiquidToggle(
                selected = { notifications.checkInConfirmation },
                onSelect = {
                    onUpdate(notifications.copy(checkInConfirmation = it))
                    if (it && !allowed) onRequestPermission()
                },
                backdrop = controlBackdrop,
                isLightTheme = isLight,
            )
        }
        if (!allowed) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(MiuixTheme.colorScheme.errorContainer)
                    .clickable(onClick = onOpenSettings)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = MiuixIcons.Regular.Info,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MiuixTheme.colorScheme.error,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "通知权限未开启，提醒和实时活动不会显示",
                    modifier = Modifier.weight(1f),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.error,
                    fontSize = 10.5.sp,
                )
                Text(
                    text = "去开启",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.error,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "实时活动使用系统计时，不常驻后台；省电模式下模糊会自动降级，减少耗电。",
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun DockSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    controlBackdrop: Backdrop,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                fontSize = 11.5.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${value.roundToInt()} $unit",
                style = MiuixTheme.textStyles.footnote2.copy(fontFeatureSettings = "tnum"),
                color = MiuixTheme.colorScheme.primary,
                fontSize = 10.5.sp,
            )
        }
        LiquidSlider(
            value = { value },
            onValueChange = onValueChange,
            valueRange = range,
            visibilityThreshold = 0.01f,
            backdrop = controlBackdrop,
            modifier = Modifier.fillMaxWidth(),
            isLightTheme = MiuixTheme.colorScheme.surface.luminance() >= 0.5f,
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    controlBackdrop: Backdrop,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurface,
                fontSize = 12.5.sp,
            )
            Text(
                text = subtitle,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 10.sp,
            )
        }
        LiquidToggle(
            selected = { checked },
            onSelect = onCheckedChange,
            backdrop = controlBackdrop,
            isLightTheme = MiuixTheme.colorScheme.surface.luminance() >= 0.5f,
        )
    }
}

@Composable
private fun TestLine(text: String, success: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (success) MiuixIcons.Basic.Check else MiuixIcons.Regular.Info,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = if (success) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.error,
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = text,
            style = MiuixTheme.textStyles.footnote2,
            color = if (success) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.error,
            fontSize = 10.5.sp,
        )
    }
}

@Composable
private fun PresetDialog(
    selectedId: String,
    onDismiss: () -> Unit,
    onSelect: (com.tick.app.data.AiPreset) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
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
                    text = "选择 AI 平台",
                    style = MiuixTheme.textStyles.title4,
                    color = MiuixTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "选择后会自动填好地址和推荐模型。",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.height(10.dp))
                aiPresets.forEach { preset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(13.dp))
                            .clickable { onSelect(preset) }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = preset.label,
                                style = MiuixTheme.textStyles.body1,
                                color = MiuixTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                            )
                            Text(
                                text = preset.hint,
                                style = MiuixTheme.textStyles.footnote2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                fontSize = 10.sp,
                            )
                        }
                        if (preset.id == selectedId) {
                            Icon(
                                imageVector = MiuixIcons.Basic.Check,
                                contentDescription = "已选择",
                                modifier = Modifier.size(18.dp),
                                tint = MiuixTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}
