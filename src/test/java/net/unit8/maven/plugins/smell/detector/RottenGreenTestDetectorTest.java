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
    void detectsAssertionOnlyInsideForLoop() {
        // A for-loop that may iterate zero times — the assertion might never run.
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testRotten() {\n" +
                "        java.util.List<Integer> items = getItems();\n" +
                "        for (Integer i : items) {\n" +
                "            assertEquals(1, i);\n" +
                "        }\n" +
                "    }\n" +
                "    java.util.List<Integer> getItems() { return java.util.Collections.emptyList(); }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.ROTTEN_GREEN_TEST);
    }

    @Test
    void detectsAssertArrayEqualsOnlyInsideForLoop() {
        // Regression: a private hardcoded set used to omit assertArrayEquals,
        // so a test whose only assertion was assertArrayEquals inside a loop
        // would be silently ignored.
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testRotten() {\n" +
                "        int[][] rows = getRows();\n" +
                "        for (int[] row : rows) {\n" +
                "            assertArrayEquals(new int[]{1, 2}, row);\n" +
                "        }\n" +
                "    }\n" +
                "    int[][] getRows() { return new int[0][]; }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.ROTTEN_GREEN_TEST);
    }

    @Test
    void doesNotFlagAssertionInsideDoWhile() {
        // Regression: a do/while body executes at least once, so an assertion
        // inside it is guaranteed to run — must NOT be flagged.
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testDoWhile() {\n" +
                "        int i = 0;\n" +
                "        do {\n" +
                "            assertEquals(0, i);\n" +
                "            i++;\n" +
                "        } while (i < 1);\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
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
