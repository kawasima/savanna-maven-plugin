package net.unit8.maven.plugins.smell.detector;

import net.unit8.maven.plugins.smell.DetectionContext;
import net.unit8.maven.plugins.smell.SmellType;
import net.unit8.maven.plugins.smell.TestSmell;
import net.unit8.maven.plugins.smell.parse.TestClassParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RottenGreenTestDetectorTest {
    private final RottenGreenTestDetector detector = new RottenGreenTestDetector();
    private final TestClassParser parser = new TestClassParser();

    @Test
    void detectsAssertionOnlyInConditional() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testRotten() {\n" +
                "        int x = getValue();\n" +
                "        if (x > 0) {\n" +
                "            assertEquals(1, x);\n" +
                "        }\n" +
                "    }\n" +
                "    int getValue() { return 0; }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.ROTTEN_GREEN_TEST);
        assertThat(smells.get(0).isHeuristic()).isTrue();
    }

    @Test
    void doesNotFlagUnconditionalAssertion() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testGood() {\n" +
                "        assertEquals(1, 1);\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }
}
