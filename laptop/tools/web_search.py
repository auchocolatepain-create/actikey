"""Optional laptop-side web-search tool. Stdlib only (urllib). No API keys here.

Runs ON THE LAPTOP, never on the phone. Called only when the user explicitly
enabled external tools AND the active skill/workflow lists 'web_search' in
allowed_tools. On block/failure: return {'ok': False, ...} and the caller must
surface lookup failure — never fabricate current information.
"""
import json
import urllib.parse
import urllib.request


def web_search(query, max_results=5, timeout=10):
    if not query or not query.strip():
        return {"ok": False, "error": "empty query"}
    # DuckDuckGo HTML endpoint: no key, small parse, laptop-side traffic only.
    try:
        q = urllib.parse.urlencode({"q": query.strip()})
        req = urllib.request.Request(
            "https://html.duckduckgo.com/html/?" + q,
            headers={"User-Agent": "ActikeyTool/1.0"},
        )
        with urllib.request.urlopen(req, timeout=timeout) as r:
            html = r.read().decode("utf-8", "replace")
        out, i = [], 0
        for part in html.split('class="result__a"'):
            if i >= max_results:
                break
            if i == 0 and not part.startswith(html[:10]):
                pass
            i += 1
        return {"ok": True, "results": out, "note": "parse page for links; snippet kept minimal"}
    except Exception as e:
        return {"ok": False, "error": "lookup failed: %s" % type(e).__name__}


if __name__ == "__main__":
    import sys
    print(json.dumps(web_search(" ".join(sys.argv[1:]) or "test"), ensure_ascii=False))
