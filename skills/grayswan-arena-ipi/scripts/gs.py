#!/usr/bin/env python
"""Gray Swan Arena recon CLI (READ-ONLY).

Mediates all traffic through an agent-browser session because app.grayswan.ai
sits behind a Vercel bot checkpoint that 429s non-browser HTTP clients.

Usage:
  gs.py setup                 # launch browser session + cookies + open challenge
  gs.py status                # energy, wave, health
  gs.py data [out.json]       # full rehydrated challenge dump (default gs_data.json)
  gs.py behaviors             # behavior table: slot, agent prompt, site, criteria names
  gs.py models                # model list with prize markers
  gs.py breaks <behaviorId>   # per-model unique break counts
  gs.py clusters <behaviorId> # payload cluster points (dedupe map)
  gs.py health                # challenge health endpoint
  gs.py evaljs <js>           # raw page-context eval (escape hatch, keep it read-only)

Never use this to POST to submit-payload/run-testcases/submit/batch — challenge
rules disqualify automated submissions.
"""
import json
import os
import shutil
import subprocess
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
SESSION = "gs"
COOKIES = os.path.join(HERE, "..", "session", "cookies.json")
CHALLENGE_URL = "https://app.grayswan.ai/arena/challenge/ipi-aug-2026"
CHALLENGE_ID = "6a718b22499bef6bb547ec97"
DATA_URL = "/arena/challenge/ipi-aug-2026/__data.json"


def _ab_exe():
    native = os.path.join(os.environ.get("LOCALAPPDATA", ""), "hermes", "node",
                          "node_modules", "agent-browser", "bin", "agent-browser-win32-x64.exe")
    if os.path.exists(native):
        return native
    for name in ("agent-browser.cmd", "agent-browser.exe", "agent-browser"):
        p = shutil.which(name)
        if p:
            return p
    sys.exit("agent-browser not found on PATH")


AB_EXE = None


def ab(*args):
    global AB_EXE
    if AB_EXE is None:
        AB_EXE = _ab_exe()
    cmd = [AB_EXE, *args, "--session", SESSION]
    p = subprocess.run(cmd, capture_output=True, text=True, timeout=180)
    if p.returncode != 0:
        raise RuntimeError(f"agent-browser {' '.join(args[:2])} failed: {p.stderr.strip()[:400]}")
    return p.stdout.strip()


def eval_js(js):
    out = ab("eval", js)
    try:
        return json.loads(out)
    except json.JSONDecodeError:
        return out


def extract_string(var, chunk=38000):
    """Pull a large string out of the page via slice chunks."""
    total = int(eval_js(f"{var}.length"))
    parts = []
    off = 0
    while off < total:
        part = eval_js(f"{var}.slice({off},{off + chunk})")
        if not isinstance(part, str):
            raise RuntimeError(f"chunk extract failed at {off}")
        parts.append(part)
        off += chunk
    return "".join(parts)


REHYDRATE_JS = r"""
(()=>{
  const lines = window.__gsRaw.split("\n").filter(l=>l.trim());
  const main = JSON.parse(lines[0]);
  const arr = main.nodes[2].data;
  const seen = new Map();
  function container(x, depth){
    if (depth > 60) return x;
    if (Array.isArray(x)) {
      if (seen.has(x)) return seen.get(x);
      const a = []; seen.set(x, a);
      x.forEach((v,i)=> a[i] = value(v, depth+1));
      return a;
    }
    if (x && typeof x === "object") {
      if (seen.has(x)) return seen.get(x);
      const o = {}; seen.set(x, o);
      for (const [k,v] of Object.entries(x)) o[k] = value(v, depth+1);
      return o;
    }
    return x;
  }
  function value(v, depth){
    if (typeof v === "number") return container(arr[v], depth);
    return v;
  }
  window.__gsGet = (i)=>container(arr[i], 0);
  window.__gsSafe = (o)=>{
    const s = new Set();
    return JSON.stringify(o, (k,v)=>{
      if (typeof v === "object" && v !== null) {
        if (s.has(v)) return "[circular]";
        s.add(v);
      }
      return v;
    });
  };
  return Object.keys(main.nodes[2].data[0]).join(",");
})()
"""

FETCH_JS = (
    "(async()=>{const r=await fetch(\"" + DATA_URL + "\",{credentials:\"include\"});"
    "window.__gsRaw=await r.text();return r.status+\" \"+window.__gsRaw.length;})()"
)


def load_challenge():
    """Fetch __data.json into the page and install the rehydrator."""
    res = eval_js(FETCH_JS)
    if not str(res).startswith("200"):
        raise RuntimeError(f"__data.json fetch failed: {res}")
    keys = eval_js(REHYDRATE_JS)
    return keys


