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
    void detectsThreeFullyQualifiedClassesInSamePackageAsThreeCollaborators() {
        // Regression: receiverName previously returned the LEFTMOST identifier
        // of a chained FieldAccessExpr. So three calls into three different
        // classes in the same package (com.foo.A.x, com.foo.B.y, com.foo.C.z)
        // all collapsed to a single bogus collaborator named "com" — set size
        // 1, threshold not met, no smell. The fix returns the rightmost
        // type-like identifier ("A", "B", "C"), giving the correct count.
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testStatic() {\n" +
                "        com.foo.Alpha.run();\n" +
                "        com.foo.Beta.run();\n" +
                "        com.foo.Gamma.run();\n" +
                "        assertEquals(1, 1);\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getMessage())
                .contains("Alpha").contains("Beta").contains("Gamma");
    }

    @Test
    void resolvesSingleLetterFullyQualifiedClassNames() {
        // Edge case: single-letter class names (pkg.X, pkg.Y, pkg.Z) should
        // still be recognised as collaborators, not collapsed into "pkg".
        DetectionContext ctx = parser.parseSource(
                "import org.junit.jupiter.api.Test;\n" +
                "import static org.junit.jupiter.api.Assertions.*;\n" +
                "class FooTest {\n" +
                "    @Test\n" +
                "    void testSingleLetterClasses() {\n" +
                "        pkg.X.run();\n" +
                "        pkg.Y.run();\n" +
                "        pkg.Z.run();\n" +
                "        assertEquals(1, 1);\n" +
                "    }\n" +
                "}\n"
        );
        List<TestSmell> smells = detector.detect(ctx);
        assertThat(smells).hasSize(1);
        assertThat(smells.get(0).getMessage())
                .contains("X").contains("Y").contains("Z");
    }

    @Test
    void collapsesMultipleCallsIntoSameFullyQualifiedClass() {
        // Counterpart to the above: when all calls go into the same fully-
        // qualified class (com.foo.Bar.x/y/z), the rightmost-type identifier
        // is "Bar" for all of them — correctly collapsed to one collaborator.
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
        // MyEnum.VALUE_A.compute() and MyEnum.VALUE_B.compute() should not
        // count as different collaborators; the chain's type-like identifier
        // is "MyEnum" for both.
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
