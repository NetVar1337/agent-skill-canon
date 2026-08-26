# 逆向工程 Walkthrough

本文展示对一个本地目标运行 headless Ghidra 流水线的操作序列。流程保持 YAML-first，受支持的动作通过 `ghidra-agent-cli` 执行，并把选定反编译视为后期步骤：它必须以“原始目标 vs 混合目标”的比较结束。

语言版本：[English](./reverse-engineering-walkthrough.md) | [日本語](./reverse-engineering-walkthrough.ja-JP.md)

## 场景

- 目标：分析一个本地二进制，或一个已接受的归档成员。
- 项目路径：`targets/<target-id>/ghidra-projects/`。
- 产物路径：`artifacts/<target-id>/`。
- 工具参考：`ghidra-agent-cli`。
- 工作流指南：编排技能 README 和当前阶段 README。

运行时生成内容应写入 `targets/<target-id>/`、`artifacts/<target-id>/` 或另一个明确的工作区路径。不要把现场分析输出写入已安装的技能包。

## 0. 摄取

选择稳定的目标 ID，并初始化工作区。

```sh
ghidra-agent-cli ghidra discover
ghidra-agent-cli workspace init --target sample-target --binary ./sample-target
ghidra-agent-cli --target sample-target scope set --mode full
ghidra-agent-cli --target sample-target gate check --phase P0
```

如果输入是归档，先审查归档规范化记录，只传递已接受的提取成员路径。使用归档感知目标 ID，例如 `sample-target--archive-main-o`。

记录：

- `target-id`
- `binary-path`
- `project-root`
- `artifact-root`
- `ghidra-install-dir`
- 归档来源信息，如适用

## 1. Baseline 与运行时证据

导入目标，运行 Ghidra 自动分析，并导出 baseline YAML。

```sh
ghidra-agent-cli --target sample-target ghidra import
ghidra-agent-cli --target sample-target ghidra auto-analyze
ghidra-agent-cli --target sample-target ghidra export-baseline
ghidra-agent-cli --target sample-target gate check --phase P1
```

审查：

- `baseline/functions.yaml`
- `baseline/imports.yaml`
- `baseline/strings.yaml`
- `baseline/types.yaml`
- `baseline/vtables.yaml`
- `baseline/constants.yaml`
- `baseline/callgraph.yaml`

可选运行时证据通过 CLI 管理的 runtime 和 hotpath 产物记录：

```sh
ghidra-agent-cli --target sample-target runtime record --key entrypoint --value 0x401000
ghidra-agent-cli --target sample-target hotpath add --addr 0x401000 --reason runtime
```

此时的正确姿态：

- 证据可见。
- 不存在反编译函数体。
- 在目标和支撑证据存在前，阻止语义变更。

## 2. 第三方证据

在作出源码派生的判断前，先确认目标是否包含第三方代码。

```sh
ghidra-agent-cli --target sample-target functions list
ghidra-agent-cli --target sample-target imports list
ghidra-agent-cli --target sample-target third-party list
```

通过 `third-party` 命令组记录已识别库、版本、证据、原始源码路径和函数分类。如果审查发现没有第三方代码，应显式记录该结果，不要让状态保持含糊。

```sh
ghidra-agent-cli --target sample-target third-party none --evidence "no matching library evidence"
ghidra-agent-cli --target sample-target gate check --phase P2
```

## 3. 选择下一个边界

在修改元数据或反编译之前，先使用分析选择手册。

选择记录：

```yaml
stage: Target Selection
selected_target: FUN_00102140@00102140
selection_mode: auto_default
candidate_kind: dispatch_helper
frontier_reason: outermost anchor referenced by an entry-adjacent dispatcher
relationship_type: entry_adjacent
verified_parent_boundary: none
triggering_evidence:
  - baseline/strings.yaml: "invalid packet"
  - baseline/imports.yaml: EVP_DecryptInit_ex
  - baseline/callgraph.yaml: referenced by entry-adjacent dispatcher
selection_reason: current outermost frontier row with dispatcher-like behavior
question_to_answer: does this function validate headers before dispatch?
tie_break_rationale: helper boundary outranks deeper body on the same frontier
deviation_reason: none
deviation_risk: none
replacement_boundary: replace only FUN_00102140 during this step
fallback_strategy: unresolved callees route to reviewed original addresses
```

