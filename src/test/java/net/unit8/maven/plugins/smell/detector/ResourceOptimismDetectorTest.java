package net.unit8.maven.plugins.smell.detector;

import net.unit8.maven.plugins.smell.DetectionContext;
import net.unit8.maven.plugins.smell.SmellType;
import net.unit8.maven.plugins.smell.TestSmell;
import net.unit8.maven.plugins.smell.parse.TestClassParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceOptimismDetectorTest {
    private final ResourceOptimismDetector detector = new ResourceOptimismDetector();
    private final TestClassParser parser = new TestClassParser();

    @Test
    void detectsFileAccessWithoutExistsCheck() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import java.io.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testRead() throws Exception {\n" +
                "        FileInputStream fis = new FileInputStream(\"data.txt\");\n" +
                "        assert true;\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.RESOURCE_OPTIMISM);
    }

    @Test
    void doesNotFlagWithExistsCheck() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import java.io.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testRead() throws Exception {\n" +
                "        File f = new File(\"data.txt\");\n" +
                "        if (f.exists()) {\n" +
                "            FileInputStream fis = new FileInputStream(f);\n" +
                "        }\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }
}
