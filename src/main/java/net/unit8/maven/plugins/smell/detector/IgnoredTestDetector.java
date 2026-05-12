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
        boolean classIgnored = context.getTestClass().getAnnotationByName("Ignore").isPresent();

        for (MethodDeclaration method : context.getTestMethods()) {
            boolean methodDisabled = method.getAnnotationByName("Disabled").isPresent();
            boolean methodIgnored = method.getAnnotationByName("Ignore").isPresent();

            if (!(classDisabled || classIgnored || methodDisabled || methodIgnored)) {
                continue;
            }

            String reason;
            if (methodDisabled) {
                reason = "Test method is @Disabled";
            } else if (methodIgnored) {
                reason = "Test method is @Ignore";
            } else if (classDisabled) {
                reason = "Enclosing class is @Disabled";
            } else {
                reason = "Enclosing class is @Ignore";
            }

            smells.add(new TestSmell(
                    SmellType.IGNORED_TEST,
                    className,
                    method.getNameAsString(),
                    method.getBegin().map(p -> p.line).orElse(0),
                    reason
            ));
        }
        return smells;
    }
}
