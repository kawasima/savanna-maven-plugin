package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import net.unit8.maven.plugins.smell.*;

import java.util.*;

public class RottenGreenTestDetector implements SmellDetector {
    private static final Set<String> ASSERTION_METHODS = new TreeSet<>(Arrays.asList(
            "assertEquals", "assertNotEquals",
            "assertTrue", "assertFalse",
            "assertNull", "assertNotNull",
            "assertSame", "assertNotSame",
            "assertThrows", "assertThat",
            "fail"
    ));

    @Override
    public SmellType type() {
        return SmellType.ROTTEN_GREEN_TEST;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        for (MethodDeclaration method : context.getTestMethods()) {
            // Check assertions only inside conditional branches
            boolean hasConditionalAssertions = false;
            boolean hasUnconditionalAssertions = false;

            // Assertions inside if blocks
            for (IfStmt ifStmt : method.findAll(IfStmt.class)) {
                if (containsAssertion(ifStmt)) {
                    hasConditionalAssertions = true;
                }
            }

            // Assertions inside catch blocks
            for (TryStmt tryStmt : method.findAll(TryStmt.class)) {
                boolean inCatch = tryStmt.getCatchClauses().stream()
                        .anyMatch(cc -> cc.findAll(MethodCallExpr.class).stream()
                                .anyMatch(call -> ASSERTION_METHODS.contains(call.getNameAsString())));
                if (inCatch) {
                    hasConditionalAssertions = true;
                }
            }

            // Check for top-level assertions
            if (method.getBody().isPresent()) {
                hasUnconditionalAssertions = method.getBody().get().getStatements().stream()
                        .anyMatch(stmt -> stmt.findAll(MethodCallExpr.class).stream()
                                .anyMatch(call -> ASSERTION_METHODS.contains(call.getNameAsString()))
                                && stmt.findAll(IfStmt.class).isEmpty()
                                && stmt.findAll(TryStmt.class).isEmpty());
            }

            if (hasConditionalAssertions && !hasUnconditionalAssertions) {
                smells.add(new TestSmell(
                        SmellType.ROTTEN_GREEN_TEST,
                        className,
                        method.getNameAsString(),
                        method.getBegin().map(p -> p.line).orElse(0),
                        "All assertions are inside conditional branches (may never execute)",
                        true
                ));
            }
        }
        return smells;
    }

    private boolean containsAssertion(IfStmt ifStmt) {
        return ifStmt.findAll(MethodCallExpr.class).stream()
                .anyMatch(call -> ASSERTION_METHODS.contains(call.getNameAsString()));
    }
}
