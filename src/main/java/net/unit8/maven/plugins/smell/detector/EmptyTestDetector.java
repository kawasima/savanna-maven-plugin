package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.Statement;
import net.unit8.maven.plugins.smell.*;

import java.util.ArrayList;
import java.util.List;

public class EmptyTestDetector implements SmellDetector {
    @Override
    public SmellType type() {
        return SmellType.EMPTY_TEST;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        // Ignored / disabled tests are already reported by IgnoredTestDetector;
        // don't double-report them as EMPTY_TEST.
        boolean classIgnored = context.getTestClass().getAnnotationByName("Disabled").isPresent()
                || context.getTestClass().getAnnotationByName("Ignore").isPresent();
        if (classIgnored) {
            return smells;
        }

        for (MethodDeclaration method : context.getTestMethods()) {
            if (method.getAnnotationByName("Disabled").isPresent()
                    || method.getAnnotationByName("Ignore").isPresent()) {
                continue;
            }
            if (isEffectivelyEmpty(method.getBody().orElse(null))) {
                smells.add(new TestSmell(
                        SmellType.EMPTY_TEST,
                        className,
                        method.getNameAsString(),
                        method.getBegin().map(p -> p.line).orElse(0),
                        "Test method has an empty body"
                ));
            }
        }
        return smells;
    }

    private boolean isEffectivelyEmpty(BlockStmt body) {
        if (body == null || body.isEmpty()) {
            return true;
        }
        for (Statement stmt : body.getStatements()) {
            if (!(stmt instanceof EmptyStmt)) {
                return false;
            }
        }
        return true;
    }
}
