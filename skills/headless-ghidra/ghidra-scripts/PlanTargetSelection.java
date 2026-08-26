// Export a reviewable target-selection plan from current program state.
//
// Script args:
//   1. output_dir
//   2. target_id (optional; defaults to current program name)
//   3. max_targets (optional; defaults to 8)
//
// This script is headless-only and writes deterministic Markdown output to
// <output_dir>/target-selection.md without mutating program metadata.

import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.FunctionManager;
import ghidra.program.model.listing.Parameter;
import ghidra.program.model.symbol.Reference;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class PlanTargetSelection extends GhidraScript {
    private static final String OUTPUT_NAME = "target-selection.md";
    private static final int DEFAULT_MAX_TARGETS = 8;

    private static class TargetRow {
        String selector;
        String functionName;
        String entry;
        String currentSignature;
        String candidateKind;
        boolean importedThunk;
        boolean namedContext;
        int incomingRefs;
        long bodySize;
        String frontierEligibility;
        String frontierBasis;
        String verifiedParentBoundary;
        String relationshipType;
        String triggeringEvidence;
        String secondaryMetrics;
        String metricsNote;
        String autoDefault;
        String selectionReason;
        String frontierReason;
        String questionToAnswer;
        String tieBreakRationale;
        String deviationReason;
        String deviationRisk;
        String status;
    }

    private void ensureDir(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private void writeText(String path, String text) throws IOException {
        FileWriter writer = new FileWriter(path);
        try {
            writer.write(text);
        } finally {
            writer.close();
        }
    }

    private int parsePositiveInt(String rawValue, int defaultValue, String label) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(rawValue.trim());
            if (parsed < 1) {
                throw new NumberFormatException("value must be positive");
            }
            return parsed;
        } catch (NumberFormatException error) {
            throw new RuntimeException("Invalid " + label + ": " + rawValue);
        }
    }

    private String cleanCell(Object value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.toString().trim();
        return cleaned.replace("\n", " ").replace("|", "\\|");
    }

    private String formatTable(String[] headers, List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("| ");
        for (int i = 0; i < headers.length; i++) {
            if (i > 0) {
                sb.append(" | ");
            }
            sb.append(headers[i]);
        }
        sb.append(" |\n| ");
        for (int i = 0; i < headers.length; i++) {
            if (i > 0) {
                sb.append(" | ");
            }
            sb.append("---");
        }
        sb.append(" |\n");
        for (String[] row : rows) {
            sb.append("| ");
            for (int i = 0; i < row.length; i++) {
                if (i > 0) {
                    sb.append(" | ");
                }
                sb.append(cleanCell(row[i]));
            }
            sb.append(" |\n");
        }
        return sb.toString();
    }

    private boolean hasDefaultFunctionName(Function function) {
        String name = function.getName().toLowerCase(Locale.ROOT);
        return name.startsWith("fun_") || name.startsWith("sub_");
    }

    private int countIncomingRefs(Function function) {
        int count = 0;
        for (Reference ignored : getReferencesTo(function.getEntryPoint())) {
            count++;
        }
        return count;
    }

    private String describeParameterList(Function function) {
        Parameter[] parameters = function.getParameters();
        if (parameters.length == 0 && !function.hasVarArgs()) {
            return "void";
        }

        List<String> pieces = new ArrayList<>();
        for (Parameter parameter : parameters) {
            String name = parameter.getName();
            if (name == null || name.trim().isEmpty()) {
                name = "param_" + (pieces.size() + 1);
            }
            pieces.add(name + ":" + parameter.getFormalDataType().getDisplayName());
        }
        if (function.hasVarArgs()) {
            pieces.add("...");
        }
        return String.join("; ", pieces);
    }

    private String describeFunctionSignature(Function function) {
        return "return=" + function.getReturnType().getDisplayName()
            + " | params=" + describeParameterList(function)
            + " | calling=" + function.getCallingConventionName();
    }

    private boolean isImportedThunk(Function function) {
        if (!function.isThunk()) {
            return false;
        }
        Function thunkedFunction = function.getThunkedFunction(false);
        return thunkedFunction != null && thunkedFunction.isExternal();
    }

    private List<TargetRow> collectRows(int maxTargets) {
        List<TargetRow> rows = new ArrayList<>();
        FunctionManager functionManager = currentProgram.getFunctionManager();
        Iterator<Function> iterator = functionManager.getFunctions(true);
        while (iterator.hasNext()) {
            Function function = iterator.next();
            TargetRow row = new TargetRow();
            row.functionName = function.getName();
            row.entry = function.getEntryPoint().toString();
            row.selector = function.getName() + "@" + function.getEntryPoint().toString();
            row.currentSignature = describeFunctionSignature(function);
            row.importedThunk = isImportedThunk(function);
            row.candidateKind = row.importedThunk ? "import_thunk"
                : (function.isThunk() ? "thunk" : "substantive_body");
            row.namedContext = !hasDefaultFunctionName(function);
            row.incomingRefs = countIncomingRefs(function);
            row.bodySize = function.getBody().getNumAddresses();
            row.frontierEligibility = "deferred";
            row.frontierBasis = "child_of_matched_boundary";
            row.verifiedParentBoundary = "pending_matched_boundary_review";
            row.relationshipType = (function.isThunk() || row.importedThunk) ? "wrapper_edge" : "callee";
            if (row.importedThunk) {
                row.triggeringEvidence =
                    "external_import_thunk; defer this row until a substantive internal boundary is unavailable or explicitly required";
            } else if (function.isThunk()) {
                row.triggeringEvidence = "helper boundary candidate; confirm whether this thunk or wrapper is the current frontier";
            } else if (row.namedContext) {
                row.triggeringEvidence = "existing_name_context; confirm whether this row is outermost or a child of a matched boundary";
            } else {
                row.triggeringEvidence = "pending_local_review; link this row to imports, strings, or a matched parent boundary";
            }
            row.secondaryMetrics = "incoming_refs=" + row.incomingRefs + "; body_size=" + row.bodySize;
            row.metricsNote = "secondary_context_only";
            rows.add(row);
        }

        Collections.sort(rows, new Comparator<TargetRow>() {
            @Override
            public int compare(TargetRow left, TargetRow right) {
                if (left.importedThunk != right.importedThunk) {
                    return left.importedThunk ? 1 : -1;
                }
                boolean leftHelper = "thunk".equals(left.candidateKind);
                boolean rightHelper = "thunk".equals(right.candidateKind);
                if (leftHelper != rightHelper) {
                    return leftHelper ? -1 : 1;
                }
                if (left.namedContext != right.namedContext) {
                    return left.namedContext ? -1 : 1;
                }
                return left.entry.compareTo(right.entry);
            }
        });

        if (rows.size() > maxTargets) {
            rows = new ArrayList<>(rows.subList(0, maxTargets));
        }

        int defaultIndex = 0;
        for (int i = 0; i < rows.size(); i++) {
            if (!rows.get(i).importedThunk) {
                defaultIndex = i;
                break;
            }
        }

        for (int i = 0; i < rows.size(); i++) {
            TargetRow row = rows.get(i);
            boolean isDefault = i == defaultIndex;
            row.autoDefault = isDefault ? "yes" : "";
            row.frontierEligibility = isDefault ? "eligible" : "deferred";
            row.frontierBasis = isDefault ? "outermost_anchor" : "child_of_matched_boundary";
            row.verifiedParentBoundary = isDefault ? "none" : "pending_matched_boundary_review";
            row.relationshipType = isDefault
                ? ("thunk".equals(row.candidateKind) || row.importedThunk ? "wrapper_edge" : "entry_adjacent")
                : ("thunk".equals(row.candidateKind) || row.importedThunk ? "wrapper_edge" : "callee");
            if (isDefault && !"thunk".equals(row.candidateKind) && !row.importedThunk) {
                row.candidateKind = "outer_anchor";
            }
            row.selectionReason = isDefault
                ? "auto_default; review whether this row is the current outermost anchor or helper boundary before deeper work"
                : "deferred until the current boundary is matched and this row has a reviewed parent boundary";
            row.frontierReason = isDefault
                ? (row.importedThunk
                    ? "fallback imported thunk candidate; confirm no substantive internal boundary is available before moving inward"
                    : "current outermost anchor candidate; confirm with reviewed evidence before moving inward")
                : "deeper row deferred until a matched parent boundary is recorded";
            row.questionToAnswer = isDefault
                ? "does this frontier row define the next safe outside-in reconstruction boundary?"
                : "which matched parent boundary would authorize this deeper row?";
            row.tieBreakRationale = isDefault
                ? (row.importedThunk
                    ? "fallback_after_substantive_internal_rows"
                    : "helper rows first, then existing name context, then stable address order")
                : "deferred_after_default";
            row.deviationReason = "";
            row.deviationRisk = "";
            row.status = isDefault ? "ready" : "blocked";
        }
        return rows;
    }

    private String buildReport(String targetId, int maxTargets, List<TargetRow> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Target Selection\n\n");
        sb.append("- Target ID: `").append(cleanCell(targetId)).append("`\n");
        sb.append("- Program: `").append(cleanCell(currentProgram.getName())).append("`\n");
        sb.append("- Generated by: `PlanTargetSelection.java`\n");
        sb.append("- Max Targets: `").append(maxTargets).append("`\n\n");

        sb.append("## Stage\n\n");
        sb.append("- Stage: `Target Selection`\n");
        sb.append("- Side Effects: `export_only`\n");
        sb.append("- Notes: this file records one automatic default target for review; the selected row still needs frontier confirmation and only a `matched` boundary can authorize deeper child selection.\n\n");

        sb.append("## Selection Gate\n\n");
        sb.append("- Only outermost anchors are eligible before any boundary is `matched`.\n");
        sb.append("- Only children of a `matched` boundary become eligible for deeper work.\n");
        sb.append("- Imported thunks are fallback-only defaults; prefer substantive internal boundaries when any are available.\n");
        sb.append("- Document any `deviation_reason` and `deviation_risk` before breaking the default frontier order.\n");
        sb.append("- Keep incoming refs and body size visible only as secondary context.\n\n");

        if (!rows.isEmpty()) {
            TargetRow selected = rows.get(0);
            sb.append("## Automatic Default Selection\n\n");
            sb.append("| Field | Value |\n");
            sb.append("| --- | --- |\n");
            sb.append("| Selected Target | `").append(cleanCell(selected.selector)).append("` |\n");
            sb.append("| Selection Mode | `auto_default` |\n");
            sb.append("| Candidate Kind | `").append(cleanCell(selected.candidateKind)).append("` |\n");
            sb.append("| Frontier Reason | `").append(cleanCell(selected.frontierReason)).append("` |\n");
            sb.append("| Selection Reason | `").append(cleanCell(selected.selectionReason)).append("` |\n");
            sb.append("| Question To Answer | `").append(cleanCell(selected.questionToAnswer)).append("` |\n");
            sb.append("| Tie-Break Rationale | `").append(cleanCell(selected.tieBreakRationale)).append("` |\n");
            sb.append("| Metrics Note | `").append(cleanCell(selected.metricsNote)).append("` |\n\n");
        }

        List<String[]> rowValues = new ArrayList<>();
        for (TargetRow row : rows) {
            rowValues.add(new String[]{
                row.autoDefault,
                row.selector,
                row.functionName,
                row.entry,
                row.candidateKind,
                row.frontierEligibility,
                row.frontierBasis,
                row.verifiedParentBoundary,
                row.relationshipType,
                row.triggeringEvidence,
                row.currentSignature,
                row.secondaryMetrics,
                row.metricsNote,
                row.selectionReason,
                row.frontierReason,
                row.questionToAnswer,
                row.tieBreakRationale,
                row.deviationReason,
                row.deviationRisk,
                row.status
            });
        }

        sb.append("## Candidate Selection Rows\n\n");
        sb.append(formatTable(
            new String[]{
                "Auto Default",
                "Function Identity",
                "Function Name",
                "Entry",
                "Candidate Kind",
                "Frontier Eligibility",
                "Frontier Basis",
                "Verified Parent Boundary",
                "Relationship Type",
                "Triggering Evidence",
                "Current Signature",
                "Secondary Metrics",
                "Metrics Note",
                "Selection Reason",
                "Frontier Reason",
                "Question To Answer",
                "Tie-Break Rationale",
                "Deviation Reason",
                "Deviation Risk",
                "Status"
            },
            rowValues
        ));
        sb.append("\n");

        sb.append("## Review Prompts\n\n");
        sb.append("1. Confirm that the automatic default row belongs to the current outside-in frontier.\n");
        sb.append("2. Confirm that any deeper child is linked to a `matched` parent boundary before moving inward.\n");
        sb.append("3. Carry only reviewed frontier rows into `decompiled-output.md`, `renaming-log.md`, or `signature-log.md`.\n");
        return sb.toString();
    }

    @Override
    public void run() throws Exception {
        String[] args = getScriptArgs();
        if (args.length < 1) {
            printerr("Usage: PlanTargetSelection.java <output_dir> [target_id] [max_targets]");
            throw new RuntimeException("Missing output_dir argument");
        }

        String outputDir = args[0];
        String targetId = args.length > 1 ? args[1] : currentProgram.getName();
        int maxTargets =
            args.length > 2 ? parsePositiveInt(args[2], DEFAULT_MAX_TARGETS, "max_targets") : DEFAULT_MAX_TARGETS;
        String outputPath = new File(outputDir, OUTPUT_NAME).getAbsolutePath();

        ensureDir(outputDir);

        List<TargetRow> rows = collectRows(maxTargets);
        writeText(outputPath, buildReport(targetId, maxTargets, rows));
    }
}
