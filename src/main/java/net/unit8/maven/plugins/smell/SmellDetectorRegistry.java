package net.unit8.maven.plugins.smell;

import net.unit8.maven.plugins.smell.detector.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SmellDetectorRegistry {
    private SmellDetectorRegistry() {
    }

    public static List<SmellDetector> allDetectors() {
        List<SmellDetector> detectors = new ArrayList<>();
        detectors.add(new EmptyTestDetector());
        detectors.add(new IgnoredTestDetector());
        detectors.add(new MissingAssertionDetector());
        detectors.add(new RedundantPrintDetector());
        detectors.add(new SleepyTestDetector());
        detectors.add(new ConditionalTestLogicDetector());
        detectors.add(new ExceptionHandlingDetector());
        detectors.add(new ConstructorInitializationDetector());
        detectors.add(new AssertionRouletteDetector());
        detectors.add(new DefaultTestDetector());
        // Phase 2
        detectors.add(new MagicNumberTestDetector());
        detectors.add(new RedundantAssertionDetector());
        detectors.add(new SensitiveEqualityDetector());
        detectors.add(new DuplicateAssertDetector());
        detectors.add(new VerboseTestDetector());
        detectors.add(new ObscureInlineSetupDetector());
        detectors.add(new TestRunWarDetector());
        detectors.add(new ResourceOptimismDetector());
        detectors.add(new ResourceLeakageDetector());
        detectors.add(new OrderDependentTestDetector());
        // Phase 3
        detectors.add(new GeneralFixtureDetector());
        detectors.add(new TestMaverickDetector());
        detectors.add(new FixtureSmellDetector());
        detectors.add(new HiddenDependencyDetector());
        detectors.add(new MysteryGuestDetector());
        detectors.add(new RottenGreenTestDetector());
        detectors.add(new FlakyTestDetector());
        // Phase 4
        detectors.add(new EagerTestDetector());
        detectors.add(new LazyTestDetector());
        detectors.add(new IndirectTestingDetector());
        detectors.add(new LackOfCohesionDetector());
        return Collections.unmodifiableList(detectors);
    }

    public static List<SmellDetector> filter(List<String> enabled, List<String> disabled) {
        List<SmellDetector> all = allDetectors();
        if (enabled != null && !enabled.isEmpty()) {
            Set<String> enabledSet = enabled.stream()
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet());
            return all.stream()
                    .filter(d -> enabledSet.contains(d.type().name()))
                    .collect(Collectors.toList());
        }
        if (disabled != null && !disabled.isEmpty()) {
            Set<String> disabledSet = disabled.stream()
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet());
            return all.stream()
                    .filter(d -> !disabledSet.contains(d.type().name()))
                    .collect(Collectors.toList());
        }
        return all;
    }
}
