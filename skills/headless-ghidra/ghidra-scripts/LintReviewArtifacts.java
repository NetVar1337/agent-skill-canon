// Lint reviewable Markdown artifacts used by the headless workflow.
//
// Script args:
//   1. output_dir
//   2. target_id (optional)
//   3+. artifact paths (optional; defaults to
//       <output_dir>/renaming-log.md and <output_dir>/signature-log.md)
//
// This script is headless-only and writes a reviewable Markdown report to
// <output_dir>/artifact-lint-report.md. Parse failures are recorded in
// the report before the script exits non-zero.

import ghidra.app.script.GhidraScript;

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

public class LintReviewArtifacts extends GhidraScript {
    private static final String OUTPUT_NAME = "artifact-lint-report.md";
    private static final List<String> EXECUTABLE_STATUSES =
        Arrays.asList("ready", "approved", "complete");
    private static final List<String> VALID_STATUSES =
        Arrays.asList("blocked", "draft", "ready", "approved", "complete", "deferred", "rejected");
    private static final List<String> VALID_CONFIDENCE =
        Arrays.asList("low", "medium", "high");

    private static class ArtifactSpec {
        final String kind;
        final String[] requiredColumns;
        final String[] executableColumns;

        ArtifactSpec(String kind, String[] requiredColumns, String[] executableColumns) {
            this.kind = kind;
            this.requiredColumns = requiredColumns;
            this.executableColumns = executableColumns;
        }
    }

    private static class ParsedArtifact {
        final ArtifactSpec spec;
        final Map<String, Integer> headerIndex;
        final List<List<String>> rows;

        ParsedArtifact(ArtifactSpec spec, Map<String, Integer> headerIndex, List<List<String>> rows) {
            this.spec = spec;
            this.headerIndex = headerIndex;
            this.rows = rows;
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
        return cleanCell(value).toLowerCase(Locale.ROOT);
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
                cells.add(cleanCell(current.toString()));
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        if (escaped) {
            current.append('\\');
        }
        cells.add(cleanCell(current.toString()));
        return cells;
    }

    private Map<String, Integer> buildHeaderIndex(List<String> headers) {
        Map<String, Integer> index = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            index.put(normalizeValue(headers.get(i)), i);
        }
        return index;
    }

    private ArtifactSpec detectSpec(String path) {
        String fileName = new File(path).getName().toLowerCase(Locale.ROOT);
        if ("renaming-log.md".equals(fileName)) {
            return new ArtifactSpec(
                "renaming-log",
                new String[] {
                    "Item Kind",
                    "Target Address",
                    "Expected Current Name",
                    "New Name",
                    "Prior Evidence",
                    "Change Summary",
                    "Confidence",
                    "Linked Selection",
                    "Open Questions",
                    "Status"
                },
                new String[] {
                    "Item Kind",
                    "Target Address",
                    "Expected Current Name",
                    "New Name",
                    "Prior Evidence",
                    "Linked Selection",
                    "Status"
                }
            );
        }
        if ("signature-log.md".equals(fileName)) {
            return new ArtifactSpec(
                "signature-log",
                new String[] {
                    "Target Address",
                    "Expected Current Name",
                    "Expected Current Signature",
                    "New Function Name",
                    "Return Type",
                    "Parameter List",
                    "Calling Convention",
                    "Prior Evidence",
                    "Change Summary",
                    "Confidence",
                    "Linked Selection",
                    "Open Questions",
                    "Status"
                },
                new String[] {
                    "Target Address",
                    "Expected Current Name",
                    "Expected Current Signature",
                    "New Function Name",
                    "Return Type",
                    "Parameter List",
                    "Calling Convention",
                    "Prior Evidence",
                    "Linked Selection",
                    "Status"
                }
            );
        }
        return null;
    }

    private String cellValue(List<String> row, Map<String, Integer> headerIndex, String name) {
        Integer index = headerIndex.get(normalizeValue(name));
        if (index == null || index.intValue() >= row.size()) {
            return "";
        }
        return row.get(index.intValue());
    }

