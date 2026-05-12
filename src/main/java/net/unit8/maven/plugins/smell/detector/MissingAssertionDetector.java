package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import net.unit8.maven.plugins.smell.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MissingAssertionDetector implements SmellDetector {
    private static final Set<String> ASSERTION_METHODS = Set.of(
            // JUnit 5 Assertions
            "assertEquals", "assertNotEquals",
            "assertTrue", "assertFalse",
            "assertNull", "assertNotNull",
            "assertSame", "assertNotSame",
            "assertArrayEquals",
            "assertThrows", "assertDoesNotThrow",
            "assertTimeout", "assertTimeoutPreemptively",
            "assertAll", "assertIterableEquals",
            "assertLinesMatch", "assertInstanceOf",
            "fail",
            // AssertJ
            "assertThat", "assertThatThrownBy",
            "assertThatCode", "assertThatExceptionOfType",
            "assertThatNoException"
    );

    @Override
    public SmellType type() {
        return SmellType.MISSING_ASSERTION;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        Set<String> classMethodNames = context.getTestClass().getMethods().stream()
                .map(MethodDeclaration::getNameAsString)
                .collect(Collectors.toCollection(HashSet::new));

        for (MethodDeclaration method : context.getTestMethods()) {
            // Skip if method expects an exception via assertThrows pattern
            if (method.getAnnotationByName("Disabled").isPresent()) {
                continue;
            }

            boolean hasAssertion = method.findAll(MethodCallExpr.class).stream()
                    .anyMatch(call -> ASSERTION_METHODS.contains(call.getNameAsString())
                            || DetectorHelpers.isAssertionCall(call)
                            || isSelfCustomAssertion(call, classMethodNames));

            if (!hasAssertion) {
                smells.add(new TestSmell(
                        SmellType.MISSING_ASSERTION,
                        className,
                        method.getNameAsString(),
                        method.getBegin().map(p -> p.line).orElse(0),
                        "Test method has no assertion"
                ));
            }
        }
        return smells;
    }

    /**
     * True when {@code call} looks like a custom assertion helper that belongs to
     * the test class itself: the call must be either unqualified ({@code verifyX()})
     * or {@code this}-qualified ({@code this.verifyX()}), and the name must match a
     * method declared in the test class. This avoids treating collaborator calls
     * with coincidentally-similar names (e.g. {@code emailService.verifyUser()}) as
     * assertions.
     */
    private boolean isSelfCustomAssertion(MethodCallExpr call, Set<String> classMethodNames) {
        if (call.getScope().isPresent() && !(call.getScope().get() instanceof ThisExpr)) {
            return false;
        }
        return DetectorHelpers.looksLikeCustomAssertionHelper(call.getNameAsString(), classMethodNames);
    }
}
