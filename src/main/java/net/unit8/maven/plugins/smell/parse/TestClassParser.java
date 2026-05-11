package net.unit8.maven.plugins.smell.parse;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import net.unit8.maven.plugins.smell.DetectionContext;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TestClassParser {
    private static final List<String> TEST_ANNOTATIONS = List.of(
            "Test", "ParameterizedTest", "RepeatedTest"
    );
    private static final List<String> SETUP_ANNOTATIONS = List.of(
            "BeforeEach", "BeforeAll"
    );
    private static final List<String> TEARDOWN_ANNOTATIONS = List.of(
            "AfterEach", "AfterAll"
    );

    private final JavaParser javaParser = new JavaParser(
            new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE));

    public List<DetectionContext> parse(Path sourceFile) throws IOException {
        ParseResult<CompilationUnit> result = javaParser.parse(sourceFile);
        CompilationUnit cu = result.getResult()
                .orElseThrow(() -> new ParseProblemException(result.getProblems()));
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

            List<MethodDeclaration> teardownMethods = clazz.getMethods().stream()
                    .filter(this::isTeardownMethod)
                    .collect(Collectors.toList());

            List<FieldDeclaration> fields = clazz.getFields();

            contexts.add(new DetectionContext(cu, clazz, testMethods, setupMethods, teardownMethods, fields, sourceFile));
        }

        return contexts;
    }

    public DetectionContext parseSource(String source) {
        ParseResult<CompilationUnit> result = javaParser.parse(source);
        CompilationUnit cu = result.getResult()
                .orElseThrow(() -> new ParseProblemException(result.getProblems()));
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

        List<MethodDeclaration> teardownMethods = clazz.getMethods().stream()
                .filter(this::isTeardownMethod)
                .collect(Collectors.toList());

        List<FieldDeclaration> fields = clazz.getFields();

        return new DetectionContext(cu, clazz, testMethods, setupMethods, teardownMethods, fields, null);
    }

    private boolean isTestMethod(MethodDeclaration method) {
        return TEST_ANNOTATIONS.stream()
                .anyMatch(ann -> method.getAnnotationByName(ann).isPresent());
    }

    private boolean isSetupMethod(MethodDeclaration method) {
        return SETUP_ANNOTATIONS.stream()
                .anyMatch(ann -> method.getAnnotationByName(ann).isPresent());
    }

    private boolean isTeardownMethod(MethodDeclaration method) {
        return TEARDOWN_ANNOTATIONS.stream()
                .anyMatch(ann -> method.getAnnotationByName(ann).isPresent());
    }
}
