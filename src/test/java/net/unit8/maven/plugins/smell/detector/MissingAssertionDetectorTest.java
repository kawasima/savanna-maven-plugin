package net.unit8.maven.plugins.smell.detector;

import net.unit8.maven.plugins.smell.DetectionContext;
import net.unit8.maven.plugins.smell.SmellType;
import net.unit8.maven.plugins.smell.TestSmell;
import net.unit8.maven.plugins.smell.parse.TestClassParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MissingAssertionDetectorTest {
    private final MissingAssertionDetector detector = new MissingAssertionDetector();
    private final TestClassParser parser = new TestClassParser();

    @Test
    void detectsTestWithoutAssertion() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testSomething() {\n" +
                "        String s = \"hello\";\n" +
                "        s.length();\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.MISSING_ASSERTION);
    }

    @Test
    void doesNotFlagTestWithJUnit5Assertion() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testSomething() {\n" +
                "        assertEquals(1, 1);\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }

    @Test
    void doesNotFlagTestWithMockitoVerify() {
        // A test that only calls Mockito.verify(...) still has an implicit
        // assertion — flagging this would noise-spam interaction-based tests.
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.mockito.Mockito.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testSomething() {\n" +
                "        verify(mock).save();\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }

    @Test
    void doesNotFlagTestWithCustomAssertHelper() {
        // assertUser(...) is a custom helper that delegates to real assertions.
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testSomething() {\n" +
                "        assertUserWasCreated();\n" +
                "    }\n" +
                "    void assertUserWasCreated() {}\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }

    @Test
    void flagsTestWhereCustomAssertNameIsNotDefinedInClass() {
        // Regression: previously any method starting with assert/verify/check/expect
        // counted as an assertion, suppressing the smell even when the method was
        // actually a production helper (e.g. verifyEmailToken on a service).
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testSomething() {\n" +
                "        emailService.verifyEmailToken(\"abc\");\n" +
                "    }\n" +
                "    Object emailService = null;\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.MISSING_ASSERTION);
    }

    @Test
    void doesNotFlagTestWithAssertThat() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.assertj.core.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testSomething() {\n" +
                "        assertThat(\"hello\").isEqualTo(\"hello\");\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }
}
