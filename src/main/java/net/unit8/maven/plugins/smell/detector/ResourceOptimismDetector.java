package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import net.unit8.maven.plugins.smell.*;

import java.util.*;

public class ResourceOptimismDetector implements SmellDetector {

    /**
     * Constructor types that read from an existing file (and therefore should
     * be guarded by an existence check). Writers / output streams are excluded
     * — they create the file.
     */
    private static final Set<String> FILE_READ_TYPES = Set.of(
            "FileInputStream", "FileReader", "RandomAccessFile"
    );

    /**
     * Files.* read methods. Their first argument is a {@code Path}.
     */
    private static final Set<String> FILES_READ_METHODS = Set.of(
            "readAllBytes", "readAllLines", "readString",
            "newInputStream", "newBufferedReader", "lines"
    );

    /**
     * Static existence-check methods on {@code java.nio.file.Files}.
     */
    private static final Set<String> FILES_EXISTS_METHODS = Set.of(
            "exists", "notExists", "isReadable", "isRegularFile", "isDirectory"
    );

    /**
     * Instance existence-check methods on {@code java.io.File} / {@code java.nio.file.Path}.
     */
    private static final Set<String> INSTANCE_EXISTS_METHODS = Set.of(
            "exists", "isFile", "isDirectory"
    );

    @Override
    public SmellType type() {
        return SmellType.RESOURCE_OPTIMISM;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        for (MethodDeclaration method : context.getTestMethods()) {
            boolean readsExistingFile = hasFileReadConstructor(method) || hasFilesReadCall(method);
            if (!readsExistingFile) {
                continue;
            }

            if (!hasExistenceCheck(method)) {
                smells.add(new TestSmell(
                        SmellType.RESOURCE_OPTIMISM,
                        className,
                        method.getNameAsString(),
                        method.getBegin().map(p -> p.line).orElse(0),
                        "File read without existence check"
                ));
            }
        }
        return smells;
    }

    private boolean hasFileReadConstructor(MethodDeclaration method) {
        return method.findAll(ObjectCreationExpr.class).stream()
                .anyMatch(expr -> FILE_READ_TYPES.contains(expr.getTypeAsString()));
    }

    private boolean hasFilesReadCall(MethodDeclaration method) {
        return method.findAll(MethodCallExpr.class).stream()
                .anyMatch(call -> FILES_READ_METHODS.contains(call.getNameAsString())
                        && call.getScope()
                                .filter(s -> s instanceof NameExpr)
                                .map(s -> ((NameExpr) s).getNameAsString())
                                .filter("Files"::equals)
                                .isPresent());
    }

    private boolean hasExistenceCheck(MethodDeclaration method) {
        return method.findAll(MethodCallExpr.class).stream()
                .anyMatch(this::isExistenceCheck);
    }

    private boolean isExistenceCheck(MethodCallExpr call) {
        String name = call.getNameAsString();
        Expression scope = call.getScope().orElse(null);
        if (scope == null) {
            return false;
        }
        if (FILES_EXISTS_METHODS.contains(name) && scopeNameIs(scope, "Files")) {
            return true;
        }
        // Instance check: anything.exists(), anything.isFile() — accept only when
        // the receiver is a simple identifier (i.e. a local variable / field),
        // not a chained call result, to avoid matching unrelated APIs like
        // some.builder().exists().
        if (INSTANCE_EXISTS_METHODS.contains(name) && scope instanceof NameExpr) {
            return true;
        }
        return false;
    }

    private boolean scopeNameIs(Expression scope, String expected) {
        if (scope instanceof NameExpr) {
            return ((NameExpr) scope).getNameAsString().equals(expected);
        }
        if (scope instanceof FieldAccessExpr) {
            return ((FieldAccessExpr) scope).getNameAsString().equals(expected);
        }
        return false;
    }
}
