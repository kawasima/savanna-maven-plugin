package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.MethodDeclaration;
import net.unit8.maven.plugins.smell.*;

import java.util.ArrayList;
import java.util.List;

public class IgnoredTestDetector implements SmellDetector {
    @Override
    public SmellType type() {
        return SmellType.IGNORED_TEST;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        for (MethodDeclaration method : context.getTestMethods()) {
            if (method.getAnnotationByName("Disabled").isPresent()) {
                smells.add(new TestSmell(
                        SmellType.IGNORED_TEST,
                        className,
                        method.getNameAsString(),
                        method.getBegin().map(p -> p.line).orElse(0),
                        "Test method is @Disabled"
                ));
            }
        }
        return smells;
    }
}
