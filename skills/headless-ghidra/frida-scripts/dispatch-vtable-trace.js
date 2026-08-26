/*
script_id: dispatch-vtable-trace
scenario: dynamic dispatch or vtable observation
invocation_shape: frida -f <target> -l dispatch-vtable-trace.js --runtime=v8 --no-pause
expected_outputs: dispatch-site notes, receiver mappings, resolved targets
coverage_notes: Supports configured dispatch sites and bounded receiver summaries.
*/

"use strict";

const CONFIG = globalThis.__GHIDRA_FRIDA_CONFIG__ || {
  dispatchSites: [],
};

function emit(kind, payload) {
  send({ script_id: "dispatch-vtable-trace", kind, payload });
}

for (const site of CONFIG.dispatchSites) {
  const target = site.address
    ? ptr(site.address)
    : Module.getExportByName(site.module || null, site.name);
  Interceptor.attach(target, {
    onEnter(args) {
      emit("dispatch", {
        site: site.name || site.address,
        receiver: args[0] ? args[0].toString() : "unknown",
      });
    },
  });
}
