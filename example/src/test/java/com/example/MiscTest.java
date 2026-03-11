package com.example;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Miscellaneous test class with intentional test smells.
 *
 * Smells embedded:
 * - Empty Test: test with no body
 * - Missing Assertion (Unknown Test): test without any assertion
 * - Sleepy Test: Thread.sleep in test
 * - Default Test: IDE-generated default method name
 * - Mystery Guest: file system access in test
 * - Resource Optimism: file access without existence check
 * - Constructor Initialization: initialization in constructor
 */
class MiscTest {

    // --- Constructor Initialization ---
    private final UserService service;

    MiscTest() {
        service = new UserService();
    }

    // --- Empty Test ---
    @Test
    void testNothing() {
    }

    // --- Missing Assertion (Unknown Test) ---
    @Test
    void testCreateUserWithoutAssertion() {
        User user = service.create("Alice", "alice@example.com");
        user.getName(); // no assertion
    }

    // --- Sleepy Test ---
    @Test
    void testWithDelay() throws InterruptedException {
        User user = service.create("Alice", "alice@example.com");
        Thread.sleep(100);
        assertNotNull(service.findById(user.getId()));
    }

    // --- Default Test: IDE-generated name ---
    @Test
    void test1() {
        assertNotNull(service);
    }

    // --- Mystery Guest + Resource Optimism ---
    @Test
    void testReadConfig() {
        File configFile = new File("config/settings.properties");
        assertTrue(configFile.getName().endsWith(".properties"));
    }

    // --- Ignored Test ---
    @Disabled
    @Test
    void testFutureFeature() {
        fail("Not implemented");
    }
}
