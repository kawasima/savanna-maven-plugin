package net.unit8.maven.plugins.smell;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class DetectionContext {
    private final CompilationUnit compilationUnit;
    private final ClassOrInterfaceDeclaration testClass;
    private final List<MethodDeclaration> testMethods;
    private final List<MethodDeclaration> setupMethods;
    private final List<MethodDeclaration> teardownMethods;
    private final List<FieldDeclaration> fields;
    private final Path sourceFile;

    public DetectionContext(CompilationUnit compilationUnit,
                           ClassOrInterfaceDeclaration testClass,
                           List<MethodDeclaration> testMethods,
                           List<MethodDeclaration> setupMethods,
                           List<FieldDeclaration> fields,
                           Path sourceFile) {
        this(compilationUnit, testClass, testMethods, setupMethods, Collections.emptyList(), fields, sourceFile);
    }

    public DetectionContext(CompilationUnit compilationUnit,
                           ClassOrInterfaceDeclaration testClass,
                           List<MethodDeclaration> testMethods,
                           List<MethodDeclaration> setupMethods,
                           List<MethodDeclaration> teardownMethods,
                           List<FieldDeclaration> fields,
                           Path sourceFile) {
        this.compilationUnit = compilationUnit;
        this.testClass = testClass;
        this.testMethods = testMethods;
        this.setupMethods = setupMethods;
        this.teardownMethods = teardownMethods;
        this.fields = fields;
        this.sourceFile = sourceFile;
    }

    public CompilationUnit getCompilationUnit() {
        return compilationUnit;
    }

    public ClassOrInterfaceDeclaration getTestClass() {
        return testClass;
    }

    public List<MethodDeclaration> getTestMethods() {
        return testMethods;
    }

    public List<MethodDeclaration> getSetupMethods() {
        return setupMethods;
    }

    public List<MethodDeclaration> getTeardownMethods() {
        return teardownMethods;
    }

    public List<FieldDeclaration> getFields() {
        return fields;
    }

    public Path getSourceFile() {
        return sourceFile;
    }
}
