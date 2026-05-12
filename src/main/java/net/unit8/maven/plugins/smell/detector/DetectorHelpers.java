package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Shared helpers for AST-level smell detectors. These centralize patterns
 * that several detectors used to reimplement (often inconsistently):
 * field-reference resolution that understands {@code this.x},
 * receiver-name extraction that follows method chains, and a uniform
 * notion of "is this a JUnit/AssertJ/Mockito assertion call?".
 */
final class DetectorHelpers {

    /** JUnit Jupiter & AssertJ entry-point methods that take a value to assert. */
    static final Set<String> JUNIT_ASSERTION_METHODS = Set.of(
            "assertEquals", "assertNotEquals",
            "assertSame", "assertNotSame",
            "assertTrue", "assertFalse",
            "assertNull", "assertNotNull",
            "assertArrayEquals", "assertIterableEquals", "assertLinesMatch",
            "assertThrows", "assertDoesNotThrow",
            "assertTimeout", "assertTimeoutPreemptively",
            "assertInstanceOf", "assertAll",
            "fail"
    );

    /** Methods that act as assertion entry points (JUnit / Hamcrest / AssertJ / BDDMockito). */
    static final Set<String> ASSERTION_ENTRY_METHODS = Set.of(
            "assertThat", "assertThatThrownBy", "assertThatExceptionOfType",
            "assertThatCode", "assertThatNoException",
            "assertThatNullPointerException", "assertThatIllegalArgumentException",
            "assertThatIllegalStateException",
            "then"
    );

    /** Mockito verification methods that act as implicit assertions. */
    static final Set<String> MOCKITO_VERIFICATION_METHODS = Set.of(
            "verify", "verifyNoInteractions", "verifyNoMoreInteractions", "verifyZeroInteractions"
    );

    /** AssertJ fluent terminal methods that, when chained off assertThat, contain assertion values. */
    static final Set<String> ASSERTJ_TERMINALS = Set.of(
            "isEqualTo", "isNotEqualTo",
            "isLessThan", "isLessThanOrEqualTo",
            "isGreaterThan", "isGreaterThanOrEqualTo",
            "isCloseTo", "isBetween",
            "isTrue", "isFalse",
            "isNull", "isNotNull",
            "isEmpty", "isNotEmpty",
            "isInstanceOf", "isExactlyInstanceOf",
            "hasSize", "hasSizeGreaterThan", "hasSizeLessThan",
            "contains", "containsExactly", "containsOnly",
            "containsExactlyInAnyOrder", "containsOnlyOnce",
            "doesNotContain",
            "startsWith", "endsWith",
            "hasMessage", "hasMessageContaining", "hasMessageStartingWith",
            "hasFieldOrPropertyWithValue", "extracting"
    );

    private DetectorHelpers() {
    }

    /**
     * Returns true when {@code expr} textually references a field with the given name.
     * Recognizes both bare {@code field} (NameExpr) and {@code this.field} (FieldAccessExpr).
     */
    static boolean isFieldReference(Expression expr, String fieldName) {
        if (expr instanceof NameExpr) {
            return ((NameExpr) expr).getNameAsString().equals(fieldName);
        }
        if (expr instanceof FieldAccessExpr) {
            FieldAccessExpr fae = (FieldAccessExpr) expr;
            return fae.getScope() instanceof ThisExpr
                    && fae.getNameAsString().equals(fieldName);
        }
        return false;
    }

    /**
     * Extracts the receiver name from a method call, treating {@code foo.bar()} and
     * {@code this.foo.bar()} as equivalent. For chained field accesses the goal
     * is to return a stable collaborator identity:
     * <ul>
     *   <li>{@code MyEnum.VALUE.compute()} → {@code "MyEnum"} (enum constants of
     *       the same type share an identity)</li>
     *   <li>{@code com.foo.Bar.baz()} → {@code "Bar"} (the class, not the
     *       package root)</li>
     * </ul>
     * Heuristic: among the identifiers in the chain, return the rightmost one
     * whose name looks like a type ({@code UpperCamelCase}); if none, return the
     * root identifier. Returns null if the chain doesn't bottom out in a NameExpr.
     */
    static String receiverName(MethodCallExpr call) {
        Expression scope = call.getScope().orElse(null);
        if (scope == null) {
            return null;
        }
        if (scope instanceof NameExpr) {
            return ((NameExpr) scope).getNameAsString();
        }
        if (scope instanceof FieldAccessExpr) {
            FieldAccessExpr fae = (FieldAccessExpr) scope;
            if (fae.getScope() instanceof ThisExpr) {
                return fae.getNameAsString();
            }
            return chainCollaboratorName(fae);
        }
        return null;
    }

