package org.actikey.settings

import android.content.Context
import androidx.preference.PreferenceManager

/** All local config: server, timeouts, appearance, haptics, gestures. */
object ActikeySettings {
  // Server
  const val HOST = "actikey_host"; const val PORT = "actikey_port"
  const val MODEL = "actikey_model"; const val WEB_TOOLS = "actikey_web_tools"
  const val EXTENDED = "actikey_extended_default"
  // Appearance / tactile (params + tiny resources, no theme blobs)
  const val KB_HEIGHT = "actikey_kb_height" // compact|normal|tall + slider offset
  const val KEY_BG = "actikey_key_bg"; const val KB_BG = "actikey_kb_bg"
  const val KEY_TEXT = "actikey_key_text"; const val ACCENT = "actikey_accent"
  const val PRESSED = "actikey_pressed"; const val BAR_COLORS = "actikey_bar_colors"
  const val THEME_MODE = "actikey_theme" // light|dark|system
  const val BORDERS = "actikey_borders"; const val CORNER_R = "actikey_corner_r"
  const val POPUP = "actikey_popup"; const val NUMBER_ROW = "actikey_number_row"
  const val LONGPRESS_SYMS = "actikey_longpress_syms"
  const val EMOJI_KEY = "actikey_emoji_key"; const val LANG_KEY = "actikey_lang_key"
  const val CLIP_KEY = "actikey_clip_key"; const val BAR_VISIBLE = "actikey_bar_visible"
  const val HAPTIC = "actikey_haptic"; const val HAPTIC_LEVEL = "actikey_haptic_level"
  const val SOUND = "actikey_sound"; const val SOUND_VOL = "actikey_sound_vol"
  const val LONGPRESS_DELAY = "actikey_longpress_delay"; const val KEY_REPEAT = "actikey_key_repeat"
  const val SWIPE = "actikey_swipe"; const val SPACE_CURSOR = "actikey_space_cursor"
  const val FONT_SCALE = "actikey_font_scale"

  fun prefs(ctx: Context) = PreferenceManager.getDefaultSharedPreferences(ctx)
  fun baseUrl(ctx: Context): String {
    val p = prefs(ctx)
    return "http://${p.getString(HOST, "192.168.1.10")}:${p.getInt(PORT, 1234)}"
  }
}
