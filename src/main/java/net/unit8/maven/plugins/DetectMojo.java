package net.unit8.maven.plugins;

import net.unit8.maven.plugins.smell.*;
import net.unit8.maven.plugins.smell.parse.TestClassParser;
import net.unit8.maven.plugins.smell.report.ConsoleSmellReporter;
import net.unit8.maven.plugins.smell.report.JsonSmellReporter;
import net.unit8.maven.plugins.smell.report.MarkdownSmellReporter;
import net.unit8.maven.plugins.smell.report.SmellReporter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

@Mojo(name = "roar", defaultPhase = LifecyclePhase.PROCESS_TEST_SOURCES)
public class DetectMojo extends AbstractMojo {

    @Parameter(property = "savanna.testSourceDirectory",
            defaultValue = "${project.build.testSourceDirectory}")
    private File testSourceDirectory;

    @Parameter(property = "savanna.includes")
    private List<String> includes;

    @Parameter(property = "savanna.excludes")
    private List<String> excludes;

    @Parameter(property = "savanna.enabledSmells")
    private List<String> enabledSmells;

    @Parameter(property = "savanna.disabledSmells")
    private List<String> disabledSmells;

    @Parameter(property = "savanna.failOnSmell", defaultValue = "false")
    private boolean failOnSmell;

    @Parameter(property = "savanna.reportFormat", defaultValue = "console")
    private String reportFormat;

    @Parameter(property = "savanna.reportOutputDirectory",
            defaultValue = "${project.build.directory}/savanna-reports")
    private File reportOutputDirectory;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    private final LionBanner banner = new LionBanner();

    @Override
    public void execute() throws MojoFailureException {
        Map<String, List<TestSmell>> smellsByFile = new LinkedHashMap<>();

        // Project-level check: skip testing detection
        if (isSmellEnabled(SmellType.SKIP_TESTING)) {
            detectSkipTesting(smellsByFile);
        }

        if (testSourceDirectory != null && testSourceDirectory.exists()) {
            List<SmellDetector> detectors = SmellDetectorRegistry.filter(enabledSmells, disabledSmells);
            if (!detectors.isEmpty()) {
                getLog().info("Scanning for test smells in " + testSourceDirectory.getAbsolutePath());
                detectTestSmells(detectors, smellsByFile);
            }
        } else if (isSmellEnabled(SmellType.NO_TEST)) {
            smellsByFile.computeIfAbsent("(project)", k -> new ArrayList<>()).add(
                    new TestSmell(SmellType.NO_TEST, "(project)", null, 0,
                            "No test source directory found"));
        }

        if (smellsByFile.isEmpty()) {
            getLog().info("No test smells detected.");
            return;
        }

        // Pick a random detected smell and display the lion banner
        List<TestSmell> allSmells = smellsByFile.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
        TestSmell randomSmell = allSmells.get(new Random().nextInt(allSmells.size()));
        getLog().warn(banner.roar(randomSmell.getType().getPropertyKey()));

        SmellReporter reporter;
        if ("json".equalsIgnoreCase(reportFormat)) {
            reporter = new JsonSmellReporter(reportOutputDirectory);
            getLog().info("Writing JSON report to " + reportOutputDirectory.getAbsolutePath());
        } else if ("markdown".equalsIgnoreCase(reportFormat) || "md".equalsIgnoreCase(reportFormat)) {
            reporter = new MarkdownSmellReporter(reportOutputDirectory);
            getLog().info("Writing Markdown report to " + reportOutputDirectory.getAbsolutePath());
        } else {
            reporter = new ConsoleSmellReporter(getLog());
        }
        reporter.report(smellsByFile);

        int totalSmells = smellsByFile.values().stream().mapToInt(List::size).sum();
        if (failOnSmell && totalSmells > 0) {
            throw new MojoFailureException("Found " + totalSmells + " test smell(s).");
        }
    }

