package com.tick.app.ui

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.effect
import com.tick.app.data.AiTestState
import com.tick.app.data.AppSettings
import com.tick.app.data.DarkMode
import com.tick.app.data.DockSettings
import com.tick.app.data.DockStyle
import com.tick.app.data.PaletteStyle
import com.tick.app.data.TickEvent
import com.tick.app.ui.components.SmallActionButton
import com.tick.app.ui.components.TickIcons
import com.tick.app.ui.components.formatWeekday
import com.tick.app.ui.history.HistoryScreen
import com.tick.app.ui.home.HomeScreen
import com.tick.app.ui.liquid.LiquidBottomTab
import com.tick.app.ui.liquid.LiquidBottomTabs
import com.kyant.shapes.Capsule
import com.tick.app.ui.settings.SettingsScreen
import com.tick.app.ui.theme.TickMotion
import kotlinx.coroutines.delay
import org.intellij.lang.annotations.Language
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill
import top.yukonga.miuix.kmp.icon.extended.Recent
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme

enum class TickTab(val label: String) {
    HOME("刻度"),
    HISTORY("记录"),
    SETTINGS("设置"),
}

@Composable
fun MainShell(
    tab: TickTab,
    onTabChange: (TickTab) -> Unit,
    events: List<TickEvent>,
    journeyStart: Long,
    dayNotes: Map<Long, String>,
    onUpdateDayNote: (Long, String) -> Unit,
    settings: AppSettings,
    testState: AiTestState,
    onCheckIn: () -> Boolean,
    onOpenRecord: () -> Unit,
    onSelectCheckInTime: () -> Unit,
    onOpenSos: () -> Unit,
    onBackfill: () -> Unit,
    onDelete: (TickEvent) -> Unit,
    onPaletteChange: (PaletteStyle) -> Unit,
    onDarkModeChange: (DarkMode) -> Unit,
    onDockChange: (DockSettings) -> Unit,
    onAutoUpdateCheckChange: (Boolean) -> Unit,
    onSaveAi: (com.tick.app.data.AiConfig) -> Unit,
    onTestAi: (com.tick.app.data.AiConfig) -> Unit,
    onResetTest: () -> Unit,
    onClearData: () -> Unit,
    notificationsAllowed: Boolean,
    onUpdateNotifications: (com.tick.app.data.NotificationSettings) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    blurEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val backdrop = rememberLayerBackdrop()
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val useGradientBlur = blurEnabled && Build.VERSION.SDK_INT >= 33
    val requestedStyle = settings.dock.style
    val effectiveStyle = when {
        !blurEnabled -> DockStyle.SOLID
        requestedStyle == DockStyle.GLASS && Build.VERSION.SDK_INT < 33 -> DockStyle.BLUR
        requestedStyle == DockStyle.BLUR && Build.VERSION.SDK_INT < 31 -> DockStyle.SOLID
        else -> requestedStyle
    }
    val useBackdrop = useGradientBlur || effectiveStyle != DockStyle.SOLID
    val floatingNavHeight = 64.dp
    val navGap = 10.dp
    val topBarVisualHeight = statusTop + 60.dp
    val bottomBarTop = navBottom + navGap + floatingNavHeight
    val topPadding = topBarVisualHeight + 12.dp
    val bottomPadding = bottomBarTop + if (tab == TickTab.HOME) 76.dp else 18.dp
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (useBackdrop) {
                        Modifier.layerBackdrop(backdrop)
                    } else {
                        Modifier
                    },
                )
                .background(MiuixTheme.colorScheme.surface),
        ) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    fadeIn(TickMotion.enter()) togetherWith fadeOut(TickMotion.exit())
                },
                label = "pageTransition",
                modifier = Modifier.fillMaxSize(),
            ) { currentTab ->
                when (currentTab) {
                TickTab.HOME -> HomeScreen(
                    events = events,
                    journeyStart = journeyStart,
                    dayNotes = dayNotes,
                    onUpdateDayNote = onUpdateDayNote,
                    topPadding = topPadding,
                    bottomPadding = bottomPadding,
                    onCheckIn = onCheckIn,
                    onOpenRecord = onOpenRecord,
                    onSelectCheckInTime = onSelectCheckInTime,
                )

                TickTab.HISTORY -> HistoryScreen(
                    events = events,
                    topPadding = topPadding,
                    bottomPadding = bottomPadding,
                    onBackfill = onBackfill,
                    onDelete = onDelete,
                )

                TickTab.SETTINGS -> SettingsScreen(
                    settings = settings,
                    testState = testState,
                    topPadding = topPadding,
                    bottomPadding = bottomPadding,
                    onPaletteChange = onPaletteChange,
                    onDarkModeChange = onDarkModeChange,
                    onDockChange = onDockChange,
                    onAutoUpdateCheckChange = onAutoUpdateCheckChange,
                    onSaveAi = onSaveAi,
                    onTestAi = onTestAi,
                    onResetTest = onResetTest,
                    onClearData = onClearData,
                    notificationsAllowed = notificationsAllowed,
                    onUpdateNotifications = onUpdateNotifications,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onOpenNotificationSettings = onOpenNotificationSettings,
                )
                }
            }
        }

        GlassTopBar(
            modifier = Modifier.align(Alignment.TopCenter).zIndex(3f),
            backdrop = backdrop,
            tab = tab,
            statusTop = statusTop,
            useBlur = useGradientBlur,
            onBackfill = onBackfill,
        )

        if (tab == TickTab.HOME) {
            EmergencyBar(
                onClick = onOpenSos,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = bottomBarTop + 10.dp,
                    )
                    .zIndex(2f),
            )
        }

        BottomDock(
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(3f),
            backdrop = backdrop,
            selectedIndex = tab.ordinal,
            navBottom = navBottom,
            style = effectiveStyle,
            dock = settings.dock,
            onSelected = { onTabChange(TickTab.entries[it]) },
        )
    }
}

