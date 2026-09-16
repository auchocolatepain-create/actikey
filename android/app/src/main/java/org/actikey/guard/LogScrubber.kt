package org.actikey.guard

/** Redacts secrets/private text before any log line. Tested by NoSecretsInLogsTest. */
object LogScrubber {
  private val bearer = Regex("(?i)bearer\\s+[A-Za-z0-9._\\-]+")
  private val keyLine = Regex("(?i)(api[_-]?key|password|passwd|pin)\\s*[:=]\\s*\\S+")
  fun scrub(s: String): String =
    keyLine.replace(bearer.replace(s, "Bearer [REDACTED]")) { m ->
      m.value.replaceAfter(":", " [REDACTED]").replaceAfter("=", " [REDACTED]")
    }
  fun safePreview(s: String, max: Int = 40): String {
    val t = scrub(s).replace("\n", " ")
    return if (t.length <= max) t else t.take(max) + "…"
  }
}
