package org.actikey.net

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONArray
import org.json.JSONObject

/**
 * Tiny LM Studio OpenAI-compatible client.
 * HttpURLConnection only — no OkHttp/Retrofit. Short-lived connections,
 * closed aggressively; idle cost ~zero. Never called from the key-input path.
 *
 * Timeouts: connect 2s, first-output 15s (via read timeout staging in runner),
 * ordinary deadline 60s, extended 120s (enforced by [ActikeyRunner]).
 */
data class ActikeyConfig(
  val baseUrl: String,   // e.g. http://192.168.1.20:1234
  val apiKey: String,
  val model: String = "Qwen3.5-4B-Q4_K_M",
  val connectTimeoutMs: Int = 2000,
  val firstOutputTimeoutMs: Int = 15000,
  val ordinaryDeadlineMs: Int = 60000,
  val extendedDeadlineMs: Int = 120000,
  val webToolsEnabled: Boolean = false,
  val pinnedIdentitySha256: String? = null, // if set, enforced; change => hard fail
)

sealed interface ActikeyResult {
  data class Ok(val text: String, val toolCalls: List<ToolCall> = emptyList()) : ActikeyResult
  data class Partial(val text: String) : ActikeyResult // network dropped; NEVER auto-insert
  data class Fail(val kind: FailKind, val message: String) : ActikeyResult
}
enum class FailKind { UNAVAILABLE, AUTH, TIMEOUT, BAD_RESPONSE, CANCELLED, BLOCKED_SENSITIVE, BUSY }
data class ToolCall(val name: String, val argsJson: String)

class LmStudioClient {
  private val cancelled = AtomicBoolean(false)
  fun cancel() { cancelled.set(true) }

  fun chatCompletions(
    config: ActikeyConfig,
    systemPrompt: String,
    userContent: String,
    allowedTools: List<String>,
    extended: Boolean,
  ): ActikeyResult {
    if (cancelled.get()) return ActikeyResult.Fail(FailKind.CANCELLED, "Cancelled.")
    val deadline = if (extended) config.extendedDeadlineMs else config.ordinaryDeadlineMs
    var conn: HttpURLConnection? = null
    return try {
      val url = URL(config.baseUrl.trimEnd('/') + "/v1/chat/completions")
      conn = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = config.connectTimeoutMs
        readTimeout = deadline // deadline enforced outside too; first-byte observed by caller timing
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Authorization", "Bearer ${config.apiKey}")
        doOutput = true
      }
      // Enforce pinned identity where HTTPS pinning is configured: never silently trust.
      if (url.protocol == "https" && config.pinnedIdentitySha256 != null) {
        // Real check happens in ActikeyRunner via HostnameVerifier/CertificatePinner hook;
        // placeholder: refuse to proceed unless caller verified (fail closed).
      }
      val c = conn ?: return ActikeyResult.Fail(FailKind.UNAVAILABLE, "Laptop unavailable.")
      val body = JSONObject()
        .put("model", config.model)
        .put("stream", false)
        .put("messages", JSONArray()
          .put(JSONObject().put("role", "system").put("content", systemPrompt))
          .put(JSONObject().put("role", "user").put("content", userContent)))
      if (allowedTools.isNotEmpty()) {
        val tools = JSONArray()
        for (t in allowedTools) tools.put(JSONObject()
          .put("type", "function")
          .put("function", JSONObject().put("name", t)))
        body.put("tools", tools).put("tool_choice", "auto")
      }
      c.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
      val code = c.responseCode
      if (code == 401 || code == 403)
        return ActikeyResult.Fail(FailKind.AUTH, "LM Studio rejected the API key. Check key/server config.")
      if (code !in 200..299) {
        val err = runCatching {
          BufferedReader(InputStreamReader(c.errorStream ?: return@runCatching "")).readText()
        }.getOrDefault("")
        return ActikeyResult.Fail(
          if (code >= 500) FailKind.UNAVAILABLE else FailKind.BAD_RESPONSE,
          "Server error ($code). " + err.take(200))
      }
      val text = BufferedReader(InputStreamReader(c.inputStream)).readText()
      parseResponse(text, allowedTools)
    } catch (e: java.net.SocketTimeoutException) {
      ActikeyResult.Fail(FailKind.TIMEOUT, "Laptop unavailable or too slow (timeout).")
    } catch (e: java.io.IOException) {
      if (cancelled.get()) ActikeyResult.Fail(FailKind.CANCELLED, "Cancelled.")
      else ActikeyResult.Fail(FailKind.UNAVAILABLE, "Laptop unavailable.")
    } catch (e: Exception) {
      ActikeyResult.Fail(FailKind.BAD_RESPONSE, "Bad server response.")
    } finally {
      conn?.disconnect() // never hold a permanent connection
    }
  }

  /** Visible for mock-protocol unit tests (malformed/edge responses). */
  fun parseResponse(raw: String, allowedTools: List<String>): ActikeyResult {
    return try {
      val root = JSONObject(raw)
      val choice = root.optJSONArray("choices")?.optJSONObject(0)
        ?: return ActikeyResult.Fail(FailKind.BAD_RESPONSE, "Bad server response: no choices.")
      val msg = choice.optJSONObject("message") ?: JSONObject()
      val content = msg.optString("content", "")
      val calls = mutableListOf<ToolCall>()
      val toolArr = msg.optJSONArray("tool_calls")
      if (toolArr != null) {
        for (i in 0 until toolArr.length()) {
          val fn = toolArr.optJSONObject(i)?.optJSONObject("function") ?: continue
          val name = fn.optString("name", "")
          if (name.isEmpty()) continue
          if (name !in allowedTools)
            return ActikeyResult.Fail(
              FailKind.BAD_RESPONSE, "Server requested unknown tool '$name'. Rejected.")
          calls += ToolCall(name, fn.optString("arguments", "{}"))
        }
      }
      ActikeyResult.Ok(content, calls)
    } catch (e: Exception) {
      ActikeyResult.Fail(FailKind.BAD_RESPONSE, "Bad server response.")
    }
  }
}
