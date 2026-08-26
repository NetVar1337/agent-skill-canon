# UnknownCheats member-source dossier: `l55legend` and `Spacebd`

Use this as a source map for community-led game/anti-cheat research, not as target-specific authority. It was synthesized from 538 rendered forum-search excerpts on 2026-08-26: 308 for `l55legend` (member `4711467`) and 230 for `Spacebd` (member `740576`). Original evidence and extraction metadata are retained locally at `C:\Users\Admin\research\unknowncheats-member-review-2026-08-26`.

## Evidence rule

Each post is a **sourced claim**. Promote it to a working fact only after a pinned-build static/runtime validation. Never carry forward target offsets, signatures, code, anti-cheat status, ban statements, or release provenance as portable knowledge.

## Contributor map

| Member | Observed focus | Reusable research lesson | Primary routes |
|---|---|---|---|
| `l55legend` | Apex, Rust, Escape from Tarkov, general FPS, anti-cheat-bypass threads | live-service build drift, engine-specific dumps/offsets, cross-tool triage, and self-built targets as RE practice | `offset-dumper`, `game-hacking`, `external-esp`, `reverse-engineering-curriculum` |
| `Spacebd` | Counter-Strike, Source/Source 2, plus some Apex/engine threads | schema-led container recovery, ABI/alignment, pointer depth, relative signatures, and rate-limited updates | `game-internals`, `offset-dumper`, `pattern-scanner`, `assembly-reversal-engineering` |

## General lessons to carry forward

1. **Build contract.** Record module hash, signature/recipe, resolved address, type, and runtime semantic assertion. A readable address is not validated data.
2. **Layout before loops.** Derive entity/container shape from schema or reflection where available; prove stride, alignment, pointer depth, and identity before iteration.
3. **Two independent views.** Confirm important fields with at least two of static code, metadata/schema, and controlled runtime observation.
4. **Separate contracts.** Do not infer gameplay meaning from a variable/list name. Validate entity collection, camera/projection, visibility, and render/update cadence independently.
5. **Controlled practice.** Build a local toy target for ABI, schema, camera, and protocol exercises before carrying the workflow to a versioned game build.

## Source anchors

### `l55legend`

- [Self-built targets for learning](https://www.unknowncheats.me/forum/general-programming-and-reversing/727551-start-reverse-engineering-post4534350.html#post4534350)
- [Version-sensitive signatures and dumping](https://www.unknowncheats.me/forum/apex-legends/678880-pattern-squirrel-scripts-post4294161.html#post4294161)
- [Build-specific offset lead](https://www.unknowncheats.me/forum/new-world/720676-offsets-post4492606.html#post4492606)
- [Cross-tool game-data troubleshooting lead](https://www.unknowncheats.me/forum/apex-legends/633238-crashing-reading-memory-post4055592.html#post4055592)

### `Spacebd`

- [Source 2 chunked entity iteration and schema-derived size](https://www.unknowncheats.me/forum/counter-strike-2-a/746481-swag-entity-list-chunked-iteration-post4644344.html#post4644344)
- [Pointer-alignment correction](https://www.unknowncheats.me/forum/counter-strike-2-a/576077-counter-strike-2-reversal-structs-offsets-post4445303.html#post4445303)
- [Pointer-depth correction](https://www.unknowncheats.me/forum/counterstrike-source/620327-writeprocessmemory-wont-write-offsets-post3971559.html#post3971559)
- [Pattern-relative global recovery lead](https://www.unknowncheats.me/forum/counter-strike-2-a/576077-counter-strike-2-reversal-structs-offsets-post4046566.html#post4046566)
- [Update-rate/performance observation](https://www.unknowncheats.me/forum/counter-strike-2-a/716056-sendclantag-post4471522.html#post4471522)

## Conversion gate

Convert a community thread into a new/updated skill only when it provides a reusable, target-independent workflow that existing skills do not own. The new instruction must state its trigger, evidence required, validation, and failure condition. Keep target-specific values and unverified operational claims in a dated research case, not in the skill catalog.
