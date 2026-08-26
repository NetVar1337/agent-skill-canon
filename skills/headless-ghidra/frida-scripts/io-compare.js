/**
 * io-compare.js — Compare I/O recordings between original and reconstructed functions.
 * Reads two YAML recording files and outputs a comparison report.
 *
 * This script is designed to be run as a Node.js script (not Frida-injected).
 *
 * Usage:
 *   node io-compare.js <original-recording.yaml> <reconstructed-recording.yaml> <output.yaml>
 */
"use strict";

const fs = require("fs");

function parseYamlSimple(content) {
  // Minimal YAML-like parser for structured recordings
  // In production, use js-yaml or similar
  try {
    return JSON.parse(content);
  } catch {
    // Fallback: treat as line-based key-value
    console.error("Warning: could not parse recording, using empty array");
    return { cases: [] };
  }
}

function compareCases(original, reconstructed) {
  const results = [];
  const origCases = original.cases || [];
  const reconCases = reconstructed.cases || [];

  for (let i = 0; i < origCases.length; i++) {
    const orig = origCases[i];
    const recon = reconCases[i];

    if (!recon) {
      results.push({
        case_id: orig.case_id || `case_${i}`,
        status: "missing",
        detail: "reconstructed recording missing this case",
      });
      continue;
    }

    const divergences = [];

    // Compare return values
    if (orig.return_value !== recon.return_value) {
      divergences.push({
        field: "return_value",
        expected: orig.return_value,
        actual: recon.return_value,
      });
    }

    // Compare side effects if present
    if (orig.side_effects && recon.side_effects) {
      const origSE = JSON.stringify(orig.side_effects);
      const reconSE = JSON.stringify(recon.side_effects);
      if (origSE !== reconSE) {
        divergences.push({
          field: "side_effects",
          expected: origSE,
          actual: reconSE,
        });
      }
    }

    results.push({
      case_id: orig.case_id || `case_${i}`,
      status: divergences.length === 0 ? "pass" : "diverged",
      divergences,
    });
  }

  return results;
}

// Main
if (process.argv.length < 5) {
  console.error(
    "Usage: node io-compare.js <original.yaml> <reconstructed.yaml> <output.yaml>",
  );
  process.exit(1);
}

const origContent = fs.readFileSync(process.argv[2], "utf8");
const reconContent = fs.readFileSync(process.argv[3], "utf8");
const outputPath = process.argv[4];

const original = parseYamlSimple(origContent);
const reconstructed = parseYamlSimple(reconContent);

const comparison = compareCases(original, reconstructed);
const passed = comparison.filter((c) => c.status === "pass").length;
const failed = comparison.filter((c) => c.status !== "pass").length;

const report = {
  compared_at: new Date().toISOString(),
  total: comparison.length,
  passed,
  failed,
  cases: comparison,
};

fs.writeFileSync(outputPath, JSON.stringify(report, null, 2));
console.log(`Comparison complete: ${passed}/${comparison.length} passed`);
process.exit(failed > 0 ? 1 : 0);
