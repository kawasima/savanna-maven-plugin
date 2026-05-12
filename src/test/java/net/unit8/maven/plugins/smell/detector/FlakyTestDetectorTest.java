package net.unit8.maven.plugins.smell.detector;

import net.unit8.maven.plugins.smell.DetectionContext;
import net.unit8.maven.plugins.smell.SmellType;
import net.unit8.maven.plugins.smell.TestSmell;
import net.unit8.maven.plugins.smell.parse.TestClassParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FlakyTestDetectorTest {
    private final FlakyTestDetector detector = new FlakyTestDetector();
    private final TestClassParser parser = new TestClassParser();

    @Test
    void detectsMultipleFlakynessIndicators() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import java.util.Random;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testFlaky() throws Exception {\n" +
                "        Random r = new Random();\n" +
                "        Thread.sleep(100);\n" +
                "        assert r.nextInt() > 0;\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.FLAKY_TEST);
        assertThat(smells.get(0).isHeuristic()).isTrue();
    }

    @Test
    void detectsSleepAlone() {
        // Thread.sleep + assertion is the canonical "flaky timing" pattern.
        // We flag it on its own — waiting on wall-clock time is unreliable.
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testSleep() throws Exception {\n" +
                "        Thread.sleep(100);\n" +
                "        assertEquals(1, 1);\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
    }

    @Test
    void detectsRandomUUIDComparedToFixedExpected() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import java.util.UUID;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testUUID() {\n" +
                "        UUID id = UUID.randomUUID();\n" +
                "        assert id != null;\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
    }

    @Test
    void detectsNewDate() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import java.util.Date;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testDate() {\n" +
                "        Date d = new Date();\n" +
                "        assert d != null;\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
    }

    @Test
    void doesNotFlagBenignTest() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testBenign() {\n" +
                "        assertEquals(1, 1);\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }

    @Test
    void doesNotFlagUserDefinedSleepOnMockOrHelper() {
        // Regression: previously the detector matched any method named sleep,
        // including user-defined helpers and mock objects.
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testSleep() {\n" +
                "        scheduler.sleep(100);\n" +
                "        assertEquals(1, 1);\n" +
                "    }\n" +
                "    Object scheduler = null;\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }

    @Test
    void doesNotFlagUserDefinedNowOrCurrentTimeMillis() {
        // Regression: previously the detector matched any method named now,
        // currentTimeMillis, or nanoTime regardless of receiver — so calls on
        // user objects (cursor.now(), helper.currentTimeMillis()) were flagged.
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testNow() {\n" +
                "        Object x = cursor.now();\n" +
                "        long t = helper.currentTimeMillis();\n" +
                "        assertEquals(1, 1);\n" +
                "    }\n" +
                "    Object cursor = null;\n" +
                "    Object helper = null;\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }

    @Test
    void detectsInstantNow() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import java.time.Instant;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testInstantNow() {\n" +
                "        Instant t = Instant.now();\n" +
                "        assert t != null;\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
    }

    @Test
    void detectsSystemCurrentTimeMillis() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testSystemTime() {\n" +
                "        long t = System.currentTimeMillis();\n" +
                "        assert t > 0;\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
    }
}
