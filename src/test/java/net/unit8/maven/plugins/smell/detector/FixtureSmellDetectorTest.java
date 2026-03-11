package net.unit8.maven.plugins.smell.detector;

import net.unit8.maven.plugins.smell.DetectionContext;
import net.unit8.maven.plugins.smell.SmellType;
import net.unit8.maven.plugins.smell.TestSmell;
import net.unit8.maven.plugins.smell.parse.TestClassParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FixtureSmellDetectorTest {
    private final FixtureSmellDetector detector = new FixtureSmellDetector();
    private final TestClassParser parser = new TestClassParser();

    @Test
    void detectsDeadField() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "class FooTest {\n" +
                "    private String unused;\n" +
                "    @Test\n" +
                "    void testA() {\n" +
                "        assert true;\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.DEAD_FIELD);
        assertThat(smells.get(0).getMessage()).contains("unused");
    }

    @Test
    void doesNotFlagUsedField() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "class FooTest {\n" +
                "    private String value;\n" +
                "    @Test\n" +
                "    void testA() {\n" +
                "        assert value != null;\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }
}