@Composable
private fun GlassTopBar(
    backdrop: Backdrop,
    tab: TickTab,
    statusTop: androidx.compose.ui.unit.Dp,
    useBlur: Boolean,
    onBackfill: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val surface = MiuixTheme.colorScheme.surface
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            clock = System.currentTimeMillis()
        }
    }
    val title = when (tab) {
        TickTab.HOME -> "刻度"
        TickTab.HISTORY -> "记录"
        TickTab.SETTINGS -> "设置"
    }
    val subtitle = when (tab) {
        TickTab.HOME -> formatWeekday(clock)
        TickTab.HISTORY -> "每一次，都算数"
        TickTab.SETTINGS -> "按你的节奏来"
    }
    // The visual blur extends 32dp below the 64dp text row, so the fade lands
    // well below the subtitle instead of stopping exactly on it.
    val base = modifier
        .fillMaxWidth()
        .height(statusTop + 60.dp)
    val painted = if (useBlur) {
        base.drawPlainBackdrop(
            backdrop = backdrop,
            shape = { RectangleShape },
            effects = {
                blur(8.dp.toPx())
                if (Build.VERSION.SDK_INT >= 33) {
                    topAlphaMask(tint = surface, intensity = 0.55f)
                }
            },
        )
    } else {
        base.background(surface)
    }
    Box(modifier = painted) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusTop, start = 20.dp, end = 12.dp)
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MiuixTheme.textStyles.title4,
                    color = MiuixTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                )
                Text(
                    text = subtitle,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 11.sp,
                )
            }
            if (tab == TickTab.HISTORY) {
                SmallActionButton(text = "补录", onClick = onBackfill, icon = MiuixIcons.Regular.Add)
            }
        }
    }
}

@RequiresApi(33)
@Language("AGSL")
private const val TOP_ALPHA_MASK_SHADER = """
uniform shader content;

uniform float2 size;
layout(color) uniform half4 tint;
uniform float tintIntensity;

half4 main(float2 coord) {
    float mask = smoothstep(size.y, size.y * 0.84, coord.y);
    return mix(content.eval(coord) * mask, tint * mask, tintIntensity);
}
"""

@RequiresApi(33)
private fun BackdropEffectScope.topAlphaMask(tint: Color, intensity: Float) {
    val shader: RuntimeShader = obtainRuntimeShader("tick_top_alpha_mask_v4", TOP_ALPHA_MASK_SHADER)
    shader.setFloatUniform("size", size.width, size.height)
    shader.setColorUniform("tint", tint.toArgb())
    shader.setFloatUniform("tintIntensity", intensity)
    effect(RenderEffect.createRuntimeShaderEffect(shader, "content"))
}

