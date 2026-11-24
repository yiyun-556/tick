package com.tick.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tick.app.data.EventType
import com.tick.app.data.TickEvent
import com.tick.app.data.TickViewModel
import com.tick.app.notify.TickNotifications
import com.tick.app.ui.components.ConfirmOverlay
import com.tick.app.ui.components.rememberBlurEffectsEnabled
import com.tick.app.ui.components.RecordSheet
import com.tick.app.ui.settings.PrivacyConsentScreen
import com.tick.app.ui.sos.SosScreen
import com.tick.app.ui.theme.TickMotion
import com.tick.app.ui.theme.TickTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlinx.coroutines.delay

private data class RecordRequest(
    val initialType: EventType,
    val allowTypeSwitch: Boolean,
)

@Composable
fun TickApp(viewModel: TickViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val appContext = LocalContext.current
    val privacyPrefs = remember {
        appContext.getSharedPreferences("tick_privacy", Context.MODE_PRIVATE)
    }
    var privacyAccepted by remember {
        mutableStateOf(privacyPrefs.getBoolean("accepted_v1", false))
    }

    if (!privacyAccepted) {
        TickTheme(settings = settings) {
            PrivacyConsentScreen(
                onAccept = {
                    privacyPrefs.edit().putBoolean("accepted_v1", true).apply()
                    privacyAccepted = true
                },
            )
        }
        return
    }

    TickTheme(settings = settings) {
        val events by viewModel.events.collectAsStateWithLifecycle()
        val journeyStart by viewModel.journeyStart.collectAsStateWithLifecycle()
        val dayNotes by viewModel.dayNotes.collectAsStateWithLifecycle()
        val messages by viewModel.messages.collectAsStateWithLifecycle()
        val isStreaming by viewModel.isStreaming.collectAsStateWithLifecycle()
        val streamingMessageId by viewModel.streamingMessageId.collectAsStateWithLifecycle()
        val testState by viewModel.testState.collectAsStateWithLifecycle()

        var tabIndex by rememberSaveable { mutableIntStateOf(0) }
        var recordRequest by remember { mutableStateOf<RecordRequest?>(null) }
        var sosVisible by rememberSaveable { mutableStateOf(false) }
        var pendingDelete by remember { mutableStateOf<TickEvent?>(null) }
        var clearConfirm by rememberSaveable { mutableStateOf(false) }

        val context = LocalContext.current
        val haptic = LocalHapticFeedback.current
        val blurEnabled = rememberBlurEffectsEnabled()
        var notificationsAllowed by remember { mutableStateOf(TickNotifications.canPost(context)) }
        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            notificationsAllowed = granted && TickNotifications.canPost(context)
            viewModel.onNotificationPermissionChanged()
        }
        val requestNotificationPermission: () -> Unit = {
            if (Build.VERSION.SDK_INT >= 33) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.onNotificationPermissionChanged()
            }
        }
        val openNotificationSettings: () -> Unit = {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                )
            }
        }

        LaunchedEffect(Unit) {
            val wantsNotifications = settings.notifications.liveUpdate ||
                settings.notifications.dailyReminder ||
                settings.notifications.checkInConfirmation
            if (Build.VERSION.SDK_INT >= 33 &&
                wantsNotifications &&
                !notificationsAllowed &&
                !viewModel.wasNotificationPermissionAsked()
            ) {
                delay(900)
                viewModel.markNotificationPermissionAsked()
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    val current = TickNotifications.canPost(context)
                    if (current != notificationsAllowed) {
                        notificationsAllowed = current
                        viewModel.onNotificationPermissionChanged()
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        val overlayActive = recordRequest != null || sosVisible || pendingDelete != null || clearConfirm
        val blurRadius by animateDpAsState(
            targetValue = if (overlayActive) 18.dp else 0.dp,
            animationSpec = TickMotion.enter(),
            label = "overlayBlur",
        )

        BackHandler(enabled = overlayActive) {
            when {
                sosVisible -> sosVisible = false
                recordRequest != null -> recordRequest = null
                pendingDelete != null -> pendingDelete = null
                clearConfirm -> clearConfirm = false
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.surface),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(if (blurEnabled) blurRadius else 0.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .then(if (overlayActive) Modifier.clearAndSetSemantics { } else Modifier),
            ) {
                MainShell(
                    tab = TickTab.entries[tabIndex.coerceIn(0, TickTab.entries.lastIndex)],
                    onTabChange = { tabIndex = it.ordinal },
                    events = events,
                    journeyStart = journeyStart,
                    dayNotes = dayNotes,
                    onUpdateDayNote = viewModel::updateDayNote,
                    settings = settings,
                    testState = testState,
                    onCheckIn = { viewModel.recordCheckIn() },
                    onOpenRecord = {
                        recordRequest = RecordRequest(EventType.RELAPSE, false)
                    },
                    onSelectCheckInTime = {
                        recordRequest = RecordRequest(EventType.CHECK_IN, false)
                    },
                    onOpenSos = {
                        recordRequest = null
                        pendingDelete = null
                        clearConfirm = false
                        sosVisible = true
                    },
                    onBackfill = {
                        recordRequest = RecordRequest(EventType.RELAPSE, true)
                    },
                    onDelete = { pendingDelete = it },
                    onPaletteChange = viewModel::updatePalette,
                    onDarkModeChange = viewModel::updateDarkMode,
                    onDockChange = viewModel::updateDock,
                    onAutoUpdateCheckChange = viewModel::updateAutoUpdateCheck,
                    onSaveAi = viewModel::saveAiConfig,
                    onTestAi = viewModel::testConnection,
                    onResetTest = viewModel::resetTestState,
                    onClearData = { clearConfirm = true },
                    notificationsAllowed = notificationsAllowed,
                    onUpdateNotifications = viewModel::updateNotifications,
                    onRequestNotificationPermission = requestNotificationPermission,
                    onOpenNotificationSettings = openNotificationSettings,
                    blurEnabled = blurEnabled,
                )
            }

            AnimatedVisibility(
                visible = recordRequest != null,
                enter = fadeIn(TickMotion.enter()) +
                    scaleIn(initialScale = 0.985f, animationSpec = TickMotion.enter()),
                exit = fadeOut(TickMotion.exit()) +
                    scaleOut(targetScale = 0.985f, animationSpec = TickMotion.exit()),
            ) {
                val request = remember { recordRequest }
                if (request != null) {
                    RecordSheet(
                        initialType = request.initialType,
                        allowTypeSwitch = request.allowTypeSwitch,
                        hasCheckInOn = viewModel::hasCheckInOn,
                        onDismiss = { recordRequest = null },
                        onConfirm = { type, at, trigger, note ->
                            val added = viewModel.addEvent(type, at, trigger, note)
                            if (added) {
                                recordRequest = null
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                Toast.makeText(
                                    context,
                                    if (type == EventType.CHECK_IN) "记下了。今天这一格，属于你。" else "记下了。新的刻度，从现在开始。",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            } else {
                                Toast.makeText(context, "这一天已经打过卡了。", Toast.LENGTH_SHORT).show()
                            }
                        },
                    )
                }
            }

            AnimatedVisibility(
                visible = sosVisible,
                enter = fadeIn(TickMotion.enter()) +
                    scaleIn(initialScale = 0.99f, animationSpec = TickMotion.enter()),
                exit = fadeOut(TickMotion.exit()) +
                    scaleOut(targetScale = 0.99f, animationSpec = TickMotion.exit()),
            ) {
                SosScreen(
                    settings = settings,
                    messages = messages,
                    isStreaming = isStreaming,
                    streamingMessageId = streamingMessageId,
                    onClose = { sosVisible = false },
                    onSend = viewModel::sendMessage,
                    onClearChat = viewModel::clearChat,
                    onOpenSettings = {
                        sosVisible = false
                        tabIndex = TickTab.SETTINGS.ordinal
                    },
                )
            }

            AnimatedVisibility(
                visible = pendingDelete != null,
                enter = fadeIn(TickMotion.enter()) +
                    scaleIn(initialScale = 0.975f, animationSpec = TickMotion.enter()),
                exit = fadeOut(TickMotion.exit()) +
                    scaleOut(targetScale = 0.975f, animationSpec = TickMotion.exit()),
            ) {
                val target = remember { pendingDelete }
                if (target != null) {
                    ConfirmOverlay(
                        title = "删除这条记录？",
                        message = "删除后，首页的统计会立刻重新计算。这个操作不能撤销。",
                        confirmLabel = "删除",
                        destructive = true,
                        onDismiss = { pendingDelete = null },
                        onConfirm = {
                            viewModel.deleteEvent(target.id)
                            pendingDelete = null
                            Toast.makeText(context, "已删除，统计已经重新计算。", Toast.LENGTH_SHORT).show()
                        },
                    )
                }
            }

            AnimatedVisibility(
                visible = clearConfirm,
                enter = fadeIn(TickMotion.enter()) +
                    scaleIn(initialScale = 0.975f, animationSpec = TickMotion.enter()),
                exit = fadeOut(TickMotion.exit()) +
                    scaleOut(targetScale = 0.975f, animationSpec = TickMotion.exit()),
            ) {
                ConfirmOverlay(
                    title = "清空全部本地数据？",
                    message = "打卡记录、对话、主题设置、AI 配置和 API Key 都会从这台手机上删除，无法恢复。",
                    confirmLabel = "清空所有数据",
                    destructive = true,
                    onDismiss = { clearConfirm = false },
                    onConfirm = {
                        viewModel.clearAllData()
                        clearConfirm = false
                        sosVisible = false
                        recordRequest = null
                        pendingDelete = null
                        tabIndex = TickTab.HOME.ordinal
                        Toast.makeText(context, "已经清空。可以重新开始，也可以先歇一会儿。", Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }
    }
}
