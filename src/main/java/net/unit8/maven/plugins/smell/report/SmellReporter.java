package net.unit8.maven.plugins.smell.report;

import net.unit8.maven.plugins.smell.TestSmell;

import java.util.List;
import java.util.Map;

public interface SmellReporter {
    void report(Map<String, List<TestSmell>> smellsByFile);
}
