package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import net.unit8.maven.plugins.smell.*;

import java.util.*;

public class ResourceOptimismDetector implements SmellDetector {
    private static final Set<String> FILE_TYPES = new TreeSet<>(Arrays.asList(
            "File", "FileInputStream", "FileOutputStream",
            "FileReader", "FileWriter",
            "RandomAccessFile", "FileChannel"
    ));

    @Override
    public SmellType type() {
        return SmellType.RESOURCE_OPTIMISM;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        for (MethodDeclaration method : context.getTestMethods()) {
            boolean hasFileAccess = method.findAll(ObjectCreationExpr.class).stream()
                    .anyMatch(expr -> FILE_TYPES.contains(expr.getTypeAsString()));

            if (!hasFileAccess) {
                continue;
            }

            boolean hasExistsCheck = method.findAll(MethodCallExpr.class).stream()
                    .anyMatch(call -> call.getNameAsString().equals("exists")
                            || call.getNameAsString().equals("isFile")
                            || call.getNameAsString().equals("isDirectory"));

            if (!hasExistsCheck) {
                smells.add(new TestSmell(
                        SmellType.RESOURCE_OPTIMISM,
                        className,
                        method.getNameAsString(),
                        method.getBegin().map(p -> p.line).orElse(0),
                        "File access without existence check"
                ));
            }
        }
        return smells;
    }
}
