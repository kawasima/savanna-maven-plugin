package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.MethodDeclaration;
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

    private static final Set<String> EXISTS_METHODS = Set.of(
            "exists", "isFile", "isDirectory", "notExists", "isReadable"
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
                .anyMatch(call -> EXISTS_METHODS.contains(call.getNameAsString()));
    }
}
