package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import net.unit8.maven.plugins.smell.*;

import java.util.*;

public class SensitiveEqualityDetector implements SmellDetector {
    private static final Set<String> EQUALITY_ASSERTIONS = new TreeSet<>(Arrays.asList(
            "assertEquals", "assertNotEquals"
    ));

    @Override
    public SmellType type() {
        return SmellType.SENSITIVE_EQUALITY;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        for (MethodDeclaration method : context.getTestMethods()) {
            for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
                if (!EQUALITY_ASSERTIONS.contains(call.getNameAsString())) {
                    continue;
                }
                boolean usesToString = call.getArguments().stream()
                        .anyMatch(this::containsToString);

                if (usesToString) {
                    smells.add(new TestSmell(
                            SmellType.SENSITIVE_EQUALITY,
                            className,
                            method.getNameAsString(),
                            call.getBegin().map(p -> p.line).orElse(0),
                            "Assertion uses toString() for equality comparison"
                    ));
                }
            }
        }
        return smells;
    }

    private boolean containsToString(Expression expr) {
        return expr.findAll(MethodCallExpr.class).stream()
                .anyMatch(call -> call.getNameAsString().equals("toString")
                        && call.getArguments().isEmpty());
    }
}
