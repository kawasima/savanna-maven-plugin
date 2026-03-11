package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import net.unit8.maven.plugins.smell.*;

import java.util.*;

/**
 * Detects magic number literals in test assertions.
 *
 * Numbers like 0, 1, -1, 2 and small commonly used values are excluded.
 * Only flags when the numeric literal is a direct argument to an assertion,
 * not wrapped in a named constant or variable.
 */
public class MagicNumberTestDetector implements SmellDetector {
    private static final Set<String> ASSERTION_METHODS = Set.of(
            "assertEquals", "assertNotEquals",
            "assertSame", "assertNotSame",
            "assertArrayEquals",
            "assertThat"
    );

    private static final Set<String> EXCLUDED_VALUES = new HashSet<>(Arrays.asList(
            "0", "1", "2", "-1", "-2",
            "0L", "1L", "2L", "-1L",
            "0.0", "1.0", "-1.0", "0.5",
            "0f", "1f", "0d", "1d",
            "10", "100", "1000"
    ));

    @Override
    public SmellType type() {
        return SmellType.MAGIC_NUMBER_TEST;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        for (MethodDeclaration method : context.getTestMethods()) {
            List<MethodCallExpr> assertions = method.findAll(MethodCallExpr.class,
                    call -> ASSERTION_METHODS.contains(call.getNameAsString()));

            for (MethodCallExpr assertion : assertions) {
                boolean hasMagicNumber = assertion.getArguments().stream()
                        .anyMatch(this::containsMagicNumber);

                if (hasMagicNumber) {
                    smells.add(new TestSmell(
                            SmellType.MAGIC_NUMBER_TEST,
                            className,
                            method.getNameAsString(),
                            assertion.getBegin().map(p -> p.line).orElse(0),
                            "Assertion contains magic number literal"
                    ));
                    break;
                }
            }
        }
        return smells;
    }

    private boolean containsMagicNumber(Expression expr) {
        if (expr instanceof IntegerLiteralExpr || expr instanceof LongLiteralExpr
                || expr instanceof DoubleLiteralExpr) {
            String value = expr.toString();
            return !EXCLUDED_VALUES.contains(value);
        }
        if (expr instanceof UnaryExpr) {
            UnaryExpr unary = (UnaryExpr) expr;
            if (unary.getOperator() == UnaryExpr.Operator.MINUS) {
                String negated = "-" + unary.getExpression().toString();
                return !EXCLUDED_VALUES.contains(negated);
            }
        }
        return false;
    }
}
