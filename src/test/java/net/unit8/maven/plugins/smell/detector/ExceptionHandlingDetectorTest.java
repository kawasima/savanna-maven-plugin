package net.unit8.maven.plugins.smell.detector;

import net.unit8.maven.plugins.smell.DetectionContext;
import net.unit8.maven.plugins.smell.SmellType;
import net.unit8.maven.plugins.smell.TestSmell;
import net.unit8.maven.plugins.smell.parse.TestClassParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionHandlingDetectorTest {
    private final ExceptionHandlingDetector detector = new ExceptionHandlingDetector();
    private final TestClassParser parser = new TestClassParser();

    @Test
    void detectsTryCatch() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testSomething() {\n" +
                "        try {\n" +
                "            throw new RuntimeException();\n" +
                "        } catch (Exception e) {\n" +
                "            assert true;\n" +
                "        }\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.EXCEPTION_HANDLING);
    }

    @Test
    void doesNotFlagTryWithResourcesPlusCatch() {
        // try-with-resources + catch combines resource cleanup with handling
        // — the catch is for IOException from close(), not test branching.
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import java.io.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testRead() {\n" +
                "        try (InputStream is = new ByteArrayInputStream(new byte[0])) {\n" +
                "            is.read();\n" +
                "        } catch (IOException e) {\n" +
                "            org.junit.jupiter.api.Assertions.fail(e);\n" +
                "        }\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }

    @Test
    void doesNotFlagTryWithResources() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import java.io.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testSomething() {\n" +
                "        try (InputStream is = new ByteArrayInputStream(new byte[0])) {\n" +
                "            assert true;\n" +
                "        }\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }
}
