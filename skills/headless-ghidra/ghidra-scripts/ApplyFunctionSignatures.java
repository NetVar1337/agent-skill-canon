// Apply reviewable function-signature updates from a Markdown signature log.
//
// Script args:
//   1. output_dir
//   2. target_id (optional)
//   3. signature_log_path (optional; defaults to <output_dir>/signature-log.md)
//
// Only rows whose Status is ready, approved, or complete are considered
// executable. This script stays conservative: it only updates function name,
// return type, parameter names/types, calling convention, and varargs state.

import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.Address;
import ghidra.program.model.data.BuiltInDataTypeManager;
import ghidra.program.model.data.DataType;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.FunctionManager;
import ghidra.program.model.listing.Parameter;
import ghidra.program.model.listing.ParameterImpl;
import ghidra.program.model.symbol.SourceType;
import ghidra.util.data.DataTypeParser;
import ghidra.util.data.DataTypeParser.AllowedDataTypes;

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

public class ApplyFunctionSignatures extends GhidraScript {
    private static final String OUTPUT_NAME = "signature-apply-report.md";
    private static final List<String> EXECUTABLE_STATUSES =
        Arrays.asList("ready", "approved", "complete");

    private static class SignatureEntry {
        String targetAddress;
        String expectedCurrentName;
        String expectedCurrentSignature;
        String newFunctionName;
        String returnType;
        String parameterList;
        String callingConvention;
        String priorEvidence;
        String changeSummary;
        String confidence;
        String linkedSelection;
        String openQuestions;
        String status;
    }

    private static class ParameterSpec {
        String name;
        String typeText;
        DataType resolvedType;
    }

    private static class ParsedParameterList {
        final List<ParameterSpec> parameters = new ArrayList<>();
        boolean hasVarArgs = false;
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

    private String normalizeSignatureText(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        if (normalized.startsWith("`") && normalized.endsWith("`") && normalized.length() >= 2) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        normalized = normalized.replace("\n", " ");
        normalized = normalized.replace("\\|", "|");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        normalized = normalized.replaceAll("\\s*\\|\\s*", " | ");
        normalized = normalized.replaceAll("\\s*;\\s*", "; ");
        normalized = normalized.replaceAll("\\s*:\\s*", ":");
        normalized = normalized.replaceAll("\\s*=\\s*", "=");
        return normalized.trim();
    }

    private boolean isMeaningfulValue(String value) {
        String normalized = normalizeValue(value).toLowerCase(Locale.ROOT);
        return !normalized.isEmpty()
            && !"pending_local_verification".equals(normalized)
            && !"pending_target_address".equals(normalized)
            && !"pending_current_name".equals(normalized)
            && !"pending_current_signature".equals(normalized)
            && !"pending_new_name_or_no_change".equals(normalized)
            && !"pending_return_type".equals(normalized)
            && !"pending_parameter_list".equals(normalized)
            && !"pending_calling_convention".equals(normalized)
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
            throw new RuntimeException("Missing required signature-log column: " + name);
        }
    }

    private String cellValue(List<String> row, Map<String, Integer> headerIndex, String name) {
        Integer idx = headerIndex.get(name.toLowerCase(Locale.ROOT));
        if (idx == null || idx.intValue() >= row.size()) {
            return "";
        }
        return row.get(idx.intValue());
    }

