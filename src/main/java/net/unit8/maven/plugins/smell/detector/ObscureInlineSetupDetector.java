package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import net.unit8.maven.plugins.smell.*;

import java.util.*;

public class ObscureInlineSetupDetector implements SmellDetector {
    private static final int DEFAULT_THRESHOLD = 10;
    private static final Set<String> ASSERTION_METHODS = new TreeSet<>(Arrays.asList(
            "assertEquals", "assertNotEquals",
            "assertTrue", "assertFalse",
            "assertNull", "assertNotNull",
            "assertSame", "assertNotSame",
            "assertArrayEquals", "assertThrows", "assertDoesNotThrow",
            "assertTimeout", "assertTimeoutPreemptively",
            "assertAll", "assertIterableEquals",
            "assertLinesMatch", "assertInstanceOf",
            "fail", "assertThat", "assertThatThrownBy",
            "assertThatCode", "assertThatExceptionOfType"
    ));

    private final int threshold;

    public ObscureInlineSetupDetector() {
        this(DEFAULT_THRESHOLD);
    }

    public ObscureInlineSetupDetector(int threshold) {
        this.threshold = threshold;
    }

    @Override
    public SmellType type() {
        return SmellType.OBSCURE_INLINE_SETUP;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        for (MethodDeclaration method : context.getTestMethods()) {
            if (!method.getBody().isPresent()) {
                continue;
            }
            List<Statement> statements = method.getBody().get().getStatements();
            int setupCount = 0;

            for (Statement stmt : statements) {
                if (isAssertionStatement(stmt)) {
                    break;
                }
                setupCount++;
            }

            if (setupCount > threshold) {
                smells.add(new TestSmell(
                        SmellType.OBSCURE_INLINE_SETUP,
                        className,
                        method.getNameAsString(),
                        method.getBegin().map(p -> p.line).orElse(0),
                        setupCount + " setup statements before first assertion (threshold: " + threshold + ")"
                ));
            }
        }
        return smells;
    }

    private boolean isAssertionStatement(Statement stmt) {
        if (!(stmt instanceof ExpressionStmt)) {
            return false;
        }
        return stmt.findAll(MethodCallExpr.class).stream()
                .anyMatch(call -> ASSERTION_METHODS.contains(call.getNameAsString()));
    }
}
