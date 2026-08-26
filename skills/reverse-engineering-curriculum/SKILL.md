---
name: reverse-engineering-curriculum
description: "Use when designing or running a from-zero reverse-engineering learning path across C/C++, assembly, debugging, static and dynamic analysis, Windows internals, game-engine architecture, and anti-cheat research. Produces phase gates, deliberate practice, and evidence-backed capstones while routing technical work to existing specialist skills."
version: 1.0.0
license: MIT
metadata:
  package: local-operator-skills
  category: learning
  author: Admin
  related_skills: [teach, systems-language-engineering, assembly-reversal-engineering, reverse-engineering, windows-internals, game-hacking, ags-anti-cheat]
---

# Reverse-Engineering Curriculum

## Overview

Build a reverse engineer by layers: first create and debug native programs, then account for their generated machine code, then recover behavior from unknown binaries, and only then combine Windows, engine, and anti-cheat subsystems into controlled research exercises.

The output is a **competency ledger**, not a reading checklist. A learner advances only when they can reproduce a result, explain its mechanism from evidence, and solve a nearby variant without a walkthrough. The supplied reading map lives in [references/reading-map.md](references/reading-map.md); treat it as a source list, not as a substitute for exercises.

## When to Use

- Planning a self-directed curriculum from C/C++ through native reverse engineering.
- Turning a book/resource list into phases, labs, retrieval prompts, and observable graduation gates.
- Assessing a learner's current level and selecting the next reverse-engineering exercise.
- Building a progression from user-mode RE to Windows internals, game engines, or anti-cheat architecture.

Use `teach` when creating a persistent lesson workspace and individual lessons. Use the relevant specialist skill once the learner begins real technical work; this skill owns sequencing and mastery gates, not the detailed execution of a specific analysis, driver, overlay, or anti-cheat experiment.

## Intake

Establish a baseline before assigning material. Record:

| Dimension | Evidence to collect |
|---|---|
| Native programming | one C/C++ program the learner wrote, its build command, warnings, and a debugger session |
| Algorithms | completed problems plus explanations of chosen complexity and data representation |
| Platform | OS, architecture, compiler, debugger/disassembler availability, and VM/snapshot capability |
| RE experience | one annotated function or binary observation; distinguish reading pseudocode from tracing instructions |
| Objective | malware analysis, software compatibility, game-engine research, defensive anti-cheat work, or general systems understanding |
| Time budget | weekly hours and preferred session length |

Do not infer mastery from titles read, tools installed, or copied code. Assign one short diagnostic per uncertain prerequisite.

**Done when:** the learner has a dated competency ledger with an objective, tooling baseline, current phase, and one concrete gap to close.

## Operating Model

Every study block follows this loop:

1. **Read narrowly.** Take one concept from the current phase's primary material.
2. **Recall unaided.** Explain the concept, sketch its data/control flow, or predict program behavior before opening a tool.
3. **Build or analyze.** Implement a minimal program or inspect a deliberately scoped artifact.
4. **Cross-check.** Compare source ↔ compiler output ↔ debugger/runtime behavior, or compare static evidence ↔ dynamic observation.
5. **Write evidence.** Preserve commands, inputs, build identifiers, address coordinates, screenshots/logs, and a short statement of what changed the learner's model.
6. **Vary one condition.** Change an optimization level, compiler, input, architecture, structure layout, or feature flag and explain the difference.

Keep AI in a coaching role: request a question, a review of an attempted explanation, or a minimal hint. Do the first reconstruction, debugging attempt, and evidence collection before requesting a solution. Never substitute generated code for the learner's own implementation during a competency check.

**Done when:** every completed exercise has an artifact, an explanation, and a variation result—not only a passing build.

## Curriculum Phases

### Phase 0 — C/C++ and computational foundations

**Goal:** reason about memory, object lifetime, data representation, asymptotic cost, and I/O without treating the standard library or a debugger as magic.

**Practice:**
- Write C programs that parse binary/text input, allocate and free dynamic structures, and handle failures.
- Implement arrays, linked lists, hash tables, heaps, trees, sorting, and graph traversal at least once in C; repeat selected work in idiomatic C++ with RAII.
- Solve algorithm problems under a constraint that forces a stated complexity and representation choice.
- Build a small networked or file-format project and debug malformed inputs.

