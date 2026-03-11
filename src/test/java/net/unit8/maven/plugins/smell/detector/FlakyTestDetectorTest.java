package net.unit8.maven.plugins.smell.detector;

import net.unit8.maven.plugins.smell.DetectionContext;
import net.unit8.maven.plugins.smell.SmellType;
import net.unit8.maven.plugins.smell.TestSmell;
import net.unit8.maven.plugins.smell.parse.TestClassParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FlakyTestDetectorTest {
    private final FlakyTestDetector detector = new FlakyTestDetector();
    private final TestClassParser parser = new TestClassParser();

    @Test
    void detectsMultipleFlakynessIndicators() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import java.util.Random;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testFlaky() throws Exception {\n" +
                "        Random r = new Random();\n" +
                "        Thread.sleep(100);\n" +
                "        assert r.nextInt() > 0;\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.FLAKY_TEST);
        assertThat(smells.get(0).isHeuristic()).isTrue();
    }

    @Test
    void doesNotFlagSingleIndicator() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testSleep() throws Exception {\n" +
                "        Thread.sleep(100);\n" +
                "        assert true;\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }
}
