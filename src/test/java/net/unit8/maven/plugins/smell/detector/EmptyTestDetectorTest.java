package net.unit8.maven.plugins.smell.detector;

import net.unit8.maven.plugins.smell.DetectionContext;
import net.unit8.maven.plugins.smell.SmellType;
import net.unit8.maven.plugins.smell.TestSmell;
import net.unit8.maven.plugins.smell.parse.TestClassParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmptyTestDetectorTest {
    private final EmptyTestDetector detector = new EmptyTestDetector();
    private final TestClassParser parser = new TestClassParser();

    @Test
    void detectsEmptyTestMethod() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void shouldDoSomething() {\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.EMPTY_TEST);
        assertThat(smells.get(0).getMethodName()).isEqualTo("shouldDoSomething");
    }

    @Test
    void doesNotFlagDisabledEmptyStub() {
        // A @Disabled empty stub is reported by IgnoredTest already.
        // Don't double-report it as EMPTY.
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import org.junit.jupiter.api.Disabled;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    @Disabled\n" +
                "    void notYet() {\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }

    @Test
    void doesNotFlagJUnit4IgnoredEmptyStub() {
        // Regression: previously only @Disabled was skipped, so an @Ignore
        // empty stub got double-reported (here + IgnoredTestDetector).
        DetectionContext ctx = parser.parseSource(
                "import org.junit.Test;\n" +
                "import org.junit.Ignore;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    @Ignore\n" +
                "    public void notYet() {\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }

    @Test
    void doesNotFlagAnyMethodWhenEnclosingClassIsDisabled() {
        // Regression: class-level @Disabled was not propagated; every empty
        // method inside it got double-reported.
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import org.junit.jupiter.api.Disabled;\n" +
                "@Disabled\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void firstStub() {}\n" +
                "    @Test\n" +
                "    void secondStub() {}\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }

    @Test
    void detectsEmptyStatementOnlyBody() {
        // `{ ; }` — one EmptyStmt, no actual work.
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void noop() {\n" +
                "        ;\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.EMPTY_TEST);
    }

    @Test
    void doesNotFlagNonEmptyTest() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void shouldDoSomething() {\n" +
                "        assertEquals(1, 1);\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }
}