    private void detectSkipTesting(Map<String, List<TestSmell>> smellsByFile) {
        boolean skipTestsInConfig = project.getBuildPlugins()
                .stream()
                .filter(p -> Objects.equals(p.getGroupId(), "org.apache.maven.plugins")
                        && Objects.equals(p.getArtifactId(), "maven-surefire-plugin"))
                .findAny()
                .map(p -> (Xpp3Dom) p.getConfiguration())
                .filter(Objects::nonNull)
                .map(dom -> dom.getChild("skipTests"))
                .filter(Objects::nonNull)
                .map(dom -> Objects.equals(dom.getValue(), "true"))
                .orElse(false);

        boolean skipByProperty = Objects.nonNull(System.getProperty("skipTests"))
                || Objects.equals(System.getProperty("maven.test.skip"), "true")
                || "true".equals(project.getProperties().getProperty("skipTests"))
                || "true".equals(project.getProperties().getProperty("maven.test.skip"));

        if (skipTestsInConfig || skipByProperty) {
            smellsByFile.computeIfAbsent("(project)", k -> new ArrayList<>()).add(
                    new TestSmell(SmellType.SKIP_TESTING, "(project)", null, 0,
                            "Test execution is skipped"));
        }
    }

    private void detectTestSmells(List<SmellDetector> detectors,
                                  Map<String, List<TestSmell>> smellsByFile) {
        List<Path> testFiles = collectTestFiles();

        TestClassParser parser = new TestClassParser();
        int totalTestMethods = 0;
        int totalDisabledMethods = 0;

        for (Path testFile : testFiles) {
            try {
                List<DetectionContext> contexts = parser.parse(testFile);
                List<TestSmell> fileSmells = new ArrayList<>();
                for (DetectionContext context : contexts) {
                    totalTestMethods += context.getTestMethods().size();
                    totalDisabledMethods += context.getTestMethods().stream()
                            .filter(m -> m.getAnnotationByName("Disabled").isPresent())
                            .count();
                    for (SmellDetector detector : detectors) {
                        fileSmells.addAll(detector.detect(context));
                    }
                }
                if (!fileSmells.isEmpty()) {
                    String relativePath = testSourceDirectory.toPath().relativize(testFile).toString();
                    smellsByFile.put(relativePath, fileSmells);
                }
            } catch (IOException e) {
                getLog().warn("Failed to parse " + testFile + ": " + e.getMessage());
            }
        }

        // Project-level check: no executable tests
        if (isSmellEnabled(SmellType.NO_TEST)) {
            if (testFiles.isEmpty()) {
                smellsByFile.computeIfAbsent("(project)", k -> new ArrayList<>()).add(
                        new TestSmell(SmellType.NO_TEST, "(project)", null, 0,
                                "No test files found in " + testSourceDirectory.getAbsolutePath()));
            } else if (totalTestMethods == 0) {
                smellsByFile.computeIfAbsent("(project)", k -> new ArrayList<>()).add(
                        new TestSmell(SmellType.NO_TEST, "(project)", null, 0,
                                "Test files exist but no @Test methods found"));
            } else if (totalTestMethods > 0 && totalTestMethods == totalDisabledMethods) {
                smellsByFile.computeIfAbsent("(project)", k -> new ArrayList<>()).add(
                        new TestSmell(SmellType.NO_TEST, "(project)", null, 0,
                                "All " + totalTestMethods + " test method(s) are @Disabled"));
            }
        }
    }

    private List<Path> collectTestFiles() {
        List<Path> files = new ArrayList<>();
        Path root = testSourceDirectory.toPath();

        List<String> includePatterns = (includes != null && !includes.isEmpty())
                ? includes
                : Arrays.asList("**/*Test.java", "**/*Tests.java", "**/*IT.java");

        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (!file.toString().endsWith(".java")) {
                        return FileVisitResult.CONTINUE;
                    }
                    String relativePath = root.relativize(file).toString();
                    boolean matched = includePatterns.stream()
                            .anyMatch(pattern -> matchGlob(pattern, relativePath));
                    if (matched) {
                        boolean excluded = excludes != null && excludes.stream()
                                .anyMatch(pattern -> matchGlob(pattern, relativePath));
                        if (!excluded) {
                            files.add(file);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            getLog().warn("Error scanning test directory: " + e.getMessage());
        }

        return files;
    }

    private boolean isSmellEnabled(SmellType smellType) {
        return SmellDetectorRegistry.isEnabled(smellType, enabledSmells, disabledSmells);
    }

    private static final Map<String, PathMatcher> GLOB_CACHE = new HashMap<>();

    private static boolean matchGlob(String pattern, String path) {
        PathMatcher matcher = GLOB_CACHE.computeIfAbsent(pattern,
                p -> FileSystems.getDefault().getPathMatcher("glob:" + p));
        return matcher.matches(Paths.get(path));
    }
}
