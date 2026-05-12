package net.unit8.maven.plugins.smell.detector;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.expr.MethodCallExpr;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DetectorHelpersTest {

    @Test
    void isAssertionCallTreatsExtractingAsNonTerminal() {
        // Regression: previously ASSERTJ_TERMINALS contained "extracting",
        // which is actually a navigation step, not a terminal. So
        // assertThat(xs).extracting("foo") used to be reported as a
        // complete assertion by DetectorHelpers.isAssertionCall, even
        // though no value-bearing check follows.
        MethodCallExpr chain = StaticJavaParser.parseExpression(
                "assertThat(xs).extracting(\"foo\")");

        // The outer extracting() call should NOT be flagged as an assertion.
        // (assertThat() itself still is — it's an entry point — but the
        // navigation step on its own is not.)
        List<MethodCallExpr> calls = chain.findAll(MethodCallExpr.class);
        MethodCallExpr extracting = calls.stream()
                .filter(c -> c.getNameAsString().equals("extracting"))
                .findFirst().orElseThrow();
        assertThat(DetectorHelpers.isAssertionCall(extracting)).isFalse();
    }

    @Test
    void isAssertionCallStillRecognisesTerminalsRootedInAssertThat() {
        MethodCallExpr chain = StaticJavaParser.parseExpression(
                "assertThat(xs).hasSize(3)");
        MethodCallExpr hasSize = chain.findAll(MethodCallExpr.class).stream()
                .filter(c -> c.getNameAsString().equals("hasSize"))
                .findFirst().orElseThrow();
        assertThat(DetectorHelpers.isAssertionCall(hasSize)).isTrue();
    }
}
