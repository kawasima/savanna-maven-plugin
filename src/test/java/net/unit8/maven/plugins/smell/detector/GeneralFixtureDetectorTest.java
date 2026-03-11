package net.unit8.maven.plugins.smell.detector;

import net.unit8.maven.plugins.smell.DetectionContext;
import net.unit8.maven.plugins.smell.SmellType;
import net.unit8.maven.plugins.smell.TestSmell;
import net.unit8.maven.plugins.smell.parse.TestClassParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GeneralFixtureDetectorTest {
    private final GeneralFixtureDetector detector = new GeneralFixtureDetector();
    private final TestClassParser parser = new TestClassParser();

    @Test
    void detectsFieldUsedByFewTests() {
        // extra is assigned in setUp but only used in 1 of 3 tests (< 50%)
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import org.junit.jupiter.api.BeforeEach;\n" +
                "class FooTest {\n" +
                "    private String shared;\n" +
                "    private int extra;\n" +
                "    @BeforeEach\n" +
                "    void setUp() {\n" +
                "        shared = \"hello\";\n" +
                "        extra = 42;\n" +
                "    }\n" +
                "    @Test\n" +
                "    void testA() {\n" +
                "        assert shared != null;\n" +
                "        assert extra == 42;\n" +
                "    }\n" +
                "    @Test\n" +
                "    void testB() {\n" +
                "        assert shared != null;\n" +
                "    }\n" +
                "    @Test\n" +
                "    void testC() {\n" +
                "        assert shared != null;\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.GENERAL_FIXTURE);
        assertThat(smells.get(0).getMessage()).contains("extra");
    }

    @Test
    void doesNotFlagWhenAllFieldsUsedByMostTests() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import org.junit.jupiter.api.BeforeEach;\n" +
                "class FooTest {\n" +
                "    private String shared;\n" +
                "    @BeforeEach\n" +
                "    void setUp() { shared = \"hello\"; }\n" +
                "    @Test\n" +
                "    void testA() { assert shared != null; }\n" +
                "    @Test\n" +
                "    void testB() { assert shared.length() > 0; }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }

    @Test
    void doesNotFlagFieldUsedByMajority() {
        // field used in 2 of 3 tests (>= 50%) should NOT be flagged
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import org.junit.jupiter.api.BeforeEach;\n" +
                "class FooTest {\n" +
                "    private String shared;\n" +
                "    private int counter;\n" +
                "    @BeforeEach\n" +
                "    void setUp() {\n" +
                "        shared = \"hello\";\n" +
                "        counter = 0;\n" +
                "    }\n" +
                "    @Test\n" +
                "    void testA() {\n" +
                "        assert shared != null;\n" +
                "        assert counter == 0;\n" +
                "    }\n" +
                "    @Test\n" +
                "    void testB() {\n" +
                "        assert shared != null;\n" +
                "        assert counter >= 0;\n" +
                "    }\n" +
                "    @Test\n" +
                "    void testC() {\n" +
                "        assert shared != null;\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }
}
