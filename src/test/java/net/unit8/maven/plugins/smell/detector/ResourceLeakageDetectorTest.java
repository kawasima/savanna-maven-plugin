package net.unit8.maven.plugins.smell.detector;

import net.unit8.maven.plugins.smell.DetectionContext;
import net.unit8.maven.plugins.smell.SmellType;
import net.unit8.maven.plugins.smell.TestSmell;
import net.unit8.maven.plugins.smell.parse.TestClassParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceLeakageDetectorTest {
    private final ResourceLeakageDetector detector = new ResourceLeakageDetector();
    private final TestClassParser parser = new TestClassParser();

    @Test
    void detectsResourceNotInTryWithResources() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import java.io.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testRead() throws Exception {\n" +
                "        FileInputStream fis = new FileInputStream(\"data.txt\");\n" +
                "        fis.read();\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.RESOURCE_LEAKAGE);
    }

    @Test
    void doesNotFlagByteArrayStreams() {
        // ByteArrayInputStream/OutputStream's close() is a no-op — flagging
        // is a textbook false positive.
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import java.io.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testBuffers() {\n" +
                "        ByteArrayInputStream in = new ByteArrayInputStream(new byte[10]);\n" +
                "        ByteArrayOutputStream out = new ByteArrayOutputStream();\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }

    @Test
    void detectsResourceCreatedInTryBody() {
        // try-with-resources only manages the resources declared in its header.
        // A `new FileInputStream(...)` created inside the try BODY (not as a resource)
        // is still leaked even though it's lexically inside a TryStmt.
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import java.io.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testLeakInTryBody() throws Exception {\n" +
                "        try (BufferedReader outer = new BufferedReader(new FileReader(\"a.txt\"))) {\n" +
                "            FileInputStream leaked = new FileInputStream(\"b.txt\");\n" +
                "            leaked.read();\n" +
                "        }\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        // FileReader is wrapped as a constructor arg of the resource — that one is fine.
        // FileInputStream in the body is leaked.
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.RESOURCE_LEAKAGE);
    }

    @Test
    void doesNotFlagTryWithResources() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import java.io.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testRead() throws Exception {\n" +
                "        try (FileInputStream fis = new FileInputStream(\"data.txt\")) {\n" +
                "            fis.read();\n" +
                "        }\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }
}
