# Reading Map

This is the resource map supplied by the operator. It is a planning aid, not a claim that the books were ingested in full, and it does not replace implementation, analysis, or verification work.

## Phase 0: Engineering foundations

| Subject | Primary | Alternative / practice |
|---|---|---|
| C | *The C Programming Language*, 2nd ed. — Kernighan & Ritchie | Write parsers, data structures, and memory-safe cleanup paths. |
| C++ | *A Tour of C++*, 3rd ed. — Bjarne Stroustrup | Rebuild selected C exercises with RAII and standard containers after first principles are understood. |
| Computer organisation | *Structured Computer Organization* — Andrew Tanenbaum | Compile and disassemble small programs; inspect data representation and calling conventions. |
| Operating systems | *Modern Operating Systems*, 4th ed. — Andrew Tanenbaum | *Operating Systems: Three Easy Pieces*; trace local processes, threads, files, and virtual memory. |
| Algorithms | *Introduction to Algorithms*, 4th ed. — Cormen et al. | *Algorithms*, 4th ed.; CSES and NeetCode exercises. |
| Networking | *Computer Networks*, 5th ed. — Andrew Tanenbaum | *Computer Networking: A Top-Down Approach*; build a small socket protocol. |

## Phase 1–3: Native RE and runtime observation

| Subject | Primary | Deliberate practice |
|---|---|---|
| x64 assembly | *The Art of 64-Bit Assembly* (2021) — Randall Hyde | Compile, disassemble, hand-trace, and write ABI-correct routines. |
| RE introduction | *Reversing: Secrets of Reverse Engineering* (2005) — Eldad Eilam | Triage and annotate self-built or benign binaries. |
| IDA | *The IDA Pro Book*, 2nd ed. — Chris Eagle | Reconstruct one input-to-output path; validate with a second method. |
| Malware-analysis concepts | *Practical Malware Analysis* (2012) | *Mastering Malware Analysis* (2019); use disposable, snapshot-backed labs. |
| Memory forensics | *The Art of Memory Forensics* (2014) | Capture and compare controlled memory artifacts. |

## Phase 4: Windows kernel and defensive telemetry

| Subject | Primary | Deliberate practice |
|---|---|---|
| Windows internals | *Windows Internals*, 7th ed., Parts 1 and 2 | Use as a reference while tracing real transactions on the exact OS build. |
| Windows kernel development | *Windows Kernel Programming* (2023) — Pavel Yosifovich | Build small, safe drivers; debug cleanup and IOCTL contracts. |
| Rootkits / platform trust | *The Rootkit Arsenal*, 2nd ed. (2012); *Rootkits and Bootkits* (2019) | Model protection and detection boundaries in a local lab. |
| EDR detection | *Evasive Malware* (2024) — Kyle Cucci; *Evading EDR* (2023) — Matt Hand | Measure a defender pipeline with clean baselines, controls, and rollback. |

## Phase 5–6: Engines, protocols, and anti-cheat systems

| Subject | Primary | Deliberate practice |
|---|---|---|
| Game-engine architecture | *Game Engine Architecture* (2018) — Jason Gregory | Build or inspect a toy entity/camera/render pipeline. |
| Game-hacking concepts | *Game Hacking* (2016) — Nick Cano | Use only as historical orientation; validate all engine and build details independently. |
| Protocol analysis | *Attacking Network Protocols* (2017) — James Forshaw | Recover a local test protocol, framing, state transitions, and error behavior. |
| Anti-cheat / EDR overlap | The defensive systems resources above | Build a toy telemetry collector and test detector quality rather than memorising “bypasses.” |

## Reference vs. linear reading

Read C/C++, assembly, algorithms, and basic OS material progressively with exercises. Use Windows Internals, rootkit, engine, and anti-cheat books as targeted references once a concrete lab exposes the relevant subsystem. Record the edition/section actually used alongside the exercise it informed.

## Suggested practice inventory

- CSES problem set and NeetCode/NeetCode 250, with written complexity and representation rationale.
- Self-built C/C++ programs: binary parser, allocator/data-structure exercise, socket chat, or small game loop.
- Compiler/debugger/disassembler triangle: source → emitted code → runtime state.
- One benign static-analysis artifact, one controlled dynamic-analysis artifact, and one build-pinned Windows transaction trace.
- One toy client/server engine or protocol, followed by a defensive telemetry/detection capstone.
