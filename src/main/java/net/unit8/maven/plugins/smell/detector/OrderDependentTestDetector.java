package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.NameExpr;
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

        // Check for @TestMethodOrder or @Order annotations
        if (context.getTestClass().getAnnotationByName("TestMethodOrder").isPresent()) {
            smells.add(new TestSmell(
                    SmellType.ORDER_DEPENDENT_TEST,
                    className,
                    null,
                    context.getTestClass().getBegin().map(p -> p.line).orElse(0),
                    "Test class uses @TestMethodOrder, indicating order dependency"
            ));
        }

        // Collect instance field names
        List<String> fieldNames = new ArrayList<>();
        for (FieldDeclaration field : context.getFields()) {
            if (!field.isStatic()) {
                field.getVariables().forEach(v -> fieldNames.add(v.getNameAsString()));
            }
        }

        if (fieldNames.isEmpty()) {
            return smells;
        }

        // Exclude fields that are reset in @BeforeEach (they are safe to write to)
        Set<String> setupResetFields = new HashSet<>();
        for (MethodDeclaration setup : context.getSetupMethods()) {
            if (setup.getAnnotationByName("BeforeEach").isPresent()) {
                setup.findAll(AssignExpr.class).stream()
                        .filter(a -> a.getTarget() instanceof NameExpr)
                        .map(a -> ((NameExpr) a.getTarget()).getNameAsString())
                        .forEach(setupResetFields::add);
            }
        }

        List<String> unsafeFieldNames = new ArrayList<>();
        for (String name : fieldNames) {
            if (!setupResetFields.contains(name)) {
                unsafeFieldNames.add(name);
            }
        }

        if (!unsafeFieldNames.isEmpty()) {
            for (MethodDeclaration method : context.getTestMethods()) {
                boolean writesSharedState = method.findAll(AssignExpr.class).stream()
                        .anyMatch(assign -> unsafeFieldNames.contains(assign.getTarget().toString()));

                if (writesSharedState) {
                    smells.add(new TestSmell(
                            SmellType.ORDER_DEPENDENT_TEST,
                            className,
                            method.getNameAsString(),
                            method.getBegin().map(p -> p.line).orElse(0),
                            "Test method writes to shared field, may cause order dependency",
                            true
                    ));
                }
            }
        }
        return smells;
    }
}
