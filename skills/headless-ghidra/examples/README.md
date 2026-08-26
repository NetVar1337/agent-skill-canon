# Headless Ghidra Examples

These documents help you decide what to do after the basic skill workflow is
installed. They are optional reading for normal use; start with the top-level
[README](../../README.md) when you only need installation and first-run
instructions.

Translations: [简体中文](./README.zh-CN.md) | [日本語](./README.ja-JP.md)

## Choose A Document

| Goal                                                         | Type         | Read                                                                    |
| ------------------------------------------------------------ | ------------ | ----------------------------------------------------------------------- |
| Decide which function or library area to analyze next        | How-to guide | [Analysis Selection Playbook](./analysis-selection-playbook.md)         |
| See the full workflow on one local target                    | Tutorial     | [Reverse Engineering Walkthrough](./reverse-engineering-walkthrough.md) |
| Write a custom Ghidra script when the CLI lacks a capability | Reference    | [Ghidra Script Authoring](./ghidra-script-authoring.md)                 |
| Review a proposed Ghidra script before running it            | Checklist    | [Ghidra Script Review Checklist](./ghidra-script-review-checklist.md)   |

## How These Fit

- P0-P4 phase READMEs define the normal skill workflow.
- These examples explain analysis choices and special cases.
- Runtime output still belongs in the active workspace under
  `targets/<target-id>/` and `artifacts/<target-id>/`.
