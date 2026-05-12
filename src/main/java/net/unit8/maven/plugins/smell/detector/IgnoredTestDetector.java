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

        boolean classDisabled = context.getTestClass().getAnnotationByName("Disabled").isPresent();

        for (MethodDeclaration method : context.getTestMethods()) {
            boolean methodDisabled = method.getAnnotationByName("Disabled").isPresent()
                    || method.getAnnotationByName("Ignore").isPresent();

            if (classDisabled || methodDisabled) {
                String reason = classDisabled && !methodDisabled
                        ? "Enclosing class is @Disabled"
                        : "Test method is @Disabled";
                smells.add(new TestSmell(
                        SmellType.IGNORED_TEST,
                        className,
                        method.getNameAsString(),
                        method.getBegin().map(p -> p.line).orElse(0),
                        reason
                ));
            }
        }
        return smells;
    }
}
