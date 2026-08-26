# Headless Ghidra Function Substitution — P4

P4 は enriched metadata を適用し、Ghidra で selected functions を decompile し、per-function substitution artifacts を記録します。Selected decompilation と substitution record を作る唯一の primary phase です。

言語: [English](./README.md) | [简体中文](./README.zh-CN.md)

## パイプライン内の位置

```text
P0 Intake -> P1 Baseline+Runtime -> P2 Third-Party -> [P3 Metadata -> P4 Substitution]*
```

P4 は batch-oriented です。batch 後、workflow はさらなる metadata enrichment のため P3 に戻るか、selected functions が完了した場合は終了します。

## 使用する場面

この skill は次の場合に使います。

- P3 が selected functions の name/signature を validate 済み。
- `substitution/next-batch.yaml` が current worklist を示している。
- selected functions に Ghidra decompilation が必要。
- function-local captures、substitutions、follow-up records を書く必要がある。
- reconstructed boundary を original target と compare する必要がある。

## フェーズ境界

- Ghidra が唯一承認された decompilation backend です。
- apply、verify、decompile、rebuild、substitution write は `ghidra-agent-cli` 経由で行います。
- active batch 内の functions だけを処理します。
- pristine third-party source を変更してはいけません。
- compare 中の unresolved callee は original target に戻します。

## 入力

- `artifacts/<target-id>/baseline/*.yaml`
- `artifacts/<target-id>/runtime/fixtures/`
- `artifacts/<target-id>/runtime/hotpaths/call-chain.yaml`
- `artifacts/<target-id>/third-party/identified.yaml`
- `artifacts/<target-id>/third-party/pristine/<library>@<version>/`
- 必要な場合の `artifacts/<target-id>/third-party/compat/<library>@<version>/`
- `artifacts/<target-id>/metadata/renames.yaml`
- `artifacts/<target-id>/metadata/signatures.yaml`
- `artifacts/<target-id>/substitution/next-batch.yaml`

## 出力

- `artifacts/<target-id>/substitution/functions/<fn_id>/capture.yaml`
- `artifacts/<target-id>/substitution/functions/<fn_id>/substitution.yaml`
- 任意の function-local blocked、injected、comparison、follow-up YAML
- P4 gate reports

## Skill が使う command

これらは skill が呼び出す可能性がある CLI 例です。通常の利用では agent に phase の実行を依頼し、手作業で実行するのは troubleshooting 時だけにしてください。

```sh
ghidra-agent-cli --target sample-target ghidra apply-renames
ghidra-agent-cli --target sample-target ghidra verify-renames
ghidra-agent-cli --target sample-target ghidra apply-signatures
ghidra-agent-cli --target sample-target ghidra verify-signatures
ghidra-agent-cli --target sample-target ghidra decompile --fn-id fn_001 --addr 0x401000
ghidra-agent-cli --target sample-target substitute add \
  --fn-id fn_001 \
  --addr 0x401000 \
  --replacement './reconstructed/fn_001.c' \
  --note 'selected boundary replacement'
ghidra-agent-cli --target sample-target substitute validate
ghidra-agent-cli --target sample-target ghidra rebuild-project
ghidra-agent-cli --target sample-target gate check --phase P4
```

Batch decompilation:

```sh
ghidra-agent-cli --target sample-target ghidra decompile --batch
```

## Phase Flow

1. P3 gate が通っていることを確認する。
2. `substitution/next-batch.yaml` をレビューする。
3. Ghidra project が変わった場合は metadata を再 apply/verify する。
4. function-local fixtures と original behavior を capture する。
5. batch 内 selected functions だけを decompile する。
6. substitution provenance、status、replacement boundary を記録する。
7. 必要に応じて hybrid target を rebuild または準備する。
8. original-versus-hybrid compare を実行する。
9. substitutions を validate し、P4 gate を実行する。

## Incremental Compare Contract

各 step は現在の outside-in boundary だけを置換します。まだ reconstruction されていない callee は original binary または original library handle に戻す必要があります。現在の compare case が `matched` と記録されてから内側へ進みます。

## 完了条件

- 処理した各 function に capture と substitution record がある。
- decompilation provenance が Ghidra/CLI を source of record として示している。
- replacement boundary と fallback route が明示されている。
- comparison result が記録されている、または block reason が明示されている。
- batch の P4 gate が通る。

## ブロッカー

次の場合は P4 を完了しません。

- function が active batch に含まれていない。
- selected function に P3 name/signature evidence がない。
- decompilation が Ghidra/CLI 経由で実行されていない。
- capture fixtures が missing。
- unresolved callee を original target に戻せない。
- pristine third-party source の変更が必要になる。

## P4 後のハンドオフ

batch が matched で functions が残っている場合は、次の outside-in selection のため `headless-ghidra-discovery` に戻ります。divergence または missing metadata が出た場合は、明示的な follow-up questions とともに P3 に戻ります。selected functions がすべて完了したら pipeline を終了します。
