package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import net.unit8.maven.plugins.smell.*;

import java.util.*;

public class MysteryGuestDetector implements SmellDetector {
    private static final Set<String> IO_TYPES = Set.of(
            "File", "FileInputStream", "FileOutputStream",
            "FileReader", "FileWriter",
            "BufferedReader", "BufferedWriter",
            "Path", "Paths",
            "RandomAccessFile",
            "URL", "HttpURLConnection",
            "Socket", "ServerSocket",
            "Connection", "DriverManager"
    );

    private static final Set<String> IO_METHOD_CALLS = Set.of(
            "readAllLines", "readAllBytes", "readString",
            "write", "newInputStream", "newOutputStream",
            "newBufferedReader", "newBufferedWriter",
            "getConnection", "openConnection"
    );

    @Override
    public SmellType type() {
        return SmellType.MYSTERY_GUEST;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        for (MethodDeclaration method : context.getTestMethods()) {
            boolean hasExternalResource = method.findAll(ObjectCreationExpr.class).stream()
                    .anyMatch(expr -> IO_TYPES.contains(expr.getTypeAsString()));

            if (!hasExternalResource) {
                hasExternalResource = method.findAll(MethodCallExpr.class).stream()
                        .anyMatch(call -> IO_METHOD_CALLS.contains(call.getNameAsString())
                                && call.getScope()
                                .filter(s -> s instanceof NameExpr)
                                .map(s -> ((NameExpr) s).getNameAsString())
                                .filter(name -> name.equals("Files") || name.equals("DriverManager"))
                                .isPresent());
            }

            if (hasExternalResource) {
                smells.add(new TestSmell(
                        SmellType.MYSTERY_GUEST,
                        className,
                        method.getNameAsString(),
                        method.getBegin().map(p -> p.line).orElse(0),
                        "Test accesses external resource (file, network, database)"
                ));
            }
        }
        return smells;
    }
}