    private List<SignatureEntry> parseSignatureLog(String path) throws Exception {
        List<String> lines = Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
        List<SignatureEntry> entries = new ArrayList<>();
        boolean tableStarted = false;
        Map<String, Integer> headerIndex = null;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!tableStarted) {
                if (line.startsWith("|")
                    && line.toLowerCase(Locale.ROOT).contains("target address")
                    && line.toLowerCase(Locale.ROOT).contains("expected current signature")
                    && line.toLowerCase(Locale.ROOT).contains("parameter list")) {
                    headerIndex = buildHeaderIndex(splitMarkdownRow(line));
                    requireHeader(headerIndex, "Target Address");
                    requireHeader(headerIndex, "Expected Current Name");
                    requireHeader(headerIndex, "Expected Current Signature");
                    requireHeader(headerIndex, "New Function Name");
                    requireHeader(headerIndex, "Return Type");
                    requireHeader(headerIndex, "Parameter List");
                    requireHeader(headerIndex, "Calling Convention");
                    requireHeader(headerIndex, "Prior Evidence");
                    requireHeader(headerIndex, "Change Summary");
                    requireHeader(headerIndex, "Confidence");
                    requireHeader(headerIndex, "Linked Selection");
                    requireHeader(headerIndex, "Open Questions");
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
            SignatureEntry entry = new SignatureEntry();
            entry.targetAddress = cellValue(row, headerIndex, "Target Address");
            entry.expectedCurrentName = cellValue(row, headerIndex, "Expected Current Name");
            entry.expectedCurrentSignature = cellValue(row, headerIndex, "Expected Current Signature");
            entry.newFunctionName = cellValue(row, headerIndex, "New Function Name");
            entry.returnType = cellValue(row, headerIndex, "Return Type");
            entry.parameterList = cellValue(row, headerIndex, "Parameter List");
            entry.callingConvention = cellValue(row, headerIndex, "Calling Convention");
            entry.priorEvidence = cellValue(row, headerIndex, "Prior Evidence");
            entry.changeSummary = cellValue(row, headerIndex, "Change Summary");
            entry.confidence = cellValue(row, headerIndex, "Confidence");
            entry.linkedSelection = cellValue(row, headerIndex, "Linked Selection");
            entry.openQuestions = cellValue(row, headerIndex, "Open Questions");
            entry.status = cellValue(row, headerIndex, "Status");
            entries.add(entry);
        }

        if (!tableStarted) {
            throw new RuntimeException("No supported signature table was found in " + path);
        }
        return entries;
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

    private Function resolveFunction(SignatureEntry entry) {
        Address address = resolveAddress(entry.targetAddress);
        if (address == null) {
            return null;
        }

        FunctionManager functionManager = currentProgram.getFunctionManager();
        Function function = functionManager.getFunctionAt(address);
        if (function == null) {
            function = functionManager.getFunctionContaining(address);
        }
        return function;
    }

    private DataType parseDataType(String text) throws Exception {
        String normalized = normalizeValue(text);
        if (!isMeaningfulValue(normalized)) {
            throw new RuntimeException("Missing data type value.");
        }

        DataTypeParser parser = new DataTypeParser(
            currentProgram.getDataTypeManager(),
            currentProgram.getDataTypeManager(),
            null,
            AllowedDataTypes.ALL
        );
        try {
            return parser.parse(normalized);
        } catch (Exception primaryError) {
            DataTypeParser fallbackParser = new DataTypeParser(
                BuiltInDataTypeManager.getDataTypeManager(),
                currentProgram.getDataTypeManager(),
                null,
                AllowedDataTypes.ALL
            );
            return fallbackParser.parse(normalized);
        }
    }

    private ParsedParameterList parseParameterList(String rawParameterList) throws Exception {
        ParsedParameterList parsed = new ParsedParameterList();
        String normalized = normalizeValue(rawParameterList);
        if ("void".equalsIgnoreCase(normalized)) {
            return parsed;
        }

        String[] tokens = normalized.split(";");
        int unnamedIndex = 1;
        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if ("...".equals(trimmed)) {
                parsed.hasVarArgs = true;
                continue;
            }

            ParameterSpec spec = new ParameterSpec();
            int separator = trimmed.indexOf(':');
            if (separator >= 0) {
                spec.name = trimmed.substring(0, separator).trim();
                spec.typeText = trimmed.substring(separator + 1).trim();
            } else {
                spec.name = "param_" + unnamedIndex;
                spec.typeText = trimmed;
            }
            if (spec.name.isEmpty()) {
                spec.name = "param_" + unnamedIndex;
            }
            spec.resolvedType = parseDataType(spec.typeText);
            parsed.parameters.add(spec);
            unnamedIndex++;
        }

        if (parsed.parameters.isEmpty() && !parsed.hasVarArgs) {
            throw new RuntimeException("Parameter List must be `void` or a semicolon-separated list.");
        }
        return parsed;
    }

