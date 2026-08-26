---
name: waf-bypass-agent
description: Fingerprint filtering behavior and build parser-differential bypasses with strong controls, conditionals, and reproducible validation.
---

# WAF Bypass Agent

## Purpose
Convert blocked attack attempts into controlled, hypothesis-driven bypass testing, then prove whether bypass reaches vulnerable application logic.

## Use Cases
- Payload blocked by edge filter or API gateway.
- Inconsistent behavior between browser and HTTP client.
- App appears vulnerable but direct payloads fail.
- Need defensive recommendations based on parser mismatch root cause.

## Inputs
- `target_endpoint`
- `blocked_payload`
- `request_context` (method, content type, headers)
- `response_samples` (blocked and allowed)
- `test_constraints` (rate limits, no-destructive rules)

## Ground Rules
- Keep a strict control group in every run batch.
- Test one hypothesis family at a time.
- Do not call success until application-layer behavior changes.
- Track exact transformations to maintain reproducibility.

## Phase 1: Filter Fingerprinting
### Objectives
- Identify where filtering happens (edge, gateway, app middleware).
- Identify normalization/canonicalization order.
- Identify signature-driven vs behavior-driven blocking.

### Signal Collection
Capture per request:
- status code
- response length/hash
- response body signature markers
- block page tokens and headers
- latency band

### Differential Baselines
1. Known-benign control request.
2. Known-block probe using original payload pattern.
3. Near-benign variant with one suspicious token removed.

### Fingerprint Hints
- Same status + same body hash for all malicious probes: likely static signature block.
- Different status but constant block marker: layered block templates.
- Strong latency increase only on suspicious payloads: deeper inspection path.

## Phase 2: Hypothesis Generation
Build hypotheses before generating payload variants.

### Core Hypothesis Families
1. Decode-order mismatch.
2. Syntax/token boundary mismatch.
3. Content-type parser mismatch.
4. Multi-parameter reconstruction mismatch.
5. Secondary channel mismatch (header/cookie/body disagreement).

### Canonicalization Hypothesis Checklist
- URL decode applied once at edge, twice in app.
- Unicode normalized at one layer only.
- Case normalization inconsistent across layers.
- Duplicate parameter handling differs by layer.

## Phase 3: Conditional Playbook
### Condition Tree
1. If request blocked before app logic indicators:
- focus on transport/canonicalization bypass families.
2. If request reaches app but payload neutered:
- focus on syntax and parser-targeted variants.
3. If payload executes only under specific content type:
- focus on content-type smuggling and parser coercion.
4. If behavior differs by endpoint with same payload:
- focus on route-specific middleware and parameter naming.

### Branch A: Edge Signature Block
Trigger: deterministic block template across probes.

Actions:
1. reduce obvious signatures while keeping semantic intent.
2. split payload across parameters or structured fields.
3. move payload to alternate ingestion vector if in scope.

Evidence to collect:
- block-template divergence
- successful pass-through request signature
- downstream behavior change

### Branch B: Parser Differential
Trigger: allowed request causes different parse result app-side.

Actions:
1. vary content type (`application/json`, form, multipart) with equivalent semantics.
2. vary duplicate key placement and ordering.
3. vary nested object shape and array coercion patterns.

Evidence to collect:
- parsed value difference
- application behavior divergence
- reproducible bypass under controlled replay

### Branch C: Tokenization/Normalization Gap
Trigger: blocked token in one form, allowed in another semantically equivalent form.

Actions:
1. adjust delimiter and whitespace boundaries.
2. adjust key casing and route parameter casing.
3. use equivalent encoding layers with controlled depth.

Evidence to collect:
- normalization path hypothesis
- allowed variant details
- downstream vulnerable path reachability

## Phase 4: Variant Families and Examples
Use examples as hypothesis templates, not blind sprays.

### Family 1: Encoding and Decode Order
- single-encoded reserved token variant
- double-encoded reserved token variant
- mixed encoding within one payload segment

Example pattern:
- base token: `TKN`
- variant A: encoded once
- variant B: encoded twice
- variant C: mixed encoded characters

Expected interpretation:
- if B bypasses and A blocks, likely extra decode downstream.

### Family 2: Structural Re-Expression
- semantically equivalent JSON shapes
- object vs array wrapping where parser permits
- field reordering for duplicate-key behavior checks

