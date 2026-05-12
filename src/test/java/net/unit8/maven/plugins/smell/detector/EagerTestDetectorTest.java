package net.unit8.maven.plugins.smell.detector;

import net.unit8.maven.plugins.smell.DetectionContext;
import net.unit8.maven.plugins.smell.SmellType;
import net.unit8.maven.plugins.smell.TestSmell;
import net.unit8.maven.plugins.smell.parse.TestClassParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EagerTestDetectorTest {
    private final EagerTestDetector detector = new EagerTestDetector();
    private final TestClassParser parser = new TestClassParser();

    @Test
    void detectsTestCallingMultipleCollaborators() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testEager() {\n" +
                "        userService.create();\n" +
                "        orderService.submit();\n" +
                "        emailService.send();\n" +
                "        assertEquals(1, 1);\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.EAGER_TEST);
    }

    @Test
    void doesNotFlagFocusedTest() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testFocused() {\n" +
                "        userService.create();\n" +
                "        assertEquals(1, 1);\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }

    @Test
    void detectsEagerTestThroughThisQualifiedCollaborators() {
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testEager() {\n" +
                "        this.userService.create();\n" +
                "        this.orderService.submit();\n" +
                "        this.emailService.send();\n" +
                "        assertEquals(1, 1);\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getType()).isEqualTo(SmellType.EAGER_TEST);
    }

    @Test
    void doesNotInflateCollaboratorCountFromFullyQualifiedStaticCalls() {
        // Regression: receiverName previously returned the leftmost identifier
        // (package root) of a fully-qualified static call. Three calls into
        // the same class (com.foo.Bar.x/y/z) would then count as one
        // collaborator named "com" plus whatever else — confusing, and could
        // also accidentally pass the threshold via package-root noise.
        // Now we return the rightmost type-like identifier ("Bar"), so
        // calls into the same class collapse to a single collaborator.
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testStatic() {\n" +
                "        com.foo.Bar.alpha();\n" +
                "        com.foo.Bar.beta();\n" +
                "        com.foo.Bar.gamma();\n" +
                "        assertEquals(1, 1);\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }

    @Test
    void collapsesEnumConstantsOfSameTypeIntoOneCollaborator() {
        // Regression: MyEnum.VALUE_A.compute() and MyEnum.VALUE_B.compute()
        // used to count as two different collaborators ("VALUE_A", "VALUE_B").
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testEnum() {\n" +
                "        MyEnum.VALUE_A.compute();\n" +
                "        MyEnum.VALUE_B.compute();\n" +
                "        MyEnum.VALUE_C.compute();\n" +
                "        assertEquals(1, 1);\n" +
                "    }\n" +
                "    enum MyEnum { VALUE_A, VALUE_B, VALUE_C; void compute() {} }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }

    @Test
    void doesNotCountLocalReturnValueVariablesAsCollaborators() {
        // service.create() returns user, service.findById() returns found
        // These local vars should not count as separate collaborators
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testLifecycle() {\n" +
                "        User user = service.create();\n" +
                "        User found = service.findById();\n" +
                "        user.getName();\n" +
                "        found.getEmail();\n" +
                "        assertEquals(1, 1);\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).isEmpty();
    }
}
