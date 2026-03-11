package net.unit8.maven.plugins.smell.report;

import net.unit8.maven.plugins.smell.TestSmell;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class JsonSmellReporter implements SmellReporter {
    private final File outputDirectory;

    public JsonSmellReporter(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    @Override
    public void report(Map<String, List<TestSmell>> smellsByFile) {
        outputDirectory.mkdirs();
        File reportFile = new File(outputDirectory, "test-smells.json");

        try (PrintWriter writer = new PrintWriter(reportFile, StandardCharsets.UTF_8.name())) {
            writer.println("{");
            writer.println("  \"timestamp\": \"" + Instant.now() + "\",");

            int total = smellsByFile.values().stream().mapToInt(List::size).sum();
            writer.println("  \"totalSmells\": " + total + ",");
            writer.println("  \"files\": [");

            int fileIndex = 0;
            for (Map.Entry<String, List<TestSmell>> entry : smellsByFile.entrySet()) {
                if (fileIndex > 0) {
                    writer.println(",");
                }
                writer.println("    {");
                writer.println("      \"file\": \"" + escapeJson(entry.getKey()) + "\",");
                writer.println("      \"smells\": [");

                List<TestSmell> smells = entry.getValue();
                for (int i = 0; i < smells.size(); i++) {
                    TestSmell smell = smells.get(i);
                    writer.println("        {");
                    writer.println("          \"type\": \"" + smell.getType().name() + "\",");
                    writer.println("          \"displayName\": \"" + escapeJson(smell.getType().getDisplayName()) + "\",");
                    writer.println("          \"className\": \"" + escapeJson(smell.getClassName()) + "\",");
                    if (smell.getMethodName() != null) {
                        writer.println("          \"methodName\": \"" + escapeJson(smell.getMethodName()) + "\",");
                    }
                    writer.println("          \"line\": " + smell.getLineNumber() + ",");
                    writer.println("          \"message\": \"" + escapeJson(smell.getMessage()) + "\",");
                    writer.println("          \"heuristic\": " + smell.isHeuristic());
                    writer.print("        }");
                    if (i < smells.size() - 1) {
                        writer.println(",");
                    } else {
                        writer.println();
                    }
                }
                writer.println("      ]");
                writer.print("    }");
                fileIndex++;
            }

            writer.println();
            writer.println("  ]");
            writer.println("}");
        } catch (IOException e) {
            throw new RuntimeException("Failed to write JSON report to " + reportFile, e);
        }
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
