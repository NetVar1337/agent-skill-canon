# Headless Ghidra 单函数分析

按严格顺序对单个函数做完整分析：types -> constants -> vtables -> function identity -> decompilation。

语言版本：[English](./README.md) | [日本語](./README.ja-JP.md)

## 何时使用

在以下场景使用本技能：

- 需要对某一个函数做完整、可追溯的分析。
- 分析开始前必须先把目标函数解析到唯一 `addr`。
- 允许更新已有 baseline、metadata 和 substitution 产物。
- 只有在 types、constants、vtables、function identity 完成后，才能进入 decompilation。

## 先解析目标函数

必须先确认唯一 `addr`，然后再确定稳定的本地 `fn_id`。

如果用户直接给了地址：

```sh
ghidra-agent-cli --target <id> functions show --addr <addr>
```

如果用户给的是 symbol 或函数名：

```sh
ghidra-agent-cli --target <id> functions list --named-only
```

从结果中匹配唯一条目，并记录对应的 `addr`。

解析规则：

- 如果用户已提供 `fn_id`，直接复用。
- 否则从规范化地址派生安全本地 id，例如 `fn_00102140`。
- 不要声称 baseline function entry 自带 `fn_id`；它并没有。
- 如果有多个匹配结果，先停下来让用户澄清。

## 严格恢复顺序

必须严格按以下顺序执行：

1. Type definitions
2. Constant definitions
3. Vtable recovery
4. Function identity
5. Decompilation

不要跳步，也不要调换顺序。只有在前 4 步完成后，第 5 步才成立。

## 允许更新的产物

本技能允许更新 `artifacts/<target-id>/` 下已有文件：

- `baseline/vtables.yaml`
- `metadata/renames.yaml`
- `metadata/signatures.yaml`
- `substitution/functions/<fn_id>/*`
- `substitution/next-batch.yaml`

也可以按需写入以下 agent 自行维护的可选笔记：

- `analysis/functions/<fn_id>/step1-types.yaml`
- `analysis/functions/<fn_id>/step2-constants.yaml`
- `analysis/functions/<fn_id>/step3-vtables.yaml`
- `analysis/functions/<fn_id>/step4-identity.yaml`
- `analysis/functions/<fn_id>/step5-decompilation.yaml`
- `analysis/functions/<fn_id>/provenance.yaml`

这些 `analysis/functions/<fn_id>/step*.yaml` 是可选的 agent-authored notes，不是 `ghidra-agent-cli` 自动生成的文件。

## 关键命令摘要

本技能依赖的关键命令如下：

```sh
# 按地址解析函数
ghidra-agent-cli --target <id> functions show --addr <addr>

# 按名称解析到唯一地址
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

# Step 5: decompilation 和 substitution
ghidra-agent-cli --target <id> ghidra decompile --fn-id <fn_id> --addr <addr>
ghidra-agent-cli --target <id> substitute add --fn-id <fn_id> --addr <addr> --replacement '<curated replacement source>'
```

## 各步骤意图

- Step 1 恢复函数涉及的 struct、enum、typedef、pointer、array 等类型。
- Step 2 恢复 magic numbers、flags、string constants 等常量语义。
- Step 3 补全 vtable 上下文，并允许更新 `baseline/vtables.yaml`。
- Step 4 用单条 `metadata enrich-function` 同时写入名称和签名，然后用不带 per-function flags 的 `ghidra apply-renames` / `ghidra apply-signatures` 回写到 Ghidra。
- Step 5 使用选定的本地 `fn_id` 反编译函数，并通过 `substitute add --replacement ...` 记录整理后的替换内容。

## 约束

- Step 1 前先解析 `addr`，Step 5 前先确定 `fn_id`。
- 地址输入用 `functions show --addr`，symbol/name 输入用 `functions list --named-only`。
- 如果没有合适的 metadata list 命令，就直接读取 `metadata/renames.yaml` 和 `metadata/signatures.yaml`。
- 保持清晰的 provenance，确保后续步骤都能追溯到前面的证据。
