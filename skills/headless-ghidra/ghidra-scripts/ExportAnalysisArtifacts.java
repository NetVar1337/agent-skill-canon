// Export standard Markdown artifacts from a headless Ghidra project.
//
// Script args:
//   1. output_dir
//   2. target_id (optional)
//   3. mode: baseline | decompile (optional; defaults to baseline)
//   4+. selected function identifiers for decompile mode (optional)
//
// This script keeps baseline evidence export separate from late-stage selected
// decompilation so the committed Markdown surface matches the staged workflow.

import ghidra.app.decompiler.DecompInterface;
import ghidra.app.decompiler.DecompileResults;
import ghidra.app.decompiler.DecompiledFunction;
import ghidra.app.script.GhidraScript;
import ghidra.program.model.symbol.Symbol;
import ghidra.program.model.symbol.SymbolTable;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.FunctionManager;
import ghidra.program.model.listing.Data;
import ghidra.program.model.data.DataType;
import ghidra.program.model.address.Address;
import ghidra.util.task.ConsoleTaskMonitor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExportAnalysisArtifacts extends GhidraScript {
    private static final String BASELINE_MODE = "baseline";
    private static final String DECOMPILE_MODE = "decompile";

    @Override
    public void run() throws Exception {
        String[] args = getScriptArgs();
        if (args.length < 1) {
            printerr("Usage: ExportAnalysisArtifacts.java <output_dir> [target_id] [mode] [selected_ids...]");
            throw new RuntimeException("Missing output_dir argument");
        }

        String outputDir = args[0];
        String targetId = args.length > 1 ? args[1] : currentProgram.getName();
        String mode = args.length > 2 ? args[2] : BASELINE_MODE;

        ensureDir(outputDir);

        if (BASELINE_MODE.equals(mode)) {
            writeBaselineArtifacts(outputDir, targetId);
        } else if (DECOMPILE_MODE.equals(mode)) {
            List<String> selectedIds = new ArrayList<>();
            for (int i = 3; i < args.length; i++) {
                if (args[i] != null && !args[i].trim().isEmpty()) {
                    selectedIds.add(args[i].trim());
                }
            }
            writeSelectedDecompilation(outputDir, targetId, selectedIds);
        } else {
            printerr("Unsupported mode: " + mode);
            throw new RuntimeException("Unsupported mode: " + mode);
        }
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

    private String cleanCell(Object value) {
        if (value == null) return "";
        return value.toString().replace("\n", " ").replace("|", "\\|");
    }

    private String formatTable(String[] headers, List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("| ");
        for (int i = 0; i < headers.length; i++) {
            if (i > 0) sb.append(" | ");
            sb.append(headers[i]);
        }
        sb.append(" |\n| ");
        for (int i = 0; i < headers.length; i++) {
            if (i > 0) sb.append(" | ");
            sb.append("---");
        }
        sb.append(" |\n");
        for (String[] row : rows) {
            sb.append("| ");
            for (int i = 0; i < row.length; i++) {
                if (i > 0) sb.append(" | ");
                sb.append(row[i]);
            }
            sb.append(" |\n");
        }
        return sb.toString();
    }

    private List<String[]> functionRows(int limit) {
        List<String[]> rows = new ArrayList<>();
        FunctionManager fm = currentProgram.getFunctionManager();
        java.util.Iterator<Function> it = fm.getFunctions(true);
        int count = 0;
        while (it.hasNext() && count < limit) {
            Function func = it.next();
            rows.add(new String[]{
                cleanCell(func.getName()),
                func.getEntryPoint().toString(),
                func.isThunk() ? "thunk" : "body"
            });
            count++;
        }
        return rows;
    }

    private List<String[]> typeRows(int limit) {
        List<String[]> rows = new ArrayList<>();
        java.util.Iterator<DataType> it = currentProgram.getDataTypeManager().getAllDataTypes();
        int count = 0;
        while (it.hasNext() && count < limit) {
            DataType dt = it.next();
            rows.add(new String[]{
                cleanCell(dt.getName()),
                cleanCell(dt.getClass().getSimpleName()),
                cleanCell(dt.getPathName())
            });
            count++;
        }
        return rows;
    }

    private List<String[]> importRows(int limit) {
        List<String[]> rows = new ArrayList<>();
        SymbolTable st = currentProgram.getSymbolTable();
        java.util.Iterator<Symbol> it = st.getExternalSymbols();
        int count = 0;
        while (it.hasNext() && count < limit) {
            Symbol sym = it.next();
            Symbol parent = sym.getParentSymbol();
            String libName = parent != null ? parent.getName() : "EXTERNAL";
            rows.add(new String[]{
                cleanCell(libName),
                cleanCell(sym.getName()),
                sym.getAddress().toString()
            });
            count++;
        }
        return rows;
    }

    private List<String[]> stringRows(int limit) {
        List<String[]> rows = new ArrayList<>();
        java.util.Iterator<Data> it = currentProgram.getListing().getDefinedData(true);
        int count = 0;
        while (it.hasNext() && count < limit) {
            Data data = it.next();
            try {
                if (data.hasStringValue()) {
                    String value = cleanCell(data.getValue());
                    if (value.length() > 80) value = value.substring(0, 80);
                    rows.add(new String[]{data.getAddress().toString(), value});
                    count++;
                }
            } catch (Exception e) {
                // skip
            }
        }
        return rows;
    }

    private List<String[]> xrefRows(int limit) {
        List<String[]> rows = new ArrayList<>();
        FunctionManager fm = currentProgram.getFunctionManager();
        java.util.Iterator<Function> it = fm.getFunctions(true);
        int count = 0;
        while (it.hasNext() && count < limit) {
            Function func = it.next();
            int refCount = 0;
            for (ghidra.program.model.symbol.Reference ref : getReferencesTo(func.getEntryPoint())) {
                refCount++;
            }
            rows.add(new String[]{
                cleanCell(func.getName()),
                func.getEntryPoint().toString(),
                String.valueOf(refCount)
            });
            count++;
        }
        return rows;
    }

    private String buildHeader(String title, String targetId) {
        return "# " + title + "\n\n"
            + "- Target ID: `" + targetId + "`\n"
            + "- Program: `" + currentProgram.getName() + "`\n"
            + "- Generated by: `ExportAnalysisArtifacts.java`\n\n";
    }

    private void writeBaselineArtifacts(String outputDir, String targetId) throws IOException {
        // Function names
        String functionMd = buildHeader("Function Names", targetId);
        functionMd += "## Stage\n\n- Stage: `Baseline Evidence`\n\n";
        functionMd += "## Observed Functions\n\n";
        functionMd += formatTable(new String[]{"Function", "Entry", "Kind"}, functionRows(50));
        functionMd += "\n";
        writeText(new File(outputDir, "function-names.md").getAbsolutePath(), functionMd);

        // Types and structs
        String typesMd = buildHeader("Types And Structs", targetId);
        typesMd += "## Stage\n\n- Stage: `Baseline Evidence`\n\n";
        typesMd += "## Observed Data Types\n\n";
        typesMd += formatTable(new String[]{"Name", "Kind", "Path"}, typeRows(50));
        typesMd += "\n";
        writeText(new File(outputDir, "types-and-structs.md").getAbsolutePath(), typesMd);

        // Imports and libraries
        String importsMd = buildHeader("Imports And Libraries", targetId);
        importsMd += "## Stage\n\n- Stage: `Baseline Evidence`\n\n";
        importsMd += "## Observed Imports\n\n";
        importsMd += formatTable(new String[]{"Library", "Symbol", "Address"}, importRows(50));
        importsMd += "\n";
        writeText(new File(outputDir, "imports-and-libraries.md").getAbsolutePath(), importsMd);

        // Strings and constants
        String stringsMd = buildHeader("Strings And Constants", targetId);
        stringsMd += "## Stage\n\n- Stage: `Baseline Evidence`\n\n";
        stringsMd += "## Observed Strings\n\n";
        stringsMd += formatTable(new String[]{"Address", "Preview"}, stringRows(50));
        stringsMd += "\n";
        writeText(new File(outputDir, "strings-and-constants.md").getAbsolutePath(), stringsMd);

        // Xrefs and call graph
        String xrefsMd = buildHeader("Xrefs And Call Graph", targetId);
        xrefsMd += "## Stage\n\n- Stage: `Baseline Evidence`\n\n";
        xrefsMd += "## Function Reference Counts\n\n";
        xrefsMd += formatTable(new String[]{"Function", "Entry", "Incoming refs"}, xrefRows(50));
        xrefsMd += "\n";
        writeText(new File(outputDir, "xrefs-and-callgraph.md").getAbsolutePath(), xrefsMd);

        // Renaming log (placeholder)
        String renameMd = buildHeader("Renaming Log", targetId);
        renameMd += "## Stage Gate\n\n";
        renameMd += "- Stage: `Semantic Reconstruction`\n";
        renameMd += "- Semantic changes are blocked until prior evidence has been reviewed.\n\n";
        renameMd += "## Mutation Schema\n\n";
        renameMd += "| Item Kind | Target Address | Expected Current Name | New Name | Prior Evidence | Change Summary | Confidence | Linked Selection | Open Questions | Status |\n";
        renameMd += "| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |\n";
        renameMd += "| function | `pending_target_address` | `pending_current_name` | `pending_new_name` | `pending_local_verification` | `pending_local_verification` | low / medium / high | `pending_local_verification` | `pending_local_verification` | blocked |\n";
        renameMd += "| symbol | `pending_target_address` | `pending_current_name` | `pending_new_name` | `pending_local_verification` | `pending_local_verification` | low / medium / high | `pending_local_verification` | `pending_local_verification` | blocked |\n";
        renameMd += "| label | `pending_target_address` | `pending_current_name` | `pending_new_name` | `pending_local_verification` | `pending_local_verification` | low / medium / high | `pending_local_verification` | `pending_local_verification` | blocked |\n";
        renameMd += "\n";
        renameMd += "Only rows marked `ready`, `approved`, or `complete` should be consumed by reusable rename scripts.\n";
        writeText(new File(outputDir, "renaming-log.md").getAbsolutePath(), renameMd);

        // Signature log (placeholder)
        String signatureMd = buildHeader("Signature Log", targetId);
        signatureMd += "## Stage Gate\n\n";
        signatureMd += "- Stage: `Semantic Reconstruction`\n";
        signatureMd += "- Signature changes are blocked until prior evidence has been reviewed.\n\n";
        signatureMd += "## Mutation Schema\n\n";
        signatureMd += "| Target Address | Expected Current Name | Expected Current Signature | New Function Name | Return Type | Parameter List | Calling Convention | Prior Evidence | Change Summary | Confidence | Linked Selection | Open Questions | Status |\n";
        signatureMd += "| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |\n";
        signatureMd += "| `pending_target_address` | `pending_current_name` | `return=undefined8 | params=param_1:undefined8 | calling=default` | `no_change` | `pending_return_type` | `param_1:undefined8` | `default` | `pending_local_verification` | `pending_local_verification` | low / medium / high | `pending_local_verification` | `pending_local_verification` | blocked |\n";
        signatureMd += "\n";
        signatureMd += "Use the canonical current-signature format `return=<type> | params=<name:type; ...> | calling=<convention>`.\n";
        signatureMd += "Use semicolon-separated parameter lists like `ctx:void *; length:uint32_t`.\n";
        signatureMd += "Use `void` for zero-parameter functions and `...` as the final token for varargs.\n";
        signatureMd += "Only rows marked `ready`, `approved`, or `complete` should be consumed by reusable signature scripts.\n";
        writeText(new File(outputDir, "signature-log.md").getAbsolutePath(), signatureMd);

        // Decompiled output (blocked placeholder)
        String decompiledMd = buildHeader("Decompiled Output", targetId);
        decompiledMd += "## Status\n\n";
        decompiledMd += "- Stage: `Baseline Evidence`\n";
        decompiledMd += "- Decompiled bodies are intentionally blocked in this stage.\n";
        decompiledMd += "- Next step: complete `Evidence Review`, `Target Selection`, and `Source Comparison` before `Selected Decompilation`.\n\n";
        decompiledMd += "## Required Entry Fields\n\n";
        decompiledMd += "| Field | Required |\n";
        decompiledMd += "| --- | --- |\n";
        decompiledMd += "| `function_identity` | Yes |\n";
        decompiledMd += "| `outer_to_inner_order` | Yes |\n";
        decompiledMd += "| `selection_reason` | Yes |\n";
        decompiledMd += "| `role_evidence` | Yes |\n";
        decompiledMd += "| `name_evidence` | Yes |\n";
        decompiledMd += "| `prototype_evidence` | Yes |\n";
        decompiledMd += "| `confidence` | Yes |\n";
        decompiledMd += "| `open_questions` | Yes |\n\n";
        decompiledMd += "## Selected Decompilation Prerequisites\n\n";
        decompiledMd += "1. Record a `selection_reason`.\n";
        decompiledMd += "2. Record `role_evidence`, `name_evidence`, and `prototype_evidence`.\n";
        decompiledMd += "3. Confirm the function is part of the current outside-in traversal.\n";
        writeText(new File(outputDir, "decompiled-output.md").getAbsolutePath(), decompiledMd);
    }

    private List<Function> matchRequestedFunctions(List<String> requestedIds) {
        List<Function> pool = new ArrayList<>();
        FunctionManager fm = currentProgram.getFunctionManager();
        java.util.Iterator<Function> it = fm.getFunctions(true);
        while (it.hasNext()) {
            pool.add(it.next());
        }

        List<Function> matches = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String wantedId : requestedIds) {
            for (Function func : pool) {
                String name = func.getName();
                String entry = func.getEntryPoint().toString();
                String label = name + "@" + entry;
                if (wantedId.equals(name) || wantedId.equals(entry) || wantedId.equals(label)) {
                    if (!seen.contains(entry)) {
                        matches.add(func);
                        seen.add(entry);
                    }
                    break;
                }
            }
        }
        return matches;
    }

    private String decompileSections(List<Function> functions) {
        DecompInterface iface = new DecompInterface();
        iface.openProgram(currentProgram);
        ConsoleTaskMonitor monitor = new ConsoleTaskMonitor();
        StringBuilder sb = new StringBuilder();
        int order = 1;
        for (Function func : functions) {
            DecompileResults result = iface.decompileFunction(func, 30, monitor);
            String code;
            if (result == null || !result.decompileCompleted()) {
                code = "/* Decompilation unavailable. */";
            } else {
                code = result.getDecompiledFunction().getC().trim();
            }
            String name = cleanCell(func.getName());
            String entry = func.getEntryPoint().toString();

            sb.append("### Function `").append(entry).append("`: `").append(name).append("`\n\n");
            sb.append("- `function_identity`: `").append(name).append("@").append(entry).append("`\n");
            sb.append("- `outer_to_inner_order`: `").append(order).append("`\n");
            sb.append("- `selection_reason`: `pending_analyst_entry`\n");
            sb.append("- `role_evidence`: `pending_analyst_entry`\n");
            sb.append("- `name_evidence`: `pending_analyst_entry`\n");
            sb.append("- `prototype_evidence`: `pending_analyst_entry`\n");
            sb.append("- `confidence`: `pending_analyst_entry`\n");
            sb.append("- `open_questions`: `pending_analyst_entry`\n\n");
            sb.append("```c\n").append(code).append("\n```\n\n");
            order++;
        }
        iface.dispose();
        return sb.toString();
    }

    private void writeSelectedDecompilation(String outputDir, String targetId, List<String> requestedIds) throws IOException {
        List<Function> functions = matchRequestedFunctions(requestedIds);
        if (functions.isEmpty()) {
            printerr("No selected functions matched the requested identifiers.");
            throw new RuntimeException("No matching functions");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(buildHeader("Decompiled Output", targetId));
        sb.append("## Status\n\n");
        sb.append("- Stage: `Selected Decompilation`\n");
        sb.append("- Requested selectors: `").append(String.join("`, `", requestedIds)).append("`\n\n");
        sb.append("## Exported Functions\n\n");
        sb.append(decompileSections(functions));
        sb.append("\n");
        writeText(new File(outputDir, "decompiled-output.md").getAbsolutePath(), sb.toString());
    }
}
