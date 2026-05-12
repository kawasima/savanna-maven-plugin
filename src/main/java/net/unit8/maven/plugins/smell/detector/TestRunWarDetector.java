package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import net.unit8.maven.plugins.smell.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Detects shared OS-level resources that make tests collide when run in
 * parallel: hardcoded ports for sockets, and absolute filesystem paths
 * under well-known shared roots like /tmp, /var, C:\Temp.
 *
 * Use {@code @TempDir} or {@code new ServerSocket(0)} to avoid these collisions.
 */
public class TestRunWarDetector implements SmellDetector {
    private static final Set<String> SOCKET_TYPES = Set.of(
            "ServerSocket", "DatagramSocket", "Socket"
    );

    private static final Set<String> FILE_TYPES = Set.of(
            "File", "FileInputStream", "FileOutputStream",
            "FileReader", "FileWriter", "RandomAccessFile"
    );

    @Override
    public SmellType type() {
        return SmellType.TEST_RUN_WAR;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        for (MethodDeclaration method : context.getTestMethods()) {
            for (ObjectCreationExpr ctor : method.findAll(ObjectCreationExpr.class)) {
                String typeName = ctor.getType().getNameAsString();
                if (SOCKET_TYPES.contains(typeName)) {
                    int port = firstIntArg(ctor);
                    if (port > 0) {
                        smells.add(make(className, method,
                                "Test binds hardcoded port " + port
                                        + " — parallel runs will collide. Use port 0 for a free port."));
                    }
                } else if (FILE_TYPES.contains(typeName)) {
                    String path = firstStringArg(ctor);
                    if (path != null && isSharedFilesystemPath(path)) {
                        smells.add(make(className, method,
                                "Test uses hardcoded shared path '" + path
                                        + "' — parallel runs will collide. Use @TempDir."));
                    }
                }
            }
        }
        return smells;
    }

    private TestSmell make(String className, MethodDeclaration method, String message) {
        return new TestSmell(
                SmellType.TEST_RUN_WAR,
                className,
                method.getNameAsString(),
                method.getBegin().map(p -> p.line).orElse(0),
                message
        );
    }

    private int firstIntArg(ObjectCreationExpr ctor) {
        if (ctor.getArguments().isEmpty()) {
            return -1;
        }
        if (ctor.getArgument(0) instanceof IntegerLiteralExpr) {
            try {
                return ((IntegerLiteralExpr) ctor.getArgument(0)).asNumber().intValue();
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }
        return -1;
    }

    private String firstStringArg(ObjectCreationExpr ctor) {
        if (ctor.getArguments().isEmpty()) {
            return null;
        }
        if (ctor.getArgument(0) instanceof StringLiteralExpr) {
            return ((StringLiteralExpr) ctor.getArgument(0)).asString();
        }
        return null;
    }

    private boolean isSharedFilesystemPath(String path) {
        return isUnderDir(path, "/tmp")
                || isUnderDir(path, "/var")
                || isUnderDir(path, "/dev")
                || isUnderDir(path, "C:\\Temp")
                || isUnderDir(path, "C:/Temp")
                || isUnderDir(path, "C:\\Windows\\Temp")
                || isUnderDir(path, "C:/Windows/Temp");
    }

    /**
     * True when {@code path} equals {@code dir} exactly or is under it
     * (followed by a path separator). Rejects {@code /tmpfile} for dir
     * {@code /tmp}.
     */
    private boolean isUnderDir(String path, String dir) {
        if (!path.startsWith(dir)) {
            return false;
        }
        if (path.length() == dir.length()) {
            return true;
        }
        char next = path.charAt(dir.length());
        return next == '/' || next == '\\';
    }
}
