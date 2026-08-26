# Headless Ghidra Third-Party Evidence — P2

P2 审查 baseline 和 runtime 证据，判断目标是否包含第三方代码。它记录库、版本、置信度、原始源码位置、本地适配区域和函数分类，供后续元数据恢复使用。

语言版本：[English](./README.md) | [日本語](./README.ja-JP.md)

## 流水线位置

```text
P0 Intake -> P1 Baseline+Runtime -> P2 Third-Party -> [P3 Metadata -> P4 Substitution]*
```

P2 是证据与分类阶段。它不向 Ghidra 应用名称或签名，也不反编译函数。

## 使用场景

在以下情况使用本技能：

- Baseline imports、strings、constants、types、vtables 和 callgraph 证据已可审查。
- 需要记录可能的上游项目或 vendored library。
- 需要登记第三方 pristine source。
- 函数需要第一方或第三方分类。
- 审查需要显式记录没有发现第三方代码。

## 阶段边界

- Baseline 读取通过 `functions`、`imports`、`constants`、`strings`、`vtables`、`types` 和 `callgraph` 命令。
- 第三方写入通过 `third-party` 命令。
- 源码获取本身不属于 CLI；CLI 在获取已审查后记录 source path 和 evidence。
- Pristine source 必须保持未修改。本地适配说明或 patch 放在 `third-party/compat/`。

## 输入

- `artifacts/<target-id>/baseline/functions.yaml`
- `artifacts/<target-id>/baseline/callgraph.yaml`
- `artifacts/<target-id>/baseline/types.yaml`
- `artifacts/<target-id>/baseline/constants.yaml`
- `artifacts/<target-id>/baseline/vtables.yaml`
- `artifacts/<target-id>/baseline/strings.yaml`
- `artifacts/<target-id>/baseline/imports.yaml`
- `artifacts/<target-id>/runtime/run-manifest.yaml`
- `artifacts/<target-id>/runtime/hotpaths/call-chain.yaml`
- 已存在的 `artifacts/<target-id>/third-party/identified.yaml`，如有

## 输出

- `artifacts/<target-id>/third-party/identified.yaml`
- `artifacts/<target-id>/third-party/pristine/<library>@<version>/`
- 可选 `artifacts/<target-id>/third-party/compat/<library>@<version>/`
- 描述证据决策的 execution log entries

## 技能会使用的命令

这些示例展示技能可能调用的 CLI。正常使用时，请让 agent 运行对应阶段；只有排障时才需要手动查看或复现。

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

如果没有第三方代码：

```sh
ghidra-agent-cli --target sample-target third-party none --evidence "review found no library match"
```

## 阶段流程

1. 审查 imports、strings、constants、types、vtables、functions 和 callgraph。
2. 从已记录证据形成 library/version 假设。
3. 在不执行上游脚本的前提下获取或识别 pristine source。
4. 登记每个接受的库和 source path。
5. 证据足够时分类函数。
6. 适用时记录显式无第三方审查。
7. 对非显然决策追加 execution log note。
8. 运行 P2 gate。

## 证据标准

使用具体锚点：符号名、导入名、字符串、文件路径、类型名、版本字符串、调用图形态或 runtime traces。避免没有记录理由的“看起来像”。置信度应反映证据质量，而不是方便程度。

## 退出标准

- 存在第三方代码时，`identified.yaml` 记录库。
- 未发现第三方代码时，`identified.yaml` 记录 `libraries: []`。
- 每个库都有版本、置信度、证据和本地源码信息。
- Pristine source path 与本地适配变更分离。
- P3 有足够分类证据恢复名称、签名和类型。
- P2 gate 通过。

## 阻塞条件

出现以下情况时不要进入 P3：

- 第三方状态含糊。
- 命名某个库但没有证据。
- Pristine 目录中的源码被修改。
- 版本置信度不足以支撑后续源码派生命名。
- 函数分类与 baseline 证据矛盾。

## 交接到 P3

P2 通过后，路由到 `headless-ghidra-discovery`。交接应总结已接受库、不确定库、pristine path、本地适配 path、已分类函数和证据缺口。
