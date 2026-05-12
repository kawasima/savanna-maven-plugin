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
            case "isTrue":
            case "isFalse":
            case "isNull":
                return isAssertJTerminalOnLiteral(call, name);
            case "isEqualTo":
            case "isSameAs":
                return isAssertJTerminalOnEqualSubject(call);
            default:
                return false;
        }
    }

    /**
     * AssertJ terminal predicate on a literal subject — e.g. {@code assertThat(true).isTrue()}.
     */
    private boolean isAssertJTerminalOnLiteral(MethodCallExpr terminal, String terminalName) {
        Expression subject = assertThatSubject(terminal);
        if (subject == null) {
            return false;
        }
        switch (terminalName) {
            case "isTrue":
                return isLiteral(subject, true);
            case "isFalse":
                return isLiteral(subject, false);
            case "isNull":
                return subject instanceof NullLiteralExpr;
            default:
                return false;
        }
    }

    /**
     * {@code assertThat(x).isEqualTo(x)} — terminal arg equals the assertThat subject by source text.
     */
    private boolean isAssertJTerminalOnEqualSubject(MethodCallExpr terminal) {
        if (terminal.getArguments().size() != 1) {
            return false;
        }
        Expression subject = assertThatSubject(terminal);
        if (subject == null) {
            return false;
        }
        return subject.toString().equals(terminal.getArgument(0).toString());
    }

    /** Returns the single argument passed to {@code assertThat(...)} that roots {@code terminal}'s chain. */
    private Expression assertThatSubject(MethodCallExpr terminal) {
        Expression scope = terminal.getScope().orElse(null);
        while (scope instanceof MethodCallExpr) {
            MethodCallExpr inner = (MethodCallExpr) scope;
            if ("assertThat".equals(inner.getNameAsString()) && inner.getArguments().size() == 1) {
                return inner.getArgument(0);
            }
            scope = inner.getScope().orElse(null);
        }
        return null;
    }

    private boolean isLiteral(Expression expr, boolean value) {
        return expr instanceof BooleanLiteralExpr
                && ((BooleanLiteralExpr) expr).getValue() == value;
    }
}
