---
name: taint-flow-tracer
description: Trace untrusted data from origin to sink across files and layers, including sanitization checkpoints and bypass conditions.
---

# Taint Flow Tracer

## Purpose
Provide deterministic source-to-sink traces for exploitability decisions.

## Inputs
- `code_path`
- `candidate_source`
- `candidate_sink`
- `execution_context`

## Trace Method
### Step 1: Source Definition
1. Define exact taint origin.
2. Classify taint trust level and attacker influence.

### Step 2: Propagation Tracking
1. Follow variable flow through wrappers and helpers.
2. Track type conversion, serialization, and parsing boundaries.
3. Track async/task boundary hops.

### Step 3: Checkpoint Evaluation
1. Record validation and canonicalization points.
2. Determine context fitness of each sanitizer.
3. Identify alternate branches that skip checks.

### Step 4: Sink Reachability
1. Confirm sink invocation path is executable.
2. Confirm attacker control survives to sink-relevant bytes.
3. Note control granularity (full, partial, none).

### Step 5: Verdict
1. `tainted_reachable`
2. `tainted_blocked`
3. `path_unknown`

## Required Trace Fields
- `source`
- `path_nodes`
- `checkpoints`
- `sink`
- `control_level`
- `verdict`

## Output Contract
```json
{
  "trace_id": "",
  "source": {},
  "path_nodes": [],
  "checkpoints": [],
  "sink": {},
  "verdict": ""
}
```

## Failure Modes
- Stopping at static reference without runtime-feasible path.
- Treating generic sanitization as context-safe by default.

## Quality Checklist
- [ ] Cross-file and cross-layer flow included.
- [ ] Checkpoints evaluated for bypassability.
- [ ] Control level at sink is explicit.

## Detailed Operator Notes
### Cross-Layer Trace Requirements
- Include controller, service, data access, and sink layers.
- Include serialization/deserialization boundary handling.
- Include async boundaries (queue/job/event) where data crosses trust zones.

### Access-Control Audit Rules
- Verify policy check location relative to resource fetch.
- Verify policy check occurs on every variant path.
- Verify tenant scoping is enforced at data query layer.

### Sanitization Audit Rules
- Context-match sanitizer to sink type.
- Confirm canonicalization happens before validation.
- Check for alternate branch paths that skip sanitizer.

### Reporting Rules
- Include function-level path with file and symbol names.
- Include bypass narrative for missing or weak control.
- Include a precise fix location and test recommendation.

## Quick Scenarios
### Scenario A: Access Check Placement
- Trace data fetch point.
- Trace policy check point.
- Determine whether check occurs before use.
- Identify alternate path without check.

### Scenario B: Sanitization Mismatch
- Map sink execution context.
- Map sanitizer type and location.
- Validate context compatibility.
- Find branch that bypasses sanitizer.

### Scenario C: Adjacent Pattern Sweep
- Identify sibling handlers/sinks.
- Compare guard and validation parity.
- Flag inconsistent control patterns.
- Prioritize high-impact siblings.

## Conditional Decision Matrix
| Condition | Action | Evidence Requirement |
|---|---|---|
| Source passes through helper wrappers | inline helper logic into trace | wrapper-expanded path |
| Policy check exists after data fetch | test prefetch exposure and side-effects | order-of-operations trace |
| Sanitizer exists but context mismatch | craft context-correct exploit hypothesis | sink-context mismatch proof |
| Async boundary carries tainted data | trace serialization and consumer validation | producer-consumer trace |
| Sibling route has weaker guards | run parity scan across sibling handlers | guard parity matrix |

## Advanced Coverage Extensions
1. Compare DTO/schema validation between create and update paths.
2. Scan migration scripts and admin tasks for latent unsafe operations.
3. Validate cache-layer authorization consistency.
4. Validate feature-flagged code paths for missing controls.
5. Validate error handling paths for secret leakage.
