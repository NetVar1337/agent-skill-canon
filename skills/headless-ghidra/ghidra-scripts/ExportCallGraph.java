// Export a focused detailed call-graph report from current analysis state.
//
// Script args:
//   1. output_dir
//   2. target_id (optional; defaults to current program name)
//   3. max_edge_rows (optional; defaults to 200)
//   4. max_summary_rows (optional; defaults to 80)
//
// This script is headless-only and writes deterministic Markdown output to
// <output_dir>/call-graph-detail.md without mutating program metadata.

import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.Address;
import ghidra.program.model.address.AddressIterator;
import ghidra.program.model.address.AddressRange;
import ghidra.program.model.address.AddressRangeIterator;
import ghidra.program.model.address.AddressSetView;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.FunctionManager;
import ghidra.program.model.symbol.ExternalLocation;
import ghidra.program.model.symbol.ExternalLocationIterator;
import ghidra.program.model.symbol.Reference;
import ghidra.program.model.symbol.ReferenceIterator;
import ghidra.program.model.symbol.ReferenceManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ExportCallGraph extends GhidraScript {
    private static final String OUTPUT_NAME = "call-graph-detail.md";
    private static final int DEFAULT_MAX_EDGE_ROWS = 200;
    private static final int DEFAULT_MAX_SUMMARY_ROWS = 80;

    private static class EdgeRow {
        String callerName;
        String callerEntry;
        String calleeKind;
        String calleeName;
        String calleeEntry;
        String refType;
        int callSiteCount;
    }

    private static class SummaryRow {
        String functionName;
        String entry;
        long bodySize;
        int incomingCallRefs;
        int uniqueOutgoingTargets;
        int outgoingCallSites;
        String nameStatus;
    }

    private static class EdgeTarget {
        String kind;
        String name;
        String entry;
    }

    private static class CallGraphData {
        List<EdgeRow> edges = new ArrayList<>();
        List<SummaryRow> summaries = new ArrayList<>();
        int totalCallSites;
        int internalEdges;
        int externalEdges;
        int unresolvedEdges;
    }

    @Override
    public void run() throws Exception {
        String[] args = getScriptArgs();
        if (args.length < 1) {
            printerr("Usage: ExportCallGraph.java <output_dir> [target_id] [max_edge_rows] [max_summary_rows]");
            throw new RuntimeException("Missing output_dir argument");
        }

        String outputDir = args[0];
        String targetId = args.length > 1 ? args[1] : currentProgram.getName();
        int maxEdgeRows = parsePositiveInt(
            args.length > 2 ? args[2] : null,
            DEFAULT_MAX_EDGE_ROWS,
            "max_edge_rows"
        );
        int maxSummaryRows = parsePositiveInt(
            args.length > 3 ? args[3] : null,
            DEFAULT_MAX_SUMMARY_ROWS,
            "max_summary_rows"
        );

        ensureDir(outputDir);

        List<Function> functions = collectFunctions();
        CallGraphData data = collectCallGraphData(functions);
        String report = buildReport(targetId, maxEdgeRows, maxSummaryRows, functions.size(), data);
        File outputFile = new File(outputDir, OUTPUT_NAME);
        writeText(outputFile.getAbsolutePath(), report);
        println("Detailed call graph exported to: " + outputFile.getAbsolutePath());
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

    private List<Function> collectFunctions() {
        List<Function> functions = new ArrayList<>();
        Iterator<Function> iterator = currentProgram.getFunctionManager().getFunctions(true);
        while (iterator.hasNext()) {
            functions.add(iterator.next());
        }
        Collections.sort(functions, new Comparator<Function>() {
            @Override
            public int compare(Function left, Function right) {
                return left.getEntryPoint().toString().compareTo(right.getEntryPoint().toString());
            }
        });
        return functions;
    }

    private EdgeTarget resolveTarget(FunctionManager functionManager, Reference reference) {
        EdgeTarget target = new EdgeTarget();
        Function callee = functionManager.getFunctionContaining(reference.getToAddress());
        if (callee != null) {
            target.kind = "function";
            target.name = callee.getName();
            target.entry = callee.getEntryPoint().toString();
            return target;
        }

        ExternalLocationIterator externalLocations = currentProgram
            .getExternalManager()
            .getExternalLocations(reference.getToAddress());
        if (externalLocations.hasNext()) {
            ExternalLocation externalLocation = externalLocations.next();
            target.kind = "external";
            target.name = choosePreferredName(externalLocation.getLabel(), reference.getToAddress().toString());
            target.entry = reference.getToAddress().toString();
            return target;
        }

        target.kind = "unresolved";
        target.name = "unresolved_target";
        target.entry = reference.getToAddress().toString();
        return target;
    }

    private String choosePreferredName(String candidate, String fallback) {
        if (candidate == null || candidate.trim().isEmpty()) {
            return fallback;
        }
        return candidate.trim();
    }

    private int countIncomingCallRefs(Function function) {
        int count = 0;
        ReferenceIterator references = currentProgram
            .getReferenceManager()
            .getReferencesTo(function.getEntryPoint());
        while (references.hasNext()) {
            Reference reference = references.next();
            if (reference.getReferenceType().isCall()) {
                count++;
            }
        }
        return count;
    }

    private CallGraphData collectCallGraphData(List<Function> functions) {
        CallGraphData data = new CallGraphData();
        Map<String, EdgeRow> edgeMap = new HashMap<>();
        FunctionManager functionManager = currentProgram.getFunctionManager();
        ReferenceManager referenceManager = currentProgram.getReferenceManager();

        for (Function function : functions) {
            SummaryRow summary = new SummaryRow();
            summary.functionName = function.getName();
            summary.entry = function.getEntryPoint().toString();
            summary.bodySize = function.getBody().getNumAddresses();
            summary.incomingCallRefs = countIncomingCallRefs(function);
            summary.nameStatus = hasDefaultFunctionName(function) ? "default" : "analyst_named_or_imported";

            Set<String> uniqueOutgoingTargets = new HashSet<>();
            AddressSetView body = function.getBody();
            AddressIterator sources = referenceManager.getReferenceSourceIterator(body, true);
            while (sources.hasNext()) {
                Address source = sources.next();
                if (!body.contains(source)) {
                    continue;
                }
                Reference[] references = referenceManager.getReferencesFrom(source);
                for (Reference reference : references) {
                    if (!reference.getReferenceType().isCall()) {
                        continue;
                    }

                    EdgeTarget target = resolveTarget(functionManager, reference);
                    String edgeKey = function.getEntryPoint().toString()
                        + "|"
                        + target.kind
                        + "|"
                        + target.entry
                        + "|"
                        + reference.getReferenceType().toString();

                    EdgeRow edge = edgeMap.get(edgeKey);
                    if (edge == null) {
                        edge = new EdgeRow();
                        edge.callerName = function.getName();
                        edge.callerEntry = function.getEntryPoint().toString();
                        edge.calleeKind = target.kind;
                        edge.calleeName = target.name;
                        edge.calleeEntry = target.entry;
                        edge.refType = reference.getReferenceType().toString();
                        edge.callSiteCount = 0;
                        edgeMap.put(edgeKey, edge);

                        if ("function".equals(target.kind)) {
                            data.internalEdges++;
                        } else if ("external".equals(target.kind)) {
                            data.externalEdges++;
                        } else {
                            data.unresolvedEdges++;
                        }
                    }

                    edge.callSiteCount++;
                    summary.outgoingCallSites++;
                    data.totalCallSites++;
                    uniqueOutgoingTargets.add(target.kind + "|" + target.entry);
                }
            }

            summary.uniqueOutgoingTargets = uniqueOutgoingTargets.size();
            data.summaries.add(summary);
        }

        data.edges.addAll(edgeMap.values());

        Collections.sort(data.edges, new Comparator<EdgeRow>() {
            @Override
            public int compare(EdgeRow left, EdgeRow right) {
                int byCaller = left.callerEntry.compareTo(right.callerEntry);
                if (byCaller != 0) {
                    return byCaller;
                }
                int byKind = left.calleeKind.compareTo(right.calleeKind);
                if (byKind != 0) {
                    return byKind;
                }
                int byCallee = left.calleeEntry.compareTo(right.calleeEntry);
                if (byCallee != 0) {
                    return byCallee;
                }
                return left.refType.compareTo(right.refType);
            }
        });

        Collections.sort(data.summaries, new Comparator<SummaryRow>() {
            @Override
            public int compare(SummaryRow left, SummaryRow right) {
                if (left.outgoingCallSites != right.outgoingCallSites) {
                    return Integer.compare(right.outgoingCallSites, left.outgoingCallSites);
                }
                if (left.incomingCallRefs != right.incomingCallRefs) {
                    return Integer.compare(right.incomingCallRefs, left.incomingCallRefs);
                }
                if (left.bodySize != right.bodySize) {
                    return Long.compare(right.bodySize, left.bodySize);
                }
                return left.entry.compareTo(right.entry);
            }
        });

        return data;
    }

    private String buildReport(
        String targetId,
        int maxEdgeRows,
        int maxSummaryRows,
        int totalFunctionCount,
        CallGraphData data
    ) {
        List<EdgeRow> edgeRows = data.edges;
        List<SummaryRow> summaryRows = data.summaries;
        int edgeRowsWritten = Math.min(edgeRows.size(), maxEdgeRows);
        int summaryRowsWritten = Math.min(summaryRows.size(), maxSummaryRows);

        StringBuilder sb = new StringBuilder();
        sb.append("# Detailed Call Graph\n\n");
        sb.append("- Target ID: `").append(cleanCell(targetId)).append("`\n");
        sb.append("- Program: `").append(cleanCell(currentProgram.getName())).append("`\n");
        sb.append("- Generated by: `ExportCallGraph.java`\n");
        sb.append("- Max Edge Rows: `").append(maxEdgeRows).append("`\n");
        sb.append("- Max Summary Rows: `").append(maxSummaryRows).append("`\n\n");

        sb.append("## Stage\n\n");
        sb.append("- Stage: `Baseline Evidence Follow-Up`\n");
        sb.append("- Side Effects: `export_only`\n");
        sb.append("- Notes: use this focused export when `xrefs-and-callgraph.md` is too coarse to justify outside-in traversal or target selection.\n\n");

        sb.append("## Export Summary\n\n");
        sb.append("- Unique call edges observed: `").append(edgeRows.size()).append("`\n");
        sb.append("- Total call sites observed: `").append(data.totalCallSites).append("`\n");
        sb.append("- Internal callee edges: `").append(data.internalEdges).append("`\n");
        sb.append("- External callee edges: `").append(data.externalEdges).append("`\n");
        sb.append("- Unresolved callee edges: `").append(data.unresolvedEdges).append("`\n");
        sb.append("- Edge rows written: `").append(edgeRowsWritten).append("/").append(edgeRows.size()).append("`\n");
        sb.append("- Function summary rows written: `").append(summaryRowsWritten).append("/").append(totalFunctionCount).append("`\n");
        sb.append("- Edge output truncated: `").append(edgeRowsWritten < edgeRows.size() ? "yes" : "no").append("`\n");
        sb.append("- Summary output truncated: `").append(summaryRowsWritten < totalFunctionCount ? "yes" : "no").append("`\n\n");

        sb.append("## Review Rules\n\n");
        sb.append("- Use this file only after baseline evidence has established the surrounding imports, strings, and coarse xref anchors.\n");
        sb.append("- Treat `external` and `unresolved` targets as follow-up prompts rather than confirmed semantics.\n");
        sb.append("- Re-run this export after meaningful rename or signature updates if call relationships still drive the next step.\n\n");

        sb.append("## Caller To Callee Relationships\n\n");
        if (edgeRows.isEmpty()) {
            sb.append("_No call edges were observed from the current program state._\n\n");
        } else {
            List<String[]> renderedEdges = new ArrayList<>();
            for (int i = 0; i < edgeRowsWritten; i++) {
                EdgeRow row = edgeRows.get(i);
                renderedEdges.add(new String[]{
                    row.callerName,
                    row.callerEntry,
                    row.calleeKind,
                    row.calleeName,
                    row.calleeEntry,
                    row.refType,
                    String.valueOf(row.callSiteCount)
                });
            }
            sb.append(formatTable(
                new String[]{
                    "Caller",
                    "Caller Entry",
                    "Callee Kind",
                    "Callee",
                    "Callee Entry",
                    "Ref Type",
                    "Call Sites"
                },
                renderedEdges
            ));
            sb.append("\n");
        }

        sb.append("## Function Summary\n\n");
        if (summaryRows.isEmpty()) {
            sb.append("_No functions were available for call-graph summarization._\n");
            return sb.toString();
        }

        List<String[]> renderedSummary = new ArrayList<>();
        for (int i = 0; i < summaryRowsWritten; i++) {
            SummaryRow row = summaryRows.get(i);
            renderedSummary.add(new String[]{
                row.functionName,
                row.entry,
                String.valueOf(row.bodySize),
                String.valueOf(row.incomingCallRefs),
                String.valueOf(row.uniqueOutgoingTargets),
                String.valueOf(row.outgoingCallSites),
                row.nameStatus
            });
        }
        sb.append(formatTable(
            new String[]{
                "Function",
                "Entry",
                "Body Size",
                "Incoming Call Refs",
                "Unique Outgoing Targets",
                "Outgoing Call Sites",
                "Name Status"
            },
            renderedSummary
        ));
        sb.append("\n");
        return sb.toString();
    }
}
