# Phone module — Actikey keyboard APK (HeliBoard overlay)

Single-app rule: this is the **only** Android app. No bridge APK.

## Structure

```
android/
  HELIBOARD_PIN.md
  settings.gradle / build.gradle / gradle.properties
  app/build.gradle            # resConfigs en+fa, actikey flavor, min SDK per HeliBoard
  app/src/main/AndroidManifest.xml  # adds INTERNET (typing never uses it)
  app/src/main/java/org/actikey/
    net/LmStudioClient.kt     # tiny HttpURLConnection client, timeouts, cancel
    net/ActikeyRunner.kt      # 1-running+1-waiting client queue, async, deadlines
    secure/KeyStore.kt        # EncryptedSharedPreferences API-key storage
    guard/InsertionGuard.kt   # app/editor/selection/inputType binding + safety
    guard/LogScrubber.kt      # log redaction
    ui/ActikeyActionBar.kt    # ≤100ms ack, async states, manual retry
    settings/ActikeySettings.kt  # host/port/key/model/timeouts/web-tools, theme, haptics
    ime/ActikeyImeBridge.kt   # hook into HeliBoard IME without touching hot path
  app/src/test/...            # offline gate, insertion safety, privacy, mock-protocol
```

HeliBoard itself is consumed as a pinned submodule (`vendor/HeliBoard`,
see `HELIBOARD_PIN.md`) and is **not** vendored in this scaffold; CI clones
with `submodules: recursive`. Overlay sources below compile against it.

## Build

```powershell
cd android
./gradlew assembleActikeyRelease
./gradlew testActikeyDebugUnitTest
```

Key budgets: ≤5 ms p95 key-event overhead, ≤50 ms open latency, 0 bytes
typing traffic. `testActikey*` runs without GPU.

## Configure on device

Settings → Actikey → Server: host, port, API key (secure), model, timeouts
(≤2 s / 15 s / 60 s / 120 s ext.), optional laptop-side web tools on/off.
No key → keyboard fully usable; AI shows laptop-unavailable on invoke.
