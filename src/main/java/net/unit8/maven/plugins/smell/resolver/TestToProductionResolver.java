package net.unit8.maven.plugins.smell.resolver;

import com.github.javaparser.ast.CompilationUnit;

import java.nio.file.Path;
import java.util.Optional;

public interface TestToProductionResolver {
    Optional<CompilationUnit> resolve(String testClassName, String packageName, Path sourceDirectory);
}
