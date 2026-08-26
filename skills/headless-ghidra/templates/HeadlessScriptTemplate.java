// Reusable template for headless Ghidra scripts in this repository.
//
// IMPORTANT: Python/Jython scripts are NOT supported by this pipeline.
// All custom Ghidra scripts MUST be written in Java extending GhidraScript.
// The file name MUST exactly match the public class name.
//
// Required metadata for every derived script:
//   - Script role: discovery | analysis | export | reconstruction-support
//   - Target outputs: tracked artifact files or generated project state
//   - Allowed side effects: read-only | export-only | analysis-metadata-update
//   - Registration: command-manifest entry + runner wiring + validation notes
//
// Script args:
//   1. output_dir
//   2. target_id (optional)

import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.FunctionManager;
import ghidra.program.model.address.Address;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class HeadlessScriptTemplate extends GhidraScript {

    @Override
    public void run() throws Exception {
        String[] args = getScriptArgs();
        if (args.length < 1) {
            printerr("Usage: HeadlessScriptTemplate.java <output_dir> [target_id]");
            throw new RuntimeException("Missing output_dir argument");
        }

        String outputDir = args[0];
        String targetId = args.length > 1 ? args[1] : currentProgram.getName();

        ensureDir(outputDir);

        String programName = currentProgram.getName();
        String imageBase = currentProgram.getImageBase().toString();
        int functionCount = currentProgram.getFunctionManager().getFunctionCount();

        StringBuilder body = new StringBuilder();
        body.append("target_id: \"").append(targetId).append("\"\n");
        body.append("program: \"").append(programName).append("\"\n");
        body.append("image_base: \"").append(imageBase).append("\"\n");
        body.append("function_count: ").append(functionCount).append("\n");
        body.append("\n# TODO:\n");
        body.append("# - Replace this section with task-specific observations.\n");
        body.append("# - Keep writes deterministic and limited to approved output paths.\n");
        body.append("# - Record any metadata mutations in the appropriate tracked artifact.\n");

        writeText(outputDir + File.separator + "template-output.yaml", body.toString());
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
}
