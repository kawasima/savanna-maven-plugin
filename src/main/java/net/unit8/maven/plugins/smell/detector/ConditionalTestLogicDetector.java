package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.*;
import net.unit8.maven.plugins.smell.*;

import java.util.ArrayList;
import java.util.List;

public class ConditionalTestLogicDetector implements SmellDetector {
    @Override
    public SmellType type() {
        return SmellType.CONDITIONAL_TEST_LOGIC;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        for (MethodDeclaration method : context.getTestMethods()) {
            boolean hasConditional =
                    !method.findAll(IfStmt.class).isEmpty()
                    || !method.findAll(SwitchStmt.class).isEmpty()
                    || !method.findAll(ForStmt.class).isEmpty()
                    || !method.findAll(ForEachStmt.class).isEmpty()
                    || !method.findAll(WhileStmt.class).isEmpty();

            if (hasConditional) {
                smells.add(new TestSmell(
                        SmellType.CONDITIONAL_TEST_LOGIC,
                        className,
                        method.getNameAsString(),
                        method.getBegin().map(p -> p.line).orElse(0),
                        "Test method contains conditional logic (if/switch/for/while)"
                ));
            }
        }
        return smells;
    }
}
