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

class MarkdownSmellReporterTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesMarkdownWithSmellDocLinks() throws IOException {
        Map<String, List<TestSmell>> smellsByFile = new LinkedHashMap<>();
        smellsByFile.put("com/example/FooTest.java", Arrays.asList(
                new TestSmell(SmellType.EMPTY_TEST, "FooTest", "testEmpty", 10, "Test method is empty"),
                new TestSmell(SmellType.SLEEPY_TEST, "FooTest", "testSlow", 25, "Thread.sleep() call")
        ));

        File outputDir = tempDir.toFile();
        new MarkdownSmellReporter(outputDir).report(smellsByFile);

        File reportFile = new File(outputDir, "test-smells.md");
        assertThat(reportFile).exists();

        String content = Files.readString(reportFile.toPath());
        // Header
        assertThat(content).contains("# Test Smell Report");
        // Summary table
        assertThat(content).contains("| Smell | Count |");
        assertThat(content).contains("[Empty Test](https://github.com/kawasima/savanna-maven-plugin/tree/main/docs/smells/empty-test.md)");
        assertThat(content).contains("[Sleepy Test](https://github.com/kawasima/savanna-maven-plugin/tree/main/docs/smells/sleepy-test.md)");
        // Details section with toString() format and linked display name
        assertThat(content).contains("### com/example/FooTest.java");
        assertThat(content).contains("[Empty Test](https://github.com/kawasima/savanna-maven-plugin/tree/main/docs/smells/empty-test.md) at line 10 in testEmpty()");
    }

    @Test
    void handlesProjectLevelSmell() throws IOException {
        Map<String, List<TestSmell>> smellsByFile = new LinkedHashMap<>();
        smellsByFile.put("(project)", Collections.singletonList(
                new TestSmell(SmellType.NO_TEST, "(project)", null, 0, "No test source directory found")
        ));

        File outputDir = tempDir.toFile();
        new MarkdownSmellReporter(outputDir).report(smellsByFile);

        String content = Files.readString(new File(outputDir, "test-smells.md").toPath());
        assertThat(content).contains("### (project)");
        assertThat(content).contains("[No Test](https://github.com/kawasima/savanna-maven-plugin/tree/main/docs/smells/no-test.md)");
    }
}
