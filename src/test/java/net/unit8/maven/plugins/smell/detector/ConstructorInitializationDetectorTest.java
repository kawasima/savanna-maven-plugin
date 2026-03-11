package net.unit8.maven.plugins.smell.detector;

import net.unit8.maven.plugins.smell.DetectionContext;
import net.unit8.maven.plugins.smell.SmellType;
import net.unit8.maven.plugins.smell.TestSmell;
import net.unit8.maven.plugins.smell.parse.TestClassParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConstructorInitializationDetectorTest {
    private final ConstructorInitializationDetector detector = new ConstructorInitializationDetector();
    private final TestClassParser parser = new TestClassParser();

    @Test
    void detectsConstructor() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "class FooTest {\n" +
                "    private String value;\n" +
                "    FooTest() {\n" +
                "        this.value = \"hello\";\n" +
                "    }\n" +
                "    @Test\n" +
                "    void testSomething() {\n" +
                "        assert true;\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.CONSTRUCTOR_INITIALIZATION);
    }

    @Test
    void doesNotFlagTestWithoutConstructor() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testSomething() {\n" +
                "        assert true;\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }
}
