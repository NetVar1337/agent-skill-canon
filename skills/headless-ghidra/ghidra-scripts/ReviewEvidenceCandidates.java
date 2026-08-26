// Export reviewable evidence candidates from the current program state.
//
// Script args:
//   1. output_dir
//   2. target_id (optional; defaults to current program name)
//   3. max_candidates (optional; defaults to 20)
//   4. max_strings (optional; defaults to 12)
//
// This script is headless-only and writes deterministic Markdown output to
// <output_dir>/evidence-candidates.md without mutating program metadata.

import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.Data;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.FunctionManager;
import ghidra.program.model.listing.Parameter;
import ghidra.program.model.symbol.Reference;
import ghidra.program.model.symbol.Symbol;
import ghidra.program.model.symbol.SymbolTable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class ReviewEvidenceCandidates extends GhidraScript {
    private static final String OUTPUT_NAME = "evidence-candidates.md";
    private static final int DEFAULT_MAX_CANDIDATES = 20;
    private static final int DEFAULT_MAX_STRINGS = 12;

    private static class FunctionCandidate {
        String name;
        String entry;
        String kind;
        boolean importedThunk;
        String namespace;
        String currentSignature;
        int incomingRefs;
        long bodySize;
        boolean namedContext;
        String frontierEligibility;
        String frontierBasis;
        String relationshipType;
        String triggeringEvidence;
        String secondaryMetrics;
        String metricsNote;
    }

    private static class StringCandidate {
        String address;
        String preview;
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
        String name = function.getName();
        String lowerName = name.toLowerCase(Locale.ROOT);
        return lowerName.startsWith("fun_") || lowerName.startsWith("sub_");
    }

    private int countIncomingRefs(Function function) {
        int count = 0;
        for (Reference ignored : getReferencesTo(function.getEntryPoint())) {
            count++;
        }
        return count;
    }

    private String namespaceName(Function function) {
        Symbol symbol = function.getSymbol();
        if (symbol == null || symbol.getParentNamespace() == null) {
            return "global";
        }
        return symbol.getParentNamespace().getName(true);
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

    private List<FunctionCandidate> collectFunctionCandidates(int limit) {
        List<FunctionCandidate> candidates = new ArrayList<>();
        FunctionManager manager = currentProgram.getFunctionManager();
        Iterator<Function> iterator = manager.getFunctions(true);
        while (iterator.hasNext()) {
            Function function = iterator.next();
            FunctionCandidate candidate = new FunctionCandidate();
            candidate.name = function.getName();
            candidate.entry = function.getEntryPoint().toString();
            candidate.importedThunk = isImportedThunk(function);
            candidate.kind = candidate.importedThunk ? "import_thunk"
                : (function.isThunk() ? "thunk" : "substantive_body");
            candidate.namespace = namespaceName(function);
            candidate.currentSignature = describeFunctionSignature(function);
            candidate.incomingRefs = countIncomingRefs(function);
            candidate.bodySize = function.getBody().getNumAddresses();
            candidate.namedContext = !hasDefaultFunctionName(function);
            candidate.frontierEligibility = "deferred";
            candidate.frontierBasis = "child_of_matched_boundary";
            candidate.relationshipType = (function.isThunk() || candidate.importedThunk) ? "wrapper_edge" : "callee";
            if (candidate.importedThunk) {
                candidate.triggeringEvidence =
                    "external_import_thunk; defer this row until a substantive internal boundary is unavailable or explicitly required";
            } else if (function.isThunk()) {
                candidate.triggeringEvidence = "thunk_edge; confirm whether this helper is the current frontier boundary";
            } else if (candidate.namedContext) {
                candidate.triggeringEvidence = "existing_name_context; confirm whether this row is outermost or a child of a matched boundary";
            } else {
                candidate.triggeringEvidence = "pending_local_review; link this row to imports, strings, or a matched parent boundary";
            }
            candidate.secondaryMetrics = "incoming_refs=" + candidate.incomingRefs + "; body_size=" + candidate.bodySize;
            candidate.metricsNote = "secondary_context_only";
            candidates.add(candidate);
        }

        Collections.sort(candidates, new Comparator<FunctionCandidate>() {
            @Override
            public int compare(FunctionCandidate left, FunctionCandidate right) {
                if (left.importedThunk != right.importedThunk) {
                    return left.importedThunk ? 1 : -1;
                }
                boolean leftHelper = "thunk".equals(left.kind);
                boolean rightHelper = "thunk".equals(right.kind);
                if (leftHelper != rightHelper) {
                    return leftHelper ? -1 : 1;
                }
                if (left.namedContext != right.namedContext) {
                    return left.namedContext ? -1 : 1;
                }
                return left.entry.compareTo(right.entry);
            }
        });

        if (candidates.size() > limit) {
            candidates = new ArrayList<>(candidates.subList(0, limit));
        }
        FunctionCandidate first = null;
        for (FunctionCandidate candidate : candidates) {
            if (!candidate.importedThunk) {
                first = candidate;
                break;
            }
        }
        if (first == null && !candidates.isEmpty()) {
            first = candidates.get(0);
        }
        if (first != null) {
            first.frontierEligibility = "eligible";
            first.frontierBasis = "outermost_anchor";
            first.relationshipType = ("thunk".equals(first.kind) || first.importedThunk) ? "wrapper_edge" : "entry_adjacent";
            if (!"thunk".equals(first.kind) && !first.importedThunk) {
                first.kind = "outer_anchor";
            }
            if (first.importedThunk) {
                first.triggeringEvidence =
                    "fallback imported thunk candidate; confirm no substantive internal boundary is available before promoting this row";
            } else {
                first.triggeringEvidence =
                    "default frontier candidate; confirm this outermost row before treating deeper children as eligible";
            }
        }
        return candidates;
    }

    private List<StringCandidate> collectStringCandidates(int limit) {
        List<StringCandidate> results = new ArrayList<>();
        Iterator<Data> iterator = currentProgram.getListing().getDefinedData(true);
        while (iterator.hasNext() && results.size() < limit) {
            Data data = iterator.next();
            try {
                if (!data.hasStringValue()) {
                    continue;
                }
                Object value = data.getValue();
                if (value == null) {
                    continue;
                }
                String preview = value.toString().trim();
                if (preview.isEmpty()) {
                    continue;
                }
                if (preview.length() > 96) {
                    preview = preview.substring(0, 96) + "...";
                }
                StringCandidate candidate = new StringCandidate();
                candidate.address = data.getAddress().toString();
                candidate.preview = preview;
                results.add(candidate);
            } catch (Exception ignored) {
                // Continue collecting reviewable strings even if one entry is malformed.
            }
        }
        return results;
    }

    private List<String[]> collectImportRows(int limit) {
        List<String[]> rows = new ArrayList<>();
        SymbolTable symbolTable = currentProgram.getSymbolTable();
        Iterator<Symbol> iterator = symbolTable.getExternalSymbols();
        while (iterator.hasNext() && rows.size() < limit) {
            Symbol symbol = iterator.next();
            Symbol parent = symbol.getParentSymbol();
            String libraryName = parent != null ? parent.getName() : "EXTERNAL";
            rows.add(new String[]{
                libraryName,
                symbol.getName(),
                symbol.getAddress().toString()
            });
        }
        return rows;
    }

    private String buildReport(
        String targetId,
        int maxCandidates,
        int maxStrings,
        List<FunctionCandidate> functions,
        List<StringCandidate> strings,
        List<String[]> imports
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Evidence Candidates\n\n");
        sb.append("- Target ID: `").append(cleanCell(targetId)).append("`\n");
        sb.append("- Program: `").append(cleanCell(currentProgram.getName())).append("`\n");
        sb.append("- Generated by: `ReviewEvidenceCandidates.java`\n");
        sb.append("- Max Candidates: `").append(maxCandidates).append("`\n");
        sb.append("- Max Strings: `").append(maxStrings).append("`\n\n");

        sb.append("## Stage\n\n");
        sb.append("- Stage: `Evidence Review`\n");
        sb.append("- Side Effects: `export_only`\n");
        sb.append("- Notes: candidate rows support frontier review only; visible metrics remain secondary context and do not decide the default next step.\n\n");

        sb.append("## Frontier Review Notes\n\n");
        sb.append("| Heuristic | Meaning |\n");
        sb.append("| --- | --- |\n");
        sb.append("| `candidate_kind` | Use helper-like rows to check whether a wrapper, thunk, or dispatch helper is the current frontier boundary. |\n");
        sb.append("| `import_thunk` | Treat imported thunks as fallback-only rows; prefer substantive internal boundaries when available. |\n");
        sb.append("| `triggering_evidence` | Link every eligible row to imports, strings, or a matched parent boundary before promoting it. |\n");
        sb.append("| `secondary_metrics` | Incoming refs and body size stay visible as context only; they never authorize progression on their own. |\n\n");

        List<String[]> functionRows = new ArrayList<>();
        for (FunctionCandidate candidate : functions) {
            functionRows.add(new String[]{
                candidate.name,
                candidate.entry,
                candidate.kind,
                candidate.namespace,
                candidate.currentSignature,
                candidate.frontierEligibility,
                candidate.frontierBasis,
                candidate.relationshipType,
                candidate.triggeringEvidence,
                candidate.secondaryMetrics,
                candidate.metricsNote
            });
        }
        sb.append("## Frontier Candidate Rows\n\n");
        sb.append(formatTable(
            new String[]{
                "Function",
                "Entry",
                "Candidate Kind",
                "Namespace",
                "Current Signature",
                "Frontier Eligibility",
                "Frontier Basis",
                "Relationship Type",
                "Triggering Evidence",
                "Secondary Metrics",
                "Metrics Note"
            },
            functionRows
        ));
        sb.append("\n");

        List<String[]> stringRows = new ArrayList<>();
        for (StringCandidate candidate : strings) {
            stringRows.add(new String[]{candidate.address, candidate.preview});
        }
        sb.append("## Context Strings\n\n");
        if (stringRows.isEmpty()) {
            sb.append("- No string values were recovered in the requested scope.\n\n");
        } else {
            sb.append(formatTable(new String[]{"Address", "Preview"}, stringRows));
            sb.append("\n");
        }

        sb.append("## External Symbol Snapshot\n\n");
        if (imports.isEmpty()) {
            sb.append("- No external symbols were recovered in the requested scope.\n\n");
        } else {
            sb.append(formatTable(new String[]{"Library", "Symbol", "Address"}, imports));
            sb.append("\n");
        }

        sb.append("## Recommended Review Prompts\n\n");
        sb.append("1. Confirm which rows belong to the current outside-in frontier instead of trusting metric-heavy rows by default.\n");
        sb.append("2. Confirm whether a matched boundary exists before treating any deeper child as eligible.\n");
        sb.append("3. Promote only reviewed frontier rows into `target-selection.md` and keep metrics secondary.\n");
        return sb.toString();
    }

    @Override
    public void run() throws Exception {
        String[] args = getScriptArgs();
        if (args.length < 1) {
            printerr("Usage: ReviewEvidenceCandidates.java <output_dir> [target_id] [max_candidates] [max_strings]");
            throw new RuntimeException("Missing output_dir argument");
        }

        String outputDir = args[0];
        String targetId = args.length > 1 ? args[1] : currentProgram.getName();
        int maxCandidates =
            args.length > 2 ? parsePositiveInt(args[2], DEFAULT_MAX_CANDIDATES, "max_candidates") : DEFAULT_MAX_CANDIDATES;
        int maxStrings =
            args.length > 3 ? parsePositiveInt(args[3], DEFAULT_MAX_STRINGS, "max_strings") : DEFAULT_MAX_STRINGS;
        String outputPath = new File(outputDir, OUTPUT_NAME).getAbsolutePath();

        ensureDir(outputDir);

        List<FunctionCandidate> functions = collectFunctionCandidates(maxCandidates);
        List<StringCandidate> strings = collectStringCandidates(maxStrings);
        List<String[]> imports = collectImportRows(Math.min(maxCandidates, 12));

        writeText(
            outputPath,
            buildReport(targetId, maxCandidates, maxStrings, functions, strings, imports)
        );
    }
}
