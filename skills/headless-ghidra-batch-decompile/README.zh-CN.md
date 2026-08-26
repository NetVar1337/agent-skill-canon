# Headless Ghidra Function Substitution — P4

P4 应用补全后的元数据，通过 Ghidra 反编译选定函数，并记录 per-function 替换产物。它是唯一会创建选定反编译和替换记录的主阶段。

语言版本：[English](./README.md) | [日本語](./README.ja-JP.md)

## 流水线位置

```text
P0 Intake -> P1 Baseline+Runtime -> P2 Third-Party -> [P3 Metadata -> P4 Substitution]*
```

P4 面向 batch。每个 batch 后，工作流要么回到 P3 继续元数据补全，要么在选定函数完成后结束。

## 使用场景

在以下情况使用本技能：

- P3 已为选定函数验证名称和签名。
- `substitution/next-batch.yaml` 标识当前 worklist。
- 选定函数需要 Ghidra 反编译。
- 需要写入 function-local captures、substitutions 和 follow-up records。
- 重构边界必须与原始目标比较。

## 阶段边界

- Ghidra 是唯一批准的反编译后端。
- Apply、verify、decompile、rebuild 和 substitution 写入都通过 `ghidra-agent-cli`。
- 只处理 active batch 中的函数。
- Pristine third-party source 不得修改。
- 比较过程中未解析 callee 应路由回原始目标。

## 输入

- `artifacts/<target-id>/baseline/*.yaml`
- `artifacts/<target-id>/runtime/fixtures/`
- `artifacts/<target-id>/runtime/hotpaths/call-chain.yaml`
- `artifacts/<target-id>/third-party/identified.yaml`
- `artifacts/<target-id>/third-party/pristine/<library>@<version>/`
- 需要时的 `artifacts/<target-id>/third-party/compat/<library>@<version>/`
- `artifacts/<target-id>/metadata/renames.yaml`
- `artifacts/<target-id>/metadata/signatures.yaml`
- `artifacts/<target-id>/substitution/next-batch.yaml`

## 输出

- `artifacts/<target-id>/substitution/functions/<fn_id>/capture.yaml`
- `artifacts/<target-id>/substitution/functions/<fn_id>/substitution.yaml`
- 可选 function-local blocked、injected、comparison 或 follow-up YAML
- P4 gate reports

## 技能会使用的命令

这些示例展示技能可能调用的 CLI。正常使用时，请让 agent 运行对应阶段；只有排障时才需要手动查看或复现。

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

Batch 反编译：

```sh
ghidra-agent-cli --target sample-target ghidra decompile --batch
```

## 阶段流程

1. 确认 P3 gate 已通过。
2. 审查 `substitution/next-batch.yaml`。
3. 如果 Ghidra 项目发生变化，重新 apply 和 verify metadata。
4. 捕获 function-local fixtures 和原始行为。
5. 只反编译 batch 中选定函数。
6. 记录 substitution provenance、status 和 replacement boundary。
7. 按需 rebuild 或准备 hybrid target。
8. 运行 original-versus-hybrid compare。
9. 验证 substitutions 并运行 P4 gate。

## 增量比较契约

每一步只替换当前 outside-in boundary。尚未重构的 callee 必须路由回原始二进制或原始库 handle。只有当前 compare case 记录为 `matched` 后，才能向内推进。

## 退出标准

- 每个已处理函数都有 capture 和 substitution records。
- Decompilation provenance 指明 Ghidra/CLI 是记录来源。
- Replacement boundary 和 fallback route 明确。
- Comparison result 已记录，或 block reason 明确。
- 当前 batch 的 P4 gate 通过。

## 阻塞条件

出现以下情况时不要完成 P4：

- 函数不在 active batch 中。
- 选定函数缺少 P3 名称或签名证据。
- 反编译没有通过 Ghidra/CLI 运行。
- Capture fixtures 缺失。
- 未解析 callee 无法路由回原始目标。
- 需要修改 pristine third-party source。

## P4 后续交接

如果 batch matched 且还有函数，回到 `headless-ghidra-discovery` 选择下一层 outside-in 目标。如果出现 divergence 或 metadata 缺失，带着明确 follow-up questions 回到 P3。如果所有选定函数完成，则结束流水线。
