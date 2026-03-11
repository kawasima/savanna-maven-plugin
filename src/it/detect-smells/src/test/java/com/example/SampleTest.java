package com.example;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SampleTest {

    // Empty Test
    @Test
    void emptyTest() {
    }

    // Missing Assertion
    @Test
    void noAssertions() {
        int x = 1 + 1;
    }

    // Ignored Test
    @Disabled
    @Test
    void ignoredTest() {
        assertEquals(1, 1);
    }

    // Redundant Print
    @Test
    void withPrintln() {
        System.out.println("debug");
        assertEquals(1, 1);
    }

    // Assertion Roulette (multiple assertions without message)
    @Test
    void rouletteTest() {
        assertEquals(1, 1);
        assertEquals(2, 2);
        assertEquals(3, 3);
    }

    // Sleepy Test
    @Test
    void sleepyTest() throws InterruptedException {
        Thread.sleep(100);
        assertTrue(true);
    }

    // Conditional Test Logic
    @Test
    void conditionalTest() {
        int x = 1;
        if (x > 0) {
            assertEquals(1, x);
        }
    }

    // Redundant Assertion
    @Test
    void redundantTest() {
        assertTrue(true);
    }
}
