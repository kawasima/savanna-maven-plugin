package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import net.unit8.maven.plugins.smell.*;

import java.util.*;

public class OrderDependentTestDetector implements SmellDetector {
    @Override
    public SmellType type() {
        return SmellType.ORDER_DEPENDENT_TEST;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        if (context.getTestClass().getAnnotationByName("TestMethodOrder").isPresent()) {
            smells.add(new TestSmell(
                    SmellType.ORDER_DEPENDENT_TEST,
                    className,
                    null,
                    context.getTestClass().getBegin().map(p -> p.line).orElse(0),
                    "Test class uses @TestMethodOrder, indicating order dependency"
            ));
        }

        Set<String> staticFieldNames = new HashSet<>();
        Set<String> instanceFieldNames = new HashSet<>();
        for (FieldDeclaration field : context.getFields()) {
            for (com.github.javaparser.ast.body.VariableDeclarator v : field.getVariables()) {
                if (field.isStatic()) {
                    staticFieldNames.add(v.getNameAsString());
                } else {
                    instanceFieldNames.add(v.getNameAsString());
                }
            }
        }

        Set<String> setupResetFields = collectSetupResetFields(context);
        boolean perClass = isPerClassLifecycle(context);

        // Instance fields are only shared between tests when the lifecycle is PER_CLASS.
        // With the default PER_METHOD lifecycle, each test gets a fresh instance.
        Set<String> sharedInstanceFieldNames = perClass
                ? subtract(instanceFieldNames, setupResetFields)
                : Collections.emptySet();
        Set<String> sharedStaticFieldNames = subtract(staticFieldNames, setupResetFields);

        for (MethodDeclaration method : context.getTestMethods()) {
            String hitField = findSharedWrite(method, sharedStaticFieldNames, sharedInstanceFieldNames);
            if (hitField != null) {
                smells.add(new TestSmell(
                        SmellType.ORDER_DEPENDENT_TEST,
                        className,
                        method.getNameAsString(),
                        method.getBegin().map(p -> p.line).orElse(0),
                        "Test method writes to shared field '" + hitField + "', may cause order dependency",
                        true
                ));
            }
        }
        return smells;
    }

    private Set<String> collectSetupResetFields(DetectionContext context) {
        Set<String> reset = new HashSet<>();
        for (MethodDeclaration setup : context.getSetupMethods()) {
            if (setup.getAnnotationByName("BeforeEach").isPresent()) {
                for (AssignExpr assign : setup.findAll(AssignExpr.class)) {
                    String name = targetFieldName(assign.getTarget());
                    if (name != null) {
                        reset.add(name);
                    }
                }
            }
        }
        return reset;
    }

    private boolean isPerClassLifecycle(DetectionContext context) {
        return context.getTestClass().getAnnotationByName("TestInstance")
                .map(ann -> ann.toString().contains("PER_CLASS"))
                .orElse(false);
    }

    private String findSharedWrite(MethodDeclaration method,
                                   Set<String> sharedStatic,
                                   Set<String> sharedInstance) {
        for (AssignExpr assign : method.findAll(AssignExpr.class)) {
            String name = targetFieldName(assign.getTarget());
            if (name == null) {
                continue;
            }
            if (sharedStatic.contains(name) || sharedInstance.contains(name)) {
                return name;
            }
        }
        return null;
    }

    private String targetFieldName(Expression target) {
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

    private Set<String> subtract(Set<String> a, Set<String> b) {
        Set<String> out = new HashSet<>(a);
        out.removeAll(b);
        return out;
    }
}
