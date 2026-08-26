---
name: dsl-vm-reverse
description: "Reverse JavaScript-based custom DSL/VM interpreters, non-standard WASM-like runtimes, and risk-control engines. Use when analyzing IIFE or switch-based opcode dispatchers, extracting instruction tables, recovering bytecode semantics, capturing VM state at runtime, or reconstructing execution flow."
version: 1.0.0
license: MIT
metadata:
  package: unleash-skills
  author: NetVar1337/unleash
  category: reverse-skill
  upstream: https://github.com/zhaoxuya520/reverse-skill
---

> Bundled with Unleash skills pack. Upstream: https://github.com/zhaoxuya520/reverse-skill

# 🔄 Custom DSL Virtual Machine Reverse Engineering (DSL VM Reverse Engineering)

> For reverse engineering custom WASM virtual machines / risk-control engines implemented in JavaScript

---

## Table of Contents

- [1. Scope](#1-scope)
- [2. DSL VM Identification Signatures](#2-dsl-vm-identification-signatures)
- [3. General Reverse Engineering Workflow](#3-general-reverse-engineering-workflow)
- [4. Opcode Extraction and Classification](#4-opcode-extraction-and-classification)
- [5. Runtime Capture Approaches](#5-runtime-capture-approaches)
- [6. Common Status Codes](#6-common-status-codes)
- [7. Skill Self-Check Checklist](#7-skill-self-check-checklist)

---

## 1. Scope

Use this skill when the target file matches **any** of the following signatures:

| # | Signature | Notes |
|---|------|------|
| 1 | Starts with an IIFE + lots of single-letter variable names | `!function(){var U=void 0,y=parseInt,E0=Function,...}` |
| 2 | Contains `DG()` or a similar function with a switch-case loop | Interpreter main loop, `d[7]&31` decodes the opcode |
| 3 | Large file (500KB+) but zero-byte ratio < 1% | Non-standard WASM, pure JS |
| 4 | Contains `C[number]` constant table references | `C[9][xxx]` function table / string table |
| 5 | Single-line minified code | 583KB on one line, obfuscated variable names |

### Exclusion Rules

| Condition | Not this skill | Go To |
|------|-----------|------|
| File starts with `\x00asm` | Standard WASM binary | `reverse-engineering/languages.md` |
| File contains `Uint8Array([0,97,115,109])` with the WASM magic bytes | Embedded WASM | Extract the .wasm, then use IDA/Ghidra |
| Standard Webpack bundle (`function(e,t,n){...}`) | Ordinary JS | `js-reverse/` |
| Zero-byte ratio > 20% | WASM binary | `reverse-engineering/languages.md` |

---

## 2. DSL VM Identification Signatures

### Code Signatures

```javascript
// Signature 1: IIFE entry, single-letter variables mapping numeric constants
!function(){
    var U=void 0, y=parseInt, E0=Function, AN=Uint8Array;
    var E=15, l=10, m=12, x=16, S=13, $=11;
    // numeric constants mapped to variable names, replacing raw numbers
    ...
}

// Signature 2: interpreter main loop DG()
function DG(C, d, ...) {
    var d = [];  // array simulating the WASM stack/locals
    for (d[7] = x; d[7] !== U;) {
        var aE = d[7] & 31;         // low 5 bits = opcode
        var O = d[7] >> 5 & 31;      // high 5 bits = sub-operation
        switch (aE) {
            case 0: /* ... */ d[7] = 612; break;
            case 1: /* ... */
            // ... N cases
        }
    }
}

// Signature 3: constant table C[9] storing function indices and strings
// C[9][0] = ["pc"]      → function parameter description
// C[9][667] = "string"  → string constant
// C[9][x] = number      → function index

// Signature 4: W(C[index], null, ...) call pattern
// W = Function.prototype.call.bind(call)
// all built-in functions are invoked via C[index] indexing

// Signature 5: instruction encoding format
// d[7] = opcode(bit 0-4) | subop(bit 5-9) | operand(bit 10+)
```

### Opcode Encoding Format

Each instruction is encoded as a 32-bit integer:

```
bit 0-4:   opcode (0-N)
bit 5-9:   sub-operation (0-31)
bit 10-31: operand/immediate

Decoding:
  aE = d[7] & 31        → opcode
  O  = d[7] >> 5 & 31   → sub-operation
  d[other] = d[7] >> 10  → operand
```

---

## 3. General Reverse Engineering Workflow

### Phase 1: File Classification (5 minutes)

```bash
# Check whether it is a DSL VM
python3 << 'EOF'
with open('target.js', 'rb') as f:
    head = f.read(100)

# 1. Check for WASM magic bytes
if head[:4] == b'\x00asm':
    print("Standard WASM binary")
    exit()

# 2. Check zero-byte ratio
data = open('target.js', 'rb').read()
zero_pct = data.count(b'\x00') / len(data) * 100
print(f"Zero-byte ratio: {zero_pct:.1f}%")

if zero_pct > 20:
    print("WASM binary")
elif head[:2] == b'!f':
    # Check for the single-letter variable pattern
    if b'var U=void 0' in head or b'U=void 0,y=parseInt' in head:
        print("→ DSL VM!")
    else:
        print("Ordinary JS IIFE")
EOF
```

### Phase 2: Variable Mapping Table Extraction (10 minutes)

```python
import re

with open('target.js', 'r', errors='replace') as f:
    s = f.read()

# Extract var X=number mappings from the first 2000 characters
mappings = re.findall(r'var\s+(\w+)\s*=\s*(\d+)', s[:2000])
print('Constant mappings:')
for name, val in mappings:
    print(f"  {name:4s} = {val:3d} (0x{int(val):02x})")
```

### Phase 3: Opcode Extraction and Classification (15 minutes)

```python
# 1. Extract all cases
all_cases = re.findall(r'case\s+(\d+):', s)
unique = sorted(set(int(c) for c in all_cases))

print(f"Total cases: {len(all_cases)}")
print(f"Unique opcodes: {len(unique)}: {unique}")

# 2. Classify each opcode
for op in unique:
    idx = s.find(f'case {op}:')
    snippet = s[idx:idx+200]
    if 'd[7]=' in snippet:
        op_type = 'BRANCH'
    elif 'return' in snippet:
        op_type = 'RETURN'
    elif 'W(C[' in snippet:
        op_type = 'CALL'
    elif 'new' in snippet:
        op_type = 'ALLOC'
    elif 'try' in snippet or 'catch' in snippet:
        op_type = 'EXCEPTION'
    else:
        op_type = 'ARITH/STORE'
    print(f"  opcode {op:2d}: {op_type}")
```

### Phase 4: Constant Table Analysis (30 minutes)

```python
const_refs = re.findall(r'C\[9\]\[(\d+)\]', s)
unique_refs = sorted(set(int(x) for x in const_refs))

print(f"C[9] references: {len(unique_refs)} indices")
print(f"Range: {min(unique_refs)} - {max(unique_refs)}")

# Analyze the context of each reference
for ref in unique_refs[:20]:
    idx = s.find(f'C[9][{ref}]')
    ctx = s[max(0,idx-50):idx+80]
    clean = ''.join(c if c.isprintable() else ' ' for c in ctx)
    print(f"  C[9][{ref}] → {clean}")
```

### Phase 5: Exported Function Tracing (1-2 hours)

Exported functions (e.g. `getToken`) are located via the following path:

```
1. Find AWSCInner.register() or a similar registration call
2. Determine the registered module and factory function
3. Find the object returned by the factory function → location of the exported function definition
4. If the function name is not in the JS → it is stored as bytecode in the C[9] constant table
5. Trace the call chain:
   AWSCInner._modules['fy'].getToken()
   → W(C[function index], null, ...)
   → DG() interpreter executes the encoded instruction sequence
```

### Phase 6: Runtime Injection (if pure static analysis is not enough)

```javascript
// Inject a minimal AWSC-compatible environment
const fakeEnv = {
    AWSCInner: {
        _modules: {},
        register(name, moduleName, factory) {
            this._modules[moduleName] = factory();
        }
    }
};

// Execute the DSL VM code
dslVmCode();

// Get the exports
const token = fakeEnv.AWSCInner._modules['fy'].getToken({});
```

---

## 4. Opcode Extraction and Classification

### Reference Opcode Mapping Table (based on existing cases)

| Opcode | Operation Type | Signature |
|--------|---------|------|
| 0 | **BRANCH** | `d[7]=xxx` unconditional jump |
| 1 | **CALL** | `W(C[Y],null,function(){...})` embedded function call |
| 2 | **ARITH** | `d[4]=0`, `d[7]=72` variable assignment |
| 3 | **ARITH** | `d[0]=d[1][C[x]]`, `d[5]=d[0]<d[3]` comparison operation |
| 4 | **STORE** | `d[8]=d[5]in d[4]` property access / existence check |
| 5 | **ARITH** | `d[8]=d[4]-d[8]` arithmetic operation |
| 6 | **RETURN** | `return gV`, `throw` return / throw exception |
| 7 | **ALLOC** | `d[6]=[]`, `d[6][C[8]](...)` push operation |
| 8 | **BRANCH** | `d[7]=d[k]?512:425` conditional jump |
| 9 | **STRING** | `d[6][C[t]]=d[m]`, `new fh(...)` regex |
| 10 | **ALLOC** | function argument preparation, call stack creation |
| 11 | **STRING** | `new fh("\\s",d[5])` regex matching |
| 12 | **STORE** | `P[d[9]]=d[4][C[H]](d[3])` data transfer |
| 13 | **CALL** | `C[9][113]=d[9]` module initialization |
| 14 | **STRING** | `d[8]=d[9]+d[m]` string concatenation |
| 15 | **RETURN** | `return EL;` function return |
| 16 | **ALLOC** | `var r,P,Z,B...` local variable declarations |
| 17 | **ALLOC** | `(Z=[])[C[8]](69,T,445)` static array initialization |
| 18 | **TABLE** | function table / type table initialization |
| 19 | **EXCEPTION** | `try{for(var RK=x;...` try-catch loop |
| 20 | **DOM** | `Is[d[o]]` DOM operation |
| 21 | **STORE** | safe global/object property access |
| 22 | **STRING** | `new fh(r,v)` string/regex handling |
| 23 | **BRANCH** | `try...catch` safe access + conditional jump |
| 24 | **CALL** | `W(C[2],null,8,z,FL)` multi-argument function call |
| 25 | **EXCEPTION** | `try{...}catch(C){...}` exception capture + jump |

---

## 5. Runtime Capture Approaches

### Option A: Selenium + CDP Native Events (recommended, highest success rate)

```python
from selenium import webdriver

driver = webdriver.Chrome()

# Inject anti-detection
driver.execute_cdp_cmd("Page.addScriptToEvaluateOnNewDocument", {
    "source": r"""
        Object.defineProperty(navigator, 'webdriver', {get: () => false});
        Object.defineProperty(navigator, 'plugins', {get: () => [1,2,3,4,5]});
        Object.defineProperty(navigator, 'languages', {get: () => ['zh-CN','zh','en']});
    """
})

# Send CDP native mouse events
driver.execute_cdp_cmd("Input.dispatchMouseEvent", {
    "type": "mousePressed",
    "x": 549.5, "y": 441.2,
    "button": "left", "buttons": 1,
    "clickCount": 1, "pointerType": "mouse"
})
```

### Option B: Playwright Headless Browser

```javascript
const { chromium } = require('playwright');

async function run() {
    const browser = await chromium.launch();
    const page = await browser.newPage();

    // Intercept network requests
    await page.route('**/api/**', async route => {
        await route.continue_();
    });

    await page.goto('https://target-page.com');

    // Wait for the DSL VM to initialize
    await page.waitForFunction(() => {
        return window.AWSCInner &&
               window.AWSCInner._modules &&
               window.AWSCInner._modules['fy'];
    });

    // Perform actions
    await page.mouse.move(500, 400);
    await page.mouse.down();
    // ... action sequence
    await page.mouse.up();
}
```

### Option C: Pure Protocol Verification (extremely low success rate)

> Tokens generated by the DSL VM are usually strongly bound to the browser context (TLS JA3 fingerprint, IP, cookies, request headers, etc.); once detached from the browser, the server can detect the context mismatch. **A pure protocol approach is not recommended.**

---

## 6. Common Status Codes

| Code | Meaning | Handling |
|------|------|------|
| 0 | **Verification passed** ✅ | Extract sessionId + sig |
| 300 | **Blocked by risk control** | Blocked, cannot pass |
| 8778 | **Verification failed, retry needed** | Retry the operation |
| 8776 | **Too fast, retry needed** | Retry after adding a delay |
| 69634 | **Generic failure** | Check whether the parameters are correct |

---

## 7. Skill Self-Check Checklist

- [ ] Did I complete DSL VM identification (IIFE + single-letter variables + DG() interpreter)?
- [ ] Did I extract the variable mapping table (`var X=number`)?
- [ ] Did I extract and classify the opcode list?
- [ ] Did I analyze the reference range of the constant table C[9]?
- [ ] Did I locate the exported function registration point?
- [ ] When pure static analysis was not enough, did I try the runtime injection approach?
- [ ] Did I write back to the field-journal after finishing the task?
- [ ] Did I discover new tools / new scenarios → update routing.md?

---

## Routing Registration

| Type | Route |
|------|------|
| **Target type**: WASM / DSL VM / custom instruction set | `reverse-engineering/dsl-vm-reverse/SKILL.md` |
| **User intent**: "DSL VM / risk-control engine reverse engineering" | this skill |
| **Toolchain**: Playwright / Selenium CDP | browser injection approach |

### Path Crossings

```
DSL VM reverse engineering path:
  reverse-engineering/dsl-vm-reverse/ → Phase 1-6 workflow
  ↓ if runtime data capture is needed
  browser-automation/ → Playwright/Selenium CDP
  ↓ if the API protocol layer needs analysis
  js-reverse/ → Observe→Capture→Rebuild
```

## Merge Addendum: Controlled Execution Entry

Before starting, confirm the sample is genuinely a custom JS opcode VM or risk-control engine, not standard WASM or an ordinary webpack bundle. For authorized online samples, complete `case-init` first and confirm `scope.md`; for offline samples use the `offline` / `lab` path. Then immediately run the file classification in Phase 1 of section 3, recording the classification rationale, sample hash, and runtime environment — do not stop at the routing or directory-browsing stage.
