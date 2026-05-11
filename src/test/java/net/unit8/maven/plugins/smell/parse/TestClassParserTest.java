package net.unit8.maven.plugins.smell.parse;

import net.unit8.maven.plugins.smell.DetectionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TestClassParserTest {
    private final TestClassParser parser = new TestClassParser();

    @Test
    void parseSource_withTextBlock_succeeds() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class TextBlockSampleTest {\n" +
                "    @Test\n" +
                "    void usesTextBlock() {\n" +
                "        String json = \"\"\"\n" +
                "                {\"k\":\"v\"}\n" +
                "                \"\"\";\n" +
                "        assertEquals(\"{\\\"k\\\":\\\"v\\\"}\\n\", json);\n" +
                "    }\n" +
                "}\n"
        );
        assertThat(ctx).isNotNull();
        assertThat(ctx.getTestMethods()).hasSize(1);
        assertThat(ctx.getTestMethods().get(0).getNameAsString()).isEqualTo("usesTextBlock");
    }

    @Test
    void parseSource_withSourceWithoutClass_returnsNull() {
        DetectionContext ctx = parser.parseSource("// only a comment\n");
        assertThat(ctx).isNull();
    }

    @Test
    void parseSource_withJava25ModuleImport_succeeds() {
        DetectionContext ctx = parser.parseSource(
                "import module java.base;\n" +
                "import org.junit.jupiter.api.Test;\n" +
                "class ModuleImportTest {\n" +
                "    @Test\n" +
                "    void m() {}\n" +
                "}\n"
        );
        assertThat(ctx).isNotNull();
        assertThat(ctx.getTestMethods()).hasSize(1);
    }

    @Test
    void parse_withTextBlockFile_succeeds(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("TextBlockFileTest.java");
        Files.writeString(file,
                "import org.junit.jupiter.api.Test;\n" +
                "class TextBlockFileTest {\n" +
                "    @Test\n" +
                "    void usesTextBlock() {\n" +
                "        String s = \"\"\"\n" +
                "                hello\n" +
                "                \"\"\";\n" +
                "    }\n" +
                "}\n");

        List<DetectionContext> contexts = parser.parse(file);

        assertThat(contexts).hasSize(1);
        assertThat(contexts.get(0).getTestMethods()).hasSize(1);
    }
}
