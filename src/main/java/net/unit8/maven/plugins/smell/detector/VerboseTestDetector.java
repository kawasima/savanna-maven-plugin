package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.MethodDeclaration;
import net.unit8.maven.plugins.smell.*;

import java.util.ArrayList;
import java.util.List;

public class VerboseTestDetector implements SmellDetector {
    private static final int DEFAULT_THRESHOLD = 30;

    private final int threshold;

    public VerboseTestDetector() {
        this(DEFAULT_THRESHOLD);
    }

    public VerboseTestDetector(int threshold) {
        this.threshold = threshold;
    }

    @Override
    public SmellType type() {
        return SmellType.VERBOSE_TEST;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        for (MethodDeclaration method : context.getTestMethods()) {
            int lines = method.getEnd().map(e -> e.line).orElse(0)
                    - method.getBegin().map(b -> b.line).orElse(0) + 1;

            if (lines > threshold) {
                smells.add(new TestSmell(
                        SmellType.VERBOSE_TEST,
                        className,
                        method.getNameAsString(),
                        method.getBegin().map(p -> p.line).orElse(0),
                        "Test method is " + lines + " lines (threshold: " + threshold + ")"
                ));
            }
        }
        return smells;
    }
}
