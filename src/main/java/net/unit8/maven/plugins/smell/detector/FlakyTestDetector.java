package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import net.unit8.maven.plugins.smell.*;

import java.util.*;

public class FlakyTestDetector implements SmellDetector {
    private static final Set<String> RANDOM_TYPES = new TreeSet<>(Arrays.asList(
            "Random", "ThreadLocalRandom", "SecureRandom"
    ));

    private static final Set<String> TIME_METHODS = new TreeSet<>(Arrays.asList(
            "now", "currentTimeMillis", "nanoTime"
    ));

    @Override
    public SmellType type() {
        return SmellType.FLAKY_TEST;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        for (MethodDeclaration method : context.getTestMethods()) {
            List<String> indicators = new ArrayList<>();

            // Random usage
            boolean hasRandom = method.findAll(ObjectCreationExpr.class).stream()
                    .anyMatch(expr -> RANDOM_TYPES.contains(expr.getTypeAsString()));
            if (!hasRandom) {
                hasRandom = method.findAll(MethodCallExpr.class).stream()
                        .anyMatch(call -> call.getNameAsString().equals("random")
                                && call.getScope()
                                .filter(s -> s instanceof NameExpr)
                                .map(s -> ((NameExpr) s).getNameAsString())
                                .filter(n -> n.equals("Math"))
                                .isPresent());
            }
            if (hasRandom) {
                indicators.add("random");
            }

            // Time-dependent
            boolean hasTime = method.findAll(MethodCallExpr.class).stream()
                    .anyMatch(call -> TIME_METHODS.contains(call.getNameAsString()));
            if (hasTime) {
                indicators.add("time-dependent");
            }

            // Thread.sleep
            boolean hasSleep = method.findAll(MethodCallExpr.class).stream()
                    .anyMatch(call -> call.getNameAsString().equals("sleep")
                            && call.getScope()
                            .filter(s -> s instanceof NameExpr)
                            .map(s -> ((NameExpr) s).getNameAsString())
                            .filter(n -> n.equals("Thread"))
                            .isPresent());
            if (hasSleep) {
                indicators.add("sleep");
            }

            if (indicators.size() >= 2) {
                smells.add(new TestSmell(
                        SmellType.FLAKY_TEST,
                        className,
                        method.getNameAsString(),
                        method.getBegin().map(p -> p.line).orElse(0),
                        "Multiple flakiness indicators: " + String.join(", ", indicators),
                        true
                ));
            }
        }
        return smells;
    }
}
