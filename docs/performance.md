# Performance & efficiency budgets

Efficiency is first-class. Prefer the least-expensive correct solution; every
substantial dependency justifies APK size, startup, RAM, CPU, battery, attack
surface. No model/net/db/skill/doc work on the key-input critical path.

## Targets

| Metric | Target |
|---|---|
| Extra p95 key-event→commit vs HeliBoard baseline | ≤ 5 ms |
| Extra keyboard-open latency (cold + warm) | ≤ 50 ms |
| Action-Bar gesture ack (even if laptop down) | ≤ 100 ms |
| Timeouts | connect 2 s, first-output 15 s, ordinary 60 s, extended 120 s |
| Idle AI/net CPU | ≈ 0 (no polling/discovery/telemetry/service) |
| Typing network traffic | 0 bytes |

## What CI measures (artifacts per build)

APK size, installed size estimate, cold/warm start, key-event p50/p95 overhead,
idle/typing/AI-request RAM, idle/typing CPU, typing battery delta (lab harness
where available), typing vs AI-op network bytes. Baselines in
`android/benchmark/BaselineStore.md`; regressions fail CI if overhead > budget.

## How

- Untouched HeliBoard input path for typing; AI async off-thread.
- `HttpURLConnection`, short-lived connections, no frameworks/SDKs.
- `resConfigs "en","fa"`, stripped dicts/assets, no duplicated theme blobs
  (theming = params + tiny resources), standard Android haptics APIs.
- Haptic/sound/long-press/repeat/swipe-space-cursor all local params.