**Gate:** given a crash in a self-written program, the learner identifies the failing invariant, confirms it in a debugger, fixes it, and explains the generated object/data layout. They can implement a basic container and justify its time/space tradeoffs.

**Route technical coding to:** `systems-language-engineering`, `lang-cpp23`, and `systematic-debugging`.

### Phase 1 — Machine model, ABI, and assembly fluency

**Goal:** map ordinary C/C++ constructs to registers, stack frames, calling conventions, relocations, and control flow.

**Practice:**
- Compile the same small functions at `-O0`, `-O2`, and with different compilers; annotate differences.
- Hand-trace loops, switches, pointers, structs, virtual calls, exceptions/error paths, and integer conversions.
- Write small assembly routines that obey the target ABI, then inspect their object-file relocations and unwind behavior.
- Use a debugger to stop before and after calls and reconcile register/stack state with the ABI.

**Gate:** the learner can recover a function's inputs, outputs, clobbers, locals, and call targets from disassembly, correctly label VA/RVA/file offset, and validate one conclusion with a source-to-assembly reproduction or debugger observation.

**Route technical work to:** `assembly-reversal-engineering`, `lang-assembly`, `zydis-disassembly-engineering`, and `windbg-ttd` where appropriate.

### Phase 2 — Static binary analysis

**Goal:** develop an evidence-first static workflow before relying on decompiler output.

**Practice:**
- Triage a benign, self-built native binary: format, architecture, sections, imports, strings, entry points, symbols, and compiler fingerprints.
- Recover one parser, one state machine, and one data structure bottom-up from code and cross-references.
- Maintain a table of module hash, address coordinate, bytes, interpretation, confidence, and validation plan.
- Compare decompiler output against the actual instructions for all important conclusions.

**Gate:** produce an annotated analysis note for an unknown-but-benign binary that identifies one end-to-end input → transform → output path and validates its key semantics using a second method.

**Route technical work to:** `reverse-engineering`, `ida-reverse`, `ghidra-reverse`, `radare2`, `re-source`, and `rev-struct`.

### Phase 3 — Dynamic analysis and hostile-code literacy

**Goal:** observe runtime behavior without confusing a debugger view with ground truth or a malware pattern with an attribution claim.

**Practice:**
- Debug instrumented toy programs with deliberate anti-debug checks, packing-like decode stubs, IPC, and thread races.
- Trace allocation, file, registry, process, and network behavior in a disposable VM.
- Use breakpoints or Frida to capture function arguments, return values, and plaintext at one carefully selected boundary.
- Analyze benign simulations or archived samples under a documented static-first, snapshot-backed lab workflow.

**Gate:** provide a timeline that joins a static code location to a runtime event, shows the exact build/tool versions, distinguishes observed facts from inferred behavior, and includes a negative control.

**Route technical work to:** `malware-analysis`, `frida-dbi`, `frida-instrumentation`, `anti-debugging-techniques`, `memory-forensics`, and `digital-forensics`.

### Phase 4 — Windows internals and kernel foundations

**Goal:** explain Windows as interacting identity, object, memory, loader, I/O, and telemetry systems rather than a list of undocumented offsets.

**Practice:**
- Trace a process creation, handle, token, module-load, file-I/O, and RPC/COM transaction end-to-end.
- Resolve symbols and compare public types, disassembly, and runtime observations on the exact Windows build.
- Build a minimal, safe driver or a user-mode mock that models an IOCTL contract, cancellation, and cleanup.
- Capture an ETW trace with a known sentinel event, then account for provider configuration and event loss.

**Gate:** present a build-pinned subsystem map for one transaction, including trust transitions, object lifetime, cleanup, and the observation that could falsify the conclusion.

**Route technical work to:** `windows-internals`, `windows-symbols-debugging`, `windows-telemetry-etw`, `kernel-dev`, `driver-comm`, and `windbg-ttd`.

### Phase 5 — Engine and protocol literacy

**Goal:** recover data flow in game-like real-time programs while respecting build provenance and server authority.

**Practice:**
- Build a toy client/server simulation with replicated entities, prediction, correction, and a serialised message format.
- Map the toy client's entity, transform, camera, and renderer data; validate each field from both runtime and static evidence.
- Implement WorldToScreen from a known matrix and prove it with synthetic camera/viewport cases.
- Decode a locally captured test protocol and document framing, state, error handling, and replay boundaries.

