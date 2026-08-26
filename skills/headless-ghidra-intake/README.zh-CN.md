# Headless Ghidra Intake — P0

P0 将用户提供的二进制文件或已接受的归档成员转换为有效的 `ghidra-agent-cli` 目标。它负责确认目标身份、验证本地工具、初始化工作区，并在任何 Ghidra 分析运行前记录分析范围。

语言版本：[English](./README.md) | [日本語](./README.ja-JP.md)

## 流水线位置

```text
P0 Intake -> P1 Baseline+Runtime -> P2 Third-Party -> [P3 Metadata -> P4 Substitution]*
```

P0 是准备阶段。它不得将二进制导入 Ghidra 做分析、导出 baseline 元数据、应用名称或签名，也不得反编译函数。

## 使用场景

在以下情况使用本技能：

- 新的目标二进制、库、对象文件或已接受归档成员需要工作区。
- 既有运行需要确认目标身份、二进制路径或范围。
- Ghidra discovery 或 binary inspection 尚未记录。
- P1 启动前需要设置 scope。

不要用它导出 baseline、分类第三方代码、恢复元数据或执行函数替换。

## 阶段边界

- 正常审查本阶段时使用本 README。
- 排障需要精确 agent 工作流规则时，查看 [headless-ghidra/SKILL.md](../headless-ghidra/SKILL.md)。
- 排障需要精确辅助命令细节时，查看 [ghidra-agent-cli/SKILL.md](../ghidra-agent-cli/SKILL.md)。
- 状态归属：`artifacts/<target-id>/pipeline-state.yaml` 和 `artifacts/<target-id>/scope.yaml`。
- 分析后端：本阶段不使用分析后端。

如果 intake 需要的能力 CLI 尚不支持，应先明确决策，再添加辅助脚本。P0 不得创建或运行新的 Ghidra 脚本。

## 输入

- 用户提供的二进制或归档路径。
- 工作区根目录。
- 可选目标 ID。
- Ghidra discovery 所需的本地环境。
- 用户关于整目标、地址、符号或函数范围的指导。

对于归档输入，只传递已接受的提取成员路径，并将归档来源信息保留在目标 ID 或 intake 记录中。

## 输出

- `targets/<target-id>/ghidra-projects/`
- `artifacts/<target-id>/pipeline-state.yaml`
- `artifacts/<target-id>/scope.yaml`
- 可选的 `artifacts/<target-id>/intake/` 阶段记录

## 技能会使用的命令

这些示例展示技能可能调用的 CLI。正常使用时，请让 agent 运行对应阶段；只有排障时才需要手动查看或复现。

```sh
ghidra-agent-cli ghidra discover
ghidra-agent-cli inspect binary --target ./sample-target
ghidra-agent-cli workspace init --target sample-target --binary ./sample-target
ghidra-agent-cli --target sample-target scope show
ghidra-agent-cli --target sample-target scope set --mode full
ghidra-agent-cli --target sample-target gate check --phase P0
```

额外 scope 命令：

```sh
ghidra-agent-cli --target sample-target scope add-entry --entry 0x401000
ghidra-agent-cli --target sample-target scope remove-entry --entry 0x401000
```

## 阶段流程

1. 选择稳定的目标 ID。
2. 运行 `ghidra discover` 并记录发现的安装路径。
3. 对实际要分析的输入运行 `inspect binary`。
4. 使用 `workspace init` 初始化工作区。
5. 将 scope 设置为整目标或显式条目。
6. 审查 `pipeline-state.yaml` 和 `scope.yaml`。
7. 运行 `gate check --phase P0`。

## Scope 指引

当目标应整体分析时使用 `--mode full`。当审查有意限制在已知地址、函数、符号或归档成员时，使用显式 entries。Scope 必须能从 YAML 本身理解；不要只依赖对话中的理由。

## 退出标准

- 目标工作区存在，并可通过 `--target <id>` 寻址。
- `pipeline-state.yaml` 记录所选二进制路径。
- `scope.yaml` 记录整目标范围或显式 entries。
- Ghidra discovery 已运行并审查。
- Binary inspection 已运行并审查。
- P0 gate 通过。

## 阻塞条件

出现以下情况时不要进入 P1：

- 二进制路径含糊或已不存在。
- 无法发现 Ghidra。
- Scope 为空或与用户请求矛盾。
- 归档规范化没有产生已接受成员。
- P0 gate 报告 intake 产物缺失或不完整。

## 交接到 P1

P0 通过后，路由到 `headless-ghidra-baseline`。P1 交接应包含目标 ID、二进制路径、scope 摘要、Ghidra discovery 结果，以及适用的归档来源信息。
