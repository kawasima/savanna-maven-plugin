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
    void detectsStaticFieldWrite() {
        // Writing to a static field is order-dependent regardless of @TestInstance lifecycle.
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "class FooTest {\n" +
                "    private static int counter = 0;\n" +
                "    @Test\n" +
                "    void testIncrement() {\n" +
                "        counter = counter + 1;\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.ORDER_DEPENDENT_TEST);
        assertThat(smells.get(0).getMethodName()).isEqualTo("testIncrement");
    }

    @Test
    void detectsThisQualifiedWriteWhenPerClass() {
        // With @TestInstance(PER_CLASS), the single test instance is shared, so
        // writing to `this.x` between tests creates order dependency.
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import org.junit.jupiter.api.TestInstance;\n" +
                "@TestInstance(TestInstance.Lifecycle.PER_CLASS)\n" +
                "class FooTest {\n" +
                "    private int counter;\n" +
                "    @Test\n" +
                "    void testIncrement() {\n" +
                "        this.counter = this.counter + 1;\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getMethodName()).isEqualTo("testIncrement");
    }

    @Test
    void doesNotFlagInstanceFieldWriteUnderDefaultLifecycle() {
        // Default lifecycle is PER_METHOD: each test gets a fresh instance, so
        // instance-field writes are NOT order-dependent.
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "class FooTest {\n" +
                "    private int counter;\n" +
                "    @Test\n" +
                "    void testA() {\n" +
                "        counter = 1;\n" +
                "    }\n" +
                "    @Test\n" +
                "    void testB() {\n" +
                "        counter = 2;\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
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