**Gate:** show one local actor transform flowing through camera projection to a screen coordinate, then distinguish local prediction from server-confirmed state in a packet/runtime timeline.

**Route technical work to:** `game-internals`, `game-hacking`, `external-esp`, `offset-dumper`, `network-protocol-re`, and `protocol-reverse`.

### Phase 6 — Anti-cheat architecture and defensive measurement

**Goal:** analyze the defender's observation and decision pipeline as a layered, falsifiable system.

**Practice:**
- Model a toy anti-cheat as game component, user-mode service, kernel telemetry collector, and backend decision system.
- Inventory observable surfaces such as handle access, image loads, memory mappings, input, ETW, driver trust, hardware, screenshots, and server-side behavior.
- Design clean baseline, positive-control, one-variable, and rollback runs; distinguish telemetry, block, kick, server correction, delayed action, and inconclusive outcomes.
- Implement one defensive detector in a lab and measure its false positives against varied benign behavior.

**Gate:** deliver a version-pinned detection hypothesis with trusted telemetry provenance, a healthy positive control, negative controls, measured outcome classes, and cleanup evidence. A missing event is not a bypass conclusion until sensor health and loss are accounted for.

**Route technical work to:** `ags-anti-cheat`, `anti-cheat-bypass`, `game-hacking`, `windows-telemetry-etw`, `kernel-callbacks`, `vbs-hvci-research`, and `tpm-attestation-research`.

### Phase 7 — Capstone and independent practice

**Goal:** solve a bounded new problem by choosing tools and evidence rather than following a copied recipe.

Choose a legal local artifact or self-built target, pin its build, write an analysis plan, and complete a project such as:

- a file-format parser reverse-engineering report with a compatible reimplementation;
- a self-built client/server protocol recovery and mutational test harness;
- an engine schema/offset recovery pipeline with regression tests;
- a Windows subsystem tracing lab with symbols, ETW, and debugger evidence; or
- a toy anti-cheat telemetry/detector evaluation with an evidence package.

**Gate:** a reviewer can reproduce the environment, follow Evidence → Finding → Path, identify known limitations, and run one independent validation command without the learner present.

## Competency Ledger Template

Maintain one row per competency:

| Date | Phase | Competency | Artifact | Independent validation | Variation/negative control | Status |
|---|---|---|---|---|---|---|
| YYYY-MM-DD | 1 | Win64 ABI call recovery | `labs/abi/notes.md` | debugger register capture | `-O0` vs `-O2` | demonstrated |

Statuses: `introduced`, `attempted`, `demonstrated`, `needs-variation`, or `regressed`. A concept is not durable until it survives a variation after a delay.

## Common Failure Modes

1. **Reading without retrieval.** Convert every chapter into a prediction, implementation, or artifact analysis before continuing.
2. **Tool-first reversal.** Start with file identity, address coordinates, and hypotheses; tools test models rather than provide them automatically.
3. **Decompiler worship.** Treat pseudocode as a hypothesis and check ABI, instructions, references, and runtime behavior.
4. **Offset collecting.** Keep build hash, source, signature/recipe, semantic assertions, and a regression result for each recovered field.
5. **Skipping systems fundamentals.** A learner who cannot explain pointers, object lifetime, ABIs, or process boundaries will stall on advanced tooling; return to the first failed gate.
6. **Copying complete solutions.** Use partial hints after an attempt; require an independently written variation before marking a skill demonstrated.
7. **Confusing a detection observation with an enforcement result.** Preserve baselines, controls, telemetry health, and backend outcome separately.

## Verification Checklist

- [ ] Objective, current baseline, tools, weekly budget, and next gap are recorded.
- [ ] Each phase has a concrete artifact and evidence-backed gate.
- [ ] Completed work includes recall, implementation/analysis, independent validation, and one variation.
- [ ] Address, offset, schema, and build claims are pinned to a specific artifact/build.
- [ ] Learner-produced explanations distinguish observed facts, inference, and unresolved questions.
- [ ] Specialist skills are selected only after the relevant curriculum phase exposes a concrete task.
- [ ] The capstone is reproducible by another reviewer from its evidence package.
