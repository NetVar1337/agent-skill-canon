/**
 * io-capture.js — Generic Frida I/O capture script.
 * Hooks a target function, records arguments and return values for each call.
 *
 * Usage:
 *   frida -l io-capture.js -p <pid> -- <function_address> <num_args>
 *
 * Output: JSON lines to stdout, one per function call.
 */
"use strict";

const targetAddr = ptr(Process.argv[0] || "0x0");
const numArgs = parseInt(Process.argv[1] || "0", 10);

const recordings = [];
let callIndex = 0;

Interceptor.attach(targetAddr, {
  onEnter(args) {
    this.callId = callIndex++;
    this.entryArgs = [];
    for (let i = 0; i < numArgs; i++) {
      this.entryArgs.push(args[i].toString());
    }
    this.timestamp = Date.now();
  },

  onLeave(retval) {
    const record = {
      call_id: this.callId,
      timestamp: this.timestamp,
      address: targetAddr.toString(),
      arguments: this.entryArgs,
      return_value: retval.toString(),
      return_value_int: retval.toInt32(),
    };
    recordings.push(record);
    send({ type: "io_capture", data: record });
  },
});

// Flush all recordings on detach
rpc.exports = {
  getRecordings() {
    return recordings;
  },
  getRecordingCount() {
    return recordings.length;
  },
};
