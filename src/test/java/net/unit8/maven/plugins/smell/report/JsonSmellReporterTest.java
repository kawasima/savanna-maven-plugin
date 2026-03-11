package net.unit8.maven.plugins.smell.report;

import net.unit8.maven.plugins.smell.SmellType;
import net.unit8.maven.plugins.smell.TestSmell;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class JsonSmellReporterTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesValidJsonReport() throws IOException {
        Map<String, List<TestSmell>> smellsByFile = new LinkedHashMap<>();
        smellsByFile.put("com/example/FooTest.java", Arrays.asList(
                new TestSmell(SmellType.EMPTY_TEST, "FooTest", "testEmpty", 10, "Test method is empty"),
                new TestSmell(SmellType.MISSING_ASSERTION, "FooTest", "testNoAssert", 20, "No assertion")
        ));

        File outputDir = tempDir.toFile();
        JsonSmellReporter reporter = new JsonSmellReporter(outputDir);
        reporter.report(smellsByFile);

        File reportFile = new File(outputDir, "test-smells.json");
        assertThat(reportFile).exists();

        String content = Files.readString(reportFile.toPath());
        assertThat(content).contains("\"totalSmells\": 2");
        assertThat(content).contains("\"type\": \"EMPTY_TEST\"");
        assertThat(content).contains("\"type\": \"MISSING_ASSERTION\"");
        assertThat(content).contains("\"className\": \"FooTest\"");
        assertThat(content).contains("\"methodName\": \"testEmpty\"");
        assertThat(content).contains("\"line\": 10");
    }

    @Test
    void escapesSpecialCharacters() throws IOException {
        Map<String, List<TestSmell>> smellsByFile = new LinkedHashMap<>();
        smellsByFile.put("Test.java", Collections.singletonList(
                new TestSmell(SmellType.EMPTY_TEST, "Test", null, 1, "contains \"quotes\" and\nnewline")
        ));

        File outputDir = tempDir.toFile();
        new JsonSmellReporter(outputDir).report(smellsByFile);

        String content = Files.readString(new File(outputDir, "test-smells.json").toPath());
        assertThat(content).contains("contains \\\"quotes\\\" and\\nnewline");
    }
}
