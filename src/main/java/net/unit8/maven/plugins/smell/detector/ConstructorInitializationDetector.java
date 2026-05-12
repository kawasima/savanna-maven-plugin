package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import net.unit8.maven.plugins.smell.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConstructorInitializationDetector implements SmellDetector {
    @Override
    public SmellType type() {
        return SmellType.CONSTRUCTOR_INITIALIZATION;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        List<ConstructorDeclaration> constructors = context.getTestClass()
                .findAll(ConstructorDeclaration.class);

        for (ConstructorDeclaration ctor : constructors) {
            if (ctor.getBody().isEmpty() || ctor.getBody().getStatements().isEmpty()) {
                continue;
            }
            if (isJustInjectionPlumbing(ctor)) {
                continue;
            }
            smells.add(new TestSmell(
                    SmellType.CONSTRUCTOR_INITIALIZATION,
                    className,
                    null,
                    ctor.getBegin().map(p -> p.line).orElse(0),
                    "Test class uses constructor for initialization instead of @BeforeEach"
            ));
        }
        return smells;
    }

    /**
     * A constructor is "just injection plumbing" when every statement either
     * (a) chains to super/this, or
     * (b) assigns a constructor parameter (or expression derived from one) to a field.
     * Such constructors are how JUnit 5 receives injected dependencies and aren't a smell.
     */
    private boolean isJustInjectionPlumbing(ConstructorDeclaration ctor) {
        Set<String> paramNames = new HashSet<>();
        for (Parameter p : ctor.getParameters()) {
            paramNames.add(p.getNameAsString());
        }
        if (paramNames.isEmpty()) {
            // No injected parameters — any real body counts as initialization work.
            return false;
        }
        for (Statement stmt : ctor.getBody().getStatements()) {
            if (stmt instanceof ExplicitConstructorInvocationStmt) {
                continue;
            }
            if (!(stmt instanceof ExpressionStmt)) {
                return false;
            }
            Expression expr = ((ExpressionStmt) stmt).getExpression();
            if (!(expr instanceof AssignExpr)) {
                return false;
            }
            AssignExpr assign = (AssignExpr) expr;
            if (!isFieldTarget(assign.getTarget())) {
                return false;
            }
            if (!referencesOnly(assign.getValue(), paramNames)) {
                return false;
            }
        }
        return true;
    }

    private boolean isFieldTarget(Expression target) {
        if (target instanceof FieldAccessExpr) {
            FieldAccessExpr fae = (FieldAccessExpr) target;
            return fae.getScope() instanceof ThisExpr;
        }
        return target instanceof NameExpr;
    }

    /**
     * True when {@code value} contains no method calls and references no identifiers
     * outside of {@code paramNames} (other than literals). This keeps "assign param"
     * patterns out of the smell but flags any computation as real init work.
     */
    private boolean referencesOnly(Expression value, Set<String> paramNames) {
        for (NameExpr ne : value.findAll(NameExpr.class)) {
            if (!paramNames.contains(ne.getNameAsString())) {
                return false;
            }
        }
        return value.findAll(com.github.javaparser.ast.expr.MethodCallExpr.class).isEmpty()
                && value.findAll(com.github.javaparser.ast.expr.ObjectCreationExpr.class).isEmpty();
    }
}
