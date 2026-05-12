package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import net.unit8.maven.plugins.smell.*;

import java.util.*;

/**
 * Detects timing/randomness patterns that commonly cause flaky tests.
 * Any one strong indicator (sleep, UUID.randomUUID, new Date, new Random)
 * is enough to fire — these are reliably hostile to deterministic testing.
 */
public class FlakyTestDetector implements SmellDetector {
    private static final Set<String> RANDOM_TYPES = Set.of(
            "Random", "ThreadLocalRandom", "SecureRandom", "SplittableRandom"
    );

    private static final Set<String> NOW_LIKE_TYPES = Set.of(
            "Date"
    );

    /** Scopes whose {@code now()} returns a wall-clock value. */
    private static final Set<String> NOW_SCOPES = Set.of(
            "Instant", "LocalDate", "LocalTime", "LocalDateTime",
            "ZonedDateTime", "OffsetDateTime", "OffsetTime",
            "Year", "YearMonth", "MonthDay", "Clock"
    );

    /** Scopes whose static methods return the current system time. */
    private static final Set<String> SYSTEM_TIME_SCOPES = Set.of("System");

    private static final Set<String> SYSTEM_TIME_METHODS = Set.of(
            "currentTimeMillis", "nanoTime"
    );

    @Override
    public SmellType type() {
        return SmellType.FLAKY_TEST;
    }

    @Override
    public List<TestSmell> detect(DetectionContext context) {
        List<TestSmell> smells = new ArrayList<>();
        String className = context.getTestClass().getNameAsString();

        for (MethodDeclaration method : context.getTestMethods()) {
            List<String> indicators = collectIndicators(method);
            if (!indicators.isEmpty()) {
                smells.add(new TestSmell(
                        SmellType.FLAKY_TEST,
                        className,
                        method.getNameAsString(),
                        method.getBegin().map(p -> p.line).orElse(0),
                        "Flakiness indicator(s): " + String.join(", ", indicators),
                        true
                ));
            }
        }
        return smells;
    }

    private List<String> collectIndicators(MethodDeclaration method) {
        List<String> indicators = new ArrayList<>();

        boolean hasRandomCtor = method.findAll(ObjectCreationExpr.class).stream()
                .anyMatch(expr -> RANDOM_TYPES.contains(expr.getTypeAsString()));
        boolean hasMathRandom = method.findAll(MethodCallExpr.class).stream()
                .anyMatch(call -> call.getNameAsString().equals("random")
                        && hasScopeName(call, "Math"));
        boolean hasUuidRandom = method.findAll(MethodCallExpr.class).stream()
                .anyMatch(call -> call.getNameAsString().equals("randomUUID")
                        && hasScopeName(call, "UUID"));
        if (hasRandomCtor || hasMathRandom || hasUuidRandom) {
            indicators.add("random");
        }

        boolean hasNewDate = method.findAll(ObjectCreationExpr.class).stream()
                .anyMatch(expr -> NOW_LIKE_TYPES.contains(expr.getTypeAsString())
                        && expr.getArguments().isEmpty());
        boolean hasNowCall = method.findAll(MethodCallExpr.class).stream()
                .anyMatch(this::isWallClockCall);
        if (hasNewDate || hasNowCall) {
            indicators.add("time-dependent");
        }

        boolean hasSleep = method.findAll(MethodCallExpr.class).stream()
                .filter(call -> call.getNameAsString().equals("sleep"))
                .anyMatch(call -> call.getScope().map(this::isSleepScope).orElse(false));
        if (hasSleep) {
            indicators.add("sleep");
        }
        return indicators;
    }

    private boolean isWallClockCall(MethodCallExpr call) {
        String name = call.getNameAsString();
        Expression scope = call.getScope().orElse(null);
        if (scope == null) {
            return false;
        }
        String scopeName = scopeName(scope);
        if (scopeName == null) {
            return false;
        }
        if (name.equals("now")) {
            return NOW_SCOPES.contains(scopeName);
        }
        if (SYSTEM_TIME_METHODS.contains(name)) {
            return SYSTEM_TIME_SCOPES.contains(scopeName);
        }
        return false;
    }

    private String scopeName(Expression scope) {
        if (scope instanceof NameExpr) {
            return ((NameExpr) scope).getNameAsString();
        }
        if (scope instanceof FieldAccessExpr) {
            return ((FieldAccessExpr) scope).getNameAsString();
        }
        return null;
    }

    private boolean isSleepScope(Expression scope) {
        if (scope instanceof NameExpr) {
            String name = ((NameExpr) scope).getNameAsString();
            return name.equals("Thread") || TIMEUNIT_NAMES.contains(name);
        }
        if (scope instanceof FieldAccessExpr) {
            FieldAccessExpr fae = (FieldAccessExpr) scope;
            return TIMEUNIT_NAMES.contains(fae.getNameAsString())
                    && isTimeUnitQualifier(fae.getScope());
        }
        return false;
    }

    private boolean isTimeUnitQualifier(Expression scope) {
        if (scope instanceof NameExpr) {
            return ((NameExpr) scope).getNameAsString().equals("TimeUnit");
        }
        if (scope instanceof FieldAccessExpr) {
            return ((FieldAccessExpr) scope).getNameAsString().equals("TimeUnit");
        }
        return false;
    }

    private boolean hasScopeName(MethodCallExpr call, String expected) {
        return call.getScope()
                .filter(s -> s instanceof NameExpr)
                .map(s -> ((NameExpr) s).getNameAsString())
                .filter(expected::equals)
                .isPresent();
    }

    private static final Set<String> TIMEUNIT_NAMES = Set.of(
            "NANOSECONDS", "MICROSECONDS", "MILLISECONDS", "SECONDS", "MINUTES", "HOURS", "DAYS"
    );
}
