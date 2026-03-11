package net.unit8.maven.plugins.smell.detector;

import net.unit8.maven.plugins.smell.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DefaultTestDetector implements SmellDetector {
    private static final Set<String> DEFAULT_NAMES = Set.of(
            "ExampleTest", "ExampleTests",
            "ExampleUnitTest", "ExampleInstrumentedTest",
            "SampleTest", "SampleTests",
            "MyTest", "MyTests",
            "TestClass", "NewTest",
            "MainActivityTest"
    );

    @Override
    public SmellType type() {
        return SmellType.DEFAULT_TEST;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        if (DEFAULT_NAMES.contains(className)) {
            smells.add(new TestSmell(
                    SmellType.DEFAULT_TEST,
                    className,
                    null,
                    context.getTestClass().getBegin().map(p -> p.line).orElse(0),
                    "Test class has a default/IDE-generated name '" + className + "'"
            ));
        }
        return smells;
    }
}
