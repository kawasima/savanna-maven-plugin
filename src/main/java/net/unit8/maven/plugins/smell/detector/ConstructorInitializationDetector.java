package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.ConstructorDeclaration;
import net.unit8.maven.plugins.smell.*;

import java.util.ArrayList;
import java.util.List;

public class ConstructorInitializationDetector implements SmellDetector {
    @Override
    public SmellType type() {
        return SmellType.CONSTRUCTOR_INITIALIZATION;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        List<ConstructorDeclaration> constructors = context.getTestClass()
                .findAll(ConstructorDeclaration.class);

        for (ConstructorDeclaration ctor : constructors) {
            if (ctor.getBody().isEmpty()) {
                continue;
            }
            smells.add(new TestSmell(
                    SmellType.CONSTRUCTOR_INITIALIZATION,
                    className,
                    null,
                    ctor.getBegin().map(p -> p.line).orElse(0),
                    "Test class uses constructor for initialization instead of @BeforeEach"
            ));
        }
        return smells;
    }
}
