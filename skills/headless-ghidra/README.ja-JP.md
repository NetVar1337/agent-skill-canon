# Headless Ghidra Orchestrator

`headless-ghidra` は Headless Ghidra skill family の入口 skill です。agent に YAML-first の reverse-engineering pipeline を実行させ、各 phase gate の後でレビューしたい場合に使います。

言語: [English](./README.md) | [简体中文](./README.zh-CN.md)

利用前に、[top-level README](../README.ja-JP.md) から skill family 全体をインストールしてください。

## 役割

- active target を検出または再開する。
- `artifacts/<target-id>/pipeline-state.yaml` を読む。
- 作業を P0-P4 phase skills にルーティングする。
- 次の phase に進む前に gate check を実行する。
- resume/restart、runtime evidence、batch selection、divergence review、completion など、利用者の判断点を明示する。

この skill 自体は Ghidra analysis を実行しません。具体的な作業は phase skills と同梱の `ghidra-agent-cli` が行います。

## 依頼例

```text
Use headless-ghidra to analyze ./sample-target from P0. Stop after each phase
gate and show me the artifacts to review.
```

```text
Resume the existing target and continue to the next valid phase. Show the
current pipeline state first.
```

```text
Run the next P3/P4 iteration for the selected hotpath functions. If metadata is
missing, return to P3 instead of decompiling.
```

## Phase Map

| Phase | Skill README                                                          | Purpose                                                          |
| ----- | --------------------------------------------------------------------- | ---------------------------------------------------------------- |
| P0    | [Intake](../headless-ghidra-intake/README.ja-JP.md)                   | target を確認し、workspace を初期化し、scope を設定する。        |
| P1    | [Baseline](../headless-ghidra-baseline/README.ja-JP.md)               | Ghidra import、baseline artifact export、runtime evidence 記録。 |
| P2    | [Evidence](../headless-ghidra-evidence/README.ja-JP.md)               | third-party code と evidence source を識別する。                 |
| P3    | [Discovery](../headless-ghidra-discovery/README.ja-JP.md)             | names、signatures、types、constants、strings を補強する。        |
| P4    | [Batch Decompile](../headless-ghidra-batch-decompile/README.ja-JP.md) | metadata を apply し、選択関数を decompile する。                |

## Review Points

- P0 後: target identity、binary path、Ghidra discovery、scope。
- P1 後: baseline exports、runtime status、hotpath evidence。
- P2 後: third-party decisions、source evidence、function classifications。
- P3 後: proposed names、signatures、types、Ghidra apply verification。
- P4 後: per-function decompilation capture、substitution record、comparison result。

runtime output は active workspace の `targets/<target-id>/` と `artifacts/<target-id>/` に書き込まれます。
