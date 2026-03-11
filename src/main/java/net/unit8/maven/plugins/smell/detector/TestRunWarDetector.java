package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import net.unit8.maven.plugins.smell.*;

import java.util.ArrayList;
import java.util.List;

public class TestRunWarDetector implements SmellDetector {
    @Override
    public SmellType type() {
        return SmellType.TEST_RUN_WAR;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        for (FieldDeclaration field : context.getFields()) {
            if (!field.isStatic()) {
                continue;
            }
            // static final is typically a constant, not mutable state
            if (field.isFinal()) {
                continue;
            }
            for (VariableDeclarator var : field.getVariables()) {
                smells.add(new TestSmell(
                        SmellType.TEST_RUN_WAR,
                        className,
                        null,
                        field.getBegin().map(p -> p.line).orElse(0),
                        "Static mutable field '" + var.getNameAsString() + "' may cause test interference"
                ));
            }
        }
        return smells;
    }
}
