# ActiKeyBoard — Actikey single-APK keyboard + laptop harness

Actikey is **one Android keyboard APK** based on HeliBoard, plus a lightweight
laptop-side harness/docs for the user's Windows/NVIDIA LM Studio server.

```
Actikey keyboard → local/LAN → LM Studio server on laptop → locally running model
```

No bridge APK. No companion Android process. No cloud-model fallback.
Ordinary typing is fully offline and must never emit network traffic.

## Repo layout

| Path | What |
|---|---|
| `android/` | Phone module: HeliBoard-based keyboard APK. IME, en-US + fa layouts, Action Bar / AI UI, tiny `HttpURLConnection` LM Studio client, `EncryptedSharedPreferences` API-key storage, insertion guard, settings. |
| `laptop/` | Desktop module: Windows/NVIDIA LM Studio setup, server config template, laptop-side web-search tool (stdlib only), queue policy (1 running + 1 waiting), real-model acceptance runner. |
| `shared/` | Skill + workflow JSON schemas, built-in definitions. |
| `evaluation/` | 60 fixed en/fa quality cases + mock-protocol tests + real-model reporter. Mock tests are **never** proof of inference. |
| `docs/` | Architecture, offline gate, privacy/network, performance budgets, model-acceptance procedure. |
| `.github/workflows/` | `android-ci.yml` (build APK + non-model tests, no GPU) and `real-model-acceptance.yml` (manual, target laptop only). |

## Defaults (pinned)

- Personal use. English (US) + Persian only. No other language packs bundled.
- HeliBoard foundation (pin: see `android/HELIBOARD_PIN.md`).
- One Android keyboard APK, networking built directly into it (`INTERNET` permission, but typing never uses it).
- Windows/NVIDIA laptop inference via LM Studio OpenAI-compatible API + API key.
- Default model: **Qwen3.5-4B Q4_K_M** (change only after full real-server acceptance run).
- Timeouts: 2 s connect, 15 s first-output, 60 s ordinary deadline, 120 s extended document mode.
- No background AI generation. No content logging. No telemetry/analytics/ads SDKs. No account integration.
- Optional external web tools, executed **laptop-side** where possible.
- Optional user-imported swipe library (failure isolated from tap typing).
- Gboard-like dimensions, configurable height/appearance/haptics, efficient local suggestions.

## Quick start

### Phone module (CI builds APK, no GPU needed)

```powershell
cd android
./gradlew assembleActikeyRelease
./gradlew testActikeyDebugUnitTest
python ..\evaluation\run_mock_protocol_tests.py
```

See `android/README.md`.

### Laptop module (target Windows/NVIDIA laptop only)

1. Install LM Studio, download `Qwen3.5-4B Q4_K_M`, start server on LAN.
2. Set API key, bind address, port (see `laptop/LM_STUDIO_SETUP.md`).
3. Point Actikey keyboard settings at `http://<laptop-lan-ip>:<port>` + key.
4. Run real-model acceptance **only on that laptop**:
```powershell
cd laptop
python acceptance\run_real_model_tests.py --server http://127.0.0.1:1234 --model Qwen3.5-4B-Q4_K_M --api-key $env:LMSTUDIO_API_KEY
```

Mock-server tests validate protocol/timeout/cancellation/insertion-safety only.

## Offline resilience = release gate

Install + enable Actikey in **airplane mode** before ever configuring the laptop.
All of typing, delete, cursor, selection, shift/caps, numbers/symbols,
punctuation, en↔fa switch, Persian RTL/ZWNJ/mixed text, emoji, clipboard,
snippets, long-press, rotate, lock/unlock, app switch, process/phone restart
must work with zero server, zero cached auth, zero network. See
`docs/offline-gate.md`.

Invoking AI while unreachable → concise "laptop unavailable" message.
Never queue offline AI jobs. Never auto-replay. Manual retry only.

## Efficiency

Lightest practical implementation: no permanent connection, no polling,
no sync framework, no cloud SDK, no telemetry. AI path is async and off the
key-input critical path. Targets: ≤5 ms p95 key-event overhead vs HeliBoard
baseline, ≤50 ms extra open latency, ~zero idle CPU. See `docs/performance.md`.

## Security / privacy

- API key in `EncryptedSharedPreferences` (AndroidKeystore), never in logs/files.
- Only minimum context for an explicitly invoked AI op is transmitted, and only
  to the configured LM Studio host (except an explicitly enabled laptop-side web tool).
- Sensitive input types (password/PIN/payment/incognito) block all AI capture.
- Cert/identity change is never silently trusted. See `docs/privacy.md`.