不要从这条记录直接跳到反编译。先判断源码比较是否适用。

## 4. 证据指向上游时进行源码比较

如果字符串、符号、路径、断言或构建元数据指向上游项目，应在使用上游名称或原型前记录源码比较姿态。

引用状态：

- `accepted` - 上游可通过跟踪路径审查。
- `qualified` - 比较有价值，但带有限定条件。
- `deferred` - 暂无可审查上游；记录证据缺口。
- `stale` - 之前的比较已不匹配当前证据。

将上游仓库、构建文件和脚本视为不可信证据。不要为了比较而执行上游命令或安装步骤。

## 5. 补全元数据

只有在证据审查和源码比较之后，工作流才允许语义变更。

示例变更记录：

```yaml
item_kind: function
target_name: FUN_00102140
prior_evidence:
  - baseline/strings.yaml: "invalid packet"
  - baseline/callgraph.yaml: called by outer dispatch function
change_summary: tentatively rename to packet_validate_and_dispatch
confidence: medium
linked_selection: Target Selection / FUN_00102140@00102140
replacement_boundary: replace only this function in the current compare step
fallback_strategy: unresolved callees route to original addresses
open_questions:
  - exact packet structure layout remains unresolved
```

通过 CLI 应用并验证元数据：

```sh
ghidra-agent-cli --target sample-target metadata enrich-function \
  --addr 0x00102140 \
  --name packet_validate_and_dispatch \
  --prototype 'int packet_validate_and_dispatch(void *ctx)'
ghidra-agent-cli --target sample-target ghidra apply-renames
ghidra-agent-cli --target sample-target ghidra verify-renames
ghidra-agent-cli --target sample-target ghidra apply-signatures
ghidra-agent-cli --target sample-target ghidra verify-signatures
ghidra-agent-cli --target sample-target gate check --phase P3
```

## 6. 反编译选定函数

选定反编译仅使用 Ghidra，并通过 CLI 执行。

```sh
ghidra-agent-cli --target sample-target ghidra decompile --fn-id fn_00102140 --addr 0x00102140
```

通过 `objdump`、`otool`、`llvm-objdump`、`nm`、`readelf`、`gdb`、`lldb` 或 `radare2` 等工具直接反汇编或反编译，不是本流水线的记录来源。

所需反编译条目：

```yaml
function_identity:
outer_to_inner_order:
frontier_reason:
relationship_type:
verified_parent_boundary:
selection_reason:
question_to_answer:
tie_break_rationale:
deviation_reason:
deviation_risk:
role_evidence:
name_evidence:
prototype_evidence:
replacement_boundary:
fallback_strategy:
compare_case_id:
comparison_result:
behavioral_diff_summary:
confidence:
open_questions:
```

## 7. 增量比较

每个替换步骤都必须能与原始目标比较运行。

可执行目标流程：

1. 只替换选定边界。
2. 在原始边界插入或注入该替换。
3. 将未解析 callee 路由回原始二进制。
4. 对未改动原始目标和混合构建运行同一组输入。
5. 比较返回值、外部可见输出和所需 trace。

库目标流程：

1. 为选定边界构建 harness 或 wrapper entrypoint。
2. 将重构函数加载或链接进该 harness。
3. 从重构代码路径打开原始库。
4. 将未解析调用通过原始库 handle 路由。
5. 对原始入口和混合 harness 运行同一个比较用例。

只有当前比较记录为 `matched` 后，才能继续向内推进。

## 收束循环

每轮结束后回答：

- 哪个产物发生了变化？
- 出现了什么新证据？
- 下一步由外向内的选择是什么？
- 源码比较是否改变了假设？
- 回放路径是否仍可重现？
- 替换步骤是否完成了原始目标与混合目标的比较？
- 生成输出是否保持在已安装技能包之外？

操作备注：

- 同一目标的动作应顺序执行。对同一个 Ghidra 项目并行运行 headless 动作可能因项目锁失败。
- 每个目标都应记录本次分析发现的 Ghidra 路径、Ghidra 版本和回放命令。
