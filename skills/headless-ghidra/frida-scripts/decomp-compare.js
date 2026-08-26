/*
script_id: decomp-compare
scenario: decompilation-to-original comparison
invocation_shape: frida -f <target> -l decomp-compare.js --runtime=v8 --no-pause
expected_outputs: call-order notes, branch markers, return summaries
coverage_notes: Supports bounded compare targets and configured branch markers.
artifact_boundary: Raw runtime values stay in local .work/ghidra-artifacts/<target-id>/ artifacts.
tracked_doc_rule: Redact or generalize raw runtime values before they appear in tracked docs.
escalation_boundary: Route broader behavior capture or new output fields to script review.
*/

"use strict";

const CONFIG = globalThis.__GHIDRA_FRIDA_CONFIG__ || {
  targets: [],
};

function emit(kind, payload) {
  // Frida send() stays inside the attached controller session; controller-owned
  // local artifacts may retain raw values, while tracked docs must redact or
  // generalize them before copying any summary into versioned Markdown.
  send({ script_id: "decomp-compare", kind, payload });
}

function formatRetvalForLocalArtifact(retval) {
  try {
    return retval.toString();
  } catch (error) {
    return `[unavailable:${error}]`;
  }
}

for (const targetConfig of CONFIG.targets) {
  const target = targetConfig.address
    ? ptr(targetConfig.address)
    : Module.getExportByName(targetConfig.module || null, targetConfig.name);
  Interceptor.attach(target, {
    onEnter() {
      emit("enter", {
        name: targetConfig.name || targetConfig.address,
        compare_notes: targetConfig.compareNotes || [],
      });
    },
    onLeave(retval) {
      emit("leave", {
        name: targetConfig.name || targetConfig.address,
        retval: formatRetvalForLocalArtifact(retval),
      });
    },
  });
}
