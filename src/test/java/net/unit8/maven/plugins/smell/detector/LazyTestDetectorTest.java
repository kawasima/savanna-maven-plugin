package net.unit8.maven.plugins.smell.detector;

import net.unit8.maven.plugins.smell.DetectionContext;
import net.unit8.maven.plugins.smell.SmellType;
import net.unit8.maven.plugins.smell.TestSmell;
import net.unit8.maven.plugins.smell.parse.TestClassParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LazyTestDetectorTest {
    private final LazyTestDetector detector = new LazyTestDetector();
    private final TestClassParser parser = new TestClassParser();

    @Test
    void detectsMultipleTestsCallingSameMethod() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testA() {\n" +
                "        svc.process();\n" +
                "        assertEquals(1, 1);\n" +
                "    }\n" +
                "    @Test\n" +
                "    void testB() {\n" +
                "        svc.process();\n" +
                "        assertEquals(2, 2);\n" +
                "    }\n" +
                "    @Test\n" +
                "    void testC() {\n" +
                "        svc.process();\n" +
                "        assertEquals(3, 3);\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.LAZY_TEST);
    }

    @Test
    void doesNotFlagDistinctMethods() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testA() {\n" +
                "        svc.methodA();\n" +
                "        assertEquals(1, 1);\n" +
                "    }\n" +
                "    @Test\n" +
                "    void testB() {\n" +
                "        svc.methodB();\n" +
                "        assertEquals(2, 2);\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }
}
