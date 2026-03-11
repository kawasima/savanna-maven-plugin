package net.unit8.maven.plugins.smell.report;

import net.unit8.maven.plugins.smell.SmellType;
import net.unit8.maven.plugins.smell.TestSmell;
import org.apache.maven.plugin.logging.Log;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConsoleSmellReporter implements SmellReporter {
    private final Log log;

    public ConsoleSmellReporter(Log log) {
        this.log = log;
    }

    @Override
    public void report(Map<String, List<TestSmell>> smellsByFile) {
        int total = 0;
        for (Map.Entry<String, List<TestSmell>> entry : smellsByFile.entrySet()) {
            String file = entry.getKey();
            List<TestSmell> smells = entry.getValue();
            if (smells.isEmpty()) {
                continue;
            }
            total += smells.size();
            log.warn("Test smells in " + file + ":");
            for (TestSmell smell : smells) {
                log.warn("  " + smell);
            }
        }

        if (total > 0) {
            Map<SmellType, Long> byType = smellsByFile.values().stream()
                    .flatMap(List::stream)
                    .collect(Collectors.groupingBy(TestSmell::getType, Collectors.counting()));

            log.warn("");
            log.warn("Summary: " + total + " test smell(s) found in "
                    + smellsByFile.size() + " file(s)");
            byType.forEach((type, count) ->
                    log.warn("  " + type.getDisplayName() + ": " + count));
        } else {
            log.info("No test smells detected.");
        }
    }
}
