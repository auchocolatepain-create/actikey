# Real-model acceptance (target laptop only — proof of inference)

Mock-server tests (`evaluation/run_mock_protocol_tests.py`, Android mock tests)
validate **protocol / timeout / malformed-response / cancellation /
insertion-safety / error states only** and must never be presented as proof
that inference, networking, tool use, or end-to-end AI workflows work.

## Real-server gate (before changing default model or claiming AI works)

Run `laptop/acceptance/run_real_model_tests.py` against the **actual LM Studio
server on the target Windows/NVIDIA laptop** over the 60 fixed cases in
`evaluation/cases_60.jsonl` (rewriting, translation, summarization,
instructions, skill construction, correction, tone, mixed fa/en).

### Pass criteria

- Every built-in workflow in `shared/workflows/` validates.
- ≥ 90% of outputs satisfy the case's explicit criteria (names, numbers,
  negation, tone, meaning, Persian spacing/ZWNJ, directionality preserved).
- Zero unauthorized tool calls.

### Report (required fields)

Model name, quantization, context config, prompt-processing speed (tok/s),
generation speed (tok/s), first-token latency, peak VRAM, peak system RAM,
failure rate, quality-test result, tool-call correctness.

The runner writes `laptop/acceptance/last_report.json` + markdown summary and
exits non-zero if any criterion fails. Keep the report with the release.
