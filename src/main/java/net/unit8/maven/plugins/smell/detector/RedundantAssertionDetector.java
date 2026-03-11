package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import net.unit8.maven.plugins.smell.*;

import java.util.ArrayList;
import java.util.List;

public class RedundantAssertionDetector implements SmellDetector {
    @Override
    public SmellType type() {
        return SmellType.REDUNDANT_ASSERTION;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        for (MethodDeclaration method : context.getTestMethods()) {
            for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
                if (isRedundant(call)) {
                    smells.add(new TestSmell(
                            SmellType.REDUNDANT_ASSERTION,
                            className,
                            method.getNameAsString(),
                            call.getBegin().map(p -> p.line).orElse(0),
                            "Assertion is trivially true: " + call
                    ));
                }
            }
        }
        return smells;
    }

    private boolean isRedundant(MethodCallExpr call) {
        String name = call.getNameAsString();
        List<Expression> args = call.getArguments();

        switch (name) {
            case "assertTrue":
                return args.size() >= 1 && isLiteral(args.get(0), true);
            case "assertFalse":
                return args.size() >= 1 && isLiteral(args.get(0), false);
            case "assertNull":
                return args.size() >= 1 && args.get(0) instanceof NullLiteralExpr;
            case "assertEquals":
            case "assertSame":
                return args.size() >= 2 && args.get(0).toString().equals(args.get(1).toString());
            default:
                return false;
        }
    }

    private boolean isLiteral(Expression expr, boolean value) {
        return expr instanceof BooleanLiteralExpr
                && ((BooleanLiteralExpr) expr).getValue() == value;
    }
}
