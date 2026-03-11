package net.unit8.maven.plugins.smell;

public enum SmellType {
    ASSERTION_ROULETTE("Assertion Roulette"),
    CONDITIONAL_TEST_LOGIC("Conditional Test Logic"),
    CONSTRUCTOR_INITIALIZATION("Constructor Initialization"),
    DEFAULT_TEST("Default Test"),
    DUPLICATE_ASSERT("Duplicate Assert"),
    EAGER_TEST("Eager Test"),
    EMPTY_TEST("Empty Test"),
    EXCEPTION_HANDLING("Exception Handling"),
    GENERAL_FIXTURE("General Fixture"),
    IGNORED_TEST("Ignored Test"),
    LAZY_TEST("Lazy Test"),
    MAGIC_NUMBER_TEST("Magic Number Test"),
    MYSTERY_GUEST("Mystery Guest"),
    REDUNDANT_PRINT("Redundant Print"),
    REDUNDANT_ASSERTION("Redundant Assertion"),
    RESOURCE_OPTIMISM("Resource Optimism"),
    SENSITIVE_EQUALITY("Sensitive Equality"),
    SLEEPY_TEST("Sleepy Test"),
    MISSING_ASSERTION("Missing Assertion"),
    LACK_OF_COHESION("Lack of Cohesion of Test Cases"),
    OBSCURE_INLINE_SETUP("Obscure In-Line Setup"),
    TEST_MAVERICK("Test Maverick"),
    INDIRECT_TESTING("Indirect Testing"),
    TEST_RUN_WAR("Test Run War"),
    VERBOSE_TEST("Verbose Test"),
    ROTTEN_GREEN_TEST("Rotten Green Test"),
    ORDER_DEPENDENT_TEST("Order-Dependent Test"),
    HIDDEN_DEPENDENCY("Hidden Dependency"),
    RESOURCE_LEAKAGE("Resource Leakage"),
    TIME_SENSITIVE_TEST("Time Sensitive Test"),
    EXTERNAL_DEPENDENCY("External Dependency"),
    DEAD_FIELD("Dead Field"),
    FLAKY_TEST("Flaky Test"),
    NO_TEST("No Test"),
    SKIP_TESTING("Skip Testing");

    private final String displayName;

    SmellType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the camelCase property key for this smell type.
     * e.g. ASSERTION_ROULETTE -> "assertionRoulette"
     */
    public String getPropertyKey() {
        String[] parts = name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            sb.append(Character.toUpperCase(parts[i].charAt(0)));
            sb.append(parts[i].substring(1));
        }
        return sb.toString();
    }
}
