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
    void doesNotFlagWriters() {
        // FileWriter/FileOutputStream CREATE the file; an existence check is
        // not expected before writing.
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import java.io.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testWrite() throws Exception {\n" +
                "        FileWriter w = new FileWriter(\"out.txt\");\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }

    @Test
    void detectsFilesReadWithoutExists() {
        // Files.readAllBytes / Files.newInputStream — modern NIO equivalents
        // also need an existence check.
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import java.nio.file.Files;\n" +
                "import java.nio.file.Paths;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testRead() throws Exception {\n" +
                "        byte[] data = Files.readAllBytes(Paths.get(\"data.bin\"));\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
    }

    @Test
    void doesNotFlagFilesReadWithExistsCheck() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import java.nio.file.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testRead() throws Exception {\n" +
                "        Path p = Paths.get(\"data.bin\");\n" +
                "        if (Files.exists(p)) {\n" +
                "            byte[] data = Files.readAllBytes(p);\n" +
                "        }\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }

    @Test
    void detectsFullyQualifiedFilesRead() {
        // Regression: hasFilesReadCall used to require scope to be a bare
        // NameExpr "Files", so a fully-qualified call (FieldAccessExpr scope)
        // was silently missed.
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testRead() throws Exception {\n" +
                "        byte[] data = java.nio.file.Files.readAllBytes(\n" +
                "            java.nio.file.Paths.get(\"data.bin\"));\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
    }

    @Test
    void flagsWhenUnrelatedApiExistsCallIsPresent() {
        // Regression: hasExistenceCheck used to accept any method named
        // exists()/isFile()/etc regardless of scope, so an unrelated API
        // method named exists() on a builder/chain could mask the smell.
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import java.io.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testRead() throws Exception {\n" +
                "        boolean ok = someService.lookup(\"x\").exists();\n" +
                "        FileInputStream fis = new FileInputStream(\"data.txt\");\n" +
                "    }\n" +
                "    Object someService = null;\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
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
