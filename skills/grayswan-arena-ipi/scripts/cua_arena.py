#!/usr/bin/env python
"""CUA-driver helper for the Gray Swan Arena (read/stage/verify only).

Wraps the verified loop from references/cua-workflow.md:
dedicated headed agent-browser Chromium + cua-driver typed browser tools.

Usage:
  cua_arena.py setup            # headed browser + cookies + cua bind (writes cua_bind.json)
  cua_arena.py snap             # snapshot; print actionable refs; save outline_cua.txt
  cua_arena.py click <substr>   # trusted click first ref whose name contains <substr>
  cua_arena.py type <text>      # browser_type into the payload editor (ref with 'type' action)
  cua_arena.py clear            # empty the editor (replace:true)
  cua_arena.py shot <path>      # save viewport PNG
  cua_arena.py state            # editor value + Launch/Preview/Submit refs

NEVER clicks Launch / Submit Chat / Submit to Judges — those stay manual
(challenge rule: no automated submission). This helper refuses those names.
"""
import base64
import json
import os
import subprocess
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
COOKIES = os.path.join(HERE, "..", "session", "cookies.json")
URL = "https://app.grayswan.ai/arena/challenge/ipi-aug-2026"
SESSION = "gs-arena"
BIND = os.path.join(os.getcwd(), "cua_bind.json")
FORBIDDEN = ("launch", "submit chat", "submit to judges")


def sh(cmd, **kw):
    p = subprocess.run(cmd, capture_output=True, timeout=300, **kw)
    return p.stdout.decode("utf-8", "replace"), p.returncode


def ab(*args):
    out, rc = sh(["agent-browser", *args, "--session", "gs"])
    if rc != 0:
        raise RuntimeError(f"agent-browser {args[0]} failed: {out.strip()[:300]}")
    return out.strip()


def cua(tool, args):
    out, rc = sh(["cua-driver", tool, json.dumps(args)])
    try:
        return json.loads(out)
    except json.JSONDecodeError:
        raise RuntimeError(f"cua-driver {tool} failed: {out[:300]}")


def bind():
    return json.load(open(BIND))


def snap(include_screenshot=False):
    b = bind()
    return cua("get_browser_state", {
        "target_id": b["T"], "tab_id": b["TAB"], "session": SESSION,
        "snapshot_format": "semantic_v2", "include_screenshot": include_screenshot})


def find_ref(d, pred):
    for r in d["refs"]:
        if pred(r):
            return r
    return None


def cmd_setup():
    # headed dedicated browser
    try:
        ab("get", "url")
    except RuntimeError:
        ab("open", "--headed")
    ab("open", URL)                      # origin first so secure cookies stick
    ab("cookies", "set", "--curl", os.path.abspath(COOKIES))
    ab("reload")
    time.sleep(6)
    title = ab("get", "title")
    print("browser title:", title)
    # cua bind
    cua("start_session", {"session": SESSION})
    out, _ = sh(["cua-driver", "list_windows", "{}"])
    ws = json.loads(out)
    win = next((w for w in ws.get("structuredContent", ws).get("windows", [])
                if "Indirect Prompt Injection" in (w.get("title") or "")), None)
    if not win:
        sys.exit("arena window not found in list_windows")
    b = cua("get_browser_state", {"pid": win["pid"], "window_id": win["window_id"], "session": SESSION})
    if b.get("status") != "ok" or b.get("binding_quality") != "exact":
        sys.exit(f"bind failed: {json.dumps(b)[:300]}")
    tab = next(t for t in b["tabs"] if "grayswan" in (t.get("url") or ""))
    json.dump({"pid": win["pid"], "window_id": win["window_id"],
               "T": b["target_id"], "TAB": tab["tab_id"]}, open(BIND, "w"))
    print("bound:", b["target_id"], tab["tab_id"], "| mutation_allowed:", b.get("mutation_allowed"))


def cmd_snap():
    d = snap()
    open("outline_cua.txt", "w", encoding="utf-8").write(d.get("outline", ""))
    for r in d["refs"]:
        n = (r.get("name") or "")
        low = n.lower()
        if any(f in low for f in FORBIDDEN):
            continue  # never advertise launch/submit refs for scripting
        if r.get("actions"):
            print(r["ref"], "|", r["role"], "|", n[:64], "|", r.get("actions"))
    print("-> outline_cua.txt")


def cmd_click(substr):
    if substr.lower() in FORBIDDEN:
        sys.exit("refusing: Launch/Submit clicks are manual-only (challenge rule)")
    d = snap()
    r = find_ref(d, lambda x: substr.lower() in (x.get("name") or "").lower())
    if not r:
        sys.exit(f"no ref matching {substr!r}; run: cua_arena.py snap")
    b = bind()
    res = cua("browser_click", {"target_id": b["T"], "tab_id": b["TAB"],
                                "ref": r["ref"], "input_route": "trusted", "session": SESSION})
    print("click", r["ref"], "->", res.get("effect"))
    time.sleep(1)
    d2 = snap()
    sel = [x["ref"] for x in d2["refs"] if (x.get("states") or {}).get("selected")]
    print("selected now:", sel)


def cmd_type(text):
    b = bind()
    d = snap()
    ed = find_ref(d, lambda x: "type" in (x.get("actions") or []))
    if not ed:
        sys.exit("no typeable editor ref; select behavior+model and save first")
    res = cua("browser_type", {"target_id": b["T"], "tab_id": b["TAB"], "ref": ed["ref"],
                               "text": text, "mode": "insert_text", "session": SESSION})
    print("type ->", res.get("effect"), res.get("route"))
    d2 = snap()
    i = d2.get("outline", "").find("Article comment")
    print(d2.get("outline", "")[i:i + 200].replace("\n", " "))


def cmd_clear():
    b = bind()
    d = snap()
    ed = find_ref(d, lambda x: "type" in (x.get("actions") or []))
    if not ed:
        sys.exit("no editor ref")
    res = cua("browser_type", {"target_id": b["T"], "tab_id": b["TAB"], "ref": ed["ref"],
                               "text": "", "mode": "insert_text", "replace": True, "session": SESSION})
    print("clear ->", res.get("effect"))


def cmd_shot(path):
    d = snap(include_screenshot=True)
    open(path, "wb").write(base64.b64decode(d["screenshot_png_b64"]))
    print("->", path, d.get("screenshot_width"), "x", d.get("screenshot_height"))


def cmd_state():
    d = snap()
    o = d.get("outline", "")
    i = o.find("Article comment")
    print("editor:", o[i:i + 180].replace("\n", " "))
    for r in d["refs"]:
        n = (r.get("name") or "").lower()
        if any(f in n for f in FORBIDDEN):
            print("MANUAL-ONLY CONTROL PRESENT:", r["ref"], "|", r.get("name"))


def main():
    a = sys.argv[1:]
    if not a:
        print(__doc__)
        return 2
    if a[0] == "setup":
        cmd_setup()
    elif a[0] == "snap":
        cmd_snap()
    elif a[0] == "click":
        cmd_click(a[1])
    elif a[0] == "type":
        cmd_type(a[1])
    elif a[0] == "clear":
        cmd_clear()
    elif a[0] == "shot":
        cmd_shot(a[1])
    elif a[0] == "state":
        cmd_state()
    else:
        print(__doc__)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
