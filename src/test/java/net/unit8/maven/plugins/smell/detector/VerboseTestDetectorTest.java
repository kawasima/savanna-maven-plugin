package net.unit8.maven.plugins.smell.detector;

import net.unit8.maven.plugins.smell.DetectionContext;
import net.unit8.maven.plugins.smell.SmellType;
import net.unit8.maven.plugins.smell.TestSmell;
import net.unit8.maven.plugins.smell.parse.TestClassParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VerboseTestDetectorTest {
    private final TestClassParser parser = new TestClassParser();

    @Test
    void detectsVerboseTest() {
        StringBuilder sb = new StringBuilder();
        sb.append("import org.junit.jupiter.api.Test;\n");
        sb.append("import static org.junit.jupiter.api.Assertions.*;\n");
        sb.append("class FooTest {\n");
        sb.append("    @Test\n");
        sb.append("    void testVerbose() {\n");
        for (int i = 0; i < 35; i++) {
            sb.append("        int v").append(i).append(" = ").append(i).append(";\n");
        }
        sb.append("        assertEquals(1, 1);\n");
        sb.append("    }\n");
        sb.append("}\n");

        VerboseTestDetector detector = new VerboseTestDetector(30);
        DetectionContext ctx = parser.parseSource(sb.toString());
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.VERBOSE_TEST);
    }

    @Test
    void doesNotFlagShortTest() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testShort() {\n" +
                "        assertEquals(1, 1);\n" +
                "    }\n" +
                "}\n"
        );
        VerboseTestDetector detector = new VerboseTestDetector(30);
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }
}
