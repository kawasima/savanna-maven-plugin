package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import net.unit8.maven.plugins.smell.*;

import java.util.*;

/**
 * Detects General Fixture smell: fields that are initialized in {@code @BeforeEach}
 * but used by fewer than half of the test methods.
 *
 * Only considers fields that are actually assigned in setup methods, not all
 * declared fields. A field used by most tests is not a general fixture problem.
 */
public class GeneralFixtureDetector implements SmellDetector {
    @Override
    public SmellType type() {
        return SmellType.GENERAL_FIXTURE;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        if (context.getSetupMethods().isEmpty()) {
            return smells;
        }

        List<MethodDeclaration> testMethods = context.getTestMethods();
        if (testMethods.size() < 2) {
            return smells;
        }

        // Collect field names that are actually assigned in @BeforeEach methods.
        // Recognize both `field = ...` (NameExpr) and `this.field = ...` (FieldAccessExpr).
        Set<String> setupFieldNames = new HashSet<>();
        for (MethodDeclaration setup : context.getSetupMethods()) {
            for (AssignExpr assign : setup.findAll(AssignExpr.class)) {
                String targetName = assignTargetFieldName(assign.getTarget());
                if (targetName != null) {
                    setupFieldNames.add(targetName);
                }
            }
        }

        // Also consider fields with no initializer (they must be set in @BeforeEach)
        Set<String> allFieldNames = new HashSet<>();
        for (FieldDeclaration field : context.getFields()) {
            for (VariableDeclarator var : field.getVariables()) {
                allFieldNames.add(var.getNameAsString());
            }
        }

        // Only check fields that are both declared and initialized in setup
        setupFieldNames.retainAll(allFieldNames);

        if (setupFieldNames.isEmpty()) {
            return smells;
        }

        for (String fieldName : setupFieldNames) {
            long usageCount = testMethods.stream()
                    .filter(method -> referencesField(method, fieldName))
                    .count();

            // Only flag if used by fewer than half of the test methods
            if (usageCount > 0 && usageCount * 2 < testMethods.size()) {
                smells.add(new TestSmell(
                        SmellType.GENERAL_FIXTURE,
                        className,
                        null,
                        context.getTestClass().getBegin().map(p -> p.line).orElse(0),
                        "Field '" + fieldName + "' initialized in @BeforeEach but used in only "
                                + usageCount + "/" + testMethods.size() + " test methods"
                ));
            }
        }
        return smells;
    }

    private String assignTargetFieldName(Expression target) {
        if (target instanceof NameExpr) {
            return ((NameExpr) target).getNameAsString();
        }
        if (target instanceof FieldAccessExpr) {
            FieldAccessExpr fae = (FieldAccessExpr) target;
            if (fae.getScope() instanceof ThisExpr) {
                return fae.getNameAsString();
            }
        }
        return null;
    }

    private boolean referencesField(MethodDeclaration method, String fieldName) {
        boolean viaName = method.findAll(NameExpr.class).stream()
                .anyMatch(ne -> ne.getNameAsString().equals(fieldName));
        if (viaName) {
            return true;
        }
        return method.findAll(FieldAccessExpr.class).stream()
                .anyMatch(fae -> fae.getScope() instanceof ThisExpr
                        && fae.getNameAsString().equals(fieldName));
    }
}