    private boolean isMeaningfulValue(String value) {
        String normalized = normalizeValue(value);
        return !normalized.isEmpty()
            && !"pending_local_verification".equals(normalized)
            && !"pending_target_address".equals(normalized)
            && !"pending_current_name".equals(normalized)
            && !"pending_new_name".equals(normalized)
            && !"pending_current_signature".equals(normalized)
            && !"pending_new_name_or_no_change".equals(normalized)
            && !"pending_analyst_entry".equals(normalized)
            && !"pending_signature".equals(normalized)
            && !"pending_return_type".equals(normalized)
            && !"pending_parameter_list".equals(normalized)
            && !"pending_calling_convention".equals(normalized)
            && !"low / medium / high".equals(normalized);
    }

    private boolean isExecutableStatus(String status) {
        return EXECUTABLE_STATUSES.contains(normalizeValue(status));
    }

    private ParsedArtifact parseArtifact(String path) throws Exception {
        ArtifactSpec spec = detectSpec(path);
        if (spec == null) {
            throw new RuntimeException("Unsupported artifact type: " + path);
        }

        List<String> lines = Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
        boolean tableStarted = false;
        Map<String, Integer> headerIndex = null;
        List<List<String>> rows = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!tableStarted) {
                if (line.startsWith("|")
                    && (line.toLowerCase(Locale.ROOT).contains("target address")
                        || line.toLowerCase(Locale.ROOT).contains("function address"))) {
                    headerIndex = buildHeaderIndex(splitMarkdownRow(line));
                    for (String column : spec.requiredColumns) {
                        if (!headerIndex.containsKey(normalizeValue(column))) {
                            throw new RuntimeException("Missing required " + spec.kind + " column: " + column);
                        }
                    }
                    tableStarted = true;
                    i++;
                }
                continue;
            }

            if (line.isEmpty() || !line.startsWith("|")) {
                break;
            }

