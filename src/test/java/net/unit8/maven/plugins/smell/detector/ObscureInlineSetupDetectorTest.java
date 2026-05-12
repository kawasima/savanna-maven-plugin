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
    void doesNotTerminateSetupOnAssertionInsideLambda() {
        // A setup line that happens to contain `assertNotNull` inside a lambda
        // (e.g. as a guard) shouldn't be treated as the first assertion.
        StringBuilder sb = new StringBuilder();
        sb.append("import org.junit.jupiter.api.Test;\n");
        sb.append("import static org.junit.jupiter.api.Assertions.*;\n");
        sb.append("import java.util.Arrays;\n");
        sb.append("class FooTest {\n");
        sb.append("    @Test\n");
        sb.append("    void testSetup() {\n");
        sb.append("        Arrays.asList(1, 2, 3).forEach(x -> assertNotNull(x));\n");
        for (int i = 0; i < 12; i++) {
            sb.append("        int v").append(i).append(" = ").append(i).append(";\n");
        }
        sb.append("        assertEquals(1, 1);\n");
        sb.append("    }\n");
        sb.append("}\n");

        ObscureInlineSetupDetector detector = new ObscureInlineSetupDetector(10);
        DetectionContext ctx = parser.parseSource(sb.toString());
        List<TestSmell> smells = detector.detect(ctx);
        // The lambda assertion is on line 1 of the body but is setup, not a real
        // assertion statement — the 12 trivial setups still trip the threshold.
        assertThat(smells).hasSize(1);
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
