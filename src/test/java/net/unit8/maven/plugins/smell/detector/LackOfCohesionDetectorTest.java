package net.unit8.maven.plugins.smell.detector;

import net.unit8.maven.plugins.smell.DetectionContext;
import net.unit8.maven.plugins.smell.SmellType;
import net.unit8.maven.plugins.smell.TestSmell;
import net.unit8.maven.plugins.smell.parse.TestClassParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LackOfCohesionDetectorTest {
    private final LackOfCohesionDetector detector = new LackOfCohesionDetector();
    private final TestClassParser parser = new TestClassParser();

    @Test
    void detectsLowCohesion() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testA() {\n" +
                "        userService.create();\n" +
                "        assertEquals(1, 1);\n" +
                "    }\n" +
                "    @Test\n" +
                "    void testB() {\n" +
                "        orderService.submit();\n" +
                "        assertEquals(2, 2);\n" +
                "    }\n" +
                "    @Test\n" +
                "    void testC() {\n" +
                "        emailService.send();\n" +
                "        assertEquals(3, 3);\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.LACK_OF_COHESION);
    }

    @Test
    void doesNotFlagCohesiveTests() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testA() {\n" +
                "        svc.create();\n" +
                "        assertEquals(1, 1);\n" +
                "    }\n" +
                "    @Test\n" +
                "    void testB() {\n" +
                "        svc.update();\n" +
                "        assertEquals(2, 2);\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }
}
