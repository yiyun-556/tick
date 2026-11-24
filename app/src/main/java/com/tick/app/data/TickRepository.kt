package com.tick.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class TickRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val secureKeyStore = SecureKeyStore()

    fun loadEvents(): List<TickEvent> {
        val raw = prefs.getString(KEY_EVENTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val type = runCatching {
                        EventType.valueOf(obj.optString("type", EventType.CHECK_IN.name))
                    }.getOrDefault(EventType.CHECK_IN)
                    val at = obj.optLong("at", 0L)
                    if (at <= 0L) continue
                    add(
                        TickEvent(
                            id = obj.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                            type = type,
                            at = at,
                            trigger = obj.optString("trigger").takeIf { it.isNotBlank() },
                            note = obj.optString("note").takeIf { it.isNotBlank() },
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveEvents(events: List<TickEvent>) {
        val array = JSONArray()
        events.forEach { event ->
            array.put(
                JSONObject().apply {
                    put("id", event.id)
                    put("type", event.type.name)
                    put("at", event.at)
                    event.trigger?.let { put("trigger", it) }
                    event.note?.let { put("note", it) }
                },
            )
        }
        prefs.edit { putString(KEY_EVENTS, array.toString()) }
    }

    fun loadJourneyStart(): Long {
        val stored = prefs.getLong(KEY_JOURNEY_START, 0L)
        if (stored > 0L) return stored
        val now = System.currentTimeMillis()
        prefs.edit { putLong(KEY_JOURNEY_START, now) }
        return now
    }

    fun saveJourneyStart(at: Long) {
        prefs.edit { putLong(KEY_JOURNEY_START, at) }
    }

    fun loadDayNotes(): Map<Long, String> {
        val raw = prefs.getString(KEY_DAY_NOTES, null) ?: return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            buildMap {
                obj.keys().forEach { key ->
                    val epochDay = key.toLongOrNull() ?: return@forEach
                    val text = obj.optString(key).trim()
                    if (text.isNotEmpty()) put(epochDay, text)
                }
            }
        }.getOrDefault(emptyMap())
    }

    fun saveDayNotes(notes: Map<Long, String>) {
        val obj = JSONObject()
        notes.forEach { (epochDay, text) ->
            if (text.isNotBlank()) obj.put(epochDay.toString(), text)
        }
        prefs.edit { putString(KEY_DAY_NOTES, obj.toString()) }
    }

    fun loadSettings(): AppSettings {
        val palette = runCatching {
            PaletteStyle.valueOf(prefs.getString(KEY_PALETTE, PaletteStyle.CLASSIC.name)!!)
        }.getOrDefault(PaletteStyle.CLASSIC)
        val dark = runCatching {
            DarkMode.valueOf(prefs.getString(KEY_DARK_MODE, DarkMode.SYSTEM.name)!!)
        }.getOrDefault(DarkMode.SYSTEM)
        val default = AiConfig()
        val notifications = NotificationSettings(
            liveUpdate = prefs.getBoolean(KEY_NOTIFY_LIVE, true),
            dailyReminder = prefs.getBoolean(KEY_NOTIFY_REMINDER, true),
            reminderHour = prefs.getInt(KEY_REMINDER_HOUR, 21).coerceIn(0, 23),
            reminderMinute = prefs.getInt(KEY_REMINDER_MINUTE, 0).coerceIn(0, 59),
            checkInConfirmation = prefs.getBoolean(KEY_NOTIFY_CONFIRM, true),
        )
        val ai = AiConfig(
            presetId = prefs.getString(KEY_AI_PRESET, default.presetId) ?: default.presetId,
            baseUrl = prefs.getString(KEY_AI_BASE_URL, default.baseUrl) ?: default.baseUrl,
            model = prefs.getString(KEY_AI_MODEL, default.model) ?: default.model,
            apiKey = secureKeyStore.decrypt(prefs.getString(KEY_AI_KEY, "").orEmpty()),
            aiName = prefs.getString(KEY_AI_NAME, default.aiName) ?: default.aiName,
            persona = prefs.getString(KEY_AI_PERSONA, default.persona) ?: default.persona,
        )
        val dockStyle = if (prefs.contains(KEY_DOCK_STYLE)) {
            runCatching {
                DockStyle.valueOf(prefs.getString(KEY_DOCK_STYLE, DockStyle.GLASS.name)!!)
            }.getOrDefault(DockStyle.GLASS)
        } else if (prefs.getBoolean(KEY_LIQUID_GLASS, true)) {
            DockStyle.GLASS
        } else {
            DockStyle.SOLID
        }
        val dock = DockSettings(
            style = dockStyle,
            blurRadius = prefs.getFloat(KEY_DOCK_BLUR, 8f).coerceIn(0f, 24f),
            refractionHeight = prefs.getFloat(KEY_DOCK_REFRACTION_HEIGHT, 24f).coerceIn(0f, 32f),
            refractionAmount = prefs.getFloat(KEY_DOCK_REFRACTION_AMOUNT, 24f).coerceIn(0f, 32f),
            chromaticAberration = prefs.getBoolean(KEY_DOCK_CHROMATIC, true),
            highlight = prefs.getBoolean(KEY_DOCK_HIGHLIGHT, true),
        )
        return AppSettings(
            palette = palette,
            darkMode = dark,
            ai = ai,
            notifications = notifications,
            dock = dock,
            autoUpdateCheck = prefs.getBoolean(KEY_AUTO_UPDATE_CHECK, false),
        )
    }

    fun saveSettings(settings: AppSettings) {
        val encryptedKey = if (settings.ai.apiKey.isBlank()) {
            ""
        } else {
            secureKeyStore.encrypt(settings.ai.apiKey)
        }
        prefs.edit {
            putString(KEY_PALETTE, settings.palette.name)
            putString(KEY_DARK_MODE, settings.darkMode.name)
            putString(KEY_AI_PRESET, settings.ai.presetId)
            putString(KEY_AI_BASE_URL, settings.ai.baseUrl)
            putString(KEY_AI_MODEL, settings.ai.model)
            putString(KEY_AI_NAME, settings.ai.aiName)
            putString(KEY_AI_PERSONA, settings.ai.persona)
            putString(KEY_AI_KEY, encryptedKey)
            putBoolean(KEY_NOTIFY_LIVE, settings.notifications.liveUpdate)
            putBoolean(KEY_NOTIFY_REMINDER, settings.notifications.dailyReminder)
            putInt(KEY_REMINDER_HOUR, settings.notifications.reminderHour)
            putInt(KEY_REMINDER_MINUTE, settings.notifications.reminderMinute)
            putBoolean(KEY_NOTIFY_CONFIRM, settings.notifications.checkInConfirmation)
            putString(KEY_DOCK_STYLE, settings.dock.style.name)
            putFloat(KEY_DOCK_BLUR, settings.dock.blurRadius)
            putFloat(KEY_DOCK_REFRACTION_HEIGHT, settings.dock.refractionHeight)
            putFloat(KEY_DOCK_REFRACTION_AMOUNT, settings.dock.refractionAmount)
            putBoolean(KEY_DOCK_CHROMATIC, settings.dock.chromaticAberration)
            putBoolean(KEY_DOCK_HIGHLIGHT, settings.dock.highlight)
            putBoolean(KEY_LIQUID_GLASS, settings.dock.style != DockStyle.SOLID)
            putBoolean(KEY_AUTO_UPDATE_CHECK, settings.autoUpdateCheck)
        }
    }

    fun loadMessages(): List<ChatMessage> {
        val raw = prefs.getString(KEY_CHAT, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val role = runCatching {
                        ChatRole.valueOf(obj.optString("role", ChatRole.ASSISTANT.name))
                    }.getOrDefault(ChatRole.ASSISTANT)
                    val text = obj.optString("text")
                    if (text.isBlank()) continue
                    add(
                        ChatMessage(
                            id = obj.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                            role = role,
                            text = text,
                            at = obj.optLong("at", System.currentTimeMillis()),
                            local = obj.optBoolean("local", false),
                            error = obj.optBoolean("error", false),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveMessages(messages: List<ChatMessage>) {
        val trimmed = messages.takeLast(MAX_CHAT_MESSAGES)
        val array = JSONArray()
        trimmed.forEach { message ->
            array.put(
                JSONObject().apply {
                    put("id", message.id)
                    put("role", message.role.name)
                    put("text", message.text)
                    put("at", message.at)
                    put("local", message.local)
                    put("error", message.error)
                },
            )
        }
        prefs.edit { putString(KEY_CHAT, array.toString()) }
    }

    fun wasNotificationPermissionAsked(): Boolean = prefs.getBoolean(KEY_NOTIFY_ASKED, false)

    fun markNotificationPermissionAsked() {
        prefs.edit { putBoolean(KEY_NOTIFY_ASKED, true) }
    }

    fun clearAll() {
        prefs.edit { clear() }
        secureKeyStore.deleteKey()
    }

    private class SecureKeyStore {
        private val alias = "tick_api_key_v1"
        private val transformation = "AES/GCM/NoPadding"

        fun encrypt(plain: String): String {
            if (plain.isBlank()) return ""
            return runCatching {
                val cipher = Cipher.getInstance(transformation)
                cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
                val iv = cipher.iv
                val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
                val payload = ByteArray(iv.size + encrypted.size)
                System.arraycopy(iv, 0, payload, 0, iv.size)
                System.arraycopy(encrypted, 0, payload, iv.size, encrypted.size)
                Base64.encodeToString(payload, Base64.NO_WRAP)
            }.getOrDefault("")
        }

        fun decrypt(payload: String): String {
            if (payload.isBlank()) return ""
            return runCatching {
                val bytes = Base64.decode(payload, Base64.NO_WRAP)
                if (bytes.size <= 12) return ""
                val iv = bytes.copyOfRange(0, 12)
                val body = bytes.copyOfRange(12, bytes.size)
                val cipher = Cipher.getInstance(transformation)
                cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
                String(cipher.doFinal(body), Charsets.UTF_8)
            }.getOrDefault("")
        }

        fun deleteKey() {
            runCatching {
                val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias)
            }
        }

        private fun getOrCreateKey(): SecretKey {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val existing = (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.secretKey
            if (existing != null) return existing
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            generator.init(
                KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            return generator.generateKey()
        }
    }

    companion object {
        private const val PREFS_NAME = "tick_local_store"
        private const val KEY_EVENTS = "events"
        private const val KEY_JOURNEY_START = "journey_start"
        private const val KEY_PALETTE = "palette"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_AI_PRESET = "ai_preset"
        private const val KEY_AI_BASE_URL = "ai_base_url"
        private const val KEY_AI_MODEL = "ai_model"
        private const val KEY_AI_KEY = "ai_key_encrypted"
        private const val KEY_AI_NAME = "ai_name"
        private const val KEY_AI_PERSONA = "ai_persona"
        private const val KEY_CHAT = "chat_messages"
        private const val KEY_DAY_NOTES = "day_notes"
        private const val KEY_NOTIFY_LIVE = "notify_live"
        private const val KEY_NOTIFY_REMINDER = "notify_reminder"
        private const val KEY_REMINDER_HOUR = "reminder_hour"
        private const val KEY_REMINDER_MINUTE = "reminder_minute"
        private const val KEY_NOTIFY_CONFIRM = "notify_checkin_confirm"
        private const val KEY_NOTIFY_ASKED = "notify_permission_asked"
        private const val KEY_LIQUID_GLASS = "liquid_glass"
        private const val KEY_DOCK_STYLE = "dock_style"
        private const val KEY_DOCK_BLUR = "dock_blur"
        private const val KEY_DOCK_REFRACTION_HEIGHT = "dock_refraction_height"
        private const val KEY_DOCK_REFRACTION_AMOUNT = "dock_refraction_amount"
        private const val KEY_DOCK_CHROMATIC = "dock_chromatic"
        private const val KEY_DOCK_HIGHLIGHT = "dock_highlight"
        private const val KEY_AUTO_UPDATE_CHECK = "auto_update_check"
        private const val MAX_CHAT_MESSAGES = 120
    }
}
