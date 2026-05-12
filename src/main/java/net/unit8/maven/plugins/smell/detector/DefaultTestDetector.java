package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.MethodDeclaration;
import net.unit8.maven.plugins.smell.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class DefaultTestDetector implements SmellDetector {
    private static final Set<String> DEFAULT_CLASS_NAMES = Set.of(
            "AppTest",
            "ExampleTest", "ExampleTests",
            "ExampleUnitTest", "ExampleInstrumentedTest",
            "SampleTest", "SampleTests",
            "MyTest", "MyTests",
            "TestClass", "NewTest",
            "MainActivityTest"
    );

    private static final Set<String> DEFAULT_METHOD_NAMES = Set.of(
            "test", "testMethod", "testCase",
            "newTest", "myTest", "sampleTest", "exampleTest"
    );

    private static final Pattern NUMBERED_TEST_PATTERN =
            Pattern.compile("^test(Method|Case)?\\d+$");

    @Override
    public SmellType type() {
        return SmellType.DEFAULT_TEST;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        if (DEFAULT_CLASS_NAMES.contains(className)) {
            smells.add(new TestSmell(
                    SmellType.DEFAULT_TEST,
                    className,
                    null,
                    context.getTestClass().getBegin().map(p -> p.line).orElse(0),
                    "Test class has a default/IDE-generated name '" + className + "'"
            ));
        }

        for (MethodDeclaration method : context.getTestMethods()) {
            String methodName = method.getNameAsString();
            if (isDefaultMethodName(methodName)) {
                smells.add(new TestSmell(
                        SmellType.DEFAULT_TEST,
                        className,
                        methodName,
                        method.getBegin().map(p -> p.line).orElse(0),
                        "Test method has a default/IDE-generated name '" + methodName + "'"
                ));
            }
        }
        return smells;
    }

    private boolean isDefaultMethodName(String name) {
        return DEFAULT_METHOD_NAMES.contains(name)
                || NUMBERED_TEST_PATTERN.matcher(name).matches();
    }
}
