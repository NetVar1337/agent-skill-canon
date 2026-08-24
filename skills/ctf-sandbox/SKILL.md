---
name: ctf-sandbox
description: Thin PRIMARY for CTF / AWD / 靶场 multi-type orchestration. Hands off to the sidecar CTF-Sandbox-Orchestrator. Use when the user says CTF, AWD, 靶场, or 比赛题 and no more specific pwn/APK/IDA route already won.
---

# CTF sandbox entry (sidecar, not a second router)

## ACTION REQUIRED（读完后立刻执行）

1. `NOW`: 跑 `../scripts/case-init.ps1`；`auth.status=granted` 前禁止对真实外网 ACT。竞赛/靶场用 `-NetworkProfile lab` 或 `offline`。
2. `NOW`: 若包根存在 `../../CTF-Sandbox-Orchestrator/ctf-sandbox-orchestrator/SKILL.md`，打开它按 sandbox 假设继续。**该 sidecar 未安装时（当前工作站状态）**：跳过此步，本 skill 直接充当编排层——按题型分流 `pwn-chain`（pwn/ROP）、`apk-reverse`（APK）、`ida-reverse`/`r2mcp-basic`（RE）、`web` 类走 `api-security`/`pentest-tools`，靶场边界仍以 scope 的 NetworkProfile 为准。
3. `MUST NOT` 把 40+ 个 `competition-*` 子技能写进 `routing.json`。本入口只是一条 PRIMARY 门闩。
4. `ACT`: 由 orchestrator 选一个 downstream `competition-*`。具体题型已明确时（pwn/ROP、APK、IDA）应已由 `routing.json` 更靠前的规则赢下，不要再抢。

## 为什么单独一层

`CTF-Sandbox-Orchestrator/` 是 **GPL 旁路包**，授权默认是沙箱内部。核心路由包仍是 MIT + `scope.md` 门禁。本 skill 只做关键词入口，不把竞赛树并进核心。

## 任务完成自检（声称完成前 MUST 通过）

- [ ] 我是否先走了 case-init / scope，而不是把“用户说了 CTF”当成已授权外网？
- [ ] 我是否打开了 sidecar orchestrator（或在 sidecar 缺失时按上面的分流表接管），而不是把 40 个子技能当 PRIMARY？
- [ ] 若任务其实是 pwn/APK/IDA，我是否让更具体的 PRIMARY 接手？
