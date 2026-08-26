# Headless Ghidra Metadata Enrichment — P3

P3 は baseline、runtime、third-party evidence を enriched metadata に変換します。対象は function names、signatures、types、constants、strings、選択した hotpath annotations です。Selected decompilation の準備をしますが、P3 自身は function を decompile しません。

言語: [English](./README.md) | [简体中文](./README.zh-CN.md)

## パイプライン内の位置

```text
P0 Intake -> P1 Baseline+Runtime -> P2 Third-Party -> [P3 Metadata -> P4 Substitution]*
```

P3 は iterative recovery loop の metadata half です。P4 batch が missing name、signature、type information を示した場合、workflow は P3 に戻ります。

## 使用する場面

この skill は次の場合に使います。

- P2 が third-party status と function classification を記録済み。
- hotpath function に P4 前の recovered name/signature が必要。
- Ghidra に CLI-mediated rename/signature apply が必要。
- selected decompilation 前に metadata YAML を validate したい。

function body decompilation や substitution record には使いません。

## フェーズ境界

- Metadata write は `ghidra-agent-cli metadata ...` で行います。
- Ghidra project mutation は CLI lock 下の serialized `ghidra-agent-cli ghidra ...` command で行います。
- candidate YAML を作る analysis は並列化できますが、Ghidra apply/verify operation は serialized です。
- P3 は過去の P4 per-function output を書き換えません。

## 入力

- `artifacts/<target-id>/pipeline-state.yaml`
- `artifacts/<target-id>/runtime/hotpaths/call-chain.yaml`
- `artifacts/<target-id>/third-party/identified.yaml`
- `artifacts/<target-id>/baseline/functions.yaml`
- `artifacts/<target-id>/baseline/callgraph.yaml`
- `artifacts/<target-id>/baseline/types.yaml`

## 出力

- `artifacts/<target-id>/metadata/renames.yaml`
- `artifacts/<target-id>/metadata/signatures.yaml`
- `artifacts/<target-id>/metadata/types.yaml`
- `artifacts/<target-id>/metadata/constants.yaml`
- `artifacts/<target-id>/metadata/strings.yaml`
- `artifacts/<target-id>/metadata/apply-records/`

## Skill が使う command

これらは skill が呼び出す可能性がある CLI 例です。通常の利用では agent に phase の実行を依頼し、手作業で実行するのは troubleshooting 時だけにしてください。

```sh
ghidra-agent-cli --target sample-target workspace state show
ghidra-agent-cli --target sample-target functions list
ghidra-agent-cli --target sample-target callgraph callers --addr 0x401000
ghidra-agent-cli --target sample-target callgraph callees --addr 0x401000
ghidra-agent-cli --target sample-target metadata enrich-function \
  --addr 0x401000 \
  --name packet_validate_and_dispatch \
  --prototype 'int packet_validate_and_dispatch(void *ctx)'
ghidra-agent-cli --target sample-target metadata validate
ghidra-agent-cli --target sample-target hotpath validate
ghidra-agent-cli --target sample-target ghidra apply-renames
ghidra-agent-cli --target sample-target ghidra verify-renames
ghidra-agent-cli --target sample-target ghidra apply-signatures
ghidra-agent-cli --target sample-target ghidra verify-signatures
ghidra-agent-cli --target sample-target gate check --phase P3
```

## Phase Flow

1. P2 gate が通っていることを確認する。
2. hotpath priority と callgraph context をレビューする。
3. 各 selected function について role、name、prototype evidence を記録する。
4. `metadata enrich-function` で metadata を書く。
5. metadata と hotpath record を validate する。
6. CLI で renames と signatures を Ghidra に apply する。
7. apply 済み renames と signatures を verify する。
8. P3 gate を実行する。

## Evidence standards

recovered name や prototype は、third-party source comparison、strings、imports、type usage、callgraph position、runtime observations、matched boundaries などの evidence に戻れる必要があります。evidence が弱い場合は name を provisional にし、その function を P4 に送らないでください。

## 完了条件

- P4 に選ばれた hotpath function に明示的な name と signature がある。
- Metadata YAML が validate される。
- Ghidra rename/signature apply operation に verification record がある。
- enrichment が baseline、runtime、third-party evidence から再現可能。
- P3 gate が通る。

## ブロッカー

次の場合は P4 に進みません。

- selected function に role、name、prototype evidence がない。
- Metadata validation が失敗する。
- Ghidra apply と verify が一致しない。
- source-derived name が stale または deferred source comparison に依存している。
- hotpath record が missing function または invalid address を参照している。

## P4 へのハンドオフ

P3 が通ったら `headless-ghidra-batch-decompile` に進みます。handoff には selected functions、recovered names、signatures、hotpath priority、known unresolved callees、incremental compare に必要な fallback strategy を含めます。
