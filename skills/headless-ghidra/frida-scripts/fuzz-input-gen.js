/**
 * fuzz-input-gen.js — Generate fuzz test inputs based on function signature.
 * Reads a function's decompilation-record.yaml to extract parameter types,
 * then generates boundary and edge-case inputs.
 *
 * This script is designed to be run as a Node.js script.
 *
 * Usage:
 *   node fuzz-input-gen.js <decompilation-record.yaml> <output.yaml>
 */
"use strict";

const fs = require("fs");

// Type-aware boundary value generators
const generators = {
  int: () => [0, 1, -1, 2147483647, -2147483648, 42, 255, 256],
  uint: () => [0, 1, 4294967295, 255, 256, 65535, 65536],
  int8_t: () => [0, 1, -1, 127, -128],
  uint8_t: () => [0, 1, 127, 128, 255],
  int16_t: () => [0, 1, -1, 32767, -32768],
  uint16_t: () => [0, 1, 32767, 32768, 65535],
  int32_t: () => [0, 1, -1, 2147483647, -2147483648],
  uint32_t: () => [0, 1, 2147483647, 2147483648, 4294967295],
  int64_t: () => [0, 1, -1, "9223372036854775807", "-9223372036854775808"],
  uint64_t: () => [0, 1, "18446744073709551615"],
  size_t: () => [0, 1, 255, 256, 65535, 65536, 1048576],
  "char*": () => ['""', '"hello"', '"A".repeat(256)', '"\\x00"', "null"],
  "void*": () => ["null", '"0x41414141"', '"0x0"'],
  bool: () => [0, 1],
  float: () => [0.0, 1.0, -1.0, 3.14159, "Infinity", "-Infinity", "NaN"],
  double: () => [
    0.0,
    1.0,
    -1.0,
    3.14159265358979,
    "Infinity",
    "-Infinity",
    "NaN",
  ],
};

function getGeneratorForType(typeStr) {
  const normalized = typeStr.replace(/\s+/g, "").toLowerCase();

  for (const [key, gen] of Object.entries(generators)) {
    if (normalized.includes(key.toLowerCase())) {
      return gen;
    }
  }

  // Pointer types
  if (normalized.includes("*")) {
    return generators["void*"];
  }

  // Default: treat as int
  return generators["int"];
}

function generateInputCombinations(params, maxCases = 20) {
  if (params.length === 0) return [{ args: [] }];

  const cases = [];
  const paramValues = params.map((p) => {
    const gen = getGeneratorForType(p.type || "int");
    return gen();
  });

  // Generate boundary cases for each param (others at default)
  for (let pi = 0; pi < params.length; pi++) {
    for (const val of paramValues[pi]) {
      if (cases.length >= maxCases) break;
      const args = paramValues.map((pv, i) => (i === pi ? val : pv[0]));
      cases.push({
        case_id: `fuzz_${String(cases.length + 1).padStart(3, "0")}`,
        source: "fuzz_generated",
        args: args.map((v, i) => ({
          name: params[i].name || `arg${i}`,
          type: params[i].type || "unknown",
          value: v,
        })),
      });
    }
  }

  return cases;
}

// Main
if (process.argv.length < 4) {
  console.error(
    "Usage: node fuzz-input-gen.js <decompilation-record.yaml> <output.yaml>",
  );
  process.exit(1);
}

const recordPath = process.argv[2];
const outputPath = process.argv[3];

let record;
try {
  const content = fs.readFileSync(recordPath, "utf8");
  record = JSON.parse(content);
} catch {
  console.error(`Warning: could not parse ${recordPath}, using empty params`);
  record = { parameters: [] };
}

const params = record.parameters || [];
const cases = generateInputCombinations(params);

const output = {
  generated_at: new Date().toISOString(),
  function_id: record.function_id || "unknown",
  function_name: record.function_name || "unknown",
  total_cases: cases.length,
  cases,
};

fs.writeFileSync(outputPath, JSON.stringify(output, null, 2));
console.log(`Generated ${cases.length} fuzz cases for ${output.function_name}`);
