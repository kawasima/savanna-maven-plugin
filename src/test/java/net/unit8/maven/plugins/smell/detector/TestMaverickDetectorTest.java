package net.unit8.maven.plugins.smell.detector;

import net.unit8.maven.plugins.smell.DetectionContext;
import net.unit8.maven.plugins.smell.TestSmell;
import net.unit8.maven.plugins.smell.parse.TestClassParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TestMaverickDetectorTest {
    private final TestMaverickDetector detector = new TestMaverickDetector();
    private final TestClassParser parser = new TestClassParser();

    @Test
    void detectsTestNotUsingFixture() {
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
                "    void testIndependent() { assert 1 == 1; }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getMethodName()).isEqualTo("testIndependent");
    }

    @Test
    void doesNotFlagWhenNoSetup() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testA() { assert true; }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }
}
