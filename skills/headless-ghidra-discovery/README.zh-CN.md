# Headless Ghidra Metadata Enrichment — P3

P3 将 baseline、runtime 和 third-party 证据转换为补全后的元数据：函数名、签名、类型、常量、字符串和选定 hotpath 注释。它为选定反编译做准备，但本身不反编译函数。

语言版本：[English](./README.md) | [日本語](./README.ja-JP.md)

## 流水线位置

```text
P0 Intake -> P1 Baseline+Runtime -> P2 Third-Party -> [P3 Metadata -> P4 Substitution]*
```

P3 是迭代恢复循环中的元数据阶段。当某个 P4 batch 暴露缺失名称、签名或类型信息时，工作流可以回到 P3。

## 使用场景

在以下情况使用本技能：

- P2 已识别第三方状态和函数分类。
- Hotpath 函数在 P4 前需要恢复名称和签名。
- Ghidra 需要通过 CLI 应用 rename 或 signature。
- Metadata YAML 需要在选定反编译前验证。

不要用它生成函数体反编译或替换记录。

## 阶段边界

- Metadata 写入通过 `ghidra-agent-cli metadata ...`。
- Ghidra 项目变更通过 CLI 锁下的串行 `ghidra-agent-cli ghidra ...` 命令。
- 生成候选 YAML 的分析可以并行，但 Ghidra apply/verify 操作必须串行。
- P3 不得重写历史 P4 per-function 输出。

## 输入

- `artifacts/<target-id>/pipeline-state.yaml`
- `artifacts/<target-id>/runtime/hotpaths/call-chain.yaml`
- `artifacts/<target-id>/third-party/identified.yaml`
- `artifacts/<target-id>/baseline/functions.yaml`
- `artifacts/<target-id>/baseline/callgraph.yaml`
- `artifacts/<target-id>/baseline/types.yaml`

## 输出

- `artifacts/<target-id>/metadata/renames.yaml`
- `artifacts/<target-id>/metadata/signatures.yaml`
- `artifacts/<target-id>/metadata/types.yaml`
- `artifacts/<target-id>/metadata/constants.yaml`
- `artifacts/<target-id>/metadata/strings.yaml`
- `artifacts/<target-id>/metadata/apply-records/`

## 技能会使用的命令

这些示例展示技能可能调用的 CLI。正常使用时，请让 agent 运行对应阶段；只有排障时才需要手动查看或复现。

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

## 阶段流程

1. 确认 P2 gate 已通过。
2. 审查 hotpath 优先级和 callgraph 上下文。
3. 对每个选定函数，记录角色、名称和原型证据。
4. 通过 `metadata enrich-function` 写入 metadata。
5. 验证 metadata 和 hotpath 记录。
6. 通过 CLI 将 renames 和 signatures 应用到 Ghidra。
7. 验证已应用的 renames 和 signatures。
8. 运行 P3 gate。

## 证据标准

每个恢复名称或原型都应能回溯到证据：第三方源码比较、字符串、导入、类型用法、调用图位置、runtime observation 或已匹配边界。证据薄弱时，名称应保持 provisional，不要将该函数送入 P4。

## 退出标准

- 选入 P4 的 hotpath 函数有明确名称和签名。
- Metadata YAML 验证通过。
- Ghidra rename 和 signature apply 操作有验证记录。
- 补全可从 baseline、runtime 和 third-party 证据重现。
- P3 gate 通过。

## 阻塞条件

出现以下情况时不要进入 P4：

- 选定函数缺少角色、名称或原型证据。
- Metadata validation 失败。
- Ghidra apply 和 verify 不一致。
- 源码派生名称依赖 stale 或 deferred 的源码比较。
- Hotpath 记录引用缺失函数或无效地址。

## 交接到 P4

P3 通过后，路由到 `headless-ghidra-batch-decompile`。交接应包含选定函数、恢复名称、签名、hotpath 优先级、已知未解析 callee，以及增量比较需要的 fallback strategy。
