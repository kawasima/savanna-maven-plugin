package net.unit8.maven.plugins.smell.resolver;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ConventionBasedResolver implements TestToProductionResolver {
    private static final List<String> SUFFIXES = Arrays.asList("Test", "Tests", "IT", "Spec");
    private static final List<String> PREFIXES = Arrays.asList("Test");

    private final Map<String, Optional<CompilationUnit>> cache = new ConcurrentHashMap<>();

    @Override
    public Optional<CompilationUnit> resolve(String testClassName, String packageName, Path sourceDirectory) {
        String productionClassName = deriveProductionClassName(testClassName);
        if (productionClassName == null || sourceDirectory == null) {
            return Optional.empty();
        }

        String key = packageName + "." + productionClassName;
        return cache.computeIfAbsent(key, k -> {
            String relativePath = packageName.replace('.', '/') + "/" + productionClassName + ".java";
            Path productionFile = sourceDirectory.resolve(relativePath);
            if (Files.exists(productionFile)) {
                try {
                    return Optional.of(StaticJavaParser.parse(productionFile));
                } catch (IOException e) {
                    return Optional.empty();
                }
            }
            return Optional.empty();
        });
    }

    public static String deriveProductionClassName(String testClassName) {
        for (String suffix : SUFFIXES) {
            if (testClassName.endsWith(suffix) && testClassName.length() > suffix.length()) {
                return testClassName.substring(0, testClassName.length() - suffix.length());
            }
        }
        for (String prefix : PREFIXES) {
            if (testClassName.startsWith(prefix) && testClassName.length() > prefix.length()) {
                return testClassName.substring(prefix.length());
            }
        }
        return null;
    }
}
