package net.unit8.maven.plugins.smell.detector;

import net.unit8.maven.plugins.smell.DetectionContext;
import net.unit8.maven.plugins.smell.SmellType;
import net.unit8.maven.plugins.smell.TestSmell;
import net.unit8.maven.plugins.smell.parse.TestClassParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderDependentTestDetectorTest {
    private final OrderDependentTestDetector detector = new OrderDependentTestDetector();
    private final TestClassParser parser = new TestClassParser();

    @Test
    void detectsTestMethodOrder() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import org.junit.jupiter.api.TestMethodOrder;\n" +
                "import org.junit.jupiter.api.MethodOrderer;\n" +
                "@TestMethodOrder(MethodOrderer.OrderAnnotation.class)\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testFirst() {\n" +
                "        assert true;\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.ORDER_DEPENDENT_TEST);
    }

    @Test
    void doesNotFlagNormalTestClass() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testA() {\n" +
                "        assertEquals(1, 1);\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }
}
