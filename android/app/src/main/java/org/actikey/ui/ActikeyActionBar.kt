package org.actikey.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import org.actikey.guard.InsertionGuard
import org.actikey.net.ActikeyConfig
import org.actikey.net.ActikeyResult
import org.actikey.net.ActikeyRunner
import org.actikey.net.FailKind

/**
 * Action Bar / AI strip. Must ack a tap within 100ms even if the laptop is
 * down (ack synchronously, run async). Never blocks typing.
 */
class ActikeyActionBar @JvmOverloads constructor(
  ctx: Context, attrs: AttributeSet? = null,
) : LinearLayout(ctx, attrs) {
  private val status = TextView(ctx)
  private val runner = ActikeyRunner()
  init { orientation = HORIZONTAL; addView(status) }

  fun onAiButton(
    config: ActikeyConfig?,
    editor: android.view.inputmethod.EditorInfo?,
    runCtx: InsertionGuard.RunContext,
    op: String, systemPrompt: String, userContent: String,
    allowedTools: List<String>, extended: Boolean,
    deliver: (ActikeyResult, InsertionGuard.RunContext) -> Unit,
  ) {
    // ≤100ms ack, synchronous:
    status.text = "Working… (cancel: ✕)"
    if (config == null || config.apiKey.isEmpty()) {
      status.text = "Laptop unavailable — configure server in Actikey settings."
      deliver(ActikeyResult.Fail(FailKind.UNAVAILABLE, "Laptop unavailable."), runCtx)
      return
    }
    if (!InsertionGuard.isAiAllowed(editor)) {
      status.text = "Not available in this field."
      deliver(ActikeyResult.Fail(FailKind.BLOCKED_SENSITIVE, "Sensitive field."), runCtx)
      return
    }
    runner.submit(config, runCtx, systemPrompt, userContent, allowedTools, extended) { res, c ->
      post {
        status.text = when (res) {
          is ActikeyResult.Ok -> "Done — tap Insert / Copy / Replace."
          is ActikeyResult.Partial -> "Incomplete — kept for review, not inserted."
          is ActikeyResult.Fail -> res.message
        }
        deliver(res, c)
      }
    }
  }

  fun onCancel() { runner.cancel(); status.text = "Cancelled." }
}
