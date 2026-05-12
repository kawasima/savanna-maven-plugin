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
    void countsAssertJChainAsSingleAssertion() {
        // A chained AssertJ assertion `.isEqualTo(y).hasSize(3)` is conceptually
        // one logical check, not two — should NOT be flagged as roulette.
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import java.util.List;\n" +
                "import static org.assertj.core.api.Assertions.assertThat;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testSomething() {\n" +
                "        List<Integer> xs = null;\n" +
                "        assertThat(xs).isNotNull().hasSize(3).contains(1);\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }

    @Test
    void countsAssertJChainWithNonTerminalMidCallAsSingleAssertion() {
        // Regression: the old `isOutermostAssertJTerminal` checked whether the
        // PARENT call name was a terminal — so when a non-terminal AssertJ
        // method (e.g. .satisfies(...)) appears between two terminals, the
        // inner terminal looked outermost and inflated the count.
        //
        // Pre-fix on this test:
        //   - isNotNull parent is satisfies (non-terminal) -> counted outermost
        //   - hasSize parent is ExpressionStmt -> counted outermost
        //   - total = 2 -> ASSERTION_ROULETTE flagged
        // Post-fix:
        //   - isNotNull IS the scope of satisfies -> NOT outermost
        //   - hasSize outermost -> total = 1 -> not flagged
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import java.util.List;\n" +
                "import static org.assertj.core.api.Assertions.assertThat;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testSomething() {\n" +
                "        List<Integer> xs = null;\n" +
                "        assertThat(xs).isNotNull().satisfies(ys -> {});\n" +
                "        assertThat(xs).hasSize(3);\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
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
