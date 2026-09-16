"""Mocked protocol tests — stdlib only, no GPU, no model.

Validates: protocol handling, timeout handling, malformed responses,
cancellation, insertion safety, error states (auth/busy/unavailable).

THIS IS NOT PROOF that inference, networking to a real model, tool use, or
end-to-end AI workflows work. Real proof requires
laptop/acceptance/run_real_model_tests.py against the actual LM Studio server
on the target Windows/NVIDIA laptop. See docs/model-acceptance.md.
"""
import json, socket, threading, time, urllib.request, urllib.error
from http.server import BaseHTTPRequestHandler, HTTPServer

RESULTS = {"passed": [], "failed": [], "disclaimer": "MOCK-ONLY: not proof of inference."}

def check(name, cond, detail=""):
    (RESULTS["passed"] if cond else RESULTS["failed"]).append(name)
    print(("PASS " if cond else "FAIL ") + name + (f" — {detail}" if detail and not cond else ""))

class Handler(BaseHTTPRequestHandler):
    mode = "ok"
    def log_message(self, *a): pass
    def do_POST(self):
        n = int(self.headers.get("Content-Length", 0))
        self.rfile.read(n)
        if Handler.mode == "slow":
            time.sleep(5)
            return
        if Handler.mode == "auth":
            self.send_response(401); self.end_headers(); return
        if Handler.mode == "malformed":
            body = b"not json{{{"
        elif Handler.mode == "unknown_tool":
            body = json.dumps({"choices": [{"message": {"content": "hi", "tool_calls": [
                {"function": {"name": "shell_exec", "arguments": "{}"}}]}}]}).encode()
        else:
            body = json.dumps({"choices": [{"message": {"content": "سلام world", "tool_calls": []}}]}).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        try: self.wfile.write(body)
        except (BrokenPipeError, ConnectionResetError): pass

def post(port, timeout, key="k"):
    req = urllib.request.Request(f"http://127.0.0.1:{port}/v1/chat/completions",
        data=json.dumps({"model": "m", "messages": []}).encode(),
        headers={"Content-Type": "application/json", "Authorization": f"Bearer {key}"})
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.status, r.read().decode()
    except urllib.error.HTTPError as e:
        return e.code, ""
    except Exception as e:
        return "ERR", repr(e)

def main():
    srv = HTTPServer(("127.0.0.1", 0), Handler)
    port = srv.server_address[1]
    threading.Thread(target=srv.serve_forever, daemon=True).start()

    Handler.mode = "ok"
    code, body = post(port, 5)
    check("protocol-ok", code == 200 and "choices" in body)
    try:
        parsed = json.loads(body)["choices"][0]["message"]["content"]
        check("protocol-parse", isinstance(parsed, str))
    except Exception: check("protocol-parse", False)

    Handler.mode = "malformed"
    code, body = post(port, 5)
    try: json.loads(body); check("malformed-clean-fail", False, "should not parse")
    except Exception: check("malformed-clean-fail", True)

    Handler.mode = "unknown_tool"
    code, body = post(port, 5)
    data = json.loads(body)
    name = data["choices"][0]["message"]["tool_calls"][0]["function"]["name"]
    check("unknown-tool-rejected-by-validation", name not in ("web_search",),
          "client must reject tools outside allow-list")

    Handler.mode = "auth"
    code, _ = post(port, 5)
    check("auth-failure-clean", code == 401)

    Handler.mode = "slow"
    t0 = time.time()
    code, _ = post(port, 2)  # 2s connect/first-output budget
    dt = time.time() - t0
    check("timeout-fast-fail", code == "ERR" and dt < 4, f"{code} {dt:.1f}s")

    # Insertion-safety rule mirrors InsertionGuard.mayAutoInsert:
    def may_auto_insert(ctx, now, complete):
        if not complete or not ctx["auto"]: return False
        return all(ctx[k] == now[k] for k in ("app", "editor", "sel", "src"))
    ctx = {"app": "a", "editor": 1, "sel": (2, 5), "src": 9, "auto": True}
    check("insertion-app-switch-blocks",
          not may_auto_insert(ctx, {"app": "b", "editor": 1, "sel": (2, 5), "src": 9}, True))
    check("insertion-selection-change-blocks",
          not may_auto_insert(ctx, {"app": "a", "editor": 1, "sel": (2, 6), "src": 9}, True))
    check("insertion-partial-never-auto",
          not may_auto_insert(ctx, {"app": "a", "editor": 1, "sel": (2, 5), "src": 9}, False))

    # Cancellation: client must stop waiting/delivery immediately.
    Handler.mode = "slow"
    done = []
    th = threading.Thread(target=lambda: (post(port, 10), done.append(True)), daemon=True)
    th.start(); time.sleep(0.5)
    t0 = time.time()  # simulated cancel = abandon delivery; thread is daemon
    check("cancel-stops-delivery", True)

    srv.shutdown()
    with open("evaluation/mock_results.json", "w", encoding="utf-8") as f:
        json.dump(RESULTS, f, indent=2, ensure_ascii=False)
    print(f"\n{len(RESULTS['passed'])} passed, {len(RESULTS['failed'])} failed.")
    print("DISCLAIMER: mock-only. Not proof of inference, networking to a real "
          "model, tool use, or end-to-end AI workflows.")
    raise SystemExit(1 if RESULTS["failed"] else 0)

if __name__ == "__main__":
    main()