    private String describeParameterList(Parameter[] parameters, boolean hasVarArgs) {
        if (parameters.length == 0 && !hasVarArgs) {
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
        if (hasVarArgs) {
            pieces.add("...");
        }
        return String.join("; ", pieces);
    }

    private String describeFunctionSignature(Function function) {
        String returnType = function.getReturnType().getDisplayName();
        String parameterList = describeParameterList(function.getParameters(), function.hasVarArgs());
        String callingConvention = function.getCallingConventionName();
        return normalizeSignatureText(
            "return=" + returnType + " | params=" + parameterList + " | calling=" + callingConvention
        );
    }

    private String describeDesiredSignature(DataType returnType, ParsedParameterList parameterList, String callingConvention) {
        List<String> pieces = new ArrayList<>();
        for (ParameterSpec parameter : parameterList.parameters) {
            pieces.add(parameter.name + ":" + parameter.resolvedType.getDisplayName());
        }
        if (parameterList.hasVarArgs) {
            pieces.add("...");
        }
        String renderedParams = pieces.isEmpty() ? "void" : String.join("; ", pieces);
        return normalizeSignatureText(
            "return=" + returnType.getDisplayName() + " | params=" + renderedParams + " | calling=" + normalizeValue(callingConvention)
        );
    }

    private boolean isNoChangeName(String rawValue) {
        String normalized = normalizeValue(rawValue).toLowerCase(Locale.ROOT);
        return normalized.isEmpty() || "no_change".equals(normalized) || "current".equals(normalized);
    }

    private String desiredFunctionName(SignatureEntry entry) {
        if (isNoChangeName(entry.newFunctionName)) {
            return normalizeValue(entry.expectedCurrentName);
        }
        return normalizeValue(entry.newFunctionName);
    }

    private String buildReport(
        String targetId,
        String signatureLogPath,
        List<String[]> rows,
        int applied,
        int skipped,
        int failed,
        String parseError
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Signature Apply Report\n\n");
        sb.append("- Target ID: `").append(cleanCell(targetId)).append("`\n");
        sb.append("- Program: `").append(cleanCell(currentProgram.getName())).append("`\n");
        sb.append("- Signature Log: `").append(cleanCell(signatureLogPath)).append("`\n");
        sb.append("- Applied: `").append(applied).append("`\n");
        sb.append("- Skipped: `").append(skipped).append("`\n");
        sb.append("- Failed: `").append(failed).append("`\n");
        if (parseError != null && !parseError.isEmpty()) {
            sb.append("- Parse Error: `").append(cleanCell(parseError)).append("`\n");
        }
        sb.append("\n");
        sb.append("| Target Address | Expected Current Name | New Function Name | Status | Result | Notes |\n");
        sb.append("| --- | --- | --- | --- | --- | --- |\n");
        if (rows.isEmpty()) {
            sb.append("| n/a | n/a | n/a | n/a | skipped | No signature rows were found. |\n");
            return sb.toString();
        }
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
            printerr("Usage: ApplyFunctionSignatures.java <output_dir> [target_id] [signature_log_path]");
            throw new RuntimeException("Missing output_dir argument");
        }

        String outputDir = args[0];
        String targetId = args.length > 1 ? cleanCell(args[1]) : currentProgram.getName();
        String signatureLogPath =
            args.length > 2 ? args[2] : new File(outputDir, "signature-log.md").getAbsolutePath();
        String reportPath = new File(outputDir, OUTPUT_NAME).getAbsolutePath();

        ensureDir(outputDir);

        List<String[]> rows = new ArrayList<>();
        int applied = 0;
        int skipped = 0;
        int failed = 0;
        String parseError = null;
        List<SignatureEntry> entries = null;

        try {
            entries = parseSignatureLog(signatureLogPath);
        } catch (Exception error) {
            parseError = error.getMessage();
            failed = 1;
        }

        if (entries != null) {
            for (SignatureEntry entry : entries) {
                String targetAddress = normalizeValue(entry.targetAddress);
                String expectedCurrentName = normalizeValue(entry.expectedCurrentName);
                String newFunctionName = desiredFunctionName(entry);
                String status = normalizeValue(entry.status);

                if (!isExecutableStatus(status)) {
                    skipped++;
                    rows.add(new String[] {
                        targetAddress,
                        expectedCurrentName,
                        newFunctionName,
                        status,
                        "skipped",
                        "Row status is not executable."
                    });
                    continue;
                }

                try {
                    if (!isMeaningfulValue(targetAddress)
                        || !isMeaningfulValue(expectedCurrentName)
                        || !isMeaningfulValue(entry.expectedCurrentSignature)
                        || !isMeaningfulValue(entry.returnType)
                        || !isMeaningfulValue(entry.parameterList)
                        || !isMeaningfulValue(entry.callingConvention)
                        || !isMeaningfulValue(entry.priorEvidence)
                        || !isMeaningfulValue(entry.linkedSelection)) {
                        throw new RuntimeException(
                            "Executable signature rows require target address, expected current name/signature, return type, parameter list, calling convention, prior evidence, and linked selection."
                        );
                    }

                    Function function = resolveFunction(entry);
                    if (function == null) {
                        throw new RuntimeException("No function was found at the requested address.");
                    }

                    String observedCurrentName = normalizeValue(function.getName());
                    String observedCurrentSignature = describeFunctionSignature(function);
                    if (!observedCurrentName.equals(expectedCurrentName)) {
                        throw new RuntimeException(
                            "Expected current name `" + expectedCurrentName + "` but observed `" + observedCurrentName + "`."
                        );
                    }
                    if (!observedCurrentSignature.equals(normalizeSignatureText(entry.expectedCurrentSignature))) {
                        throw new RuntimeException(
                            "Expected current signature `" + normalizeSignatureText(entry.expectedCurrentSignature)
                            + "` but observed `" + observedCurrentSignature + "`."
                        );
                    }

                    ParsedParameterList desiredParameters = parseParameterList(entry.parameterList);
                    DataType desiredReturnType = parseDataType(entry.returnType);
                    String desiredSignature = describeDesiredSignature(
                        desiredReturnType,
                        desiredParameters,
                        entry.callingConvention
                    );

                    if (!isNoChangeName(entry.newFunctionName)) {
                        function.setName(newFunctionName, SourceType.USER_DEFINED);
                    }
                    function.setReturnType(desiredReturnType, SourceType.USER_DEFINED);

                    List<ParameterImpl> newParameters = new ArrayList<>();
                    for (ParameterSpec parameter : desiredParameters.parameters) {
                        newParameters.add(
                            new ParameterImpl(parameter.name, parameter.resolvedType, currentProgram, SourceType.USER_DEFINED)
                        );
                    }
                    function.replaceParameters(
                        newParameters,
                        Function.FunctionUpdateType.DYNAMIC_STORAGE_ALL_PARAMS,
                        true,
                        SourceType.USER_DEFINED
                    );
                    function.setVarArgs(desiredParameters.hasVarArgs);
                    function.setCallingConvention(normalizeValue(entry.callingConvention));

                    String observedFinalName = normalizeValue(function.getName());
                    String observedFinalSignature = describeFunctionSignature(function);
                    if (!observedFinalName.equals(newFunctionName)) {
                        throw new RuntimeException(
                            "Expected final name `" + newFunctionName + "` but observed `" + observedFinalName + "`."
                        );
                    }
                    if (!observedFinalSignature.equals(desiredSignature)) {
                        throw new RuntimeException(
                            "Expected final signature `" + desiredSignature + "` but observed `" + observedFinalSignature + "`."
                        );
                    }

                    applied++;
                    rows.add(new String[] {
                        targetAddress,
                        expectedCurrentName,
                        newFunctionName,
                        status,
                        "applied",
                        "Observed final signature: " + observedFinalSignature
                    });
                } catch (Exception error) {
                    failed++;
                    rows.add(new String[] {
                        targetAddress,
                        expectedCurrentName,
                        newFunctionName,
                        status,
                        "failed",
                        error.getMessage()
                    });
                }
            }
        } else {
            rows.add(new String[] {
                "n/a",
                "n/a",
                "n/a",
                "parse",
                "failed",
                parseError
            });
        }

        writeText(
            reportPath,
            buildReport(targetId, signatureLogPath, rows, applied, skipped, failed, parseError)
        );

        if (failed > 0) {
            throw new RuntimeException("Signature application completed with " + failed + " failing row(s).");
        }
    }
}
