package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import net.unit8.maven.plugins.smell.*;

import java.util.*;

public class HiddenDependencyDetector implements SmellDetector {
    private static final Set<String> SINGLETON_PATTERNS = Set.of(
            "getInstance", "getDefault", "getSingleton",
            "newInstance", "current", "getContext"
    );

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
                String description = describeHiddenDependency(call);
                if (description != null) {
                    smells.add(new TestSmell(
                            SmellType.HIDDEN_DEPENDENCY,
                            className,
                            method.getNameAsString(),
                            call.getBegin().map(p -> p.line).orElse(0),
                            description,
                            true
                    ));
                    break;
                }
            }
        }
        return smells;
    }

    private String describeHiddenDependency(MethodCallExpr call) {
        String name = call.getNameAsString();
        String scopeName = call.getScope()
                .filter(s -> s instanceof NameExpr)
                .map(s -> ((NameExpr) s).getNameAsString())
                .orElse(null);
        if (scopeName == null) {
            return null;
        }
        if ("System".equals(scopeName) && (name.equals("getProperty")
                || name.equals("getenv") || name.equals("getProperties"))) {
            return "Reads global environment state: System." + name + "()";
        }
        if (SINGLETON_PATTERNS.contains(name)) {
            return "Singleton/static factory access: " + scopeName + "." + name + "()";
        }
        return null;
    }
}
