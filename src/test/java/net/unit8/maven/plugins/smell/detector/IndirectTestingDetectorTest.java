package net.unit8.maven.plugins.smell.detector;

import net.unit8.maven.plugins.smell.DetectionContext;
import net.unit8.maven.plugins.smell.SmellType;
import net.unit8.maven.plugins.smell.TestSmell;
import net.unit8.maven.plugins.smell.parse.TestClassParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IndirectTestingDetectorTest {
    private final IndirectTestingDetector detector = new IndirectTestingDetector();
    private final TestClassParser parser = new TestClassParser();

    @Test
    void detectsIndirectTesting() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class UserServiceTest {\n" +
                "    @Test\n" +
                "    void testUser() {\n" +
                "        repo.save();\n" +
                "        repo.findById();\n" +
                "        repo.delete();\n" +
                "        userService.create();\n" +
                "        assertEquals(1, 1);\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.INDIRECT_TESTING);
    }

    @Test
    void doesNotFlagDirectTesting() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class UserServiceTest {\n" +
                "    @Test\n" +
                "    void testUser() {\n" +
                "        userService.create();\n" +
                "        assertEquals(1, 1);\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }

    @Test
    void doesNotFlagWhenFieldTypeMatchesProductionClass() {
        // Field named "service" but typed as UserService should be recognized
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class UserServiceTest {\n" +
                "    private UserService service;\n" +
                "    @Test\n" +
                "    void testUser() {\n" +
                "        service.create();\n" +
                "        service.findById();\n" +
                "        helper.doSomething();\n" +
                "        assertEquals(1, 1);\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }

    @Test
    void doesNotFlagWhenExpectedClassHasMajorityOfCalls() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class UserServiceTest {\n" +
                "    @Test\n" +
                "    void testUser() {\n" +
                "        userService.create();\n" +
                "        userService.findById();\n" +
                "        userService.update();\n" +
                "        helper.doSomething();\n" +
                "        assertEquals(1, 1);\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }
}
