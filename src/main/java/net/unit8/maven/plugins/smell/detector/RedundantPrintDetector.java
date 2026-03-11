package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import net.unit8.maven.plugins.smell.*;

import java.util.ArrayList;
import java.util.List;

public class RedundantPrintDetector implements SmellDetector {
    @Override
    public SmellType type() {
        return SmellType.REDUNDANT_PRINT;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        for (MethodDeclaration method : context.getTestMethods()) {
            boolean hasPrint = method.findAll(MethodCallExpr.class).stream()
                    .anyMatch(this::isPrintCall);

            if (hasPrint) {
                smells.add(new TestSmell(
                        SmellType.REDUNDANT_PRINT,
                        className,
                        method.getNameAsString(),
                        method.getBegin().map(p -> p.line).orElse(0),
                        "Test method contains System.out/err print statement"
                ));
            }
        }
        return smells;
    }

    private boolean isPrintCall(MethodCallExpr call) {
        String methodName = call.getNameAsString();
        if (!methodName.startsWith("print") && !methodName.equals("write")) {
            return false;
        }
        return call.getScope()
                .filter(scope -> scope instanceof FieldAccessExpr)
                .map(scope -> (FieldAccessExpr) scope)
                .filter(fa -> fa.getNameAsString().equals("out") || fa.getNameAsString().equals("err"))
                .flatMap(fa -> fa.getScope() instanceof com.github.javaparser.ast.expr.NameExpr
                        ? java.util.Optional.of(((com.github.javaparser.ast.expr.NameExpr) fa.getScope()).getNameAsString())
                        : java.util.Optional.empty())
                .filter(name -> name.equals("System"))
                .isPresent();
    }
}
