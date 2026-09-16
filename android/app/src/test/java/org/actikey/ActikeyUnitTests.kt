package org.actikey

import org.actikey.guard.InsertionGuard
import org.actikey.guard.LogScrubber
import org.actikey.net.ActikeyResult
import org.actikey.net.LmStudioClient
import org.junit.Assert.*
import org.junit.Test

/** Mock-protocol tests: timeouts, malformed responses, tool rejection.
 *  These NEVER prove inference works — see docs/model-acceptance.md. */
class MockProtocolTest {
  private val client = LmStudioClient()

  @Test fun malformedJsonIsCleanFail() {
    val r = client.parseResponse("not json{{{", emptyList())
    assertTrue(r is ActikeyResult.Fail)
  }

  @Test fun noChoicesIsCleanFail() {
    val r = client.parseResponse("{\"x\":1}", emptyList())
    assertTrue(r is ActikeyResult.Fail)
    assertEquals("Bad server response: no choices.", (r as ActikeyResult.Fail).message)
  }

  @Test fun unknownToolRejected() {
    val raw = """{"choices":[{"message":{"content":"hi",
      "tool_calls":[{"function":{"name":"shell_exec","arguments":"{}"}}]}}]}"""
    val r = client.parseResponse(raw, listOf("web_search"))
    assertTrue(r is ActikeyResult.Fail)
    assertTrue((r as ActikeyResult.Fail).message.contains("unknown tool"))
  }

  @Test fun allowedToolAccepted() {
    val raw = """{"choices":[{"message":{"content":"hi",
      "tool_calls":[{"function":{"name":"web_search","arguments":"{\"q\":\"x\"}"}}]}}]}"""
    val r = client.parseResponse(raw, listOf("web_search"))
    assertTrue(r is ActikeyResult.Ok)
    assertEquals("web_search", (r as ActikeyResult.Ok).toolCalls.single().name)
  }
}

class InsertionSafetyTest {
  private fun ctx() = InsertionGuard.RunContext("com.a", 1, 2, 5, 99, 1, "rewrite", true)

  @Test fun appSwitchBlocksAutoInsert() {
    assertFalse(InsertionGuard.mayAutoInsert(ctx(), "com.b", 1, 2, 5, 99, true))
  }

  @Test fun selectionChangeBlocksReplace() {
    assertFalse(InsertionGuard.mayAutoInsert(ctx(), "com.a", 1, 2, 6, 99, true))
  }

  @Test fun partialNeverAutoInserts() {
    assertFalse(InsertionGuard.mayAutoInsert(ctx(), "com.a", 1, 2, 5, 99, false))
  }

  @Test fun stableContextAllowsExplicitOp() {
    assertTrue(InsertionGuard.mayAutoInsert(ctx(), "com.a", 1, 2, 5, 99, true))
  }

  @Test fun opWithoutAutoReplaceNeverAutoInserts() {
    val c = ctx().copy(autoReplaceAllowed = false)
    assertFalse(InsertionGuard.mayAutoInsert(c, "com.a", 1, 2, 5, 99, true))
  }
}

class NoSecretsInLogsTest {
  @Test fun bearerRedacted() {
    assertFalse(LogScrubber.scrub("Authorization: Bearer sk-secret-123").contains("sk-secret-123"))
  }

  @Test fun apiKeyRedacted() {
    assertFalse(LogScrubber.scrub("api_key=super-secret").contains("super-secret"))
  }
}
