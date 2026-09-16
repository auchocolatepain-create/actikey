# Offline gate (release-blocking)

Install and enable Actikey in **airplane mode** before the laptop has ever
been configured or contacted. No server, cached auth, internet, LAN, or model.

Must all work locally:

- [ ] Type, delete chars/words, move cursor, select/replace
- [ ] Shift, Caps Lock, numbers/symbols, punctuation
- [ ] en-US ↔ fa switch (fast, local, no IME restart, no network)
- [ ] Persian RTL, mixed fa/en, ZWNJ (نیم‌فاصله), fa+Latin numbers, URLs
- [ ] Backspace around joined chars + ZWNJ; RTL/LTR selection
- [ ] Emoji, emoji sequences, clipboard, paste snippets, long-press alternatives
- [ ] Rotate, lock/unlock, switch apps, keyboard-process restart, phone restart

None may depend on LM Studio, laptop, internet, LAN, API key, prior connection,
or cached server data. The keyboard must be a complete keyboard even if AI is
never configured.

Automated: `android/.../OfflineGateTest` + manual checklist above signed off
per release. AI invoked while unreachable → ≤100 ms ack + concise
laptop-unavailable message (see `ActikeyActionBar`).
