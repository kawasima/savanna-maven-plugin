package net.unit8.maven.plugins;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DetectMojoTest {

    @Test
    void noTestReason_returnsNoFilesMessage_whenNoTestFiles() {
        Optional<String> reason = DetectMojo.noTestReason(0, 0, 0, 0, "/src/test/java");
        assertThat(reason).contains("No test files found in /src/test/java");
    }

    @Test
    void noTestReason_returnsEmpty_whenEveryFileFailedToParse() {
        Optional<String> reason = DetectMojo.noTestReason(3, 3, 0, 0, "/src/test/java");
        assertThat(reason).isEmpty();
    }

    @Test
    void noTestReason_returnsNoTestMethodsMessage_whenSomeFilesParsedButNoTestMethods() {
        Optional<String> reason = DetectMojo.noTestReason(3, 1, 0, 0, "/src/test/java");
        assertThat(reason).contains("Test files exist but no @Test methods found");
    }

    @Test
    void noTestReason_returnsAllDisabledMessage_whenEveryTestIsDisabled() {
        Optional<String> reason = DetectMojo.noTestReason(2, 0, 5, 5, "/src/test/java");
        assertThat(reason).contains("All 5 test method(s) are @Disabled");
    }

    @Test
    void noTestReason_returnsEmpty_whenAtLeastOneTestIsRunnable() {
        Optional<String> reason = DetectMojo.noTestReason(2, 0, 5, 3, "/src/test/java");
        assertThat(reason).isEmpty();
    }

    @Test
    void noTestReason_returnsEmpty_whenAllFailedExceptOneWithRunnableTests() {
        Optional<String> reason = DetectMojo.noTestReason(3, 2, 1, 0, "/src/test/java");
        assertThat(reason).isEmpty();
    }
}
