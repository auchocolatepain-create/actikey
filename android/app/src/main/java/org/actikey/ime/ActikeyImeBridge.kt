package org.actikey.ime

/**
 * Bridge between HeliBoard's IME and Actikey additions.
 * RULE: never add work to the key-input hot path. Typing flows through
 * HeliBoard unchanged; this class only exposes editor snapshots + explicit
 * AI entry points used by ActikeyActionBar.
 */
object ActikeyImeBridge {
  /**
   * Minimum context for an explicitly invoked op. Called ONLY from the AI
   * action handler (never from onKey/onUpdateSelection typing callbacks).
   */
  fun snapshotForAi(
    packageName: String, editorHash: Int, selStart: Int, selEnd: Int,
    sourceText: CharSequence?, inputType: Int, op: String, autoReplace: Boolean,
  ): org.actikey.guard.InsertionGuard.RunContext =
    org.actikey.guard.InsertionGuard.RunContext(
      packageName, editorHash, selStart, selEnd,
      sourceText?.toString()?.hashCode() ?: 0, inputType, op, autoReplace,
    )
}
