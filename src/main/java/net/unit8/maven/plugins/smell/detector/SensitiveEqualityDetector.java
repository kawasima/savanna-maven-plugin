package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import net.unit8.maven.plugins.smell.*;

import java.util.*;

public class SensitiveEqualityDetector implements SmellDetector {
    private static final Set<String> EQUALITY_ASSERTIONS = Set.of(
            "assertEquals", "assertNotEquals", "assertSame", "assertNotSame",
            "assertArrayEquals", "assertIterableEquals", "assertLinesMatch"
    );

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
                if (!isEqualityComparison(call)) {
                    continue;
                }
                boolean uses = call.getArguments().stream().anyMatch(this::containsStringification);
                if (!uses) {
                    // AssertJ chain: the value-under-test lives inside assertThat(...)
                    // upstream of the .isEqualTo() terminal, not in its argument list.
                    uses = upstreamAssertThatArgUsesStringification(call);
                }

                if (uses) {
                    smells.add(new TestSmell(
                            SmellType.SENSITIVE_EQUALITY,
                            className,
                            method.getNameAsString(),
                            call.getBegin().map(p -> p.line).orElse(0),
                            "Assertion compares via toString()/String.valueOf — fragile to formatting changes"
                    ));
                }
            }
        }
        return smells;
    }

    private boolean isEqualityComparison(MethodCallExpr call) {
        String name = call.getNameAsString();
        if (EQUALITY_ASSERTIONS.contains(name)) {
            return true;
        }
        // AssertJ: assertThat(x.toString()).isEqualTo(...) — terminal in chain
        if (name.equals("isEqualTo") || name.equals("isNotEqualTo")) {
            return DetectorHelpers.chainRootedIn(call, DetectorHelpers.ASSERTION_ENTRY_METHODS);
        }
        return false;
    }

    /**
     * True when {@code expr} converts something to a String for the purpose of
     * the surrounding equality check. Covers explicit {@code .toString()},
     * {@code String.valueOf(x)}, {@code Objects.toString(x)}, and
     * {@code "" + x} concatenations.
     */
    private boolean containsStringification(Expression expr) {
        // AssertJ chain root: assertThat(obj.toString()).isEqualTo(...) — the
        // chain's `assertThat` call itself contains the toString() in its argument.
        // findAll on the chain element catches that.
        for (MethodCallExpr call : expr.findAll(MethodCallExpr.class)) {
            String name = call.getNameAsString();
            if (name.equals("toString") && call.getArguments().isEmpty()
                    && call.getScope().isPresent()) {
                return true;
            }
            if (name.equals("valueOf") && isScopeNamed(call, "String")) {
                return true;
            }
            if (name.equals("toString") && isScopeNamed(call, "Objects")
                    && !call.getArguments().isEmpty()) {
                return true;
            }
        }
        for (BinaryExpr bin : expr.findAll(BinaryExpr.class)) {
            if (bin.getOperator() == BinaryExpr.Operator.PLUS
                    && (bin.getLeft() instanceof StringLiteralExpr
                            || bin.getRight() instanceof StringLiteralExpr)) {
                if (isEmptyStringConcat(bin)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isScopeNamed(MethodCallExpr call, String expectedName) {
        return call.getScope()
                .filter(s -> s instanceof NameExpr)
                .map(s -> ((NameExpr) s).getNameAsString())
                .filter(expectedName::equals)
                .isPresent();
    }

    private boolean upstreamAssertThatArgUsesStringification(MethodCallExpr call) {
        Expression scope = call.getScope().orElse(null);
        while (scope instanceof MethodCallExpr) {
            MethodCallExpr inner = (MethodCallExpr) scope;
            if (DetectorHelpers.ASSERTION_ENTRY_METHODS.contains(inner.getNameAsString())) {
                return inner.getArguments().stream().anyMatch(this::containsStringification);
            }
            scope = inner.getScope().orElse(null);
        }
        return false;
    }

    private boolean isEmptyStringConcat(BinaryExpr bin) {
        return (bin.getLeft() instanceof StringLiteralExpr
                        && ((StringLiteralExpr) bin.getLeft()).asString().isEmpty())
                || (bin.getRight() instanceof StringLiteralExpr
                        && ((StringLiteralExpr) bin.getRight()).asString().isEmpty());
    }
}
