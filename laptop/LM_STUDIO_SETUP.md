# LM Studio setup (target Windows/NVIDIA laptop)

1. Install LM Studio for Windows. Install NVIDIA drivers + CUDA runtime.
2. Download default model: **Qwen3.5-4B Q4_K_M** (stay on this until the
   real-model acceptance gate passes for any replacement).
3. Start the server: LM Studio → Developer/Local Server → Start.
   - Bind to LAN (e.g. `0.0.0.0:1234`) so the phone can reach it, or use
     `127.0.0.1:1234` for loopback-only testing.
   - Enable API-key auth; generate a long random key.
   - Note the exact model id shown by `/v1/models`.
4. Firewall: allow inbound TCP on the chosen port from the phone's LAN only.
5. In Actikey keyboard settings enter: host (laptop LAN IP), port, API key,
   model id. Keep timeouts at 2 s / 15 s / 60 s (120 s extended).
6. Verify: `curl -H "Authorization: Bearer <key>" http://<ip>:<port>/v1/models`
7. Optional web tools: only if you enable them in the keyboard AND install
   `laptop/tools/web_search.py` usage on this machine. External fetches run
   here, never on the phone. If the search engine blocks a lookup, report
   failure — never fabricate.
8. Record context config, VRAM/RAM peaks during the acceptance run
   (Task Manager / `nvidia-smi`) into the report.
