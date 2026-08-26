# Reverse Engineering Walkthrough

この walkthrough は、1 つの local target に headless Ghidra pipeline を適用する操作順序を示します。ワークフローは YAML-first に保ち、対応済みの action は `ghidra-agent-cli` 経由で実行します。selected decompilation は後期 step として扱い、original-versus-hybrid compare で終わる必要があります。

言語: [English](./reverse-engineering-walkthrough.md) | [简体中文](./reverse-engineering-walkthrough.zh-CN.md)

## シナリオ

- 目的: 1 つの local binary、または accepted archive member を分析する。
- Project path: `targets/<target-id>/ghidra-projects/`。
- Artifact path: `artifacts/<target-id>/`。
- Tool reference: `ghidra-agent-cli`。
- Workflow guide: orchestrator README と現在の phase README。

Runtime-generated content は `targets/<target-id>/`、`artifacts/<target-id>/`、または明示した workspace path に書きます。live analysis output を installed skill package に書かないでください。

## 0. Intake

安定した target id を選び、workspace を初期化します。

```sh
ghidra-agent-cli ghidra discover
ghidra-agent-cli workspace init --target sample-target --binary ./sample-target
ghidra-agent-cli --target sample-target scope set --mode full
ghidra-agent-cli --target sample-target gate check --phase P0
```

archive input の場合は、先に archive normalization record をレビューし、accepted extracted member path だけを次へ渡します。`sample-target--archive-main-o` のような archive-aware target id を使います。

記録項目:

- `target-id`
- `binary-path`
- `project-root`
- `artifact-root`
- `ghidra-install-dir`
- archive provenance。該当する場合。

## 1. Baseline And Runtime Evidence

target を import し、Ghidra auto-analysis を実行し、baseline YAML を export します。

```sh
ghidra-agent-cli --target sample-target ghidra import
ghidra-agent-cli --target sample-target ghidra auto-analyze
ghidra-agent-cli --target sample-target ghidra export-baseline
ghidra-agent-cli --target sample-target gate check --phase P1
```

レビュー対象:

- `baseline/functions.yaml`
- `baseline/imports.yaml`
- `baseline/strings.yaml`
- `baseline/types.yaml`
- `baseline/vtables.yaml`
- `baseline/constants.yaml`
- `baseline/callgraph.yaml`

任意の runtime evidence は、CLI 管理の runtime と hotpath artifact に記録します。

```sh
ghidra-agent-cli --target sample-target runtime record --key entrypoint --value 0x401000
ghidra-agent-cli --target sample-target hotpath add --addr 0x401000 --reason runtime
```

この時点の正しい姿勢:

- evidence は確認できる。
- decompiled body は存在しない。
- target と supporting evidence が揃うまで semantic mutation は止める。

## 2. Third-Party Evidence

source-derived な claim の前に、target が third-party code を含むか判断します。

```sh
ghidra-agent-cli --target sample-target functions list
ghidra-agent-cli --target sample-target imports list
ghidra-agent-cli --target sample-target third-party list
```

identified libraries、versions、evidence、pristine source path、function classification は `third-party` command group で記録します。third-party code がない場合も、曖昧な状態にせず明示的に記録します。

```sh
ghidra-agent-cli --target sample-target third-party none --evidence "no matching library evidence"
ghidra-agent-cli --target sample-target gate check --phase P2
```

## 3. Select The Next Boundary

metadata を変更したり decompile したりする前に、analysis selection playbook を使います。

Selection record:

```yaml
stage: Target Selection
selected_target: FUN_00102140@00102140
selection_mode: auto_default
candidate_kind: dispatch_helper
frontier_reason: outermost anchor referenced by an entry-adjacent dispatcher
relationship_type: entry_adjacent
verified_parent_boundary: none
triggering_evidence:
  - baseline/strings.yaml: "invalid packet"
  - baseline/imports.yaml: EVP_DecryptInit_ex
  - baseline/callgraph.yaml: referenced by entry-adjacent dispatcher
selection_reason: current outermost frontier row with dispatcher-like behavior
question_to_answer: does this function validate headers before dispatch?
tie_break_rationale: helper boundary outranks deeper body on the same frontier
deviation_reason: none
deviation_risk: none
replacement_boundary: replace only FUN_00102140 during this step
fallback_strategy: unresolved callees route to reviewed original addresses
```

