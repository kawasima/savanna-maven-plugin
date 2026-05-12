package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import net.unit8.maven.plugins.smell.*;

import java.util.ArrayList;
import java.util.List;

public class RottenGreenTestDetector implements SmellDetector {

    @Override
    public SmellType type() {
        return SmellType.ROTTEN_GREEN_TEST;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        for (MethodDeclaration method : context.getTestMethods()) {
            List<MethodCallExpr> assertions = method.findAll(MethodCallExpr.class).stream()
                    .filter(DetectorHelpers::isAssertionCall)
                    .collect(java.util.stream.Collectors.toList());

            if (assertions.isEmpty()) {
                continue;
            }

            boolean hasUnconditional = assertions.stream()
                    .anyMatch(call -> !isInsideConditional(call));

            if (!hasUnconditional) {
                smells.add(new TestSmell(
                        SmellType.ROTTEN_GREEN_TEST,
                        className,
                        method.getNameAsString(),
                        method.getBegin().map(p -> p.line).orElse(0),
                        "All assertions are inside conditional branches or loops (may never execute)",
                        true
                ));
            }
        }
        return smells;
    }

    /**
     * True when this call lies inside any construct that can skip its body —
     * if/switch/try-catch (the catch arm is conditional on a throw) or any loop
     * that may iterate zero times.
     */
    private boolean isInsideConditional(MethodCallExpr call) {
        Node current = call.getParentNode().orElse(null);
        while (current != null) {
            if (current instanceof IfStmt
                    || current instanceof SwitchStmt
                    || current instanceof ForStmt
                    || current instanceof ForEachStmt
                    || current instanceof WhileStmt
                    || current instanceof DoStmt) {
                return true;
            }
            if (current instanceof TryStmt) {
                TryStmt tryStmt = (TryStmt) current;
                // Inside a catch-clause body? Conditional on the exception being thrown.
                if (tryStmt.getCatchClauses().stream()
                        .anyMatch(cc -> isAncestorOf(cc, call))) {
                    return true;
                }
            }
            if (current instanceof MethodDeclaration) {
                return false;
            }
            current = current.getParentNode().orElse(null);
        }
        return false;
    }

    private boolean isAncestorOf(Node ancestor, Node descendant) {
        Node n = descendant;
        while (n != null) {
            if (n == ancestor) {
                return true;
            }
            n = n.getParentNode().orElse(null);
        }
        return false;
    }
}
