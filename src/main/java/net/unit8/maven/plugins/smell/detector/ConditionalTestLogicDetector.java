package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import net.unit8.maven.plugins.smell.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ConditionalTestLogicDetector implements SmellDetector {

    /**
     * Calls whose lambda bodies represent the production code under test, not
     * test control flow — conditionals inside their lambdas should be ignored.
     */
    private static final Set<String> ASSERTION_LAMBDA_HOSTS = Set.of(
            "assertThrows", "assertDoesNotThrow",
            "assertTimeout", "assertTimeoutPreemptively",
            "assertThatThrownBy", "assertThatCode", "assertThatNoException",
            "assertThatExceptionOfType"
    );

    @Override
    public SmellType type() {
        return SmellType.CONDITIONAL_TEST_LOGIC;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        for (MethodDeclaration method : context.getTestMethods()) {
            boolean hasConditional = hasConditionalOutsideAssertionLambda(method);

            if (hasConditional) {
                smells.add(new TestSmell(
                        SmellType.CONDITIONAL_TEST_LOGIC,
                        className,
                        method.getNameAsString(),
                        method.getBegin().map(p -> p.line).orElse(0),
                        "Test method contains conditional logic (if/switch/for/while/do-while/?:)"
                ));
            }
        }
        return smells;
    }

    private boolean hasConditionalOutsideAssertionLambda(MethodDeclaration method) {
        List<Node> conditionals = new ArrayList<>();
        conditionals.addAll(method.findAll(IfStmt.class));
        conditionals.addAll(method.findAll(SwitchStmt.class));
        conditionals.addAll(method.findAll(ForStmt.class));
        conditionals.addAll(method.findAll(ForEachStmt.class));
        conditionals.addAll(method.findAll(WhileStmt.class));
        conditionals.addAll(method.findAll(DoStmt.class));
        conditionals.addAll(method.findAll(ConditionalExpr.class));

        return conditionals.stream().anyMatch(node -> !isInsideAssertionLambda(node));
    }

    private boolean isInsideAssertionLambda(Node node) {
        Node current = node;
        while (current != null) {
            if (current instanceof LambdaExpr) {
                Node parent = current.getParentNode().orElse(null);
                if (parent instanceof MethodCallExpr
                        && ASSERTION_LAMBDA_HOSTS.contains(((MethodCallExpr) parent).getNameAsString())) {
                    return true;
                }
            }
            current = current.getParentNode().orElse(null);
        }
        return false;
    }
}
