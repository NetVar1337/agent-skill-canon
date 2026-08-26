# Headless Ghidra Baseline And Runtime — P1

P1 は target を Ghidra に import し、auto-analysis を実行し、baseline YAML を export し、後続 phase が優先順位付けと比較に使う runtime evidence を記録します。P1 は evidence を作る phase であり、semantic reconstruction は行いません。

言語: [English](./README.md) | [简体中文](./README.zh-CN.md)

## パイプライン内の位置

```text
P0 Intake -> P1 Baseline+Runtime -> P2 Third-Party -> [P3 Metadata -> P4 Substitution]*
```

P1 は Ghidra analysis を初めて実行する phase です。ただし substitution 用の function body decompilation、recovered name/signature apply、raw evidence を超えた third-party claim は行いません。

## 使用する場面

この skill は次の場合に使います。

- P0 が target workspace と scope を作成済み。
- functions、callgraph、types、vtables、constants、strings、imports の baseline export が必要。
- runtime run records、fixtures、hotpath evidence を記録したい。
- P1 gate material を検証したい。

source comparison、semantic naming、selected decompilation、substitution には使いません。

## フェーズ境界

- Ghidra operation は `ghidra-agent-cli ghidra ...` 経由で行います。
- Runtime と hotpath record は `runtime`、`hotpath`、または対応済み `frida` command で記録します。
- Java と shell backend script は CLI 背後の実装詳細です。
- P1 は `metadata/`、`third-party/`、`substitution/` を変更しません。

## 入力

- `artifacts/<target-id>/pipeline-state.yaml`
- `artifacts/<target-id>/scope.yaml`
- `targets/<target-id>/ghidra-projects/`
- P0 が記録した binary path
- 任意の runtime または harness instruction

## 出力

- `artifacts/<target-id>/baseline/functions.yaml`
- `artifacts/<target-id>/baseline/callgraph.yaml`
- `artifacts/<target-id>/baseline/types.yaml`
- `artifacts/<target-id>/baseline/vtables.yaml`
- `artifacts/<target-id>/baseline/constants.yaml`
- `artifacts/<target-id>/baseline/strings.yaml`
- `artifacts/<target-id>/baseline/imports.yaml`
- `artifacts/<target-id>/runtime/run-manifest.yaml`
- `artifacts/<target-id>/runtime/run-records/*.yaml`
- `artifacts/<target-id>/runtime/fixtures/**`
- `artifacts/<target-id>/runtime/hotpaths/call-chain.yaml`
- harness が必要な場合の `artifacts/<target-id>/runtime/project/**`

## Skill が使う command

これらは skill が呼び出す可能性がある CLI 例です。通常の利用では agent に phase の実行を依頼し、手作業で実行するのは troubleshooting 時だけにしてください。

```sh
ghidra-agent-cli --target sample-target ghidra import
ghidra-agent-cli --target sample-target ghidra auto-analyze
ghidra-agent-cli --target sample-target ghidra export-baseline
ghidra-agent-cli --target sample-target runtime record --key entrypoint --value 0x401000
ghidra-agent-cli --target sample-target hotpath add --addr 0x401000 --reason runtime
ghidra-agent-cli --target sample-target runtime validate
ghidra-agent-cli --target sample-target hotpath validate
ghidra-agent-cli --target sample-target gate check --phase P1
```

任意の Frida command:

```sh
ghidra-agent-cli frida device-list
ghidra-agent-cli frida device-attach --pid 1234
ghidra-agent-cli frida io-capture --target ./sample-target --timeout 60
ghidra-agent-cli frida trace --target ./sample-target --functions open,read
```

## Phase Flow

1. P0 gate が通っていることを確認する。
2. target を Ghidra project に import する。
3. Ghidra auto-analysis を実行する。
4. baseline YAML を export する。
5. functions、imports、strings、types、vtables、constants、callgraph edge の coverage をレビューする。
6. runtime availability または unavailability を記録する。
7. runtime observation がある場合は hotpath evidence を追加する。
8. runtime と hotpath artifact を validate する。
9. P1 gate を実行する。

## Runtime 指針

Executable target では具体的な invocation、input fixtures、observed output を記録します。Library target では harness expectation、または runtime execution を deferred する理由を記録します。Frida が使えない場合は、その事実を明示し、runtime status を暗黙にしないでください。

## 完了条件

- すべての baseline YAML file が存在し、読める。
- Runtime status が明示され、再現可能。
- Hotpath evidence が存在する、または不在理由が説明されている。
- Decompiled function body や P4 substitution artifact が作られていない。
- P1 gate が通る。

## ブロッカー

次の場合は P2 に進みません。

- Ghidra import または auto-analysis が失敗した。
- Baseline YAML が missing または unreadable。
- Runtime requirement が不明。
- Hotpath record が baseline address space と矛盾する。
- P1 gate が baseline/runtime artifact の不足または不完全さを報告している。

## P2 へのハンドオフ

P1 が通ったら `headless-ghidra-evidence` に進みます。handoff には baseline artifact path、runtime status、hotpath summary、P2 が third-party code を識別する際に考慮すべき gap を含めます。
