package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.TryStmt;
import net.unit8.maven.plugins.smell.*;

import java.util.*;

public class ResourceLeakageDetector implements SmellDetector {
    private static final Set<String> CLOSEABLE_TYPES = Set.of(
            "InputStream", "OutputStream",
            "FileInputStream", "FileOutputStream",
            "BufferedReader", "BufferedWriter",
            "FileReader", "FileWriter",
            "InputStreamReader", "OutputStreamWriter",
            "ByteArrayInputStream", "ByteArrayOutputStream",
            "ObjectInputStream", "ObjectOutputStream",
            "PrintWriter", "PrintStream",
            "Scanner", "Connection",
            "Statement", "PreparedStatement",
            "ResultSet", "Socket",
            "ServerSocket", "Channel"
    );

    @Override
    public SmellType type() {
        return SmellType.RESOURCE_LEAKAGE;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        for (MethodDeclaration method : context.getTestMethods()) {
            List<ObjectCreationExpr> creations = method.findAll(ObjectCreationExpr.class,
                    expr -> CLOSEABLE_TYPES.contains(expr.getTypeAsString()));

            if (creations.isEmpty()) {
                continue;
            }

            // Check if all resource creations are inside try-with-resources
            for (ObjectCreationExpr creation : creations) {
                boolean inTryWithResources = creation.findAncestor(TryStmt.class)
                        .filter(t -> !t.getResources().isEmpty())
                        .isPresent();

                if (!inTryWithResources) {
                    smells.add(new TestSmell(
                            SmellType.RESOURCE_LEAKAGE,
                            className,
                            method.getNameAsString(),
                            creation.getBegin().map(p -> p.line).orElse(0),
                            "Resource '" + creation.getTypeAsString() + "' not in try-with-resources"
                    ));
                }
            }
        }
        return smells;
    }
}
