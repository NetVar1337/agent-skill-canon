# Headless Ghidra Third-Party Evidence — P2

P2 は baseline evidence と runtime evidence をレビューし、target に third-party code が含まれるか判断します。libraries、versions、confidence、pristine source location、local adaptation area、function classification を記録し、後続の metadata recovery に渡します。

言語: [English](./README.md) | [简体中文](./README.zh-CN.md)

## パイプライン内の位置

```text
P0 Intake -> P1 Baseline+Runtime -> P2 Third-Party -> [P3 Metadata -> P4 Substitution]*
```

P2 は evidence と classification の phase です。Ghidra に name/signature を apply せず、function decompilation も行いません。

## 使用する場面

この skill は次の場合に使います。

- Baseline imports、strings、constants、types、vtables、callgraph evidence がレビュー可能。
- upstream project または vendored library の可能性を記録したい。
- pristine third-party source を登録したい。
- function を first-party または third-party と分類したい。
- third-party code が見つからなかったことを明示的に記録したい。

## フェーズ境界

- Baseline read は `functions`、`imports`、`constants`、`strings`、`vtables`、`types`、`callgraph` command で行います。
- Third-party write は `third-party` command で行います。
- Source acquisition 自体は CLI 外です。取得内容をレビューした後、CLI が source path と evidence を記録します。
- Pristine source は未変更のまま保持します。Local adaptation note や patch は `third-party/compat/` に置きます。

## 入力

- `artifacts/<target-id>/baseline/functions.yaml`
- `artifacts/<target-id>/baseline/callgraph.yaml`
- `artifacts/<target-id>/baseline/types.yaml`
- `artifacts/<target-id>/baseline/constants.yaml`
- `artifacts/<target-id>/baseline/vtables.yaml`
- `artifacts/<target-id>/baseline/strings.yaml`
- `artifacts/<target-id>/baseline/imports.yaml`
- `artifacts/<target-id>/runtime/run-manifest.yaml`
- `artifacts/<target-id>/runtime/hotpaths/call-chain.yaml`
- 既存の `artifacts/<target-id>/third-party/identified.yaml`。あれば。

## 出力

- `artifacts/<target-id>/third-party/identified.yaml`
- `artifacts/<target-id>/third-party/pristine/<library>@<version>/`
- 任意の `artifacts/<target-id>/third-party/compat/<library>@<version>/`
- evidence decision を説明する execution log entries

## Skill が使う command

これらは skill が呼び出す可能性がある CLI 例です。通常の利用では agent に phase の実行を依頼し、手作業で実行するのは troubleshooting 時だけにしてください。

```sh
ghidra-agent-cli --target sample-target functions list
ghidra-agent-cli --target sample-target imports list
ghidra-agent-cli --target sample-target strings list
ghidra-agent-cli --target sample-target constants list
ghidra-agent-cli --target sample-target types list
ghidra-agent-cli --target sample-target vtables list
ghidra-agent-cli --target sample-target callgraph list
ghidra-agent-cli --target sample-target third-party add \
  --library zlib \
  --version 1.2.13 \
  --confidence high \
  --evidence "import and string evidence"
ghidra-agent-cli --target sample-target third-party vendor-pristine \
  --library zlib \
  --source-path ./third_party/upstream/zlib
ghidra-agent-cli --target sample-target third-party classify-function \
  --addr 0x401000 \
  --classification third-party \
  --evidence "matches zlib inflate path"
ghidra-agent-cli --target sample-target gate check --phase P2
```

third-party code がない場合:

```sh
ghidra-agent-cli --target sample-target third-party none --evidence "review found no library match"
```

## Phase Flow

1. imports、strings、constants、types、vtables、functions、callgraph をレビューする。
2. 記録済み evidence から library/version hypothesis を立てる。
3. upstream script を実行せずに pristine source を取得または識別する。
4. 受け入れた library と source path を登録する。
5. evidence が十分な function を分類する。
6. 該当する場合は no-third-party review を明示的に記録する。
7. 自明でない判断に execution log note を追加する。
8. P2 gate を実行する。

## Evidence standards

symbol name、import name、string、file path、type name、version string、callgraph shape、runtime trace など具体的な anchor を使います。理由のない「それらしい」は避けます。Confidence は都合ではなく evidence quality を反映してください。

## 完了条件

- third-party code がある場合、`identified.yaml` が library を記録している。
- 見つからなかった場合、`identified.yaml` が `libraries: []` を記録している。
- 各 library に version、confidence、evidence、local source information がある。
- Pristine source path と local adaptation change が分離されている。
- P3 が name、signature、type を回復するのに十分な classification evidence がある。
- P2 gate が通る。

## ブロッカー

次の場合は P3 に進みません。

- Third-party status が曖昧。
- evidence なしで library が命名されている。
- pristine directory 内の source が変更されている。
- version confidence が downstream source-derived name に不十分。
- function classification が baseline evidence と矛盾する。

## P3 へのハンドオフ

P2 が通ったら `headless-ghidra-discovery` に進みます。handoff には accepted libraries、uncertain libraries、pristine paths、local adaptation paths、classified functions、evidence gaps をまとめます。
