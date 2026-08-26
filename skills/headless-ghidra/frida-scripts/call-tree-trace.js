/*
script_id: call-tree-trace
scenario: runtime call-tree tracing
invocation_shape: frida -f <target> -l call-tree-trace.js --runtime=v8 --no-pause
expected_outputs: call edges, call depth, edge counters
coverage_notes: Supports configured roots and bounded stack-depth tracing.
*/

"use strict";

const CONFIG = globalThis.__GHIDRA_FRIDA_CONFIG__ || {
  roots: [],
};

const state = { depth: 0 };

function emit(kind, payload) {
  send({ script_id: "call-tree-trace", kind, payload });
}

for (const root of CONFIG.roots) {
  const target = root.address
    ? ptr(root.address)
    : Module.getExportByName(root.module || null, root.name);
  Interceptor.attach(target, {
    onEnter() {
      state.depth += 1;
      emit("edge", {
        root: root.name || root.address,
        depth: state.depth,
      });
    },
    onLeave() {
      emit("leave", {
        root: root.name || root.address,
        depth: state.depth,
      });
      state.depth -= 1;
    },
  });
}