    /**
     * Walks the field-access chain and returns the rightmost identifier that
     * looks like a type name. Falls back to the leftmost identifier when no
     * step looks like a type.
     *
     * <p>The "type-like" judgement is context-sensitive on the chain root:
     * <ul>
     *   <li>If the root is a lowercase identifier (a package segment, as in
     *       {@code com.foo.Bar.baz()}), a single uppercase letter also counts
     *       as a type — covers single-letter class names like {@code X}.</li>
     *   <li>If the root is itself uppercase (an enum/class scenario like
     *       {@code MyEnum.VALUE_A.compute()}), single uppercase letters
     *       continue to be treated as constants, so the root wins.</li>
     * </ul>
     */
    private static String chainCollaboratorName(FieldAccessExpr fae) {
        List<String> names = new ArrayList<>();
        Expression cursor = fae;
        while (cursor instanceof FieldAccessExpr) {
            FieldAccessExpr current = (FieldAccessExpr) cursor;
            names.add(current.getNameAsString());
            cursor = current.getScope();
        }
        if (!(cursor instanceof NameExpr)) {
            return null;
        }
        String root = ((NameExpr) cursor).getNameAsString();
        boolean rootIsLowerCase = !root.isEmpty() && Character.isLowerCase(root.charAt(0));
        Collections.reverse(names);
        String collaborator = root;
        for (String n : names) {
            if (looksLikeTypeName(n, rootIsLowerCase)) {
                collaborator = n;
            }
        }
        return collaborator;
    }

    /**
     * True when {@code name} reads as a type identifier. Multi-character
     * UpperCamelCase ({@code Bar}, {@code MyEnum}) always qualifies; a single
     * uppercase letter only qualifies when {@code permitSingleUpper} is set
     * (typically when the chain root is a package segment).
     */
    private static boolean looksLikeTypeName(String name, boolean permitSingleUpper) {
        if (name.isEmpty() || !Character.isUpperCase(name.charAt(0))) {
            return false;
        }
        if (name.length() == 1) {
            return permitSingleUpper;
        }
        for (int i = 0; i < name.length(); i++) {
            if (Character.isLowerCase(name.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * True when {@code call} is any kind of assertion (JUnit, AssertJ, Hamcrest, Mockito verify).
     * Recognizes AssertJ fluent terminals only when the chain is rooted in an
     * {@code assertThat(...)} call to avoid matching unrelated methods of the same name.
     */
    static boolean isAssertionCall(MethodCallExpr call) {
        String name = call.getNameAsString();
        if (JUNIT_ASSERTION_METHODS.contains(name)) {
            return true;
        }
        if (ASSERTION_ENTRY_METHODS.contains(name)) {
            return true;
        }
        if (MOCKITO_VERIFICATION_METHODS.contains(name)) {
            return true;
        }
        if (ASSERTJ_TERMINALS.contains(name)) {
            return chainRootedIn(call, ASSERTION_ENTRY_METHODS);
        }
        return false;
    }

    /** True when the chain root method name (deepest scope of {@code call}) is in {@code names}. */
    static boolean chainRootedIn(MethodCallExpr call, Set<String> names) {
        Expression scope = call.getScope().orElse(null);
        while (scope instanceof MethodCallExpr) {
            MethodCallExpr inner = (MethodCallExpr) scope;
            if (names.contains(inner.getNameAsString())) {
                return true;
            }
            scope = inner.getScope().orElse(null);
        }
        return false;
    }

    /**
     * True when {@code name} matches the camel-case naming convention for a
     * custom assertion helper ({@code assertX}, {@code verifyX}, {@code checkX},
     * {@code expectX}). This is a pure naming check — callers should combine
     * it with additional context (e.g. "is the call unqualified and does the
     * test class actually declare this method?") to avoid suppressing genuine
     * MISSING_ASSERTION cases.
     */
    static boolean hasCustomAssertionHelperName(String name) {
        return startsWithCamelPrefix(name, "assert")
                || startsWithCamelPrefix(name, "verify")
                || startsWithCamelPrefix(name, "check")
                || startsWithCamelPrefix(name, "expect");
    }

    private static boolean startsWithCamelPrefix(String name, String prefix) {
        if (!name.startsWith(prefix) || name.length() == prefix.length()) {
            return false;
        }
        char next = name.charAt(prefix.length());
        return Character.isUpperCase(next);
    }
}
