import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";
import type { ExtensionAPI, ExtensionContext } from "@earendil-works/pi-coding-agent";
import { getAgentDir } from "@earendil-works/pi-coding-agent";

type ThinkingLevel = "off" | "minimal" | "low" | "medium" | "high" | "xhigh" | "max";

interface ResearchProfile {
  tools: string[];
  instructions: string;
  thinkingLevel?: ThinkingLevel;
}

type Profiles = Record<string, ResearchProfile>;
interface ProfileState { name: string }

function loadProfiles(): Profiles {
  const path = join(getAgentDir(), "research-profiles.json");
  if (!existsSync(path)) return {};
  try {
    const data: unknown = JSON.parse(readFileSync(path, "utf8"));
    return data && typeof data === "object" ? data as Profiles : {};
  } catch {
    return {};
  }
}

export default function researchProfiles(pi: ExtensionAPI) {
  let profiles: Profiles = {};
  let activeName: string | undefined;
  let activeProfile: ResearchProfile | undefined;

  function updateStatus(ctx: ExtensionContext): void {
    ctx.ui.setStatus(
      "research-profile",
      activeName ? ctx.ui.theme.fg("accent", ` profile:${activeName}`) : undefined,
    );
  }

  function apply(name: string, ctx: ExtensionContext, persist: boolean): string | undefined {
    const profile = profiles[name];
    if (!profile) return `Unknown profile "${name}". Available: ${Object.keys(profiles).sort().join(", ") || "none"}`;

    const available = new Set(pi.getAllTools().map((tool) => tool.name));
    const tools = profile.tools.filter((tool) => available.has(tool));
    if (tools.length === 0) return `Profile "${name}" has no available tools.`;

    pi.setActiveTools(tools);
    if (profile.thinkingLevel) pi.setThinkingLevel(profile.thinkingLevel);
    activeName = name;
    activeProfile = profile;
    if (persist) pi.appendEntry<ProfileState>("research-profile", { name });
    updateStatus(ctx);
    return undefined;
  }

  function restore(ctx: ExtensionContext): void {
    profiles = loadProfiles();
    activeName = undefined;
    activeProfile = undefined;
    for (const entry of ctx.sessionManager.getBranch()) {
      if (entry.type !== "custom" || entry.customType !== "research-profile") continue;
      const data = entry.data as Partial<ProfileState> | undefined;
      if (typeof data?.name === "string") apply(data.name, ctx, false);
    }
    updateStatus(ctx);
  }

  pi.registerCommand("research-profile", {
    description: "Set a security-research profile: triage, research, windows, or report",
    handler: async (args, ctx) => {
      profiles = loadProfiles();
      const name = args.trim();
      if (!name) {
        ctx.ui.notify(`Active: ${activeName ?? "default"}. Available: ${Object.keys(profiles).sort().join(", ")}`, "info");
        return;
      }
      const error = apply(name, ctx, true);
      ctx.ui.notify(error ?? `Research profile: ${name}`, error ? "error" : "info");
    },
  });

  pi.registerShortcut("ctrl+shift+r", {
    description: "Cycle security-research profiles",
    handler: async (ctx) => {
      profiles = loadProfiles();
      const names = Object.keys(profiles).sort();
      if (names.length === 0) return;
      const next = names[(Math.max(0, names.indexOf(activeName ?? "")) + 1) % names.length]!;
      const error = apply(next, ctx, true);
      ctx.ui.notify(error ?? `Research profile: ${next}`, error ? "error" : "info");
    },
  });

  pi.on("session_start", (_event, ctx) => restore(ctx));
  pi.on("session_tree", (_event, ctx) => restore(ctx));
  pi.on("before_agent_start", (event) => activeProfile
    ? { systemPrompt: `${event.systemPrompt}\n\n${activeProfile.instructions}` }
    : undefined,
  );
}
