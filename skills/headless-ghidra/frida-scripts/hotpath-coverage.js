/*
script_id: hotpath-coverage
scenario: hot-path or coverage observation
invocation_shape: frida -f <target> -l hotpath-coverage.js --runtime=v8 --no-pause
expected_outputs: counters, branch-hit summaries, hot-path ranking
coverage_notes: Supports configured functions and bounded counter aggregation.
*/

"use strict";

const CONFIG = globalThis.__GHIDRA_FRIDA_CONFIG__ || {
  functions: [],
};

const counters = new Map();

function emit(kind, payload) {
  send({ script_id: "hotpath-coverage", kind, payload });
}

for (const entry of CONFIG.functions) {
  const target = entry.address
    ? ptr(entry.address)
    : Module.getExportByName(entry.module || null, entry.name);
  counters.set(entry.name || entry.address, 0);
  Interceptor.attach(target, {
    onEnter() {
      const key = entry.name || entry.address;
      counters.set(key, (counters.get(key) || 0) + 1);
      emit("counter", {
        function: key,
        count: counters.get(key),
      });
    },
  });
}
