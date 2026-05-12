package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import net.unit8.maven.plugins.smell.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Detects test methods that use sleep calls, which make tests slow and fragile.
 * Recognizes both {@code Thread.sleep()} and {@code TimeUnit.*.sleep()}.
 */
public class SleepyTestDetector implements SmellDetector {
    private static final Set<String> TIMEUNIT_NAMES = Set.of(
            "NANOSECONDS", "MICROSECONDS", "MILLISECONDS", "SECONDS", "MINUTES", "HOURS", "DAYS"
    );

    @Override
    public SmellType type() {
        return SmellType.SLEEPY_TEST;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        for (MethodDeclaration method : context.getTestMethods()) {
            boolean hasSleep = method.findAll(MethodCallExpr.class).stream()
                    .filter(call -> call.getNameAsString().equals("sleep"))
                    .anyMatch(call -> call.getScope().map(this::isSleepScope).orElse(false));

            if (hasSleep) {
                smells.add(new TestSmell(
                        SmellType.SLEEPY_TEST,
                        className,
                        method.getNameAsString(),
                        method.getBegin().map(p -> p.line).orElse(0),
                        "Test method contains Thread.sleep() or TimeUnit.sleep()"
                ));
            }
        }
        return smells;
    }

    private boolean isSleepScope(Expression scope) {
        if (scope instanceof NameExpr) {
            String name = ((NameExpr) scope).getNameAsString();
            return name.equals("Thread") || TIMEUNIT_NAMES.contains(name);
        }
        if (scope instanceof FieldAccessExpr) {
            FieldAccessExpr fae = (FieldAccessExpr) scope;
            return TIMEUNIT_NAMES.contains(fae.getNameAsString())
                    && isTimeUnitQualifier(fae.getScope());
        }
        return false;
    }

    /**
     * True when {@code scope} resolves to {@code TimeUnit} —
     * either the bare name (after static-import-ish usage) or a qualified
     * reference rooted at {@code java.util.concurrent.TimeUnit}.
     */
    private boolean isTimeUnitQualifier(Expression scope) {
        if (scope instanceof NameExpr) {
            return ((NameExpr) scope).getNameAsString().equals("TimeUnit");
        }
        if (scope instanceof FieldAccessExpr) {
            return ((FieldAccessExpr) scope).getNameAsString().equals("TimeUnit");
        }
        return false;
    }
}
