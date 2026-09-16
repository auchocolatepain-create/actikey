# Queue policy (single-user installation)

- Bound: **1 running + 1 waiting**. Any further request is immediately
  rejected with a busy message — never silently queued.
- No offline queueing: if the server is unreachable, fail fast with the
  laptop-unavailable message. Never store AI jobs for later auto-execution.
- No auto-replay of failed model or web requests. The user may retry manually.
- Cancellation stops delivery of generated output to the phone client
  immediately, even if model-side computation takes slightly longer to
  terminate (client drops the stream; server abort is best-effort via
  connection close).
- Implemented phone-side in `ActikeyRunner` (semaphores 1+1); this document is
  the normative policy the runner and the acceptance tests enforce.