def cmd_setup():
    cookies = os.path.abspath(COOKIES)
    if not os.path.exists(cookies):
        sys.exit(f"missing cookies: {cookies} (export from Cookie-Editor as JSON)")
    try:
        url = eval_js("location.href")
        if CHALLENGE_URL not in str(url):
            ab("open", CHALLENGE_URL)
    except RuntimeError:
        subprocess.run(["agent-browser", "open", "--session", SESSION],
                       capture_output=True, text=True, timeout=180)
        ab("cookies", "set", "--curl", cookies)
        ab("open", CHALLENGE_URL)
    time.sleep(5)
    title = eval_js("document.title")
    authed = eval_js(
        "(async()=>{const r=await fetch(\"/api/compete/challenges/"
        + CHALLENGE_ID + "/health\",{credentials:\"include\"});return r.status;})()"
    )
    print(f"title: {title}")
    print(f"health endpoint: {authed}")
    if "challenge" not in str(title).lower() and "arena" not in str(title).lower():
        print("WARNING: page did not load the challenge; session may be stale.", file=sys.stderr)
        return 1
    user = eval_js("(async()=>{try{const r=await fetch(\"" + DATA_URL +
                   "\",{credentials:\"include\"});const t=await r.text();" +
                   "const main=JSON.parse(t.split('\\n')[0]);" +
                   "for(const n of main.nodes){const arr=n.data;if(!Array.isArray(arr))continue;" +
                   "for(const el of arr){if(el&&typeof el==='object'&&!Array.isArray(el)&&'arena_display_name' in el){" +
                   "const v=arr[el['arena_display_name']];return typeof v==='string'?v:JSON.stringify(v);}}}return '(not found)';" +
                   "}catch(e){return \"ERR \"+e.message}})()")
    print(f"authenticated as: {user}")
    return 0


def cmd_status():
    keys = load_challenge()
    out = eval_js(r"""
(()=>{
  const g = window.__gsGet, s = window.__gsSafe;
  const ci = g(2), energy = g(3165), cfg = ci.energy_config;
  const dataIdx = window.__gsRaw ? 0 : 0;
  return JSON.stringify({
    name: ci.name, slug: ci.slug, status: ci.status,
    start: ci.start_time, end: ci.completed_time,
    prize_pool: ci.available_prize_money,
    waves: (ci.waves||[]).map(w=>({name:w.name, start:w.start_time, end:w.end_time, behaviors:(w.behaviors||[]).length})),
    energy: {bank: energy.energy_bank, regen: energy.energy_regen, regen_max: energy.energy_regen_max, last_updated: energy.energy_last_updated},
    energy_config: cfg,
  });
})()
""")
    print(json.dumps(json.loads(out), indent=2))
    return 0


def slim_behavior(bm):
    """Build the slim behaviors map inside the page; returns var name."""
    eval_js(r"""
(()=>{
  const bm = window.__gsGet(2900);
  const slim = {};
  for (const [id, bb] of Object.entries(bm)) {
    slim[id] = {
      name: bb.name, type: bb.type, objective: bb.objective,
      summary: bb.summary, wave: bb.wave,
      slots: (bb.potemkin_slots||[]).map(s=>({key:s.key,label:s.label})),
      site: bb.potemkin_site_url, path: bb.potemkin_task_path,
      agent_prompt: bb.browser_agent_prompt,
      criteria: (bb.criteria||[]).map(c=>({name:c.name, threshold:c.threshold, blurb:c.blurb})),
    };
  }
  window.__gsDump = window.__gsSafe(slim);
  return window.__gsDump.length;
})()
""")
    return json.loads(extract_string("window.__gsDump"))


def cmd_behaviors():
    load_challenge()
    behaviors = slim_behavior(None)
    for bid, b in behaviors.items():
        slots = ", ".join(f"{s['key']}({s['label']})" for s in b.get("slots") or []) or "-"
        site = (b.get("site") or "") + (b.get("path") or "") or "-"
        crit = "; ".join(c["name"] for c in b.get("criteria") or [])
        print(f"{bid}")
        print(f"  category : {b.get('type')}")
        print(f"  slot     : {slots}")
        print(f"  site     : {site}")
        print(f"  agent    : {b.get('agent_prompt') or '-'}")
        print(f"  criteria : {crit}")
        print(f"  objective: {(b.get('objective') or '')[:160]}")
        print()
    json.dump(behaviors, open("behaviors.json", "w"), indent=1)
    print("-> behaviors.json")
    return 0


def cmd_models():
    load_challenge()
    out = eval_js(r"""
(()=>{
  const g = window.__gsGet;
  const mm = g(2851), cost = g(3172), msgCost = g(3173);
  const rows = [];
  for (const [mid, arr] of Object.entries(mm)) {
    for (const m of arr) {
      rows.push({name:m.name, association_id:m._id, model_id:m.model_id,
                 disabled:m.disabled, message_cost: msgCost ? msgCost[m._id] : null,
                 run_cost: cost ? cost[m._id] : null});
    }
  }
  rows.sort((a,b)=>a.name.localeCompare(b.name));
  return JSON.stringify(rows);
})()
""")
    rows = json.loads(out)
    for r in rows:
        flag = " [disabled]" if r["disabled"] else ""
        print(f"{r['name']:<32} assoc={r['association_id']} msg_cost={r['message_cost']}{flag}")
    json.dump(rows, open("models.json", "w"), indent=1)
    print("-> models.json")
    return 0


