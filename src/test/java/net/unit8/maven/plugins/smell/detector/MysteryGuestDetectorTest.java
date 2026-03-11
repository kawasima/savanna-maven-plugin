package net.unit8.maven.plugins.smell.detector;

import net.unit8.maven.plugins.smell.DetectionContext;
import net.unit8.maven.plugins.smell.SmellType;
import net.unit8.maven.plugins.smell.TestSmell;
import net.unit8.maven.plugins.smell.parse.TestClassParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MysteryGuestDetectorTest {
    private final MysteryGuestDetector detector = new MysteryGuestDetector();
    private final TestClassParser parser = new TestClassParser();

    @Test
    void detectsFileAccess() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import java.io.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testFile() throws Exception {\n" +
                "        FileReader reader = new FileReader(\"config.xml\");\n" +
                "        assert reader != null;\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.MYSTERY_GUEST);
    }

    @Test
    void doesNotFlagPureLogic() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testPure() {\n" +
                "        assertEquals(4, 2 + 2);\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }
}