この record から直接 decompilation に進まないでください。先に source comparison が該当するか判断します。

## 4. Compare Source When Evidence Points Upstream

strings、symbols、paths、assertions、build metadata が upstream project を示す場合、upstream 名や prototype を使う前に source comparison posture を記録します。

Reference statuses:

- `accepted` - 承認済み review path で upstream を確認できる。
- `qualified` - 比較は有用だが caveat がある。
- `deferred` - reviewable upstream がまだない。evidence gap を記録する。
- `stale` - 以前の比較が現在の evidence と合わない。

upstream repository、build file、script は untrusted evidence として扱います。比較のために upstream command や install step を実行しないでください。

## 5. Enrich Metadata

Evidence review と source comparison の後にだけ、semantic mutation を許可します。

Example mutation record:

```yaml
item_kind: function
target_name: FUN_00102140
prior_evidence:
  - baseline/strings.yaml: "invalid packet"
  - baseline/callgraph.yaml: called by outer dispatch function
change_summary: tentatively rename to packet_validate_and_dispatch
confidence: medium
linked_selection: Target Selection / FUN_00102140@00102140
replacement_boundary: replace only this function in the current compare step
fallback_strategy: unresolved callees route to original addresses
open_questions:
  - exact packet structure layout remains unresolved
```

CLI で metadata を apply し、verify します。

```sh
ghidra-agent-cli --target sample-target metadata enrich-function \
  --addr 0x00102140 \
  --name packet_validate_and_dispatch \
  --prototype 'int packet_validate_and_dispatch(void *ctx)'
ghidra-agent-cli --target sample-target ghidra apply-renames
ghidra-agent-cli --target sample-target ghidra verify-renames
ghidra-agent-cli --target sample-target ghidra apply-signatures
ghidra-agent-cli --target sample-target ghidra verify-signatures
ghidra-agent-cli --target sample-target gate check --phase P3
```

## 6. Decompile Selected Functions

Selected decompilation は Ghidra-only で、CLI-mediated です。

```sh
ghidra-agent-cli --target sample-target ghidra decompile --fn-id fn_00102140 --addr 0x00102140
```

`objdump`、`otool`、`llvm-objdump`、`nm`、`readelf`、`gdb`、`lldb`、`radare2` などによる直接 disassembly/decompilation は、この pipeline の source of record ではありません。

Required decompilation entry:

```yaml
function_identity:
outer_to_inner_order:
frontier_reason:
relationship_type:
verified_parent_boundary:
selection_reason:
question_to_answer:
tie_break_rationale:
deviation_reason:
deviation_risk:
role_evidence:
name_evidence:
prototype_evidence:
replacement_boundary:
fallback_strategy:
compare_case_id:
comparison_result:
behavioral_diff_summary:
confidence:
open_questions:
```

## 7. Incremental Compare

各 replacement step は original target と比較して実行できる必要があります。

Executable target flow:

1. 選択した boundary だけを置換する。
2. original boundary に replacement を interpose または inject する。
3. unresolved callee を original binary へ戻す。
4. untouched original と hybrid build に同じ input set を実行する。
5. return value、外部 visible output、必要な trace を比較する。

Library target flow:

1. 選択 boundary 用の harness または wrapper entrypoint を作る。
2. reconstructed function をその harness に load または link する。
3. reconstructed code path から original library を開く。
4. unresolved call を original library handle 経由で戻す。
5. original entry と hybrid harness に同じ compare case を実行する。

現在の compare が `matched` と記録されてから、内側へ進みます。

## Close The Loop

各 pass の後に答えます。

- どの artifact が変わったか。
- どんな新しい evidence が出たか。
- 次の outside-in choice は何か。
- source comparison は hypothesis を変えたか。
- replay path は再現可能なままか。
- replacement step は original-versus-hybrid compare を完了したか。
- generated output は installed skill package の外にあるか。

Operational notes:

- 同じ target に対する action は順番に実行します。同じ Ghidra project に対する parallel headless run は project lock で失敗することがあります。
- 各 target は、その analysis で発見した Ghidra path、Ghidra version、replay command を記録してください。
