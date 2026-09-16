# Privacy & network isolation

`INTERNET` permission is intentionally present (keyboard connects directly to
the laptop's LM Studio server). Isolation replaces prohibition:

## Rules

- Ordinary typing **never** causes network traffic. `org.actikey.net.*` is
  reachable only from explicit AI/skill/tool actions.
- Only the minimum context for the invoked op is sent, only to the configured
  host — except an explicitly enabled laptop-side web tool, whose external
  requests originate on the **laptop**, not the phone.
- No telemetry, analytics SDK, ads SDK, content logging, cloud fallback,
  account sync, or auto content-sync.
- Logs never contain API keys, passwords, full private text, or prompts with
  sensitive editor content (enforced by `LogScrubber` + `NoSecretsInLogsTest`).
- Sensitive input types block AI: `TYPE_TEXT_VARIATION_PASSWORD`,
  `TYPE_TEXT_VARIATION_VISIBLE_PASSWORD`, `TYPE_NUMBER_VARIATION_PASSWORD`,
  `TYPE_TEXT_VARIATION_WEB_PASSWORD`, PIN/phone-payment flags, and
  incognito/private (`IME_FLAG_NO_PERSONALIZED_LEARNING` / web incognito) —
  enforced in `InsertionGuard.isAiAllowed(editorInfo)` before any capture.
- API key stored in `EncryptedSharedPreferences` (AndroidKeystore-backed),
  never plaintext files/logs. Cert/identity pin change → hard fail, explicit
  user re-accept required.

## Verification (per release)

1. `NoTrafficOnTypingTest` (mock-socket layer asserts zero `connect()` during
   scripted en/fa typing, autocorrect, suggestions, clipboard, emoji, lang
   switch, app switch, restart).
2. Manual tcpdump: capture while doing extensive normal typing → expect zero
   Actikey packets; then invoke one AI op → expect connections **only** to the
   configured `<host>:<port>` (+ laptop-originated web traffic only if the
   optional tool was explicitly invoked).
3. `SensitiveInputBlockTest`: password/PIN/incognito editors → request refused,
   zero bytes captured/sent.
