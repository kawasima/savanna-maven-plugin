package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import net.unit8.maven.plugins.smell.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Detects when test methods in a class target unrelated objects,
 * suggesting the test class lacks cohesion.
 */
public class LackOfCohesionDetector implements SmellDetector {
    private static final Set<String> EXCLUDED_SCOPES = Set.of(
            "System", "Arrays", "Collections", "Objects", "Math",
            "Assertions", "Assert", "Mockito",
            "String", "Integer", "Long", "Double", "Boolean"
    );

    @Override
    public SmellType type() {
        return SmellType.LACK_OF_COHESION;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        if (context.getTestMethods().size() < 2) {
            return smells;
        }

        // For each test method, collect the set of scopes it interacts with
        List<Set<String>> methodScopes = new ArrayList<>();
        for (MethodDeclaration method : context.getTestMethods()) {
            Set<String> scopes = method.findAll(MethodCallExpr.class).stream()
                    .filter(call -> call.getScope().isPresent())
                    .filter(call -> call.getScope().get() instanceof NameExpr)
                    .map(call -> ((NameExpr) call.getScope().get()).getNameAsString())
                    .filter(scope -> !EXCLUDED_SCOPES.contains(scope))
                    .filter(scope -> !scope.equals("this"))
                    .collect(Collectors.toSet());
            if (!scopes.isEmpty()) {
                methodScopes.add(scopes);
            }
        }

        if (methodScopes.size() < 2) {
            return smells;
        }

        // Calculate cohesion: ratio of method pairs sharing at least one scope
        int pairs = 0;
        int sharedPairs = 0;
        for (int i = 0; i < methodScopes.size(); i++) {
            for (int j = i + 1; j < methodScopes.size(); j++) {
                pairs++;
                Set<String> intersection = new HashSet<>(methodScopes.get(i));
                intersection.retainAll(methodScopes.get(j));
                if (!intersection.isEmpty()) {
                    sharedPairs++;
                }
            }
        }

        if (pairs > 0) {
            double cohesion = (double) sharedPairs / pairs;
            if (cohesion < 0.3) {
                smells.add(new TestSmell(
                        SmellType.LACK_OF_COHESION,
                        className,
                        null,
                        context.getTestClass().getBegin().map(p -> p.line).orElse(0),
                        String.format("Low cohesion (%.0f%%): test methods target unrelated objects", cohesion * 100),
                        true
                ));
            }
        }
        return smells;
    }
}
