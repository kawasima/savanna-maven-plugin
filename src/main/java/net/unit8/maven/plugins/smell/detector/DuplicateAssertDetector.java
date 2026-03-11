package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import net.unit8.maven.plugins.smell.*;

import java.util.*;

public class DuplicateAssertDetector implements SmellDetector {
    private static final Set<String> ASSERTION_METHODS = new TreeSet<>(Arrays.asList(
            "assertEquals", "assertNotEquals",
            "assertTrue", "assertFalse",
            "assertNull", "assertNotNull",
            "assertSame", "assertNotSame",
            "assertArrayEquals", "assertThrows",
            "assertThat"
    ));

    @Override
    public SmellType type() {
        return SmellType.DUPLICATE_ASSERT;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        for (MethodDeclaration method : context.getTestMethods()) {
            List<MethodCallExpr> assertions = method.findAll(MethodCallExpr.class,
                    call -> ASSERTION_METHODS.contains(call.getNameAsString()));

            Set<String> seen = new HashSet<>();
            Set<String> duplicated = new LinkedHashSet<>();
            for (MethodCallExpr assertion : assertions) {
                String key = assertion.toString();
                if (!seen.add(key)) {
                    duplicated.add(key);
                }
            }

            if (!duplicated.isEmpty()) {
                smells.add(new TestSmell(
                        SmellType.DUPLICATE_ASSERT,
                        className,
                        method.getNameAsString(),
                        method.getBegin().map(p -> p.line).orElse(0),
                        duplicated.size() + " duplicated assertion(s)"
                ));
            }
        }
        return smells;
    }
}
