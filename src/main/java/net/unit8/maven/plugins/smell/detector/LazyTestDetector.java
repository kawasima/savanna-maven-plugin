package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import net.unit8.maven.plugins.smell.*;

import java.util.*;

/**
 * Detects when multiple test methods test the exact same production method call
 * (same scope and method name), suggesting test duplication.
 */
public class LazyTestDetector implements SmellDetector {
    private static final Set<String> EXCLUDED_METHODS = Set.of(
            "assertEquals", "assertNotEquals", "assertTrue", "assertFalse",
            "assertNull", "assertNotNull", "assertThrows", "assertThat",
            "assertThatThrownBy", "assertAll", "fail",
            "when", "given", "verify", "mock", "spy",
            "toString", "hashCode", "equals", "getClass"
    );

    @Override
    public SmellType type() {
        return SmellType.LAZY_TEST;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        // Map: "scope.method" -> list of test method names that call it
        Map<String, List<String>> callToTests = new LinkedHashMap<>();

        for (MethodDeclaration method : context.getTestMethods()) {
            Set<String> calls = new HashSet<>();
            for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
                if (EXCLUDED_METHODS.contains(call.getNameAsString())) {
                    continue;
                }
                String scope = call.getScope()
                        .filter(s -> s instanceof NameExpr)
                        .map(s -> ((NameExpr) s).getNameAsString())
                        .orElse(null);
                if (scope != null) {
                    calls.add(scope + "." + call.getNameAsString());
                }
            }
            for (String call : calls) {
                callToTests.computeIfAbsent(call, k -> new ArrayList<>())
                        .add(method.getNameAsString());
            }
        }

        // Find production methods tested by multiple test methods
        Set<String> reported = new HashSet<>();
        for (Map.Entry<String, List<String>> entry : callToTests.entrySet()) {
            if (entry.getValue().size() >= 3) {
                String key = String.join(",", entry.getValue());
                if (reported.add(key)) {
                    smells.add(new TestSmell(
                            SmellType.LAZY_TEST,
                            className,
                            null,
                            context.getTestClass().getBegin().map(p -> p.line).orElse(0),
                            entry.getValue().size() + " test methods all call " + entry.getKey() + "()",
                            true
                    ));
                }
            }
        }
        return smells;
    }
}
