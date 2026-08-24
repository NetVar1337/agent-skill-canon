# RULES — 硬性规则与完成 Checklist

> 本文件是 router skill 引用的规则源。适用范围：整个 reverse-skill 技能包下所有子 skill。

## RFC 2119 语义

- `MUST` / `MUST NOT`：硬门，违背即任务失败或安全违规。
- `SHOULD`：默认要做，跳过必须给出一句理由。
- `MAY`：可选。

## 授权门（每个任务开始前）

1. `case-init` 落地 `work/<case>/scope.md`，写明目标、来源、授权依据（operator 站内长期授权写 "standing"即可）。
2. auth 未 granted 时禁止对目标做任何主动操作（ACT）；只读分析也必须先落 scope。
3. 硬停（继承工作站 AGENTS.md）：不外传 secrets、不覆盖未提交工作、不代发消息/公开 PR、不 force-push main。

## 工具使用

1. 工具路径只认 `tool-index.md`（`~/.agents/skills/tool-index.md`）。缺工具 → bootstrap（manifest 内）或记录为缺失，MUST NOT 编造路径。
2. 版本敏感的结论（偏移、结构体、syscall 号）MUST 记录目标 build 号。

## 任务完成后的硬性 Checklist（声称完成前 MUST 全过）

- [ ] 路由三轴完成：目标类型 × 用户意图 × 工具链，PRIMARY 已执行而非只读目录。
- [ ] 所有结论可追溯：Evidence（命令输出/文件路径）→ Finding → Path。
- [ ] 目标 build / 版本 / 架构已记录（适用时）。
- [ ] 未验证的推断已显式标注 "推测"，与事实区分。
- [ ] 产生的工件（PoC、dump、报告）路径已汇报；临时文件已清理或在 scope 内标注。
- [ ] 经验回写 `field-journal/`：新任务查 `_index.md`，完成后按 `_template.md` 追加一条（日期_主题.md），先例文件（precedent-*.md）按需更新。
- [ ] 若安装/升级了工具，`tool-index.md` 已回写。

## field-journal 回写格式

文件名：`YYYY-MM-DD_短主题.md`（见 `_template.md`）。至少包含：目标与 build、做了什么、踩坑、可复用结论、关联 skill 名。回写是 `SHOULD`（用户要交付物时升格为 `MUST`）。
