# Headless Ghidra 示例文档

这些文档用于帮助你在安装并开始使用技能族后，决定下一步分析什么、如何审查流程，以及何时需要自定义 Ghidra 脚本。只需要安装和首次使用时，请先看顶层 [README](../../README.zh-CN.md)。

语言版本：[English](./README.md) | [日本語](./README.ja-JP.md)

## 选择文档

| 目标                                 | 类型        | 阅读                                                               |
| ------------------------------------ | ----------- | ------------------------------------------------------------------ |
| 决定下一步分析哪个函数或库区域       | How-to 指南 | [分析选择手册](./analysis-selection-playbook.zh-CN.md)             |
| 查看一个本地目标的完整流程           | Tutorial    | [逆向工程 walkthrough](./reverse-engineering-walkthrough.zh-CN.md) |
| CLI 缺少能力时编写自定义 Ghidra 脚本 | Reference   | [Ghidra 脚本编写](./ghidra-script-authoring.md)                    |
| 运行前审查拟新增的 Ghidra 脚本       | Checklist   | [Ghidra 脚本审查清单](./ghidra-script-review-checklist.md)         |

## 它们如何配合

- P0-P4 阶段 README 定义正常技能流程。
- 这些示例解释分析选择和特殊情况。
- 运行产生的内容仍应写入当前工作区的 `targets/<target-id>/` 和 `artifacts/<target-id>/`。
