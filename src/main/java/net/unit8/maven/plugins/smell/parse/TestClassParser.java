package net.unit8.maven.plugins.smell.parse;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import net.unit8.maven.plugins.smell.DetectionContext;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TestClassParser {
    private static final List<String> TEST_ANNOTATIONS = Arrays.asList(
            "Test", "ParameterizedTest", "RepeatedTest"
    );
    private static final List<String> SETUP_ANNOTATIONS = Arrays.asList(
            "BeforeEach", "BeforeAll"
    );

    public List<DetectionContext> parse(Path sourceFile) throws IOException {
        CompilationUnit cu = StaticJavaParser.parse(sourceFile);
        List<DetectionContext> contexts = new ArrayList<>();

        for (ClassOrInterfaceDeclaration clazz : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            List<MethodDeclaration> testMethods = clazz.getMethods().stream()
                    .filter(this::isTestMethod)
                    .collect(Collectors.toList());

            if (testMethods.isEmpty()) {
                continue;
            }

            List<MethodDeclaration> setupMethods = clazz.getMethods().stream()
                    .filter(this::isSetupMethod)
                    .collect(Collectors.toList());

            List<FieldDeclaration> fields = clazz.getFields();

            contexts.add(new DetectionContext(cu, clazz, testMethods, setupMethods, fields, sourceFile));
        }

        return contexts;
    }

    public DetectionContext parseSource(String source) {
        CompilationUnit cu = StaticJavaParser.parse(source);
        Optional<ClassOrInterfaceDeclaration> clazzOpt = cu.findFirst(ClassOrInterfaceDeclaration.class);
        if (!clazzOpt.isPresent()) {
            return null;
        }
        ClassOrInterfaceDeclaration clazz = clazzOpt.get();

        List<MethodDeclaration> testMethods = clazz.getMethods().stream()
                .filter(this::isTestMethod)
                .collect(Collectors.toList());

        List<MethodDeclaration> setupMethods = clazz.getMethods().stream()
                .filter(this::isSetupMethod)
                .collect(Collectors.toList());

        List<FieldDeclaration> fields = clazz.getFields();

        return new DetectionContext(cu, clazz, testMethods, setupMethods, fields, null);
    }

    private boolean isTestMethod(MethodDeclaration method) {
        return TEST_ANNOTATIONS.stream()
                .anyMatch(ann -> method.getAnnotationByName(ann).isPresent());
    }

    private boolean isSetupMethod(MethodDeclaration method) {
        return SETUP_ANNOTATIONS.stream()
                .anyMatch(ann -> method.getAnnotationByName(ann).isPresent());
    }
}
