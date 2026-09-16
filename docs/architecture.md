# Architecture — single-app, offline-first

## Constraint

> Actikey must be implemented as a single keyboard application. There must not
> be a separate bridge APK. … Despite having networking capability, the keyboard
> itself must remain fully functional without any network connection whatsoever.

## Components (all inside one APK)

```
┌─ Actikey IME (HeliBoard input path, UNTOUCHED hot path) ─────────┐
│ tap → key-event → editor commit (≤5ms p95 overhead target)        │
│ en-US QWERTY + fa standard layouts, ZWNJ, RTL/LTR, emoji, clip   │
└───────────────────────────────────────────────────────────────────┘
        │ explicit user gesture only
        ▼
┌─ Action Bar / AI UI ─────────────────────────────────────────────┐
│ ack gesture ≤100ms even if laptop unreachable; async runs;       │
│ InsertionGuard binds: app id, editor id, selection, inputType,   │
│ op, autoReplaceAllowed. App/editor/selection change → block      │
│ auto-insert; partial output never auto-inserts.                   │
└───────────────────────────────────────────────────────────────────┘
        │ on-demand short-lived HTTPS/HTTP POST, then close
        ▼
┌─ LmStudioClient (tiny) ──────────────────────────────────────────┐
│ java.net.HttpURLConnection only. No OkHttp/Retrofit/cronet.      │
│ Timeouts: connect 2s, first-byte 15s, ordinary 60s, ext. 120s.   │
│ Auth: Bearer API key from EncryptedSharedPreferences.            │
│ No polling, no persistent socket, no sync framework.             │
│ Close or pooled-keepalive ≤1 conn, idle = zero CPU.              │
└───────────────────────────────────────────────────────────────────┘
                          LAN / local
                          ▼
              LM Studio server (laptop) → local model
              (+ optional laptop-side web_search tool)
```

Ordinary typing **never** touches `org.actikey.net.*`. Verified by
`NoTrafficOnTypingTest` + manual tcpdump procedure in `docs/privacy.md`.

## Laptop side

The laptop runs stock LM Studio (OpenAI-compatible `/v1/chat/completions`).
This repo's `laptop/` adds only: setup docs, config template, a stdlib-only
`web_search.py` tool executed **on the laptop**, a documented queue policy
(1 running + 1 waiting, excess → immediate busy-reject), and the real-model
acceptance runner. No custom daemon is required for inference itself.

## Failure table (implemented in `ActikeyRunner` + `InsertionGuard`)

Laptop off / offline / LM Studio stopped / asleep → concise unavailable msg.
Key rejected → auth/config error, typing unaffected.
Model OOM/crash → clean fail, typing unaffected.
Net drop mid-run → partial marked INCOMPLETE, never inserted.
App/editor/selection change → auto-insert/replace forbidden; user picks
insert/copy/replace explicitly. Password/PIN/incognito → request blocked
before any capture. Cert/pin change → hard fail, require explicit re-accept.
Config change → fail safe, require correction. Never auto-replay; manual retry.
Cancel → stop delivery to client immediately (best-effort abort server-side).
