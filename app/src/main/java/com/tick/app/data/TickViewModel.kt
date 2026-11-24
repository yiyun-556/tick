package com.tick.app.data

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tick.app.ai.AiClient
import com.tick.app.ai.AiException
import com.tick.app.ai.ChatTurn
import com.tick.app.ai.CompanionPrompt
import com.tick.app.ai.LocalCompanion
import com.tick.app.notify.TickNotifications
import com.tick.app.update.UpdateScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface AiTestState {
    data object Idle : AiTestState
    data object Testing : AiTestState
    data class Success(val message: String) : AiTestState
    data class Failure(val message: String) : AiTestState
}

class TickViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TickRepository(application)
    private val aiClient = AiClient()

    private val _events = MutableStateFlow(repository.loadEvents().sortedBy { it.at })
    val events: StateFlow<List<TickEvent>> = _events.asStateFlow()

    private val _journeyStart = MutableStateFlow(repository.loadJourneyStart())
    val journeyStart: StateFlow<Long> = _journeyStart.asStateFlow()

    private val _dayNotes = MutableStateFlow(repository.loadDayNotes())
    val dayNotes: StateFlow<Map<Long, String>> = _dayNotes.asStateFlow()

    private val _settings = MutableStateFlow(repository.loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _messages = MutableStateFlow(initialMessages())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _streamingMessageId = MutableStateFlow<String?>(null)
    val streamingMessageId: StateFlow<String?> = _streamingMessageId.asStateFlow()

    private val _testState = MutableStateFlow<AiTestState>(AiTestState.Idle)
    val testState: StateFlow<AiTestState> = _testState.asStateFlow()

    private var streamJob: Job? = null
    private var testJob: Job? = null

    private val dataChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != TickNotifications.ACTION_DATA_CHANGED) return
            _events.value = repository.loadEvents().sortedBy { it.at }
            _journeyStart.value = repository.loadJourneyStart()
        }
    }

    init {
        val app = getApplication<Application>()
        TickNotifications.ensureChannels(app)
        TickNotifications.syncAll(app)
        UpdateScheduler.setEnabled(app, _settings.value.autoUpdateCheck)
        ContextCompat.registerReceiver(
            app,
            dataChangeReceiver,
            IntentFilter(TickNotifications.ACTION_DATA_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    private fun initialMessages(): List<ChatMessage> {
        val stored = repository.loadMessages()
        if (stored.isNotEmpty()) return stored
        val greeting = ChatMessage(
            role = ChatRole.ASSISTANT,
            text = LocalCompanion.greeting(repository.loadSettings().ai.aiName),
            local = true,
        )
        repository.saveMessages(listOf(greeting))
        return listOf(greeting)
    }

    fun addEvent(type: EventType, at: Long, trigger: String? = null, note: String? = null): Boolean {
        val safeAt = at.coerceAtMost(System.currentTimeMillis()).coerceAtLeast(0L)
        if (type == EventType.CHECK_IN && hasCheckInOn(safeAt)) return false
        val event = TickEvent(
            id = UUID.randomUUID().toString(),
            type = type,
            at = safeAt,
            trigger = trigger?.trim()?.takeIf { it.isNotEmpty() },
            note = note?.trim()?.takeIf { it.isNotEmpty() },
        )
        _events.update { current -> (current + event).sortedBy { it.at } }
        repository.saveEvents(_events.value)
        val app = getApplication<Application>()
        TickNotifications.syncLive(app)
        if (type == EventType.CHECK_IN &&
            StatsCalculator.sameDay(event.at, System.currentTimeMillis())
        ) {
            TickNotifications.showCheckInConfirmation(app)
        }
        return true
    }

    fun recordCheckIn(): Boolean {
        val added = addEvent(EventType.CHECK_IN, System.currentTimeMillis())
        return added
    }

    fun updateDayNote(epochDay: Long, text: String) {
        val normalized = text.trim().take(280)
        val updated = _dayNotes.value.toMutableMap().apply {
            if (normalized.isEmpty()) remove(epochDay) else put(epochDay, normalized)
        }
        _dayNotes.value = updated
        repository.saveDayNotes(updated)
    }

    fun deleteEvent(id: String) {
        _events.update { current -> current.filterNot { it.id == id } }
        repository.saveEvents(_events.value)
        TickNotifications.syncLive(getApplication())
    }

    fun hasCheckInOn(timestamp: Long): Boolean = _events.value.any {
        it.type == EventType.CHECK_IN && StatsCalculator.sameDay(it.at, timestamp)
    }

    fun updatePalette(palette: PaletteStyle) {
        _settings.update { it.copy(palette = palette) }
        repository.saveSettings(_settings.value)
    }

    fun updateDarkMode(mode: DarkMode) {
        _settings.update { it.copy(darkMode = mode) }
        repository.saveSettings(_settings.value)
    }

    fun updateDock(dock: DockSettings) {
        _settings.update { it.copy(dock = dock) }
        repository.saveSettings(_settings.value)
    }

    fun updateAutoUpdateCheck(enabled: Boolean) {
        _settings.update { it.copy(autoUpdateCheck = enabled) }
        repository.saveSettings(_settings.value)
        UpdateScheduler.setEnabled(getApplication(), enabled)
    }

    fun updateNotifications(notifications: NotificationSettings) {
        _settings.update { it.copy(notifications = notifications) }
        repository.saveSettings(_settings.value)
        TickNotifications.syncAll(getApplication())
    }

    fun wasNotificationPermissionAsked(): Boolean = repository.wasNotificationPermissionAsked()

    fun markNotificationPermissionAsked() = repository.markNotificationPermissionAsked()

    fun onNotificationPermissionChanged() {
        TickNotifications.syncAll(getApplication())
    }

    fun saveAiConfig(config: AiConfig) {
        testJob?.cancel()
        testJob = null
        val sanitized = config.copy(
            baseUrl = config.baseUrl.trim(),
            model = config.model.trim(),
            apiKey = config.apiKey.trim(),
            aiName = config.aiName.trim().ifBlank { "小刻" },
            persona = config.persona.trim(),
        )
        _settings.update { it.copy(ai = sanitized) }
        repository.saveSettings(_settings.value)
        _testState.value = AiTestState.Idle
    }

    fun resetTestState() {
        testJob?.cancel()
        testJob = null
        _testState.value = AiTestState.Idle
    }

    fun testConnection(config: AiConfig) {
        testJob?.cancel()
        val error = AiClient.validate(config)
        if (error != null) {
            _testState.value = AiTestState.Failure(error)
            return
        }
        _testState.value = AiTestState.Testing
        testJob = viewModelScope.launch {
            try {
                val message = aiClient.testConnection(config)
                _testState.value = AiTestState.Success(message)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _testState.value = AiTestState.Failure(
                    when (e) {
                        is AiException -> e.userMessage
                        else -> "连接失败，请检查地址、模型和网络。"
                    },
                )
            }
        }
    }

    fun sendMessage(rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty() || _isStreaming.value) return

        val config = _settings.value.ai
        val userMessage = ChatMessage(role = ChatRole.USER, text = text)
        val assistantMessage = ChatMessage(role = ChatRole.ASSISTANT, text = "", at = System.currentTimeMillis())
        val history = _messages.value.toList()

        _messages.update { it + userMessage + assistantMessage }
        repository.saveMessages(_messages.value)
        _isStreaming.value = true
        _streamingMessageId.value = assistantMessage.id

        streamJob = viewModelScope.launch {
            try {
                if (!config.isReady) {
                    delay(500)
                    val reply = LocalCompanion.reply(text, config.aiName)
                    updateAssistant(assistantMessage.id) { it.copy(text = reply, local = true) }
                } else {
                    val turns = buildTurns(config, history + userMessage)
                    aiClient.streamChat(config, turns).collect { delta ->
                        updateAssistant(assistantMessage.id) { current ->
                            current.copy(text = current.text + delta)
                        }
                    }
                    val finished = _messages.value.firstOrNull { it.id == assistantMessage.id }
                    if (finished == null || finished.text.isBlank()) {
                        throw AiException("模型没有返回内容。")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val friendly = (e as? AiException)?.userMessage ?: "网络好像走神了。"
                val textFallback = "刚才没能连上（$friendly）。不过我还在这里，你不需要一个人把这个瞬间撑过去。先喝口水，或者把手机放远一点，等你想试的时候，我们再连一次。"
                updateAssistant(assistantMessage.id) { it.copy(text = textFallback, error = true) }
            } finally {
                _isStreaming.value = false
                _streamingMessageId.value = null
                repository.saveMessages(_messages.value)
            }
        }
    }

    private fun buildTurns(config: AiConfig, history: List<ChatMessage>): List<ChatTurn> {
        val turns = mutableListOf<ChatTurn>()
        turns += ChatTurn("system", CompanionPrompt.build(config))
        val usable = history
            .filter { !it.error && it.text.isNotBlank() }
            .takeLast(24)
        var budget = 12_000
        val selected = ArrayDeque<ChatMessage>()
        for (message in usable.asReversed()) {
            val cost = message.text.length + 16
            if (budget - cost < 0) break
            budget -= cost
            selected.addFirst(message)
        }
        selected.forEach { message ->
            turns += ChatTurn(
                role = if (message.role == ChatRole.USER) "user" else "assistant",
                content = message.text.take(3_500),
            )
        }
        return turns
    }

    private fun updateAssistant(id: String, transform: (ChatMessage) -> ChatMessage) {
        _messages.update { list ->
            list.map { if (it.id == id) transform(it) else it }
        }
    }

    fun clearChat() {
        streamJob?.cancel()
        streamJob = null
        aiClient.cancel()
        _isStreaming.value = false
        _streamingMessageId.value = null
        val greeting = ChatMessage(
            role = ChatRole.ASSISTANT,
            text = LocalCompanion.greeting(_settings.value.ai.aiName),
            local = true,
        )
        _messages.value = listOf(greeting)
        repository.saveMessages(_messages.value)
    }

    fun clearAllData() {
        streamJob?.cancel()
        streamJob = null
        aiClient.cancel()
        _isStreaming.value = false
        _streamingMessageId.value = null
        testJob?.cancel()
        testJob = null
        _testState.value = AiTestState.Idle
        repository.clearAll()
        val now = System.currentTimeMillis()
        repository.saveJourneyStart(now)
        _journeyStart.value = now
        _events.value = emptyList()
        _dayNotes.value = emptyMap()
        _settings.value = AppSettings()
        repository.saveSettings(_settings.value)
        UpdateScheduler.setEnabled(getApplication(), false)
        val greeting = ChatMessage(
            role = ChatRole.ASSISTANT,
            text = LocalCompanion.greeting(AppSettings().ai.aiName),
            local = true,
        )
        _messages.value = listOf(greeting)
        repository.saveMessages(_messages.value)
        TickNotifications.cancelAll(getApplication())
    }

    override fun onCleared() {
        runCatching { getApplication<Application>().unregisterReceiver(dataChangeReceiver) }
        streamJob?.cancel()
        testJob?.cancel()
        aiClient.cancel()
        super.onCleared()
    }
}
