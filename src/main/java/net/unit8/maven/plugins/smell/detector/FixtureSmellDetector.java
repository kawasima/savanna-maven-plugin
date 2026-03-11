package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.NameExpr;
import net.unit8.maven.plugins.smell.*;

import java.util.*;

public class FixtureSmellDetector implements SmellDetector {
    @Override
    public SmellType type() {
        return SmellType.DEAD_FIELD;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        // Collect all NameExpr references in test, setup, and teardown methods
        Set<String> referencedNames = new HashSet<>();
        for (MethodDeclaration method : context.getTestMethods()) {
            method.findAll(NameExpr.class).forEach(ne -> referencedNames.add(ne.getNameAsString()));
        }
        for (MethodDeclaration method : context.getSetupMethods()) {
            method.findAll(NameExpr.class).forEach(ne -> referencedNames.add(ne.getNameAsString()));
        }
        for (MethodDeclaration method : context.getTeardownMethods()) {
            method.findAll(NameExpr.class).forEach(ne -> referencedNames.add(ne.getNameAsString()));
        }

        for (FieldDeclaration field : context.getFields()) {
            if (field.isStatic() && field.isFinal()) {
                continue; // Skip constants
            }
            for (VariableDeclarator var : field.getVariables()) {
                if (!referencedNames.contains(var.getNameAsString())) {
                    smells.add(new TestSmell(
                            SmellType.DEAD_FIELD,
                            className,
                            null,
                            field.getBegin().map(p -> p.line).orElse(0),
                            "Field '" + var.getNameAsString() + "' is never referenced in tests or setup"
                    ));
                }
            }
        }
        return smells;
    }
}
