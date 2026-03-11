package com.example;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class OrderService {
    private final List<Order> orders = new ArrayList<>();

    public Order placeOrder(String item, int quantity, BigDecimal price) {
        Order order = new Order(item, quantity, price);
        orders.add(order);
        return order;
    }

    public BigDecimal calculateTotal() {
        return orders.stream()
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int getOrderCount() {
        return orders.size();
    }

    public void clear() {
        orders.clear();
    }
}
