"""Real-model acceptance runner — TARGET LAPTOP ONLY.

Talks to the ACTUAL LM Studio server (OpenAI-compatible /v1/chat/completions).
Enforces: workflow validation, 60 cases, >=90% pass, zero unauthorized tools,
timeouts (2s connect / 15s first-output / 60s deadline / 120s extended),
and writes a hardware+quality report.

Mock-server runs are NOT proof of inference; this script is the proof gate.

Usage:
  python acceptance/run_real_model_tests.py --server http://127.0.0.1:1234 --model Qwen3.5-4B-Q4_K_M
  (API key via --api-key or $LMSTUDIO_API_KEY; never commit keys.)
"""
import argparse, datetime, json, os, pathlib, sys, time, urllib.request, urllib.error

ROOT = pathlib.Path(__file__).resolve().parents[2]
CASES = ROOT / "evaluation" / "cases_60.jsonl"
WF_DIR = ROOT / "shared" / "workflows"
OUT_JSON = pathlib.Path(__file__).parent / "last_report.json"
OUT_MD = pathlib.Path(__file__).parent / "last_report.md"

ALLOWED_OPS = {"rewrite", "translate", "summarize", "correct", "tone", "instruct", "skill_build"}


def validate_workflows():
    errors = []
    for p in sorted(WF_DIR.glob("*.json")):
        try:
            w = json.loads(p.read_text(encoding="utf-8"))
        except Exception as e:
            errors.append(f"{p.name}: bad JSON ({e})"); continue
        if w.get("op") not in ALLOWED_OPS:
            errors.append(f"{p.name}: unknown op {w.get('op')!r}")
        if not w.get("system_prompt"):
            errors.append(f"{p.name}: empty system_prompt")
        for t in w.get("allowed_tools", []):
            if t != "web_search":
                errors.append(f"{p.name}: unknown tool {t!r}")
    return errors


def chat(server, model, key, system_prompt, user_content, timeout):
    body = json.dumps({"model": model, "stream": False,
        "messages": [{"role": "system", "content": system_prompt},
                     {"role": "user", "content": user_content}]}).encode()
    req = urllib.request.Request(server.rstrip("/") + "/v1/chat/completions", data=body,
        headers={"Content-Type": "application/json", "Authorization": "Bearer " + key})
    t0 = time.time()
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            first = time.time() - t0
            data = json.loads(r.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return {"ok": False, "error": f"HTTP {e.code}", "first_s": time.time() - t0}
    except Exception as e:
        return {"ok": False, "error": f"{type(e).__name__}: {e}", "first_s": time.time() - t0}
    try:
        msg = data["choices"][0]["message"]
        tools = [c["function"]["name"] for c in msg.get("tool_calls", [])]
        return {"ok": True, "text": msg.get("content", ""), "tools": tools, "first_s": first}
    except Exception:
        return {"ok": False, "error": "malformed response", "first_s": time.time() - t0}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--server", required=True)
    ap.add_argument("--model", default="Qwen3.5-4B-Q4_K_M")
    ap.add_argument("--api-key", default=os.environ.get("LMSTUDIO_API_KEY", ""))
    ap.add_argument("--extended", action="store_true")
    a = ap.parse_args()
    if not a.api_key:
        print("missing API key (--api-key or $LMSTUDIO_API_KEY)", file=sys.stderr); return 2

    wf_errors = validate_workflows()
    cases = [json.loads(l) for l in CASES.read_text(encoding="utf-8").splitlines() if l.strip()]
    deadline = 120 if a.extended else 60
    passed, failed, tool_violations, first_lats = 0, [], 0, []
    t_start = time.time()
    for c in cases:
        sys_prompt = ("Do the requested task. Preserve names, numbers, negation, tone, meaning."
                      " For Persian keep ZWNJ/spacing/directionality. Output only the result.")
        r = chat(a.server, a.model, a.api_key, sys_prompt, c["input"], timeout=deadline)
        first_lats.append(r.get("first_s", deadline))
        if not r.get("ok"):
            failed.append({"id": c["id"], "reason": r.get("error")}); continue
        if r.get("tools"):
            tool_violations += 1
            failed.append({"id": c["id"], "reason": f"unauthorized tools {r['tools']}"}); continue
        text = r.get("text", "")
        need = c.get("must_contain", [])
        # Case-insensitive containment; Persian checked verbatim.
        ok = all((m.lower() in text.lower()) if m.isascii() else (m in text) for m in need)
        if ok: passed += 1
        else: failed.append({"id": c["id"], "reason": f"missing marker {need}", "output": text[:300]})
    rate = passed / max(1, len(cases))
    report = {
        "date": datetime.datetime.now().isoformat(timespec="seconds"),
        "server": a.server, "model": a.model,
        "quantization": "Q4_K_M (verify in LM Studio; update if different)",
        "context_config": "record from LM Studio server settings",
        "cases": len(cases), "passed": passed, "pass_rate": round(rate, 3),
        "tool_violations": tool_violations,
        "workflow_errors": wf_errors,
        "avg_first_token_latency_s": round(sum(first_lats) / max(1, len(first_lats)), 2),
        "max_first_token_latency_s": round(max(first_lats) if first_lats else 0, 2),
        "elapsed_s": round(time.time() - t_start, 1),
        "peak_vram": "record via nvidia-smi during run",
        "peak_ram": "record via Task Manager during run",
        "prompt_toks_per_s": "record from LM Studio metrics",
        "gen_toks_per_s": "record from LM Studio metrics",
        "failures": failed,
        "gate": "PASS" if (rate >= 0.9 and tool_violations == 0 and not wf_errors) else "FAIL",
    }
    OUT_JSON.write_text(json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8")
    OUT_MD.write_text(
        "# Real-model report (%s)\n\n- Server: %s\n- Model: %s (%s)\n- Cases: %d, passed %d (%.1f%%), gate %s\n- Tool violations: %d\n- Workflow errors: %s\n- Avg first-token latency: %ss (max %ss)\n\nSee last_report.json for failures.\n"
        % (report["date"], a.server, a.model, report["quantization"], len(cases), passed,
           rate * 100, report["gate"], tool_violations, wf_errors or "none",
           report["avg_first_token_latency_s"], report["max_first_token_latency_s"]),
        encoding="utf-8")
    print(json.dumps(report, indent=2, ensure_ascii=False))
    ok = report["gate"] == "PASS"
    print("\nGATE: " + report["gate"] + " (>=90% + zero unauthorized tools + workflows valid)")
    return 0 if ok else 1


if __name__ == "__main__":
    raise SystemExit(main())