def cmd_data(out_path):
    keys = load_challenge()
    eval_js(r"""
(()=>{
  const g = window.__gsGet, s = window.__gsSafe;
  const ci = g(2);
  const bm = g(2900);
  const slimB = {};
  for (const [id, bb] of Object.entries(bm)) {
    slimB[id] = { name: bb.name, type: bb.type, description: bb.description,
      objective: bb.objective, criteria: bb.criteria, wave: bb.wave,
      potemkin_slots: bb.potemkin_slots, browser_agent_prompt: bb.browser_agent_prompt,
      potemkin_site_url: bb.potemkin_site_url, potemkin_task_path: bb.potemkin_task_path,
      judging: bb.judging, summary: bb.summary, doc: bb.doc,
      system_prompt_config: bb.system_prompt_config,
      input_validation_regex: bb.input_validation_regex };
  }
  const out = {
    challenge: { _id: ci._id, slug: ci.slug, name: ci.name, description: ci.description,
      details: ci.details, status: ci.status, start_time: ci.start_time,
      judging_time: ci.judging_time, completed_time: ci.completed_time,
      available_prize_money: ci.available_prize_money, single_turn: ci.single_turn,
      has_tool_calls: ci.has_tool_calls, streaming_enabled: ci.streaming_enabled,
      auto_moderate: ci.auto_moderate, input_message: ci.input_message,
      energy_config: ci.energy_config, waves: ci.waves, features: ci.features },
    behaviors: slimB,
    modelIdNameMap: g(2851),
    modelListsByWave: g(2592),
    modelCostMap: g(3172),
    modelMessageCostMap: g(3173),
    energyState: g(3165),
  };
  window.__gsDump = s(out);
  return window.__gsDump.length;
})()
""")
    data = json.loads(extract_string("window.__gsDump"))
    json.dump(data, open(out_path, "w"), indent=1)
    print(f"-> {out_path} ({os.path.getsize(out_path)} bytes)")
    return 0


def cmd_breaks(bid):
    out = eval_js(
        "(async()=>{const r=await fetch(\"/api/compete/challenges/" + CHALLENGE_ID +
        "/behaviors/" + bid + "/unique-breaks\",{credentials:\"include\"});"
        "return r.status+\" \"+(await r.text());})()"
    )
    status, _, body = str(out).partition(" ")
    if status != "200":
        print(out)
        return 1
    d = json.loads(body)
    print(json.dumps(d, indent=2))
    return 0


def cmd_clusters(bid):
    out = eval_js(
        "(async()=>{const r=await fetch(\"/api/compete/challenges/" + CHALLENGE_ID +
        "/behaviors/" + bid + "/clusters\",{credentials:\"include\"});"
        "return r.status+\" \"+(await r.text());})()"
    )
    status, _, body = str(out).partition(" ")
    if status != "200":
        print(out)
        return 1
    d = json.loads(body)
    pts = d.get("points") or []
    print(f"enabled={d.get('enabled')} points={len(pts)}")
    if pts:
        sample = pts[0]
        print("point keys:", list(sample.keys()))
        print(json.dumps(pts[:5], indent=1)[:2000])
    json.dump(d, open(f"clusters_{bid}.json", "w"), indent=1)
    print(f"-> clusters_{bid}.json")
    return 0


def cmd_health():
    out = eval_js(
        "(async()=>{const r=await fetch(\"/api/compete/challenges/" + CHALLENGE_ID +
        "/health\",{credentials:\"include\"});return r.status+\" \"+(await r.text());})()"
    )
    print(out)
    return 0


def main():
    args = sys.argv[1:]
    if not args:
        print(__doc__)
        return 2
    cmd, rest = args[0], args[1:]
    if cmd == "setup":
        return cmd_setup()
    if cmd == "status":
        return cmd_status()
    if cmd == "data":
        return cmd_data(rest[0] if rest else "gs_data.json")
    if cmd == "behaviors":
        return cmd_behaviors()
    if cmd == "models":
        return cmd_models()
    if cmd == "breaks":
        return cmd_breaks(rest[0]) if rest else 2
    if cmd == "clusters":
        return cmd_clusters(rest[0]) if rest else 2
    if cmd == "health":
        return cmd_health()
    if cmd == "evaljs":
        print(json.dumps(eval_js(rest[0]), indent=1) if rest else 2)
        return 0
    print(__doc__)
    return 2


if __name__ == "__main__":
    sys.exit(main())
