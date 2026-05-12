package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;

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
     * {@code this.foo.bar()} as equivalent. For chained field accesses like
     * {@code MyEnum.VALUE.compute()} or {@code pkg.Cls.STATIC.compute()}, returns
     * the root identifier ({@code MyEnum} / {@code pkg}) so that two distinct enum
     * constants or static fields of the same type don't look like different
     * collaborators. Returns null if the call has no scope (implicit {@code this})
     * or a non-name scope.
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
            return fieldAccessRoot(fae);
        }
        return null;
    }

    /**
     * Returns the root name of a chained FieldAccessExpr — i.e. the leftmost
     * identifier in {@code a.b.c.d}. Returns null if the chain bottoms out in
     * something other than a NameExpr (e.g. {@code foo().b.c}).
     */
    private static String fieldAccessRoot(FieldAccessExpr fae) {
        Expression cursor = fae.getScope();
        while (cursor instanceof FieldAccessExpr) {
            cursor = ((FieldAccessExpr) cursor).getScope();
        }
        if (cursor instanceof NameExpr) {
            return ((NameExpr) cursor).getNameAsString();
        }
        return null;
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
     * True when {@code name} looks like a custom assertion helper AND is the name of
     * a method actually defined in the surrounding test class. Combining the camel-case
     * naming convention with an in-class lookup keeps unrelated production methods
     * (e.g. {@code verifyEmailToken()}, {@code checkBalance()} on a service) from
     * silently being treated as assertions.
     */
    static boolean looksLikeCustomAssertionHelper(String name, Set<String> classMethodNames) {
        if (!classMethodNames.contains(name)) {
            return false;
        }
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
