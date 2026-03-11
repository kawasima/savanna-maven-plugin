package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import net.unit8.maven.plugins.smell.*;

import java.util.*;

public class HiddenDependencyDetector implements SmellDetector {
    private static final Set<String> SINGLETON_PATTERNS = new TreeSet<>(Arrays.asList(
            "getInstance", "getDefault", "getSingleton",
            "newInstance", "current", "getContext"
    ));

    @Override
    public SmellType type() {
        return SmellType.HIDDEN_DEPENDENCY;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        for (MethodDeclaration method : context.getTestMethods()) {
            for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
                if (SINGLETON_PATTERNS.contains(call.getNameAsString())
                        && call.getScope().filter(s -> s instanceof NameExpr).isPresent()) {
                    smells.add(new TestSmell(
                            SmellType.HIDDEN_DEPENDENCY,
                            className,
                            method.getNameAsString(),
                            call.getBegin().map(p -> p.line).orElse(0),
                            "Singleton/static factory access: " + call.getScope().get() + "." + call.getNameAsString() + "()",
                            true
                    ));
                    break;
                }
            }
        }
        return smells;
    }
}
