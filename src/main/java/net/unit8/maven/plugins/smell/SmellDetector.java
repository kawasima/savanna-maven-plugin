package net.unit8.maven.plugins.smell;

import java.util.List;

public interface SmellDetector {
    SmellType type();

    List<TestSmell> detect(DetectionContext context);
}
