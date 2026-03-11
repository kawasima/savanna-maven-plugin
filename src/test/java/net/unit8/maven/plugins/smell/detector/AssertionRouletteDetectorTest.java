package net.unit8.maven.plugins.smell.detector;

import net.unit8.maven.plugins.smell.DetectionContext;
import net.unit8.maven.plugins.smell.SmellType;
import net.unit8.maven.plugins.smell.TestSmell;
import net.unit8.maven.plugins.smell.parse.TestClassParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AssertionRouletteDetectorTest {
    private final AssertionRouletteDetector detector = new AssertionRouletteDetector();
    private final TestClassParser parser = new TestClassParser();

    @Test
    void detectsMultipleJUnitAssertionsWithoutMessages() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testSomething() {\n" +
                "        assertEquals(1, 1);\n" +
                "        assertEquals(2, 2);\n" +
                "        assertTrue(true);\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.ASSERTION_ROULETTE);
    }

    @Test
    void doesNotFlagJUnitAssertionsWithMessages() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testSomething() {\n" +
                "        assertEquals(1, 1, \"first check\");\n" +
                "        assertEquals(2, 2, \"second check\");\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }

    @Test
    void doesNotFlagSingleAssertion() {
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

    @Test
    void detectsMultipleAssertJAssertionsWithoutAs() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.assertj.core.api.Assertions.assertThat;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testSomething() {\n" +
                "        assertThat(a).isEqualTo(1);\n" +
                "        assertThat(b).isNotNull();\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.ASSERTION_ROULETTE);
    }

    @Test
    void doesNotFlagAssertJAssertionsWithAs() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.assertj.core.api.Assertions.assertThat;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testSomething() {\n" +
                "        assertThat(a).as(\"check a\").isEqualTo(1);\n" +
                "        assertThat(b).describedAs(\"check b\").isNotNull();\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }
}
