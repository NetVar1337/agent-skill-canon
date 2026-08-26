# Headless Ghidra Intake — P0

P0 は、ユーザーが指定した binary または受け入れ済み archive member を、有効な `ghidra-agent-cli` target に変換します。Ghidra analysis を実行する前に、target identity、local tooling、workspace、analysis scope を確定します。

言語: [English](./README.md) | [简体中文](./README.zh-CN.md)

## パイプライン内の位置

```text
P0 Intake -> P1 Baseline+Runtime -> P2 Third-Party -> [P3 Metadata -> P4 Substitution]*
```

P0 は準備 phase です。Ghidra analysis の import、baseline metadata export、name/signature apply、function decompilation は行いません。

## 使用する場面

この skill は次の場合に使います。

- 新しい binary、library、object file、accepted archive member に workspace が必要。
- 既存 run の target identity、binary path、scope を確認したい。
- Ghidra discovery または binary inspection が未記録。
- P1 の前に scope を設定する必要がある。

baseline export、third-party classification、metadata recovery、function substitution には使いません。

## フェーズ境界

- 通常の phase review ではこの README を使います。
- troubleshooting で正確な agent workflow rule が必要な場合は [headless-ghidra/SKILL.md](../headless-ghidra/SKILL.md) を参照します。
- troubleshooting で正確な helper command detail が必要な場合は [ghidra-agent-cli/SKILL.md](../ghidra-agent-cli/SKILL.md) を参照します。
- State owner: `artifacts/<target-id>/pipeline-state.yaml` と `artifacts/<target-id>/scope.yaml`。
- Analysis backend: この phase では使用しません。

intake に必要な能力が CLI にない場合は、helper script を追加する前に明示的に判断します。P0 で新しい Ghidra script を作成または実行しないでください。

## 入力

- ユーザー指定の binary または archive path。
- Workspace root。
- 任意の preferred target id。
- Ghidra discovery に必要な local environment。
- whole-target、address、symbol、function scope に関する user guidance。

archive input の場合は、accepted extracted member path だけを引き継ぎ、archive provenance を target id または intake record に残します。

## 出力

- `targets/<target-id>/ghidra-projects/`
- `artifacts/<target-id>/pipeline-state.yaml`
- `artifacts/<target-id>/scope.yaml`
- 任意の `artifacts/<target-id>/intake/` phase record

## Skill が使う command

これらは skill が呼び出す可能性がある CLI 例です。通常の利用では agent に phase の実行を依頼し、手作業で実行するのは troubleshooting 時だけにしてください。

```sh
ghidra-agent-cli ghidra discover
ghidra-agent-cli inspect binary --target ./sample-target
ghidra-agent-cli workspace init --target sample-target --binary ./sample-target
ghidra-agent-cli --target sample-target scope show
ghidra-agent-cli --target sample-target scope set --mode full
ghidra-agent-cli --target sample-target gate check --phase P0
```

追加 scope command:

```sh
ghidra-agent-cli --target sample-target scope add-entry --entry 0x401000
ghidra-agent-cli --target sample-target scope remove-entry --entry 0x401000
```

## Phase Flow

1. 安定した target id を選ぶ。
2. `ghidra discover` を実行し、発見した install path を記録する。
3. 実際に分析する input に `inspect binary` を実行する。
4. `workspace init` で workspace を初期化する。
5. scope を whole target または explicit entries に設定する。
6. `pipeline-state.yaml` と `scope.yaml` をレビューする。
7. `gate check --phase P0` を実行する。

## Scope 指針

target 全体を分析する場合は `--mode full` を使います。既知の address、function、symbol、archive member に意図的に限定する場合は explicit entries を使います。Scope は YAML だけで理解できる必要があり、会話だけの理由に依存しないでください。

## 完了条件

- target workspace が存在し、`--target <id>` で参照できる。
- `pipeline-state.yaml` が選択 binary path を記録している。
- `scope.yaml` が whole-target scope または explicit entries を記録している。
- Ghidra discovery が実行、レビュー済み。
- Binary inspection が実行、レビュー済み。
- P0 gate が通る。

## ブロッカー

次の場合は P1 に進みません。

- binary path が曖昧、または存在しない。
- Ghidra を発見できない。
- Scope が空、または user request と矛盾している。
- Archive normalization が accepted member を生成していない。
- P0 gate が intake artifact の不足または不完全さを報告している。

## P1 へのハンドオフ

P0 が通ったら `headless-ghidra-baseline` に進みます。P1 handoff には target id、binary path、scope summary、Ghidra discovery result、該当する archive provenance を含めます。
