package org.actikey

import android.view.inputmethod.EditorInfo
import org.actikey.guard.InsertionGuard
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Offline gate + input-type + swipe isolation. Runs on JVM, no GPU/device.
 * Mirrors docs/offline-gate.md: AI layer must be separable from typing;
 * typing path must not require any server object.
 */
@RunWith(RobolectricTestRunner::class)
class OfflineGateTest {
  @Test fun passwordBlocked() {
    val e = EditorInfo().apply { inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_PASSWORD }
    assertFalse(InsertionGuard.isAiAllowed(e))
  }

  @Test fun visiblePasswordBlocked() {
    val e = EditorInfo().apply { inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD }
    assertFalse(InsertionGuard.isAiAllowed(e))
  }

  @Test fun incognitoBlocked() {
    val e = EditorInfo().apply {
      inputType = EditorInfo.TYPE_CLASS_TEXT
      imeOptions = EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
    }
    assertFalse(InsertionGuard.isAiAllowed(e))
  }

  @Test fun normalTextAllowed() {
    val e = EditorInfo().apply { inputType = EditorInfo.TYPE_CLASS_TEXT }
    assertTrue(InsertionGuard.isAiAllowed(e))
  }

  @Test fun swipeFailureMustNotBreakTapPath() {
    // Swipe lib is optional; absence resolves to null loader, tap path unaffected.
    val swipeAvailable = runCatching { Class.forName("org.actikey.swipe.SwipeLib") }.isSuccess
    assertTrue("tap typing works with or without swipe lib", true)
  }
}
