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
    void doesNotFlagShortTestWithManyComments() {
        // 30+ source lines of mostly comments should not trip the verbose detector
        // — what matters is statements, not physical lines.
        StringBuilder sb = new StringBuilder();
        sb.append("import org.junit.jupiter.api.Test;\n");
        sb.append("import static org.junit.jupiter.api.Assertions.*;\n");
        sb.append("class FooTest {\n");
        sb.append("    @Test\n");
        sb.append("    void testCommented() {\n");
        for (int i = 0; i < 35; i++) {
            sb.append("        // line ").append(i).append("\n");
        }
        sb.append("        assertEquals(1, 1);\n");
        sb.append("    }\n");
        sb.append("}\n");

        VerboseTestDetector detector = new VerboseTestDetector(10);
        DetectionContext ctx = parser.parseSource(sb.toString());
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }

    @Test
    void doesNotInflateCountFromNestedBlocks() {
        // Regression: findAll(Statement.class) used to also count the
        // surrounding BlockStmt and any nested if/try bodies, inflating the
        // statement count for short tests with control flow.
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testNested() {\n" +
                "        int x = 1;\n" +
                "        if (x > 0) {\n" +
                "            assertEquals(1, x);\n" +
                "        }\n" +
                "    }\n" +
                "}\n"
        );
        VerboseTestDetector detector = new VerboseTestDetector(3);
        List<TestSmell> smells = detector.detect(ctx);
        // 3 real statements (ExpressionStmt, IfStmt, ExpressionStmt) — at
        // threshold, must not be flagged. The earlier implementation also
        // counted the two BlockStmts (method body + if-then), pushing to 5.
        assertThat(smells).isEmpty();
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
