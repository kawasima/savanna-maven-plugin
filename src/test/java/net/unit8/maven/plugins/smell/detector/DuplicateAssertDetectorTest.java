package net.unit8.maven.plugins.smell.detector;

import net.unit8.maven.plugins.smell.DetectionContext;
import net.unit8.maven.plugins.smell.SmellType;
import net.unit8.maven.plugins.smell.TestSmell;
import net.unit8.maven.plugins.smell.parse.TestClassParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DuplicateAssertDetectorTest {
    private final DuplicateAssertDetector detector = new DuplicateAssertDetector();
    private final TestClassParser parser = new TestClassParser();

    @Test
    void detectsDuplicateAssertions() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testDup() {\n" +
                "        assertEquals(1, getValue());\n" +
                "        assertEquals(1, getValue());\n" +
                "    }\n" +
                "    int getValue() { return 1; }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.DUPLICATE_ASSERT);
    }

    @Test
    void detectsDuplicateAssertJTerminalOnSameValue() {
        // The same terminal applied to assertThat of the same expression twice
        // is a duplicate assertion even though only the chain root differs slightly.
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.assertj.core.api.Assertions.assertThat;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testDup() {\n" +
                "        assertThat(getValue()).hasSize(3);\n" +
                "        assertThat(getValue()).hasSize(3);\n" +
                "    }\n" +
                "    java.util.List<Integer> getValue() { return null; }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
    }

    @Test
    void detectsDuplicateAssertJChains() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.assertj.core.api.Assertions.assertThat;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testDup() {\n" +
                "        assertThat(getValue()).isEqualTo(1);\n" +
                "        assertThat(getValue()).isEqualTo(1);\n" +
                "    }\n" +
                "    int getValue() { return 1; }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.DUPLICATE_ASSERT);
    }

    @Test
    void doesNotFlagDistinctAssertions() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testDistinct() {\n" +
                "        assertEquals(1, getA());\n" +
                "        assertEquals(2, getB());\n" +
                "    }\n" +
                "    int getA() { return 1; }\n" +
                "    int getB() { return 2; }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }
}
