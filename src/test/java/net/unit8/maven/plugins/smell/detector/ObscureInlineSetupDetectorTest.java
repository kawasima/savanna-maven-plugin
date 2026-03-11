package net.unit8.maven.plugins.smell.detector;

import net.unit8.maven.plugins.smell.DetectionContext;
import net.unit8.maven.plugins.smell.SmellType;
import net.unit8.maven.plugins.smell.TestSmell;
import net.unit8.maven.plugins.smell.parse.TestClassParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ObscureInlineSetupDetectorTest {
    private final TestClassParser parser = new TestClassParser();

    @Test
    void detectsExcessiveSetup() {
        StringBuilder sb = new StringBuilder();
        sb.append("import org.junit.jupiter.api.Test;\n");
        sb.append("import static org.junit.jupiter.api.Assertions.*;\n");
        sb.append("class FooTest {\n");
        sb.append("    @Test\n");
        sb.append("    void testSetup() {\n");
        for (int i = 0; i < 12; i++) {
            sb.append("        int v").append(i).append(" = ").append(i).append(";\n");
        }
        sb.append("        assertEquals(1, 1);\n");
        sb.append("    }\n");
        sb.append("}\n");

        ObscureInlineSetupDetector detector = new ObscureInlineSetupDetector(10);
        DetectionContext ctx = parser.parseSource(sb.toString());
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.OBSCURE_INLINE_SETUP);
    }

    @Test
    void doesNotFlagMinimalSetup() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testMinimal() {\n" +
                "        int x = 1;\n" +
                "        assertEquals(1, x);\n" +
                "    }\n" +
                "}\n"
        );
        ObscureInlineSetupDetector detector = new ObscureInlineSetupDetector(10);
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }
}
