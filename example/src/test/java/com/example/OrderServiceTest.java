package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class with intentional test smells for demonstration.
 *
 * Smells embedded:
 * - Verbose Test: overly long test method
 * - Duplicate Assert: same assertion repeated
 * - Redundant Assertion: assertEquals(true, true)
 * - Conditional Test Logic: if statement in test
 * - Exception Handling: try-catch in test
 */
class OrderServiceTest {

    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService();
    }

    // --- Verbose Test: too many lines ---
    @Test
    void testPlaceMultipleOrders() {
        Order order1 = service.placeOrder("Widget", 2, new BigDecimal("10.00"));
        assertNotNull(order1);
        assertEquals("Widget", order1.getItem());
        assertEquals(2, order1.getQuantity());
        assertEquals(new BigDecimal("10.00"), order1.getPrice());
        assertEquals(new BigDecimal("20.00"), order1.getTotal());

        Order order2 = service.placeOrder("Gadget", 1, new BigDecimal("25.50"));
        assertNotNull(order2);
        assertEquals("Gadget", order2.getItem());
        assertEquals(1, order2.getQuantity());
        assertEquals(new BigDecimal("25.50"), order2.getPrice());
        assertEquals(new BigDecimal("25.50"), order2.getTotal());

        Order order3 = service.placeOrder("Doohickey", 3, new BigDecimal("5.00"));
        assertNotNull(order3);
        assertEquals("Doohickey", order3.getItem());
        assertEquals(3, order3.getQuantity());
        assertEquals(new BigDecimal("5.00"), order3.getPrice());
        assertEquals(new BigDecimal("15.00"), order3.getTotal());

        assertEquals(3, service.getOrderCount());
        assertEquals(new BigDecimal("60.50"), service.calculateTotal());
    }

    // --- Duplicate Assert ---
    @Test
    void testOrderTotal() {
        Order order = service.placeOrder("Widget", 5, new BigDecimal("10.00"));
        assertEquals(new BigDecimal("50.00"), order.getTotal());
        assertEquals(new BigDecimal("50.00"), service.calculateTotal());
        assertEquals(new BigDecimal("50.00"), order.getTotal()); // duplicate
    }

    // --- Redundant Assertion ---
    @Test
    void testOrderServiceExists() {
        assertNotNull(service);
        assertEquals(true, true); // redundant
        assertEquals(0, service.getOrderCount());
    }

    // --- Conditional Test Logic ---
    @Test
    void testClearOrders() {
        service.placeOrder("Widget", 1, new BigDecimal("10.00"));
        if (service.getOrderCount() > 0) {
            service.clear();
        }
        assertEquals(0, service.getOrderCount());
    }

    // --- Exception Handling in test ---
    @Test
    void testPlaceOrderWithExceptionHandling() {
        try {
            Order order = service.placeOrder("Widget", 1, new BigDecimal("10.00"));
            assertNotNull(order);
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }
}
