package net.unit8.maven.plugins.smell.detector;

import net.unit8.maven.plugins.smell.DetectionContext;
import net.unit8.maven.plugins.smell.SmellType;
import net.unit8.maven.plugins.smell.TestSmell;
import net.unit8.maven.plugins.smell.parse.TestClassParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConditionalTestLogicDetectorTest {
    private final ConditionalTestLogicDetector detector = new ConditionalTestLogicDetector();
    private final TestClassParser parser = new TestClassParser();

    @Test
    void detectsIfStatement() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testSomething() {\n" +
                "        if (true) {\n" +
                "            assert true;\n" +
                "        }\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.CONDITIONAL_TEST_LOGIC);
    }

    @Test
    void detectsForLoop() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testSomething() {\n" +
                "        for (int i = 0; i < 10; i++) {\n" +
                "            assert true;\n" +
                "        }\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
    }

    @Test
    void doesNotFlagSimpleTest() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testSomething() {\n" +
                "        assertEquals(1, 1);\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }
}
