package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.NameExpr;
import net.unit8.maven.plugins.smell.*;

import java.util.*;

public class TestMaverickDetector implements SmellDetector {
    @Override
    public SmellType type() {
        return SmellType.TEST_MAVERICK;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        if (context.getSetupMethods().isEmpty()) {
            return smells;
        }

        Set<String> fixtureFields = new HashSet<>();
        for (FieldDeclaration field : context.getFields()) {
            for (VariableDeclarator var : field.getVariables()) {
                fixtureFields.add(var.getNameAsString());
            }
        }

        if (fixtureFields.isEmpty()) {
            return smells;
        }

        for (MethodDeclaration method : context.getTestMethods()) {
            boolean usesAnyFixture = method.findAll(NameExpr.class).stream()
                    .anyMatch(ne -> fixtureFields.contains(ne.getNameAsString()));

            if (!usesAnyFixture) {
                smells.add(new TestSmell(
                        SmellType.TEST_MAVERICK,
                        className,
                        method.getNameAsString(),
                        method.getBegin().map(p -> p.line).orElse(0),
                        "Test method does not use any shared fixture fields"
                ));
            }
        }
        return smells;
    }
}
