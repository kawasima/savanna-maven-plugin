package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import net.unit8.maven.plugins.smell.*;

import java.util.*;

/**
 * Detects Assertion Roulette: multiple assertions in a single test method
 * without explanatory messages, making it hard to identify which assertion failed.
 *
 * Recognizes both JUnit 5 assertions (message as last parameter) and
 * AssertJ fluent API (message via {@code as()} or {@code describedAs()} in the chain).
 */
public class AssertionRouletteDetector implements SmellDetector {
    private static final Set<String> JUNIT_ASSERTIONS = new TreeSet<>(Arrays.asList(
            "assertEquals", "assertNotEquals",
            "assertTrue", "assertFalse",
            "assertNull", "assertNotNull",
            "assertSame", "assertNotSame",
            "assertArrayEquals"
    ));

    private static final Set<String> ASSERTJ_TERMINAL_METHODS = new TreeSet<>(Arrays.asList(
            "isEqualTo", "isNotEqualTo", "isNull", "isNotNull",
            "isTrue", "isFalse", "isEmpty", "isNotEmpty",
            "isPresent", "isNotPresent", "isInstanceOf",
            "hasSize", "contains", "containsExactly", "containsOnly",
            "startsWith", "endsWith", "matches",
            "isGreaterThan", "isLessThan", "isZero", "isPositive", "isNegative",
            "isSameAs", "isNotSameAs", "isBetween"
    ));

    @Override
    public SmellType type() {
        return SmellType.ASSERTION_ROULETTE;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        for (MethodDeclaration method : context.getTestMethods()) {
            List<MethodCallExpr> allCalls = method.findAll(MethodCallExpr.class);

            // Count JUnit assertions without message
            long junitWithoutMessage = allCalls.stream()
                    .filter(call -> JUNIT_ASSERTIONS.contains(call.getNameAsString()))
                    .filter(this::lacksJUnitMessage)
                    .count();

            // Count AssertJ assertion chains without as()/describedAs()
            long assertjWithoutMessage = allCalls.stream()
                    .filter(call -> ASSERTJ_TERMINAL_METHODS.contains(call.getNameAsString()))
                    .filter(call -> !hasAssertJDescription(call))
                    .count();

            long totalWithoutMessage = junitWithoutMessage + assertjWithoutMessage;

            if (totalWithoutMessage >= 2) {
                smells.add(new TestSmell(
                        SmellType.ASSERTION_ROULETTE,
                        className,
                        method.getNameAsString(),
                        method.getBegin().map(p -> p.line).orElse(0),
                        totalWithoutMessage + " assertions without explanation message"
                ));
            }
        }
        return smells;
    }

    private boolean lacksJUnitMessage(MethodCallExpr call) {
        String name = call.getNameAsString();
        int argCount = call.getArguments().size();

        if (name.equals("assertTrue") || name.equals("assertFalse")
                || name.equals("assertNull") || name.equals("assertNotNull")) {
            return argCount == 1;
        }
        return argCount == 2;
    }

    /**
     * Checks if an AssertJ assertion chain contains as() or describedAs()
     * by walking up the method call chain.
     */
    private boolean hasAssertJDescription(MethodCallExpr terminalCall) {
        MethodCallExpr current = terminalCall;
        while (current.getScope().isPresent() && current.getScope().get() instanceof MethodCallExpr) {
            current = (MethodCallExpr) current.getScope().get();
            String name = current.getNameAsString();
            if (name.equals("as") || name.equals("describedAs")) {
                return true;
            }
        }
        return false;
    }
}
