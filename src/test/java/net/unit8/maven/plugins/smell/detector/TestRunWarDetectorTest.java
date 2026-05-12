package net.unit8.maven.plugins.smell.detector;

import net.unit8.maven.plugins.smell.DetectionContext;
import net.unit8.maven.plugins.smell.SmellType;
import net.unit8.maven.plugins.smell.TestSmell;
import net.unit8.maven.plugins.smell.parse.TestClassParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TestRunWarDetectorTest {
    private final TestRunWarDetector detector = new TestRunWarDetector();
    private final TestClassParser parser = new TestClassParser();

    @Test
    void detectsHardcodedPort() {
        // A fixed port number means two parallel test runs on the same machine
        // will fight over the socket — the canonical "test run war" pattern.
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import java.net.ServerSocket;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testServer() throws Exception {\n" +
                "        ServerSocket s = new ServerSocket(8080);\n" +
                "        s.close();\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.TEST_RUN_WAR);
    }

    @Test
    void detectsHardcodedTempPath() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import java.io.File;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testFile() {\n" +
                "        File f = new File(\"/tmp/test-data.txt\");\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.TEST_RUN_WAR);
    }

    @Test
    void doesNotFlagDynamicPort() {
        // Port 0 means "OS picks a free port" — no collision possible.
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import java.net.ServerSocket;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testServer() throws Exception {\n" +
                "        ServerSocket s = new ServerSocket(0);\n" +
                "        s.close();\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }

    @Test
    void detectsHardcodedPortInSocketHostPortConstructor() {
        // Regression: Socket(String host, int port) keeps the port as the
        // 2nd argument. Previously only argument index 0 was inspected, so
        // Socket("localhost", 8080) was silently missed.
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import java.net.Socket;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testClient() throws Exception {\n" +
                "        Socket s = new Socket(\"localhost\", 8080);\n" +
                "        s.close();\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getMessage()).contains("8080");
    }

    @Test
    void doesNotFlagPathThatMerelySharesPrefix() {
        // Regression: startsWith("/tmp") used to also match "/tmpfile" — a
        // sibling path that isn't actually under /tmp.
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import java.io.File;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testFile() {\n" +
                "        File f = new File(\"/tmpfile.dat\");\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }

    @Test
    void doesNotFlagBareTest() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testName() { assert true; }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }
}
