package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import net.unit8.maven.plugins.smell.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Detects when a single test method calls methods on multiple different
 * production-level objects, suggesting it tests too many things at once.
 *
 * Local variables that are assigned from method return values (e.g.,
 * {@code User user = service.create()}) are excluded from scope counting,
 * since they represent the result of the object under test, not a separate
 * collaborator.
 */
public class EagerTestDetector implements SmellDetector {
    private static final Set<String> EXCLUDED_SCOPES = Set.of(
            "System", "Arrays", "Collections", "Objects", "Math",
            "String", "Integer", "Long", "Double", "Boolean",
            "Assertions", "Assert", "Mockito", "BDDMockito",
            "Optional", "Stream", "Thread"
    );

    private static final int THRESHOLD = 3;

    @Override
    public SmellType type() {
        return SmellType.EAGER_TEST;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        for (MethodDeclaration method : context.getTestMethods()) {
            // Collect local variable names assigned from method return values.
            // These are results, not independent collaborators.
            Set<String> localResultVars = new HashSet<>();
            method.findAll(VariableDeclarator.class).stream()
                    .filter(v -> v.getInitializer().isPresent())
                    .filter(v -> v.getInitializer().get() instanceof MethodCallExpr)
                    .forEach(v -> localResultVars.add(v.getNameAsString()));

            Set<String> targetScopes = method.findAll(MethodCallExpr.class).stream()
                    .filter(call -> call.getScope().isPresent())
                    .filter(call -> call.getScope().get() instanceof NameExpr)
                    .map(call -> ((NameExpr) call.getScope().get()).getNameAsString())
                    .filter(scope -> !EXCLUDED_SCOPES.contains(scope))
                    .filter(scope -> !scope.equals("this"))
                    .filter(scope -> !localResultVars.contains(scope))
                    .collect(Collectors.toSet());

            if (targetScopes.size() >= THRESHOLD) {
                smells.add(new TestSmell(
                        SmellType.EAGER_TEST,
                        className,
                        method.getNameAsString(),
                        method.getBegin().map(p -> p.line).orElse(0),
                        "Test method interacts with " + targetScopes.size()
                                + " different collaborators: " + targetScopes,
                        true
                ));
            }
        }
        return smells;
    }
}
