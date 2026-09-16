# Desktop / laptop module (Windows + NVIDIA)

The laptop runs **stock LM Studio** — no custom inference daemon. This folder
adds setup docs, a config template, a stdlib-only laptop-side web-search tool,
the documented queue policy, and the real-model acceptance runner.

## Files

- `LM_STUDIO_SETUP.md` — install + LAN + API-key + model setup.
- `server_config.template.json` — host/port/model/timeout expectations.
- `tools/web_search.py` — optional external tool, runs on the laptop (stdlib
  `urllib` only). Keyboard never fetches the web directly.
- `QUEUE_POLICY.md` — 1 running + 1 waiting, busy-reject, cancel semantics.
- `acceptance/run_real_model_tests.py` — real-server gate (60 cases, ≥90%,
  zero unauthorized tools, full hardware report). Requires the actual server.
