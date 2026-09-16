package org.actikey.guard

import android.view.inputmethod.EditorInfo

/**
 * Insertion safety: binds every AI run to its origin. Auto-insert/replace is
 * allowed ONLY if app, editor, selection and inputType are unchanged AND the
 * op explicitly permitted auto-replace AND output is complete (not Partial).
 */
object InsertionGuard {
  data class RunContext(
    val packageName: String,
    val editorIdHash: Int,
    val selectionStart: Int,
    val selectionEnd: Int,
    val sourceHash: Int,
    val inputType: Int,
    val op: String,
    val autoReplaceAllowed: Boolean,
  )

  fun isAiAllowed(info: EditorInfo?): Boolean {
    if (info == null) return false
    val cls = info.inputType and EditorInfo.TYPE_MASK_CLASS
    val variation = info.inputType and EditorInfo.TYPE_MASK_VARIATION
    if (variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD ||
      variation == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
      variation == EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
      variation == EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD
    ) return false
    if ((info.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0) return false
    if (cls == EditorInfo.TYPE_CLASS_PHONE) return false
    return true
  }

  fun mayAutoInsert(ctx: RunContext, nowPackage: String, nowEditorHash: Int,
                    nowSelStart: Int, nowSelEnd: Int, nowSourceHash: Int,
                    outputComplete: Boolean): Boolean {
    if (!outputComplete) return false
    if (!ctx.autoReplaceAllowed) return false
    return ctx.packageName == nowPackage &&
      ctx.editorIdHash == nowEditorHash &&
      ctx.selectionStart == nowSelStart &&
      ctx.selectionEnd == nowSelEnd &&
      ctx.sourceHash == nowSourceHash
  }
}
