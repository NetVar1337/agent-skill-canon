# Headless Ghidra 単一関数分析

単一関数を strict order で詳細解析します: types -> constants -> vtables -> function identity -> decompilation。

言語: [English](./README.md) | [简体中文](./README.zh-CN.md)

## 使う場面

この skill は次の場合に使います。

- 1 つの関数に対して、provenance を保った完全な解析が必要。
- 解析開始前に target function を一意の `addr` に解決する必要がある。
- 既存の baseline、metadata、substitution artifacts の更新が許可されている。
- types、constants、vtables、function identity を回収した後でのみ decompilation を行う必要がある。

## まず対象関数を解決する

Step 1 の前に一意の `addr` を確定し、その後で安定した local `fn_id` を選びます。

ユーザーが address を与えた場合:

```sh
ghidra-agent-cli --target <id> functions show --addr <addr>
```

ユーザーが symbol または function name を与えた場合:

```sh
ghidra-agent-cli --target <id> functions list --named-only
```

結果から該当する 1 件を特定し、対応する `addr` を記録します。

解決ルール:

- ユーザーが `fn_id` を与えているならそれを再利用する。
- そうでなければ、正規化した address から安全な local id を作る。例: `fn_00102140`。
- baseline function entry に `fn_id` が入っているとは書かないこと。baseline entry はそれを持ちません。
- 複数候補がある場合は、どの関数かユーザーに確認してから進めます。

## Strict Recovery Order

必ず次の順序で進めます。

1. Type definitions
2. Constant definitions
3. Vtable recovery
4. Function identity
5. Decompilation

Step を飛ばしたり並び替えたりしてはいけません。Step 5 は Step 1-4 が完了してからのみ有効です。

## 更新してよい artifacts

この skill は `artifacts/<target-id>/` 配下の既存 artifacts を更新できます。

- `baseline/vtables.yaml`
- `metadata/renames.yaml`
- `metadata/signatures.yaml`
- `substitution/functions/<fn_id>/*`
- `substitution/next-batch.yaml`

必要なら agent-authored notes として次も書けます。

- `analysis/functions/<fn_id>/step1-types.yaml`
- `analysis/functions/<fn_id>/step2-constants.yaml`
- `analysis/functions/<fn_id>/step3-vtables.yaml`
- `analysis/functions/<fn_id>/step4-identity.yaml`
- `analysis/functions/<fn_id>/step5-decompilation.yaml`
- `analysis/functions/<fn_id>/provenance.yaml`

これらの `analysis/functions/<fn_id>/step*.yaml` は optional なメモであり、`ghidra-agent-cli` が自動生成するファイルではありません。

## 主要コマンド要約

この skill が依存する主要コマンドです。

```sh
# address で関数を解決する
ghidra-agent-cli --target <id> functions show --addr <addr>

# 名前を一意の address に解決する
ghidra-agent-cli --target <id> functions list --named-only

# Step 1: types
ghidra-agent-cli --target <id> types list
ghidra-agent-cli --target <id> ghidra export-baseline

# Step 2: constants
ghidra-agent-cli --target <id> constants list
ghidra-agent-cli --target <id> strings list

# Step 3: vtables
ghidra-agent-cli --target <id> vtables list
ghidra-agent-cli --target <id> ghidra analyze-vtables --write-baseline

# Step 4: function identity
ghidra-agent-cli --target <id> metadata enrich-function --addr <addr> --name '<recovered_name>' --prototype '<signature>'
ghidra-agent-cli --target <id> ghidra apply-renames
ghidra-agent-cli --target <id> ghidra apply-signatures

# Step 5: decompilation と substitution
ghidra-agent-cli --target <id> ghidra decompile --fn-id <fn_id> --addr <addr>
ghidra-agent-cli --target <id> substitute add --fn-id <fn_id> --addr <addr> --replacement '<curated replacement source>'
```

## 各 Step の目的

- Step 1 では関数が使う struct、enum、typedef、pointer、array などの型を回収します。
- Step 2 では magic numbers、flags、string constants などの定数意味を回収します。
- Step 3 では vtable 文脈を回復し、`baseline/vtables.yaml` を更新できます。
- Step 4 では単一の `metadata enrich-function` で name と signature を更新し、その後 per-function flags なしで `ghidra apply-renames` / `ghidra apply-signatures` を適用します。
- Step 5 では選んだ local `fn_id` で decompile し、`substitute add --replacement ...` で curated substitution を記録します。

## 制約

- Step 1 の前に `addr` を解決し、Step 5 の前に `fn_id` を決めること。
- address 入力には `functions show --addr`、symbol/name 入力には `functions list --named-only` を使うこと。
- 適切な metadata list コマンドがなければ、`metadata/renames.yaml` と `metadata/signatures.yaml` を直接読むこと。
- 後続 Step が前段の証拠に追跡できるよう、provenance を明確に保つこと。
