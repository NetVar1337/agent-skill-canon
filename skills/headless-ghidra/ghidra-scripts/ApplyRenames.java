// Apply reviewable renames from a Markdown renaming log.
//
// Script args:
//   1. output_dir
//   2. target_id (optional)
//   3. rename_log_path (optional; defaults to <output_dir>/renaming-log.md)
//
// Only rows whose Status is ready, approved, or complete are considered
// executable. Supported Item Kind values are function, symbol, and label.

import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.FunctionManager;
import ghidra.program.model.symbol.SourceType;
import ghidra.program.model.symbol.Symbol;
import ghidra.program.model.symbol.SymbolTable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ApplyRenames extends GhidraScript {
    private static final List<String> EXECUTABLE_STATUSES =
        Arrays.asList("ready", "approved", "complete");

    private enum ItemKind {
        FUNCTION,
        SYMBOL
    }

    private static class RenameEntry {
        String itemKind;
        String targetAddress;
        String expectedCurrentName;
        String newName;
        String priorEvidence;
        String changeSummary;
        String confidence;
        String linkedSelection;
        String openQuestions;
        String status;
    }

    private static class ResolvedTarget {
        final ItemKind kind;
        final String displayKind;
        final Address address;
        final Function function;
        final Symbol symbol;

        ResolvedTarget(ItemKind kind, Address address, Function function, Symbol symbol) {
            this.kind = kind;
            this.displayKind = kind == ItemKind.FUNCTION ? "function" : "symbol";
            this.address = address;
            this.function = function;
            this.symbol = symbol;
        }

        String getCurrentName() {
            if (kind == ItemKind.FUNCTION) {
                return function.getName();
            }
            return symbol.getName();
        }

        void setName(String newName) throws Exception {
            if (kind == ItemKind.FUNCTION) {
                function.setName(newName, SourceType.USER_DEFINED);
                return;
            }
            symbol.setName(newName, SourceType.USER_DEFINED);
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

    private String cleanCell(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.trim();
        if (cleaned.startsWith("`") && cleaned.endsWith("`") && cleaned.length() >= 2) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }
        return cleaned.replace("\n", " ").replace("|", "\\|");
    }

    private String normalizeValue(String value) {
        return cleanCell(value);
    }

    private boolean isMeaningfulValue(String value) {
        String normalized = normalizeValue(value).toLowerCase(Locale.ROOT);
        return !normalized.isEmpty()
            && !"pending_local_verification".equals(normalized)
            && !"pending_target_address".equals(normalized)
            && !"pending_current_name".equals(normalized)
            && !"pending_new_name".equals(normalized)
            && !"pending_analyst_entry".equals(normalized);
    }

    private boolean isExecutableStatus(String status) {
        return EXECUTABLE_STATUSES.contains(normalizeValue(status).toLowerCase(Locale.ROOT));
    }

    private List<String> splitMarkdownRow(String line) {
        String trimmed = line.trim();
        if (trimmed.startsWith("|")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("|")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (escaped) {
                if (ch == '|' || ch == '\\') {
                    current.append(ch);
                } else {
                    current.append('\\');
                    current.append(ch);
                }
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == '|') {
                cells.add(normalizeValue(current.toString()));
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        if (escaped) {
            current.append('\\');
        }
        cells.add(normalizeValue(current.toString()));
        return cells;
    }

    private Map<String, Integer> buildHeaderIndex(List<String> headers) {
        Map<String, Integer> index = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            index.put(headers.get(i).toLowerCase(Locale.ROOT), i);
        }
        return index;
    }

    private void requireHeader(Map<String, Integer> headerIndex, String name) {
        if (!headerIndex.containsKey(name.toLowerCase(Locale.ROOT))) {
            throw new RuntimeException("Missing required rename-log column: " + name);
        }
    }

    private String cellValue(List<String> row, Map<String, Integer> headerIndex, String name) {
        Integer idx = headerIndex.get(name.toLowerCase(Locale.ROOT));
        if (idx == null || idx.intValue() >= row.size()) {
            return "";
        }
        return row.get(idx.intValue());
    }

    private List<RenameEntry> parseRenameLog(String path) throws Exception {
        List<String> lines = Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
        List<RenameEntry> entries = new ArrayList<>();
        boolean tableStarted = false;
        Map<String, Integer> headerIndex = null;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!tableStarted) {
                if (line.startsWith("|")
                    && line.toLowerCase(Locale.ROOT).contains("item kind")
                    && line.toLowerCase(Locale.ROOT).contains("target address")
                    && line.toLowerCase(Locale.ROOT).contains("new name")) {
                    headerIndex = buildHeaderIndex(splitMarkdownRow(line));
                    requireHeader(headerIndex, "Item Kind");
                    requireHeader(headerIndex, "Target Address");
                    requireHeader(headerIndex, "Expected Current Name");
                    requireHeader(headerIndex, "New Name");
                    requireHeader(headerIndex, "Status");
                    tableStarted = true;
                    i++;
                }
                continue;
            }

            if (line.isEmpty() || !line.startsWith("|")) {
                break;
            }

            List<String> row = splitMarkdownRow(line);
            RenameEntry entry = new RenameEntry();
            entry.itemKind = cellValue(row, headerIndex, "Item Kind");
            entry.targetAddress = cellValue(row, headerIndex, "Target Address");
            entry.expectedCurrentName = cellValue(row, headerIndex, "Expected Current Name");
            entry.newName = cellValue(row, headerIndex, "New Name");
            entry.priorEvidence = cellValue(row, headerIndex, "Prior Evidence");
            entry.changeSummary = cellValue(row, headerIndex, "Change Summary");
            entry.confidence = cellValue(row, headerIndex, "Confidence");
            entry.linkedSelection = cellValue(row, headerIndex, "Linked Selection");
            entry.openQuestions = cellValue(row, headerIndex, "Open Questions");
            entry.status = cellValue(row, headerIndex, "Status");
            entries.add(entry);
        }

        if (!tableStarted) {
            throw new RuntimeException("No supported rename table was found in " + path);
        }
        return entries;
    }

    private ItemKind parseItemKind(String rawItemKind) {
        String normalized = normalizeValue(rawItemKind).toLowerCase(Locale.ROOT);
        if ("function".equals(normalized)) {
            return ItemKind.FUNCTION;
        }
        if ("symbol".equals(normalized) || "label".equals(normalized)) {
            return ItemKind.SYMBOL;
        }
        throw new RuntimeException("Unsupported item kind: " + rawItemKind);
    }

    private Address resolveAddress(String rawAddress) {
        String value = normalizeValue(rawAddress);
        if (value.startsWith("0x") || value.startsWith("0X")) {
            value = value.substring(2);
        }
        try {
            return currentProgram.getAddressFactory().getDefaultAddressSpace().getAddress(value);
        } catch (Exception error) {
            return null;
        }
    }

    private ResolvedTarget resolveTarget(RenameEntry entry) {
        Address address = resolveAddress(entry.targetAddress);
        if (address == null) {
            return null;
        }

        ItemKind kind = parseItemKind(entry.itemKind);
        if (kind == ItemKind.FUNCTION) {
            FunctionManager functionManager = currentProgram.getFunctionManager();
            Function function = functionManager.getFunctionAt(address);
            if (function == null) {
                function = functionManager.getFunctionContaining(address);
            }
            if (function == null) {
                return null;
            }
            return new ResolvedTarget(kind, address, function, null);
        }

        SymbolTable symbolTable = currentProgram.getSymbolTable();
        Symbol symbol = symbolTable.getPrimarySymbol(address);
        if (symbol == null) {
            Symbol[] symbols = symbolTable.getSymbols(address);
            if (symbols.length > 0) {
                symbol = symbols[0];
            }
        }
        if (symbol == null) {
            return null;
        }
        return new ResolvedTarget(kind, address, null, symbol);
    }

    private String buildReport(
        String targetId,
        String renameLogPath,
        List<String[]> rows,
        int applied,
        int skipped,
        int failed,
        String parseError
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Rename Apply Report\n\n");
        sb.append("- Target ID: `").append(targetId).append("`\n");
        sb.append("- Program: `").append(currentProgram.getName()).append("`\n");
        sb.append("- Rename Log: `").append(renameLogPath).append("`\n");
        sb.append("- Applied: `").append(applied).append("`\n");
        sb.append("- Skipped: `").append(skipped).append("`\n");
        sb.append("- Failed: `").append(failed).append("`\n");
        if (parseError != null && !parseError.isEmpty()) {
            sb.append("- Parse Error: `").append(cleanCell(parseError)).append("`\n");
        }
        sb.append("\n");
        sb.append("| Item Kind | Target Address | Expected Current Name | New Name | Status | Result | Notes |\n");
        sb.append("| --- | --- | --- | --- | --- | --- | --- |\n");
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

    @Override
    public void run() throws Exception {
        String[] args = getScriptArgs();
        if (args.length < 1) {
            printerr("Usage: ApplyRenames.java <output_dir> [target_id] [rename_log_path]");
            throw new RuntimeException("Missing output_dir argument");
        }

        String outputDir = args[0];
        String targetId = args.length > 1 ? args[1] : currentProgram.getName();
        String renameLogPath =
            args.length > 2 ? args[2] : new File(outputDir, "renaming-log.md").getAbsolutePath();
        String reportPath = new File(outputDir, "rename-apply-report.md").getAbsolutePath();

        ensureDir(outputDir);

        List<String[]> reportRows = new ArrayList<>();
        int applied = 0;
        int skipped = 0;
        int failed = 0;
        List<RenameEntry> entries;

        try {
            entries = parseRenameLog(renameLogPath);
        } catch (Exception error) {
            reportRows.add(new String[]{
                "n/a",
                renameLogPath,
                "",
                "",
                "parse_failed",
                "failed",
                error.getMessage()
            });
            writeText(
                reportPath,
                buildReport(targetId, renameLogPath, reportRows, applied, skipped, 1, error.getMessage())
            );
            throw error;
        }

        for (RenameEntry entry : entries) {
            String normalizedStatus = normalizeValue(entry.status).toLowerCase(Locale.ROOT);
            if (!isExecutableStatus(entry.status)) {
                skipped++;
                reportRows.add(new String[]{
                    entry.itemKind,
                    entry.targetAddress,
                    entry.expectedCurrentName,
                    entry.newName,
                    normalizedStatus,
                    "skipped",
                    "Status is not executable."
                });
                continue;
            }

            if (!isMeaningfulValue(entry.targetAddress)
                || !isMeaningfulValue(entry.expectedCurrentName)
                || !isMeaningfulValue(entry.newName)) {
                failed++;
                reportRows.add(new String[]{
                    entry.itemKind,
                    entry.targetAddress,
                    entry.expectedCurrentName,
                    entry.newName,
                    normalizedStatus,
                    "failed",
                    "Executable rows require target address, expected current name, and new name."
                });
                continue;
            }

            final ResolvedTarget target;
            try {
                target = resolveTarget(entry);
            } catch (Exception error) {
                failed++;
                reportRows.add(new String[]{
                    entry.itemKind,
                    entry.targetAddress,
                    entry.expectedCurrentName,
                    entry.newName,
                    normalizedStatus,
                    "failed",
                    error.getMessage()
                });
                continue;
            }

            if (target == null) {
                failed++;
                reportRows.add(new String[]{
                    entry.itemKind,
                    entry.targetAddress,
                    entry.expectedCurrentName,
                    entry.newName,
                    normalizedStatus,
                    "failed",
                    "No supported target was found at the requested address."
                });
                continue;
            }

            String currentName = target.getCurrentName();
            String expectedCurrentName = normalizeValue(entry.expectedCurrentName);
            String newName = normalizeValue(entry.newName);
            if (currentName.equals(newName)) {
                applied++;
                reportRows.add(new String[]{
                    target.displayKind,
                    entry.targetAddress,
                    expectedCurrentName,
                    newName,
                    normalizedStatus,
                    "already_applied",
                    "Target already matches the requested new name."
                });
                continue;
            }

            if (!currentName.equals(expectedCurrentName)) {
                failed++;
                reportRows.add(new String[]{
                    target.displayKind,
                    entry.targetAddress,
                    expectedCurrentName,
                    newName,
                    normalizedStatus,
                    "failed",
                    "Expected current name mismatch. Observed: " + currentName
                });
                continue;
            }

            target.setName(newName);
            applied++;
            reportRows.add(new String[]{
                target.displayKind,
                entry.targetAddress,
                expectedCurrentName,
                newName,
                normalizedStatus,
                "applied",
                "Rename was applied successfully."
            });
        }

        writeText(reportPath, buildReport(targetId, renameLogPath, reportRows, applied, skipped, failed, ""));

        if (failed > 0) {
            throw new RuntimeException("Rename application completed with " + failed + " failure(s).");
        }
    }
}
