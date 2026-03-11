package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
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

        for (MethodDeclaration method : context.getTestMethods()) {
            if (method.getBody().map(BlockStmt::isEmpty).orElse(true)) {
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
}