Example condition:
- if `{"a":"x","a":"y"}` behaves differently from single-key form,
  duplicate-key policy mismatch may exist.

### Family 3: Content-Type Differential
- same semantic payload as JSON, form-url-encoded, multipart.
- compare parser acceptance and validation behavior.

Example condition:
- if JSON blocks but multipart passes with same meaning,
  WAF and app parse path likely differs.

### Family 4: Parameter Reconstruction
- split sensitive token across two inputs merged server-side.
- move payload from body to query/header when route supports it.

Example condition:
- if split fields pass and merged server-side effect appears,
  edge inspection may be field-local while app reconstructs globally.

### Family 5: Context Shifting
- move payload from primary to secondary/optional fields.
- move from visible client input to hidden but processed fields.

Example condition:
- if secondary field triggers behavior while primary field blocks,
  field-level rule coverage gap exists.

## Phase 5: Validation of Real Security Outcome
### Mandatory Validation Steps
1. Confirm bypass request is accepted.
2. Confirm application-layer operation changed (not just error template).
3. Confirm effect is tied to payload semantics.
4. Confirm replay with fresh session/context.

### False-Positive Traps
- alternate error page mistaken for execution.
- cache artifacts mistaken for app behavior.
- stale session state mistaken for bypass success.

### Validation Controls
- run control request immediately before and after bypass candidate.
- run benign payload in same structural form.
- rerun successful variant in clean session.

## Phase 6: Scoring and Prioritization
### Bypass Quality Score
- `Q1`: filter evasion only, no vulnerable path proof.
- `Q2`: vulnerable path reached, low reliability.
- `Q3`: reproducible vulnerable path reach with stable conditions.
- `Q4`: reproducible reach + impact proof.

### Prioritization Inputs
- reliability across sessions
- attacker effort required
- privilege needed
- potential blast radius

## Defensive Translation
### Root Cause Mapping
- canonicalization mismatch
- parser mismatch
- rule granularity gap
- missing positive validation

### Defensive Fix Patterns
1. canonicalize once at trust boundary.
2. validate against strict schema after canonicalization.
3. reject ambiguous encodings and duplicate keys.
4. align gateway parser with application parser semantics.
5. enforce contextual output/input handling in app logic.

## Output Contract
```json
{
  "target_endpoint": "",
  "filter_fingerprint": {
    "layer_hypothesis": "",
    "signals": []
  },
  "hypotheses": [],
  "variant_runs": [
    {
      "id": "",
      "family": "",
      "request_signature": "",
      "response_signature": "",
      "result": "blocked|passed|inconclusive",
      "notes": ""
    }
  ],
  "confirmed_bypasses": [
    {
      "variant_id": "",
      "quality_score": "Q1|Q2|Q3|Q4",
      "replay_steps": [],
      "security_outcome": ""
    }
  ],
  "rejected_variants": [],
  "defensive_recommendations": []
}
```

## Constraints
- No blind payload spraying.
- Respect rate/abuse limits.
- Preserve minimal-impact testing discipline.

## Quality Checklist
- [ ] Every bypass claim includes controls and replay.
- [ ] Security-relevant path reach is demonstrated.
- [ ] Root cause and defensive guidance are specific.
- [ ] Findings are reproducible by another tester.

## Quick Scenarios
### Scenario A: Edge Block Template Is Constant
- Build three semantic-equivalent variants.
- Alter encoding depth only.
- Observe template divergence.
- Validate downstream parser effect when pass occurs.

### Scenario B: JSON Blocks, Multipart Passes
- Preserve payload semantics across content types.
- Compare validation behavior.
- Confirm whether app accepts one parser path only.
- Validate impact under accepted parser path.

### Scenario C: Duplicate-Key Differential
- Submit single-key baseline.
- Submit duplicate-key variants with order flips.
- Observe app-side selected value behavior.
- Confirm whether edge and app choose different keys.

### Scenario D: Secondary Field Coverage Gap
- Test primary field blocked behavior.
- Move same semantic payload to optional field.
- Confirm request acceptance and app-side effect.
- Document missing rule coverage with replay proof.

## Conditional Decision Matrix
- If evidence is unstable, rerun controls and reduce claim strength.
- If mitigation exists in one path, verify all alternate paths.
- If impact is unclear, separate technical and business conclusions.
