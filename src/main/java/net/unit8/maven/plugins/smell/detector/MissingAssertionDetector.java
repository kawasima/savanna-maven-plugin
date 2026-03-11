package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import net.unit8.maven.plugins.smell.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class MissingAssertionDetector implements SmellDetector {
    private static final Set<String> ASSERTION_METHODS = new TreeSet<>(Arrays.asList(
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
            "assertThatNoException",
            // Hamcrest
            "assertThat"
    ));

    @Override
    public SmellType type() {
        return SmellType.MISSING_ASSERTION;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        for (MethodDeclaration method : context.getTestMethods()) {
            // Skip if method expects an exception via assertThrows pattern
            if (method.getAnnotationByName("Disabled").isPresent()) {
                continue;
            }

            boolean hasAssertion = method.findAll(MethodCallExpr.class).stream()
                    .anyMatch(call -> ASSERTION_METHODS.contains(call.getNameAsString()));

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
}
