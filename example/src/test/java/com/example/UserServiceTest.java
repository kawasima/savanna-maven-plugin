package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class with intentional test smells for demonstration.
 *
 * Smells embedded:
 * - Assertion Roulette: multiple assertions without messages
 * - Magic Number Test: literal numbers in assertions
 * - Redundant Print: System.out.println in tests
 * - Eager Test: single test method calling many production methods
 * - Sensitive Equality: using toString() in assertions
 */
class UserServiceTest {

    private UserService service;
    private OrderService orderService; // General Fixture: not used by all tests

    @BeforeEach
    void setUp() {
        service = new UserService();
        orderService = new OrderService(); // General Fixture smell
    }

    // --- Assertion Roulette: multiple assertions without messages ---
    @Test
    void testCreateUser() {
        User user = service.create("Alice", "alice@example.com");
        assertNotNull(user);
        assertEquals(1, user.getId());
        assertEquals("Alice", user.getName());
        assertEquals("alice@example.com", user.getEmail());
    }

    // --- Magic Number Test ---
    @Test
    void testUserCount() {
        service.create("Alice", "alice@example.com");
        service.create("Bob", "bob@example.com");
        service.create("Charlie", "charlie@example.com");
        assertEquals(3, service.count());
    }

    // --- Redundant Print ---
    @Test
    void testFindUser() {
        User user = service.create("Alice", "alice@example.com");
        User found = service.findById(user.getId());
        System.out.println("Found user: " + found);
        assertNotNull(found);
        assertEquals("Alice", found.getName());
    }

    // --- Eager Test: one test method calls create, update, findById, delete ---
    @Test
    void testUserLifecycle() {
        User user = service.create("Alice", "alice@example.com");
        service.update(user.getId(), "Alice Updated");
        User updated = service.findById(user.getId());
        assertEquals("Alice Updated", updated.getName());
        assertTrue(service.delete(user.getId()));
        assertNull(service.findById(user.getId()));
    }

    // --- Sensitive Equality: using toString() for comparison ---
    @Test
    void testUserToString() {
        User user = service.create("Alice", "alice@example.com");
        assertEquals("User{id=1, name='Alice', email='alice@example.com'}", user.toString());
    }

    // --- Ignored Test ---
    @Disabled("TODO: implement later")
    @Test
    void testConcurrentAccess() {
        // not yet implemented
    }
}
