package org.actikey.secure

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** API-key storage: AndroidKeystore-backed EncryptedSharedPreferences. Never plaintext. */
object KeyStore {
  private const val FILE = "actikey_secure"
  private const val KEY_API = "lmstudio_api_key"

  private fun prefs(ctx: Context) = EncryptedSharedPreferences.create(
    ctx, FILE,
    MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
  )

  fun getApiKey(ctx: Context): String = prefs(ctx).getString(KEY_API, "") ?: ""
  fun setApiKey(ctx: Context, v: String) { prefs(ctx).edit().putString(KEY_API, v).apply() }
  fun clearApiKey(ctx: Context) { prefs(ctx).edit().remove(KEY_API).apply() }
  fun hasApiKey(ctx: Context): Boolean = getApiKey(ctx).isNotEmpty()
}
