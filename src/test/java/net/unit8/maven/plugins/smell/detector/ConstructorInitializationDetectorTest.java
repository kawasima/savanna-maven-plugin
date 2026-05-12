package net.unit8.maven.plugins.smell.detector;

import net.unit8.maven.plugins.smell.DetectionContext;
import net.unit8.maven.plugins.smell.SmellType;
import net.unit8.maven.plugins.smell.TestSmell;
import net.unit8.maven.plugins.smell.parse.TestClassParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConstructorInitializationDetectorTest {
    private final ConstructorInitializationDetector detector = new ConstructorInitializationDetector();
    private final TestClassParser parser = new TestClassParser();

    @Test
    void detectsConstructor() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "class FooTest {\n" +
                "    private String value;\n" +
                "    FooTest() {\n" +
                "        this.value = \"hello\";\n" +
                "    }\n" +
                "    @Test\n" +
                "    void testSomething() {\n" +
                "        assert true;\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.CONSTRUCTOR_INITIALIZATION);
    }

    @Test
    void doesNotFlagJUnit5InjectionConstructor() {
        // JUnit 5 supports constructor injection of TestInfo/TestReporter etc.
        // A constructor that ONLY captures injected parameters into fields is not
        // an "initialization smell" — it's the only way to receive these objects.
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import org.junit.jupiter.api.TestInfo;\n" +
                "class FooTest {\n" +
                "    private final TestInfo info;\n" +
                "    FooTest(TestInfo info) {\n" +
                "        this.info = info;\n" +
                "    }\n" +
                "    @Test\n" +
                "    void testSomething() {\n" +
                "        assert true;\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }

    @Test
    void doesNotFlagEmptyConstructorBody() {
        // A no-op explicit constructor is just a declaration, not an initialization smell.
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "class FooTest {\n" +
                "    FooTest() { /* nothing */ }\n" +
                "    @Test\n" +
                "    void testSomething() { assert true; }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }

    @Test
    void doesNotFlagTestWithoutConstructor() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testSomething() {\n" +
                "        assert true;\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }
}