@Composable
private fun BottomDock(
    backdrop: Backdrop,
    selectedIndex: Int,
    navBottom: androidx.compose.ui.unit.Dp,
    style: DockStyle,
    dock: DockSettings,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val surface = MiuixTheme.colorScheme.surface
    val isLight = surface.luminance() >= 0.5f
    val dockModifier = modifier
        .padding(start = 18.dp, end = 18.dp, bottom = navBottom + 10.dp)
        .fillMaxWidth()
        .height(floatingNavHeight())

    Box(modifier = dockModifier) {
        AnimatedContent(
            targetState = style,
            transitionSpec = {
                fadeIn(TickMotion.enter()) togetherWith fadeOut(TickMotion.exit())
            },
            label = "dockStyle",
        ) { current ->
            when (current) {
                DockStyle.GLASS -> GlassDock(
                    backdrop = backdrop,
                    selectedIndex = selectedIndex,
                    isLight = isLight,
                    dock = dock,
                    onSelected = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelected(it)
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                DockStyle.BLUR -> FallbackDock(
                    backdrop = backdrop,
                    selectedIndex = selectedIndex,
                    blur = true,
                    blurRadius = dock.blurRadius,
                    containerColor = if (isLight) {
                        Color(0xFFFAFAFA).copy(alpha = 0.55f)
                    } else {
                        Color(0xFF121212).copy(alpha = 0.62f)
                    },
                    onSelected = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelected(it)
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                DockStyle.SOLID -> FallbackDock(
                    backdrop = backdrop,
                    selectedIndex = selectedIndex,
                    blur = false,
                    blurRadius = 0f,
                    containerColor = surface.copy(alpha = 0.96f),
                    onSelected = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelected(it)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun GlassDock(
    backdrop: Backdrop,
    selectedIndex: Int,
    isLight: Boolean,
    dock: DockSettings,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidBottomTabs(
        selectedTabIndex = { selectedIndex },
        onTabSelected = onSelected,
        backdrop = backdrop,
        tabsCount = TickTab.entries.size,
        isLightTheme = isLight,
        blurRadius = dock.blurRadius.dp,
        refractionHeight = dock.refractionHeight.dp,
        refractionAmount = dock.refractionAmount.dp,
        chromaticAberration = dock.chromaticAberration,
        highlightEnabled = dock.highlight,
        modifier = modifier,
    ) {
        TickTab.entries.forEachIndexed { index, tab ->
            LiquidBottomTab(onClick = { onSelected(index) }) {
                DockContent(tab, selected = false, glass = true)
            }
        }
    }
}

@Composable
private fun FallbackDock(
    backdrop: Backdrop,
    selectedIndex: Int,
    blur: Boolean,
    blurRadius: Float,
    containerColor: Color,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val surface = MiuixTheme.colorScheme.surface
    val shape = Capsule()
    val base = if (blur) {
        modifier
            .drawPlainBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    blur(blurRadius.dp.toPx())
                },
                onDrawSurface = { drawRect(containerColor) },
            )
    } else {
        modifier
            .clip(shape)
            .background(surface.copy(alpha = 0.96f))
            .border(0.7.dp, MiuixTheme.colorScheme.dividerLine, shape)
    }
    BoxWithConstraints(modifier = base) {
        val itemWidth = (maxWidth - 12.dp) / TickTab.entries.size
        val thumbWidth = itemWidth - 12.dp
        val pillX by animateDpAsState(
            targetValue = 6.dp + itemWidth * selectedIndex + 6.dp,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 520f),
            label = "fallbackPill",
        )
        Box(
            modifier = Modifier
                .offset(x = pillX)
                .align(Alignment.CenterStart)
                .width(thumbWidth)
                .height(42.dp)
                .clip(RoundedCornerShape(21.dp))
                .background(MiuixTheme.colorScheme.primaryContainer),
        )
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TickTab.entries.forEach { tab ->
                val selected = tab.ordinal == selectedIndex
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(21.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                        ) { onSelected(tab.ordinal) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    DockContent(tab, selected = selected, glass = false)
                }
            }
        }
    }
}

@Composable
private fun DockContent(tab: TickTab, selected: Boolean, glass: Boolean) {
    val tint = if (glass) {
        MiuixTheme.colorScheme.onSurface
    } else if (selected) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.onSurfaceVariantSummary
    }
    val icon = when (tab) {
        TickTab.HOME -> TickIcons.Home
        TickTab.HISTORY -> MiuixIcons.Regular.Recent
        TickTab.SETTINGS -> MiuixIcons.Regular.Settings
    }
    Icon(
        imageVector = icon,
        contentDescription = tab.label,
        modifier = Modifier.size(21.dp),
        tint = tint,
    )
    Spacer(Modifier.height(3.dp))
    Text(
        text = tab.label,
        color = tint,
        style = MiuixTheme.textStyles.footnote2,
        fontSize = 10.5.sp,
        fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
    )
}

private fun floatingNavHeight() = 64.dp

@Composable
private fun EmergencyBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(19.dp),
        color = MiuixTheme.colorScheme.primary,
        contentColor = MiuixTheme.colorScheme.onPrimary,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MiuixTheme.colorScheme.onPrimary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = MiuixIcons.Regular.FavoritesFill,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = MiuixTheme.colorScheme.onPrimary,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "现在有冲动",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                )
                Text(
                    text = "先慢下来，我陪你过这一阵",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onPrimary.copy(alpha = 0.78f),
                    fontSize = 9.5.sp,
                )
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MiuixTheme.colorScheme.onPrimary.copy(alpha = 0.16f))
                    .padding(horizontal = 11.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "急救",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onPrimary,
                    fontSize = 10.5.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                )
            }
        }
    }
}
