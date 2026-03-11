package net.unit8.maven.plugins.smell.detector;

import net.unit8.maven.plugins.smell.DetectionContext;
import net.unit8.maven.plugins.smell.SmellType;
import net.unit8.maven.plugins.smell.TestSmell;
import net.unit8.maven.plugins.smell.parse.TestClassParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultTestDetectorTest {
    private final DefaultTestDetector detector = new DefaultTestDetector();
    private final TestClassParser parser = new TestClassParser();

    @Test
    void detectsDefaultTestName() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "class ExampleTest {\n" +
                "    @Test\n" +
                "    void testSomething() {\n" +
                "        assert true;\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.DEFAULT_TEST);
    }

    @Test
    void doesNotFlagMeaningfulName() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "class UserServiceTest {\n" +
                "    @Test\n" +
                "    void testSomething() {\n" +
                "        assert true;\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }
}
