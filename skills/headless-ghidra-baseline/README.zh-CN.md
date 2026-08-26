# Headless Ghidra Baseline And Runtime — P1

P1 将目标导入 Ghidra，运行自动分析，导出 baseline YAML，并记录后续阶段用于排序和比较的运行时证据。它创建证据，不做语义重构。

语言版本：[English](./README.md) | [日本語](./README.ja-JP.md)

## 流水线位置

```text
P0 Intake -> P1 Baseline+Runtime -> P2 Third-Party -> [P3 Metadata -> P4 Substitution]*
```

P1 是第一个调用 Ghidra 分析的阶段。它仍不得为替换反编译函数体、应用恢复名称或签名，也不得在原始证据之外作第三方判断。

## 使用场景

在以下情况使用本技能：

- P0 已产生目标工作区和 scope。
- 需要导出函数、调用图、类型、虚表、常量、字符串和导入 baseline。
- 需要捕获 runtime run records、fixtures 或 hotpath 证据。
- 需要验证 P1 gate 材料。

不要用它做源码比较、语义命名、选定反编译或替换。

## 阶段边界

- Ghidra 操作必须通过 `ghidra-agent-cli ghidra ...`。
- Runtime 和 hotpath 记录必须通过 `runtime`、`hotpath` 或受支持的 `frida` 命令。
- Java 和 shell 后端脚本只是 CLI 后面的实现细节。
- P1 不修改 `metadata/`、`third-party/` 或 `substitution/`。

## 输入

- `artifacts/<target-id>/pipeline-state.yaml`
- `artifacts/<target-id>/scope.yaml`
- `targets/<target-id>/ghidra-projects/`
- P0 记录的二进制路径
- 可选 runtime 或 harness 指令

## 输出

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
- 需要 harness 时的 `artifacts/<target-id>/runtime/project/**`

## 技能会使用的命令

这些示例展示技能可能调用的 CLI。正常使用时，请让 agent 运行对应阶段；只有排障时才需要手动查看或复现。

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

可选 Frida 命令：

```sh
ghidra-agent-cli frida device-list
ghidra-agent-cli frida device-attach --pid 1234
ghidra-agent-cli frida io-capture --target ./sample-target --timeout 60
ghidra-agent-cli frida trace --target ./sample-target --functions open,read
```

## 阶段流程

1. 确认 P0 gate 已通过。
2. 将目标导入 Ghidra 项目。
3. 运行 Ghidra 自动分析。
4. 导出 baseline YAML。
5. 审查函数、导入、字符串、类型、虚表、常量和调用边覆盖情况。
6. 记录 runtime 可用性或不可用性。
7. 存在运行时观察时添加 hotpath 证据。
8. 验证 runtime 和 hotpath 产物。
9. 运行 P1 gate。

## Runtime 指引

可执行目标应记录具体调用参数、输入 fixtures 和观察输出。库目标应记录 harness 预期，或说明 runtime execution 为什么暂缓。如果 Frida 不可用，应显式记录，不要让 runtime 状态隐含。

## 退出标准

- 所有 baseline YAML 文件存在且可读。
- Runtime 状态明确且可重现。
- Hotpath 证据存在，或其缺失已有解释。
- 未创建反编译函数体或 P4 替换产物。
- P1 gate 通过。

## 阻塞条件

出现以下情况时不要进入 P2：

- Ghidra import 或 auto-analysis 失败。
- Baseline YAML 缺失或不可读。
- Runtime 要求未知。
- Hotpath 记录与 baseline 地址空间矛盾。
- P1 gate 报告 baseline/runtime 产物缺失或不完整。

## 交接到 P2

P1 通过后，路由到 `headless-ghidra-evidence`。交接应包含 baseline 产物路径、runtime 状态、hotpath 摘要，以及 P2 识别第三方代码时必须考虑的明显缺口。