            rows.add(splitMarkdownRow(line));
        }

        if (!tableStarted) {
            throw new RuntimeException("No supported review-artifact table was found in " + path);
        }
        return new ParsedArtifact(spec, headerIndex, rows);
    }

    private void addIssue(
        List<String[]> issues,
        String artifactPath,
        String kind,
        String rowLabel,
        String severity,
        String result,
        String notes
    ) {
        issues.add(new String[] {
            artifactPath,
            kind,
            rowLabel,
            severity,
            result,
            notes
        });
    }

    private void lintRenamingRow(
        String artifactPath,
        Map<String, Integer> headerIndex,
        List<String> row,
        int rowNumber,
        List<String[]> issues
    ) {
        String itemKind = cellValue(row, headerIndex, "Item Kind");
        String status = cellValue(row, headerIndex, "Status");
        String confidence = cellValue(row, headerIndex, "Confidence");

        if (!VALID_STATUSES.contains(normalizeValue(status))) {
            addIssue(issues, artifactPath, "renaming-log", "row " + rowNumber, "error", "failed",
                "Unsupported status: " + status);
        }
        if (isMeaningfulValue(confidence) && !VALID_CONFIDENCE.contains(normalizeValue(confidence))) {
            addIssue(issues, artifactPath, "renaming-log", "row " + rowNumber, "error", "failed",
                "Confidence must be low, medium, or high.");
        }

        String normalizedItemKind = normalizeValue(itemKind);
        if (!normalizedItemKind.equals("function")
            && !normalizedItemKind.equals("symbol")
            && !normalizedItemKind.equals("label")) {
            addIssue(issues, artifactPath, "renaming-log", "row " + rowNumber, "error", "failed",
                "Unsupported Item Kind: " + itemKind);
        }

        if (!isExecutableStatus(status)) {
            return;
        }

        String[] executableColumns = {
            "Target Address",
            "Expected Current Name",
            "New Name",
            "Prior Evidence",
            "Linked Selection"
        };
        for (String column : executableColumns) {
            if (!isMeaningfulValue(cellValue(row, headerIndex, column))) {
                addIssue(issues, artifactPath, "renaming-log", "row " + rowNumber, "error", "failed",
                    "Executable row is missing required value for " + column + ".");
            }
        }
    }

    private void lintSignatureRow(
        String artifactPath,
        Map<String, Integer> headerIndex,
        List<String> row,
        int rowNumber,
        List<String[]> issues
    ) {
        String status = cellValue(row, headerIndex, "Status");
        String confidence = cellValue(row, headerIndex, "Confidence");

        if (!VALID_STATUSES.contains(normalizeValue(status))) {
            addIssue(issues, artifactPath, "signature-log", "row " + rowNumber, "error", "failed",
                "Unsupported status: " + status);
        }
        if (isMeaningfulValue(confidence) && !VALID_CONFIDENCE.contains(normalizeValue(confidence))) {
            addIssue(issues, artifactPath, "signature-log", "row " + rowNumber, "error", "failed",
                "Confidence must be low, medium, or high.");
        }
        if (!isExecutableStatus(status)) {
            return;
        }

        String[] executableColumns = {
            "Target Address",
            "Expected Current Name",
            "Expected Current Signature",
            "New Function Name",
            "Return Type",
            "Parameter List",
            "Calling Convention",
            "Prior Evidence",
            "Linked Selection"
        };
        for (String column : executableColumns) {
            if (!isMeaningfulValue(cellValue(row, headerIndex, column))) {
                addIssue(issues, artifactPath, "signature-log", "row " + rowNumber, "error", "failed",
                    "Executable row is missing required value for " + column + ".");
            }
        }
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

    private String buildReport(
        String targetId,
        List<String> artifactPaths,
        List<String[]> issues,
        int passed,
        int failed
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Artifact Lint Report\n\n");
        sb.append("- Target ID: `").append(cleanCell(targetId)).append("`\n");
        sb.append("- Program: `").append(cleanCell(currentProgram.getName())).append("`\n");
        sb.append("- Artifacts Checked: `").append(artifactPaths.size()).append("`\n");
        sb.append("- Passed: `").append(passed).append("`\n");
        sb.append("- Failed: `").append(failed).append("`\n\n");

        sb.append("## Checked Paths\n\n");
        for (String path : artifactPaths) {
            sb.append("- `").append(cleanCell(path)).append("`\n");
        }
        sb.append("\n");

        sb.append("## Findings\n\n");
        if (issues.isEmpty()) {
            sb.append("| Artifact | Kind | Row | Severity | Result | Notes |\n");
            sb.append("| --- | --- | --- | --- | --- | --- |\n");
            sb.append("| n/a | lint | n/a | info | passed | No lint findings. |\n");
            return sb.toString();
        }

        sb.append(formatTable(
            new String[] {"Artifact", "Kind", "Row", "Severity", "Result", "Notes"},
            issues
        ));
        sb.append("\n");
        return sb.toString();
    }

    @Override
    public void run() throws Exception {
        String[] args = getScriptArgs();
        if (args.length < 1) {
            printerr("Usage: LintReviewArtifacts.java <output_dir> [target_id] [artifact_paths...]");
            throw new RuntimeException("Missing output_dir argument");
        }

        String outputDir = args[0];
        String targetId = args.length > 1 ? cleanCell(args[1]) : currentProgram.getName();
        List<String> artifactPaths = new ArrayList<>();
        if (args.length > 2) {
            for (int i = 2; i < args.length; i++) {
                String artifactPath = cleanCell(args[i]);
                if (!artifactPath.isEmpty()) {
                    artifactPaths.add(artifactPath);
                }
            }
        }
        if (artifactPaths.isEmpty()) {
            artifactPaths.add(new File(outputDir, "renaming-log.md").getAbsolutePath());
            artifactPaths.add(new File(outputDir, "signature-log.md").getAbsolutePath());
        }

        ensureDir(outputDir);

        List<String[]> issues = new ArrayList<>();
        int passed = 0;
        int failed = 0;

        for (String artifactPath : artifactPaths) {
            ParsedArtifact artifact;
            try {
                artifact = parseArtifact(artifactPath);
            } catch (Exception error) {
                failed++;
                addIssue(issues, artifactPath, "parse", "file", "error", "failed", error.getMessage());
                continue;
            }

            int issuesBefore = issues.size();
            for (int i = 0; i < artifact.rows.size(); i++) {
                List<String> row = artifact.rows.get(i);
                int rowNumber = i + 1;
                if ("renaming-log".equals(artifact.spec.kind)) {
                    lintRenamingRow(artifactPath, artifact.headerIndex, row, rowNumber, issues);
                } else if ("signature-log".equals(artifact.spec.kind)) {
                    lintSignatureRow(artifactPath, artifact.headerIndex, row, rowNumber, issues);
                }
            }

            if (issues.size() == issuesBefore) {
                passed++;
            } else {
                failed++;
            }
        }

        writeText(
            new File(outputDir, OUTPUT_NAME).getAbsolutePath(),
            buildReport(targetId, artifactPaths, issues, passed, failed)
        );

        if (failed > 0) {
            throw new RuntimeException("Artifact lint completed with " + failed + " failing artifact(s).");
        }
    }
}
