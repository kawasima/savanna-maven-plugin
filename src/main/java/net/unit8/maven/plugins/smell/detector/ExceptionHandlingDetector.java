package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.TryStmt;
import net.unit8.maven.plugins.smell.*;

import java.util.ArrayList;
import java.util.List;

public class ExceptionHandlingDetector implements SmellDetector {
    @Override
    public SmellType type() {
        return SmellType.EXCEPTION_HANDLING;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        for (MethodDeclaration method : context.getTestMethods()) {
            List<TryStmt> tryStmts = method.findAll(TryStmt.class);
            // try-with-resources without catch is acceptable
            boolean hasTryCatch = tryStmts.stream()
                    .anyMatch(t -> !t.getCatchClauses().isEmpty());

            if (hasTryCatch) {
                smells.add(new TestSmell(
                        SmellType.EXCEPTION_HANDLING,
                        className,
                        method.getNameAsString(),
                        method.getBegin().map(p -> p.line).orElse(0),
                        "Test method uses try/catch instead of assertThrows"
                ));
            }
        }
        return smells;
    }
}
