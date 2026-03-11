package net.unit8.maven.plugins.smell.detector;

import net.unit8.maven.plugins.smell.DetectionContext;
import net.unit8.maven.plugins.smell.SmellType;
import net.unit8.maven.plugins.smell.TestSmell;
import net.unit8.maven.plugins.smell.parse.TestClassParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MagicNumberTestDetectorTest {
    private final MagicNumberTestDetector detector = new MagicNumberTestDetector();
    private final TestClassParser parser = new TestClassParser();

    @Test
    void detectsMagicNumber() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testCalc() {\n" +
                "        assertEquals(42, compute());\n" +
                "    }\n" +
                "    int compute() { return 42; }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.MAGIC_NUMBER_TEST);
    }

    @Test
    void doesNotFlagCommonValues() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testCalc() {\n" +
                "        assertEquals(0, compute());\n" +
                "        assertEquals(1, compute());\n" +
                "        assertEquals(2, compute());\n" +
                "        assertEquals(100, compute());\n" +
                "    }\n" +
                "    int compute() { return 0; }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }

    @Test
    void doesNotFlagNegativeOne() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testCalc() {\n" +
                "        assertEquals(-1, compute());\n" +
                "    }\n" +
                "    int compute() { return -1; }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }
}
