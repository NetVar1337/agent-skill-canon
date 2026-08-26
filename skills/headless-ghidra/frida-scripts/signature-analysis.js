/*
script_id: signature-analysis
scenario: function signature analysis
invocation_shape: frida -f <target> -l signature-analysis.js --runtime=v8 --no-pause
expected_outputs: parameter samples, return samples, call counts
coverage_notes: Supports configured exports or addresses and bounded parameter sampling.
*/

"use strict";

const CONFIG = globalThis.__GHIDRA_FRIDA_CONFIG__ || {
  functions: [],
  maxSamplesPerFunction: 32,
};

function emit(kind, payload) {
  send({ script_id: "signature-analysis", kind, payload });
}

for (const entry of CONFIG.functions) {
  const target = entry.address
    ? ptr(entry.address)
    : Module.getExportByName(entry.module || null, entry.name);
  let sampleCount = 0;
  Interceptor.attach(target, {
    onEnter(args) {
      if (sampleCount >= CONFIG.maxSamplesPerFunction) {
        return;
      }
      sampleCount += 1;
      emit("call", {
        name: entry.name || entry.address,
        sample: sampleCount,
        args: entry.argIndices || [],
      });
    },
    onLeave(retval) {
      emit("return", {
        name: entry.name || entry.address,
        sample: sampleCount,
        retval: retval.toString(),
      });
    },
  });
}
