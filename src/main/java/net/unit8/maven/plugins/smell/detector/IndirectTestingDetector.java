package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import net.unit8.maven.plugins.smell.*;
import net.unit8.maven.plugins.smell.resolver.ConventionBasedResolver;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Detects when test methods primarily invoke methods on classes other than
 * the expected production class (derived from the test class name).
 *
 * Uses both variable name and field type name to determine whether a scope
 * corresponds to the expected production class. Only flags when the dominant
 * scope is clearly unrelated and accounts for the majority of calls.
 */
public class IndirectTestingDetector implements SmellDetector {
    private static final Set<String> EXCLUDED_SCOPES = new TreeSet<>(Arrays.asList(
            "System", "Arrays", "Collections", "Objects", "Math",
            "Assertions", "Assert", "Mockito", "BDDMockito",
            "String", "Integer", "Long", "Double", "Boolean",
            "Optional", "Stream", "List", "Map", "Set"
    ));

    @Override
    public SmellType type() {
        return SmellType.INDIRECT_TESTING;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String testClassName = context.getTestClass().getNameAsString();
        String productionClassName = ConventionBasedResolver.deriveProductionClassName(testClassName);

        if (productionClassName == null) {
            return smells;
        }

        // Build a set of variable names that could refer to the production class.
        // Includes: camelCase of production class name, exact class name,
        // and any field whose declared type matches the production class name.
        Set<String> expectedScopes = new HashSet<>();
        expectedScopes.add(Character.toLowerCase(productionClassName.charAt(0))
                + productionClassName.substring(1));
        expectedScopes.add(productionClassName);

        for (FieldDeclaration field : context.getFields()) {
            for (VariableDeclarator var : field.getVariables()) {
                String typeName = var.getTypeAsString();
                int genericsIdx = typeName.indexOf('<');
                if (genericsIdx > 0) {
                    typeName = typeName.substring(0, genericsIdx);
                }
                if (typeName.equals(productionClassName)) {
                    expectedScopes.add(var.getNameAsString());
                }
            }
        }

        for (MethodDeclaration method : context.getTestMethods()) {
            Map<String, Long> scopeCounts = method.findAll(MethodCallExpr.class).stream()
                    .filter(call -> call.getScope().isPresent())
                    .filter(call -> call.getScope().get() instanceof NameExpr)
                    .map(call -> ((NameExpr) call.getScope().get()).getNameAsString())
                    .filter(scope -> !EXCLUDED_SCOPES.contains(scope))
                    .filter(scope -> !scope.equals("this"))
                    .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

            if (scopeCounts.size() < 2) {
                continue;
            }

            long totalCalls = scopeCounts.values().stream().mapToLong(Long::longValue).sum();
            long expectedCalls = scopeCounts.entrySet().stream()
                    .filter(e -> expectedScopes.contains(e.getKey()))
                    .mapToLong(Map.Entry::getValue)
                    .sum();

            // Only flag if expected class receives less than 30% of the calls
            if (expectedCalls * 100 / totalCalls < 30) {
                String dominantScope = scopeCounts.entrySet().stream()
                        .filter(e -> !expectedScopes.contains(e.getKey()))
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse(null);

                if (dominantScope != null) {
                    smells.add(new TestSmell(
                            SmellType.INDIRECT_TESTING,
                            testClassName,
                            method.getNameAsString(),
                            method.getBegin().map(p -> p.line).orElse(0),
                            "Test primarily invokes '" + dominantScope + "' instead of expected '"
                                    + productionClassName + "'",
                            true
                    ));
                }
            }
        }
        return smells;
    }
}
