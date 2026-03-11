package net.unit8.maven.plugins.smell.report;

import net.unit8.maven.plugins.smell.SmellType;
import net.unit8.maven.plugins.smell.TestSmell;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MarkdownSmellReporter implements SmellReporter {
    private static final String DOCS_BASE_URL =
            "https://github.com/kawasima/savanna-maven-plugin/tree/main/docs/smells/";

    private final File outputDirectory;

    public MarkdownSmellReporter(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    @Override
    public void report(Map<String, List<TestSmell>> smellsByFile) {
        outputDirectory.mkdirs();
        File reportFile = new File(outputDirectory, "test-smells.md");

        int total = smellsByFile.values().stream().mapToInt(List::size).sum();

        try (PrintWriter w = new PrintWriter(reportFile, StandardCharsets.UTF_8.name())) {
            w.println("# Test Smell Report");
            w.println();
            w.println("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            w.println();

            // Summary
            Map<SmellType, Long> byType = smellsByFile.values().stream()
                    .flatMap(List::stream)
                    .collect(Collectors.groupingBy(TestSmell::getType, Collectors.counting()));

            w.println("## Summary");
            w.println();
            w.println("Total: **" + total + "** smell(s) in **" + smellsByFile.size() + "** file(s)");
            w.println();
            w.println("| Smell | Count |");
            w.println("|-------|------:|");
            byType.forEach((type, count) ->
                    w.println("| [" + type.getDisplayName() + "](" + smellDocUrl(type) + ") | " + count + " |"));
            w.println();

            // Details
            w.println("## Details");
            w.println();

            for (Map.Entry<String, List<TestSmell>> entry : smellsByFile.entrySet()) {
                String file = entry.getKey();
                List<TestSmell> smells = entry.getValue();
                if (smells.isEmpty()) {
                    continue;
                }

                w.println("### " + file);
                w.println();

                for (TestSmell smell : smells) {
                    // Use toString() format but replace displayName with a link
                    String text = smell.toString();
                    String displayName = smell.getType().getDisplayName();
                    String link = "[" + displayName + "](" + smellDocUrl(smell.getType()) + ")";
                    w.println("- " + text.replaceFirst(java.util.regex.Pattern.quote(displayName), link));
                }
                w.println();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write Markdown report to " + reportFile, e);
        }
    }

    private static String smellDocUrl(SmellType type) {
        return DOCS_BASE_URL + type.name().toLowerCase().replace('_', '-') + ".md";
    }
}
