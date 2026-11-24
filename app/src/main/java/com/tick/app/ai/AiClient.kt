package com.tick.app.ai

import com.tick.app.data.AiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicReference

data class ChatTurn(
    val role: String,
    val content: String,
)

class AiException(
    val userMessage: String,
    val detail: String? = null,
) : IOException(userMessage)

class AiClient {
    private val activeConnection = AtomicReference<HttpURLConnection?>(null)

    fun cancel() {
        activeConnection.getAndSet(null)?.let { runCatching { it.disconnect() } }
    }

    suspend fun testConnection(config: AiConfig): String = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("model", config.model.trim())
            put("stream", false)
            put("temperature", 0.2)
            put("max_tokens", 16)
            put(
                "messages",
                JSONArray().apply {
                    put(JSONObject().apply { put("role", "user"); put("content", "你好，这是一次连接测试。") })
                },
            )
        }
        val response = execute(config, body)
        val text = parseFullResponse(response)
        if (text.isBlank()) "连接成功，模型已响应。" else "连接成功：${text.trim().take(48)}"
    }

    fun streamChat(config: AiConfig, turns: List<ChatTurn>): Flow<String> = flow {
        val body = JSONObject().apply {
            put("model", config.model.trim())
            put("stream", true)
            put("temperature", 0.85)
            put("top_p", 0.95)
            put("max_tokens", 600)
            put(
                "messages",
                JSONArray().apply {
                    turns.forEach { turn ->
                        put(JSONObject().apply {
                            put("role", turn.role)
                            put("content", turn.content)
                        })
                    }
                },
            )
        }

        val connection = openConnection(config)
        activeConnection.set(connection)
        try {
            writeBody(connection, body)
            val code = connection.responseCode
            if (code !in 200..299) {
                throw httpError(code, readErrorBody(connection))
            }

            val contentType = connection.contentType.orEmpty()
            if (!contentType.contains("event-stream", ignoreCase = true)) {
                val raw = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val text = parseFullResponse(raw)
                if (text.isBlank()) throw AiException("服务返回了空内容，请检查模型名或接口地址。")
                emit(text)
                return@flow
            }

            val reader = connection.inputStream.bufferedReader(Charsets.UTF_8)
            var done = false
            while (!done) {
                currentCoroutineContext().ensureActive()
                val line = reader.readLine() ?: break
                if (!line.startsWith("data:")) continue
                val payload = line.removePrefix("data:").trim()
                if (payload.isEmpty()) continue
                if (payload == "[DONE]") {
                    done = true
                    continue
                }
                val delta = parseDelta(payload)
                if (delta.isNotEmpty()) emit(delta)
            }
        } catch (e: AiException) {
            throw e
        } catch (e: IOException) {
            if (e.message?.contains("closed", ignoreCase = true) == true) return@flow
            throw AiException("网络连接中断了，检查一下网络或接口地址。", e.message)
        } finally {
            activeConnection.compareAndSet(connection, null)
            runCatching { connection.disconnect() }
        }
    }.flowOn(Dispatchers.IO)

    private fun openConnection(config: AiConfig): HttpURLConnection {
        val endpoint = endpoint(config.baseUrl)
        val connection = (URL(endpoint).openConnection() as? HttpURLConnection)
            ?: throw AiException("接口地址格式不正确。")
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 20_000
        connection.readTimeout = 90_000
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.setRequestProperty("Accept", "text/event-stream, application/json")
        connection.setRequestProperty("Authorization", "Bearer ${config.apiKey.trim()}")
        connection.setRequestProperty("User-Agent", "Tick/1.0 (Android)")
        return connection
    }

    private fun execute(config: AiConfig, body: JSONObject): String {
        val connection = openConnection(config)
        activeConnection.set(connection)
        try {
            writeBody(connection, body)
            val code = connection.responseCode
            if (code !in 200..299) throw httpError(code, readErrorBody(connection))
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: AiException) {
            throw e
        } catch (e: IOException) {
            throw AiException("网络连接失败，检查一下网络或接口地址。", e.message)
        } finally {
            activeConnection.compareAndSet(connection, null)
            runCatching { connection.disconnect() }
        }
    }

    private fun writeBody(connection: HttpURLConnection, body: JSONObject) {
        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
    }

    private fun parseDelta(payload: String): String {
        val root = runCatching { JSONObject(payload) }.getOrNull() ?: return ""
        val choices = root.optJSONArray("choices") ?: return ""
        if (choices.length() == 0) return ""
        val first = choices.optJSONObject(0) ?: return ""
        val delta = first.optJSONObject("delta") ?: return ""
        val content = delta.opt("content")
        return when (content) {
            is String -> content
            is JSONArray -> buildString {
                for (i in 0 until content.length()) {
                    val part = content.optJSONObject(i) ?: continue
                    val text = part.optString("text")
                    if (text.isNotEmpty()) append(text)
                }
            }
            else -> ""
        }
    }

    private fun parseFullResponse(raw: String): String {
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return ""
        root.optJSONArray("choices")?.optJSONObject(0)?.let { choice ->
            choice.optJSONObject("message")?.optString("content")?.let { if (it.isNotBlank()) return it }
            choice.optJSONObject("delta")?.optString("content")?.let { if (it.isNotBlank()) return it }
            choice.optString("text").let { if (it.isNotBlank()) return it }
        }
        root.optString("output_text").let { if (it.isNotBlank()) return it }
        root.optJSONObject("error")?.optString("message")?.let { if (it.isNotBlank()) throw AiException(it.take(180)) }
        return ""
    }

    private fun readErrorBody(connection: HttpURLConnection): String {
        val stream = connection.errorStream ?: return ""
        return runCatching {
            stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrDefault("")
    }

    private fun httpError(code: Int, raw: String): AiException {
        val serverMessage = runCatching {
            JSONObject(raw).optJSONObject("error")?.optString("message")
                ?: JSONObject(raw).optString("message")
        }.getOrNull()?.takeIf { it.isNotBlank() }
        val friendly = when (code) {
            400 -> "请求参数不被服务接受，请检查模型名。"
            401, 403 -> "API Key 无效或没有权限。"
            404 -> "接口地址不存在，请检查 Base URL。"
            408 -> "请求超时了，稍后再试。"
            413 -> "发送的内容太长了。"
            429 -> "请求太频繁或额度不足，休息一会儿再试。"
            in 500..599 -> "服务暂时不可用，稍后再试。"
            else -> "请求失败（HTTP $code）。"
        }
        return AiException(friendly, serverMessage ?: raw.take(240))
    }

    companion object {
        fun endpoint(baseUrl: String): String {
            var base = baseUrl.trim()
            if (base.isBlank()) throw AiException("还没有填写接口地址。")
            if (!base.startsWith("http://", true) && !base.startsWith("https://", true)) {
                base = "https://$base"
            }
            base = base.trimEnd('/')
            return if (base.endsWith("/chat/completions", ignoreCase = true)) {
                base
            } else {
                "$base/chat/completions"
            }
        }

        fun validate(config: AiConfig): String? {
            if (config.apiKey.isBlank()) return "还没有填写 API Key。"
            if (config.baseUrl.isBlank()) return "还没有填写 Base URL。"
            if (config.model.isBlank()) return "还没有填写模型名称。"
            return runCatching {
                val url = URL(endpoint(config.baseUrl))
                if (url.protocol != "http" && url.protocol != "https") "Base URL 只支持 http/https。" else null
            }.getOrElse { "Base URL 格式不正确。" }
        }
    }
}
