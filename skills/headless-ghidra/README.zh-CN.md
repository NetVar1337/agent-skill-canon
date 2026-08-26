# Headless Ghidra 编排技能

`headless-ghidra` 是 Headless Ghidra 技能族的入口技能。需要 agent 运行完整 YAML-first 逆向工程流水线，并在每个阶段 gate 后暂停审查时，使用它。

语言版本：[English](./README.md) | [日本語](./README.ja-JP.md)

使用前请先按[顶层 README](../README.zh-CN.md) 安装完整技能族。

## 它负责什么

- 检测或继续当前目标。
- 读取 `artifacts/<target-id>/pipeline-state.yaml`。
- 将工作路由到 P0-P4 阶段技能。
- 进入下一阶段前运行 gate check。
- 把用户需要决定的事项显式提出来：继续或重开、是否补充 runtime 证据、batch 选择、divergence 审查和完成确认。

它本身不执行 Ghidra 分析。具体工作由阶段技能和随附的 `ghidra-agent-cli` 完成。

## 常用请求

```text
请使用 headless-ghidra 从 P0 开始分析 ./sample-target。
每个阶段 gate 后暂停，并显示我需要审查的产物。
```

```text
继续已有目标，先显示当前 pipeline state，再进入下一个有效阶段。
```

```text
对选定 hotpath 函数运行下一轮 P3/P4。如果 metadata 不足，回到 P3，
不要直接反编译。
```

## 阶段地图

| 阶段 | 技能 README                                                           | 用途                                         |
| ---- | --------------------------------------------------------------------- | -------------------------------------------- |
| P0   | [Intake](../headless-ghidra-intake/README.zh-CN.md)                   | 确认目标、初始化工作区、设置范围。           |
| P1   | [Baseline](../headless-ghidra-baseline/README.zh-CN.md)               | 导入 Ghidra、导出 baseline、记录运行时证据。 |
| P2   | [Evidence](../headless-ghidra-evidence/README.zh-CN.md)               | 识别第三方代码和证据来源。                   |
| P3   | [Discovery](../headless-ghidra-discovery/README.zh-CN.md)             | 补全名称、签名、类型、常量和字符串。         |
| P4   | [Batch Decompile](../headless-ghidra-batch-decompile/README.zh-CN.md) | 对选定函数应用元数据并反编译。               |

## 审查点

- P0 后：目标身份、二进制路径、Ghidra discovery 和 scope。
- P1 后：baseline 导出、runtime 状态和 hotpath 证据。
- P2 后：第三方判断、源码证据和函数分类。
- P3 后：候选名称、签名、类型和 Ghidra apply 验证。
- P4 后：单函数反编译 capture、substitution record 和 comparison result。

运行产生的内容会写入当前工作区的 `targets/<target-id>/` 和 `artifacts/<target-id>/`。
