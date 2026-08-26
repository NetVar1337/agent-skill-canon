# Headless Ghidra Examples

これらの文書は、skill family をインストールして使い始めた後に、次に何を分析するか、どのようにレビューするか、いつ custom Ghidra script が必要かを判断するためのものです。インストールと最初の実行だけが必要な場合は、top-level [README](../../README.ja-JP.md) から始めてください。

言語: [English](./README.md) | [简体中文](./README.zh-CN.md)

## Choose A Document

| 目的                                               | 種類         | 読むもの                                                                      |
| -------------------------------------------------- | ------------ | ----------------------------------------------------------------------------- |
| 次に分析する function や library area を選ぶ       | How-to guide | [Analysis Selection Playbook](./analysis-selection-playbook.ja-JP.md)         |
| 1 つの local target の全体 workflow を見る         | Tutorial     | [Reverse Engineering Walkthrough](./reverse-engineering-walkthrough.ja-JP.md) |
| CLI に能力がない場合に custom Ghidra script を書く | Reference    | [Ghidra Script Authoring](./ghidra-script-authoring.md)                       |
| 実行前に proposed Ghidra script をレビューする     | Checklist    | [Ghidra Script Review Checklist](./ghidra-script-review-checklist.md)         |

## How These Fit

- P0-P4 phase README が通常の skill workflow を定義します。
- これらの examples は analysis choice と special case を説明します。
- runtime output は引き続き active workspace の `targets/<target-id>/` と `artifacts/<target-id>/` に書き込みます。
