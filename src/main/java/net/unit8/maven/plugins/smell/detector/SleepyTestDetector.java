package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.MethodDeclaration;
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
                    .anyMatch(call -> call.getScope()
                            .filter(s -> s instanceof NameExpr)
                            .map(s -> ((NameExpr) s).getNameAsString())
                            .filter(name -> name.equals("Thread") || TIMEUNIT_NAMES.contains(name))
                            .isPresent());

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
}
